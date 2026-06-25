package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.SubTaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RoutineDetailFragment extends Fragment {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_ACTIVE_INPUT = 1;

    private AppDatabase db;
    private SubTaskAdapter adapter;
    private List<SubTaskItem> dataList = new ArrayList<>();

    // Variables to track which folder we are inside
    private int parentRoutineId = -1;
    private String parentRoutineTitle = "ROUTINE";

    private boolean isAddingNewSubTask = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine_detail, container, false);
        db = AppDatabase.getDatabase(requireContext());

        // 1. Get the Routine ID and Title passed from the main screen
        if (getArguments() != null) {
            parentRoutineId = getArguments().getInt("ROUTINE_ID", -1);
            parentRoutineTitle = getArguments().getString("ROUTINE_TITLE", "ROUTINE DETAILS");
        }

        // Set the parent title on the screen
        TextView tvParentTitle = view.findViewById(R.id.tvParentRoutineTitle);
        tvParentTitle.setText(parentRoutineTitle.toUpperCase());

        // 2. Setup RecyclerView
        RecyclerView rv = view.findViewById(R.id.rvSubTasks);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SubTaskAdapter();
        rv.setAdapter(adapter);

        // 3. Handle Navigation / Cancel
        view.findViewById(R.id.rbTodo).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        // 4. Handle Adding a New Sub-Task (The (+) Button)
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddSubTask);
        fabAdd.setOnClickListener(v -> insertNewSubTaskInputRow());

        // 5. Handle Completing the Routine (The Checkmark Button)
        FloatingActionButton fabComplete = view.findViewById(R.id.fabCompleteRoutine);
        fabComplete.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Routine Completed for today!", Toast.LENGTH_SHORT).show();
            // Go back to the main menu
            getParentFragmentManager().popBackStack();
        });

        loadData();
        return view;
    }

    private void loadData() {
        dataList.clear();
        // Only load sub-tasks that belong to THIS specific parent routine
        if (parentRoutineId != -1) {
            dataList.addAll(db.appDao().getSubTasksForRoutine(parentRoutineId));
        }
        adapter.notifyDataSetChanged();
    }

    private void insertNewSubTaskInputRow() {
        if (!isAddingNewSubTask) {
            isAddingNewSubTask = true;
            dataList.add(0, null); // Add placeholder at the top
            adapter.notifyItemInserted(0);

            RecyclerView rv = getView().findViewById(R.id.rvSubTasks);
            if(rv != null) rv.scrollToPosition(0);
        }
    }

    private void cancelNewSubTaskInsertion() {
        if (isAddingNewSubTask) {
            isAddingNewSubTask = false;
            dataList.remove(0);
            adapter.notifyItemRemoved(0);
        }
    }

    // ==========================================
    // THE ADAPTER
    // ==========================================
    private class SubTaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            if (isAddingNewSubTask && position == 0) return VIEW_TYPE_ACTIVE_INPUT;
            return VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
            if (viewType == VIEW_TYPE_ACTIVE_INPUT) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_subtask_active_input, p, false);
                return new ActiveInputViewHolder(v);
            } else {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_subtask_element, p, false);
                return new SubTaskViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (getItemViewType(pos) == VIEW_TYPE_ACTIVE_INPUT) {
                ActiveInputViewHolder activeHolder = (ActiveInputViewHolder) holder;

                activeHolder.btnConfirm.setOnClickListener(v -> {
                    String title = activeHolder.etInput.getText().toString().trim();
                    if (!title.isEmpty() && parentRoutineId != -1) {
                        SubTaskItem newItem = new SubTaskItem();
                        newItem.title = title;
                        newItem.isCompleted = false;
                        newItem.parentRoutineId = parentRoutineId; // Link it to the parent!

                        db.appDao().insertSubTask(newItem);
                        activeHolder.etInput.setText("");
                        isAddingNewSubTask = false;
                        loadData();
                    }
                });

                activeHolder.btnCancel.setOnClickListener(v -> cancelNewSubTaskInsertion());

            } else {
                SubTaskViewHolder itemHolder = (SubTaskViewHolder) holder;
                SubTaskItem current = dataList.get(pos);
                itemHolder.tvTitle.setText(current.title);

                itemHolder.cb.setOnCheckedChangeListener(null);
                itemHolder.cb.setChecked(current.isCompleted);

                itemHolder.cb.setOnCheckedChangeListener((b, isChecked) -> {
                    current.isCompleted = isChecked;
                    db.appDao().updateSubTask(current);
                });

                itemHolder.btnDel.setOnClickListener(v -> {
                    db.appDao().deleteSubTask(current);
                    loadData();
                });
            }
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        class SubTaskViewHolder extends RecyclerView.ViewHolder {
            CheckBox cb;
            TextView tvTitle;
            TextView btnDel;

            public SubTaskViewHolder(@NonNull View itemView) {
                super(itemView);
                cb = itemView.findViewById(R.id.cbSubTaskStatus);
                tvTitle = itemView.findViewById(R.id.tvSubTaskTitle);
                btnDel = itemView.findViewById(R.id.btnDeleteSubTask);
            }
        }

        class ActiveInputViewHolder extends RecyclerView.ViewHolder {
            EditText etInput;
            ImageButton btnConfirm;
            ImageButton btnCancel;

            public ActiveInputViewHolder(@NonNull View itemView) {
                super(itemView);
                etInput = itemView.findViewById(R.id.etNewSubTaskInput);
                btnConfirm = itemView.findViewById(R.id.btnConfirmNewSubTask);
                btnCancel = itemView.findViewById(R.id.btnCancelNewSubTask);
            }
        }
    }
}