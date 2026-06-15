package com.studybuddy.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "todo_list")
public class TodoEntity {

    @PrimaryKey(autoGenerate = true)
    private int todoId = 0;

    @ColumnInfo(name = "userId")
    private int userId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "status")
    private String status; // "Pending" or "Done"

    @ColumnInfo(name = "createdAt")
    private String createdAt;

    // Constructor
    public TodoEntity(int userId, String title, String status, String createdAt) {
        this.userId = userId;
        this.title = title;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getTodoId() { return todoId; }
    public void setTodoId(int todoId) { this.todoId = todoId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}