package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.RadioButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import java.util.ArrayList;
import java.util.List;

public class TaskManagerFragment extends Fragment {
    private AppDatabase db;
    private TaskAdapter adapter;
    private List<TaskItem> dataList = new ArrayList<>();
    private EditText etInput;
    private RadioButton rbRoutine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_manager, container, false);
        db = AppDatabase.getDatabase(requireContext());

        etInput = view.findViewById(R.id.etTaskInput);
        rbRoutine = view.findViewById(R.id.rbRoutine);
        ImageButton btnAdd = view.findViewById(R.id.btnAddTask);
        RecyclerView rv = view.findViewById(R.id.rvTasks);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter();
        rv.setAdapter(adapter);

        view.findViewById(R.id.rbTodo).setOnClickListener(v -> loadData());
        rbRoutine.setOnClickListener(v -> loadData());

        btnAdd.setOnClickListener(v -> {
            String title = etInput.getText().toString().trim();
            if(!title.isEmpty()){
                TaskItem item = new TaskItem();
                item.title = title;
                item.isCompleted = false;
                item.isRoutine = rbRoutine.isChecked();
                db.appDao().insertTask(item);
                etInput.setText("");
                loadData();
            }
        });

        loadData();
        return view;
    }

    private void loadData() {
        dataList.clear();
        if (rbRoutine.isChecked()) {
            dataList.addAll(db.appDao().getAllRoutines());
        } else {
            dataList.addAll(db.appDao().getAllTodos());
        }
        adapter.notifyDataSetChanged();
    }

    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
        @NonNull @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_task_element, p, false);
            return new TaskViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int pos) {
            TaskItem current = dataList.get(pos);
            holder.tvTitle.setText(current.title);
            holder.cb.setChecked(current.isCompleted);
            holder.cb.setOnCheckedChangeListener((b, isChecked) -> {
                current.isCompleted = isChecked;
                db.appDao().updateTask(current);
            });
            holder.btnDel.setOnClickListener(v -> {
                db.appDao().deleteTask(current);
                loadData();
            });
        }
        @Override public int getItemCount() { return dataList.size(); }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            CheckBox cb; TextView tvTitle; ImageButton btnDel;
            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                cb = itemView.findViewById(R.id.cbTaskStatus);
                tvTitle = itemView.findViewById(R.id.tvTaskTitle);
                btnDel = itemView.findViewById(R.id.btnDeleteTask);
            }
        }
    }
}