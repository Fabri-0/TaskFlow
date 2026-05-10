package com.taskflow.ui.task;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskTemplateEntity;
import com.taskflow.data.repository.ProjectRepository;
import com.taskflow.data.repository.ResultCallback;
import com.taskflow.data.repository.TagRepository;
import com.taskflow.data.repository.TaskRepository;
import com.taskflow.session.SessionManager;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final SessionManager sessionManager;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        projectRepository = new ProjectRepository(application);
        tagRepository = new TagRepository(application);
        sessionManager = new SessionManager(application);
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public LiveData<TaskFull> getTask(long taskId) {
        return repository.getTaskFull(taskId, sessionManager.getActiveUserId());
    }

    public LiveData<List<ProjectEntity>> getProjects() {
        return projectRepository.getProjects(sessionManager.getActiveUserId());
    }

    public LiveData<List<TagEntity>> getTags() {
        return tagRepository.getTags(sessionManager.getActiveUserId());
    }

    public LiveData<List<TaskTemplateEntity>> getTemplates() {
        return repository.getTemplates(sessionManager.getActiveUserId());
    }

    public void createTask(String title, String description, Long dueDate, Long reminderDate,
                           String projectName, String tagName, List<String> subtaskTitles, boolean starred) {
        repository.createTask(sessionManager.getActiveUserId(), title, description, dueDate, reminderDate,
                projectName, tagName, subtaskTitles, starred, saveCallback());
    }

    public void createTask(String title, String description, Long dueDate, Long reminderDate,
                           Long reminderDate2, Long reminderDate3, String projectName, List<String> tagNames,
                           List<String> subtaskTitles, boolean starred, int priority, String recurrenceType,
                           int recurrenceInterval, String boardColumn, String attachment, int estimatedMinutes) {
        repository.createTask(sessionManager.getActiveUserId(), title, description, dueDate, reminderDate,
                reminderDate2, reminderDate3, projectName, tagNames, subtaskTitles, starred, priority,
                recurrenceType, recurrenceInterval, boardColumn, attachment, estimatedMinutes, saveCallback());
    }

    public void updateTask(long taskId, String title, String description, Long dueDate, Long reminderDate,
                           String projectName, String tagName, List<String> subtaskTitles, boolean starred) {
        repository.updateTask(sessionManager.getActiveUserId(), taskId, title, description, dueDate, reminderDate,
                projectName, tagName, subtaskTitles, starred, saveCallback());
    }

    public void updateTask(long taskId, String title, String description, Long dueDate, Long reminderDate,
                           Long reminderDate2, Long reminderDate3, String projectName, List<String> tagNames,
                           List<String> subtaskTitles, boolean starred, int priority, String recurrenceType,
                           int recurrenceInterval, String boardColumn, String attachment, int estimatedMinutes) {
        repository.updateTask(sessionManager.getActiveUserId(), taskId, title, description, dueDate, reminderDate,
                reminderDate2, reminderDate3, projectName, tagNames, subtaskTitles, starred, priority,
                recurrenceType, recurrenceInterval, boardColumn, attachment, estimatedMinutes, saveCallback());
    }

    public void toggleCompleted(long taskId, boolean completed) {
        repository.toggleCompleted(sessionManager.getActiveUserId(), taskId, completed);
    }

    public void toggleSubtask(long subtaskId, boolean completed) {
        repository.toggleSubtask(subtaskId, completed);
    }

    public void deleteTask(long taskId) {
        repository.deleteTask(sessionManager.getActiveUserId(), taskId, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                deleted.setValue(value);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void deleteTaskPermanently(long taskId) {
        repository.deleteTaskPermanently(sessionManager.getActiveUserId(), taskId, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                deleted.setValue(value);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void archiveTask(long taskId, boolean archived) {
        repository.archiveTask(sessionManager.getActiveUserId(), taskId, archived, booleanCallback());
    }

    public void restoreTask(long taskId) {
        repository.restoreTask(sessionManager.getActiveUserId(), taskId, booleanCallback());
    }

    public void duplicateTask(long taskId) {
        repository.duplicateTask(sessionManager.getActiveUserId(), taskId, saveCallback());
    }

    public void saveAsTemplate(long taskId) {
        repository.saveAsTemplate(sessionManager.getActiveUserId(), taskId, new ResultCallback<Long>() {
            @Override
            public void onSuccess(Long value) {
                saved.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void createTaskFromTemplate(TaskTemplateEntity template) {
        repository.createTaskFromTemplate(sessionManager.getActiveUserId(), template, saveCallback());
    }

    public void updateBoardColumn(long taskId, String boardColumn) {
        repository.updateBoardColumn(sessionManager.getActiveUserId(), taskId, boardColumn, booleanCallback());
    }

    public void markFocused(long taskId) {
        repository.markFocused(sessionManager.getActiveUserId(), taskId);
    }

    public void moveTask(long taskId, long sortOrder) {
        repository.moveTask(sessionManager.getActiveUserId(), taskId, sortOrder);
    }

    private ResultCallback<Long> saveCallback() {
        return new ResultCallback<Long>() {
            @Override
            public void onSuccess(Long value) {
                saved.setValue(true);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        };
    }

    private ResultCallback<Boolean> booleanCallback() {
        return new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                saved.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        };
    }
}
