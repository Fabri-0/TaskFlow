package com.taskflow.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.taskflow.R;

import java.util.ArrayList;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayHolder> {
    public interface Listener {
        void onDayClicked(CalendarDay day);
    }

    private final Listener listener;
    private final List<CalendarDay> days = new ArrayList<>();

    public CalendarDayAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CalendarDay> values) {
        days.clear();
        if (values != null) {
            days.addAll(values);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayHolder holder, int position) {
        holder.bind(days.get(position));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class DayHolder extends RecyclerView.ViewHolder {
        final View dayContainer;
        final TextView textDayNumber;
        final TextView textDayMarker;

        DayHolder(@NonNull View itemView) {
            super(itemView);
            dayContainer = itemView.findViewById(R.id.dayContainer);
            textDayNumber = itemView.findViewById(R.id.textDayNumber);
            textDayMarker = itemView.findViewById(R.id.textDayMarker);
        }

        void bind(CalendarDay day) {
            textDayNumber.setText(String.valueOf(day.dayOfMonth));
            textDayMarker.setText(day.taskCount > 0 ? "✨ " + day.taskCount : "");
            dayContainer.setAlpha(day.currentMonth ? 1f : 0.35f);
            textDayNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            textDayMarker.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_teal));
            textDayNumber.setBackground(null);
            dayContainer.setBackgroundResource(R.drawable.bg_calendar_cell);
            if (day.selected) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_selected_cell);
                textDayNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.on_primary));
                textDayMarker.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.on_primary));
            } else if (day.taskCount > 0) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_task_cell);
                textDayNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            } else if (day.today) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_today_cell);
                textDayNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_blue));
            }
            itemView.setOnClickListener(v -> listener.onDayClicked(day));
        }
    }

    public static class CalendarDay {
        public final long dateMillis;
        public final int dayOfMonth;
        public final boolean currentMonth;
        public final boolean today;
        public final boolean selected;
        public final int taskCount;

        public CalendarDay(long dateMillis, int dayOfMonth, boolean currentMonth, boolean today, boolean selected, int taskCount) {
            this.dateMillis = dateMillis;
            this.dayOfMonth = dayOfMonth;
            this.currentMonth = currentMonth;
            this.today = today;
            this.selected = selected;
            this.taskCount = taskCount;
        }
    }
}
