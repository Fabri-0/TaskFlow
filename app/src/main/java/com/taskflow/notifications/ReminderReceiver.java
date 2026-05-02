package com.taskflow.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.taskflow.R;
import com.taskflow.ui.task.TaskDetailActivity;
import com.taskflow.utils.Constants;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.ensureChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        long taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, -1L);
        String title = intent.getStringExtra(Constants.EXTRA_TASK_TITLE);
        Intent openIntent = new Intent(context, TaskDetailActivity.class);
        openIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) taskId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Recordatorio TaskFlow")
                .setContentText(title == null ? "Tienes una tarea pendiente." : title)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManagerCompat.from(context).notify((int) taskId, builder.build());
    }
}
