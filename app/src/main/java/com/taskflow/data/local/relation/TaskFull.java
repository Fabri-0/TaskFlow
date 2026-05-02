package com.taskflow.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.entity.TaskTagCrossRef;

import java.util.ArrayList;
import java.util.List;

public class TaskFull {
    @Embedded
    public TaskEntity task;

    @Relation(parentColumn = "id", entityColumn = "taskId")
    public List<SubtaskEntity> subtasks = new ArrayList<>();

    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(value = TaskTagCrossRef.class, parentColumn = "taskId", entityColumn = "tagId")
    )
    public List<TagEntity> tags = new ArrayList<>();

    @Relation(parentColumn = "projectId", entityColumn = "id")
    public ProjectEntity project;
}
