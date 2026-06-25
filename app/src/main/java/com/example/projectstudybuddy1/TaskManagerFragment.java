package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import androidx.navigation.Navigation;

public class TaskManagerFragment extends Fragment {

    // 1. ADDED: Unique view types to differentiate between Todo and Routine UI
    private static final int VIEW_TYPE_TODO = 0;
    private static final int VIEW_TYPE_ROUTINE = 1;
    private static final int VIEW_TYPE_ACTIVE_INPUT = 2;

    private AppDatabase db;
    private TaskAdapter adapter;
    private List<TaskItem> dataList = new ArrayList<>();
    private RadioGroup rgTaskType;
    private boolean isRoutineView = false;
    private boolean isAddingNewTodo = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_manager, container, false);
        db = AppDatabase.getDatabase(requireContext());

        rgTaskType = view.findViewById(R.id.rgTaskType);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddTask);
        RecyclerView rv = view.findViewById(R.id.rvTasks);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter();
        rv.setAdapter(adapter);

        rgTaskType.setOnCheckedChangeListener((group, checkedId) -> {
            if (isAddingNewTodo) cancelNewTodoInsertion();

            if (checkedId == R.id.rbRoutine) {
                isRoutineView = true;
            } else if (checkedId == R.id.rbTodo) {
                isRoutineView = false;
            }
            loadData();
        });


        // Wired up the FAB to open the Add Routine screen via Navigation Component
        fabAdd.setOnClickListener(v -> {
            if (isRoutineView) {
                Navigation.findNavController(v).navigate(R.id.routineAddFragment);
            } else {
                insertNewTodoInputRow();
            }
        });

        loadData();
        return view;
    }

    private void loadData() {
        dataList.clear();
        if (isRoutineView) {
            dataList.addAll(db.appDao().getAllRoutines());
        } else {
            dataList.addAll(db.appDao().getAllTodos());
        }
        adapter.notifyDataSetChanged();
    }

    private void insertNewTodoInputRow() {
        if (!isAddingNewTodo) {
            isAddingNewTodo = true;
            dataList.add(0, null);
            adapter.notifyItemInserted(0);
            RecyclerView rv = getView().findViewById(R.id.rvTasks);
            if(rv != null) rv.scrollToPosition(0);
        }
    }

    private void cancelNewTodoInsertion() {
        if (isAddingNewTodo) {
            isAddingNewTodo = false;
            dataList.remove(0);
            adapter.notifyItemRemoved(0);
        }
    }

    // ==========================================
    // THE ADAPTER
    // ==========================================
    private class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            if (isAddingNewTodo && position == 0) return VIEW_TYPE_ACTIVE_INPUT;
            if (isRoutineView) return VIEW_TYPE_ROUTINE;
            return VIEW_TYPE_TODO;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
            if (viewType == VIEW_TYPE_ACTIVE_INPUT) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_todo_active_input, p, false);
                return new ActiveInputViewHolder(v);
            }
            // 3. ADDED: Inflate the correct layout with the % box for routines
            else if (viewType == VIEW_TYPE_ROUTINE) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_routine_element, p, false);
                return new RoutineItemViewHolder(v);
            }
            else {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_todo_element, p, false);
                return new TodoItemViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (getItemViewType(pos) == VIEW_TYPE_ACTIVE_INPUT) {
                ActiveInputViewHolder activeHolder = (ActiveInputViewHolder) holder;

                activeHolder.btnConfirm.setOnClickListener(v -> {
                    String title = activeHolder.etInput.getText().toString().trim();
                    if (!title.isEmpty()) {
                        TaskItem newItem = new TaskItem();
                        newItem.title = title;
                        newItem.isCompleted = false;
                        newItem.isRoutine = false;
                        db.appDao().insertTask(newItem);
                        activeHolder.etInput.setText("");
                        isAddingNewTodo = false;
                        loadData();
                    }
                });

                activeHolder.btnCancel.setOnClickListener(v -> cancelNewTodoInsertion());

            }
            // 4. ADDED: Logic for the Routine Items (Opening the Detail Screen)
            else if (getItemViewType(pos) == VIEW_TYPE_ROUTINE) {
                RoutineItemViewHolder routineHolder = (RoutineItemViewHolder) holder;
                TaskItem current = dataList.get(pos);
                routineHolder.tvTitle.setText(current.title);

                // --- NEW DYNAMIC PERCENTAGE LOGIC ---
                // 1. Get all subtasks for this specific routine TODO
                List<com.example.projectstudybuddy1.data.SubTaskItem> subTasks = db.appDao().getSubTasksForRoutine(current.id);

                // 2. Calculate the percentage
                if (subTasks.isEmpty()) {
                    routineHolder.tvPercentage.setText("0%");
                } else {
                    int completedCount = 0;
                    for (com.example.projectstudybuddy1.data.SubTaskItem sub : subTasks) {
                        if (sub.isCompleted) {
                            completedCount++;
                        }
                    }
                    // Do the math: (completed / total) * 100
                    int percent = (int) (((float) completedCount / subTasks.size()) * 100);
                    routineHolder.tvPercentage.setText(percent + "%");
                }
                // ------------------------------------

                // Navigate to the Routine Detail Screen when clicked
                routineHolder.tvTitle.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putInt("ROUTINE_ID", current.id);
                    args.putString("ROUTINE_TITLE", current.title);

                    Navigation.findNavController(v).navigate(R.id.routineDetailFragment, args);
                });

                routineHolder.btnDel.setOnClickListener(v -> {
                    db.appDao().deleteTask(current);
                    loadData();
                });
            }
            // Logic for standard To-do Items
            else {
                TodoItemViewHolder itemHolder = (TodoItemViewHolder) holder;
                TaskItem current = dataList.get(pos);
                itemHolder.tvTitle.setText(current.title);

                itemHolder.cb.setOnCheckedChangeListener(null);
                itemHolder.cb.setChecked(current.isCompleted);

                itemHolder.cb.setOnCheckedChangeListener((b, isChecked) -> {
                    current.isCompleted = isChecked;
                    db.appDao().updateTask(current);
                });

                itemHolder.btnDel.setOnClickListener(v -> {
                    db.appDao().deleteTask(current);
                    loadData();
                });
            }
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        // --- ViewHolders ---

        class TodoItemViewHolder extends RecyclerView.ViewHolder {
            CheckBox cb; TextView tvTitle; TextView btnDel;
            public TodoItemViewHolder(@NonNull View itemView) {
                super(itemView);
                cb = itemView.findViewById(R.id.cbTodoStatus);
                tvTitle = itemView.findViewById(R.id.tvTodoTitle);
                btnDel = itemView.findViewById(R.id.btnDeleteTodo);
            }
        }

        // 5. ADDED: ViewHolder specifically for routines
        class RoutineItemViewHolder extends RecyclerView.ViewHolder {
            TextView tvPercentage; TextView tvTitle; TextView btnDel;
            public RoutineItemViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPercentage = itemView.findViewById(R.id.tvRoutinePercentage);
                tvTitle = itemView.findViewById(R.id.tvRoutineTitle);
                btnDel = itemView.findViewById(R.id.btnDeleteRoutine);
            }
        }

        class ActiveInputViewHolder extends RecyclerView.ViewHolder {
            EditText etInput; ImageButton btnConfirm; ImageButton btnCancel;
            public ActiveInputViewHolder(@NonNull View itemView) {
                super(itemView);
                etInput = itemView.findViewById(R.id.etNewTodoInput);
                btnConfirm = itemView.findViewById(R.id.btnConfirmNewTodo);
                btnCancel = itemView.findViewById(R.id.btnCancelNewTodo);
            }
        }
    }
}