package com.taskflow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.entity.TaskTagCrossRef;

import java.util.List;

@Dao
public interface TaskTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertRelation(TaskTagCrossRef ref);

    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    void deleteRelationsForTask(long taskId);

    @Query("SELECT tagId FROM task_tag_cross_ref WHERE taskId = :taskId")
    List<Long> getTagIdsForTask(long taskId);

    @Query("SELECT tasks.* FROM tasks INNER JOIN task_tag_cross_ref ON tasks.id = task_tag_cross_ref.taskId WHERE task_tag_cross_ref.tagId = :tagId AND tasks.userId = :userId")
    LiveData<List<TaskEntity>> getTasksByTag(long userId, long tagId);
}
