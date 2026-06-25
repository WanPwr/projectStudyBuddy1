package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

// This links every sub-task to its parent Routine using a Foreign Key
@Entity(tableName = "subtask_table",
        foreignKeys = @ForeignKey(entity = TaskItem.class,
                parentColumns = "id",
                childColumns = "parentRoutineId",
                onDelete = ForeignKey.CASCADE))
public class SubTaskItem {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // The ID of the "Weekend Routine" this belongs to
    public int parentRoutineId;

    public String title;

    public boolean isCompleted;
}