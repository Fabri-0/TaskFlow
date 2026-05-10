package com.taskflow.notifications;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.taskflow.R;
import com.taskflow.ui.task.ReminderAlarmActivity;
import com.taskflow.utils.Constants;

public class ReminderScheduler {
    private final Context context;
    private final AlarmManager alarmManager;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.REMINDER_CHANNEL_ID,
                    context.getString(R.string.channel_reminders),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.channel_reminders_description));
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (sound == null) {
                sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(sound, attributes);
            channel.enableVibration(PermissionPrefs.isVibrationEnabled(context));
            if (PermissionPrefs.isVibrationEnabled(context)) {
                channel.setVibrationPattern(new long[]{0L, 350L, 220L, 550L, 220L, 850L, 220L, 1200L});
            }
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public boolean canNotify() {
        if (!PermissionPrefs.isAlarmsEnabled(context)) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public boolean schedule(long taskId, String title, long reminderAt) {
        return schedule(taskId, 1, title, reminderAt);
    }

    public boolean schedule(long taskId, int slot, String title, long reminderAt) {
        if (reminderAt <= System.currentTimeMillis()) {
            return false;
        }
        if (!PermissionPrefs.isAlarmsEnabled(context)) {
            return false;
        }
        ensureChannel(context);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        intent.putExtra(Constants.EXTRA_TASK_TITLE, title);
        intent.putExtra(Constants.EXTRA_REMINDER_SLOT, slot);
        PendingIntent pendingIntent = pendingIntent(taskId, slot, intent);
        if (alarmManager == null) {
            return false;
        }
        try {
            if (PermissionPrefs.isExactEnabled(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(reminderAt, showIntent(taskId, slot, title)), pendingIntent);
                return true;
            }
            if (PermissionPrefs.isExactEnabled(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
            } else if (PermissionPrefs.isExactEnabled(context)) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
            }
        } catch (SecurityException ignored) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
            }
        }
        return true;
    }

    public void cancel(long taskId) {
        if (alarmManager != null) {
            Intent intent = new Intent(context, ReminderReceiver.class);
            alarmManager.cancel(pendingIntent(taskId, intent));
            for (int slot = 1; slot <= 3; slot++) {
                alarmManager.cancel(pendingIntent(taskId, slot, intent));
            }
        }
    }

    private PendingIntent pendingIntent(long taskId, Intent intent) {
        return pendingIntent(taskId, 0, intent);
    }

    private PendingIntent pendingIntent(long taskId, int slot, Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode(taskId, slot), intent, flags);
    }

    private PendingIntent showIntent(long taskId, int slot, String title) {
        Intent intent = new Intent(context, ReminderAlarmActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        intent.putExtra(Constants.EXTRA_TASK_TITLE, title);
        intent.putExtra(Constants.EXTRA_REMINDER_SLOT, slot);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, requestCode(taskId, slot), intent, flags);
    }

    private int requestCode(long taskId, int slot) {
        long value = taskId * 10L + slot;
        if (value > Integer.MAX_VALUE) {
            value = taskId % Integer.MAX_VALUE;
        }
        return (int) value;
    }
}
