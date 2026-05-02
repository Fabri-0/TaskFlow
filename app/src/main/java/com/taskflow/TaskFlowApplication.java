package com.taskflow;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.taskflow.notifications.ReminderScheduler;
import com.taskflow.session.SessionManager;

public class TaskFlowApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        int mode = new SessionManager(this).getThemeMode(AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setDefaultNightMode(mode);
        ReminderScheduler.ensureChannel(this);
    }
}
