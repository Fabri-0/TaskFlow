package com.taskflow.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.data.repository.ProjectRepository;
import com.taskflow.data.repository.TagRepository;
import com.taskflow.data.repository.TaskRepository;
import com.taskflow.utils.Constants;
import com.taskflow.utils.ProgressUtils;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final MediatorLiveData<List<TaskFull>> visibleTasks = new MediatorLiveData<>();
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private LiveData<List<TaskFull>> currentSource;
    private long userId;
    private int filter = Constants.FILTER_ALL;
    private String query = "";
    private Long projectId;
    private Long tagId;

    public MainViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        projectRepository = new ProjectRepository(application);
        tagRepository = new TagRepository(application);
    }

    public void setUserId(long userId) {
        this.userId = userId;
        refresh();
    }

    public LiveData<List<TaskFull>> getVisibleTasks() {
        return visibleTasks;
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<List<ProjectEntity>> getProjects() {
        return projectRepository.getProjects(userId);
    }

    public LiveData<List<TagEntity>> getTags() {
        return tagRepository.getTags(userId);
    }

    public void setFilter(int filter) {
        this.filter = filter;
        projectId = null;
        tagId = null;
        refresh();
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        refresh();
    }

    public void setProjectFilter(Long projectId) {
        this.projectId = projectId;
        this.tagId = null;
        refresh();
    }

    public void setTagFilter(Long tagId) {
        this.tagId = tagId;
        this.projectId = null;
        refresh();
    }

    private void refresh() {
        if (userId <= 0) {
            return;
        }
        if (currentSource != null) {
            visibleTasks.removeSource(currentSource);
        }
        currentSource = taskRepository.getVisibleTasks(userId, filter, query, projectId, tagId);
        visibleTasks.addSource(currentSource, tasks -> {
            visibleTasks.setValue(tasks);
            progress.setValue(ProgressUtils.visibleTasksProgress(tasks));
        });
    }
}
