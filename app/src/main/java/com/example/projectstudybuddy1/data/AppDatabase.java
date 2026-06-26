package com.example.projectstudybuddy1.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;
import java.util.List;

// Added SubTaskItem.class to the database entity array list registry
@Database(entities = {TaskItem.class, JournalEntry.class, FlashcardItem.class, SubTaskItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "studybuddy_db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration() // Wipes data safely if schema mismatch happens
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Dao
    public interface AppDao {
        // Task operations
        @Query("SELECT * FROM tasks WHERE isRoutine = 0")
        List<TaskItem> getAllTodos();

        @Query("SELECT * FROM tasks WHERE isRoutine = 1")
        List<TaskItem> getAllRoutines();

        // Modified task inserts to return 'long' so we get the auto-generated row id for nested mapping
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insertTask(TaskItem item);

        @Update
        void updateTask(TaskItem item);

        @Delete
        void deleteTask(TaskItem item);

        // Journal operations
        @Query("SELECT * FROM journal ORDER BY id DESC")
        List<JournalEntry> getAllJournalEntries();

        @Insert
        void insertJournal(JournalEntry entry);

        @Update
        void updateJournal(JournalEntry entry);

        @Delete
        void deleteJournal(JournalEntry entry);

        // Flashcard operations
        @Query("SELECT DISTINCT deckName FROM flashcards")
        List<String> getUniqueDecks();

        @Query("SELECT * FROM flashcards WHERE deckName = :deck")
        List<FlashcardItem> getCardsFromDeck(String deck);

        @Insert
        void insertCard(FlashcardItem card);

        @Delete
        void deleteCard(FlashcardItem card);

        // ==========================================
        // NEW GOOGLE KEEP SUB-TASK WORKSPACE HANDLERS
        // ==========================================
        @Query("SELECT * FROM tasks ORDER BY taskId DESC")
        List<TaskItem> getAllTasks();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertSubTask(SubTaskItem subTask);

        @Delete
        void deleteSubTask(SubTaskItem subTask);

        @Query("SELECT * FROM sub_tasks WHERE parentTaskId = :parentId")
        List<SubTaskItem> getSubTasksForParent(int parentId);

        @Query("SELECT COUNT(*) FROM sub_tasks")
        int getTotalSubTaskCount();

        @Query("SELECT COUNT(*) FROM sub_tasks WHERE isChecked = 1")
        int getCheckedSubTaskCount();
    }
}