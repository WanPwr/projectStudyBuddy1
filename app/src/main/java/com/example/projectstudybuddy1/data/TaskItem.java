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
    // ==========================================
    // NEW FIELDS FOR ROUTINES
    // ==========================================

    // Stores the selected days like "mo,we,fr"
    public String daysToRepeat;

    // Tracks if the reminder toggle is on
    public boolean hasReminder;

    // Stores either "Alarm" or "Push"
    public String reminderType;

    // Stores the military time for the reminder (e.g., 20 for 8 PM)
    public int reminderHour;
    public int reminderMinute;
}