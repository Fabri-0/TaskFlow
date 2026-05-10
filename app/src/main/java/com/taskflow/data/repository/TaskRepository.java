package com.taskflow.data.repository;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.taskflow.data.local.dao.ProjectDao;
import com.taskflow.data.local.dao.SubtaskDao;
import com.taskflow.data.local.dao.TagDao;
import com.taskflow.data.local.dao.TaskDao;
import com.taskflow.data.local.dao.TaskTagDao;
import com.taskflow.data.local.dao.TaskTemplateDao;
import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.entity.TaskTagCrossRef;
import com.taskflow.data.local.entity.TaskTemplateEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.notifications.ReminderScheduler;
import com.taskflow.notifications.TaskFlowWidgetProvider;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;
import com.taskflow.utils.Validators;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private final Context context;
    private final TaskDao taskDao;
    private final SubtaskDao subtaskDao;
    private final ProjectDao projectDao;
    private final TagDao tagDao;
    private final TaskTagDao taskTagDao;
    private final TaskTemplateDao taskTemplateDao;
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
        taskTemplateDao = database.taskTemplateDao();
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

    public LiveData<List<TaskFull>> getAllTasksForFiltering(long userId) {
        return taskDao.getAllFullTasksIncludingArchivedAndDeleted(userId);
    }

    public LiveData<List<TaskTemplateEntity>> getTemplates(long userId) {
        return taskTemplateDao.getTemplates(userId);
    }

    public LiveData<List<TaskFull>> getTasksByDate(long userId, long timestamp) {
        return taskDao.getFullTasksByDateRange(userId, DateUtils.startOfDay(timestamp), DateUtils.endOfDay(timestamp));
    }

    public LiveData<TaskFull> getTaskFull(long taskId, long userId) {
        return taskDao.getTaskFullById(taskId, userId);
    }

    public LiveData<TaskFull> getTaskFullAnyUser(long taskId) {
        return taskDao.getTaskFullByIdAnyUser(taskId);
    }

    public void createTask(long userId, String title, String description, Long dueDate, Long reminderDate,
                           String projectName, String tagName, List<String> subtaskTitles, boolean starred,
                           ResultCallback<Long> callback) {
        List<String> tags = new ArrayList<>();
        if (!TextUtils.isEmpty(tagName)) {
            tags.add(tagName);
        }
        createTask(userId, title, description, dueDate, reminderDate, null, null, projectName, tags,
                subtaskTitles, starred, Constants.PRIORITY_LOW, Constants.RECURRENCE_NONE, 1,
                Constants.BOARD_TODO, "", 25, callback);
    }

    public void createTask(long userId, String title, String description, Long dueDate,
                           Long reminderDate, Long reminderDate2, Long reminderDate3,
                           String projectName, List<String> tagNames, List<String> subtaskTitles,
                           boolean starred, int priority, String recurrenceType, int recurrenceInterval,
                           String boardColumn, String attachment, int estimatedMinutes,
                           ResultCallback<Long> callback) {
        executor.execute(() -> {
            if (!Validators.isValidTaskTitle(title)) {
                postError(callback, "El titulo de la tarea es obligatorio.");
                return;
            }
            TaskEntity task = new TaskEntity(title.trim(), normalize(description), userId, dueDate, normalizeReminder(reminderDate));
            applyAdvancedFields(task, reminderDate2, reminderDate3, priority, starred, recurrenceType,
                    recurrenceInterval, boardColumn, attachment, estimatedMinutes);
            task.projectId = resolveProjectId(userId, projectName);
            long taskId = taskDao.insertTask(task);
            saveSubtasks(taskId, subtaskTitles, null);
            linkTagsIfPresent(userId, taskId, tagNames);
            scheduleAll(taskId, task.title, task);
            TaskFlowWidgetProvider.updateAll(context);
            postSuccess(callback, taskId);
        });
    }

    public void updateTask(long userId, long taskId, String title, String description, Long dueDate, Long reminderDate,
                           String projectName, String tagName, List<String> subtaskTitles, boolean starred,
                           ResultCallback<Long> callback) {
        List<String> tags = new ArrayList<>();
        if (!TextUtils.isEmpty(tagName)) {
            tags.add(tagName);
        }
        updateTask(userId, taskId, title, description, dueDate, reminderDate, null, null, projectName,
                tags, subtaskTitles, starred, Constants.PRIORITY_LOW, Constants.RECURRENCE_NONE, 1,
                Constants.BOARD_TODO, "", 25, callback);
    }

    public void updateTask(long userId, long taskId, String title, String description, Long dueDate,
                           Long reminderDate, Long reminderDate2, Long reminderDate3,
                           String projectName, List<String> tagNames, List<String> subtaskTitles,
                           boolean starred, int priority, String recurrenceType, int recurrenceInterval,
                           String boardColumn, String attachment, int estimatedMinutes,
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
            Map<String, Boolean> previousSubtasks = completedSubtasksByTitle(taskId);
            reminderScheduler.cancel(taskId);
            task.title = title.trim();
            task.description = normalize(description);
            task.dueDate = dueDate;
            task.reminderDate = normalizeReminder(reminderDate);
            applyAdvancedFields(task, reminderDate2, reminderDate3, priority, starred, recurrenceType,
                    recurrenceInterval, boardColumn, attachment, estimatedMinutes);
            task.projectId = resolveProjectId(userId, projectName);
            task.updatedAt = System.currentTimeMillis();
            taskDao.updateTask(task);
            subtaskDao.deleteByTask(taskId);
            saveSubtasks(taskId, subtaskTitles, previousSubtasks);
            taskTagDao.deleteRelationsForTask(taskId);
            linkTagsIfPresent(userId, taskId, tagNames);
            scheduleAll(taskId, task.title, task);
            TaskFlowWidgetProvider.updateAll(context);
            postSuccess(callback, taskId);
        });
    }

    public void toggleCompleted(long userId, long taskId, boolean completed) {
        executor.execute(() -> {
            TaskEntity task = taskDao.getTaskById(taskId, userId);
            if (task == null) {
                return;
            }
            long now = System.currentTimeMillis();
            String boardColumn = completed ? Constants.BOARD_DONE : Constants.BOARD_TODO;
            taskDao.updateTaskCompleted(taskId, userId, completed, completed ? now : 0L, now, boardColumn);
            if (completed) {
                reminderScheduler.cancel(taskId);
                createNextRecurrenceIfNeeded(task, now);
            } else {
                scheduleAll(taskId, task.title, task);
            }
            TaskFlowWidgetProvider.updateAll(context);
        });
    }

    public void completeTaskFromNotification(long taskId) {
        executor.execute(() -> {
            TaskEntity task = taskDao.getTaskByIdAnyUser(taskId);
            if (task == null) {
                return;
            }
            long now = System.currentTimeMillis();
            taskDao.updateTaskCompletedAnyUser(taskId, true, now, now, Constants.BOARD_DONE);
            reminderScheduler.cancel(taskId);
            createNextRecurrenceIfNeeded(task, now);
            TaskFlowWidgetProvider.updateAll(context);
        });
    }

    public void toggleSubtask(long subtaskId, boolean completed) {
        executor.execute(() -> subtaskDao.updateCompleted(subtaskId, completed));
    }

    public void archiveTask(long userId, long taskId, boolean archived, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            taskDao.updateTaskArchived(taskId, userId, archived, archived ? now : 0L, now);
            if (archived) {
                reminderScheduler.cancel(taskId);
            } else {
                TaskEntity task = taskDao.getTaskById(taskId, userId);
                if (task != null) {
                    scheduleAll(taskId, task.title, task);
                }
            }
            TaskFlowWidgetProvider.updateAll(context);
            postSuccess(callback, true);
        });
    }

    public void restoreTask(long userId, long taskId, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            taskDao.updateTaskDeleted(taskId, userId, false, 0L, now);
            taskDao.updateTaskArchived(taskId, userId, false, 0L, now);
            TaskEntity task = taskDao.getTaskById(taskId, userId);
            if (task != null) {
                scheduleAll(taskId, task.title, task);
            }
            TaskFlowWidgetProvider.updateAll(context);
            postSuccess(callback, true);
        });
    }

    public void updateBoardColumn(long userId, long taskId, String boardColumn, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            TaskEntity task = taskDao.getTaskById(taskId, userId);
            long now = System.currentTimeMillis();
            String normalized = normalizeBoardColumn(boardColumn);
            boolean completed = Constants.BOARD_DONE.equals(normalized);
            taskDao.updateBoardColumn(taskId, userId, normalized, completed, completed ? now : 0L, now);
            if (completed) {
                reminderScheduler.cancel(taskId);
                createNextRecurrenceIfNeeded(task, now);
            }
            TaskFlowWidgetProvider.updateAll(context);
            postSuccess(callback, true);
        });
    }

    public void moveTask(long userId, long taskId, long sortOrder) {
        executor.execute(() -> taskDao.updateSortOrder(taskId, userId, sortOrder, System.currentTimeMillis()));
    }

    public void markFocused(long userId, long taskId) {
        executor.execute(() -> taskDao.updateLastFocused(taskId, userId, System.currentTimeMillis()));
    }

    public void duplicateTask(long userId, long taskId, ResultCallback<Long> callback) {
        executor.execute(() -> {
            TaskEntity task = taskDao.getTaskById(taskId, userId);
            if (task == null) {
                postError(callback, "La tarea ya no existe.");
                return;
            }
            TaskEntity copy = copyTaskShell(task);
            copy.title = task.title + " (copia)";
            copy.isCompleted = false;
            copy.completedAt = 0L;
            copy.isDeleted = false;
            copy.deletedAt = 0L;
            copy.boardColumn = Constants.BOARD_TODO;
            long copyId = taskDao.insertTask(copy);
            List<SubtaskEntity> subtasks = subtaskDao.getSubtasksByTaskSync(taskId);
            for (SubtaskEntity subtask : subtasks) {
                subtaskDao.insertSubtask(new SubtaskEntity(copyId, subtask.title, false));
            }
            for (Long tagId : taskTagDao.getTagIdsForTask(taskId)) {
                taskTagDao.insertRelation(new TaskTagCrossRef(copyId, tagId));
            }
            scheduleAll(copyId, copy.title, copy);
            TaskFlowWidgetProvider.updateAll(context);
            postSuccess(callback, copyId);
        });
    }

    public void saveAsTemplate(long userId, long taskId, ResultCallback<Long> callback) {
        executor.execute(() -> {
            TaskEntity task = taskDao.getTaskById(taskId, userId);
            if (task == null) {
                postError(callback, "La tarea ya no existe.");
                return;
            }
            String name = task.title;
            if (taskTemplateDao.countByName(userId, name) > 0) {
                name = name + " " + System.currentTimeMillis();
            }
            TaskTemplateEntity template = new TaskTemplateEntity(userId, name, task.title);
            template.description = normalize(task.description);
            template.projectName = projectName(task.projectId, userId);
            template.tagsCsv = tagsCsv(taskId);
            template.subtasksCsv = subtasksCsv(taskId);
            template.priority = task.priority;
            template.recurrenceType = normalizeRecurrence(task.recurrenceType);
            template.boardColumn = normalizeBoardColumn(task.boardColumn);
            template.attachment = normalize(task.attachment);
            template.estimatedMinutes = safeMinutes(task.estimatedMinutes);
            long templateId = taskTemplateDao.insertTemplate(template);
            postSuccess(callback, templateId);
        });
    }

    public void createTaskFromTemplate(long userId, TaskTemplateEntity template, ResultCallback<Long> callback) {
        if (template == null) {
            postError(callback, "Selecciona una plantilla valida.");
            return;
        }
        createTask(userId, template.title, template.description, null, null, null, null,
                template.projectName, splitCsv(template.tagsCsv), splitLines(template.subtasksCsv),
                false, template.priority, template.recurrenceType, 1, template.boardColumn,
                template.attachment, template.estimatedMinutes, callback);
    }

    public void saveTemplate(long userId, TaskTemplateEntity template, ResultCallback<Long> callback) {
        executor.execute(() -> {
            if (template == null) {
                postError(callback, "Completa la plantilla.");
                return;
            }
            if (TextUtils.isEmpty(template.name) || TextUtils.isEmpty(template.name.trim())) {
                postError(callback, "El nombre de la plantilla es obligatorio.");
                return;
            }
            if (TextUtils.isEmpty(template.title) || TextUtils.isEmpty(template.title.trim())) {
                postError(callback, "El titulo base de la tarea es obligatorio.");
                return;
            }
            TaskTemplateEntity existing = template.id > 0 ? taskTemplateDao.getTemplate(template.id, userId) : null;
            if (template.id > 0 && existing == null) {
                postError(callback, "La plantilla ya no existe.");
                return;
            }
            template.userId = userId;
            template.name = template.name.trim();
            template.title = template.title.trim();
            template.description = normalize(template.description);
            template.projectName = normalize(template.projectName);
            template.tagsCsv = normalize(template.tagsCsv);
            template.subtasksCsv = normalize(template.subtasksCsv);
            template.recurrenceType = normalizeRecurrence(template.recurrenceType);
            template.boardColumn = normalizeBoardColumn(template.boardColumn);
            template.attachment = normalize(template.attachment);
            template.priority = Math.max(Constants.PRIORITY_LOW, Math.min(Constants.PRIORITY_URGENT, template.priority));
            template.estimatedMinutes = safeMinutes(template.estimatedMinutes);
            template.createdAt = existing == null ? System.currentTimeMillis() : existing.createdAt;
            long templateId = taskTemplateDao.insertTemplate(template);
            postSuccess(callback, templateId);
        });
    }

    public void deleteTemplate(long userId, long templateId, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            int deleted = taskTemplateDao.deleteTemplateById(templateId, userId);
            if (deleted > 0) {
                postSuccess(callback, true);
            } else {
                postError(callback, "No se pudo eliminar la plantilla.");
            }
        });
    }

    public void deleteTask(long userId, long taskId, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            reminderScheduler.cancel(taskId);
            long now = System.currentTimeMillis();
            taskDao.updateTaskDeleted(taskId, userId, true, now, now);
            TaskFlowWidgetProvider.updateAll(context);
            postSuccess(callback, true);
        });
    }

    public void deleteTaskPermanently(long userId, long taskId, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            reminderScheduler.cancel(taskId);
            taskTagDao.deleteRelationsForTask(taskId);
            subtaskDao.deleteByTask(taskId);
            int deleted = taskDao.deleteTaskById(taskId, userId);
            if (deleted > 0) {
                TaskFlowWidgetProvider.updateAll(context);
                postSuccess(callback, true);
            } else {
                postError(callback, "No se pudo eliminar la tarea.");
            }
        });
    }

    public void exportTasks(long userId, ResultCallback<String> callback) {
        executor.execute(() -> {
            try {
                List<TaskFull> tasks = taskDao.getAllFullTasksIncludingArchivedSync(userId);
                JSONArray taskArray = new JSONArray();
                for (TaskFull taskFull : tasks) {
                    if (taskFull == null || taskFull.task == null) {
                        continue;
                    }
                    taskArray.put(toJson(taskFull));
                }
                JSONObject root = new JSONObject();
                root.put("app", "TaskFlow");
                root.put("exportedAt", System.currentTimeMillis());
                root.put("tasks", taskArray);
                File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) {
                    dir = context.getFilesDir();
                }
                if (!dir.exists() && !dir.mkdirs()) {
                    postError(callback, "No se pudo crear la carpeta de exportacion.");
                    return;
                }
                File file = new File(dir, "taskflow_export_" + System.currentTimeMillis() + ".json");
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(root.toString(2));
                }
                postSuccess(callback, file.getAbsolutePath());
            } catch (Exception e) {
                postError(callback, "No se pudo exportar: " + e.getMessage());
            }
        });
    }

    private void applyAdvancedFields(TaskEntity task, Long reminderDate2, Long reminderDate3, int priority,
                                     boolean starred, String recurrenceType, int recurrenceInterval,
                                     String boardColumn, String attachment, int estimatedMinutes) {
        task.reminderDate2 = normalizeReminder(reminderDate2);
        task.reminderDate3 = normalizeReminder(reminderDate3);
        task.priority = Math.max(Constants.PRIORITY_LOW, Math.min(Constants.PRIORITY_URGENT, priority));
        task.isStarred = starred || task.priority >= Constants.PRIORITY_HIGH;
        task.recurrenceType = normalizeRecurrence(recurrenceType);
        task.recurrenceInterval = Math.max(1, recurrenceInterval);
        task.boardColumn = normalizeBoardColumn(boardColumn);
        task.attachment = normalize(attachment);
        task.estimatedMinutes = safeMinutes(estimatedMinutes);
    }

    private TaskEntity copyTaskShell(TaskEntity task) {
        long now = System.currentTimeMillis();
        TaskEntity copy = new TaskEntity(task.title, task.description, task.userId, task.dueDate, task.reminderDate);
        copy.reminderDate2 = task.reminderDate2;
        copy.reminderDate3 = task.reminderDate3;
        copy.projectId = task.projectId;
        copy.sectionId = task.sectionId;
        copy.priority = task.priority;
        copy.isStarred = task.isStarred;
        copy.isArchived = false;
        copy.isDeleted = false;
        copy.deletedAt = 0L;
        copy.recurrenceType = normalizeRecurrence(task.recurrenceType);
        copy.recurrenceInterval = Math.max(1, task.recurrenceInterval);
        copy.boardColumn = normalizeBoardColumn(task.boardColumn);
        copy.attachment = normalize(task.attachment);
        copy.estimatedMinutes = safeMinutes(task.estimatedMinutes);
        copy.createdAt = now;
        copy.updatedAt = now;
        copy.sortOrder = now;
        return copy;
    }

    private void createNextRecurrenceIfNeeded(TaskEntity completedTask, long now) {
        if (completedTask == null || TextUtils.isEmpty(completedTask.recurrenceType)) {
            return;
        }
        long base = completedTask.dueDate == null ? now : completedTask.dueDate;
        long nextDue = DateUtils.addRecurrence(base, completedTask.recurrenceType, completedTask.recurrenceInterval);
        TaskEntity next = copyTaskShell(completedTask);
        next.dueDate = nextDue;
        next.reminderDate = shiftReminder(completedTask.dueDate, completedTask.reminderDate, nextDue);
        next.reminderDate2 = shiftReminder(completedTask.dueDate, completedTask.reminderDate2, nextDue);
        next.reminderDate3 = shiftReminder(completedTask.dueDate, completedTask.reminderDate3, nextDue);
        next.isCompleted = false;
        next.completedAt = 0L;
        next.boardColumn = Constants.BOARD_TODO;
        long nextId = taskDao.insertTask(next);
        List<SubtaskEntity> subtasks = subtaskDao.getSubtasksByTaskSync(completedTask.id);
        for (SubtaskEntity subtask : subtasks) {
            subtaskDao.insertSubtask(new SubtaskEntity(nextId, subtask.title, false));
        }
        for (Long tagId : taskTagDao.getTagIdsForTask(completedTask.id)) {
            taskTagDao.insertRelation(new TaskTagCrossRef(nextId, tagId));
        }
        scheduleAll(nextId, next.title, next);
    }

    private Long shiftReminder(Long oldDue, Long oldReminder, long nextDue) {
        if (oldReminder == null) {
            return null;
        }
        if (oldDue == null) {
            return normalizeReminder(DateUtils.addRecurrence(oldReminder, Constants.RECURRENCE_DAILY, 1));
        }
        long offset = oldDue - oldReminder;
        return normalizeReminder(nextDue - Math.max(0L, offset));
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

    private void linkTagsIfPresent(long userId, long taskId, List<String> tagNames) {
        if (tagNames == null) {
            return;
        }
        for (String tagName : tagNames) {
            if (TextUtils.isEmpty(tagName)) {
                continue;
            }
            String clean = tagName.trim();
            if (clean.isEmpty()) {
                continue;
            }
            TagEntity tag = tagDao.getTagByName(clean, userId);
            long tagId = tag == null ? tagDao.insertTag(new TagEntity(clean, "#EFA4C8", userId)) : tag.id;
            taskTagDao.insertRelation(new TaskTagCrossRef(taskId, tagId));
        }
    }

    private void saveSubtasks(long taskId, List<String> subtaskTitles, Map<String, Boolean> previousCompleted) {
        if (subtaskTitles == null) {
            return;
        }
        for (String subtaskTitle : subtaskTitles) {
            if (!TextUtils.isEmpty(subtaskTitle)) {
                String clean = subtaskTitle.trim();
                boolean completed = previousCompleted != null && Boolean.TRUE.equals(previousCompleted.get(clean.toLowerCase()));
                subtaskDao.insertSubtask(new SubtaskEntity(taskId, clean, completed));
            }
        }
    }

    private Map<String, Boolean> completedSubtasksByTitle(long taskId) {
        Map<String, Boolean> values = new HashMap<>();
        List<SubtaskEntity> subtasks = subtaskDao.getSubtasksByTaskSync(taskId);
        for (SubtaskEntity subtask : subtasks) {
            if (subtask != null && !TextUtils.isEmpty(subtask.title)) {
                values.put(subtask.title.trim().toLowerCase(), subtask.isCompleted);
            }
        }
        return values;
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

    private int safeMinutes(int minutes) {
        return Math.max(5, Math.min(240, minutes));
    }

    private String normalizeRecurrence(String recurrenceType) {
        if (Constants.RECURRENCE_DAILY.equals(recurrenceType)
                || Constants.RECURRENCE_WEEKLY.equals(recurrenceType)
                || Constants.RECURRENCE_MONTHLY.equals(recurrenceType)) {
            return recurrenceType;
        }
        return Constants.RECURRENCE_NONE;
    }

    private String normalizeBoardColumn(String boardColumn) {
        if (Constants.BOARD_DOING.equals(boardColumn) || Constants.BOARD_DONE.equals(boardColumn)) {
            return boardColumn;
        }
        return Constants.BOARD_TODO;
    }

    private void scheduleAll(long taskId, String title, TaskEntity task) {
        if (task == null || task.isArchived || task.isDeleted || task.isCompleted) {
            return;
        }
        scheduleIfValid(taskId, 1, title, task.reminderDate);
        scheduleIfValid(taskId, 2, title, task.reminderDate2);
        scheduleIfValid(taskId, 3, title, task.reminderDate3);
    }

    private void scheduleIfValid(long taskId, int slot, String title, Long reminderDate) {
        if (reminderDate != null && reminderDate > System.currentTimeMillis()) {
            reminderScheduler.schedule(taskId, slot, title, reminderDate);
        }
    }

    private String projectName(Long projectId, long userId) {
        if (projectId == null) {
            return "";
        }
        ProjectEntity project = projectDao.getProjectById(projectId, userId);
        return project == null ? "" : project.name;
    }

    private String tagsCsv(long taskId) {
        List<Long> ids = taskTagDao.getTagIdsForTask(taskId);
        List<TagEntity> allTags = tagDao.getTagsByUserSync(taskDao.getTaskByIdAnyUser(taskId).userId);
        List<String> names = new ArrayList<>();
        for (TagEntity tag : allTags) {
            if (ids.contains(tag.id)) {
                names.add(tag.name);
            }
        }
        return TextUtils.join(", ", names);
    }

    private String subtasksCsv(long taskId) {
        List<String> values = new ArrayList<>();
        for (SubtaskEntity subtask : subtaskDao.getSubtasksByTaskSync(taskId)) {
            values.add(subtask.title);
        }
        return TextUtils.join("\n", values);
    }

    private List<String> splitCsv(String csv) {
        List<String> values = new ArrayList<>();
        if (TextUtils.isEmpty(csv)) {
            return values;
        }
        for (String item : csv.split(",")) {
            String clean = item.trim();
            if (!clean.isEmpty()) {
                values.add(clean);
            }
        }
        return values;
    }

    private List<String> splitLines(String text) {
        List<String> values = new ArrayList<>();
        if (TextUtils.isEmpty(text)) {
            return values;
        }
        for (String item : text.split("\\r?\\n")) {
            String clean = item.trim();
            if (!clean.isEmpty()) {
                values.add(clean);
            }
        }
        return values;
    }

    private JSONObject toJson(TaskFull taskFull) throws Exception {
        TaskEntity task = taskFull.task;
        JSONObject json = new JSONObject();
        json.put("id", task.id);
        json.put("title", task.title);
        json.put("description", task.description);
        json.put("dueDate", task.dueDate);
        json.put("reminders", remindersToJson(task));
        json.put("completed", task.isCompleted);
        json.put("archived", task.isArchived);
        json.put("priority", task.priority);
        json.put("starred", task.isStarred);
        json.put("recurrence", DateUtils.recurrenceLabel(task.recurrenceType, task.recurrenceInterval));
        json.put("boardColumn", task.boardColumn);
        json.put("attachment", task.attachment);
        json.put("estimatedMinutes", task.estimatedMinutes);
        json.put("project", taskFull.project == null ? "" : taskFull.project.name);
        JSONArray tags = new JSONArray();
        if (taskFull.tags != null) {
            for (TagEntity tag : taskFull.tags) {
                tags.put(tag.name);
            }
        }
        json.put("tags", tags);
        JSONArray subtasks = new JSONArray();
        if (taskFull.subtasks != null) {
            for (SubtaskEntity subtask : taskFull.subtasks) {
                JSONObject item = new JSONObject();
                item.put("title", subtask.title);
                item.put("completed", subtask.isCompleted);
                subtasks.put(item);
            }
        }
        json.put("subtasks", subtasks);
        return json;
    }

    private JSONArray remindersToJson(TaskEntity task) {
        JSONArray reminders = new JSONArray();
        if (task.reminderDate != null) {
            reminders.put(task.reminderDate);
        }
        if (task.reminderDate2 != null) {
            reminders.put(task.reminderDate2);
        }
        if (task.reminderDate3 != null) {
            reminders.put(task.reminderDate3);
        }
        return reminders;
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
