package com.taskflow.ui.project;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.R;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.session.SessionManager;

public class ProjectActivity extends AppCompatActivity {
    private ProjectViewModel viewModel;
    private SessionManager sessionManager;
    private LinearLayout projectList;
    private LinearLayout tagList;
    private TextInputEditText editProjectName;
    private TextInputEditText editTagName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        viewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        sessionManager = new SessionManager(this);
        projectList = findViewById(R.id.projectList);
        tagList = findViewById(R.id.tagList);
        editProjectName = findViewById(R.id.editProjectName);
        editTagName = findViewById(R.id.editTagName);
        long userId = sessionManager.getActiveUserId();
        findViewById(R.id.buttonCreateProject).setOnClickListener(v -> {
            viewModel.createProject(userId, text(editProjectName));
            editProjectName.setText("");
        });
        findViewById(R.id.buttonCreateTag).setOnClickListener(v -> {
            viewModel.createTag(userId, text(editTagName));
            editTagName.setText("");
        });
        viewModel.getProjects(userId).observe(this, projects -> {
            projectList.removeAllViews();
            if (projects == null || projects.isEmpty()) {
                addEmptyText(projectList, "Sin categorias todavia");
                return;
            }
            for (ProjectEntity project : projects) {
                addProjectRow(project);
            }
        });
        viewModel.getTags(userId).observe(this, tags -> {
            tagList.removeAllViews();
            if (tags == null || tags.isEmpty()) {
                addEmptyText(tagList, "Sin etiquetas todavia");
                return;
            }
            for (TagEntity tag : tags) {
                addTagRow(tag);
            }
        });
        viewModel.getError().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addProjectRow(ProjectEntity project) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_manage_entry, projectList, false);
        TextView textName = row.findViewById(R.id.textEntryName);
        MaterialButton buttonEdit = row.findViewById(R.id.buttonEditEntry);
        MaterialButton buttonDelete = row.findViewById(R.id.buttonDeleteEntry);
        textName.setText(project.name);
        buttonEdit.setOnClickListener(v -> showEditDialog("Editar categoria", project.name,
                value -> viewModel.updateProject(project, value)));
        buttonDelete.setOnClickListener(v -> confirmDelete("Eliminar categoria",
                "Las tareas de esta categoria quedaran sin categoria.",
                () -> viewModel.deleteProject(project)));
        projectList.addView(row);
    }

    private void addTagRow(TagEntity tag) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_manage_entry, tagList, false);
        TextView textName = row.findViewById(R.id.textEntryName);
        MaterialButton buttonEdit = row.findViewById(R.id.buttonEditEntry);
        MaterialButton buttonDelete = row.findViewById(R.id.buttonDeleteEntry);
        textName.setText(tag.name);
        buttonEdit.setOnClickListener(v -> showEditDialog("Editar etiqueta", tag.name,
                value -> viewModel.updateTag(tag, value)));
        buttonDelete.setOnClickListener(v -> confirmDelete("Eliminar etiqueta",
                "La etiqueta se quitara de las tareas donde este usada.",
                () -> viewModel.deleteTag(tag)));
        tagList.addView(row);
    }

    private void addEmptyText(LinearLayout target, String value) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(getColor(R.color.text_primary));
        textView.setTextSize(17f);
        textView.setPadding(20, 18, 20, 18);
        textView.setBackgroundResource(R.drawable.bg_card_dark);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        target.addView(textView, params);
    }

    private void showEditDialog(String title, String currentValue, NameResult result) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_edit_name, null, false);
        TextView textDialogTitle = content.findViewById(R.id.textDialogTitle);
        TextView textDialogMessage = content.findViewById(R.id.textDialogMessage);
        TextInputLayout inputLayout = content.findViewById(R.id.inputDialogName);
        TextInputEditText input = content.findViewById(R.id.editDialogName);
        MaterialButton buttonCancel = content.findViewById(R.id.buttonDialogCancel);
        MaterialButton buttonSave = content.findViewById(R.id.buttonDialogSave);

        textDialogTitle.setText(title + " ✨");
        textDialogMessage.setText(title.toLowerCase().contains("etiqueta")
                ? "Renombra la etiqueta y manten tus tareas bien agrupadas."
                : "Renombra la categoria y conserva tu organizacion limpia.");
        input.setText(currentValue);
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            inputLayout.setError(null);
            String value = input.getText() == null ? "" : input.getText().toString().trim();
            if (value.isEmpty()) {
                inputLayout.setError("Escribe un nombre para guardar.");
                return;
            }
            result.onName(value);
            dialog.dismiss();
        });
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            input.requestFocus();
            input.post(input::selectAll);
        });
        dialog.setView(content);
        dialog.show();
    }

    private void confirmDelete(String title, String message, DeleteAction action) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null, false);
        TextView textDeleteTitle = content.findViewById(R.id.textDeleteTitle);
        TextView textDeleteMessage = content.findViewById(R.id.textDeleteMessage);
        MaterialButton buttonCancel = content.findViewById(R.id.buttonDeleteCancel);
        MaterialButton buttonConfirm = content.findViewById(R.id.buttonDeleteConfirm);

        textDeleteTitle.setText(title + " 🗑️");
        textDeleteMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonConfirm.setOnClickListener(v -> {
            action.run();
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

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private interface NameResult {
        void onName(String value);
    }

    private interface DeleteAction {
        void run();
    }
}
