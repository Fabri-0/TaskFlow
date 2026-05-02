package com.taskflow.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.taskflow.data.local.dao.ProjectDao;
import com.taskflow.data.local.dao.SubtaskDao;
import com.taskflow.data.local.dao.TagDao;
import com.taskflow.data.local.dao.TaskDao;
import com.taskflow.data.local.dao.TaskTagDao;
import com.taskflow.data.local.dao.UserDao;
import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.entity.TaskTagCrossRef;
import com.taskflow.data.local.entity.UserEntity;
import com.taskflow.session.SessionManager;
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
            return;
        }
        long categoryOne = projectDao.insertProject(new ProjectEntity("Categoria 1", "#B56BE8", userId));
        long categoryTwo = projectDao.insertProject(new ProjectEntity("Categoria 2", "#FFD166", userId));
        long categoryThree = projectDao.insertProject(new ProjectEntity("Categoria 3", "#EFA4C8", userId));
        long tagOne = tagDao.insertTag(new TagEntity("Etiqueta 1", "#B56BE8", userId));
        long tagTwo = tagDao.insertTag(new TagEntity("Etiqueta 2", "#76D39B", userId));
        long tagThree = tagDao.insertTag(new TagEntity("Etiqueta 3", "#5BC0EB", userId));

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        TaskEntity taskOne = new TaskEntity("Tarea 1", "Descripcion de ejemplo para una tarea pendiente.", userId, calendar.getTimeInMillis(), null);
        taskOne.projectId = categoryOne;
        long taskOneId = taskDao.insertTask(taskOne);
        subtaskDao.insertSubtask(new SubtaskEntity(taskOneId, "Subtarea 1", false));
        subtaskDao.insertSubtask(new SubtaskEntity(taskOneId, "Subtarea 2", false));
        taskTagDao.insertRelation(new TaskTagCrossRef(taskOneId, tagOne));

        calendar.add(Calendar.DAY_OF_MONTH, 2);
        TaskEntity taskTwo = new TaskEntity("Tarea 2", "Ejemplo con fecha proxima para probar el calendario.", userId, calendar.getTimeInMillis(), null);
        taskTwo.projectId = categoryTwo;
        long taskTwoId = taskDao.insertTask(taskTwo);
        subtaskDao.insertSubtask(new SubtaskEntity(taskTwoId, "Subtarea 1", false));
        taskTagDao.insertRelation(new TaskTagCrossRef(taskTwoId, tagTwo));

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        TaskEntity taskThree = new TaskEntity("Tarea 3", "Muestra de una tarea ya completada.", userId, calendar.getTimeInMillis(), null);
        taskThree.projectId = categoryThree;
        taskThree.isCompleted = true;
        long taskThreeId = taskDao.insertTask(taskThree);
        subtaskDao.insertSubtask(new SubtaskEntity(taskThreeId, "Subtarea 1", true));
        taskTagDao.insertRelation(new TaskTagCrossRef(taskThreeId, tagThree));
    }

    private <T> void postSuccess(ResultCallback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private <T> void postError(ResultCallback<T> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
