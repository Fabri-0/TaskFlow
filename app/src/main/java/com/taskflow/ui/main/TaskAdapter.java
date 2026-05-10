package com.taskflow.ui.main;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taskflow.R;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.utils.Constants;
import com.taskflow.utils.DateUtils;
import com.taskflow.utils.ProgressUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TASK = 1;

    public interface Listener {
        void onTaskClicked(TaskFull task);

        void onTaskCompleted(TaskFull task, boolean completed);

        default void onTaskLongPressed(TaskFull task) {
        }

        default void onTaskMoved(TaskFull task, int adapterPosition) {
        }
    }

    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();

    public TaskAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<TaskFull> tasks) {
        rows.clear();
        Map<String, List<TaskFull>> grouped = new LinkedHashMap<>();
        grouped.put("Vencidas / Anterior", new ArrayList<>());
        grouped.put("Hoy", new ArrayList<>());
        grouped.put("Proximas 7 dias", new ArrayList<>());
        grouped.put("Sin fecha", new ArrayList<>());
        grouped.put("Mas adelante", new ArrayList<>());
        grouped.put("Archivadas", new ArrayList<>());
        grouped.put("Papelera", new ArrayList<>());
        if (tasks != null) {
            for (TaskFull task : tasks) {
                grouped.get(sectionFor(task)).add(task);
            }
        }
        for (Map.Entry<String, List<TaskFull>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                rows.add(Row.header(entry.getKey()));
                for (TaskFull task : entry.getValue()) {
                    rows.add(Row.task(task));
                }
            }
        }
        if (rows.isEmpty()) {
            rows.add(Row.header("Sin tareas para mostrar"));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_section_header, parent, false));
        }
        return new TaskHolder(inflater.inflate(R.layout.item_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).textHeader.setText(row.title);
        } else if (holder instanceof TaskHolder) {
            ((TaskHolder) holder).bind(row.task);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public TaskFull getTaskAt(int position) {
        if (position < 0 || position >= rows.size()) {
            return null;
        }
        return rows.get(position).task;
    }

    public boolean isTaskRow(int position) {
        return position >= 0 && position < rows.size() && rows.get(position).type == TYPE_TASK;
    }

    public void moveTaskRow(int fromPosition, int toPosition) {
        if (!isTaskRow(fromPosition) || !isTaskRow(toPosition)) {
            return;
        }
        Row row = rows.remove(fromPosition);
        rows.add(toPosition, row);
        notifyItemMoved(fromPosition, toPosition);
        listener.onTaskMoved(row.task, toPosition);
    }

    private String sectionFor(TaskFull task) {
        if (task == null || task.task == null) {
            return "Mas adelante";
        }
        if (task.task.isDeleted) {
            return "Papelera";
        }
        if (task.task.isArchived) {
            return "Archivadas";
        }
        if (DateUtils.isOverdue(task.task.dueDate, task.task.isCompleted)) {
            return "Vencidas / Anterior";
        }
        Long dueDate = task.task.dueDate;
        if (dueDate == null) {
            return "Sin fecha";
        }
        if (dueDate != null && dueDate >= DateUtils.startOfToday() && dueDate <= DateUtils.endOfToday()) {
            return "Hoy";
        }
        long sevenDays = DateUtils.startOfToday() + 7L * 24L * 60L * 60L * 1000L;
        if (dueDate <= sevenDays) {
            return "Proximas 7 dias";
        }
        return "Mas adelante";
    }

    private static class Row {
        final int type;
        final String title;
        final TaskFull task;

        private Row(int type, String title, TaskFull task) {
            this.type = type;
            this.title = title;
            this.task = task;
        }

        static Row header(String title) {
            return new Row(TYPE_HEADER, title, null);
        }

        static Row task(TaskFull task) {
            return new Row(TYPE_TASK, null, task);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView textHeader;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            textHeader = itemView.findViewById(R.id.textHeader);
        }
    }

    class TaskHolder extends RecyclerView.ViewHolder {
        final CheckBox checkCompleted;
        final TextView textTitle;
        final TextView textMeta;
        final TextView textProgressSmall;
        final TextView textStar;

        TaskHolder(@NonNull View itemView) {
            super(itemView);
            checkCompleted = itemView.findViewById(R.id.checkCompleted);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMeta = itemView.findViewById(R.id.textMeta);
            textProgressSmall = itemView.findViewById(R.id.textProgressSmall);
            textStar = itemView.findViewById(R.id.textStar);
        }

        void bind(TaskFull taskFull) {
            if (taskFull == null || taskFull.task == null) {
                return;
            }
            checkCompleted.setOnCheckedChangeListener(null);
            checkCompleted.setChecked(taskFull.task.isCompleted);
            textTitle.setText(taskFull.task.title);
            textTitle.setPaintFlags(taskFull.task.isCompleted
                    ? textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            String project = taskFull.project == null ? "Sin categoria" : taskFull.project.name;
            textMeta.setText(metaText(taskFull, project));
            int done = ProgressUtils.completedSubtasks(taskFull.subtasks);
            int total = taskFull.subtasks == null ? 0 : taskFull.subtasks.size();
            String small = total == 0 ? "☘️ Sin subtareas" : "☘️ Subtareas " + ProgressUtils.formatCounter(done, total);
            if (!taskFull.task.recurrenceType.isEmpty()) {
                small += " - " + DateUtils.recurrenceLabel(taskFull.task.recurrenceType, taskFull.task.recurrenceInterval);
            }
            if (!taskFull.task.attachment.isEmpty()) {
                small += " - 🖼️ imagen";
            }
            textProgressSmall.setText(small);
            textStar.setVisibility(taskFull.task.isStarred ? View.VISIBLE : View.INVISIBLE);
            textStar.setText(priorityBadge(taskFull.task.priority, taskFull.task.isStarred));
            itemView.setOnClickListener(v -> listener.onTaskClicked(taskFull));
            itemView.setOnLongClickListener(v -> {
                listener.onTaskLongPressed(taskFull);
                return true;
            });
            checkCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onTaskCompleted(taskFull, isChecked));
        }

        private String metaText(TaskFull taskFull, String project) {
            StringBuilder builder = new StringBuilder();
            builder.append(DateUtils.formatDate(taskFull.task.dueDate)).append(" - ").append(project);
            if (taskFull.tags != null && !taskFull.tags.isEmpty()) {
                builder.append(" - ");
                for (int i = 0; i < taskFull.tags.size(); i++) {
                    TagEntity tag = taskFull.tags.get(i);
                    if (tag == null) {
                        continue;
                    }
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(tag.name);
                }
            }
            builder.append(" - ").append(priorityLabel(taskFull.task.priority));
            return builder.toString();
        }

        private String priorityLabel(int priority) {
            if (priority >= Constants.PRIORITY_URGENT) {
                return "Urgente";
            }
            if (priority >= Constants.PRIORITY_HIGH) {
                return "Alta";
            }
            if (priority >= Constants.PRIORITY_MEDIUM) {
                return "Media";
            }
            return "Baja";
        }

        private String priorityBadge(int priority, boolean starred) {
            if (priority >= Constants.PRIORITY_URGENT) {
                return "!";
            }
            if (priority >= Constants.PRIORITY_HIGH) {
                return "^";
            }
            return starred ? "*" : "";
        }
    }
}
