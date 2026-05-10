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

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    TaskEntity getTaskByIdAnyUser(long taskId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId AND userId = :userId LIMIT 1")
    LiveData<TaskFull> getTaskFullById(long taskId, long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    LiveData<TaskFull> getTaskFullByIdAnyUser(long taskId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 ORDER BY isCompleted ASC, priority DESC, sortOrder ASC, COALESCE(dueDate, 9223372036854775807) ASC, createdAt DESC")
    LiveData<List<TaskEntity>> getAllTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 ORDER BY isCompleted ASC, priority DESC, sortOrder ASC, COALESCE(dueDate, 9223372036854775807) ASC, createdAt DESC")
    LiveData<List<TaskFull>> getAllFullTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0 ORDER BY isArchived ASC, isCompleted ASC, priority DESC, COALESCE(dueDate, 9223372036854775807) ASC, createdAt DESC")
    LiveData<List<TaskFull>> getAllFullTasksIncludingArchived(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY isDeleted ASC, isArchived ASC, isCompleted ASC, priority DESC, COALESCE(dueDate, 9223372036854775807) ASC, createdAt DESC")
    LiveData<List<TaskFull>> getAllFullTasksIncludingArchivedAndDeleted(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0 ORDER BY isArchived ASC, isCompleted ASC, priority DESC, COALESCE(dueDate, 9223372036854775807) ASC, createdAt DESC")
    List<TaskFull> getAllFullTasksIncludingArchivedSync(long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND dueDate BETWEEN :startOfDay AND :endOfDay ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getTasksForToday(long userId, long startOfDay, long endOfDay);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND dueDate BETWEEN :startOfDay AND :endOfDay ORDER BY dueDate ASC")
    LiveData<List<TaskFull>> getFullTasksForToday(long userId, long startOfDay, long endOfDay);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :todayStart ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getOverdueTasks(long userId, long todayStart);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :todayStart ORDER BY dueDate ASC")
    LiveData<List<TaskFull>> getFullOverdueTasks(long userId, long todayStart);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 1 ORDER BY updatedAt DESC")
    LiveData<List<TaskEntity>> getCompletedTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 1 ORDER BY updatedAt DESC")
    LiveData<List<TaskFull>> getFullCompletedTasks(long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 0 ORDER BY priority DESC, COALESCE(dueDate, 9223372036854775807) ASC")
    LiveData<List<TaskEntity>> getPendingTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 0 ORDER BY priority DESC, COALESCE(dueDate, 9223372036854775807) ASC")
    LiveData<List<TaskFull>> getFullPendingTasks(long userId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isStarred = 1 ORDER BY isCompleted ASC, COALESCE(dueDate, 9223372036854775807) ASC")
    LiveData<List<TaskFull>> getFullStarredTasks(long userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR attachment LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<TaskEntity>> searchTasks(long userId, String query);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR attachment LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    LiveData<List<TaskFull>> searchFullTasks(long userId, String query);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND projectId = :projectId ORDER BY updatedAt DESC")
    LiveData<List<TaskEntity>> getTasksByProject(long userId, long projectId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND projectId = :projectId ORDER BY updatedAt DESC")
    LiveData<List<TaskFull>> getFullTasksByProject(long userId, long projectId);

    @Transaction
    @Query("SELECT tasks.* FROM tasks INNER JOIN task_tag_cross_ref ON tasks.id = task_tag_cross_ref.taskId WHERE tasks.userId = :userId AND tasks.isArchived = 0 AND tasks.isDeleted = 0 AND task_tag_cross_ref.tagId = :tagId ORDER BY tasks.updatedAt DESC")
    LiveData<List<TaskFull>> getFullTasksByTag(long userId, long tagId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND dueDate BETWEEN :start AND :end ORDER BY dueDate ASC")
    LiveData<List<TaskEntity>> getTasksByDateRange(long userId, long start, long end);

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND dueDate BETWEEN :start AND :end ORDER BY dueDate ASC")
    LiveData<List<TaskFull>> getFullTasksByDateRange(long userId, long start, long end);

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt, boardColumn = :boardColumn WHERE id = :taskId AND userId = :userId")
    void updateTaskCompleted(long taskId, long userId, boolean completed, long completedAt, long updatedAt, String boardColumn);

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt, boardColumn = :boardColumn WHERE id = :taskId")
    void updateTaskCompletedAnyUser(long taskId, boolean completed, long completedAt, long updatedAt, String boardColumn);

    @Query("UPDATE tasks SET isArchived = :archived, archivedAt = :archivedAt, updatedAt = :updatedAt WHERE id = :taskId AND userId = :userId")
    void updateTaskArchived(long taskId, long userId, boolean archived, long archivedAt, long updatedAt);

    @Query("UPDATE tasks SET isDeleted = :deleted, deletedAt = :deletedAt, isArchived = 0, archivedAt = 0, updatedAt = :updatedAt WHERE id = :taskId AND userId = :userId")
    void updateTaskDeleted(long taskId, long userId, boolean deleted, long deletedAt, long updatedAt);

    @Query("UPDATE tasks SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :taskId AND userId = :userId")
    void updateSortOrder(long taskId, long userId, long sortOrder, long updatedAt);

    @Query("UPDATE tasks SET boardColumn = :boardColumn, isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId AND userId = :userId")
    void updateBoardColumn(long taskId, long userId, String boardColumn, boolean completed, long completedAt, long updatedAt);

    @Query("UPDATE tasks SET lastFocusedAt = :focusedAt, updatedAt = :focusedAt WHERE id = :taskId AND userId = :userId")
    void updateLastFocused(long taskId, long userId, long focusedAt);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId")
    int countTasksForUser(long userId);

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND isDeleted = 0 AND isCompleted = 0 AND ((reminderDate IS NOT NULL AND reminderDate > :now) OR (reminderDate2 IS NOT NULL AND reminderDate2 > :now) OR (reminderDate3 IS NOT NULL AND reminderDate3 > :now))")
    List<TaskEntity> getFutureReminderTasks(long now);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 0 AND dueDate BETWEEN :start AND :end")
    int countPendingByDateRange(long userId, long start, long end);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 0")
    int countPending(long userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isArchived = 0 AND isDeleted = 0 AND isCompleted = 1 AND completedAt BETWEEN :start AND :end")
    int countCompletedBetween(long userId, long start, long end);
}
