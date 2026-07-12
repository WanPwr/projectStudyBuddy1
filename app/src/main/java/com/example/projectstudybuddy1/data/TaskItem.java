package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskItem {
    @PrimaryKey(autoGenerate = true)
    public int taskId;

    public String title;
    public String dateCreated;
    public boolean isPinned = false;

    // Unifies your older queries and resolves the compilation symbol errors:
    public boolean isRoutine = false;
    public boolean completed = false;

    // FIX: Links tasks and journals to a specific user to stop room compilation errors and crashes
    public int userId;
}