package com.example.projectstudybuddy1;

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
    private MasterTaskAdapter masterAdapter;
    private final List<TaskItem> masterTaskList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        // Bind the Pie Chart component view
        pieChart = v.findViewById(R.id.todoPieChart);

        RecyclerView rvMaster = v.findViewById(R.id.rvMasterTasks);
        rvMaster.setLayoutManager(new LinearLayoutManager(getContext()));
        masterAdapter = new MasterTaskAdapter();
        rvMaster.setAdapter(masterAdapter);

        loadDashboardMetrics();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardMetrics();
    }

    private void loadDashboardMetrics() {
        int previousSize = masterTaskList.size();
        masterTaskList.clear();
        masterTaskList.addAll(db.appDao().getAllTasks());
        masterAdapter.notifyItemRangeChanged(0, Math.max(previousSize, masterTaskList.size()));

        // Calculate database metrics
        int totalItems = db.appDao().getTotalSubTaskCount();
        int completedItems = db.appDao().getCheckedSubTaskCount();
        int incompleteItems = totalItems - completedItems;

        // Fallback display initialization when no data entries exist yet
        if (totalItems == 0) {
            incompleteItems = 1;
        }

        // Configure datasets following the Figma design layout
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) completedItems, "COMPLETED"));
        entries.add(new PieEntry((float) incompleteItems, "INCOMPLETE"));

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Match color scheme: Mint Green vs Terracotta Coral Red
        ArrayList<Integer> customColors = new ArrayList<>();
        customColors.add(Color.parseColor("#81B29A"));
        customColors.add(Color.parseColor("#E07A5F"));
        dataSet.setColors(customColors);

        // Fixed syntax errors: passing float values directly to formatting methods
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Customize layout structure of the chart space
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setTextColor(Color.parseColor("#3D405B"));
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(0f);
        pieChart.setTransparentCircleRadius(0f);

        pieChart.animateY(1200);
        pieChart.invalidate();
    }

    private class MasterTaskAdapter extends RecyclerView.Adapter<MasterTaskAdapter.MasterViewHolder> {
        @NonNull
        @Override
        public MasterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View rv = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_row, parent, false);
            return new MasterViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(@NonNull MasterViewHolder holder, int pos) {
            TaskItem task = masterTaskList.get(pos);
            holder.tvTitle.setText(task.title);
            holder.tvDate.setText(task.dateCreated);

            List<SubTaskItem> subTasks = db.appDao().getSubTasksForParent(task.taskId);
            int totalSubTasks = subTasks.size();
            int completedSubTasks = 0;

            for (SubTaskItem sub : subTasks) {
                if (sub.isChecked) {
                    completedSubTasks++;
                }
            }

            int itemProgressPercent = 0;
            if (totalSubTasks > 0) {
                itemProgressPercent = (completedSubTasks * 100) / totalSubTasks;
            }

            holder.tvItemPercent.setText(String.format(Locale.getDefault(), "%d%%", itemProgressPercent));
            holder.pbItemMeter.setProgress(itemProgressPercent);

            holder.ivDelete.setOnClickListener(view -> {
                int indexPosition = holder.getBindingAdapterPosition();
                if (indexPosition != RecyclerView.NO_POSITION) {
                    db.appDao().deleteTask(task);
                    masterTaskList.remove(indexPosition);
                    notifyItemRemoved(indexPosition);
                    loadDashboardMetrics();
                }
            });
        }

        @Override
        public int getItemCount() { return masterTaskList.size(); }

        class MasterViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvItemPercent;
            ImageView ivDelete;
            ProgressBar pbItemMeter;

            public MasterViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                ivDelete = itemView.findViewById(R.id.ivTaskRowDelete);
                pbItemMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                tvItemPercent = itemView.findViewById(R.id.tvRowTaskPercentage);
            }
        }
    }
}