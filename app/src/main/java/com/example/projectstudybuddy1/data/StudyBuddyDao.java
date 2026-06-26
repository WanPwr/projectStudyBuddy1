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

    // Original Core User Task Operations
    @Query("SELECT * FROM todo_list WHERE userId = :userId ORDER BY createdAt DESC")
    List<TodoEntity> getAllTasksForUser(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTask(TodoEntity todo);

    @Update
    void updateTask(TodoEntity todo);

    @Delete
    void deleteTask(TodoEntity todo);

    // ==========================================================
    // NEW TASK WORKSPACE CONNECTIONS (Google Keep-Style Subtasks)
    // ==========================================================

    // Parent Task Row Sync Engine
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(TaskItem task);

    @Update
    void updateTask(TaskItem task);

    @Query("SELECT * FROM tasks ORDER BY taskId DESC")
    List<TaskItem> getAllTasks();

    // Child Nested Sub-Task Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSubTask(SubTaskItem subTask);

    @Delete
    void deleteSubTask(SubTaskItem subTask);

    @Query("SELECT * FROM sub_tasks WHERE parentTaskId = :parentId")
    List<SubTaskItem> getSubTasksForParent(int parentId);

    // Live Progress Dashboard Metrics Queries
    @Query("SELECT COUNT(*) FROM sub_tasks")
    int getTotalSubTaskCount();

    @Query("SELECT COUNT(*) FROM sub_tasks WHERE isChecked = 1")
    int getCheckedSubTaskCount();
}