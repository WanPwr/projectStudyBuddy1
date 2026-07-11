package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskManagerFragment extends Fragment {
    private AppDatabase db;
    private View panelListView, cardKeepEditor, layoutColorPickerRow;
    private EditText etMainTitle;
    private TextView tvTimestamp;

    private MasterTaskAdapter masterAdapter;
    private SubChecklistAdapter subAdapter;

    private final List<TaskItem> masterTaskList = new ArrayList<>();
    private final List<SubTaskItem> localizedSubTasks = new ArrayList<>();
    private TaskItem runningActiveParentTask;
    private SharedPreferences prefs;

    private String currentSelectedColorHex = "#FFFFFF";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_manager, container, false);
        db = AppDatabase.getDatabase(requireContext());
        prefs = requireContext().getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        panelListView = v.findViewById(R.id.panelTaskListView);
        cardKeepEditor = v.findViewById(R.id.cardKeepEditor);
        layoutColorPickerRow = v.findViewById(R.id.layoutTodoColorPicker);
        etMainTitle = v.findViewById(R.id.etNoteMainTitle);
        tvTimestamp = v.findViewById(R.id.tvEditedTimestamp);

        RecyclerView rvMaster = v.findViewById(R.id.rvTodoTasks);
        if (rvMaster != null) {
            rvMaster.setLayoutManager(new LinearLayoutManager(getContext()));
            masterAdapter = new MasterTaskAdapter();
            rvMaster.setAdapter(masterAdapter);
        }

        RecyclerView rvSub = v.findViewById(R.id.rvNestedChecklist);
        if (rvSub != null) {
            rvSub.setLayoutManager(new LinearLayoutManager(getContext()));
            subAdapter = new SubChecklistAdapter();
            rvSub.setAdapter(subAdapter);
        }

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

                if (subAdapter != null) {
                    subAdapter.setFocusOnNextBind(true);
                    subAdapter.notifyItemInserted(localizedSubTasks.size() - 1);
                }
            });
        }

        View btnClose = v.findViewById(R.id.btnKeepCloseAndSave);
        if (btnClose != null) {
            btnClose.setOnClickListener(view -> closeAndSaveKeepNoteWorkspace());
        }

        setupColorPickerClickListeners(v);

        return v;
    }

    private void setupColorPickerClickListeners(View root) {
        if (layoutColorPickerRow == null) return;
        View pickerYellow = layoutColorPickerRow.findViewById(R.id.todoDotYellow);
        View pickerBlue = layoutColorPickerRow.findViewById(R.id.todoDotBlue);
        View pickerRed = root.findViewById(R.id.todoDotRed);
        View pickerGreen = root.findViewById(R.id.todoDotGreen);
        View pickerPurple = root.findViewById(R.id.todoDotPurple);

        if (pickerYellow != null) pickerYellow.setOnClickListener(v -> applyEditorContainerTint("#FDF0CD"));
        if (pickerBlue != null) pickerBlue.setOnClickListener(v -> applyEditorContainerTint("#3D405B"));
        if (pickerRed != null) pickerRed.setOnClickListener(v -> applyEditorContainerTint("#E63946"));
        if (pickerGreen != null) pickerGreen.setOnClickListener(v -> applyEditorContainerTint("#81B29A"));
        if (pickerPurple != null) pickerPurple.setOnClickListener(v -> applyEditorContainerTint("#A06CD5"));
    }

    private void applyEditorContainerTint(String hexColor) {
        currentSelectedColorHex = hexColor;
        int parsedColor = Color.parseColor(hexColor);

        if (cardKeepEditor != null) {
            View layoutWrapper = cardKeepEditor.findViewById(R.id.layoutTodoDialogWrapper);
            if (layoutWrapper != null) {
                layoutWrapper.setBackgroundTintList(ColorStateList.valueOf(parsedColor));
            }
        }

        // CONTRAST ENGINE: Added #E63946 (red) to high-contrast white calculation group
        boolean isDarkBg = hexColor.equals("#3D405B") || hexColor.equals("#A06CD5") || hexColor.equals("#81B29A") || hexColor.equals("#E63946");
        int primaryText = isDarkBg ? Color.WHITE : Color.parseColor("#3D405B");
        int secondaryText = isDarkBg ? Color.parseColor("#E0E0E0") : Color.GRAY;

        if (etMainTitle != null) {
            etMainTitle.setBackgroundTintList(ColorStateList.valueOf(parsedColor));
            etMainTitle.setTextColor(primaryText);
            etMainTitle.setHintTextColor(secondaryText);
        }

        if (tvTimestamp != null) {
            tvTimestamp.setTextColor(secondaryText);
        }

        View addListItem = cardKeepEditor != null ? cardKeepEditor.findViewById(R.id.layoutAddListItemTrigger) : null;
        if (addListItem instanceof TextView) {
            TextView tvAdd = (TextView) addListItem;
            tvAdd.setTextColor(secondaryText);
            tvAdd.setCompoundDrawableTintList(ColorStateList.valueOf(secondaryText));
        }

        View btnClose = cardKeepEditor != null ? cardKeepEditor.findViewById(R.id.btnKeepCloseAndSave) : null;
        if (btnClose instanceof TextView) {
            ((TextView) btnClose).setTextColor(primaryText);
        }

        if (subAdapter != null) {
            subAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMasterDashboardData();
    }

    private void loadMasterDashboardData() {
        int previousSize = masterTaskList.size();
        masterTaskList.clear();
        int activeUserId = prefs.getInt("userId", 1);

        List<TaskItem> allItems = db.appDao().getAllTasks(activeUserId);
        for (TaskItem item : allItems) {
            if (item.title != null) {
                boolean isJournal = item.title.startsWith("JOURNAL_NOTE:");
                boolean isFlashcardDeck = item.title.startsWith("DECK_NOTE:");

                if (!isJournal && !isFlashcardDeck) {
                    masterTaskList.add(item);
                }
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

        int activeUserId = prefs.getInt("userId", 1);

        if (selectedTask == null) {
            runningActiveParentTask = new TaskItem();
            runningActiveParentTask.title = "";
            runningActiveParentTask.isRoutine = false;
            runningActiveParentTask.userId = activeUserId;

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
            runningActiveParentTask.dateCreated = sdf.format(new Date());

            if (etMainTitle != null) {
                etMainTitle.setText("");
                etMainTitle.setHint("Title");
            }
            if (tvTimestamp != null) {
                tvTimestamp.setText(Html.fromHtml("<b>Status:</b> New To-Do Item", Html.FROM_HTML_MODE_LEGACY));
            }

            currentSelectedColorHex = "#FFFFFF";
            applyEditorContainerTint(currentSelectedColorHex);

            long generatedId = db.appDao().insertTask(runningActiveParentTask);
            runningActiveParentTask.taskId = (int) generatedId;
        } else {
            runningActiveParentTask = selectedTask;

            String displayTitle = selectedTask.title != null ? selectedTask.title : "";
            currentSelectedColorHex = "#FFFFFF";

            if (displayTitle.contains("||COLOR_SEP||")) {
                String[] colorSplit = displayTitle.split("\\|\\|COLOR_SEP\\|\\|");
                if (colorSplit.length > 0) {
                    displayTitle = colorSplit[0].trim();
                }
                if (colorSplit.length > 1) {
                    currentSelectedColorHex = colorSplit[1].trim();
                }
            }

            if (etMainTitle != null) {
                etMainTitle.setText(displayTitle);
            }
            if (tvTimestamp != null) {
                tvTimestamp.setText(Html.fromHtml("<b>Created:</b> " + selectedTask.dateCreated, Html.FROM_HTML_MODE_LEGACY));
            }

            applyEditorContainerTint(currentSelectedColorHex);
            localizedSubTasks.addAll(db.appDao().getSubTasksForParent(selectedTask.taskId));
        }

        if (!localizedSubTasks.isEmpty() && subAdapter != null) {
            subAdapter.notifyItemRangeInserted(0, localizedSubTasks.size());
        }

        if (panelListView != null) panelListView.setVisibility(View.GONE);
        if (layoutColorPickerRow != null) layoutColorPickerRow.setVisibility(View.VISIBLE);
        if (cardKeepEditor != null) cardKeepEditor.setVisibility(View.VISIBLE);
    }

    private void closeAndSaveKeepNoteWorkspace() {
        if (runningActiveParentTask != null) {
            String updatedTitle = etMainTitle != null ? etMainTitle.getText().toString().trim() : "";
            if (updatedTitle.isEmpty()) {
                updatedTitle = "Untitled To-Do";
            }

            runningActiveParentTask.title = updatedTitle + " ||COLOR_SEP|| " + currentSelectedColorHex;

            if (runningActiveParentTask.userId == 0) {
                runningActiveParentTask.userId = prefs.getInt("userId", 1);
            }

            db.appDao().updateTask(runningActiveParentTask);

            for (SubTaskItem sub : localizedSubTasks) {
                sub.parentTaskId = runningActiveParentTask.taskId;
                db.appDao().insertSubTask(sub);
            }
        }

        if (cardKeepEditor != null) cardKeepEditor.setVisibility(View.GONE);
        if (layoutColorPickerRow != null) layoutColorPickerRow.setVisibility(View.GONE);
        if (panelListView != null) panelListView.setVisibility(View.VISIBLE);
        loadMasterDashboardData();
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

            try {
                int parsedColor = Color.parseColor(displayColorHex);
                holder.cardContainer.setBackgroundTintList(ColorStateList.valueOf(parsedColor));

                // CONTRAST ENGINE: Master task row toggles updated with Red (#E63946) rules logic
                if (displayColorHex.equals("#3D405B") || displayColorHex.equals("#A06CD5") || displayColorHex.equals("#81B29A") || displayColorHex.equals("#E63946")) {
                    holder.tvTitle.setTextColor(Color.WHITE);
                    holder.tvDate.setTextColor(Color.LTGRAY);
                    holder.tvItemPercent.setTextColor(Color.WHITE);
                } else {
                    holder.tvTitle.setTextColor(Color.parseColor("#3D405B"));
                    holder.tvDate.setTextColor(Color.GRAY);
                    holder.tvItemPercent.setTextColor(Color.parseColor("#3D405B"));
                }
            } catch (IllegalArgumentException e) {
                holder.cardContainer.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            }

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

            if (holder.deleteClickBox != null) {
                holder.deleteClickBox.setOnClickListener(view ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete To-Do Item")
                                .setMessage("Are you sure you want to delete this to-do item?")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    int indexPosition = holder.getBindingAdapterPosition();
                                    if (indexPosition != RecyclerView.NO_POSITION) {
                                        db.appDao().deleteTask(task);
                                        masterTaskList.remove(indexPosition);
                                        notifyItemRemoved(indexPosition);
                                    }
                                    Toast.makeText(getContext(), "To-Do item deleted", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show()
                );
            }
        }

        @Override public int getItemCount() { return masterTaskList.size(); }

        class MasterViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvItemPercent;
            View deleteClickBox;
            android.widget.ProgressBar pbItemMeter;
            View cardContainer;

            public MasterViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskRowTitle);
                tvDate = itemView.findViewById(R.id.tvTaskRowDate);
                pbItemMeter = itemView.findViewById(R.id.pbRowTaskMeter);
                tvItemPercent = itemView.findViewById(R.id.tvRowTaskPercentage);
                deleteClickBox = itemView.findViewById(R.id.flDeleteContainer);
                cardContainer = itemView;
            }
        }
    }

    private class SubChecklistAdapter extends RecyclerView.Adapter<SubChecklistAdapter.SubViewHolder> {
        private boolean requestFocusOnBind = false;

        @NonNull
        @Override
        public SubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nested_check, parent, false);
            return new SubViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SubViewHolder holder, int pos) {
            SubTaskItem currentSub = localizedSubTasks.get(pos);

            if (holder.textWatcherInstance != null) {
                holder.etSubText.removeTextChangedListener(holder.textWatcherInstance);
            }

            holder.etSubText.setText(currentSub.subTaskText);
            holder.cbSubCheck.setOnCheckedChangeListener(null);
            holder.cbSubCheck.setChecked(currentSub.isChecked);

            // CONTRAST ENGINE: Checklist nested item text contrast mapping updated to support Red (#E63946)
            boolean isDarkBg = currentSelectedColorHex.equals("#3D405B") || currentSelectedColorHex.equals("#A06CD5") || currentSelectedColorHex.equals("#81B29A") || currentSelectedColorHex.equals("#E63946");
            holder.etSubText.setTextColor(isDarkBg ? Color.WHITE : Color.parseColor("#3D405B"));
            holder.etSubText.setHintTextColor(isDarkBg ? Color.parseColor("#E0E0E0") : Color.GRAY);
            holder.btnDeleteSubRow.setColorFilter(isDarkBg ? Color.WHITE : Color.parseColor("#3D405B"));

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

            if (requestFocusOnBind && pos == localizedSubTasks.size() - 1) {
                requestFocusOnBind = false;
                holder.etSubText.post(() -> {
                    holder.etSubText.requestFocus();
                    InputMethodManager imm = (InputMethodManager)
                            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(holder.etSubText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        }

        @Override public int getItemCount() { return localizedSubTasks.size(); }

        public void setFocusOnNextBind(boolean focus) {
            this.requestFocusOnBind = focus;
        }

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