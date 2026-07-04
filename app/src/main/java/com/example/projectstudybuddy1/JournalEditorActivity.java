package com.example.projectstudybuddy1;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_editor);
        db = AppDatabase.getDatabase(this);

        // Bind layout views
        etTitle = findViewById(R.id.etJournalEntryTitle);
        etBody = findViewById(R.id.etJournalEntryBody);
        ImageButton btnBack = findViewById(R.id.btnJournalEditorBack);
        FloatingActionButton fabSave = findViewById(R.id.fabSaveJournalEntry);

        btnBack.setOnClickListener(v -> finish());
        fabSave.setOnClickListener(v -> saveJournalWorkspaceData());

        // Check if loading an existing entry or creating a new one
        int existingId = getIntent().getIntExtra("JOURNAL_ID", -1);
        if (existingId != -1) {
            isEditMode = true;
            new Thread(() -> {
                List<TaskItem> entries = db.appDao().getAllTasks();
                for (TaskItem item : entries) {
                    if (item.taskId == existingId) {
                        activeEntry = item;
                        runOnUiThread(() -> {
                            // Clean display: Strip out the internal hidden tracking prefix when showing it to the user
                            if (activeEntry.title != null) {
                                String cleanTitle = activeEntry.title
                                        .replace("Journal: ", "")
                                        .replaceAll("^Journal\\s\\d+$", "");
                                etTitle.setText(cleanTitle.trim());
                            }
                        });
                        break;
                    }
                }
            }).start();
        } else {
            activeEntry = new TaskItem();
        }
    }

    private void saveJournalWorkspaceData() {
        String titleInput = etTitle.getText().toString().trim();
        String finalTitle;

        if (titleInput.isEmpty()) {
            // AUTO-GENERATION: Count how many journals exist to name it "Journal X"
            int currentJournalCount = 1;
            List<TaskItem> allItems = db.appDao().getAllTasks();
            for (TaskItem item : allItems) {
                if (item.title != null && (item.title.startsWith("Journal ") || item.title.startsWith("Journal:"))) {
                    currentJournalCount++;
                }
            }
            finalTitle = "Journal " + currentJournalCount;
        } else {
            // ENFORCED ISOLATION PREFIX: Force it to start with "Journal: " so TaskManagerFragment hides it!
            if (!titleInput.startsWith("Journal ") && !titleInput.startsWith("Notetaking ")) {
                finalTitle = "Journal: " + titleInput;
            } else {
                finalTitle = titleInput;
            }
        }

        activeEntry.title = finalTitle;

        // Match your database model defaults
        activeEntry.isRoutine = false;
        activeEntry.completed = false;

        // Background save thread execution
        new Thread(() -> {
            if (!isEditMode) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
                activeEntry.dateCreated = sdf.format(new Date());
                db.appDao().insertTask(activeEntry);
            } else {
                db.appDao().updateTask(activeEntry);
            }

            // Close editor screen immediately after the Room database transaction completes
            runOnUiThread(this::finish);
        }).start();
    }
}