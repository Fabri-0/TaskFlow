package com.taskflow.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class DateUtils {
    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private DateUtils() {
    }

    public static long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long endOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static long startOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long endOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static String formatDateTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return "Sin fecha";
        }
        return DATE_TIME_FORMAT.format(timestamp);
    }

    public static String formatDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return "Sin fecha";
        }
        return DATE_FORMAT.format(timestamp);
    }

    public static boolean isOverdue(Long dueDate, boolean completed) {
        return dueDate != null && !completed && dueDate < startOfToday();
    }

    public static long addRecurrence(long timestamp, String recurrenceType, int interval) {
        int safeInterval = Math.max(1, interval);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        if (Constants.RECURRENCE_DAILY.equals(recurrenceType)) {
            calendar.add(Calendar.DAY_OF_MONTH, safeInterval);
        } else if (Constants.RECURRENCE_WEEKLY.equals(recurrenceType)) {
            calendar.add(Calendar.WEEK_OF_YEAR, safeInterval);
        } else if (Constants.RECURRENCE_MONTHLY.equals(recurrenceType)) {
            calendar.add(Calendar.MONTH, safeInterval);
        }
        return calendar.getTimeInMillis();
    }

    public static String recurrenceLabel(String recurrenceType, int interval) {
        int safeInterval = Math.max(1, interval);
        if (Constants.RECURRENCE_DAILY.equals(recurrenceType)) {
            return safeInterval == 1 ? "Cada dia" : "Cada " + safeInterval + " dias";
        }
        if (Constants.RECURRENCE_WEEKLY.equals(recurrenceType)) {
            return safeInterval == 1 ? "Cada semana" : "Cada " + safeInterval + " semanas";
        }
        if (Constants.RECURRENCE_MONTHLY.equals(recurrenceType)) {
            return safeInterval == 1 ? "Cada mes" : "Cada " + safeInterval + " meses";
        }
        return "No se repite";
    }
}
