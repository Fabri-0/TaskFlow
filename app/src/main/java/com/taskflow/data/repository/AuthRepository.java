package com.taskflow.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.taskflow.data.local.dao.ProjectDao;
import com.taskflow.data.local.dao.SubtaskDao;
import com.taskflow.data.local.dao.TagDao;
import com.taskflow.data.local.dao.TaskDao;
import com.taskflow.data.local.dao.TaskTagDao;
import com.taskflow.data.local.dao.TaskTemplateDao;
import com.taskflow.data.local.dao.UserDao;
import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.entity.TaskTemplateEntity;
import com.taskflow.data.local.entity.TaskTagCrossRef;
import com.taskflow.data.local.entity.UserEntity;
import com.taskflow.session.SessionManager;
import com.taskflow.utils.Constants;
import com.taskflow.utils.PasswordUtils;
import com.taskflow.utils.Validators;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepository {
    private final UserDao userDao;
    private final TaskDao taskDao;
    private final ProjectDao projectDao;
    private final SubtaskDao subtaskDao;
    private final TagDao tagDao;
    private final TaskTagDao taskTagDao;
    private final TaskTemplateDao taskTemplateDao;
    private final SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AuthRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        userDao = database.userDao();
        taskDao = database.taskDao();
        projectDao = database.projectDao();
        subtaskDao = database.subtaskDao();
        tagDao = database.tagDao();
        taskTagDao = database.taskTagDao();
        taskTemplateDao = database.taskTemplateDao();
        sessionManager = new SessionManager(context);
    }

    public void register(String name, String email, String username, String password, ResultCallback<Long> callback) {
        executor.execute(() -> {
            String cleanName = name == null ? "" : name.trim();
            String cleanEmail = email == null ? "" : email.trim().toLowerCase();
            String cleanUsername = username == null ? "" : username.trim().toLowerCase();
            if (!Validators.isValidName(cleanName)) {
                postError(callback, "El nombre debe tener al menos 2 caracteres.");
                return;
            }
            if (!Validators.isValidEmail(cleanEmail)) {
                postError(callback, "Ingresa un correo valido.");
                return;
            }
            if (!Validators.isValidUsername(cleanUsername)) {
                postError(callback, "El usuario debe tener 3 a 20 caracteres: letras, numeros, punto o guion bajo.");
                return;
            }
            if (!Validators.isValidPassword(password)) {
                postError(callback, "La contrasena debe tener al menos 6 caracteres.");
                return;
            }
            if (userDao.countByEmail(cleanEmail) > 0) {
                postError(callback, "Ese correo ya esta registrado.");
                return;
            }
            if (userDao.countByUsername(cleanUsername) > 0) {
                postError(callback, "Ese nombre de usuario ya esta en uso.");
                return;
            }
            UserEntity user = new UserEntity(cleanName, cleanEmail, cleanUsername, PasswordUtils.hashPassword(password), System.currentTimeMillis());
            long userId = userDao.insertUser(user);
            seedDemoDataIfNeeded(userId);
            sessionManager.saveSession(userId);
            postSuccess(callback, userId);
        });
    }

    public void login(String email, String password, ResultCallback<Long> callback) {
        executor.execute(() -> {
            String identifier = email == null ? "" : email.trim().toLowerCase();
            UserEntity user = userDao.getUserByEmailOrUsername(identifier);
            if (user == null || !PasswordUtils.matches(password == null ? "" : password, user.passwordHash)) {
                postError(callback, "Usuario/correo o contrasena incorrectos.");
                return;
            }
            sessionManager.saveSession(user.id);
            postSuccess(callback, user.id);
        });
    }

    public void getUser(long userId, ResultCallback<UserEntity> callback) {
        executor.execute(() -> {
            UserEntity user = userDao.getUserById(userId);
            if (user == null) {
                postError(callback, "Usuario no encontrado.");
            } else {
                postSuccess(callback, user);
            }
        });
    }

    public void updateAccount(long userId, String name, String email, String username, String currentPassword, String newPassword, ResultCallback<UserEntity> callback) {
        executor.execute(() -> {
            UserEntity user = userDao.getUserById(userId);
            if (user == null) {
                postError(callback, "Usuario no encontrado.");
                return;
            }
            String cleanName = name == null ? "" : name.trim();
            String cleanEmail = email == null ? "" : email.trim().toLowerCase();
            String cleanUsername = username == null ? "" : username.trim().toLowerCase();
            String cleanNewPassword = newPassword == null ? "" : newPassword;
            if (!Validators.isValidName(cleanName)) {
                postError(callback, "El nombre debe tener al menos 2 caracteres.");
                return;
            }
            if (!Validators.isValidEmail(cleanEmail)) {
                postError(callback, "Ingresa un correo valido.");
                return;
            }
            if (!Validators.isValidUsername(cleanUsername)) {
                postError(callback, "El usuario debe tener 3 a 20 caracteres: letras, numeros, punto o guion bajo.");
                return;
            }
            if (userDao.countByEmailForOtherUser(cleanEmail, userId) > 0) {
                postError(callback, "Ese correo ya esta registrado.");
                return;
            }
            if (userDao.countByUsernameForOtherUser(cleanUsername, userId) > 0) {
                postError(callback, "Ese nombre de usuario ya esta en uso.");
                return;
            }
            if (!cleanNewPassword.isEmpty()) {
                String cleanCurrentPassword = currentPassword == null ? "" : currentPassword;
                if (!PasswordUtils.matches(cleanCurrentPassword, user.passwordHash)) {
                    postError(callback, "La contrasena actual no coincide.");
                    return;
                }
                if (!Validators.isValidPassword(cleanNewPassword)) {
                    postError(callback, "La nueva contrasena debe tener al menos 6 caracteres.");
                    return;
                }
                user.passwordHash = PasswordUtils.hashPassword(cleanNewPassword);
            }
            user.name = cleanName;
            user.email = cleanEmail;
            user.username = cleanUsername;
            userDao.updateUser(user);
            postSuccess(callback, user);
        });
    }

    private void seedDemoDataIfNeeded(long userId) {
        if (taskDao.countTasksForUser(userId) > 0) {
            seedTemplateIfNeeded(userId);
            return;
        }
        long categoryOne = projectDao.insertProject(new ProjectEntity("Estudio", "#5BC0EB", userId));
        long categoryTwo = projectDao.insertProject(new ProjectEntity("Trabajo", "#B56BE8", userId));
        long categoryThree = projectDao.insertProject(new ProjectEntity("Personal", "#76D39B", userId));
        long tagOne = tagDao.insertTag(new TagEntity("Prioritario", "#EF476F", userId));
        long tagTwo = tagDao.insertTag(new TagEntity("Revision", "#FFD166", userId));
        long tagThree = tagDao.insertTag(new TagEntity("Ideas", "#5BC0EB", userId));

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        TaskEntity taskOne = new TaskEntity("Organizar la bandeja inicial", "Ejemplo simple sin subtareas para ver como funciona una tarea directa.", userId, calendar.getTimeInMillis(), null);
        taskOne.projectId = categoryOne;
        taskOne.priority = Constants.PRIORITY_MEDIUM;
        long taskOneId = taskDao.insertTask(taskOne);
        taskTagDao.insertRelation(new TaskTagCrossRef(taskOneId, tagOne));

        calendar.add(Calendar.DAY_OF_MONTH, 2);
        TaskEntity taskTwo = new TaskEntity("Preparar entrega de proyecto", "Muestra una tarea en progreso: una subtarea ya esta hecha y las demas quedan pendientes.", userId, calendar.getTimeInMillis(), null);
        taskTwo.projectId = categoryTwo;
        taskTwo.priority = Constants.PRIORITY_HIGH;
        taskTwo.isStarred = true;
        taskTwo.boardColumn = Constants.BOARD_DOING;
        long taskTwoId = taskDao.insertTask(taskTwo);
        subtaskDao.insertSubtask(new SubtaskEntity(taskTwoId, "Reunir requisitos", true));
        subtaskDao.insertSubtask(new SubtaskEntity(taskTwoId, "Preparar resumen", false));
        subtaskDao.insertSubtask(new SubtaskEntity(taskTwoId, "Enviar version final", false));
        taskTagDao.insertRelation(new TaskTagCrossRef(taskTwoId, tagTwo));

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        TaskEntity taskThree = new TaskEntity("Revisar rutina completada", "Ejemplo de tarea lista para mostrar el cierre y el tablero.", userId, calendar.getTimeInMillis(), null);
        taskThree.projectId = categoryThree;
        taskThree.isCompleted = true;
        taskThree.completedAt = System.currentTimeMillis();
        taskThree.boardColumn = Constants.BOARD_DONE;
        long taskThreeId = taskDao.insertTask(taskThree);
        subtaskDao.insertSubtask(new SubtaskEntity(taskThreeId, "Confirmar checklist", true));
        subtaskDao.insertSubtask(new SubtaskEntity(taskThreeId, "Guardar evidencia", true));
        taskTagDao.insertRelation(new TaskTagCrossRef(taskThreeId, tagThree));
        seedTemplateIfNeeded(userId);
    }

    private void seedTemplateIfNeeded(long userId) {
        if (taskTemplateDao.countByName(userId, "Revision semanal") > 0) {
            return;
        }
        TaskTemplateEntity template = new TaskTemplateEntity(userId, "Revision semanal", "Planificar la semana");
        template.description = "Revisar pendientes, elegir prioridades y dejar claro el siguiente paso.";
        template.projectName = "Personal";
        template.tagsCsv = "Revision, Prioritario";
        template.subtasksCsv = "Revisar tareas abiertas\nElegir 3 prioridades\nProgramar alarmas necesarias";
        template.priority = Constants.PRIORITY_MEDIUM;
        template.boardColumn = Constants.BOARD_TODO;
        template.recurrenceType = Constants.RECURRENCE_WEEKLY;
        template.estimatedMinutes = 30;
        taskTemplateDao.insertTemplate(template);
    }

    private <T> void postSuccess(ResultCallback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private <T> void postError(ResultCallback<T> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
