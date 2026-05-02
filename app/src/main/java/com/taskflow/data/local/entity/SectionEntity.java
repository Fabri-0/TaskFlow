package com.taskflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "sections",
        foreignKeys = @ForeignKey(entity = ProjectEntity.class, parentColumns = "id", childColumns = "projectId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("projectId")}
)
public class SectionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    public long projectId;

    public SectionEntity() {
    }

    @Ignore
    public SectionEntity(@NonNull String name, long projectId) {
        this.name = name;
        this.projectId = projectId;
    }
}
