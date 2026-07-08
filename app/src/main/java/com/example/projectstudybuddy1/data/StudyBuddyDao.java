package com.example.projectstudybuddy1.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface StudyBuddyDao {

    // ==========================================================
    // LEGACY TODO OPERATIONS (Entity: TodoEntity)
    // ==========================================================
    @Query("SELECT * FROM todo_list WHERE userId = :userId ORDER BY createdAt DESC")
    List<TodoEntity> getAllTasksForUser(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLegacyTodo(TodoEntity todo);

    @Update
    void updateLegacyTodo(TodoEntity todo);

    @Delete
    void deleteLegacyTodo(TodoEntity todo);

    // ==========================================================
    // PRIMARY TASK & WORKSPACE SYSTEM (Entity: TaskItem)
    // ==========================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(TaskItem task);

    @Update
    void updateTask(TaskItem task);

    @Delete
    void deleteTask(TaskItem task);

    // FIXED: Combined query names to perfectly match your Fragment expectations
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY taskId DESC")
    List<TaskItem> getAllTasks(int userId);

    // FIXED: Added missing tracking lookup needed by your FlashcardStudyActivity
    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    TaskItem getTaskById(int taskId);

    @Query("SELECT * FROM tasks WHERE title LIKE 'JOURNAL_NOTE:%' AND userId = :userId ORDER BY taskId DESC")
    List<TaskItem> getJournalsForUser(int userId);

    // Legacy fallback lookup
    @Query("SELECT * FROM tasks ORDER BY taskId DESC")
    List<TaskItem> getAllTasksFallback();

    // ==========================================================
    // NESTED SUB-TASK WORKSPACES (Entity: SubTaskItem)
    // ==========================================================
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

    // ==========================================================
    // FLASHCARD CARD ENGINE WORKSPACES (Entity: FlashcardItem)
    // ==========================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCard(FlashcardItem card);

    @Delete
    void deleteCard(FlashcardItem card);

    @Query("SELECT * FROM flashcard_cards WHERE parentDeckId = :deckId ORDER BY cardId ASC")
    List<FlashcardItem> getCardsForDeck(int deckId);
}