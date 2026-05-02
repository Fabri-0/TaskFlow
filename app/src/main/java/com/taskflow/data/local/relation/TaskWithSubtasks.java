package com.taskflow.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TaskEntity;

import java.util.ArrayList;
import java.util.List;

public class TaskWithSubtasks {
    @Embedded
    public TaskEntity task;

    @Relation(parentColumn = "id", entityColumn = "taskId")
    public List<SubtaskEntity> subtasks = new ArrayList<>();
}
