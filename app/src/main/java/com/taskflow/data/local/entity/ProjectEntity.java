package com.taskflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "projects",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("userId"),
                @Index(value = {"name", "userId"}, unique = true)
        }
)
public class ProjectEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @NonNull
    public String color = "#B56BE8";

    public long userId;

    public ProjectEntity() {
    }

    @Ignore
    public ProjectEntity(@NonNull String name, @NonNull String color, long userId) {
        this.name = name;
        this.color = color;
        this.userId = userId;
    }
}
