package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public boolean isCompleted;
    public boolean isRoutine; // false = todo list, true = routine list
}