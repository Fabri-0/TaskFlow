package com.taskflow.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.data.repository.ProjectRepository;
import com.taskflow.data.repository.TagRepository;
import com.taskflow.data.repository.TaskRepository;
import com.taskflow.utils.Constants;
import com.taskflow.utils.ProgressUtils;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final MediatorLiveData<List<TaskFull>> visibleTasks = new MediatorLiveData<>();
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private LiveData<List<TaskFull>> currentSource;
    private long userId;
    private int filter = Constants.FILTER_ALL;
    private String query = "";
    private Long projectId;
    private Long tagId;
    private Integer priority;

    public MainViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        projectRepository = new ProjectRepository(application);
        tagRepository = new TagRepository(application);
    }

    public void setUserId(long userId) {
        this.userId = userId;
        refresh();
    }

    public LiveData<List<TaskFull>> getVisibleTasks() {
        return visibleTasks;
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<List<ProjectEntity>> getProjects() {
        return projectRepository.getProjects(userId);
    }

    public LiveData<List<TagEntity>> getTags() {
        return tagRepository.getTags(userId);
    }

    public void setFilter(int filter) {
        this.filter = filter;
        refresh();
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        refresh();
    }

    public void setProjectFilter(Long projectId) {
        this.projectId = projectId;
        refresh();
    }

    public void setTagFilter(Long tagId) {
        this.tagId = tagId;
        refresh();
    }

    public void setPriorityFilter(Integer priority) {
        this.priority = priority;
        refresh();
    }

    public void clearAdvancedFilters() {
        projectId = null;
        tagId = null;
        priority = null;
        refresh();
    }

    private void refresh() {
        if (userId <= 0) {
            return;
        }
        if (currentSource != null) {
            visibleTasks.removeSource(currentSource);
        }
        currentSource = taskRepository.getAllTasksForFiltering(userId);
        visibleTasks.addSource(currentSource, tasks -> {
            List<TaskFull> filtered = filterTasks(tasks);
            visibleTasks.setValue(filtered);
            progress.setValue(ProgressUtils.visibleTasksProgress(filtered));
        });
    }

    private List<TaskFull> filterTasks(List<TaskFull> tasks) {
        java.util.ArrayList<TaskFull> filtered = new java.util.ArrayList<>();
        if (tasks == null) {
            return filtered;
        }
        String cleanQuery = query == null ? "" : query.trim().toLowerCase();
        long todayStart = com.taskflow.utils.DateUtils.startOfToday();
        long todayEnd = com.taskflow.utils.DateUtils.endOfToday();
        long upcomingEnd = todayStart + 7L * 24L * 60L * 60L * 1000L;
        for (TaskFull taskFull : tasks) {
            if (taskFull == null || taskFull.task == null) {
                continue;
            }
            TaskEntity task = taskFull.task;
            if (filter == Constants.FILTER_TRASH) {
                if (!task.isDeleted) {
                    continue;
                }
            } else if (task.isDeleted) {
                continue;
            } else if (filter == Constants.FILTER_ARCHIVED) {
                if (!task.isArchived) {
                    continue;
                }
            } else if (task.isArchived) {
                continue;
            }
            if (projectId != null && projectId > 0 && (task.projectId == null || !projectId.equals(task.projectId))) {
                continue;
            }
            if (tagId != null && tagId > 0 && !hasTag(taskFull, tagId)) {
                continue;
            }
            if (priority != null && task.priority != priority) {
                continue;
            }
            if (!cleanQuery.isEmpty() && !matchesQuery(taskFull, cleanQuery)) {
                continue;
            }
            if (!matchesFilter(task, todayStart, todayEnd, upcomingEnd)) {
                continue;
            }
            filtered.add(taskFull);
        }
        return filtered;
    }

    private boolean hasTag(TaskFull taskFull, long activeTagId) {
        if (taskFull.tags == null) {
            return false;
        }
        for (TagEntity tag : taskFull.tags) {
            if (tag != null && tag.id == activeTagId) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesQuery(TaskFull taskFull, String cleanQuery) {
        TaskEntity task = taskFull.task;
        if (contains(task.title, cleanQuery) || contains(task.description, cleanQuery) || contains(task.attachment, cleanQuery)) {
            return true;
        }
        if (taskFull.project != null && contains(taskFull.project.name, cleanQuery)) {
            return true;
        }
        if (taskFull.tags != null) {
            for (TagEntity tag : taskFull.tags) {
                if (tag != null && contains(tag.name, cleanQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private boolean matchesFilter(TaskEntity task, long todayStart, long todayEnd, long upcomingEnd) {
        switch (filter) {
            case Constants.FILTER_TODAY:
                return task.dueDate != null && task.dueDate >= todayStart && task.dueDate <= todayEnd;
            case Constants.FILTER_OVERDUE:
                return !task.isCompleted && task.dueDate != null && task.dueDate < todayStart;
            case Constants.FILTER_COMPLETED:
                return task.isCompleted;
            case Constants.FILTER_PENDING:
                return !task.isCompleted;
            case Constants.FILTER_STARRED:
                return task.isStarred;
            case Constants.FILTER_UPCOMING:
                return !task.isCompleted && task.dueDate != null && task.dueDate > todayEnd && task.dueDate <= upcomingEnd;
            case Constants.FILTER_INBOX:
                return task.projectId == null && !task.isCompleted;
            case Constants.FILTER_NO_DATE:
                return task.dueDate == null;
            case Constants.FILTER_HIGH_PRIORITY:
                return task.priority >= Constants.PRIORITY_HIGH;
            case Constants.FILTER_TRASH:
                return true;
            case Constants.FILTER_ARCHIVED:
            case Constants.FILTER_ALL:
            default:
                return true;
        }
    }
}
