package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import com.example.projectstudybuddy1.data.SubTaskItem;

// MPAndroidChart Library Components
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private AppDatabase db;
    private RecyclerView rvIndividualTodoCompletion;
    private DashboardTaskAdapter adapter;
    private final List<TaskItem> pureTodoList = new ArrayList<>();

    private TextView tvWelcomeHeader;
    private PieChart pieChartTodo;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        tvWelcomeHeader = v.findViewById(R.id.tvWelcomeUserHeader);
        pieChartTodo = v.findViewById(R.id.todoPieChart);
        rvIndividualTodoCompletion = v.findViewById(R.id.rvMasterTasks);

        ImageView ivAvatar = v.findViewById(R.id.ivDashboardAvatar);
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(view -> {
                Intent goToProfile = new Intent(getActivity(), ProfileActivity.class);
                startActivity(goToProfile);
            });
        }

        if (pieChartTodo != null) {
            configurePieChartAppearance();
        }

        if (rvIndividualTodoCompletion != null) {
            rvIndividualTodoCompletion.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new DashboardTaskAdapter();
            rvIndividualTodoCompletion.setAdapter(adapter);
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (tvWelcomeHeader != null && prefs != null) {
            String activeUser = prefs.getString("username", "user");
            tvWelcomeHeader.setText("WELCOME BACK, " + activeUser.toUpperCase(Locale.US));
        }

        loadDashboardMetrics();
    }

    private void configurePieChartAppearance() {
        pieChartTodo.getDescription().setEnabled(false);
        pieChartTodo.getLegend().setEnabled(false);
        pieChartTodo.setUsePercentValues(true);
        pieChartTodo.setDrawHoleEnabled(true);
        pieChartTodo.setHoleColor(Color.TRANSPARENT);
        pieChartTodo.setTransparentCircleRadius(0f);
        pieChartTodo.setHoleRadius(40f);

        pieChartTodo.setNoDataText("Calculating completion metrics...");
        pieChartTodo.setNoDataTextColor(Color.parseColor("#3D405B"));
        pieChartTodo.invalidate();
    }

    private void loadDashboardMetrics() {
        pureTodoList.clear();

        // Retrieve current active user integer ID from preferences context
        int activeUserId = prefs.getInt("userId", 1);

        List<TaskItem> allItems = db.appDao().getAllTasks(activeUserId);

        int totalSubTasksCount = 0;
        int completedSubTasksCount = 0;

        for (TaskItem item : allItems) {
            if (item.title != null) {
                boolean isJournal = item.title.startsWith("JOURNAL_NOTE:");
                boolean isFlashcardDeck = item.title.startsWith("DECK_NOTE:");

                if (!isJournal && !isFlashcardDeck) {
                    pureTodoList.add(item);

                    List<SubTaskItem> subs = db.appDao().getSubTasksForParent(item.taskId);
                    if (subs.isEmpty()) {
                        totalSubTasksCount++;
                        if (item.completed) {
                            completedSubTasksCount++;
                        }
                    } else {
                        for (SubTaskItem sub : subs) {
                            totalSubTasksCount++;
                            if (sub.isChecked) {
                                completedSubTasksCount++;
                            }
                        }
                    }
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        calculateAndRenderPieChart(completedSubTasksCount, totalSubTasksCount);
    }

    private void calculateAndRenderPieChart(int completed, int total) {
        if (pieChartTodo == null) return;

        pieChartTodo.setData(null);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> assignedColors = new ArrayList<>();

        int colorGreen = Color.parseColor("#81B29A");
        int colorOrangeRed = Color.parseColor("#E07A5F");

        if (total == 0 || completed == 0) {
            entries.add(new PieEntry(100f, "INCOMPLETE"));
            assignedColors.add(colorOrangeRed);
        }
        else if (completed == total) {
            entries.add(new PieEntry(100f, "COMPLETED"));
            assignedColors.add(colorGreen);
        }
        else {
            float completedPercent = ((float) completed / total) * 100f;
            float incompletePercent = 100f - completedPercent;

            entries.add(new PieEntry(completedPercent, "COMPLETED"));
            assignedColors.add(colorGreen);

            entries.add(new PieEntry(incompletePercent, "INCOMPLETE"));
            assignedColors.add(colorOrangeRed);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(assignedColors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int rounded = (int) Math.round(value);
                if (total == 0 || rounded == 0) return "";
                return rounded + "%";
            }
        });

        PieData data = new PieData(dataSet);
        if (total == 0 || completed == 0) {
            data.setValueTextSize(0f);
        }

        pieChartTodo.setData(data);
        pieChartTodo.setEntryLabelColor(Color.WHITE);
        pieChartTodo.setEntryLabelTextSize(10f);

        pieChartTodo.notifyDataSetChanged();
        pieChartTodo.invalidate();
    }

    private class DashboardTaskAdapter extends RecyclerView.Adapter<DashboardTaskAdapter.DashboardViewHolder> {
        @NonNull
        @Override
        public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View r = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_row, parent, false);
            return new DashboardViewHolder(r);
        }

        @Override
        public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {
            TaskItem task = pureTodoList.get(position);
            holder.tvTitle.setText(task.title);
            holder.tvDate.setText(task.dateCreated);

            List<SubTaskItem> subTasks = db.appDao().getSubTasksForParent(task.taskId);
            int totalSubs = subTasks.size();
            int checkedSubs = 0;

            for (SubTaskItem sub : subTasks) {
                if (sub.isChecked) checkedSubs++;
            }

            int itemProgress = 0;
            if (totalSubs > 0) {
                itemProgress = (checkedSubs * 100) / totalSubs;
            }

            holder.tvPercent.setText(String.format(Locale.getDefault(), "%d%%", itemProgress));
            holder.pbMeter.setProgress(itemProgress);
        }

        @Override
        public int getItemCount() { return pureTodoList.size(); }

        class DashboardViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvPercent;
            ProgressBar pbMeter;
            View ivDeleteIcon;

            public DashboardViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                tvPercent = itemView.findViewById(R.id.tvRowTaskPercentage);
                pbMeter = itemView.findViewById(R.id.pbRowTaskMeter);

                ivDeleteIcon = itemView.findViewById(R.id.ivTaskRowDelete);
                if (ivDeleteIcon != null) ivDeleteIcon.setVisibility(View.GONE);
            }
        }
    }
}