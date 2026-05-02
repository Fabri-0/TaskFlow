package com.taskflow.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.taskflow.R;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.data.repository.TaskRepository;
import com.taskflow.session.SessionManager;
import com.taskflow.ui.main.TaskAdapter;
import com.taskflow.ui.task.TaskDetailActivity;
import com.taskflow.ui.task.TaskViewModel;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity implements TaskAdapter.Listener {
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat selectedFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
    private TaskRepository repository;
    private TaskViewModel taskViewModel;
    private SessionManager sessionManager;
    private TaskAdapter taskAdapter;
    private CalendarDayAdapter dayAdapter;
    private TextView textMonthName;
    private TextView textSelectedDate;
    private TextView textMonthTaskCount;
    private LiveData<List<TaskFull>> currentSource;
    private List<TaskFull> allTasks = new ArrayList<>();
    private final Calendar visibleMonth = Calendar.getInstance();
    private long selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        repository = new TaskRepository(this);
        taskViewModel = new androidx.lifecycle.ViewModelProvider(this).get(TaskViewModel.class);
        sessionManager = new SessionManager(this);
        selectedDate = DateUtils.startOfToday();
        visibleMonth.setTimeInMillis(selectedDate);
        visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
        bindViews();
        observeAllTasks();
        loadDate(selectedDate);
        renderMonth();
    }

    private void bindViews() {
        textMonthName = findViewById(R.id.textMonthName);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        textMonthTaskCount = findViewById(R.id.textMonthTaskCount);
        RecyclerView recyclerDays = findViewById(R.id.recyclerCalendarDays);
        recyclerDays.setLayoutManager(new GridLayoutManager(this, 7));
        dayAdapter = new CalendarDayAdapter(day -> {
            selectedDate = DateUtils.startOfDay(day.dateMillis);
            visibleMonth.setTimeInMillis(selectedDate);
            visibleMonth.set(Calendar.DAY_OF_MONTH, 1);
            renderMonth();
            loadDate(selectedDate);
        });
        recyclerDays.setAdapter(dayAdapter);
        RecyclerView recycler = findViewById(R.id.recyclerCalendarTasks);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this);
        recycler.setAdapter(taskAdapter);
        findViewById(R.id.buttonPreviousMonth).setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, -1);
            selectedDate = DateUtils.startOfDay(visibleMonth.getTimeInMillis());
            renderMonth();
            loadDate(selectedDate);
        });
        findViewById(R.id.buttonNextMonth).setOnClickListener(v -> {
            visibleMonth.add(Calendar.MONTH, 1);
            selectedDate = DateUtils.startOfDay(visibleMonth.getTimeInMillis());
            renderMonth();
            loadDate(selectedDate);
        });
    }

    private void observeAllTasks() {
        repository.getVisibleTasks(sessionManager.getActiveUserId(), Constants.FILTER_ALL, "", null, null)
                .observe(this, tasks -> {
                    allTasks = tasks == null ? new ArrayList<>() : tasks;
                    renderMonth();
                });
    }

    private void renderMonth() {
        textMonthName.setText(monthFormat.format(visibleMonth.getTime()).toUpperCase(Locale.getDefault()));
        textSelectedDate.setText("📌 " + selectedFormat.format(selectedDate));
        Map<Long, Integer> counts = taskCountsByDay();
        List<CalendarDayAdapter.CalendarDay> days = buildMonthDays(counts);
        dayAdapter.submit(days);
        textMonthTaskCount.setText("🗓️ " + tasksInVisibleMonth() + " tareas este mes");
    }

    private List<CalendarDayAdapter.CalendarDay> buildMonthDays(Map<Long, Integer> counts) {
        List<CalendarDayAdapter.CalendarDay> days = new ArrayList<>();
        Calendar cursor = (Calendar) visibleMonth.clone();
        int firstDay = cursor.get(Calendar.DAY_OF_WEEK);
        int mondayOffset = (firstDay + 5) % 7;
        cursor.add(Calendar.DAY_OF_MONTH, -mondayOffset);
        int currentMonth = visibleMonth.get(Calendar.MONTH);
        long today = DateUtils.startOfToday();
        for (int i = 0; i < 42; i++) {
            long date = DateUtils.startOfDay(cursor.getTimeInMillis());
            days.add(new CalendarDayAdapter.CalendarDay(
                    date,
                    cursor.get(Calendar.DAY_OF_MONTH),
                    cursor.get(Calendar.MONTH) == currentMonth,
                    date == today,
                    date == DateUtils.startOfDay(selectedDate),
                    counts.containsKey(date) ? counts.get(date) : 0
            ));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private Map<Long, Integer> taskCountsByDay() {
        Map<Long, Integer> counts = new HashMap<>();
        for (TaskFull task : allTasks) {
            if (task != null && task.task != null && task.task.dueDate != null) {
                long day = DateUtils.startOfDay(task.task.dueDate);
                counts.put(day, counts.containsKey(day) ? counts.get(day) + 1 : 1);
            }
        }
        return counts;
    }

    private int tasksInVisibleMonth() {
        Calendar start = (Calendar) visibleMonth.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        long startMillis = DateUtils.startOfDay(start.getTimeInMillis());
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        long endMillis = DateUtils.startOfDay(end.getTimeInMillis());
        int count = 0;
        for (TaskFull task : allTasks) {
            if (task != null && task.task != null && task.task.dueDate != null
                    && task.task.dueDate >= startMillis && task.task.dueDate < endMillis) {
                count++;
            }
        }
        return count;
    }

    private void loadDate(long timestamp) {
        if (currentSource != null) {
            currentSource.removeObservers(this);
        }
        currentSource = repository.getTasksByDate(sessionManager.getActiveUserId(), timestamp);
        currentSource.observe(this, taskAdapter::submit);
    }

    @Override
    public void onTaskClicked(TaskFull task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, task.task.id);
        startActivity(intent);
    }

    @Override
    public void onTaskCompleted(TaskFull task, boolean completed) {
        taskViewModel.toggleCompleted(task.task.id, completed);
    }
}
