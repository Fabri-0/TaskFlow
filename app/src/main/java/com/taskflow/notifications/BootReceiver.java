package com.taskflow.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.taskflow.data.local.db.AppDatabase;
import com.taskflow.data.local.entity.TaskEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            List<TaskEntity> tasks = AppDatabase.getInstance(context)
                    .taskDao()
                    .getFutureReminderTasks(System.currentTimeMillis());
            ReminderScheduler scheduler = new ReminderScheduler(context);
            for (TaskEntity task : tasks) {
                if (task.reminderDate != null) {
                    scheduler.schedule(task.id, 1, task.title, task.reminderDate);
                }
                if (task.reminderDate2 != null) {
                    scheduler.schedule(task.id, 2, task.title, task.reminderDate2);
                }
                if (task.reminderDate3 != null) {
                    scheduler.schedule(task.id, 3, task.title, task.reminderDate3);
                }
            }
        });
    }
}
