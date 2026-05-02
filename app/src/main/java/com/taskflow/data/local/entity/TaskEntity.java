package com.taskflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "tasks",
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = ProjectEntity.class, parentColumns = "id", childColumns = "projectId", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = SectionEntity.class, parentColumns = "id", childColumns = "sectionId", onDelete = ForeignKey.SET_NULL)
        },
        indices = {
                @Index("userId"),
                @Index("projectId"),
                @Index("sectionId"),
                @Index("dueDate"),
                @Index("isCompleted")
        }
)
public class TaskEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title = "";

    public String description;
    public boolean isCompleted;
    public Long dueDate;
    public Long reminderDate;
    public long userId;
    public Long projectId;
    public Long sectionId;
    public int priority;
    public boolean isStarred;
    public long createdAt;
    public long updatedAt;

    public TaskEntity() {
    }

    @Ignore
    public TaskEntity(@NonNull String title, String description, long userId, Long dueDate, Long reminderDate) {
        long now = System.currentTimeMillis();
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.dueDate = dueDate;
        this.reminderDate = reminderDate;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
