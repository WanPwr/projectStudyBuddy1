package com.example.projectstudybuddy1;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskManagerFragment extends Fragment {
    private AppDatabase db;
    private View panelListView, cardKeepEditor;
    private EditText etMainTitle;
    private TextView tvTimestamp;
    private PieChart pieChart;

    private MasterTaskAdapter masterAdapter;
    private SubChecklistAdapter subAdapter;

    private final List<TaskItem> masterTaskList = new ArrayList<>();
    private final List<SubTaskItem> localizedSubTasks = new ArrayList<>();
    private TaskItem runningActiveParentTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        db = AppDatabase.getDatabase(requireContext());

        panelListView = v.findViewById(R.id.panelTaskListView);
        pieChart = v.findViewById(R.id.todoPieChart);
        cardKeepEditor = v.findViewById(R.id.cardKeepEditor);
        etMainTitle = v.findViewById(R.id.etNoteMainTitle);
        tvTimestamp = v.findViewById(R.id.tvEditedTimestamp);

        RecyclerView rvMaster = v.findViewById(R.id.rvMasterTasks);
        rvMaster.setLayoutManager(new LinearLayoutManager(getContext()));
        masterAdapter = new MasterTaskAdapter();
        rvMaster.setAdapter(masterAdapter);

        RecyclerView rvSub = v.findViewById(R.id.rvNestedChecklist);
        rvSub.setLayoutManager(new LinearLayoutManager(getContext()));
        subAdapter = new SubChecklistAdapter();
        rvSub.setAdapter(subAdapter);

        v.findViewById(R.id.fabCreateList).setOnClickListener(view -> launchKeepEditorWorkspace(null));

        v.findViewById(R.id.layoutAddListItemTrigger).setOnClickListener(view -> {
            SubTaskItem newSub = new SubTaskItem();
            newSub.subTaskText = "";
            newSub.isChecked = false;
            if (runningActiveParentTask != null) {
                newSub.parentTaskId = runningActiveParentTask.taskId;
            }
            localizedSubTasks.add(newSub);
            subAdapter.notifyItemInserted(localizedSubTasks.size() - 1);
        });

        v.findViewById(R.id.btnKeepCloseAndSave).setOnClickListener(view -> closeAndSaveKeepNoteWorkspace());

        loadMasterDashboardData();
        return v;
    }

    private void loadMasterDashboardData() {
        int previousSize = masterTaskList.size();
        masterTaskList.clear();
        masterTaskList.addAll(db.appDao().getAllTasks());

        masterAdapter.notifyItemRangeChanged(0, Math.max(previousSize, masterTaskList.size()));
        updateDashboardCompletionMeter();
    }

    private void updateDashboardCompletionMeter() {
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

    private void launchKeepEditorWorkspace(@Nullable TaskItem selectedTask) {
        int previousSubCount = localizedSubTasks.size();
        localizedSubTasks.clear();
        if (previousSubCount > 0) {
            subAdapter.notifyItemRangeRemoved(0, previousSubCount);
        }

        if (selectedTask == null) {
            runningActiveParentTask = new TaskItem();
            runningActiveParentTask.title = "";
            runningActiveParentTask.isRoutine = false;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
            runningActiveParentTask.dateCreated = sdf.format(new Date());

            etMainTitle.setText("");
            etMainTitle.setHint(runningActiveParentTask.dateCreated);

            tvTimestamp.setText(Html.fromHtml("<b>Status:</b> Edited Just Now", Html.FROM_HTML_MODE_LEGACY));

            long generatedId = db.appDao().insertTask(runningActiveParentTask);
            runningActiveParentTask.taskId = (int) generatedId;
        } else {
            runningActiveParentTask = selectedTask;
            etMainTitle.setText(selectedTask.title);
            tvTimestamp.setText(Html.fromHtml("<b>Created:</b> " + selectedTask.dateCreated, Html.FROM_HTML_MODE_LEGACY));
            localizedSubTasks.addAll(db.appDao().getSubTasksForParent(selectedTask.taskId));
        }

        if (!localizedSubTasks.isEmpty()) {
            subAdapter.notifyItemRangeInserted(0, localizedSubTasks.size());
        }

        panelListView.setVisibility(View.GONE);
        cardKeepEditor.setVisibility(View.VISIBLE);
    }

    private void closeAndSaveKeepNoteWorkspace() {
        if (runningActiveParentTask != null) {
            String updatedTitle = etMainTitle.getText().toString().trim();
            runningActiveParentTask.title = updatedTitle.isEmpty() ? etMainTitle.getHint().toString() : updatedTitle;

            db.appDao().updateTask(runningActiveParentTask);

            for (SubTaskItem sub : localizedSubTasks) {
                sub.parentTaskId = runningActiveParentTask.taskId;
                db.appDao().insertSubTask(sub);
            }
        }

        cardKeepEditor.setVisibility(View.GONE);
        panelListView.setVisibility(View.VISIBLE);
        loadMasterDashboardData();
    }

    // =======================================================
    // ADAPTER 1: JOURNAL-STYLE OUTLINED MASTER CARD LIST
    // =======================================================
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

            holder.itemView.setOnClickListener(view -> launchKeepEditorWorkspace(task));

            holder.ivDelete.setOnClickListener(view -> {
                int indexPosition = holder.getBindingAdapterPosition();
                if (indexPosition != RecyclerView.NO_POSITION) {
                    db.appDao().deleteTask(task);
                    masterTaskList.remove(indexPosition);
                    notifyItemRemoved(indexPosition);
                    updateDashboardCompletionMeter();
                }
            });
        }

        @Override public int getItemCount() { return masterTaskList.size(); }

        class MasterViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvItemPercent;
            ImageView ivDelete;
            android.widget.ProgressBar pbItemMeter;

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

    // =======================================================
    // ADAPTER 2: KEEP WORKSPACE SUB-CHECKLIST
    // =======================================================
    private class SubChecklistAdapter extends RecyclerView.Adapter<SubChecklistAdapter.SubViewHolder> {
        @NonNull
        @Override
        public SubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nested_check, parent, false);
            return new SubViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SubViewHolder holder, int pos) {
            SubTaskItem currentSub = localizedSubTasks.get(pos);
            holder.etSubText.setText(currentSub.subTaskText);

            holder.cbSubCheck.setOnCheckedChangeListener(null);
            holder.cbSubCheck.setChecked(currentSub.isChecked);

            holder.etSubText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    currentSub.subTaskText = s.toString().trim();
                }
            });

            holder.cbSubCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentSub.isChecked = isChecked;
                if (currentSub.subTaskId != 0) {
                    db.appDao().insertSubTask(currentSub);
                    updateDashboardCompletionMeter();
                }
            });

            holder.btnDeleteSubRow.setOnClickListener(v -> {
                int indexPosition = holder.getBindingAdapterPosition();
                if (indexPosition != RecyclerView.NO_POSITION) {
                    SubTaskItem removedItem = localizedSubTasks.get(indexPosition);
                    if (removedItem.subTaskId != 0) {
                        db.appDao().deleteSubTask(removedItem);
                    }
                    localizedSubTasks.remove(indexPosition);
                    notifyItemRemoved(indexPosition);
                    updateDashboardCompletionMeter();
                }
            });
        }

        @Override public int getItemCount() { return localizedSubTasks.size(); }

        class SubViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbSubCheck;
            EditText etSubText;
            ImageButton btnDeleteSubRow;

            public SubViewHolder(@NonNull View itemView) {
                super(itemView);
                cbSubCheck = itemView.findViewById(R.id.cbSubCheckbox);
                etSubText = itemView.findViewById(R.id.etSubTaskValue);
                btnDeleteSubRow = itemView.findViewById(R.id.btnDeleteSubRow);
            }
        }
    }
}