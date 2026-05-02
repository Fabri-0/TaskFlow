package com.taskflow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.taskflow.data.local.entity.ProjectEntity;

import java.util.List;

@Dao
public interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertProject(ProjectEntity project);

    @Update
    void updateProject(ProjectEntity project);

    @Delete
    void deleteProject(ProjectEntity project);

    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY name ASC")
    LiveData<List<ProjectEntity>> getProjectsByUser(long userId);

    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY name ASC")
    List<ProjectEntity> getProjectsByUserSync(long userId);

    @Query("SELECT * FROM projects WHERE id = :projectId AND userId = :userId LIMIT 1")
    ProjectEntity getProjectById(long projectId, long userId);

    @Query("SELECT * FROM projects WHERE name = :name AND userId = :userId LIMIT 1")
    ProjectEntity getProjectByName(String name, long userId);
}
