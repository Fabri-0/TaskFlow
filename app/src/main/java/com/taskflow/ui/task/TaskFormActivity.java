package com.taskflow.ui.task;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.R;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskTemplateEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.notifications.ReminderPermissionHelper;
import com.taskflow.ui.common.TaskFlowPickerDialogs;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskFormActivity extends AppCompatActivity {
    private static final int REQUEST_IMPORT_IMAGE = 91;
    private static final String NO_PROJECT = "Sin categoria";
    private static final String NO_TEMPLATE = "Sin plantilla";

    private TaskViewModel viewModel;
    private TextInputLayout inputTitle;
    private TextInputEditText editTitle;
    private TextInputEditText editDescription;
    private TextInputEditText editTags;
    private TextInputEditText editAttachment;
    private TextInputEditText editEstimatedMinutes;
    private TextView textSelectedImage;
    private MaterialAutoCompleteTextView dropdownTemplate;
    private MaterialAutoCompleteTextView dropdownProject;
    private MaterialAutoCompleteTextView dropdownPriority;
    private MaterialAutoCompleteTextView dropdownBoard;
    private MaterialAutoCompleteTextView dropdownRecurrence;
    private LinearLayout subtaskInputList;
    private final List<TextInputEditText> subtaskInputs = new ArrayList<>();
    private final Map<String, TaskTemplateEntity> templatesByName = new HashMap<>();
    private final List<String> availableTagNames = new ArrayList<>();
    private final List<String> selectedTagNames = new ArrayList<>();
    private CheckBox checkStarred;
    private MaterialButton buttonDueDate;
    private MaterialButton buttonReminder;
    private MaterialButton buttonReminder2;
    private MaterialButton buttonReminder3;
    private long taskId = -1L;
    private Long dueDate;
    private Long reminderDate;
    private Long reminderDate2;
    private Long reminderDate3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_form);
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskId = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, -1L);
        bindViews();
        prepareDropdown(dropdownTemplate);
        prepareDropdown(dropdownProject);
        prepareDropdown(dropdownPriority);
        prepareDropdown(dropdownBoard);
        prepareDropdown(dropdownRecurrence);
        setupFixedDropdowns();
        setupTagSelector();
        if (taskId > 0) {
            ((TextView) findViewById(R.id.textFormTitle)).setText("Editar tarea");
            ((TextView) findViewById(R.id.textFormSubtitle)).setText("Ajusta contenido, prioridad, avisos, recurrencia y tablero.");
            ((MaterialButton) findViewById(R.id.buttonSaveTask)).setText("Guardar cambios");
            viewModel.getTask(taskId).observe(this, this::renderTask);
        }
        viewModel.getProjects().observe(this, this::populateProjectDropdown);
        viewModel.getTags().observe(this, this::populateTagSelector);
        viewModel.getTemplates().observe(this, this::populateTemplates);
        findViewById(R.id.buttonAddSubtask).setOnClickListener(v -> addSubtaskField(""));
        findViewById(R.id.buttonImportImage).setOnClickListener(v -> pickImage());
        buttonDueDate.setOnClickListener(v -> pickDate(value -> {
            dueDate = value;
            buttonDueDate.setText(DateUtils.formatDate(dueDate));
        }));
        buttonReminder.setOnClickListener(v -> pickReminder(1));
        buttonReminder2.setOnClickListener(v -> pickReminder(2));
        buttonReminder3.setOnClickListener(v -> pickReminder(3));
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
        editTags = findViewById(R.id.dropdownTag);
        editAttachment = findViewById(R.id.editAttachment);
        editEstimatedMinutes = findViewById(R.id.editEstimatedMinutes);
        textSelectedImage = findViewById(R.id.textSelectedImage);
        dropdownTemplate = findViewById(R.id.dropdownTemplate);
        dropdownProject = findViewById(R.id.dropdownProject);
        dropdownPriority = findViewById(R.id.dropdownPriority);
        dropdownBoard = findViewById(R.id.dropdownBoard);
        dropdownRecurrence = findViewById(R.id.dropdownRecurrence);
        subtaskInputList = findViewById(R.id.subtaskInputList);
        checkStarred = findViewById(R.id.checkStarred);
        buttonDueDate = findViewById(R.id.buttonDueDate);
        buttonReminder = findViewById(R.id.buttonReminder);
        buttonReminder2 = findViewById(R.id.buttonReminder2);
        buttonReminder3 = findViewById(R.id.buttonReminder3);
    }

    private void setupFixedDropdowns() {
        setDropdownItems(dropdownPriority, list("Baja", "Media", "Alta", "Urgente"), "Baja");
        setDropdownItems(dropdownBoard, list("Por hacer", "En progreso", "Listo"), "Por hacer");
        setDropdownItems(dropdownRecurrence, list("No se repite", "Cada dia", "Cada semana", "Cada mes"), "No se repite");
        dropdownTemplate.setOnItemClickListener((parent, view, position, id) -> {
            String selected = dropdownTemplate.getText() == null ? "" : dropdownTemplate.getText().toString();
            TaskTemplateEntity template = templatesByName.get(selected);
            if (template != null && taskId <= 0) {
                applyTemplate(template);
            }
        });
    }

    private void setupTagSelector() {
        editTags.setKeyListener(null);
        editTags.setCursorVisible(false);
        editTags.setOnClickListener(v -> showTagSelectionDialog());
        editTags.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showTagSelectionDialog();
            }
        });
    }

    private void renderTask(TaskFull taskFull) {
        if (taskFull == null || taskFull.task == null) {
            return;
        }
        editTitle.setText(taskFull.task.title);
        editDescription.setText(taskFull.task.description);
        dropdownProject.setText(taskFull.project == null ? NO_PROJECT : taskFull.project.name, false);
        selectedTagNames.clear();
        selectedTagNames.addAll(tagNames(taskFull.tags));
        updateSelectedTagsText();
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
        reminderDate2 = taskFull.task.reminderDate2;
        reminderDate3 = taskFull.task.reminderDate3;
        buttonDueDate.setText(DateUtils.formatDate(dueDate));
        buttonReminder.setText(DateUtils.formatDateTime(reminderDate));
        buttonReminder2.setText(DateUtils.formatDateTime(reminderDate2));
        buttonReminder3.setText(DateUtils.formatDateTime(reminderDate3));
        dropdownPriority.setText(priorityLabel(taskFull.task.priority), false);
        dropdownBoard.setText(boardLabel(taskFull.task.boardColumn), false);
        dropdownRecurrence.setText(recurrenceLabel(taskFull.task.recurrenceType), false);
        editAttachment.setText(taskFull.task.attachment);
        updateSelectedImageText();
        editEstimatedMinutes.setText(String.valueOf(taskFull.task.estimatedMinutes));
    }

    private void save() {
        inputTitle.setError(null);
        String title = text(editTitle);
        if (title.trim().isEmpty()) {
            inputTitle.setError("El titulo es obligatorio.");
            return;
        }
        if (hasReminder()) {
            ReminderPermissionHelper.ensureReminderAccess(this);
        }
        warnIfReminderCannotNotify();
        if (taskId > 0) {
            viewModel.updateTask(taskId, title, text(editDescription), dueDate, reminderDate, reminderDate2, reminderDate3,
                    selected(dropdownProject, NO_PROJECT), collectTags(), collectSubtasks(), checkStarred.isChecked(),
                    selectedPriority(), selectedRecurrence(), 1, selectedBoard(), text(editAttachment), selectedMinutes());
        } else {
            viewModel.createTask(title, text(editDescription), dueDate, reminderDate, reminderDate2, reminderDate3,
                    selected(dropdownProject, NO_PROJECT), collectTags(), collectSubtasks(), checkStarred.isChecked(),
                    selectedPriority(), selectedRecurrence(), 1, selectedBoard(), text(editAttachment), selectedMinutes());
        }
    }

    private void applyTemplate(TaskTemplateEntity template) {
        editTitle.setText(template.title);
        editDescription.setText(template.description);
        dropdownProject.setText(TextUtils.isEmpty(template.projectName) ? NO_PROJECT : template.projectName, false);
        selectedTagNames.clear();
        for (String tag : splitCsv(template.tagsCsv)) {
            if (availableTagNames.contains(tag)) {
                selectedTagNames.add(tag);
            }
        }
        updateSelectedTagsText();
        editAttachment.setText(template.attachment);
        updateSelectedImageText();
        editEstimatedMinutes.setText(String.valueOf(template.estimatedMinutes));
        dropdownPriority.setText(priorityLabel(template.priority), false);
        dropdownBoard.setText(boardLabel(template.boardColumn), false);
        dropdownRecurrence.setText(recurrenceLabel(template.recurrenceType), false);
        subtaskInputList.removeAllViews();
        subtaskInputs.clear();
        for (String subtask : splitLines(template.subtasksCsv)) {
            addSubtaskField(subtask);
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

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMPORT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
        }
        editAttachment.setText(uri.toString());
        updateSelectedImageText();
    }

    private void updateSelectedImageText() {
        String value = text(editAttachment);
        if (TextUtils.isEmpty(value)) {
            textSelectedImage.setText("🖼️ Sin imagen");
            textSelectedImage.setTextColor(getColor(R.color.text_secondary));
        } else {
            textSelectedImage.setText("🖼️ Imagen importada");
            textSelectedImage.setTextColor(getColor(R.color.text_primary));
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

    private List<String> collectTags() {
        return new ArrayList<>(selectedTagNames);
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

    private void populateTemplates(List<TaskTemplateEntity> templates) {
        List<String> names = new ArrayList<>();
        templatesByName.clear();
        names.add(NO_TEMPLATE);
        if (templates != null) {
            for (TaskTemplateEntity template : templates) {
                names.add(template.name);
                templatesByName.put(template.name, template);
            }
        }
        setDropdownItems(dropdownTemplate, names, NO_TEMPLATE);
    }

    private void populateTagSelector(List<TagEntity> tags) {
        availableTagNames.clear();
        if (tags == null || tags.isEmpty()) {
            updateSelectedTagsText();
            return;
        }
        for (TagEntity tag : tags) {
            if (tag != null && !TextUtils.isEmpty(tag.name)) {
                availableTagNames.add(tag.name);
            }
        }
        selectedTagNames.removeIf(name -> !availableTagNames.contains(name));
        updateSelectedTagsText();
    }

    private void showTagSelectionDialog() {
        if (availableTagNames.isEmpty()) {
            Toast.makeText(this, "Primero crea etiquetas desde Organizacion.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = availableTagNames.toArray(new String[0]);
        boolean[] checked = new boolean[names.length];
        for (int i = 0; i < names.length; i++) {
            checked[i] = selectedTagNames.contains(names[i]);
        }
        List<String> pendingSelection = new ArrayList<>(selectedTagNames);
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar etiquetas")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    String name = names[which];
                    if (isChecked && !pendingSelection.contains(name)) {
                        pendingSelection.add(name);
                    } else if (!isChecked) {
                        pendingSelection.remove(name);
                    }
                })
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    selectedTagNames.clear();
                    selectedTagNames.addAll(pendingSelection);
                    updateSelectedTagsText();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateSelectedTagsText() {
        editTags.setText(selectedTagNames.isEmpty() ? "Sin etiquetas" : TextUtils.join(", ", selectedTagNames));
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

    private int selectedPriority() {
        String value = text(dropdownPriority);
        if ("Urgente".equals(value)) {
            return Constants.PRIORITY_URGENT;
        }
        if ("Alta".equals(value)) {
            return Constants.PRIORITY_HIGH;
        }
        if ("Media".equals(value)) {
            return Constants.PRIORITY_MEDIUM;
        }
        return Constants.PRIORITY_LOW;
    }

    private String selectedRecurrence() {
        String value = text(dropdownRecurrence);
        if ("Cada dia".equals(value)) {
            return Constants.RECURRENCE_DAILY;
        }
        if ("Cada semana".equals(value)) {
            return Constants.RECURRENCE_WEEKLY;
        }
        if ("Cada mes".equals(value)) {
            return Constants.RECURRENCE_MONTHLY;
        }
        return Constants.RECURRENCE_NONE;
    }

    private String selectedBoard() {
        String value = text(dropdownBoard);
        if ("En progreso".equals(value)) {
            return Constants.BOARD_DOING;
        }
        if ("Listo".equals(value)) {
            return Constants.BOARD_DONE;
        }
        return Constants.BOARD_TODO;
    }

    private int selectedMinutes() {
        try {
            return Integer.parseInt(text(editEstimatedMinutes).trim());
        } catch (NumberFormatException e) {
            return 25;
        }
    }

    private String priorityLabel(int priority) {
        if (priority >= Constants.PRIORITY_URGENT) {
            return "Urgente";
        }
        if (priority >= Constants.PRIORITY_HIGH) {
            return "Alta";
        }
        if (priority >= Constants.PRIORITY_MEDIUM) {
            return "Media";
        }
        return "Baja";
    }

    private String boardLabel(String boardColumn) {
        if (Constants.BOARD_DOING.equals(boardColumn)) {
            return "En progreso";
        }
        if (Constants.BOARD_DONE.equals(boardColumn)) {
            return "Listo";
        }
        return "Por hacer";
    }

    private String recurrenceLabel(String recurrenceType) {
        if (Constants.RECURRENCE_DAILY.equals(recurrenceType)) {
            return "Cada dia";
        }
        if (Constants.RECURRENCE_WEEKLY.equals(recurrenceType)) {
            return "Cada semana";
        }
        if (Constants.RECURRENCE_MONTHLY.equals(recurrenceType)) {
            return "Cada mes";
        }
        return "No se repite";
    }

    private String tagsText(List<TagEntity> tags) {
        List<String> names = tagNames(tags);
        return TextUtils.join(", ", names);
    }

    private List<String> tagNames(List<TagEntity> tags) {
        List<String> names = new ArrayList<>();
        if (tags == null || tags.isEmpty()) {
            return names;
        }
        for (TagEntity tag : tags) {
            if (tag != null && !TextUtils.isEmpty(tag.name)) {
                names.add(tag.name);
            }
        }
        return names;
    }

    private List<String> splitCsv(String text) {
        List<String> values = new ArrayList<>();
        if (TextUtils.isEmpty(text)) {
            return values;
        }
        for (String item : text.split(",")) {
            String clean = item.trim();
            if (!clean.isEmpty()) {
                values.add(clean);
            }
        }
        return values;
    }

    private List<String> splitLines(String text) {
        List<String> values = new ArrayList<>();
        if (TextUtils.isEmpty(text)) {
            return values;
        }
        for (String item : text.split("\\r?\\n")) {
            String clean = item.trim();
            if (!clean.isEmpty()) {
                values.add(clean);
            }
        }
        return values;
    }

    private void warnIfReminderCannotNotify() {
        if (hasReminder() && !ReminderPermissionHelper.hasReminderAccess(this)) {
            Toast.makeText(this, "Faltan permisos de alarmas. Activalos para que la alarma suene aunque la app este cerrada.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasReminder() {
        return reminderDate != null || reminderDate2 != null || reminderDate3 != null;
    }

    private void pickReminder(int slot) {
        ReminderPermissionHelper.ensureReminderAccess(this);
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        labels.add("Elegir fecha y hora");
        values.add(null);
        addPreset(labels, values, "En 1 hora", 1L * 60L * 60L * 1000L);
        addPreset(labels, values, "En 5 horas", 5L * 60L * 60L * 1000L);
        addPreset(labels, values, "En 12 horas", 12L * 60L * 60L * 1000L);
        addPreset(labels, values, "En 2 dias", 2L * 24L * 60L * 60L * 1000L);
        addPreset(labels, values, "En 3 dias", 3L * 24L * 60L * 60L * 1000L);
        if (dueDate != null) {
            addBeforeDue(labels, values, "1 hora antes de la fecha limite", 1L * 60L * 60L * 1000L);
            addBeforeDue(labels, values, "5 horas antes de la fecha limite", 5L * 60L * 60L * 1000L);
            addBeforeDue(labels, values, "12 horas antes de la fecha limite", 12L * 60L * 60L * 1000L);
            addBeforeDue(labels, values, "2 dias antes de la fecha limite", 2L * 24L * 60L * 60L * 1000L);
            addBeforeDue(labels, values, "3 dias antes de la fecha limite", 3L * 24L * 60L * 60L * 1000L);
        }
        new AlertDialog.Builder(this)
                .setTitle("Configurar alarma")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    Long value = values.get(which);
                    if (value == null) {
                        pickDateTime(selected -> setReminder(slot, selected));
                    } else {
                        setReminder(slot, value);
                    }
                })
                .show();
    }

    private void addPreset(List<String> labels, List<Long> values, String label, long offset) {
        labels.add(label);
        values.add(System.currentTimeMillis() + offset);
    }

    private void addBeforeDue(List<String> labels, List<Long> values, String label, long offset) {
        long value = dueDate - offset;
        if (value > System.currentTimeMillis()) {
            labels.add(label);
            values.add(value);
        }
    }

    private void setReminder(int slot, long value) {
        if (value <= System.currentTimeMillis()) {
            Toast.makeText(this, "La alarma debe quedar en el futuro.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (slot == 1) {
            reminderDate = value;
            buttonReminder.setText(DateUtils.formatDateTime(reminderDate));
        } else if (slot == 2) {
            reminderDate2 = value;
            buttonReminder2.setText(DateUtils.formatDateTime(reminderDate2));
        } else {
            reminderDate3 = value;
            buttonReminder3.setText(DateUtils.formatDateTime(reminderDate3));
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

    private String text(MaterialAutoCompleteTextView editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private List<String> list(String... values) {
        List<String> list = new ArrayList<>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }

    private interface DateResult {
        void onSelected(long value);
    }
}
