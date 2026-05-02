package com.taskflow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.taskflow.data.local.entity.SubtaskEntity;

import java.util.List;

@Dao
public interface SubtaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSubtask(SubtaskEntity subtask);

    @Update
    void updateSubtask(SubtaskEntity subtask);

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    void deleteByTask(long taskId);

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    void deleteById(long subtaskId);

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    LiveData<List<SubtaskEntity>> getSubtasksByTask(long taskId);

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    List<SubtaskEntity> getSubtasksByTaskSync(long taskId);

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    int countTotal(long taskId);

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId AND isCompleted = 1")
    int countCompleted(long taskId);

    @Query("UPDATE subtasks SET isCompleted = :completed WHERE id = :subtaskId")
    void updateCompleted(long subtaskId, boolean completed);
}
