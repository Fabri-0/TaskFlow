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

public class LoginActivity extends AppCompatActivity {
    private AuthViewModel viewModel;
    private TextInputLayout inputEmail;
    private TextInputLayout inputPassword;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        MaterialButton buttonLogin = findViewById(R.id.buttonLogin);
        TextView linkRegister = findViewById(R.id.linkRegister);

        buttonLogin.setOnClickListener(v -> {
            inputEmail.setError(null);
            inputPassword.setError(null);
            viewModel.login(text(editEmail), text(editPassword));
        });
        linkRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        viewModel.getError().observe(this, message -> {
            inputPassword.setError(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
        viewModel.getAuthSuccess().observe(this, userId -> openMain());
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
