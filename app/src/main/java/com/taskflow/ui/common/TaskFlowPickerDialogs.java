package com.taskflow.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.material.button.MaterialButton;
import com.taskflow.R;

import java.util.Calendar;

public final class TaskFlowPickerDialogs {
    private TaskFlowPickerDialogs() {
    }

    public static void showDatePicker(Context context, DateResult result) {
        Calendar calendar = Calendar.getInstance();
        showDateDialog(
                context,
                "📅 Fecha de entrega",
                "Elige el dia limite para mantener la tarea en camino.",
                calendar,
                (year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 23, 59, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    result.onSelected(selected.getTimeInMillis());
                }
        );
    }

    public static void showDateTimePicker(Context context, DateResult result) {
        Calendar calendar = Calendar.getInstance();
        showDateDialog(
                context,
                "🔔 Dia del recordatorio",
                "Primero elige el dia; despues marcamos la hora exacta.",
                calendar,
                (year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    showTimeDialog(context, selected, result);
                }
        );
    }

    private static void showDateDialog(Context context, String title, String subtitle, Calendar initial, DateSelection selection) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        android.view.View view = LayoutInflater.from(context).inflate(R.layout.dialog_taskflow_date_picker, null, false);
        TextView textTitle = view.findViewById(R.id.textPickerTitle);
        TextView textSubtitle = view.findViewById(R.id.textPickerSubtitle);
        DatePicker datePicker = view.findViewById(R.id.datePickerTaskFlow);
        MaterialButton cancel = view.findViewById(R.id.buttonPickerCancel);
        MaterialButton confirm = view.findViewById(R.id.buttonPickerConfirm);

        textTitle.setText(title);
        textSubtitle.setText(subtitle);
        datePicker.init(
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH),
                null
        );
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            selection.onSelected(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            dialog.dismiss();
        });
        dialog.setContentView(view);
        showPolished(dialog, context);
    }

    private static void showTimeDialog(Context context, Calendar selectedDate, DateResult result) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        android.view.View view = LayoutInflater.from(context).inflate(R.layout.dialog_taskflow_time_picker, null, false);
        TimePicker timePicker = view.findViewById(R.id.timePickerTaskFlow);
        MaterialButton cancel = view.findViewById(R.id.buttonPickerCancel);
        MaterialButton confirm = view.findViewById(R.id.buttonPickerConfirm);

        Calendar now = Calendar.getInstance();
        timePicker.setIs24HourView(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(now.get(Calendar.HOUR_OF_DAY));
            timePicker.setMinute(now.get(Calendar.MINUTE));
        } else {
            timePicker.setCurrentHour(now.get(Calendar.HOUR_OF_DAY));
            timePicker.setCurrentMinute(now.get(Calendar.MINUTE));
        }
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            int hour = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? timePicker.getHour() : timePicker.getCurrentHour();
            int minute = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? timePicker.getMinute() : timePicker.getCurrentMinute();
            selectedDate.set(Calendar.HOUR_OF_DAY, hour);
            selectedDate.set(Calendar.MINUTE, minute);
            selectedDate.set(Calendar.SECOND, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);
            result.onSelected(selectedDate.getTimeInMillis());
            dialog.dismiss();
        });
        dialog.setContentView(view);
        showPolished(dialog, context);
    }

    private static void showPolished(Dialog dialog, Context context) {
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.58f;
        params.windowAnimations = R.style.TaskFlowCenterDialogAnimation;
        window.setAttributes(params);
        int width = context.getResources().getDisplayMetrics().widthPixels - dp(context, 32);
        window.setLayout(Math.min(width, dp(context, 430)), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public interface DateResult {
        void onSelected(long value);
    }

    private interface DateSelection {
        void onSelected(int year, int month, int dayOfMonth);
    }
}
