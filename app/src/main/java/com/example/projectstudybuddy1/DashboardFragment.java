package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private AppDatabase db;
    private RecyclerView rvIndividualTodoCompletion;
    private DashboardTaskAdapter adapter;
    private final List<TaskItem> pureTodoList = new ArrayList<>();

    private TextView tvWelcomeHeader;
    private TextView tvEmptyTasksPlaceholder;
    private PieChart pieChartTodo;
    private SharedPreferences prefs;

    // Streak System View Binding Array & Tracking Storage Configuration
    private final TextView[] tvStreakDaysArray = new TextView[7];
    private static final String STREAK_PREFS_BASE = "StreakTrackingPrefs_User_";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = AppDatabase.getDatabase(requireContext());
        prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        tvWelcomeHeader = v.findViewById(R.id.tvWelcomeUserHeader);
        pieChartTodo = v.findViewById(R.id.todoPieChart);
        rvIndividualTodoCompletion = v.findViewById(R.id.rvMasterTasks);
        tvEmptyTasksPlaceholder = v.findViewById(R.id.tvEmptyTasksPlaceholder);
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

        // Bind circular streak TextView resources into the memory list array pointers
        tvStreakDaysArray[0] = v.findViewById(R.id.tvStreakDay1);
        tvStreakDaysArray[1] = v.findViewById(R.id.tvStreakDay2);
        tvStreakDaysArray[2] = v.findViewById(R.id.tvStreakDay3);
        tvStreakDaysArray[3] = v.findViewById(R.id.tvStreakDay4);
        tvStreakDaysArray[4] = v.findViewById(R.id.tvStreakDay5);
        tvStreakDaysArray[6] = v.findViewById(R.id.tvStreakDay7);

        // Run isolated multi-account streak detection tracking logic sequences
        evaluateDailyCheckinLifecycle();

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
        renderStreakMeterVisuals();
    }

    // Helper method to retrieve the unique SharedPreferences file name for the logged-in account
    private SharedPreferences getIsolatedUserStreakPrefs() {
        int activeUserId = prefs.getInt("userId", 1);
        String userSpecificPrefsName = STREAK_PREFS_BASE + activeUserId;
        return requireContext().getSharedPreferences(userSpecificPrefsName, Context.MODE_PRIVATE);
    }

    private void evaluateDailyCheckinLifecycle() {
        if (getContext() == null) return;
        SharedPreferences streakPrefs = getIsolatedUserStreakPrefs();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todaySignature = sdf.format(new Date());
        String lastCheckinDate = streakPrefs.getString("last_checkin_date_stamp", "");

        // 1. If they haven't logged a check-in record yet today, verify if their consecutive streak is alive
        if (!todaySignature.equals(lastCheckinDate)) {
            if (!lastCheckinDate.isEmpty()) {
                try {
                    Date today = sdf.parse(todaySignature);
                    Date lastCheck = sdf.parse(lastCheckinDate);

                    long diffInMillis = Math.abs(today.getTime() - lastCheck.getTime());
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                    // Reset streak if they missed more than 1 calendar day since last logging in
                    if (diffInDays > 1) {
                        streakPrefs.edit().putInt("current_streak_count", 0).apply();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            renderStreakMeterVisuals();
            displayDailyCheckinModalDialog(todaySignature);
        } else {
            renderStreakMeterVisuals();
        }
    }

    private void renderStreakMeterVisuals() {
        if (getContext() == null) return;
        SharedPreferences streakPrefs = getIsolatedUserStreakPrefs();

        int currentStreak = streakPrefs.getInt("current_streak_count", 0);

        for (int i = 0; i < 7; i++) {
            if (tvStreakDaysArray[i] != null) {
                if (i < currentStreak) {
                    tvStreakDaysArray[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E07A5F")));
                    tvStreakDaysArray[i].setTextColor(Color.WHITE);
                } else {
                    tvStreakDaysArray[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C4A484")));
                    tvStreakDaysArray[i].setTextColor(Color.parseColor("#3D405B"));
                }
            }
        }
    }

    private void displayDailyCheckinModalDialog(String todaySignature) {
        if (getActivity() == null || !isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_daily_checkin, null);
        builder.setView(dialogView);

        AlertDialog alert = builder.create();
        if (alert.getWindow() != null) {
            alert.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnLetsGo = dialogView.findViewById(R.id.btnCheckInConfirm);
        if (btnLetsGo != null) {
            btnLetsGo.setOnClickListener(v -> {
                SharedPreferences streakPrefs = getIsolatedUserStreakPrefs();

                int currentStreak = streakPrefs.getInt("current_streak_count", 0);
                currentStreak++;

                if (currentStreak > 7) {
                    currentStreak = 7;
                }

                streakPrefs.edit()
                        .putString("last_checkin_date_stamp", todaySignature)
                        .putInt("current_streak_count", currentStreak)
                        .apply();

                renderStreakMeterVisuals();
                alert.dismiss();
                Toast.makeText(getContext(), "Check-In Complete! Current Streak: " + currentStreak + " Days!", Toast.LENGTH_SHORT).show();
            });
        }

        alert.setCancelable(false);
        alert.show();
    }

    private void configurePieChartAppearance() {
        pieChartTodo.getDescription().setEnabled(false);
        pieChartTodo.getLegend().setEnabled(false);
        pieChartTodo.setUsePercentValues(true);
        pieChartTodo.setDrawHoleEnabled(true);
        pieChartTodo.setHoleColor(Color.TRANSPARENT);
        pieChartTodo.setTransparentCircleRadius(0f);
        pieChartTodo.setHoleRadius(40f);

        pieChartTodo.setNoDataText("No tasks available");
        pieChartTodo.setNoDataTextColor(Color.parseColor("#3D405B"));
        pieChartTodo.invalidate();
    }

    private void loadDashboardMetrics() {
        pureTodoList.clear();

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

        if (tvEmptyTasksPlaceholder != null) {
            if (pureTodoList.isEmpty()) {
                tvEmptyTasksPlaceholder.setVisibility(View.VISIBLE);
                rvIndividualTodoCompletion.setVisibility(View.GONE);
            } else {
                tvEmptyTasksPlaceholder.setVisibility(View.GONE);
                rvIndividualTodoCompletion.setVisibility(View.VISIBLE);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        calculateAndRenderPieChart(completedSubTasksCount, totalSubTasksCount);
    }

    private void calculateAndRenderPieChart(int completed, int total) {
        if (pieChartTodo == null) return;

        if (total == 0) {
            pieChartTodo.setData(null);
            pieChartTodo.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> assignedColors = new ArrayList<>();

        int colorGreen = Color.parseColor("#81B29A");
        int colorOrangeRed = Color.parseColor("#E07A5F");

        if (completed == total) {
            entries.add(new PieEntry(100f, "COMPLETED"));
            assignedColors.add(colorGreen);
        }
        else if (completed == 0) {
            entries.add(new PieEntry(100f, "INCOMPLETE"));
            assignedColors.add(colorOrangeRed);
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
                if (rounded == 0) return "";
                return rounded + "%";
            }
        });

        PieData data = new PieData(dataSet);
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

            String displayColorHex = "#FFFFFF";
            String rawTitleClean = task.title != null ? task.title : "";

            if (rawTitleClean.contains("||COLOR_SEP||")) {
                String[] colorSplit = rawTitleClean.split("\\|\\|COLOR_SEP\\|\\|");
                if (colorSplit.length > 0) {
                    rawTitleClean = colorSplit[0].trim();
                }
                if (colorSplit.length > 1) {
                    displayColorHex = colorSplit[1].trim();
                }
            }

            holder.tvTitle.setText(rawTitleClean);
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

            try {
                int parsedColor = Color.parseColor(displayColorHex);
                holder.cardContainer.setBackgroundTintList(ColorStateList.valueOf(parsedColor));

                boolean isDarkBg = displayColorHex.equals("#3D405B") ||
                        displayColorHex.equals("#A06CD5") ||
                        displayColorHex.equals("#81B29A") ||
                        displayColorHex.equals("#E63946");

                if (isDarkBg) {
                    holder.tvTitle.setTextColor(Color.WHITE);
                    holder.tvDate.setTextColor(Color.LTGRAY);
                    holder.tvPercent.setTextColor(Color.WHITE);

                    holder.pbMeter.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
                    holder.pbMeter.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DFFFFFF")));
                } else {
                    holder.tvTitle.setTextColor(Color.parseColor("#3D405B"));
                    holder.tvDate.setTextColor(Color.GRAY);
                    holder.tvPercent.setTextColor(Color.parseColor("#3D405B"));

                    holder.pbMeter.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#6F4E37")));
                    holder.pbMeter.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                }
            } catch (IllegalArgumentException e) {
                holder.cardContainer.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                holder.tvTitle.setTextColor(Color.parseColor("#3D405B"));
            }
        }

        @Override
        public int getItemCount() { return pureTodoList.size(); }

        class DashboardViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvPercent;
            ProgressBar pbMeter;
            View ivDeleteIcon;
            View cardContainer;

            public DashboardViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                tvPercent = itemView.findViewById(R.id.tvRowTaskPercentage);
                pbMeter = itemView.findViewById(R.id.pbRowTaskMeter);

                cardContainer = itemView;

                ivDeleteIcon = itemView.findViewById(R.id.ivTaskRowDelete);
                if (ivDeleteIcon != null) ivDeleteIcon.setVisibility(View.GONE);
            }
        }
    }
}