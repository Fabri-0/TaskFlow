package com.taskflow.ui.task;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.taskflow.R;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.ui.main.CelebrationView;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;
import com.taskflow.utils.ProgressUtils;

public class TaskDetailActivity extends AppCompatActivity {
    private TaskViewModel viewModel;
    private TextView textDetailTitle;
    private TextView textDetailDescription;
    private TextView textDetailMeta;
    private TextView textDetailProgress;
    private LinearProgressIndicator progressDetailSubtasks;
    private MaterialButton buttonToggleComplete;
    private SubtaskAdapter subtaskAdapter;
    private long taskId;
    private TaskFull currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        taskId = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, -1L);
        if (taskId <= 0) {
            finish();
            return;
        }
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        bindViews();
        viewModel.getTask(taskId).observe(this, this::render);
        viewModel.getDeleted().observe(this, deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                finish();
            }
        });
        viewModel.getError().observe(this, message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void bindViews() {
        textDetailTitle = findViewById(R.id.textDetailTitle);
        textDetailDescription = findViewById(R.id.textDetailDescription);
        textDetailMeta = findViewById(R.id.textDetailMeta);
        textDetailProgress = findViewById(R.id.textDetailProgress);
        progressDetailSubtasks = findViewById(R.id.progressDetailSubtasks);
        buttonToggleComplete = findViewById(R.id.buttonToggleComplete);
        RecyclerView recyclerSubtasks = findViewById(R.id.recyclerSubtasks);
        recyclerSubtasks.setLayoutManager(new LinearLayoutManager(this));
        subtaskAdapter = new SubtaskAdapter((subtask, completed) -> viewModel.toggleSubtask(subtask.id, completed));
        recyclerSubtasks.setAdapter(subtaskAdapter);
        findViewById(R.id.buttonEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskFormActivity.class);
            intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
            startActivity(intent);
        });
        findViewById(R.id.buttonDelete).setOnClickListener(v -> confirmDelete());
        buttonToggleComplete.setOnClickListener(v -> {
            if (currentTask != null && currentTask.task != null) {
                boolean completed = !currentTask.task.isCompleted;
                viewModel.toggleCompleted(taskId, completed);
                playCelebration(completed ? "Completada 🎉" : "Reabierta 🔁",
                        completed ? CelebrationView.completePalette() : CelebrationView.reopenPalette());
            }
        });
    }

    private void render(TaskFull taskFull) {
        if (taskFull == null || taskFull.task == null) {
            return;
        }
        currentTask = taskFull;
        textDetailTitle.setText(taskFull.task.title);
        textDetailDescription.setText(taskFull.task.description == null || taskFull.task.description.isEmpty()
                ? "Sin descripcion"
                : taskFull.task.description);
        String project = taskFull.project == null ? "Sin categoria" : taskFull.project.name;
        textDetailMeta.setText("📅 " + DateUtils.formatDateTime(taskFull.task.dueDate) + "\n🗂️ " + project);
        int done = ProgressUtils.completedSubtasks(taskFull.subtasks);
        int total = taskFull.subtasks == null ? 0 : taskFull.subtasks.size();
        int progress = total == 0 ? (taskFull.task.isCompleted ? 100 : 0) : ProgressUtils.calculatePercentage(done, total);
        textDetailProgress.setText(total == 0
                ? (taskFull.task.isCompleted ? "✅ Tarea completada" : "✨ Sin subtareas, lista para enfocarte")
                : "🚀 Progreso: " + ProgressUtils.formatCounter(done, total) + " (" + progress + "%)");
        progressDetailSubtasks.setProgress(progress);
        buttonToggleComplete.setText(taskFull.task.isCompleted ? "Reabrir" : "Completar");
        buttonToggleComplete.setBackgroundTintList(ColorStateList.valueOf(getColor(
                taskFull.task.isCompleted ? R.color.accent_blue : R.color.success
        )));
        subtaskAdapter.submit(taskFull.subtasks);
    }

    private void confirmDelete() {
        android.view.View content = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null, false);
        TextView textDeleteTitle = content.findViewById(R.id.textDeleteTitle);
        TextView textDeleteMessage = content.findViewById(R.id.textDeleteMessage);
        MaterialButton buttonCancel = content.findViewById(R.id.buttonDeleteCancel);
        MaterialButton buttonConfirm = content.findViewById(R.id.buttonDeleteConfirm);
        textDeleteTitle.setText("Eliminar tarea 🗑️");
        textDeleteMessage.setText("Tambien se eliminaran sus subtareas y etiquetas asociadas.");
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonConfirm.setOnClickListener(v -> {
            viewModel.deleteTask(taskId);
            dialog.dismiss();
        });
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.setView(content);
        dialog.show();
    }

    private void playCelebration(String message, CelebrationView.Palette palette) {
        CelebrationView view = new CelebrationView(this);
        addContentView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        view.bringToFront();
        view.play(message, palette);
    }
}
