package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class JournalFragment extends Fragment {

    private AppDatabase db;
    private JournalAdapter adapter;
    private final List<TaskItem> journalList = new ArrayList<>();
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);
        db = AppDatabase.getDatabase(requireContext());
        prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        RecyclerView rvJournalEntries = view.findViewById(R.id.rvJournalEntries);
        if (rvJournalEntries != null) {
            rvJournalEntries.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new JournalAdapter();
            rvJournalEntries.setAdapter(adapter);
        }

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddJournalEntry);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), JournalEditorActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadJournalEntries();
    }

    private void loadJournalEntries() {
        journalList.clear();
        int activeUserId = prefs.getInt("userId", 1);
        List<TaskItem> allItems = db.appDao().getAllTasks(activeUserId);

        for (TaskItem item : allItems) {
            if (item.title != null && item.title.startsWith("JOURNAL_NOTE:")) {
                journalList.add(item);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {
        @NonNull
        @Override
        public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_row, parent, false);
            return new JournalViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
            TaskItem entry = journalList.get(position);

            String displayColorHex = "#FFFFFF";
            String rawTitleClean = entry.title.replace("JOURNAL_NOTE:", "").trim();

            if (rawTitleClean.contains("||COLOR_SEP||")) {
                String[] colorSplit = rawTitleClean.split("\\|\\|COLOR_SEP\\|\\|");
                if (colorSplit.length > 0) {
                    rawTitleClean = colorSplit[0].trim();
                }
                if (colorSplit.length > 1) {
                    displayColorHex = colorSplit[1].trim();
                    if (displayColorHex.equals("#E07A5F")) {
                        displayColorHex = "#FDF0CD";
                    }
                }
            }

            String userCleanTitle;
            if (rawTitleClean.contains("||CONTENT_SEP||")) {
                String[] segments = rawTitleClean.split("\\s*\\|\\|CONTENT_SEP\\|\\|\\s*");
                userCleanTitle = segments[0].trim();
            } else {
                userCleanTitle = rawTitleClean;
            }

            holder.tvTitle.setText(userCleanTitle);
            holder.tvDate.setText(entry.dateCreated);

            try {
                int parsedColor = Color.parseColor(displayColorHex);
                holder.cardRoot.setBackgroundTintList(ColorStateList.valueOf(parsedColor));

                // CONTRAST ENGINE: White text triggers synchronized for Red (#E63946)
                if (displayColorHex.equals("#3D405B") || displayColorHex.equals("#A06CD5") || displayColorHex.equals("#81B29A") || displayColorHex.equals("#E63946")) {
                    holder.tvTitle.setTextColor(Color.WHITE);
                    holder.tvDate.setTextColor(Color.LTGRAY);
                } else {
                    holder.tvTitle.setTextColor(Color.parseColor("#3D405B"));
                    holder.tvDate.setTextColor(Color.GRAY);
                }
            } catch (IllegalArgumentException e) {
                holder.cardRoot.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                holder.tvTitle.setTextColor(Color.parseColor("#3D405B"));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), JournalEditorActivity.class);
                intent.putExtra("JOURNAL_ID", entry.taskId);
                startActivity(intent);
            });

            if (holder.deleteClickBox != null) {
                holder.deleteClickBox.setOnClickListener(v ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Journal Entry")
                                .setMessage("Are you sure you want to delete this journal entry?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    db.appDao().deleteTask(entry);
                                    int currentPos = holder.getBindingAdapterPosition();
                                    if (currentPos != RecyclerView.NO_POSITION && currentPos < journalList.size()) {
                                        journalList.remove(currentPos);
                                        notifyItemRemoved(currentPos);
                                    }
                                    Toast.makeText(getContext(), "Journal entry deleted", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show()
                );
            }
        }

        @Override public int getItemCount() { return journalList.size(); }

        class JournalViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate;
            View deleteClickBox;
            View cardRoot;

            public JournalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                deleteClickBox = itemView.findViewById(R.id.flDeleteContainer);

                cardRoot = itemView;

                View progressMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                if (progressMeter != null) progressMeter.setVisibility(View.GONE);
                View progressText = itemView.findViewById(R.id.tvRowTaskPercentage);
                if (progressText != null) progressText.setVisibility(View.GONE);
            }
        }
    }
}