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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(TaskItem task);

    @Update
    void updateTask(TaskItem task);

    @Delete
    void deleteTask(TaskItem task);

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY taskId DESC")
    List<TaskItem> getAllTasks(int userId);

    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    TaskItem getTaskById(int taskId);

    @Query("SELECT * FROM tasks ORDER BY taskId DESC")
    List<TaskItem> getAllTasksFallback();

    // FIXED: Added wildcard search string support directly within your primary data access object interface
    @Query("SELECT * FROM tasks WHERE userId = :userId AND title LIKE '%' || :searchQuery || '%' ORDER BY taskId DESC")
    List<TaskItem> searchTasksByQuery(int userId, String searchQuery);

    // ==========================================================
    // NESTED SUB-TASK WORKSPACES
    // ==========================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSubTask(SubTaskItem subTask);

    @Delete
    void deleteSubTask(SubTaskItem subTask);

    @Query("SELECT * FROM sub_tasks WHERE parentTaskId = :parentId")
    List<SubTaskItem> getSubTasksForParent(int parentId);

    // ==========================================================
    // FLASHCARD CARD ENGINE WORKSPACES
    // ==========================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCard(FlashcardItem card);

    @Delete
    void deleteCard(FlashcardItem card);

    @Query("SELECT * FROM flashcard_cards WHERE parentDeckId = :deckId ORDER BY cardId ASC")
    List<FlashcardItem> getCardsForDeck(int deckId);
}