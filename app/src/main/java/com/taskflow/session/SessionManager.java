package com.taskflow.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.taskflow.utils.Constants;

public class SessionManager {
    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId) {
        preferences.edit().putLong(Constants.KEY_ACTIVE_USER_ID, userId).apply();
    }

    public long getActiveUserId() {
        return preferences.getLong(Constants.KEY_ACTIVE_USER_ID, -1L);
    }

    public boolean isLoggedIn() {
        return getActiveUserId() > 0;
    }

    public void clearSession() {
        preferences.edit().remove(Constants.KEY_ACTIVE_USER_ID).apply();
    }

    public void saveThemeMode(int mode) {
        preferences.edit().putInt(Constants.KEY_THEME_MODE, mode).apply();
    }

    public int getThemeMode(int defaultMode) {
        return preferences.getInt(Constants.KEY_THEME_MODE, defaultMode);
    }
}
