package com.taskflow.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.taskflow.R;
import com.taskflow.session.SessionManager;
import com.taskflow.ui.auth.LoginActivity;
import com.taskflow.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(this);
            Class<?> target = sessionManager.isLoggedIn() ? MainActivity.class : LoginActivity.class;
            startActivity(new Intent(this, target));
            finish();
        }, 500);
    }
}
