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

@Database(entities = {TaskItem.class, FlashcardItem.class, SubTaskItem.class}, version = 3, exportSchema = false)
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
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Dao
    public interface AppDao {
        // --- Core Task & Journal Operations (User Filtered) ---
        @Query("SELECT * FROM tasks WHERE isRoutine = 0 AND userId = :userId AND title NOT LIKE 'JOURNAL_NOTE:%' AND title NOT LIKE 'DECK_NOTE:%'")
        List<TaskItem> getAllTodos(int userId);

        @Query("SELECT * FROM tasks WHERE isRoutine = 1 AND userId = :userId")
        List<TaskItem> getAllRoutines(int userId);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insertTask(TaskItem item);

        @Update
        void updateTask(TaskItem item);

        @Delete
        void deleteTask(TaskItem item);

        @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY taskId DESC")
        List<TaskItem> getAllTasks(int userId);

        @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
        TaskItem getTaskById(int taskId);

        // FIXED: Added wildcard query search feature into the correct active runtime AppDao definition
        @Query("SELECT * FROM tasks WHERE userId = :userId AND title LIKE '%' || :searchQuery || '%' ORDER BY taskId DESC")
        List<TaskItem> searchTasksByQuery(int userId, String searchQuery);

        // --- Keep-Style Checklist Operations ---
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

        // --- Flashcard Operations ---
        @Query("SELECT * FROM flashcard_cards")
        List<FlashcardItem> getUniqueDecks();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertCard(FlashcardItem card);

        @Delete
        void deleteCard(FlashcardItem card);

        @Query("SELECT * FROM flashcard_cards WHERE parentDeckId = :deckId ORDER BY cardId ASC")
        List<FlashcardItem> getCardsForDeck(int deckId);
    }
}