package com.taskflow.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.taskflow.data.local.dao.ProjectDao;
import com.taskflow.data.local.dao.SubtaskDao;
import com.taskflow.data.local.dao.TagDao;
import com.taskflow.data.local.dao.TaskDao;
import com.taskflow.data.local.dao.TaskTagDao;
import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.entity.TaskTagCrossRef;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.notifications.ReminderScheduler;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;
import com.taskflow.utils.Validators;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private final Context context;
    private final TaskDao taskDao;
    private final SubtaskDao subtaskDao;
    private final ProjectDao projectDao;
    private final TagDao tagDao;
    private final TaskTagDao taskTagDao;
    private final ReminderScheduler reminderScheduler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TaskRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        taskDao = database.taskDao();
        subtaskDao = database.subtaskDao();
        projectDao = database.projectDao();
        tagDao = database.tagDao();
        taskTagDao = database.taskTagDao();
        reminderScheduler = new ReminderScheduler(this.context);
    }

    public LiveData<List<TaskFull>> getVisibleTasks(long userId, int filter, String query, Long projectId, Long tagId) {
        if (!TextUtils.isEmpty(query)) {
            return taskDao.searchFullTasks(userId, query.trim());
        }
        if (tagId != null && tagId > 0) {
            return taskDao.getFullTasksByTag(userId, tagId);
        }
        if (projectId != null && projectId > 0) {
            return taskDao.getFullTasksByProject(userId, projectId);
        }
        if (filter == Constants.FILTER_TODAY) {
            return taskDao.getFullTasksForToday(userId, DateUtils.startOfToday(), DateUtils.endOfToday());
        }
        if (filter == Constants.FILTER_OVERDUE) {
            return taskDao.getFullOverdueTasks(userId, DateUtils.startOfToday());
        }
        if (filter == Constants.FILTER_COMPLETED) {
            return taskDao.getFullCompletedTasks(userId);
        }
        if (filter == Constants.FILTER_PENDING) {
            return taskDao.getFullPendingTasks(userId);
        }
        if (filter == Constants.FILTER_STARRED) {
            return taskDao.getFullStarredTasks(userId);
        }
        return taskDao.getAllFullTasks(userId);
    }

    public LiveData<List<TaskFull>> getTasksByDate(long userId, long timestamp) {
        return taskDao.getFullTasksByDateRange(userId, DateUtils.startOfDay(timestamp), DateUtils.endOfDay(timestamp));
    }

    public LiveData<TaskFull> getTaskFull(long taskId, long userId) {
        return taskDao.getTaskFullById(taskId, userId);
    }

    public void createTask(long userId, String title, String description, Long dueDate, Long reminderDate,
                           String projectName, String tagName, List<String> subtaskTitles, boolean starred,
                           ResultCallback<Long> callback) {
        executor.execute(() -> {
            if (!Validators.isValidTaskTitle(title)) {
                postError(callback, "El titulo de la tarea es obligatorio.");
                return;
            }
            TaskEntity task = new TaskEntity(title.trim(), normalize(description), userId, dueDate, normalizeReminder(reminderDate));
            task.projectId = resolveProjectId(userId, projectName);
            task.isStarred = starred;
            long taskId = taskDao.insertTask(task);
            saveSubtasks(taskId, subtaskTitles);
            linkTagIfPresent(userId, taskId, tagName);
            scheduleIfValid(taskId, task.title, task.reminderDate);
            postSuccess(callback, taskId);
        });
    }

    public void updateTask(long userId, long taskId, String title, String description, Long dueDate, Long reminderDate,
                           String projectName, String tagName, List<String> subtaskTitles, boolean starred,
                           ResultCallback<Long> callback) {
        executor.execute(() -> {
            TaskEntity task = taskDao.getTaskById(taskId, userId);
            if (task == null) {
                postError(callback, "La tarea ya no existe.");
                return;
            }
            if (!Validators.isValidTaskTitle(title)) {
                postError(callback, "El titulo de la tarea es obligatorio.");
                return;
            }
            reminderScheduler.cancel(taskId);
            task.title = title.trim();
            task.description = normalize(description);
            task.dueDate = dueDate;
            task.reminderDate = normalizeReminder(reminderDate);
            task.projectId = resolveProjectId(userId, projectName);
            task.isStarred = starred;
            task.updatedAt = System.currentTimeMillis();
            taskDao.updateTask(task);
            subtaskDao.deleteByTask(taskId);
            saveSubtasks(taskId, subtaskTitles);
            taskTagDao.deleteRelationsForTask(taskId);
            linkTagIfPresent(userId, taskId, tagName);
            scheduleIfValid(taskId, task.title, task.reminderDate);
            postSuccess(callback, taskId);
        });
    }

    public void toggleCompleted(long userId, long taskId, boolean completed) {
        executor.execute(() -> {
            taskDao.updateTaskCompleted(taskId, userId, completed, System.currentTimeMillis());
            if (completed) {
                reminderScheduler.cancel(taskId);
            }
        });
    }

    public void toggleSubtask(long subtaskId, boolean completed) {
        executor.execute(() -> subtaskDao.updateCompleted(subtaskId, completed));
    }

    public void deleteTask(long userId, long taskId, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            reminderScheduler.cancel(taskId);
            taskTagDao.deleteRelationsForTask(taskId);
            subtaskDao.deleteByTask(taskId);
            int deleted = taskDao.deleteTaskById(taskId, userId);
            if (deleted > 0) {
                postSuccess(callback, true);
            } else {
                postError(callback, "No se pudo eliminar la tarea.");
            }
        });
    }

    private Long resolveProjectId(long userId, String projectName) {
        if (TextUtils.isEmpty(projectName)) {
            return null;
        }
        String clean = projectName.trim();
        ProjectEntity existing = projectDao.getProjectByName(clean, userId);
        if (existing != null) {
            return existing.id;
        }
        return projectDao.insertProject(new ProjectEntity(clean, "#B56BE8", userId));
    }

    private void linkTagIfPresent(long userId, long taskId, String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return;
        }
        String clean = tagName.trim();
        TagEntity tag = tagDao.getTagByName(clean, userId);
        long tagId = tag == null ? tagDao.insertTag(new TagEntity(clean, "#EFA4C8", userId)) : tag.id;
        taskTagDao.insertRelation(new TaskTagCrossRef(taskId, tagId));
    }

    private void saveSubtasks(long taskId, List<String> subtaskTitles) {
        if (subtaskTitles == null) {
            return;
        }
        for (String subtaskTitle : subtaskTitles) {
            if (!TextUtils.isEmpty(subtaskTitle)) {
                subtaskDao.insertSubtask(new SubtaskEntity(taskId, subtaskTitle.trim(), false));
            }
        }
    }

    private Long normalizeReminder(Long reminderDate) {
        if (reminderDate == null || reminderDate <= System.currentTimeMillis()) {
            return null;
        }
        return reminderDate;
    }

    private String normalize(String value) {
        return TextUtils.isEmpty(value) ? "" : value.trim();
    }

    private void scheduleIfValid(long taskId, String title, Long reminderDate) {
        if (reminderDate != null && reminderDate > System.currentTimeMillis()) {
            reminderScheduler.schedule(taskId, title, reminderDate);
        }
    }

    private <T> void postSuccess(ResultCallback<T> callback, T value) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(value));
        }
    }

    private <T> void postError(ResultCallback<T> callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
}
