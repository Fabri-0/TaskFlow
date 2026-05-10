package com.taskflow.ui.task;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.taskflow.R;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.ui.main.CelebrationView;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;
import com.taskflow.utils.ProgressUtils;

public class TaskDetailActivity extends AppCompatActivity {
    private TaskViewModel viewModel;
    private TextView textDetailTitle;
    private TextView textDetailDescription;
    private TextView textDetailProgress;
    private TextView textImageAttachment;
    private ImageView imageDetailAttachment;
    private LinearLayout subtasksPanel;
    private LinearLayout imageAttachmentPanel;
    private LinearLayout detailInfoContainer;
    private LinearProgressIndicator progressDetailSubtasks;
    private SubtaskAdapter subtaskAdapter;
    private long taskId;
    private TaskFull currentTask;
    private Uri currentImageUri;
    private PopupWindow actionPopup;

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
        textDetailProgress = findViewById(R.id.textDetailProgress);
        textImageAttachment = findViewById(R.id.textImageAttachment);
        imageDetailAttachment = findViewById(R.id.imageDetailAttachment);
        subtasksPanel = findViewById(R.id.subtasksPanel);
        imageAttachmentPanel = findViewById(R.id.imageAttachmentPanel);
        detailInfoContainer = findViewById(R.id.detailInfoContainer);
        progressDetailSubtasks = findViewById(R.id.progressDetailSubtasks);
        RecyclerView recyclerSubtasks = findViewById(R.id.recyclerSubtasks);
        recyclerSubtasks.setLayoutManager(new LinearLayoutManager(this));
        subtaskAdapter = new SubtaskAdapter((subtask, completed) -> viewModel.toggleSubtask(subtask.id, completed));
        recyclerSubtasks.setAdapter(subtaskAdapter);
        FloatingActionButton fabTaskActions = findViewById(R.id.fabTaskActions);
        fabTaskActions.setOnClickListener(this::showActionMenu);
        imageDetailAttachment.setOnClickListener(v -> showImagePreview());
    }

    private void render(TaskFull taskFull) {
        if (taskFull == null || taskFull.task == null) {
            return;
        }
        currentTask = taskFull;
        textDetailTitle.setText(taskFull.task.title);
        if (TextUtils.isEmpty(taskFull.task.description)) {
            textDetailDescription.setVisibility(View.GONE);
        } else {
            textDetailDescription.setVisibility(View.VISIBLE);
            textDetailDescription.setText(taskFull.task.description);
        }
        renderImage(taskFull.task.attachment);
        renderInfoCards(taskFull);

        int done = ProgressUtils.completedSubtasks(taskFull.subtasks);
        int total = taskFull.subtasks == null ? 0 : taskFull.subtasks.size();
        int progress = total == 0 ? (taskFull.task.isCompleted ? 100 : 0) : ProgressUtils.calculatePercentage(done, total);
        if (total == 0) {
            subtasksPanel.setVisibility(View.GONE);
            subtaskAdapter.submit(null);
        } else {
            subtasksPanel.setVisibility(View.VISIBLE);
            textDetailProgress.setText("☘️ Subtareas: " + ProgressUtils.formatCounter(done, total) + " (" + progress + "%)");
            progressDetailSubtasks.setProgress(progress);
            subtaskAdapter.submit(taskFull.subtasks);
        }
    }

    private void renderImage(String attachment) {
        if (!isImageUri(attachment)) {
            currentImageUri = null;
            imageAttachmentPanel.setVisibility(View.GONE);
            return;
        }
        try {
            currentImageUri = Uri.parse(attachment);
            imageDetailAttachment.setImageURI(currentImageUri);
            imageAttachmentPanel.setVisibility(View.VISIBLE);
            textImageAttachment.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            currentImageUri = null;
            imageAttachmentPanel.setVisibility(View.GONE);
        }
    }

    private void showImagePreview() {
        if (currentImageUri == null) {
            return;
        }
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.rgb(12, 12, 18));
        ImageView preview = new ImageView(this);
        preview.setImageURI(currentImageUri);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        frame.addView(preview, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_close_24);
        close.setBackgroundResource(R.drawable.bg_profile_glass);
        close.setColorFilter(ContextCompat.getColor(this, R.color.on_primary));
        close.setPadding(dp(10), dp(10), dp(10), dp(10));
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.TOP | Gravity.END);
        closeParams.setMargins(0, dp(18), dp(18), 0);
        frame.addView(close, closeParams);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setView(frame);
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        });
        dialog.show();
    }

    private boolean isImageUri(String value) {
        return !TextUtils.isEmpty(value) && (value.startsWith("content://") || value.startsWith("file://"));
    }

    private void renderInfoCards(TaskFull taskFull) {
        detailInfoContainer.removeAllViews();
        if (taskFull.task.dueDate != null) {
            addInfoCard("📅 Fecha", DateUtils.formatDateTime(taskFull.task.dueDate), R.color.accent_blue_dark);
        }
        addInfoCard("🚩 Prioridad", priorityLabel(taskFull.task.priority), priorityColor(taskFull.task.priority));
        if (taskFull.project != null && !TextUtils.isEmpty(taskFull.project.name)) {
            addInfoCard("🗂️ Categoria", taskFull.project.name, R.color.accent_teal_dark);
        }
        if (taskFull.tags != null && !taskFull.tags.isEmpty()) {
            addInfoCard("🏷️ Etiquetas", tagsText(taskFull), R.color.primary_purple_dark);
        }
        if (!TextUtils.isEmpty(taskFull.task.boardColumn)) {
            addInfoCard("🧩 Tablero", boardLabel(taskFull.task.boardColumn), R.color.success_dark);
        }
        if (!TextUtils.isEmpty(taskFull.task.recurrenceType)) {
            addInfoCard("🔁 Repeticion", DateUtils.recurrenceLabel(taskFull.task.recurrenceType, taskFull.task.recurrenceInterval), R.color.accent_pink_dark);
        }
        String reminders = remindersText(taskFull);
        if (!TextUtils.isEmpty(reminders)) {
            addInfoCard("⏰ Alarmas", reminders, R.color.warning_dark);
        }
        if (taskFull.task.estimatedMinutes > 0) {
            addInfoCard("⏱️ Temporizador", taskFull.task.estimatedMinutes + " min", R.color.accent_blue_dark);
        }
        if (taskFull.task.isCompleted) {
            addInfoCard("✅ Estado", "Completada", R.color.success_dark);
        }
        if (taskFull.task.isArchived) {
            addInfoCard("📦 Estado", "Archivada", R.color.warning_dark);
        }
        if (taskFull.task.isDeleted) {
            addInfoCard("🗑️ Estado", "En papelera", R.color.danger_dark);
        }
    }

    private void addInfoCard(String title, String value, int colorRes) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        TextView card = new TextView(this);
        card.setText(title + "\n" + value);
        card.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
        card.setTextSize(14f);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setTypeface(card.getTypeface(), android.graphics.Typeface.BOLD);
        card.setLineSpacing(dp(2), 1f);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(infoGradient(colorRes));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, detailInfoContainer.getChildCount() == 0 ? 0 : dp(9), 0, 0);
        detailInfoContainer.addView(card, params);
    }

    private void showActionMenu(View anchor) {
        if (currentTask == null || currentTask.task == null) {
            return;
        }
        if (actionPopup != null) {
            actionPopup.dismiss();
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));
        content.setBackground(popupGradient());

        TextView header = new TextView(this);
        header.setText("⋮ Acciones");
        header.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
        header.setTextSize(16f);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));
        header.setBackground(infoGradient(R.color.primary_purple_dark));
        content.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
        ));

        if (!currentTask.task.isDeleted) {
            addAction(content, currentTask.task.isCompleted ? "Reabrir" : "Completar",
                    R.drawable.ic_check_circle_24, R.color.success, v -> toggleComplete());
            addAction(content, "Editar", R.drawable.ic_edit_24, R.color.accent_blue, v -> openEdit());
            addAction(content, "Temporizador", R.drawable.ic_today_24, R.color.accent_teal, v -> openFocus());
            addAction(content, "Duplicar", R.drawable.ic_add_24, R.color.primary_purple, v -> duplicateTask());
            addAction(content, "Fijar como plantilla", R.drawable.ic_star_24, R.color.accent_blue, v -> saveTemplate());
        }

        String archiveLabel = currentTask.task.isDeleted || currentTask.task.isArchived ? "Restaurar" : "Archivar";
        addAction(content, archiveLabel, R.drawable.ic_folder_24, R.color.warning, v -> archiveOrRestore());
        addAction(content, currentTask.task.isDeleted ? "Eliminar definitivamente" : "Mover a papelera",
                R.drawable.ic_delete_24, R.color.danger, v -> confirmDelete());

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(72), dp(276));
        int height = Math.min(dp(336), (int) (getResources().getDisplayMetrics().heightPixels * 0.46f));
        actionPopup = new PopupWindow(scrollView, width, height, true);
        actionPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        actionPopup.setOutsideTouchable(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            actionPopup.setElevation(dp(16));
        }
        actionPopup.setOnDismissListener(() -> actionPopup = null);
        final int[] lastY = new int[1];
        final int[] offset = new int[]{dp(92)};
        header.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastY[0] = (int) event.getRawY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                int nowY = (int) event.getRawY();
                offset[0] = Math.max(dp(46), Math.min(getResources().getDisplayMetrics().heightPixels - dp(160), offset[0] - (nowY - lastY[0])));
                lastY[0] = nowY;
                int x = getResources().getDisplayMetrics().widthPixels - width - dp(22);
                int y = getResources().getDisplayMetrics().heightPixels - height - offset[0];
                if (actionPopup != null) {
                    actionPopup.update(x, y, width, height);
                }
                return true;
            }
            return false;
        });
        actionPopup.showAtLocation(anchor, Gravity.BOTTOM | Gravity.END, dp(22), offset[0]);
    }

    private void addAction(LinearLayout parent, String label, int iconRes, int colorRes, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
        button.setIconResource(iconRes);
        button.setIconTintResource(R.color.on_primary);
        button.setIconPadding(dp(10));
        button.setCornerRadius(dp(14));
        button.setBackgroundTintList(null);
        button.setBackground(actionGradient(colorRes));
        button.setOnClickListener(v -> {
            if (actionPopup != null) {
                actionPopup.dismiss();
            }
            listener.onClick(v);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        params.setMargins(0, parent.getChildCount() == 0 ? 0 : dp(8), 0, 0);
        parent.addView(button, params);
    }

    @Override
    protected void onStop() {
        if (actionPopup != null) {
            actionPopup.dismiss();
        }
        super.onStop();
    }

    private void toggleComplete() {
        boolean completed = !currentTask.task.isCompleted;
        viewModel.toggleCompleted(taskId, completed);
        playCelebration(completed ? "Completada 🎉" : "Reabierta 🔁",
                completed ? CelebrationView.completePalette() : CelebrationView.reopenPalette());
    }

    private void openEdit() {
        Intent intent = new Intent(this, TaskFormActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        startActivity(intent);
    }

    private void openFocus() {
        Intent intent = new Intent(this, FocusActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        startActivity(intent);
    }

    private void duplicateTask() {
        viewModel.duplicateTask(taskId);
        Toast.makeText(this, "Tarea duplicada.", Toast.LENGTH_SHORT).show();
    }

    private void saveTemplate() {
        viewModel.saveAsTemplate(taskId);
        Toast.makeText(this, "Plantilla guardada.", Toast.LENGTH_SHORT).show();
    }

    private void archiveOrRestore() {
        if (currentTask.task.isDeleted) {
            viewModel.restoreTask(taskId);
            Toast.makeText(this, "Tarea restaurada.", Toast.LENGTH_SHORT).show();
        } else if (currentTask.task.isArchived) {
            viewModel.archiveTask(taskId, false);
            Toast.makeText(this, "Tarea desarchivada.", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.archiveTask(taskId, true);
            Toast.makeText(this, "Tarea archivada.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String tagsText(TaskFull taskFull) {
        if (taskFull.tags == null || taskFull.tags.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < taskFull.tags.size(); i++) {
            TagEntity tag = taskFull.tags.get(i);
            if (tag == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(tag.name);
        }
        return builder.length() == 0 ? "" : builder.toString();
    }

    private String remindersText(TaskFull taskFull) {
        StringBuilder builder = new StringBuilder();
        appendReminder(builder, taskFull.task.reminderDate);
        appendReminder(builder, taskFull.task.reminderDate2);
        appendReminder(builder, taskFull.task.reminderDate3);
        return builder.toString();
    }

    private void appendReminder(StringBuilder builder, Long timestamp) {
        if (timestamp == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" | ");
        }
        builder.append(DateUtils.formatDateTime(timestamp));
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

    private int priorityColor(int priority) {
        if (priority >= Constants.PRIORITY_URGENT) {
            return R.color.danger_dark;
        }
        if (priority >= Constants.PRIORITY_HIGH) {
            return R.color.warning_dark;
        }
        if (priority >= Constants.PRIORITY_MEDIUM) {
            return R.color.accent_blue_dark;
        }
        return R.color.success_dark;
    }

    private GradientDrawable infoGradient(int colorRes) {
        int source = ContextCompat.getColor(this, colorRes);
        int purple = ContextCompat.getColor(this, R.color.primary_purple_dark);
        int blue = ContextCompat.getColor(this, R.color.accent_blue_dark);
        int teal = ContextCompat.getColor(this, R.color.accent_teal_dark);
        int start = blend(source, purple, 0.58f);
        int middle = blend(source, blue, 0.52f);
        int end = blend(source, teal, 0.44f);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{start, middle, end});
        drawable.setCornerRadius(dp(14));
        drawable.setStroke(dp(1), blend(middle, Color.WHITE, 0.22f));
        return drawable;
    }

    private GradientDrawable popupGradient() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        ContextCompat.getColor(this, R.color.card_dark_soft),
                        ContextCompat.getColor(this, R.color.card_dark)
                }
        );
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), ContextCompat.getColor(this, R.color.primary_purple_dark));
        return drawable;
    }

    private GradientDrawable actionGradient(int colorRes) {
        int base = ContextCompat.getColor(this, colorRes);
        int end = blend(base, ContextCompat.getColor(this, R.color.primary_purple_dark), 0.34f);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{base, end});
        drawable.setCornerRadius(dp(14));
        return drawable;
    }

    private int blend(int from, int to, float ratio) {
        float inverse = 1f - ratio;
        int red = Math.round(Color.red(from) * inverse + Color.red(to) * ratio);
        int green = Math.round(Color.green(from) * inverse + Color.green(to) * ratio);
        int blue = Math.round(Color.blue(from) * inverse + Color.blue(to) * ratio);
        return Color.rgb(red, green, blue);
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

    private void confirmDelete() {
        boolean permanent = currentTask != null && currentTask.task != null && currentTask.task.isDeleted;
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null, false);
        TextView textDeleteTitle = content.findViewById(R.id.textDeleteTitle);
        TextView textDeleteMessage = content.findViewById(R.id.textDeleteMessage);
        MaterialButton buttonCancel = content.findViewById(R.id.buttonDeleteCancel);
        MaterialButton buttonConfirm = content.findViewById(R.id.buttonDeleteConfirm);
        textDeleteTitle.setText(permanent ? "Eliminar definitivamente 🗑️" : "Mover a papelera 🗑️");
        textDeleteMessage.setText(permanent
                ? "Esta accion eliminara la tarea, sus subtareas y etiquetas asociadas."
                : "La tarea ira a Papeleria y podras restaurarla despues.");
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonConfirm.setOnClickListener(v -> {
            if (permanent) {
                viewModel.deleteTaskPermanently(taskId);
            } else {
                viewModel.deleteTask(taskId);
            }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
