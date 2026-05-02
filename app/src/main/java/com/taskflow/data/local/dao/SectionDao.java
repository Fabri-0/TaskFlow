package com.taskflow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.taskflow.data.local.entity.SectionEntity;

import java.util.List;

@Dao
public interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSection(SectionEntity section);

    @Query("SELECT * FROM sections WHERE projectId = :projectId ORDER BY name ASC")
    LiveData<List<SectionEntity>> getSectionsByProject(long projectId);

    @Query("SELECT * FROM sections WHERE projectId = :projectId ORDER BY name ASC")
    List<SectionEntity> getSectionsByProjectSync(long projectId);
}
