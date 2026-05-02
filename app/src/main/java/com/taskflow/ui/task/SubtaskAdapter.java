package com.taskflow.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taskflow.R;
import com.taskflow.data.local.entity.SubtaskEntity;

import java.util.ArrayList;
import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.Holder> {
    public interface Listener {
        void onSubtaskToggled(SubtaskEntity subtask, boolean completed);
    }

    private final Listener listener;
    private final List<SubtaskEntity> subtasks = new ArrayList<>();

    public SubtaskAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<SubtaskEntity> values) {
        subtasks.clear();
        if (values != null) {
            subtasks.addAll(values);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtask, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(subtasks.get(position));
    }

    @Override
    public int getItemCount() {
        return subtasks.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        final CheckBox checkSubtask;
        final TextView textSubtaskTitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            checkSubtask = itemView.findViewById(R.id.checkSubtask);
            textSubtaskTitle = itemView.findViewById(R.id.textSubtaskTitle);
        }

        void bind(SubtaskEntity subtask) {
            checkSubtask.setOnCheckedChangeListener(null);
            checkSubtask.setChecked(subtask.isCompleted);
            textSubtaskTitle.setText(subtask.title);
            checkSubtask.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onSubtaskToggled(subtask, isChecked));
        }
    }
}
