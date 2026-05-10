package com.taskflow.utils;

public final class Constants {
    public static final String PREFS_NAME = "taskflow_session";
    public static final String KEY_ACTIVE_USER_ID = "activeUserId";
    public static final String KEY_THEME_MODE = "themeMode";
    public static final String REMINDER_CHANNEL_ID = "taskflow_alarm_alerts_v3";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_REMINDER_SLOT = "extra_reminder_slot";
    public static final String ACTION_COMPLETE_TASK = "com.taskflow.ACTION_COMPLETE_TASK";
    public static final String ACTION_SNOOZE_TASK = "com.taskflow.ACTION_SNOOZE_TASK";
    public static final String ACTION_DISMISS_REMINDER = "com.taskflow.ACTION_DISMISS_REMINDER";
    public static final int FILTER_ALL = 0;
    public static final int FILTER_TODAY = 1;
    public static final int FILTER_OVERDUE = 2;
    public static final int FILTER_COMPLETED = 3;
    public static final int FILTER_PENDING = 4;
    public static final int FILTER_STARRED = 5;
    public static final int FILTER_UPCOMING = 6;
    public static final int FILTER_INBOX = 7;
    public static final int FILTER_NO_DATE = 8;
    public static final int FILTER_ARCHIVED = 9;
    public static final int FILTER_HIGH_PRIORITY = 10;
    public static final int FILTER_TRASH = 11;
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int PRIORITY_URGENT = 3;
    public static final String RECURRENCE_NONE = "";
    public static final String RECURRENCE_DAILY = "DAILY";
    public static final String RECURRENCE_WEEKLY = "WEEKLY";
    public static final String RECURRENCE_MONTHLY = "MONTHLY";
    public static final String BOARD_TODO = "TODO";
    public static final String BOARD_DOING = "DOING";
    public static final String BOARD_DONE = "DONE";

    private Constants() {
    }
}
