package com.example.projectstudybuddy1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);
        db = AppDatabase.getDatabase(requireContext());

        RecyclerView rvJournalEntries = view.findViewById(R.id.rvJournalEntries);
        rvJournalEntries.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new JournalAdapter();
        rvJournalEntries.setAdapter(adapter);

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddJournalEntry);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), JournalEditorActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadJournalEntries();
    }

    private void loadJournalEntries() {
        journalList.clear();

        // Separate Logic: Pull only items flagged with Journal or Notetaking keywords
        List<TaskItem> allItems = db.appDao().getAllTasks();
        for (TaskItem item : allItems) {
            if (item.title != null && (item.title.startsWith("Journal ") || item.title.startsWith("Notetaking "))) {
                journalList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
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
            holder.tvTitle.setText(entry.title);
            holder.tvDate.setText(entry.dateCreated);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), JournalEditorActivity.class);
                intent.putExtra("JOURNAL_ID", entry.taskId);
                startActivity(intent);
            });

            holder.ivDelete.setOnClickListener(v -> {
                db.appDao().deleteTask(entry);
                journalList.remove(position);
                notifyItemRemoved(position);
            });
        }

        @Override
        public int getItemCount() { return journalList.size(); }

        class JournalViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate;
            ImageView ivDelete;

            public JournalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                ivDelete = itemView.findViewById(R.id.ivTaskRowDelete);

                View progressMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                if (progressMeter != null) progressMeter.setVisibility(View.GONE);
                View progressText = itemView.findViewById(R.id.tvRowTaskPercentage);
                if (progressText != null) progressText.setVisibility(View.GONE);
            }
        }
    }
}