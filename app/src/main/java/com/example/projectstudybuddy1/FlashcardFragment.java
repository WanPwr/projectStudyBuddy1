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
import androidx.appcompat.widget.SearchView;
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
    private DeckAdapter adapter;
    private final List<TaskItem> deckList = new ArrayList<>();
    private SharedPreferences prefs;
    private String lastKnownSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcard, container, false);
        db = AppDatabase.getDatabase(requireContext());
        prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        RecyclerView rvFlashcardDecks = view.findViewById(R.id.rvFlashcardDecks);
        if (rvFlashcardDecks != null) {
            rvFlashcardDecks.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new DeckAdapter();
            rvFlashcardDecks.setAdapter(adapter);
        }

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddFlashcardDeck);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Create new deck", Toast.LENGTH_SHORT).show();
            });
        }

        SearchView svCardsSearch = view.findViewById(R.id.svCardsSearch);
        if (svCardsSearch != null) {
            svCardsSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    lastKnownSearchQuery = query.trim();
                    loadFlashcardDecks();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    lastKnownSearchQuery = newText.trim();
                    loadFlashcardDecks();
                    return true;
                }
            });
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

    private class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {
        @NonNull
        @Override
        public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_row, parent, false);
            return new DeckViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
            TaskItem deck = deckList.get(position);
            String rawTitleClean = deck.title.replace("DECK_NOTE:", "").trim();
            holder.tvTitle.setText(rawTitleClean);
            holder.tvDate.setText(deck.dateCreated);
            holder.cardRoot.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            holder.tvTitle.setTextColor(Color.parseColor("#3D405B"));
            holder.tvDate.setTextColor(Color.GRAY);
        }

        @Override public int getItemCount() { return deckList.size(); }

        class DeckViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate;
            View cardRoot;

            public DeckViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                cardRoot = itemView;

                View progressMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                if (progressMeter != null) progressMeter.setVisibility(View.GONE);
                View progressText = itemView.findViewById(R.id.tvRowTaskPercentage);
                if (progressText != null) progressText.setVisibility(View.GONE);
            }
        }
    }
}