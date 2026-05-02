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
import com.taskflow.ui.task.TaskDetailActivity;
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
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (sound == null) {
                sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(sound, attributes);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public boolean canNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public boolean schedule(long taskId, String title, long reminderAt) {
        if (reminderAt <= System.currentTimeMillis()) {
            return false;
        }
        ensureChannel(context);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        intent.putExtra(Constants.EXTRA_TASK_TITLE, title);
        PendingIntent pendingIntent = pendingIntent(taskId, intent);
        if (alarmManager == null) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(reminderAt, showIntent(taskId)), pendingIntent);
                return true;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent);
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
        }
    }

    private PendingIntent pendingIntent(long taskId, Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, (int) taskId, intent, flags);
    }

    private PendingIntent showIntent(long taskId) {
        Intent intent = new Intent(context, TaskDetailActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, (int) taskId, intent, flags);
    }
}
