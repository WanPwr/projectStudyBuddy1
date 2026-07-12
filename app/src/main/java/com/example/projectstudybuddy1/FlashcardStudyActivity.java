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

        btnPrev = findViewById(R.id.btnPreviousCard);
        btnNext = findViewById(R.id.btnNextCard);

        ImageButton btnDelete = findViewById(R.id.btnDeleteFlashcard);
        FloatingActionButton fabAddCard = findViewById(R.id.fabAddNewFlashcard);
        cvSurface = findViewById(R.id.cvFlashcardSurface);

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
            placeholder.answer = "Tap the edit icon above to edit card content.";
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

        cvSurface.setCardBackgroundColor(Color.WHITE);

        // FIXED: Force-breaks the asset cache lock so the view engine must redraw it
        btnToggleEdit.setImageDrawable(null);

        if (activeEditorMode) {
            // --- EDIT MODE ACTIVE ---
            tvDisplayContent.setVisibility(View.GONE);
            llEditorPanel.setVisibility(View.VISIBLE);

            tvHeaderTitle.setVisibility(View.GONE);
            etDeckTitleEdit.setVisibility(View.VISIBLE);

            etInputQuestion.setText(activeItem.question);
            etInputAnswer.setText(activeItem.answer);

            // Render floppy disk icon to represent save action availability
            btnToggleEdit.setImageResource(android.R.drawable.ic_menu_save);
        } else {
            // --- READ/STUDY MODE ACTIVE ---
            llEditorPanel.setVisibility(View.GONE);
            tvDisplayContent.setVisibility(View.VISIBLE);

            etDeckTitleEdit.setVisibility(View.GONE);
            tvHeaderTitle.setVisibility(View.VISIBLE);

            tvDisplayContent.setText(activeItem.question);

            // FIXED: Explicitly force the system pencil asset to draw on the toolbar layout frame
            btnToggleEdit.setImageResource(android.R.drawable.ic_menu_edit);
        }

        updateNavigationArrowStates();
    }

    private void updateNavigationArrowStates() {
        if (btnPrev == null || btnNext == null) return;

        int totalCardsCount = currentDeckCards.size();

        if (activeCardIndex <= 0 || totalCardsCount == 0 || activeEditorMode) {
            btnPrev.setEnabled(false);
            btnPrev.setAlpha(0.3f);
            btnPrev.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        } else {
            btnPrev.setEnabled(true);
            btnPrev.setAlpha(1.0f);
            btnPrev.setImageTintList(ColorStateList.valueOf(Color.parseColor("#81B29A")));
        }

        if (activeCardIndex >= totalCardsCount - 1 || totalCardsCount == 0 || activeEditorMode) {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.3f);
            btnNext.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        } else {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
            btnNext.setImageTintList(ColorStateList.valueOf(Color.parseColor("#81B29A")));
        }
    }

    private void handleCardFlipAction() {
        if (activeEditorMode || currentDeckCards.isEmpty()) return;

        FlashcardItem activeItem = currentDeckCards.get(activeCardIndex);
        showingAnswerState = !showingAnswerState;

        if (showingAnswerState) {
            tvDisplayContent.setText(activeItem.answer);
            cvSurface.setCardBackgroundColor(Color.parseColor("#FFFDF6"));
        } else {
            tvDisplayContent.setText(activeItem.question);
            cvSurface.setCardBackgroundColor(Color.WHITE);
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

            // Save elements into database architecture
            currentCard.question = qInput;
            currentCard.answer = aInput;
            db.appDao().insertCard(currentCard);

            if (runningParentDeckTask != null) {
                runningParentDeckTask.title = "DECK_NOTE: " + updatedDeckTitle;
                db.appDao().updateTask(runningParentDeckTask);
                tvHeaderTitle.setText(updatedDeckTitle);
            }

            // Flip tracking flag off safely
            activeEditorMode = false;

            // FIXED: Immediately call state render pass to lock graphic changes down
            renderActiveCardState();

            Toast.makeText(this, "Changes Saved!", Toast.LENGTH_SHORT).show();
        } else {
            activeEditorMode = true;
            // FIXED: Force layout redraw instantly upon stepping into edit mode configuration
            renderActiveCardState();
        }
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
        renderActiveCardState();
    }

    private void triggerCardDeletionConfirmation() {
        if (currentDeckCards.isEmpty()) return;

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