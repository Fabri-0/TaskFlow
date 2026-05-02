package com.taskflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "subtasks",
        foreignKeys = @ForeignKey(entity = TaskEntity.class, parentColumns = "id", childColumns = "taskId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("taskId")}
)
public class SubtaskEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long taskId;

    @NonNull
    public String title = "";

    public boolean isCompleted;
    public long createdAt;

    public SubtaskEntity() {
    }

    @Ignore
    public SubtaskEntity(long taskId, @NonNull String title, boolean completed) {
        this.taskId = taskId;
        this.title = title;
        this.isCompleted = completed;
        this.createdAt = System.currentTimeMillis();
    }
}
