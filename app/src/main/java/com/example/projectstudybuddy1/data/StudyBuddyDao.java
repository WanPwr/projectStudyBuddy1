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

    @Query("SELECT * FROM todo_list WHERE userId = :userId ORDER BY createdAt DESC")
    List<TodoEntity> getAllTasksForUser(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTask(TodoEntity todo);

    @Update
    void updateTask(TodoEntity todo);

    @Delete
    void deleteTask(TodoEntity todo);
}