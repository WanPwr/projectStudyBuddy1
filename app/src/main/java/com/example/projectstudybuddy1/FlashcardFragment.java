package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private final List<String> deckList = new ArrayList<>();

    private View viewDecks, viewStudy;
    private TextView tvCardStaticText;
    private EditText etDeckTitle, etQuestionInput, etAnswerInput;
    private LinearLayout layoutEditFields;
    private ImageButton btnEditMode, btnNext;
    private ImageView ivCheckedBadge;

    private final List<FlashcardItem> currentSessionCards = new ArrayList<>();
    private int cardIndex = 0;
    private boolean showingQuestion = true;
    private boolean isEditWorkspaceActive = false;
    private String currentSelectedDeckName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_flashcard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        // Bind layout structures
        viewDecks = v.findViewById(R.id.viewDeckSelection);
        viewStudy = v.findViewById(R.id.viewStudyEngine);
        tvCardStaticText = v.findViewById(R.id.tvCardContent);
        etDeckTitle = v.findViewById(R.id.etFigmaDeckTitle);

        layoutEditFields = v.findViewById(R.id.layoutEditFields);
        etQuestionInput = v.findViewById(R.id.etFigmaQuestionInput);
        etAnswerInput = v.findViewById(R.id.etFigmaAnswerInput);

        btnEditMode = v.findViewById(R.id.btnEditModeToggle);
        ivCheckedBadge = v.findViewById(R.id.ivCheckStatusBadge);

        RecyclerView rv = v.findViewById(R.id.rvDecks);
        FloatingActionButton fab = v.findViewById(R.id.fabAddDeckCard);
        ImageButton btnPrev = v.findViewById(R.id.btnFigmaPrevCard);
        btnNext = v.findViewById(R.id.btnFigmaNextCard);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeckAdapter();
        rv.setAdapter(adapter);

        // Core directory interactions
        v.findViewById(R.id.btnStudyBack).setOnClickListener(view -> closeStudySession());
        fab.setOnClickListener(view -> createNewBlankFigmaDeck());

        // STUDY STATE: Smooth interactive Tap-to-flip controller engine
        v.findViewById(R.id.cardWorkspace).setOnClickListener(view -> {
            if (!currentSessionCards.isEmpty() && !isEditWorkspaceActive) {
                if (showingQuestion) {
                    // Reveal Answer card back
                    tvCardStaticText.setText(currentSessionCards.get(cardIndex).answer);
                    tvCardStaticText.setTextColor(android.graphics.Color.parseColor("#81B29A")); // Mint Green
                } else {
                    // Reveal Question card front
                    tvCardStaticText.setText(currentSessionCards.get(cardIndex).question);
                    tvCardStaticText.setTextColor(android.graphics.Color.parseColor("#3D405B")); // Dark Navy
                }
                showingQuestion = !showingQuestion;
            }
        });

        // LEFT ARROW: Loop backward smoothly
        btnPrev.setOnClickListener(view -> {
            if (!currentSessionCards.isEmpty()) {
                saveCurrentCardStateIfEditing();
                cardIndex = (cardIndex - 1 + currentSessionCards.size()) % currentSessionCards.size();
                presentCardState();
            }
        });

        // RIGHT ACTION BUTTON: Next card index navigation or placeholder generation at the end
        btnNext.setOnClickListener(view -> {
            if (!currentSessionCards.isEmpty()) {
                // Check if user hits the right button while on the LAST card item
                if (cardIndex == currentSessionCards.size() - 1) {
                    saveCurrentCardStateIfEditing();

                    // Generate new placeholder row item directly into Room Database
                    FlashcardItem newTemplateCard = new FlashcardItem();
                    newTemplateCard.deckName = currentSelectedDeckName;
                    newTemplateCard.question = "QUESTION";
                    newTemplateCard.answer = "ANSWER";
                    db.appDao().insertCard(newTemplateCard);

                    // Refresh localized device stack lists
                    currentSessionCards.clear();
                    currentSessionCards.addAll(db.appDao().getCardsFromDeck(currentSelectedDeckName));
                    cardIndex = currentSessionCards.size() - 1;

                    // Drop layout straight forward into Edit Mode
                    isEditWorkspaceActive = true;
                    btnEditMode.setVisibility(View.GONE);
                    ivCheckedBadge.setVisibility(View.VISIBLE);
                    etDeckTitle.setEnabled(true);

                    presentCardState();

                    // Initialize clean checkmark saving listener closure routine
                    ivCheckedBadge.setOnClickListener(vBadge -> {
                        saveCurrentCardStateIfEditing();
                        isEditWorkspaceActive = false;
                        ivCheckedBadge.setVisibility(View.GONE);
                        btnEditMode.setVisibility(View.VISIBLE);
                        etDeckTitle.setEnabled(false);
                        initiateStudySession(currentSelectedDeckName);
                    });
                } else {
                    // Standard navigation behavior: jump forward 1 element frame index
                    saveCurrentCardStateIfEditing();
                    cardIndex = (cardIndex + 1) % currentSessionCards.size();
                    presentCardState();
                }
            }
        });

        btnEditMode.setOnClickListener(view -> toggleFigmaWorkspaceMode());

        loadDecks();
        return v;
    }

    private void loadDecks() {
        deckList.clear();
        deckList.addAll(db.appDao().getUniqueDecks());
        adapter.notifyDataSetChanged();
    }

    private void initiateStudySession(String deckName) {
        currentSelectedDeckName = deckName;
        currentSessionCards.clear();
        currentSessionCards.addAll(db.appDao().getCardsFromDeck(deckName));
        if (currentSessionCards.isEmpty()) return;

        cardIndex = 0;
        viewDecks.setVisibility(View.GONE);
        viewStudy.setVisibility(View.VISIBLE);

        isEditWorkspaceActive = false;
        btnEditMode.setVisibility(View.VISIBLE);
        ivCheckedBadge.setVisibility(View.GONE);
        etDeckTitle.setEnabled(false);

        presentCardState();
    }

    private void presentCardState() {
        showingQuestion = true;
        FlashcardItem item = currentSessionCards.get(cardIndex);
        etDeckTitle.setText(item.deckName.toUpperCase());

        // DYNAMIC ICON FLIP: Check if this button handles next navigation or generation
        if (cardIndex == currentSessionCards.size() - 1) {
            btnNext.setImageResource(android.R.drawable.ic_input_add); // '+' sign
            btnNext.setColorFilter(android.graphics.Color.parseColor("#81B29A")); // Mint Green
        } else {
            btnNext.setImageResource(android.R.drawable.ic_media_next); // Arrow ➔
            btnNext.setColorFilter(android.graphics.Color.parseColor("#7D5A44")); // Earth Brown
        }

        if (isEditWorkspaceActive) {
            tvCardStaticText.setVisibility(View.GONE);
            layoutEditFields.setVisibility(View.VISIBLE);

            // Wipe standard placeholder tracking strings so user views neat clean inputs
            etQuestionInput.setText(item.question.equals("QUESTION") ? "" : item.question);
            etAnswerInput.setText(item.answer.equals("ANSWER") ? "" : item.answer);
            etQuestionInput.requestFocus();
        } else {
            layoutEditFields.setVisibility(View.GONE);
            tvCardStaticText.setVisibility(View.VISIBLE);
            tvCardStaticText.setText(item.question);
            tvCardStaticText.setTextColor(android.graphics.Color.parseColor("#3D405B"));
        }
    }

    private void toggleFigmaWorkspaceMode() {
        if (!isEditWorkspaceActive) {
            isEditWorkspaceActive = true;
            btnEditMode.setVisibility(View.GONE);
            ivCheckedBadge.setVisibility(View.VISIBLE);
            etDeckTitle.setEnabled(true);

            ivCheckedBadge.setOnClickListener(v -> {
                saveCurrentCardStateIfEditing();
                isEditWorkspaceActive = false;
                ivCheckedBadge.setVisibility(View.GONE);
                btnEditMode.setVisibility(View.VISIBLE);
                etDeckTitle.setEnabled(false);
                initiateStudySession(currentSelectedDeckName);
            });
            presentCardState();
        }
    }

    private void saveCurrentCardStateIfEditing() {
        if (isEditWorkspaceActive && !currentSessionCards.isEmpty()) {
            FlashcardItem card = currentSessionCards.get(cardIndex);

            String qInput = etQuestionInput.getText().toString().trim();
            String aInput = etAnswerInput.getText().toString().trim();
            String updatedDeckTitleName = etDeckTitle.getText().toString().trim();

            if (!updatedDeckTitleName.isEmpty()) {
                card.deckName = updatedDeckTitleName;
                currentSelectedDeckName = updatedDeckTitleName;
            }

            card.question = qInput.isEmpty() ? "QUESTION" : qInput;
            card.answer = aInput.isEmpty() ? "ANSWER" : aInput;

            // Clear baseline collision vectors
            db.appDao().deleteCard(card);
            db.appDao().insertCard(card);
        }
    }

    private void createNewBlankFigmaDeck() {
        FlashcardItem templateItem = new FlashcardItem();
        templateItem.deckName = "Blank";
        templateItem.question = "QUESTION";
        templateItem.answer = "ANSWER";
        db.appDao().insertCard(templateItem);
        loadDecks();
    }

    private void closeStudySession() {
        saveCurrentCardStateIfEditing();
        viewStudy.setVisibility(View.GONE);
        viewDecks.setVisibility(View.VISIBLE);
        loadDecks();
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
            holder.tvName.setText(current.toUpperCase());
            holder.itemView.setOnClickListener(v -> initiateStudySession(current));

            holder.tvMinus.setOnClickListener(v -> {
                List<FlashcardItem> cardsToDelete = db.appDao().getCardsFromDeck(current);
                for (FlashcardItem item : cardsToDelete) {
                    db.appDao().deleteCard(item);
                }
                loadDecks();
            });
        }
        @Override public int getItemCount() { return deckList.size(); }

        class DeckViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvMinus;
            public DeckViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvDeckNameDisplay);
                tvMinus = itemView.findViewById(R.id.tvFigmaMinus);
            }
        }
    }
}