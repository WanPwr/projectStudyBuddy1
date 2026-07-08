package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

                        // RESTORE SPLIT FIELDS: Decouple title versus body text input structures safely
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
        }
    }

    private void saveJournalWorkspaceData() {
        String titleInput = etTitle.getText().toString().trim();
        String bodyInput = etBody.getText().toString().trim();

        if (titleInput.isEmpty()) {
            titleInput = "Untitled Entry";
        }

        // SERIALIZE BOTH FIELDS TOGETHER: Pack title and body securely inside the entity string model
        activeEntry.title = "JOURNAL_NOTE: " + titleInput + " ||CONTENT_SEP|| " + bodyInput;
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