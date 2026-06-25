package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import java.util.List;

public class DashboardFragment extends Fragment {
    private AppDatabase db;
    private TextView tvComp, tvIncomp;
    private LinearLayout routineLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        tvComp = view.findViewById(R.id.tvLegendComplete);
        tvIncomp = view.findViewById(R.id.tvLegendIncomplete);
        routineLayout = view.findViewById(R.id.routineDisplayContainer);

        calculateMetrics();
        renderRoutineSummary();
        return view;
    }

    private void calculateMetrics() {
        List<TaskItem> allTodos = db.appDao().getAllTodos();
        if (allTodos.isEmpty()) {
            tvComp.setText("■ 0% COMPLETED");
            tvIncomp.setText("■ 0% INCOMPLETE");
            return;
        }
        int done = 0;
        for (TaskItem t : allTodos) { if (t.isCompleted) done++; }
        int pctDone = (done * 100) / allTodos.size();
        tvComp.setText("■ " + pctDone + "% COMPLETED");
        tvIncomp.setText("■ " + (100 - pctDone) + "% INCOMPLETE");
    }

    private void renderRoutineSummary() {
        routineLayout.removeAllViews();
        List<TaskItem> routines = db.appDao().getAllRoutines();

        for (TaskItem routine : routines) {
            View card = LayoutInflater.from(getContext()).inflate(R.layout.item_routine_row, routineLayout, false);
            TextView tvPct = card.findViewById(R.id.tvRoutinePercent);
            TextView tvName = card.findViewById(R.id.tvRoutineName);

            tvName.setText(routine.title.toUpperCase());
            if (routine.isCompleted) {
                tvPct.setText("100%");
                card.setBackgroundResource(R.drawable.bg_routine_complete);
                tvPct.setTextColor(android.graphics.Color.WHITE);
                tvName.setTextColor(android.graphics.Color.WHITE);
            } else {
                tvPct.setText("0%");
                card.setBackgroundResource(R.drawable.bg_rounded_card);
            }
            routineLayout.addView(card);
        }
    }
}