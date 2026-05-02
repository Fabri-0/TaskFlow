package com.taskflow.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.taskflow.data.local.entity.UserEntity;
import com.taskflow.data.repository.AuthRepository;
import com.taskflow.data.repository.ResultCallback;
import com.taskflow.utils.Validators;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Long> authSuccess = new MutableLiveData<>();
    private final MutableLiveData<UserEntity> user = new MutableLiveData<>();
    private final MutableLiveData<UserEntity> accountUpdated = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Long> getAuthSuccess() {
        return authSuccess;
    }

    public LiveData<UserEntity> getUserLiveData() {
        return user;
    }

    public LiveData<UserEntity> getAccountUpdated() {
        return accountUpdated;
    }

    public void login(String identifier, String password) {
        loading.setValue(true);
        repository.login(identifier, password, new ResultCallback<Long>() {
            @Override
            public void onSuccess(Long value) {
                loading.setValue(false);
                authSuccess.setValue(value);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void register(String name, String email, String username, String password, String confirmation) {
        if (!Validators.isValidName(name)) {
            error.setValue("El nombre debe tener al menos 2 caracteres.");
            return;
        }
        if (!Validators.isValidEmail(email)) {
            error.setValue("Ingresa un correo valido.");
            return;
        }
        if (!Validators.isValidUsername(username)) {
            error.setValue("El usuario debe tener 3 a 20 caracteres: letras, numeros, punto o guion bajo.");
            return;
        }
        if (!Validators.isValidPassword(password)) {
            error.setValue("La contrasena debe tener al menos 6 caracteres.");
            return;
        }
        if (!password.equals(confirmation)) {
            error.setValue("La confirmacion no coincide.");
            return;
        }
        loading.setValue(true);
        repository.register(name, email, username, password, new ResultCallback<Long>() {
            @Override
            public void onSuccess(Long value) {
                loading.setValue(false);
                authSuccess.setValue(value);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void loadUser(long userId) {
        repository.getUser(userId, new ResultCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity value) {
                user.setValue(value);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void updateAccount(long userId, String name, String email, String username, String currentPassword, String newPassword, String confirmation) {
        if (!Validators.isValidName(name)) {
            error.setValue("El nombre debe tener al menos 2 caracteres.");
            return;
        }
        if (!Validators.isValidEmail(email)) {
            error.setValue("Ingresa un correo valido.");
            return;
        }
        if (!Validators.isValidUsername(username)) {
            error.setValue("El usuario debe tener 3 a 20 caracteres: letras, numeros, punto o guion bajo.");
            return;
        }
        String cleanNewPassword = newPassword == null ? "" : newPassword;
        if (!cleanNewPassword.isEmpty()) {
            if (!Validators.isValidPassword(cleanNewPassword)) {
                error.setValue("La nueva contrasena debe tener al menos 6 caracteres.");
                return;
            }
            if (!cleanNewPassword.equals(confirmation == null ? "" : confirmation)) {
                error.setValue("La confirmacion no coincide.");
                return;
            }
        }
        loading.setValue(true);
        repository.updateAccount(userId, name, email, username, currentPassword, cleanNewPassword, new ResultCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity value) {
                loading.setValue(false);
                user.setValue(value);
                accountUpdated.setValue(value);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }
}
