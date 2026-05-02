package com.taskflow.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.taskflow.data.local.dao.TagDao;
import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.TagEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TagRepository {
    private final TagDao tagDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TagRepository(Context context) {
        tagDao = AppDatabase.getInstance(context).tagDao();
    }

    public LiveData<List<TagEntity>> getTags(long userId) {
        return tagDao.getTagsByUser(userId);
    }

    public void createTag(long userId, String name, ResultCallback<Long> callback) {
        executor.execute(() -> {
            String clean = name == null ? "" : name.trim();
            if (clean.isEmpty()) {
                mainHandler.post(() -> callback.onError("El nombre de la etiqueta es obligatorio."));
                return;
            }
            TagEntity existing = tagDao.getTagByName(clean, userId);
            if (existing != null) {
                mainHandler.post(() -> callback.onError("Esa etiqueta ya existe."));
                return;
            }
            long id = tagDao.insertTag(new TagEntity(clean, "#EFA4C8", userId));
            mainHandler.post(() -> callback.onSuccess(id));
        });
    }

    public void updateTag(TagEntity tag, String name, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            String clean = name == null ? "" : name.trim();
            if (tag == null || tag.id <= 0) {
                mainHandler.post(() -> callback.onError("La etiqueta ya no existe."));
                return;
            }
            if (clean.isEmpty()) {
                mainHandler.post(() -> callback.onError("El nombre de la etiqueta es obligatorio."));
                return;
            }
            TagEntity existing = tagDao.getTagByName(clean, tag.userId);
            if (existing != null && existing.id != tag.id) {
                mainHandler.post(() -> callback.onError("Esa etiqueta ya existe."));
                return;
            }
            tag.name = clean;
            tagDao.updateTag(tag);
            mainHandler.post(() -> callback.onSuccess(true));
        });
    }

    public void deleteTag(TagEntity tag, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            if (tag == null || tag.id <= 0) {
                mainHandler.post(() -> callback.onError("La etiqueta ya no existe."));
                return;
            }
            tagDao.deleteTag(tag);
            mainHandler.post(() -> callback.onSuccess(true));
        });
    }
}
