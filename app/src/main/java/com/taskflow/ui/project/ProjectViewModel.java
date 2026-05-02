package com.taskflow.ui.project;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.repository.ProjectRepository;
import com.taskflow.data.repository.ResultCallback;
import com.taskflow.data.repository.TagRepository;

import java.util.List;

public class ProjectViewModel extends AndroidViewModel {
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProjectViewModel(@NonNull Application application) {
        super(application);
        projectRepository = new ProjectRepository(application);
        tagRepository = new TagRepository(application);
    }

    public LiveData<List<ProjectEntity>> getProjects(long userId) {
        return projectRepository.getProjects(userId);
    }

    public LiveData<List<TagEntity>> getTags(long userId) {
        return tagRepository.getTags(userId);
    }

    public LiveData<String> getError() {
        return error;
    }

    public void createProject(long userId, String name) {
        projectRepository.createProject(userId, name, new ResultCallback<Long>() {
            @Override
            public void onSuccess(Long value) {
                error.setValue(null);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void createTag(long userId, String name) {
        tagRepository.createTag(userId, name, new ResultCallback<Long>() {
            @Override
            public void onSuccess(Long value) {
                error.setValue(null);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void updateProject(ProjectEntity project, String name) {
        projectRepository.updateProject(project, name, booleanCallback());
    }

    public void deleteProject(ProjectEntity project) {
        projectRepository.deleteProject(project, booleanCallback());
    }

    public void updateTag(TagEntity tag, String name) {
        tagRepository.updateTag(tag, name, booleanCallback());
    }

    public void deleteTag(TagEntity tag) {
        tagRepository.deleteTag(tag, booleanCallback());
    }

    private ResultCallback<Boolean> booleanCallback() {
        return new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                error.setValue(null);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        };
    }
}
