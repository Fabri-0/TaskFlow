package com.taskflow.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.R;
import com.taskflow.ui.main.MainActivity;

public class RegisterActivity extends AppCompatActivity {
    private AuthViewModel viewModel;
    private TextInputLayout inputName;
    private TextInputLayout inputEmail;
    private TextInputLayout inputUsername;
    private TextInputLayout inputPassword;
    private TextInputLayout inputConfirm;
    private TextInputEditText editName;
    private TextInputEditText editEmail;
    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private TextInputEditText editConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirm = findViewById(R.id.inputConfirm);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editConfirm = findViewById(R.id.editConfirm);
        MaterialButton buttonRegister = findViewById(R.id.buttonRegister);
        TextView linkLogin = findViewById(R.id.linkLogin);

        buttonRegister.setOnClickListener(v -> {
            clearErrors();
            viewModel.register(text(editName), text(editEmail), text(editUsername), text(editPassword), text(editConfirm));
        });
        linkLogin.setOnClickListener(v -> finish());
        viewModel.getError().observe(this, message -> {
            inputConfirm.setError(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
        viewModel.getAuthSuccess().observe(this, userId -> openMain());
    }

    private void clearErrors() {
        inputName.setError(null);
        inputEmail.setError(null);
        inputUsername.setError(null);
        inputPassword.setError(null);
        inputConfirm.setError(null);
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
