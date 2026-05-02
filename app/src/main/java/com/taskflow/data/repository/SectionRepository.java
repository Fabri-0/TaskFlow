package com.taskflow.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.taskflow.data.local.dao.SectionDao;
import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.SectionEntity;

import java.util.List;

public class SectionRepository {
    private final SectionDao sectionDao;

    public SectionRepository(Context context) {
        sectionDao = AppDatabase.getInstance(context).sectionDao();
    }

    public LiveData<List<SectionEntity>> getSections(long projectId) {
        return sectionDao.getSectionsByProject(projectId);
    }
}
