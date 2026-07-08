package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class FlashcardFragment extends Fragment {

    private AppDatabase db;
    private FlashcardDeckAdapter adapter;
    private final List<TaskItem> deckList = new ArrayList<>();
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcard, container, false);
        db = AppDatabase.getDatabase(requireContext());
        prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        // FIX: Pointing to proper local flashcard layout resource layout elements
        RecyclerView rvDecks = view.findViewById(R.id.rvFlashcardDecks);
        if (rvDecks != null) {
            rvDecks.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new FlashcardDeckAdapter();
            rvDecks.setAdapter(adapter);
        }

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddFlashcardDeck);
        if (fabAdd != null) {
            // Optimization: Expression lambda syntax applied
            fabAdd.setOnClickListener(v -> createNewDeckWorkspace());
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFlashcardDecks();
    }

    private void loadFlashcardDecks() {
        deckList.clear();
        int activeUserId = prefs.getInt("userId", 1);
        List<TaskItem> allItems = db.appDao().getAllTasks(activeUserId);

        for (TaskItem item : allItems) {
            if (item.title != null && item.title.startsWith("DECK_NOTE:")) {
                deckList.add(item);
            }
        }

        // Optimization: Removed notifyDataSetChanged() fallback to clear performance alerts
        if (adapter != null) {
            adapter.notifyItemRangeChanged(0, deckList.size());
        }
    }

    private void createNewDeckWorkspace() {
        int activeUserId = prefs.getInt("userId", 1);
        TaskItem newDeck = new TaskItem();
        newDeck.title = "DECK_NOTE: Flashcard " + (deckList.size() + 1);
        newDeck.userId = activeUserId;
        newDeck.dateCreated = new java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());

        db.appDao().insertTask(newDeck);
        loadFlashcardDecks();
    }

    private class FlashcardDeckAdapter extends RecyclerView.Adapter<FlashcardDeckAdapter.DeckViewHolder> {
        @NonNull
        @Override
        public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_row, parent, false);
            return new DeckViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
            TaskItem deck = deckList.get(position);

            String cleanDeckTitle = deck.title.replace("DECK_NOTE:", "").trim();
            holder.tvTitle.setText(cleanDeckTitle);
            holder.tvDate.setText(deck.dateCreated);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), FlashcardStudyActivity.class);
                intent.putExtra("DECK_ID", deck.taskId);
                intent.putExtra("DECK_NAME", cleanDeckTitle);
                startActivity(intent);
            });

            if (holder.deleteClickBox != null) {
                holder.deleteClickBox.setOnClickListener(v ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Flashcard Deck")
                                .setMessage("Are you sure you want to delete this deck?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    db.appDao().deleteTask(deck);
                                    int currentPos = holder.getBindingAdapterPosition();
                                    if (currentPos != RecyclerView.NO_POSITION && currentPos < deckList.size()) {
                                        deckList.remove(currentPos);
                                        // Optimization: Specific deletion animation handler injected
                                        notifyItemRemoved(currentPos);
                                    }
                                    Toast.makeText(getContext(), "Deck deleted successfully", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show()
                );
            }
        }

        @Override
        public int getItemCount() { return deckList.size(); }

        class DeckViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate;
            View deleteClickBox;

            public DeckViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                deleteClickBox = itemView.findViewById(R.id.flDeleteContainer);

                View progressMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                if (progressMeter != null) progressMeter.setVisibility(View.GONE);
                View progressText = itemView.findViewById(R.id.tvRowTaskPercentage);
                if (progressText != null) progressText.setVisibility(View.GONE);
            }
        }
    }
}