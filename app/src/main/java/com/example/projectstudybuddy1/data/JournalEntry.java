package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "journal")
public class JournalEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String content;
    public String timestamp;
}