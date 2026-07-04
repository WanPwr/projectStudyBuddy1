package com.example.projectstudybuddy1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import com.example.projectstudybuddy1.data.SubTaskItem;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private AppDatabase db;
    private PieChart pieChart;
    private TextView tvStreakCountLabel;
    private RecyclerView rvMasterTasks;
    private DashboardListAdapter listAdapter;
    private final List<TaskItem> routineDatasetList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        pieChart = view.findViewById(R.id.todoPieChart);
        tvStreakCountLabel = view.findViewById(R.id.tvStreakCountLabel);
        rvMasterTasks = view.findViewById(R.id.rvMasterTasks);

        if (rvMasterTasks != null) {
            rvMasterTasks.setLayoutManager(new LinearLayoutManager(getContext()));
            listAdapter = new DashboardListAdapter();
            rvMasterTasks.setAdapter(listAdapter);
        }

        loadOverviewAnalyticsData();
        return view;
    }

    private void loadOverviewAnalyticsData() {
        // Fetch values from Room DB
        routineDatasetList.clear();
        routineDatasetList.addAll(db.appDao().getAllTasks());
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }

        // Calculate chart totals
        int totalSubtasks = db.appDao().getTotalSubTaskCount();
        int completedSubtasks = db.appDao().getCheckedSubTaskCount();
        int incompleteSubtasks = totalSubtasks - completedSubtasks;

        if (totalSubtasks == 0) {
            incompleteSubtasks = 1; // Fallback placeholder slice
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) completedSubtasks, "COMPLETED"));
        entries.add(new PieEntry((float) incompleteSubtasks, "INCOMPLETE"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> customColors = new ArrayList<>();
        customColors.add(Color.parseColor("#81B29A")); // Green
        customColors.add(Color.parseColor("#E07A5F")); // Red/Orange
        dataSet.setColors(customColors);

        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        if (pieChart != null) {
            pieChart.setData(data);
            pieChart.getDescription().setEnabled(false);
            pieChart.getLegend().setTextColor(Color.parseColor("#3D405B"));
            pieChart.setUsePercentValues(true);
            pieChart.setEntryLabelColor(Color.TRANSPARENT);
            pieChart.setHoleRadius(0f);
            pieChart.setTransparentCircleRadius(0f);
            pieChart.animateY(1000);
            pieChart.invalidate();
        }
    }

    // =======================================================
    // ADAPTER: OVERVIEW LIST ROWS FOR DASHBOARD
    // =======================================================
    private class DashboardListAdapter extends RecyclerView.Adapter<DashboardListAdapter.DashboardViewHolder> {
        @NonNull
        @Override
        public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rv = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_row, parent, false);
            return new DashboardViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(@NonNull DashboardViewHolder holder, int pos) {
            TaskItem task = routineDatasetList.get(pos);
            holder.tvTitle.setText(task.title);
            holder.tvDate.setText(task.dateCreated);

            List<SubTaskItem> subTasks = db.appDao().getSubTasksForParent(task.taskId);
            int totalSub = subTasks.size();
            int completedSub = 0;

            for (SubTaskItem sub : subTasks) {
                if (sub.isChecked) completedSub++;
            }

            int itemProgressPercent = 0;
            if (totalSub > 0) {
                itemProgressPercent = (completedSub * 100) / totalSub;
            }

            holder.tvPercent.setText(String.format(Locale.getDefault(), "%d%%", itemProgressPercent));
            holder.pbMeter.setProgress(itemProgressPercent);

            // Hide row deletion widget inside main overview dashboard page
            holder.ivDeleteRow.setVisibility(View.GONE);
        }

        @Override public int getItemCount() { return routineDatasetList.size(); }

        class DashboardViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvPercent;
            android.widget.ProgressBar pbMeter;
            ImageView ivDeleteRow;

            public DashboardViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                tvPercent = itemView.findViewById(R.id.tvRowTaskPercentage);
                pbMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                ivDeleteRow = itemView.findViewById(R.id.ivTaskRowDelete);
            }
        }
    }
}