package com.taskflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.taskflow.utils.Constants;

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
                @Index("isCompleted"),
                @Index("isArchived"),
                @Index("isDeleted"),
                @Index("priority"),
                @Index("boardColumn")
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
    public Long reminderDate2;
    public Long reminderDate3;
    public long userId;
    public Long projectId;
    public Long sectionId;
    public int priority;
    public boolean isStarred;
    @ColumnInfo(defaultValue = "0")
    public boolean isArchived;
    @ColumnInfo(defaultValue = "0")
    public boolean isDeleted;
    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String recurrenceType = Constants.RECURRENCE_NONE;
    @ColumnInfo(defaultValue = "1")
    public int recurrenceInterval = 1;
    @NonNull
    @ColumnInfo(defaultValue = "'TODO'")
    public String boardColumn = Constants.BOARD_TODO;
    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String attachment = "";
    @ColumnInfo(defaultValue = "25")
    public int estimatedMinutes = 25;
    @ColumnInfo(defaultValue = "0")
    public long sortOrder;
    public long createdAt;
    public long updatedAt;
    @ColumnInfo(defaultValue = "0")
    public long completedAt;
    @ColumnInfo(defaultValue = "0")
    public long archivedAt;
    @ColumnInfo(defaultValue = "0")
    public long deletedAt;
    @ColumnInfo(defaultValue = "0")
    public long lastFocusedAt;

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
        this.sortOrder = now;
    }
}
