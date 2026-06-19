package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "flashcards")
public class FlashcardItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String deckName;
    public String question;
    public String answer;
}