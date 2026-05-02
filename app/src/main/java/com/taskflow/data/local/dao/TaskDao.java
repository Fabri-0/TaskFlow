package com.taskflow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.relation.TaskFull;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(TaskEntity task);

    @Update
    void updateTask(TaskEntity task);

    @Query("DELETE FROM tasks WHERE id = :taskId AND userId = :userId")
    int deleteTaskById(long taskId, long userId);

    @Query("SELECT * FROM tasks WHERE id = :taskId AND userId = :userId LIMIT 1")
    TaskEntity getTaskById(long taskId, long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId AND userId = :userId LIMIT 1")
    LiveData<TaskFull> getTaskFullById(long taskId, long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY isCompleted ASC, COALESCE(dueDate, 9223372036854775807) ASC, createdAt DESC")
    LiveData<List<TaskEntity>> getAllTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY isCompleted ASC, COALESCE(dueDate, 9223372036854775807) ASC, createdAt DESC")
    LiveData<List<TaskFull>> getAllFullTasks(long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate BETWEEN :startOfDay AND :endOfDay ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getTasksForToday(long userId, long startOfDay, long endOfDay);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate BETWEEN :startOfDay AND :endOfDay ORDER BY dueDate ASC")
    LiveData<List<TaskFull>> getFullTasksForToday(long userId, long startOfDay, long endOfDay);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :todayStart ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getOverdueTasks(long userId, long todayStart);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :todayStart ORDER BY dueDate ASC")
    LiveData<List<TaskFull>> getFullOverdueTasks(long userId, long todayStart);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 ORDER BY updatedAt DESC")
    LiveData<List<TaskEntity>> getCompletedTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 ORDER BY updatedAt DESC")
    LiveData<List<TaskFull>> getFullCompletedTasks(long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 ORDER BY COALESCE(dueDate, 9223372036854775807) ASC")
    LiveData<List<TaskEntity>> getPendingTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 ORDER BY COALESCE(dueDate, 9223372036854775807) ASC")
    LiveData<List<TaskFull>> getFullPendingTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isStarred = 1 ORDER BY isCompleted ASC, COALESCE(dueDate, 9223372036854775807) ASC")
    LiveData<List<TaskFull>> getFullStarredTasks(long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<TaskEntity>> searchTasks(long userId, String query);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<TaskFull>> searchFullTasks(long userId, String query);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND projectId = :projectId ORDER BY updatedAt DESC")
    LiveData<List<TaskEntity>> getTasksByProject(long userId, long projectId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND projectId = :projectId ORDER BY updatedAt DESC")
    LiveData<List<TaskFull>> getFullTasksByProject(long userId, long projectId);

    @Transaction
    @Query("SELECT tasks.* FROM tasks INNER JOIN task_tag_cross_ref ON tasks.id = task_tag_cross_ref.taskId WHERE tasks.userId = :userId AND task_tag_cross_ref.tagId = :tagId ORDER BY tasks.updatedAt DESC")
    LiveData<List<TaskFull>> getFullTasksByTag(long userId, long tagId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate BETWEEN :start AND :end ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getTasksByDateRange(long userId, long start, long end);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate BETWEEN :start AND :end ORDER BY dueDate ASC")
    LiveData<List<TaskFull>> getFullTasksByDateRange(long userId, long start, long end);

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAt = :updatedAt WHERE id = :taskId AND userId = :userId")
    void updateTaskCompleted(long taskId, long userId, boolean completed, long updatedAt);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId")
    int countTasksForUser(long userId);

    @Query("SELECT * FROM tasks WHERE reminderDate IS NOT NULL AND reminderDate > :now AND isCompleted = 0")
    List<TaskEntity> getFutureReminderTasks(long now);
}
