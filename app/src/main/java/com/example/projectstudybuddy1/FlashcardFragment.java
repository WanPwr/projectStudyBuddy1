package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem; // Reusing structural entity row cache layouts
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FlashcardFragment extends Fragment {

    private AppDatabase db;
    private RecyclerView rvFlashcardDecks;
    private FlashcardAdapter adapter;
    private final List<TaskItem> deckList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_flashcard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        // FIXED: Only hook up layout views that exist in your clean XML
        rvFlashcardDecks = v.findViewById(R.id.rvFlashcardDecks);
        rvFlashcardDecks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FlashcardAdapter();
        rvFlashcardDecks.setAdapter(adapter);

        FloatingActionButton fabAdd = v.findViewById(R.id.fabAddFlashcardDeck);
        fabAdd.setOnClickListener(view -> createNewFlashcardDeckWorkspace());

        loadFlashcardDecks();
        return v;
    }

    private void loadFlashcardDecks() {
        deckList.clear();
        List<TaskItem> allItems = db.appDao().getAllTasks();
        for (TaskItem item : allItems) {
            if (item.title != null && item.title.startsWith("Deck: ")) {
                deckList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void createNewFlashcardDeckWorkspace() {
        TaskItem newDeck = new TaskItem();
        int deckNumber = deckList.size() + 1;
        newDeck.title = "Deck: Flashcard " + deckNumber;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        newDeck.dateCreated = sdf.format(new Date());

        new Thread(() -> {
            db.appDao().insertTask(newDeck);
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::loadFlashcardDecks);
            }
        }).start();
    }

    // =======================================================
    // ADAPTER: CLEAN UNIFIED FLASHCARD DECKS
    // =======================================================
    private class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {
        @NonNull
        @Override
        public FlashcardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flashcard_row, parent, false);
            return new FlashcardViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull FlashcardViewHolder holder, int position) {
            TaskItem deck = deckList.get(position);
            holder.tvTitle.setText(deck.title.replace("Deck: ", ""));

            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Open study session for: " + deck.title, Toast.LENGTH_SHORT).show();
            });

            holder.ivDelete.setOnClickListener(v -> {
                new Thread(() -> {
                    db.appDao().deleteTask(deck);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            deckList.remove(position);
                            notifyItemRemoved(position);
                        });
                    }
                }).start();
            });
        }

        @Override
        public int getItemCount() { return deckList.size(); }

        class FlashcardViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            ImageView ivDelete;

            public FlashcardViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvFlashcardRowTitle);
                ivDelete = itemView.findViewById(R.id.ivFlashcardRowDelete);
            }
        }
    }
}