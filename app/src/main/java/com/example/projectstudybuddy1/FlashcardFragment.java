package com.example.projectstudybuddy1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FlashcardFragment extends Fragment {
    private AppDatabase db;
    private DeckAdapter adapter;
    private final List<TaskItem> deckList = new ArrayList<>();
    private SharedPreferences prefs;
    private String lastKnownSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_flashcard, container, false);
        db = AppDatabase.getDatabase(requireContext());
        prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        RecyclerView rv = v.findViewById(R.id.rvFlashcardDecks);
        FloatingActionButton fab = v.findViewById(R.id.fabAddFlashcardDeck);

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new DeckAdapter();
            rv.setAdapter(adapter);
        }

        if (fab != null) {
            fab.setOnClickListener(view -> displayCreationDialog());
        }

        SearchView svCardsSearch = v.findViewById(R.id.svCardsSearch);
        if (svCardsSearch != null) {
            svCardsSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    lastKnownSearchQuery = query.trim();
                    loadDecks();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    lastKnownSearchQuery = newText.trim();
                    loadDecks();
                    return true;
                }
            });
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDecks();
    }

    private void loadDecks() {
        deckList.clear();
        int activeUserId = prefs.getInt("userId", 1);

        List<TaskItem> allItems;
        if (lastKnownSearchQuery.isEmpty()) {
            allItems = db.appDao().getAllTasks(activeUserId);
        } else {
            allItems = db.appDao().searchTasksByQuery(activeUserId, lastKnownSearchQuery);
        }

        for (TaskItem item : allItems) {
            if (item.title != null && item.title.startsWith("DECK_NOTE:")) {
                deckList.add(item);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void displayCreationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Flashcard Deck");

        final EditText inputField = new EditText(requireContext());
        inputField.setHint("Enter deck title...");
        builder.setView(inputField);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String deckName = inputField.getText().toString().trim();
            if (deckName.isEmpty()) {
                Toast.makeText(getContext(), "Deck name cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            int activeUserId = prefs.getInt("userId", 1);

            TaskItem newDeck = new TaskItem();
            newDeck.title = "DECK_NOTE: " + deckName;
            newDeck.userId = activeUserId;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            newDeck.dateCreated = sdf.format(new Date());
            newDeck.completed = false;
            newDeck.isRoutine = false;

            db.appDao().insertTask(newDeck);
            loadDecks();
            Toast.makeText(getContext(), "Deck created successfully!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {
        @NonNull
        @Override
        public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_row, parent, false);
            return new DeckViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DeckViewHolder holder, int pos) {
            TaskItem currentDeck = deckList.get(pos);
            if (currentDeck.title != null) {
                String cleanName = currentDeck.title.replace("DECK_NOTE:", "").trim();
                holder.tvTitle.setText(cleanName);
            }
            holder.tvDate.setText(currentDeck.dateCreated);

            holder.textLayoutContainer.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), FlashcardStudyActivity.class);
                intent.putExtra("DECK_ID", currentDeck.taskId);
                startActivity(intent);
            });

            if (holder.deleteClickBox != null) {
                holder.deleteClickBox.setOnClickListener(v ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Flashcard Deck")
                                .setMessage("Are you sure you want to delete this deck?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    db.appDao().deleteTask(currentDeck);
                                    loadDecks();
                                    Toast.makeText(getContext(), "Deck deleted", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show()
                );
            }
        }

        @Override
        public int getItemCount() { return deckList.size(); }

        class DeckViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate;
            View deleteClickBox;
            View textLayoutContainer;

            public DeckViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                deleteClickBox = itemView.findViewById(R.id.flDeleteContainer);
                textLayoutContainer = itemView.findViewById(R.id.llTextContainer);

                View progressMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                if (progressMeter != null) progressMeter.setVisibility(View.GONE);

                View progressText = itemView.findViewById(R.id.tvRowTaskPercentage);
                if (progressText != null) progressText.setVisibility(View.GONE);

                // FIXED: Changed fields to exact ConstraintLayout structural names to fix the syntax error
                if (textLayoutContainer != null && textLayoutContainer.getLayoutParams() instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                            (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) textLayoutContainer.getLayoutParams();

                    params.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    textLayoutContainer.setLayoutParams(params);
                }
            }
        }
    }
}