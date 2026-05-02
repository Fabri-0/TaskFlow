package com.taskflow.ui.filter;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taskflow.R;

public class FilterBottomSheet extends BottomSheetDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(36, 28, 36, 28);
        content.setBackgroundColor(requireContext().getColor(R.color.bg_dark_2));
        TextView title = new TextView(requireContext());
        title.setText("Filtros disponibles: todas, hoy, vencidas, completadas y pendientes.");
        title.setTextColor(requireContext().getColor(R.color.text_primary));
        title.setTextSize(16f);
        content.addView(title);
        dialog.setContentView(content);
        return dialog;
    }
}
