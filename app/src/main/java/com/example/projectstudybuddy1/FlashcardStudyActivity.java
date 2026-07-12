package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.FlashcardItem;
import com.example.projectstudybuddy1.data.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class FlashcardStudyActivity extends AppCompatActivity {

    private AppDatabase db;
    private int targetDeckId;
    private TaskItem runningParentDeckTask;
    private final List<FlashcardItem> currentDeckCards = new ArrayList<>();
    private int activeCardIndex = 0;
    private boolean showingAnswerState = false;
    private boolean activeEditorMode = false;

    // View Components
    private TextView tvHeaderTitle, tvDisplayContent, tvBadgeIndex;
    private EditText etInputQuestion, etInputAnswer, etDeckTitleEdit;
    private LinearLayout llEditorPanel;
    private ImageButton btnToggleEdit, btnPrev, btnNext;
    private CardView cvSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_study);

        db = AppDatabase.getDatabase(this);
        targetDeckId = getIntent().getIntExtra("DECK_ID", -1);

        tvHeaderTitle = findViewById(R.id.tvDeckNameHeader);
        etDeckTitleEdit = findViewById(R.id.etDeckNameEdit);
        tvDisplayContent = findViewById(R.id.tvFlashcardDisplayContent);
        tvBadgeIndex = findViewById(R.id.tvCardIndexBadge);
        etInputQuestion = findViewById(R.id.etEditQuestion);
        etInputAnswer = findViewById(R.id.etEditAnswer);
        llEditorPanel = findViewById(R.id.llEditorWorkspacePanel);
        btnToggleEdit = findViewById(R.id.btnEditFlashcard);

        // Initializing navigation pointers
        btnPrev = findViewById(R.id.btnPreviousCard);
        btnNext = findViewById(R.id.btnNextCard);

        ImageButton btnDelete = findViewById(R.id.btnDeleteFlashcard);
        FloatingActionButton fabAddCard = findViewById(R.id.fabAddNewFlashcard);
        cvSurface = findViewById(R.id.cvFlashcardSurface);

        // Fetch primary deck parameters directly from Room DB data tables
        runningParentDeckTask = db.appDao().getTaskById(targetDeckId);
        if (runningParentDeckTask != null && runningParentDeckTask.title != null) {
            String cleanTitle = runningParentDeckTask.title.replace("DECK_NOTE:", "").trim();
            tvHeaderTitle.setText(cleanTitle);
            etDeckTitleEdit.setText(cleanTitle);
        }

        findViewById(R.id.btnFlashcardBack).setOnClickListener(v -> finish());

        cvSurface.setOnClickListener(v -> handleCardFlipAction());
        btnToggleEdit.setOnClickListener(v -> toggleWorkspaceMode());
        btnPrev.setOnClickListener(v -> navigateDeckSequence(-1));
        btnNext.setOnClickListener(v -> navigateDeckSequence(1));
        fabAddCard.setOnClickListener(v -> appendNewCardInstance());

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> triggerCardDeletionConfirmation());
        }

        loadFlashcardCollection();
    }

    private void loadFlashcardCollection() {
        currentDeckCards.clear();
        currentDeckCards.addAll(db.appDao().getCardsForDeck(targetDeckId));

        if (currentDeckCards.isEmpty()) {
            FlashcardItem placeholder = new FlashcardItem();
            placeholder.parentDeckId = targetDeckId;
            placeholder.question = "Welcome to your new Deck!";
            placeholder.answer = "Tap Edit above to add card content.";
            db.appDao().insertCard(placeholder);
            currentDeckCards.addAll(db.appDao().getCardsForDeck(targetDeckId));
        }

        activeCardIndex = 0;
        renderActiveCardState();
    }

    private void renderActiveCardState() {
        if (currentDeckCards.isEmpty()) return;
        if (activeCardIndex >= currentDeckCards.size()) {
            activeCardIndex = currentDeckCards.size() - 1;
        }

        FlashcardItem activeItem = currentDeckCards.get(activeCardIndex);
        tvBadgeIndex.setText(String.valueOf(activeCardIndex + 1));
        showingAnswerState = false;

        cvSurface.setCardBackgroundColor(android.graphics.Color.WHITE);

        if (activeEditorMode) {
            tvDisplayContent.setVisibility(View.GONE);
            llEditorPanel.setVisibility(View.VISIBLE);

            // Swap display states for title fields in toolbar layer
            tvHeaderTitle.setVisibility(View.GONE);
            etDeckTitleEdit.setVisibility(View.VISIBLE);

            etInputQuestion.setText(activeItem.question);
            etInputAnswer.setText(activeItem.answer);
        } else {
            llEditorPanel.setVisibility(View.GONE);
            tvDisplayContent.setVisibility(View.VISIBLE);

            // Revert workspace back to primary reading mode settings
            etDeckTitleEdit.setVisibility(View.GONE);
            tvHeaderTitle.setVisibility(View.VISIBLE);

            tvDisplayContent.setText(activeItem.question);
        }

        // Trigger dynamic gray-out verification state whenever a card is bound
        updateNavigationArrowStates();
    }

    // FIXED: Added dynamic boundary evaluation to control button activation and alpha opacity
    private void updateNavigationArrowStates() {
        if (btnPrev == null || btnNext == null) return;

        int totalCardsCount = currentDeckCards.size();

        // 1. EVALUATE PREVIOUS (LEFT) ARROW
        if (activeCardIndex <= 0 || totalCardsCount == 0 || activeEditorMode) {
            btnPrev.setEnabled(false);
            btnPrev.setAlpha(0.3f); // 30% Opacity grayed-out look
            btnPrev.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        } else {
            btnPrev.setEnabled(true);
            btnPrev.setAlpha(1.0f); // Fully solid active look
            btnPrev.setImageTintList(ColorStateList.valueOf(Color.parseColor("#81B29A")));
        }

        // 2. EVALUATE NEXT (RIGHT) ARROW
        if (activeCardIndex >= totalCardsCount - 1 || totalCardsCount == 0 || activeEditorMode) {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.3f); // 30% Opacity grayed-out look
            btnNext.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        } else {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f); // Fully solid active look
            btnNext.setImageTintList(ColorStateList.valueOf(Color.parseColor("#81B29A")));
        }
    }

    private void handleCardFlipAction() {
        if (activeEditorMode || currentDeckCards.isEmpty()) return;

        FlashcardItem activeItem = currentDeckCards.get(activeCardIndex);
        showingAnswerState = !showingAnswerState;

        if (showingAnswerState) {
            tvDisplayContent.setText(activeItem.answer);
            cvSurface.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFDF6"));
        } else {
            tvDisplayContent.setText(activeItem.question);
            cvSurface.setCardBackgroundColor(android.graphics.Color.WHITE);
        }
    }

    private void toggleWorkspaceMode() {
        if (currentDeckCards.isEmpty()) return;

        FlashcardItem currentCard = currentDeckCards.get(activeCardIndex);

        if (activeEditorMode) {
            String qInput = etInputQuestion.getText().toString().trim();
            String aInput = etInputAnswer.getText().toString().trim();
            String updatedDeckTitle = etDeckTitleEdit.getText().toString().trim();

            if (qInput.isEmpty() || aInput.isEmpty() || updatedDeckTitle.isEmpty()) {
                Toast.makeText(this, "Fields cannot be blank!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save individual Flashcard text entries
            currentCard.question = qInput;
            currentCard.answer = aInput;
            db.appDao().insertCard(currentCard);

            // Save parent Deck structural title modifications
            if (runningParentDeckTask != null) {
                runningParentDeckTask.title = "DECK_NOTE: " + updatedDeckTitle;
                db.appDao().updateTask(runningParentDeckTask);
                tvHeaderTitle.setText(updatedDeckTitle);
            }

            btnToggleEdit.setImageResource(android.R.drawable.ic_menu_edit);
            activeEditorMode = false;
            Toast.makeText(this, "Changes Saved!", Toast.LENGTH_SHORT).show();
        } else {
            btnToggleEdit.setImageResource(android.R.drawable.checkbox_on_background);
            activeEditorMode = true;
        }
        renderActiveCardState();
    }

    private void navigateDeckSequence(int stepDirection) {
        if (activeEditorMode) {
            Toast.makeText(this, "Save changes before turning pages!", Toast.LENGTH_SHORT).show();
            return;
        }

        int targetIndex = activeCardIndex + stepDirection;
        if (targetIndex >= 0 && targetIndex < currentDeckCards.size()) {
            activeCardIndex = targetIndex;
            renderActiveCardState();
        }
    }

    private void appendNewCardInstance() {
        if (activeEditorMode) {
            Toast.makeText(this, "Save changes first!", Toast.LENGTH_SHORT).show();
            return;
        }

        FlashcardItem freshItem = new FlashcardItem();
        freshItem.parentDeckId = targetDeckId;
        freshItem.question = "New Question";
        freshItem.answer = "New Answer";

        db.appDao().insertCard(freshItem);

        currentDeckCards.clear();
        currentDeckCards.addAll(db.appDao().getCardsForDeck(targetDeckId));

        activeCardIndex = currentDeckCards.size() - 1;
        activeEditorMode = true;
        btnToggleEdit.setImageResource(android.R.drawable.checkbox_on_background);
        renderActiveCardState();
    }

    private void triggerCardDeletionConfirmation() {
        if (currentDeckCards.isEmpty()) return;

        // INTERCEPT TRIGGER: Guard rail rule to prevent zero-card deck states
        if (currentDeckCards.size() == 1) {
            new AlertDialog.Builder(this)
                    .setTitle("Cannot Delete Card")
                    .setMessage("Every flashcard deck must have at least one card left to study.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        FlashcardItem itemToDelete = currentDeckCards.get(activeCardIndex);

        new AlertDialog.Builder(this)
                .setTitle("Delete Flashcard")
                .setMessage("Are you sure you want to delete this card?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.appDao().deleteCard(itemToDelete);
                    Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show();

                    currentDeckCards.clear();
                    currentDeckCards.addAll(db.appDao().getCardsForDeck(targetDeckId));

                    if (activeCardIndex >= currentDeckCards.size() && activeCardIndex > 0) {
                        activeCardIndex--;
                    }

                    renderActiveCardState();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}