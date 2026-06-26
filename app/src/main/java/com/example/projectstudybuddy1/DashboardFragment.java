package com.example.projectstudybuddy1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private AppDatabase db;
    private PieChart pieChart;
    private TextView tvStreakCountLabel;
    private MasterTaskAdapter masterAdapter;
    private final List<TaskItem> masterTaskList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        pieChart = v.findViewById(R.id.todoPieChart);
        tvStreakCountLabel = v.findViewById(R.id.tvStreakCountLabel);

        RecyclerView rvMaster = v.findViewById(R.id.rvMasterTasks);
        rvMaster.setLayoutManager(new LinearLayoutManager(getContext()));
        masterAdapter = new MasterTaskAdapter();
        rvMaster.setAdapter(masterAdapter);

        loadDashboardMetrics();
        evaluateDailyStreakCheckIn();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardMetrics();
        refreshStreakDisplay();
    }

    private void refreshStreakDisplay() {
        if (tvStreakCountLabel == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);
        int currentStreak = prefs.getInt("user_streak_count", 1);
        tvStreakCountLabel.setText(String.format(Locale.getDefault(), "🔥 Current Streak: %d Days", currentStreak));
    }

    private void evaluateDailyStreakCheckIn() {
        SharedPreferences prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayString = sdf.format(new Date());
        String lastCheckInDate = prefs.getString("last_checkin_date", "");

        if (todayString.equals(lastCheckInDate)) {
            refreshStreakDisplay();
            return;
        }

        int currentStreak = prefs.getInt("user_streak_count", 0);
        try {
            if (!lastCheckInDate.isEmpty()) {
                Calendar todayCal = Calendar.getInstance();
                Calendar prevCal = Calendar.getInstance();
                prevCal.setTime(sdf.parse(lastCheckInDate));
                prevCal.add(Calendar.DAY_OF_YEAR, 1);

                String expectedStreakDay = sdf.format(prevCal.getTime());
                if (todayString.equals(expectedStreakDay)) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
            } else {
                currentStreak = 1;
            }
        } catch (Exception e) {
            currentStreak = 1;
        }

        final int verifiedStreakValue = currentStreak;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_daily_checkin, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        dialog.setView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvSubtitle = dialogView.findViewById(R.id.tvCheckInSubtitle);
        tvSubtitle.setText(String.format(Locale.getDefault(), "Welcome back! You are on a %d day streak!", verifiedStreakValue));

        dialogView.findViewById(R.id.btnCheckInConfirm).setOnClickListener(v -> {
            prefs.edit()
                    .putString("last_checkin_date", todayString)
                    .putInt("user_streak_count", verifiedStreakValue)
                    .apply();

            refreshStreakDisplay();
            dialog.dismiss();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void loadDashboardMetrics() {
        int previousSize = masterTaskList.size();
        masterTaskList.clear();
        masterTaskList.addAll(db.appDao().getAllTasks());
        masterAdapter.notifyItemRangeChanged(0, Math.max(previousSize, masterTaskList.size()));

        int totalItems = db.appDao().getTotalSubTaskCount();
        int completedItems = db.appDao().getCheckedSubTaskCount();
        int incompleteItems = totalItems - completedItems;

        if (totalItems == 0) {
            incompleteItems = 1;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) completedItems, "COMPLETED"));
        entries.add(new PieEntry((float) incompleteItems, "INCOMPLETE"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> customColors = new ArrayList<>();
        customColors.add(Color.parseColor("#81B29A"));
        customColors.add(Color.parseColor("#E07A5F"));
        dataSet.setColors(customColors);

        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

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