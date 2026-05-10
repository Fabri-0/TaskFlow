package com.taskflow.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.R;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.UserEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.data.repository.ProjectRepository;
import com.taskflow.data.repository.TagRepository;
import com.taskflow.data.repository.TaskRepository;
import com.taskflow.notifications.ReminderPermissionHelper;
import com.taskflow.session.SessionManager;
import com.taskflow.ui.auth.AuthViewModel;
import com.taskflow.ui.auth.LoginActivity;
import com.taskflow.ui.project.ProjectActivity;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;
import com.taskflow.utils.ProgressUtils;
import com.taskflow.utils.Validators;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private TextView textCompletionRate;
    private LinearProgressIndicator progressProfileCompletion;
    private TextView textTodayCount;
    private TextView textTotalTasks;
    private TextView textPendingTasks;
    private TextView textCompletedTasks;
    private TextView textBusyDay;
    private TextView textStarredTasks;
    private TextView textCategoryCount;
    private TextView textTagCount;
    private TextView textThemeStatus;
    private TextView textNotificationStatus;
    private MaterialButton buttonNotificationPermission;
    private List<TaskFull> dashboardTasks = new ArrayList<>();
    private List<ProjectEntity> dashboardProjects = new ArrayList<>();
    private List<TagEntity> dashboardTags = new ArrayList<>();
    private AuthViewModel authViewModel;
    private UserEntity currentUser;
    private AlertDialog accountDialog;
    private View accountDialogContent;
    private long busiestDayMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        sessionManager = new SessionManager(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        TextView textName = findViewById(R.id.textProfileName);
        TextView textUsername = findViewById(R.id.textProfileUsername);
        textCompletionRate = findViewById(R.id.textCompletionRate);
        progressProfileCompletion = findViewById(R.id.progressProfileCompletion);
        textTodayCount = findViewById(R.id.textTodayCount);
        textTotalTasks = findViewById(R.id.textTotalTasks);
        textPendingTasks = findViewById(R.id.textPendingTasks);
        textCompletedTasks = findViewById(R.id.textCompletedTasks);
        textBusyDay = findViewById(R.id.textBusyDay);
        textStarredTasks = findViewById(R.id.textStarredTasks);
        textCategoryCount = findViewById(R.id.textCategoryCount);
        textTagCount = findViewById(R.id.textTagCount);
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        textThemeStatus = findViewById(R.id.textThemeStatus);
        textNotificationStatus = findViewById(R.id.textNotificationStatus);
        buttonNotificationPermission = findViewById(R.id.buttonNotificationPermission);
        setupInfoCards();
        authViewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                textName.setText(user.name);
                textUsername.setText("@" + visibleUsername(user));
            }
        });
        authViewModel.getError().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
        authViewModel.getAccountUpdated().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                Toast.makeText(this, "Cuenta actualizada.", Toast.LENGTH_SHORT).show();
                if (accountDialog != null && accountDialog.isShowing() && accountDialogContent != null) {
                    closeInfoDialog(accountDialog, accountDialogContent);
                }
            }
        });
        authViewModel.loadUser(sessionManager.getActiveUserId());
        observeDashboard();
        int savedMode = sessionManager.getThemeMode(AppCompatDelegate.MODE_NIGHT_YES);
        boolean darkMode = savedMode != AppCompatDelegate.MODE_NIGHT_NO;
        switchDarkMode.setChecked(darkMode);
        updateThemeStatus(darkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            sessionManager.saveThemeMode(mode);
            updateThemeStatus(isChecked);
            AppCompatDelegate.setDefaultNightMode(mode);
        });
        updateNotificationStatus();
        buttonNotificationPermission.setOnClickListener(v -> requestNotificationPermission());
        findViewById(R.id.buttonAccountSettings).setOnClickListener(v -> showAccountSettingsDialog());
        findViewById(R.id.buttonManageCategories).setOnClickListener(v -> startActivity(new Intent(this, ProjectActivity.class)));
        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (textNotificationStatus != null) {
            updateNotificationStatus();
        }
    }

    private void updateThemeStatus(boolean darkMode) {
        textThemeStatus.setText(darkMode ? "Tema oscuro activo 🌙" : "Tema claro activo ☀️");
    }

    private void observeDashboard() {
        long userId = sessionManager.getActiveUserId();
        TaskRepository taskRepository = new TaskRepository(this);
        ProjectRepository projectRepository = new ProjectRepository(this);
        TagRepository tagRepository = new TagRepository(this);
        taskRepository.getVisibleTasks(userId, Constants.FILTER_ALL, "", null, null)
                .observe(this, this::renderTaskStats);
        projectRepository.getProjects(userId).observe(this, this::renderProjectStats);
        tagRepository.getTags(userId).observe(this, this::renderTagStats);
    }

    private void renderTaskStats(List<TaskFull> tasks) {
        dashboardTasks = tasks == null ? new ArrayList<>() : tasks;
        int total = tasks == null ? 0 : tasks.size();
        int today = 0;
        int pending = 0;
        int completed = 0;
        int starred = 0;
        Map<Long, Integer> dueDays = new HashMap<>();
        if (tasks != null) {
            for (TaskFull task : tasks) {
                if (task == null || task.task == null) {
                    continue;
                }
                if (task.task.isCompleted) {
                    completed++;
                } else {
                    pending++;
                }
                if (task.task.isStarred) {
                    starred++;
                }
                if (task.task.dueDate != null) {
                    long day = DateUtils.startOfDay(task.task.dueDate);
                    int dayLoad = 1 + (task.subtasks == null ? 0 : task.subtasks.size());
                    dueDays.put(day, dueDays.containsKey(day) ? dueDays.get(day) + dayLoad : dayLoad);
                    if (task.task.dueDate >= DateUtils.startOfToday() && task.task.dueDate <= DateUtils.endOfToday()) {
                        today++;
                    }
                }
            }
        }
        int completionRate = ProgressUtils.calculatePercentage(completed, total);
        setCompactInlineNumber(textCompletionRate, "🚀 Avance general: ", completionRate + "%", " completado");
        progressProfileCompletion.setProgress(completionRate);
        setCompactMetricNumber(textTodayCount, "📅 Hoy", today);
        setCompactMetricNumber(textTotalTasks, "🧾 Total", total);
        setCompactMetricNumber(textPendingTasks, "⏳ Pendientes", pending);
        setCompactMetricNumber(textCompletedTasks, "✅ Hechas", completed);
        setCompactMetricNumber(textStarredTasks, "⭐ Favoritas", starred);
        textBusyDay.setText("🔥 Dia mas cargado: " + busiestDayText(dueDays));
    }

    private void renderProjectStats(List<ProjectEntity> projects) {
        dashboardProjects = projects == null ? new ArrayList<>() : projects;
        int count = projects == null ? 0 : projects.size();
        setCompactMetricNumber(textCategoryCount, "🗂️ Categorias", count);
    }

    private void renderTagStats(List<TagEntity> tags) {
        dashboardTags = tags == null ? new ArrayList<>() : tags;
        int count = tags == null ? 0 : tags.size();
        setCompactMetricNumber(textTagCount, "🏷️ Etiquetas creadas", count);
    }

    private void setCompactMetricNumber(TextView view, String label, int value) {
        String number = String.valueOf(value);
        SpannableString text = new SpannableString(label + "\n" + number);
        int start = label.length() + 1;
        text.setSpan(new RelativeSizeSpan(0.78f), start, start + number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(text);
    }

    private void setCompactInlineNumber(TextView view, String prefix, String number, String suffix) {
        SpannableString text = new SpannableString(prefix + number + suffix);
        int start = prefix.length();
        text.setSpan(new RelativeSizeSpan(0.78f), start, start + number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(text);
    }

    private void setupInfoCards() {
        textCompletionRate.setOnClickListener(v -> showTasksDialog(
                "Avance general 🚀",
                "Lectura de las tareas usadas para calcular tu progreso.",
                dashboardTasks
        ));
        textTodayCount.setOnClickListener(v -> showTasksDialog(
                "Tareas de hoy 📅",
                "Tareas con fecha de entrega para hoy.",
                tasksMatching(task -> isDueToday(task))
        ));
        textTotalTasks.setOnClickListener(v -> showTasksDialog(
                "Todas las tareas 🧾",
                "Vista completa de tus tareas visibles.",
                dashboardTasks
        ));
        textPendingTasks.setOnClickListener(v -> showTasksDialog(
                "Pendientes ⏳",
                "Tareas que todavia esperan completarse.",
                tasksMatching(task -> !task.task.isCompleted)
        ));
        textCompletedTasks.setOnClickListener(v -> showTasksDialog(
                "Hechas ✅",
                "Tareas que ya marcaste como completadas.",
                tasksMatching(task -> task.task.isCompleted)
        ));
        textStarredTasks.setOnClickListener(v -> showTasksDialog(
                "Favoritas ⭐",
                "Tareas que marcaste como favoritas.",
                tasksMatching(task -> task.task.isStarred)
        ));
        textBusyDay.setOnClickListener(v -> showTasksDialog(
                "Dia mas cargado 🔥",
                "Tareas del dia con mas entregas.",
                tasksMatching(task -> task.task.dueDate != null
                        && DateUtils.startOfDay(task.task.dueDate) == busiestDayMillis)
        ));
        textCategoryCount.setOnClickListener(v -> showTextDialog(
                "Categorias 🗂️",
                "Lista de categorias creadas.",
                projectRows()
        ));
        textTagCount.setOnClickListener(v -> showTextDialog(
                "Etiquetas 🏷️",
                "Lista de etiquetas creadas.",
                tagRows()
        ));
    }

    private String visibleUsername(UserEntity user) {
        if (user == null || user.username == null || user.username.trim().isEmpty()) {
            return "usuario";
        }
        return user.username.trim();
    }

    private void showAccountSettingsDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "Cargando datos de la cuenta.", Toast.LENGTH_SHORT).show();
            return;
        }
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_account_settings, null, false);
        TextInputLayout inputName = content.findViewById(R.id.inputAccountName);
        TextInputLayout inputEmail = content.findViewById(R.id.inputAccountEmail);
        TextInputLayout inputUsername = content.findViewById(R.id.inputAccountUsername);
        TextInputLayout inputCurrentPassword = content.findViewById(R.id.inputCurrentPassword);
        TextInputLayout inputNewPassword = content.findViewById(R.id.inputNewPassword);
        TextInputLayout inputConfirmNewPassword = content.findViewById(R.id.inputConfirmNewPassword);
        TextInputEditText editName = content.findViewById(R.id.editAccountName);
        TextInputEditText editEmail = content.findViewById(R.id.editAccountEmail);
        TextInputEditText editUsername = content.findViewById(R.id.editAccountUsername);
        TextInputEditText editCurrentPassword = content.findViewById(R.id.editCurrentPassword);
        TextInputEditText editNewPassword = content.findViewById(R.id.editNewPassword);
        TextInputEditText editConfirmNewPassword = content.findViewById(R.id.editConfirmNewPassword);
        MaterialButton buttonCancel = content.findViewById(R.id.buttonAccountCancel);
        MaterialButton buttonSave = content.findViewById(R.id.buttonAccountSave);

        editName.setText(currentUser.name);
        editEmail.setText(currentUser.email);
        editUsername.setText(currentUser.username);

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        accountDialog = dialog;
        accountDialogContent = content;
        content.setAlpha(0f);
        content.setScaleX(0.86f);
        content.setScaleY(0.86f);
        content.setTranslationY(dp(22));
        buttonCancel.setOnClickListener(v -> closeInfoDialog(dialog, content));
        buttonSave.setOnClickListener(v -> {
            clearAccountErrors(inputName, inputEmail, inputUsername, inputCurrentPassword, inputNewPassword, inputConfirmNewPassword);
            String name = text(editName);
            String email = text(editEmail);
            String username = text(editUsername);
            String currentPassword = text(editCurrentPassword);
            String newPassword = text(editNewPassword);
            String confirmation = text(editConfirmNewPassword);
            if (!Validators.isValidName(name)) {
                inputName.setError("Minimo 2 caracteres.");
                return;
            }
            if (!Validators.isValidEmail(email)) {
                inputEmail.setError("Correo invalido.");
                return;
            }
            if (!Validators.isValidUsername(username)) {
                inputUsername.setError("Usa 3 a 20 caracteres: letras, numeros, punto o guion bajo.");
                return;
            }
            if (!newPassword.isEmpty() || !confirmation.isEmpty() || !currentPassword.isEmpty()) {
                if (currentPassword.isEmpty()) {
                    inputCurrentPassword.setError("Escribe tu contrasena actual.");
                    return;
                }
                if (newPassword.isEmpty()) {
                    inputNewPassword.setError("Escribe una nueva contrasena.");
                    return;
                }
                if (!Validators.isValidPassword(newPassword)) {
                    inputNewPassword.setError("Minimo 6 caracteres.");
                    return;
                }
                if (!newPassword.equals(confirmation)) {
                    inputConfirmNewPassword.setError("No coincide.");
                    return;
                }
            }
            authViewModel.updateAccount(sessionManager.getActiveUserId(), name, email, username, currentPassword, newPassword, confirmation);
        });
        dialog.setOnDismissListener(d -> {
            if (accountDialog == dialog) {
                accountDialog = null;
                accountDialogContent = null;
            }
        });
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setWindowAnimations(R.style.TaskFlowCenterDialogAnimation);
            }
            content.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(280)
                    .setInterpolator(new OvershootInterpolator(1.08f))
                    .start();
        });
        dialog.setView(content);
        dialog.show();
    }

    private void clearAccountErrors(TextInputLayout... inputs) {
        for (TextInputLayout input : inputs) {
            input.setError(null);
        }
    }

    private void showTasksDialog(String title, String subtitle, List<TaskFull> tasks) {
        List<String> rows = new ArrayList<>();
        if (tasks != null) {
            for (TaskFull task : tasks) {
                if (task == null || task.task == null) {
                    continue;
                }
                String subtaskLine = subtaskPreviewLine(task);
                String row = (task.task.isCompleted ? "✅ " : "⏳ ")
                        + "Tarea: " + task.task.title
                        + subtaskLine
                        + "\n📅 Fecha de entrega: " + DateUtils.formatDateTime(task.task.dueDate);
                rows.add(row);
            }
        }
        showTextDialog(title, subtitle, rows);
    }

    private String subtaskPreviewLine(TaskFull task) {
        if (task.subtasks == null || task.subtasks.isEmpty()) {
            return "";
        }
        for (int i = 0; i < task.subtasks.size(); i++) {
            if (task.subtasks.get(i) == null || task.subtasks.get(i).title == null) {
                continue;
            }
            String title = task.subtasks.get(i).title.trim();
            if (!title.isEmpty()) {
                return "\n🔖 Subtarea: " + firstWords(title, 2);
            }
        }
        return "";
    }

    private String firstWords(String value, int limit) {
        String[] words = value.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length && i < limit; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(words[i]);
        }
        return builder.toString();
    }

    private void showTextDialog(String title, String subtitle, List<String> rows) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_profile_info, null, false);
        TextView textTitle = content.findViewById(R.id.textInfoTitle);
        TextView textSubtitle = content.findViewById(R.id.textInfoSubtitle);
        LinearLayout list = content.findViewById(R.id.infoTaskList);
        MaterialButton buttonClose = content.findViewById(R.id.buttonInfoClose);
        textTitle.setText(title);
        textSubtitle.setText(subtitle);
        if (rows == null || rows.isEmpty()) {
            addInfoRow(list, "No hay informacion para mostrar todavia.", 0);
        } else {
            int index = 0;
            for (String row : rows) {
                addInfoRow(list, row, index++);
            }
        }
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        content.setAlpha(0f);
        content.setScaleX(0.82f);
        content.setScaleY(0.82f);
        content.setTranslationY(dp(26));
        content.setRotationX(-9f);
        content.setCameraDistance(9000f);
        buttonClose.setOnClickListener(v -> closeInfoDialog(dialog, content));
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setWindowAnimations(R.style.TaskFlowCenterDialogAnimation);
            }
            content.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .rotationX(0f)
                    .setDuration(340)
                    .setInterpolator(new OvershootInterpolator(1.15f))
                    .start();
        });
        dialog.setView(content);
        dialog.show();
    }

    private void closeInfoDialog(AlertDialog dialog, View content) {
        content.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(dp(18))
                .rotationX(6f)
                .setDuration(180)
                .withEndAction(dialog::dismiss)
                .start();
    }

    private void addInfoRow(LinearLayout list, String value, int index) {
        TextView row = new TextView(this);
        row.setText(value);
        row.setTextColor(getColor(R.color.text_primary));
        row.setTextSize(14f);
        row.setLineSpacing(2f, 1.05f);
        row.setBackgroundResource(R.drawable.bg_card_soft);
        int padding = dp(14);
        row.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        list.addView(row, params);
        row.setAlpha(0f);
        row.setTranslationY(dp(14));
        row.setScaleX(0.96f);
        row.setScaleY(0.96f);
        row.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(130L + index * 45L)
                .setDuration(260)
                .setInterpolator(new OvershootInterpolator(1.05f))
                .start();
    }

    private List<TaskFull> tasksMatching(TaskMatcher matcher) {
        List<TaskFull> values = new ArrayList<>();
        for (TaskFull task : dashboardTasks) {
            if (task != null && task.task != null && matcher.matches(task)) {
                values.add(task);
            }
        }
        return values;
    }

    private boolean isDueToday(TaskFull task) {
        return task.task.dueDate != null
                && task.task.dueDate >= DateUtils.startOfToday()
                && task.task.dueDate <= DateUtils.endOfToday();
    }

    private List<String> projectRows() {
        List<String> rows = new ArrayList<>();
        for (ProjectEntity project : dashboardProjects) {
            rows.add("🗂️ " + project.name);
        }
        return rows;
    }

    private List<String> tagRows() {
        List<String> rows = new ArrayList<>();
        for (TagEntity tag : dashboardTags) {
            rows.add("🏷️ " + tag.name);
        }
        return rows;
    }

    private String busiestDayText(Map<Long, Integer> dueDays) {
        if (dueDays == null || dueDays.isEmpty()) {
            busiestDayMillis = 0;
            return "sin fechas todavia";
        }
        long bestDay = 0;
        int bestCount = 0;
        for (Map.Entry<Long, Integer> entry : dueDays.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestDay = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        busiestDayMillis = bestDay;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(bestDay);
        String day = new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(calendar.getTime());
        return day + " (" + bestCount + " tareas/pasos)";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private interface TaskMatcher {
        boolean matches(TaskFull task);
    }

    private void updateNotificationStatus() {
        boolean ready = ReminderPermissionHelper.hasReminderAccess(this);
        textNotificationStatus.setText(ready
                ? "Alarmas listas: hora exacta, pantalla completa y segundo plano configurados."
                : "Configura los permisos para que las alarmas suenen aunque TaskFlow este cerrada.");
        buttonNotificationPermission.setEnabled(!ready);
        buttonNotificationPermission.setText(ready ? "Alarmas listas" : "Configurar alarmas");
    }

    private void requestNotificationPermission() {
        ReminderPermissionHelper.ensureReminderAccess(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ReminderPermissionHelper.REQUEST_NOTIFICATIONS) {
            updateNotificationStatus();
        }
    }
}
