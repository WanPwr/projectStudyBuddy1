package com.example.projectstudybuddy1.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "sub_tasks",
        indices = {@Index(value = {"parentTaskId"})}
)
public class SubTaskItem {
    @PrimaryKey(autoGenerate = true)
    public int subTaskId;

    public int parentTaskId;
    public String subTaskText;
    public boolean isChecked = false;
}