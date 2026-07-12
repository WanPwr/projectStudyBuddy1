package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "journal")
public class JournalEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String content; // or body
    public String dateCreated;

    // FIX COUPLING: Add this field to assign the journal to your logged-in user
    public int userId;
}