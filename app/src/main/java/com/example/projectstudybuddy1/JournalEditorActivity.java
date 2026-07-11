package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalEditorActivity extends AppCompatActivity {

    private AppDatabase db;
    private EditText etTitle, etBody;
    private TaskItem activeEntry;
    private boolean isEditMode = false;
    private SharedPreferences prefs;

    private String currentSelectedColorHex = "#FFFFFF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_editor);

        db = AppDatabase.getDatabase(this);
        prefs = getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        etTitle = findViewById(R.id.etJournalEntryTitle);
        etBody = findViewById(R.id.etJournalEntryBody);
        ImageButton btnBack = findViewById(R.id.btnJournalEditorBack);
        FloatingActionButton fabSave = findViewById(R.id.fabSaveJournalEntry);

        btnBack.setOnClickListener(v -> finish());
        fabSave.setOnClickListener(v -> saveJournalWorkspaceData());

        setupColorClickListeners();

        int existingId = getIntent().getIntExtra("JOURNAL_ID", -1);
        int activeUserId = prefs.getInt("userId", 1);

        if (existingId != -1) {
            isEditMode = true;
            List<TaskItem> entries = db.appDao().getAllTasks(activeUserId);
            for (TaskItem item : entries) {
                if (item.taskId == existingId) {
                    activeEntry = item;
                    if (activeEntry.title != null) {
                        String rawTitleClean = activeEntry.title.replace("JOURNAL_NOTE:", "").trim();

                        if (rawTitleClean.contains("||COLOR_SEP||")) {
                            String[] colorSplit = rawTitleClean.split("\\|\\|COLOR_SEP\\|\\|");
                            if (colorSplit.length > 0) {
                                rawTitleClean = colorSplit[0].trim();
                            }
                            if (colorSplit.length > 1) {
                                currentSelectedColorHex = colorSplit[1].trim();
                                if (currentSelectedColorHex.equals("#E07A5F")) {
                                    currentSelectedColorHex = "#FDF0CD";
                                }
                                applyVisualBackgroundTint(currentSelectedColorHex);
                            }
                        }

                        if (rawTitleClean.contains("||CONTENT_SEP||")) {
                            String[] segments = rawTitleClean.split("\\|\\|CONTENT_SEP\\|\\|");
                            etTitle.setText(segments[0].trim());
                            if (segments.length > 1) {
                                etBody.setText(segments[1].trim());
                            }
                        } else {
                            etTitle.setText(rawTitleClean);
                            etBody.setText("");
                        }
                    }
                    break;
                }
            }
        }

        if (activeEntry == null) {
            isEditMode = false;
            activeEntry = new TaskItem();
            activeEntry.userId = activeUserId;
            activeEntry.isPinned = false;
            activeEntry.isRoutine = false;
            activeEntry.completed = false;
            applyVisualBackgroundTint(currentSelectedColorHex);
        }
    }

    private void setupColorClickListeners() {
        if (findViewById(R.id.dotYellow) != null) {
            findViewById(R.id.dotYellow).setOnClickListener(v -> applyVisualBackgroundTint("#FDF0CD"));
        }
        findViewById(R.id.dotBlue).setOnClickListener(v -> applyVisualBackgroundTint("#3D405B"));
        findViewById(R.id.dotRed).setOnClickListener(v -> applyVisualBackgroundTint("#E63946"));
        findViewById(R.id.dotGreen).setOnClickListener(v -> applyVisualBackgroundTint("#81B29A"));
        findViewById(R.id.dotPurple).setOnClickListener(v -> applyVisualBackgroundTint("#A06CD5"));
    }

    private void applyVisualBackgroundTint(String hexColor) {
        currentSelectedColorHex = hexColor;
        int parsedColor = Color.parseColor(hexColor);

        etTitle.setBackgroundTintList(ColorStateList.valueOf(parsedColor));
        etBody.setBackgroundTintList(ColorStateList.valueOf(parsedColor));

        // CONTRAST ENGINE: Dynamic high-contrast white toggle including Red (#E63946)
        if (hexColor.equals("#3D405B") || hexColor.equals("#A06CD5") || hexColor.equals("#81B29A") || hexColor.equals("#E63946")) {
            etTitle.setTextColor(Color.WHITE);
            etBody.setTextColor(Color.WHITE);
            etTitle.setHintTextColor(Color.parseColor("#E0E0E0"));
            etBody.setHintTextColor(Color.parseColor("#E0E0E0"));
        } else {
            etTitle.setTextColor(Color.parseColor("#3D405B"));
            etBody.setTextColor(Color.parseColor("#3D405B"));
            etTitle.setHintTextColor(Color.GRAY);
            etBody.setHintTextColor(Color.GRAY);
        }
    }

    private void saveJournalWorkspaceData() {
        String titleInput = etTitle.getText().toString().trim();
        String bodyInput = etBody.getText().toString().trim();

        if (titleInput.isEmpty()) {
            titleInput = "Untitled Entry";
        }

        activeEntry.title = "JOURNAL_NOTE: " + titleInput + " ||CONTENT_SEP|| " + bodyInput + " ||COLOR_SEP|| " + currentSelectedColorHex;
        activeEntry.isRoutine = false;
        activeEntry.completed = false;

        if (activeEntry.userId == 0) {
            activeEntry.userId = prefs.getInt("userId", 1);
        }

        try {
            if (!isEditMode) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
                activeEntry.dateCreated = sdf.format(new Date());
                db.appDao().insertTask(activeEntry);
            } else {
                db.appDao().updateTask(activeEntry);
            }

            Toast.makeText(this, "Journal saved successfully!", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}