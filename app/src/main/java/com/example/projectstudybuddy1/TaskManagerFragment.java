package com.example.projectstudybuddy1;

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

    private MasterTaskAdapter masterAdapter;
    private SubChecklistAdapter subAdapter;

    private final List<TaskItem> masterTaskList = new ArrayList<>();
    private final List<SubTaskItem> localizedSubTasks = new ArrayList<>();
    private TaskItem runningActiveParentTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_manager, container, false);
        db = AppDatabase.getDatabase(requireContext());

        // Layout Bindings
        panelListView = v.findViewById(R.id.panelTaskListView);
        cardKeepEditor = v.findViewById(R.id.cardKeepEditor);
        etMainTitle = v.findViewById(R.id.etNoteMainTitle);
        tvTimestamp = v.findViewById(R.id.tvEditedTimestamp);

        // Core Task Card List Initialization
        RecyclerView rvMaster = v.findViewById(R.id.rvTodoTasks);
        if (rvMaster != null) {
            rvMaster.setLayoutManager(new LinearLayoutManager(getContext()));
            masterAdapter = new MasterTaskAdapter();
            rvMaster.setAdapter(masterAdapter);
        }

        // Keep-style Nested Checklist Checklist Initialization
        RecyclerView rvSub = v.findViewById(R.id.rvNestedChecklist);
        if (rvSub != null) {
            rvSub.setLayoutManager(new LinearLayoutManager(getContext()));
            subAdapter = new SubChecklistAdapter();
            rvSub.setAdapter(subAdapter);
        }

        // Trigger action click listeners
        View fabAdd = v.findViewById(R.id.fabAddTask);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(view -> launchKeepEditorWorkspace(null));
        }

        View addRowTrigger = v.findViewById(R.id.layoutAddListItemTrigger);
        if (addRowTrigger != null) {
            addRowTrigger.setOnClickListener(view -> {
                SubTaskItem newSub = new SubTaskItem();
                newSub.subTaskText = "";
                newSub.isChecked = false;
                if (runningActiveParentTask != null) {
                    newSub.parentTaskId = runningActiveParentTask.taskId;
                }
                localizedSubTasks.add(newSub);
                subAdapter.notifyItemInserted(localizedSubTasks.size() - 1);
            });
        }

        View btnClose = v.findViewById(R.id.btnKeepCloseAndSave);
        if (btnClose != null) {
            btnClose.setOnClickListener(view -> closeAndSaveKeepNoteWorkspace());
        }

        loadMasterDashboardData();
        return v;
    }

    private void loadMasterDashboardData() {
        int previousSize = masterTaskList.size();
        masterTaskList.clear();

        // BOUNDARY ISOLATION FILTER: Strip out entries belonging to the Journal workspace
        List<TaskItem> allItems = db.appDao().getAllTasks();
        for (TaskItem item : allItems) {
            if (item.title != null && !item.title.startsWith("Journal ") && !item.title.startsWith("Notetaking ")) {
                masterTaskList.add(item);
            }
        }

        if (masterAdapter != null) {
            masterAdapter.notifyItemRangeChanged(0, Math.max(previousSize, masterTaskList.size()));
        }
    }

    private void launchKeepEditorWorkspace(@Nullable TaskItem selectedTask) {
        int previousSubCount = localizedSubTasks.size();
        localizedSubTasks.clear();
        if (previousSubCount > 0 && subAdapter != null) {
            subAdapter.notifyItemRangeRemoved(0, previousSubCount);
        }

        if (selectedTask == null) {
            runningActiveParentTask = new TaskItem();
            runningActiveParentTask.title = "";
            runningActiveParentTask.isRoutine = false;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
            runningActiveParentTask.dateCreated = sdf.format(new Date());

            if (etMainTitle != null) {
                etMainTitle.setText("");
                etMainTitle.setHint("Title"); // FIXED: Displays crisp clean placeholder "Title" text hint
            }
            if (tvTimestamp != null) {
                tvTimestamp.setText(Html.fromHtml("<b>Status:</b> New To-Do Item", Html.FROM_HTML_MODE_LEGACY));
            }

            long generatedId = db.appDao().insertTask(runningActiveParentTask);
            runningActiveParentTask.taskId = (int) generatedId;
        } else {
            runningActiveParentTask = selectedTask;
            if (etMainTitle != null) {
                etMainTitle.setText(selectedTask.title);
            }
            if (tvTimestamp != null) {
                tvTimestamp.setText(Html.fromHtml("<b>Created:</b> " + selectedTask.dateCreated, Html.FROM_HTML_MODE_LEGACY));
            }
            localizedSubTasks.addAll(db.appDao().getSubTasksForParent(selectedTask.taskId));
        }

        if (!localizedSubTasks.isEmpty() && subAdapter != null) {
            subAdapter.notifyItemRangeInserted(0, localizedSubTasks.size());
        }

        if (panelListView != null) panelListView.setVisibility(View.GONE);
        if (cardKeepEditor != null) cardKeepEditor.setVisibility(View.VISIBLE);
    }

    private void closeAndSaveKeepNoteWorkspace() {
        if (runningActiveParentTask != null) {
            String updatedTitle = etMainTitle != null ? etMainTitle.getText().toString().trim() : "";
            if (updatedTitle.isEmpty()) {
                runningActiveParentTask.title = "Untitled To-Do";
            } else {
                runningActiveParentTask.title = updatedTitle;
            }

            db.appDao().updateTask(runningActiveParentTask);

            for (SubTaskItem sub : localizedSubTasks) {
                sub.parentTaskId = runningActiveParentTask.taskId;
                db.appDao().insertSubTask(sub);
            }
        }

        if (cardKeepEditor != null) cardKeepEditor.setVisibility(View.GONE);
        if (panelListView != null) panelListView.setVisibility(View.VISIBLE);
        loadMasterDashboardData();
    }

    // =======================================================
    // ADAPTER 1: TO-DO ROW MASTER DISPLAY LIST
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
                if (sub.isChecked) completedSubTasks++;
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

            // Wipe out older concurrent listeners from recycled item memory tracks
            if (holder.textWatcherInstance != null) {
                holder.etSubText.removeTextChangedListener(holder.textWatcherInstance);
            }

            holder.etSubText.setText(currentSub.subTaskText);
            holder.cbSubCheck.setOnCheckedChangeListener(null);
            holder.cbSubCheck.setChecked(currentSub.isChecked);

            holder.textWatcherInstance = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    currentSub.subTaskText = s.toString().trim();
                }
            };
            holder.etSubText.addTextChangedListener(holder.textWatcherInstance);

            holder.cbSubCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentSub.isChecked = isChecked;
                if (currentSub.subTaskId != 0) {
                    db.appDao().insertSubTask(currentSub);
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
                }
            });
        }

        @Override public int getItemCount() { return localizedSubTasks.size(); }

        class SubViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbSubCheck;
            EditText etSubText;
            ImageButton btnDeleteSubRow;
            TextWatcher textWatcherInstance;

            public SubViewHolder(@NonNull View itemView) {
                super(itemView);
                cbSubCheck = itemView.findViewById(R.id.cbSubCheckbox);
                etSubText = itemView.findViewById(R.id.etSubTaskValue);
                btnDeleteSubRow = itemView.findViewById(R.id.btnDeleteSubRow);
            }
        }
    }
}