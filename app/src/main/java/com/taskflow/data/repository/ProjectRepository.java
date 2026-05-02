package com.taskflow.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.taskflow.data.local.dao.ProjectDao;
import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.ProjectEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectRepository {
    private final ProjectDao projectDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ProjectRepository(Context context) {
        projectDao = AppDatabase.getInstance(context).projectDao();
    }

    public LiveData<List<ProjectEntity>> getProjects(long userId) {
        return projectDao.getProjectsByUser(userId);
    }

    public void createProject(long userId, String name, ResultCallback<Long> callback) {
        executor.execute(() -> {
            String clean = name == null ? "" : name.trim();
            if (clean.isEmpty()) {
                mainHandler.post(() -> callback.onError("El nombre del proyecto es obligatorio."));
                return;
            }
            ProjectEntity existing = projectDao.getProjectByName(clean, userId);
            if (existing != null) {
                mainHandler.post(() -> callback.onError("Ese proyecto ya existe."));
                return;
            }
            long id = projectDao.insertProject(new ProjectEntity(clean, "#B56BE8", userId));
            mainHandler.post(() -> callback.onSuccess(id));
        });
    }

    public void updateProject(ProjectEntity project, String name, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            String clean = name == null ? "" : name.trim();
            if (project == null || project.id <= 0) {
                mainHandler.post(() -> callback.onError("La categoria ya no existe."));
                return;
            }
            if (clean.isEmpty()) {
                mainHandler.post(() -> callback.onError("El nombre de la categoria es obligatorio."));
                return;
            }
            ProjectEntity existing = projectDao.getProjectByName(clean, project.userId);
            if (existing != null && existing.id != project.id) {
                mainHandler.post(() -> callback.onError("Esa categoria ya existe."));
                return;
            }
            project.name = clean;
            projectDao.updateProject(project);
            mainHandler.post(() -> callback.onSuccess(true));
        });
    }

    public void deleteProject(ProjectEntity project, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            if (project == null || project.id <= 0) {
                mainHandler.post(() -> callback.onError("La categoria ya no existe."));
                return;
            }
            projectDao.deleteProject(project);
            mainHandler.post(() -> callback.onSuccess(true));
        });
    }
}
