package com.taskflow.ui.task;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.R;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.notifications.ReminderScheduler;
import com.taskflow.ui.common.TaskFlowPickerDialogs;
import com.taskflow.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class QuickTaskBottomSheet extends BottomSheetDialogFragment {
    private static final String NO_PROJECT = "Sin categoria";
    private static final String NO_TAG = "Sin etiqueta";

    private TaskViewModel viewModel;
    private TextInputLayout inputQuickTitle;
    private TextInputEditText editQuickTitle;
    private MaterialAutoCompleteTextView dropdownQuickProject;
    private MaterialAutoCompleteTextView dropdownQuickTag;
    private LinearLayout quickSubtaskList;
    private final List<TextInputEditText> subtaskInputs = new ArrayList<>();
    private MaterialButton buttonQuickDueDate;
    private MaterialButton buttonQuickReminder;
    private Long dueDate;
    private Long reminderDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_quick_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        inputQuickTitle = view.findViewById(R.id.inputQuickTitle);
        editQuickTitle = view.findViewById(R.id.editQuickTitle);
        dropdownQuickProject = view.findViewById(R.id.dropdownQuickProject);
        dropdownQuickTag = view.findViewById(R.id.dropdownQuickTag);
        quickSubtaskList = view.findViewById(R.id.quickSubtaskList);
        buttonQuickDueDate = view.findViewById(R.id.buttonQuickDueDate);
        buttonQuickReminder = view.findViewById(R.id.buttonQuickReminder);
        MaterialButton buttonQuickAddSubtask = view.findViewById(R.id.buttonQuickAddSubtask);
        MaterialButton buttonQuickSave = view.findViewById(R.id.buttonQuickSave);

        prepareDropdown(dropdownQuickProject);
        prepareDropdown(dropdownQuickTag);
        viewModel.getProjects().observe(getViewLifecycleOwner(), this::populateProjectDropdown);
        viewModel.getTags().observe(getViewLifecycleOwner(), this::populateTagDropdown);
        buttonQuickAddSubtask.setOnClickListener(v -> addSubtaskField(""));
        buttonQuickDueDate.setOnClickListener(v -> pickDate(value -> {
            dueDate = value;
            buttonQuickDueDate.setText(DateUtils.formatDate(dueDate));
        }));
        buttonQuickReminder.setOnClickListener(v -> {
            requestNotificationPermissionIfNeeded();
            pickDateTime(value -> {
                reminderDate = value;
                buttonQuickReminder.setText(DateUtils.formatDateTime(reminderDate));
            });
        });
        buttonQuickSave.setOnClickListener(v -> save());
        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) {
                getParentFragmentManager().setFragmentResult("quick_task_saved", new Bundle());
                dismissAllowingStateLoss();
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void save() {
        inputQuickTitle.setError(null);
        String title = text(editQuickTitle);
        if (title.trim().isEmpty()) {
            inputQuickTitle.setError("El titulo es obligatorio.");
            return;
        }
        if (reminderDate != null && !new ReminderScheduler(requireContext()).canNotify()) {
            Toast.makeText(requireContext(), "No hay permiso de notificaciones; la tarea se guardara sin mostrar aviso.", Toast.LENGTH_LONG).show();
        }
        viewModel.createTask(title, "", dueDate, reminderDate,
                selected(dropdownQuickProject, NO_PROJECT),
                selected(dropdownQuickTag, NO_TAG),
                collectSubtasks(),
                false);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void addSubtaskField(String value) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_subtask_input, quickSubtaskList, false);
        TextInputEditText editSubtask = row.findViewById(R.id.editSubtaskInput);
        MaterialButton buttonRemove = row.findViewById(R.id.buttonRemoveSubtask);
        editSubtask.setText(value);
        buttonRemove.setOnClickListener(v -> {
            quickSubtaskList.removeView(row);
            subtaskInputs.remove(editSubtask);
        });
        subtaskInputs.add(editSubtask);
        quickSubtaskList.addView(row);
        editSubtask.requestFocus();
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
        setDropdownItems(dropdownQuickProject, names, NO_PROJECT);
    }

    private void populateTagDropdown(List<TagEntity> tags) {
        List<String> names = new ArrayList<>();
        names.add(NO_TAG);
        if (tags != null) {
            for (TagEntity tag : tags) {
                names.add(tag.name);
            }
        }
        setDropdownItems(dropdownQuickTag, names, NO_TAG);
    }

    private void setDropdownItems(MaterialAutoCompleteTextView dropdown, List<String> names, String emptyLabel) {
        dropdown.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown_option, names));
        String current = dropdown.getText() == null ? "" : dropdown.getText().toString();
        if (current.trim().isEmpty() || !names.contains(current)) {
            dropdown.setText(emptyLabel, false);
        }
    }

    private String selected(MaterialAutoCompleteTextView dropdown, String emptyLabel) {
        String value = dropdown.getText() == null ? "" : dropdown.getText().toString().trim();
        return value.equals(emptyLabel) ? "" : value;
    }

    private void pickDate(DateResult result) {
        TaskFlowPickerDialogs.showDatePicker(requireContext(), result::onSelected);
    }

    private void pickDateTime(DateResult result) {
        TaskFlowPickerDialogs.showDateTimePicker(requireContext(), result::onSelected);
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private interface DateResult {
        void onSelected(long value);
    }
}
