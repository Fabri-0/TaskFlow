package com.taskflow.utils;

import java.util.List;

import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.relation.TaskFull;

public final class ProgressUtils {
    private ProgressUtils() {
    }

    public static int calculatePercentage(int completed, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((completed * 100f) / total);
    }

    public static String formatCounter(int completed, int total) {
        return completed + "/" + total;
    }

    public static int completedSubtasks(List<SubtaskEntity> subtasks) {
        if (subtasks == null) {
            return 0;
        }
        int count = 0;
        for (SubtaskEntity subtask : subtasks) {
            if (subtask.isCompleted) {
                count++;
            }
        }
        return count;
    }

    public static int taskCompletionPercentage(TaskFull taskFull) {
        if (taskFull == null || taskFull.subtasks == null || taskFull.subtasks.isEmpty()) {
            return taskFull != null && taskFull.task != null && taskFull.task.isCompleted ? 100 : 0;
        }
        return calculatePercentage(completedSubtasks(taskFull.subtasks), taskFull.subtasks.size());
    }

    public static int visibleTasksProgress(List<TaskFull> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        int completed = 0;
        for (TaskFull task : tasks) {
            if (task.task != null && task.task.isCompleted) {
                completed++;
            }
        }
        return calculatePercentage(completed, tasks.size());
    }
}
