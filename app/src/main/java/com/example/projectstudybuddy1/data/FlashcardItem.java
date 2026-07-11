package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Explicitly register table namespace, foreign keys for cascade deletions, and index paths to prevent compilation warnings
@Entity(
        tableName = "flashcard_cards",
        foreignKeys = @ForeignKey(
                entity = TaskItem.class,
                parentColumns = "taskId",
                childColumns = "parentDeckId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = {"parentDeckId"})}
)
public class FlashcardItem {

    @PrimaryKey(autoGenerate = true)
    public int cardId;

    // This matches your AppDatabase compilation query criteria perfectly
    public int parentDeckId;

    public String question;
    public String answer;

    // Empty constructor required by Room framework components
    public FlashcardItem() {
    }
}