package com.taskflow.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.taskflow.R;
import com.taskflow.data.repository.TaskRepository;
import com.taskflow.ui.task.ReminderAlarmActivity;
import com.taskflow.utils.Constants;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Constants.ACTION_COMPLETE_TASK.equals(intent.getAction())) {
            long taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, -1L);
            if (taskId > 0) {
                new TaskRepository(context).completeTaskFromNotification(taskId);
            }
            return;
        }
        if (intent != null && Constants.ACTION_SNOOZE_TASK.equals(intent.getAction())) {
            long taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, -1L);
            String title = intent.getStringExtra(Constants.EXTRA_TASK_TITLE);
            if (taskId > 0) {
                new ReminderScheduler(context).schedule(taskId, 1, title, System.currentTimeMillis() + 5L * 60L * 1000L);
                NotificationManagerCompat.from(context).cancel((int) taskId);
            }
            return;
        }
        if (intent != null && Constants.ACTION_DISMISS_REMINDER.equals(intent.getAction())) {
            long taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, -1L);
            if (taskId > 0) {
                NotificationManagerCompat.from(context).cancel((int) taskId);
            }
            return;
        }
        if (intent == null) {
            return;
        }
        if (!PermissionPrefs.isAlarmsEnabled(context)) {
            return;
        }
        ReminderScheduler.ensureChannel(context);
        long taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, -1L);
        String title = intent.getStringExtra(Constants.EXTRA_TASK_TITLE);
        int slot = intent.getIntExtra(Constants.EXTRA_REMINDER_SLOT, 1);
        Intent openIntent = new Intent(context, ReminderAlarmActivity.class);
        openIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        openIntent.putExtra(Constants.EXTRA_TASK_TITLE, title);
        openIntent.putExtra(Constants.EXTRA_REMINDER_SLOT, slot);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            try {
                context.startActivity(openIntent);
            } catch (RuntimeException ignored) {
            }
            return;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) taskId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent snoozeIntent = actionIntent(context, Constants.ACTION_SNOOZE_TASK, taskId, title, slot + 20);
        PendingIntent dismissIntent = actionIntent(context, Constants.ACTION_DISMISS_REMINDER, taskId, title, slot + 30);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) {
            sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        long[] vibration = new long[]{0L, 350L, 220L, 550L, 220L, 850L, 220L, 1200L};
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Alarma de tarea")
                .setContentText(title == null ? "Tienes una tarea pendiente." : title)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_notifications_24, "Posponer 5 min", snoozeIntent)
                .addAction(R.drawable.ic_close_24, "Apagar", dismissIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(PermissionPrefs.isVibrationEnabled(context) ? Notification.DEFAULT_ALL : Notification.DEFAULT_SOUND)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(sound)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (PermissionPrefs.isFullscreenEnabled(context)) {
            builder.setFullScreenIntent(contentIntent, true);
        }
        if (PermissionPrefs.isVibrationEnabled(context)) {
            builder.setVibrate(vibration);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSound(sound, android.media.AudioManager.STREAM_ALARM);
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_INSISTENT;
        NotificationManagerCompat.from(context).notify((int) taskId, notification);
        try {
            if (PermissionPrefs.isFullscreenEnabled(context)) {
                context.startActivity(openIntent);
            }
        } catch (RuntimeException ignored) {
            // Full-screen notification remains available if background launch is restricted.
        }
    }

    private PendingIntent actionIntent(Context context, String action, long taskId, String title, int requestSlot) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(action);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        intent.putExtra(Constants.EXTRA_TASK_TITLE, title);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, (int) (taskId * 10L + requestSlot), intent, flags);
    }
}
