package com.taskflow.ui.task;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.R;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.notifications.ReminderScheduler;
import com.taskflow.ui.common.TaskFlowPickerDialogs;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class TaskFormActivity extends AppCompatActivity {
    private static final String NO_PROJECT = "Sin categoria";
    private static final String NO_TAG = "Sin etiqueta";

    private TaskViewModel viewModel;
    private TextInputLayout inputTitle;
    private TextInputEditText editTitle;
    private TextInputEditText editDescription;
    private MaterialAutoCompleteTextView dropdownProject;
    private MaterialAutoCompleteTextView dropdownTag;
    private LinearLayout subtaskInputList;
    private final List<TextInputEditText> subtaskInputs = new ArrayList<>();
    private CheckBox checkStarred;
    private MaterialButton buttonDueDate;
    private MaterialButton buttonReminder;
    private long taskId = -1L;
    private Long dueDate;
    private Long reminderDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_form);
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskId = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, -1L);
        bindViews();
        if (taskId > 0) {
            ((TextView) findViewById(R.id.textFormTitle)).setText("Editar tarea ✨");
            ((TextView) findViewById(R.id.textFormSubtitle)).setText("Ajusta lo importante sin perder el ritmo: pasos, avisos y organizacion.");
            ((MaterialButton) findViewById(R.id.buttonSaveTask)).setText("Guardar cambios");
            viewModel.getTask(taskId).observe(this, this::renderTask);
        }
        prepareDropdown(dropdownProject);
        prepareDropdown(dropdownTag);
        viewModel.getProjects().observe(this, this::populateProjectDropdown);
        viewModel.getTags().observe(this, this::populateTagDropdown);
        findViewById(R.id.buttonAddSubtask).setOnClickListener(v -> addSubtaskField(""));
        buttonDueDate.setOnClickListener(v -> pickDate(value -> {
            dueDate = value;
            buttonDueDate.setText(DateUtils.formatDate(dueDate));
        }));
        buttonReminder.setOnClickListener(v -> {
            requestNotificationPermissionIfNeeded();
            pickDateTime(value -> {
                reminderDate = value;
                buttonReminder.setText(DateUtils.formatDateTime(reminderDate));
            });
        });
        findViewById(R.id.buttonSaveTask).setOnClickListener(v -> save());
        viewModel.getSaved().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                finish();
            }
        });
        viewModel.getError().observe(this, message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void bindViews() {
        inputTitle = findViewById(R.id.inputTitle);
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        dropdownProject = findViewById(R.id.dropdownProject);
        dropdownTag = findViewById(R.id.dropdownTag);
        subtaskInputList = findViewById(R.id.subtaskInputList);
        checkStarred = findViewById(R.id.checkStarred);
        buttonDueDate = findViewById(R.id.buttonDueDate);
        buttonReminder = findViewById(R.id.buttonReminder);
    }

    private void renderTask(TaskFull taskFull) {
        if (taskFull == null || taskFull.task == null) {
            return;
        }
        editTitle.setText(taskFull.task.title);
        editDescription.setText(taskFull.task.description);
        dropdownProject.setText(taskFull.project == null ? NO_PROJECT : taskFull.project.name, false);
        dropdownTag.setText(taskFull.tags == null || taskFull.tags.isEmpty() ? NO_TAG : taskFull.tags.get(0).name, false);
        subtaskInputList.removeAllViews();
        subtaskInputs.clear();
        if (taskFull.subtasks != null) {
            for (SubtaskEntity subtask : taskFull.subtasks) {
                addSubtaskField(subtask.title);
            }
        }
        checkStarred.setChecked(taskFull.task.isStarred);
        dueDate = taskFull.task.dueDate;
        reminderDate = taskFull.task.reminderDate;
        buttonDueDate.setText(DateUtils.formatDate(dueDate));
        buttonReminder.setText(DateUtils.formatDateTime(reminderDate));
    }

    private void save() {
        inputTitle.setError(null);
        String title = text(editTitle);
        if (title.trim().isEmpty()) {
            inputTitle.setError("El titulo es obligatorio.");
            return;
        }
        if (taskId > 0) {
            warnIfReminderCannotNotify();
            viewModel.updateTask(taskId, title, text(editDescription), dueDate, reminderDate,
                    selected(dropdownProject, NO_PROJECT), selected(dropdownTag, NO_TAG),
                    collectSubtasks(), checkStarred.isChecked());
        } else {
            warnIfReminderCannotNotify();
            viewModel.createTask(title, text(editDescription), dueDate, reminderDate,
                    selected(dropdownProject, NO_PROJECT), selected(dropdownTag, NO_TAG),
                    collectSubtasks(), checkStarred.isChecked());
        }
    }

    private void addSubtaskField(String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_subtask_input, subtaskInputList, false);
        TextInputEditText editSubtask = row.findViewById(R.id.editSubtaskInput);
        MaterialButton buttonRemove = row.findViewById(R.id.buttonRemoveSubtask);
        editSubtask.setText(value);
        buttonRemove.setOnClickListener(v -> {
            subtaskInputList.removeView(row);
            subtaskInputs.remove(editSubtask);
        });
        subtaskInputs.add(editSubtask);
        subtaskInputList.addView(row);
        if (value == null || value.isEmpty()) {
            editSubtask.requestFocus();
        }
    }

    private List<String> collectSubtasks() {
        List<String> values = new ArrayList<>();
        for (TextInputEditText input : subtaskInputs) {
            String value = text(input).trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private void prepareDropdown(MaterialAutoCompleteTextView dropdown) {
        dropdown.setKeyListener(null);
        dropdown.setCursorVisible(false);
        dropdown.setThreshold(0);
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
        dropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dropdown.showDropDown();
            }
        });
    }

    private void populateProjectDropdown(List<ProjectEntity> projects) {
        List<String> names = new ArrayList<>();
        names.add(NO_PROJECT);
        if (projects != null) {
            for (ProjectEntity project : projects) {
                names.add(project.name);
            }
        }
        setDropdownItems(dropdownProject, names, NO_PROJECT);
    }

    private void populateTagDropdown(List<TagEntity> tags) {
        List<String> names = new ArrayList<>();
        names.add(NO_TAG);
        if (tags != null) {
            for (TagEntity tag : tags) {
                names.add(tag.name);
            }
        }
        setDropdownItems(dropdownTag, names, NO_TAG);
    }

    private void setDropdownItems(MaterialAutoCompleteTextView dropdown, List<String> names, String emptyLabel) {
        dropdown.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown_option, names));
        String current = dropdown.getText() == null ? "" : dropdown.getText().toString();
        if (current.trim().isEmpty() || !names.contains(current)) {
            dropdown.setText(emptyLabel, false);
        }
    }

    private String selected(MaterialAutoCompleteTextView dropdown, String emptyLabel) {
        String value = dropdown.getText() == null ? "" : dropdown.getText().toString().trim();
        return value.equals(emptyLabel) ? "" : value;
    }

    private void warnIfReminderCannotNotify() {
        if (reminderDate != null && !new ReminderScheduler(this).canNotify()) {
            Toast.makeText(this, "No hay permiso de notificaciones; la tarea se guardara sin mostrar aviso.", Toast.LENGTH_LONG).show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void pickDate(DateResult result) {
        TaskFlowPickerDialogs.showDatePicker(this, result::onSelected);
    }

    private void pickDateTime(DateResult result) {
        TaskFlowPickerDialogs.showDateTimePicker(this, result::onSelected);
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private interface DateResult {
        void onSelected(long value);
    }
}
