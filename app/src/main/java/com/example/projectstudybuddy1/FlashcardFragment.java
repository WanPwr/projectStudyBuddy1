package com.example.projectstudybuddy1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.FlashcardItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class FlashcardFragment extends Fragment {
    private AppDatabase db;
    private DeckAdapter adapter;
    private List<String> deckList = new ArrayList<>();

    private View viewDecks, viewStudy;
    private TextView tvDeckTitle, tvCardText;
    private List<FlashcardItem> currentSessionCards = new ArrayList<>();
    private int cardIndex = 0;
    private boolean showingQuestion = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_flashcard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        viewDecks = v.findViewById(R.id.viewDeckSelection);
        viewStudy = v.findViewById(R.id.viewStudyEngine);
        tvDeckTitle = v.findViewById(R.id.tvDeckSessionTitle);
        tvCardText = v.findViewById(R.id.tvCardContent);

        RecyclerView rv = v.findViewById(R.id.rvDecks);
        FloatingActionButton fab = v.findViewById(R.id.fabAddDeckCard);
        Button btnNext = v.findViewById(R.id.btnNextCard);
        v.findViewById(R.id.btnStudyBack).setOnClickListener(view -> closeStudySession());

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeckAdapter();
        rv.setAdapter(adapter);

        fab.setOnClickListener(view -> displayCreationDialog());

        v.findViewById(R.id.cardWorkspace).setOnClickListener(view -> {
            if (!currentSessionCards.isEmpty()) {
                if (showingQuestion) {
                    tvCardText.setText(currentSessionCards.get(cardIndex).answer);
                    tvCardText.setTextColor(android.graphics.Color.parseColor("#81B29A")); // Green indicator flip
                } else {
                    tvCardText.setText(currentSessionCards.get(cardIndex).question);
                    tvCardText.setTextColor(android.graphics.Color.parseColor("#3D405B"));
                }
                showingQuestion = !showingQuestion;
            }
        });

        btnNext.setOnClickListener(view -> {
            if (!currentSessionCards.isEmpty()) {
                cardIndex = (cardIndex + 1) % currentSessionCards.size();
                presentCardState();
            }
        });

        loadDecks();
        return v;
    }

    private void loadDecks() {
        deckList.clear();
        deckList.addAll(db.appDao().getUniqueDecks());
        adapter.notifyDataSetChanged();
    }

    private void initiateStudySession(String deckName) {
        currentSessionCards.clear();
        currentSessionCards.addAll(db.appDao().getCardsFromDeck(deckName));
        if(currentSessionCards.isEmpty()) return;

        cardIndex = 0;
        tvDeckTitle.setText(deckName.toUpperCase());
        viewDecks.setVisibility(View.GONE);
        viewStudy.setVisibility(View.VISIBLE);
        presentCardState();
    }

    private void presentCardState() {
        showingQuestion = true;
        tvCardText.setText(currentSessionCards.get(cardIndex).question);
        tvCardText.setTextColor(android.graphics.Color.parseColor("#3D405B"));
    }

    private void closeStudySession() {
        viewStudy.setVisibility(View.GONE);
        viewDecks.setVisibility(View.VISIBLE);
        loadDecks();
    }

    private void displayCreationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("New Flashcard Object");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etDeck = new EditText(getContext()); etDeck.setHint("Deck Subject Tag..."); layout.addView(etDeck);
        final EditText etQ = new EditText(getContext()); etQ.setHint("Question text..."); layout.addView(etQ);
        final EditText etA = new EditText(getContext()); etA.setHint("Answer field..."); layout.addView(etA);
        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String d = etDeck.getText().toString().trim();
            String q = etQ.getText().toString().trim();
            String a = etA.getText().toString().trim();
            if(!d.isEmpty() && !q.isEmpty() && !a.isEmpty()){
                FlashcardItem item = new FlashcardItem();
                item.deckName = d;
                item.question = q;
                item.answer = a;
                db.appDao().insertCard(item);
                loadDecks();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {
        @NonNull @Override
        public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck_row, parent, false);
            return new DeckViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull DeckViewHolder holder, int pos) {
            String current = deckList.get(pos);
            holder.tvName.setText(current);
            holder.itemView.setOnClickListener(v -> initiateStudySession(current));
        }
        @Override public int getItemCount() { return deckList.size(); }

        class DeckViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            public DeckViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvDeckNameDisplay);
            }
        }
    }
}