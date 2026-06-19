package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.JournalEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalFragment extends Fragment {
    private AppDatabase db;
    private JournalAdapter adapter;
    private List<JournalEntry> entries = new ArrayList<>();

    private View panelList, panelEdit;
    private EditText etTitle, etContent;
    private JournalEntry selectedEntry = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);
        db = AppDatabase.getDatabase(requireContext());

        panelList = view.findViewById(R.id.panelJournalListView);
        panelEdit = view.findViewById(R.id.panelJournalEditWorkspace);
        etTitle = view.findViewById(R.id.etJournalTitle);
        etContent = view.findViewById(R.id.etJournalContent);

        RecyclerView rv = view.findViewById(R.id.rvJournal);
        FloatingActionButton fab = view.findViewById(R.id.fabAddJournal);
        ImageButton btnBack = view.findViewById(R.id.btnWorkspaceBack);
        ImageButton btnSave = view.findViewById(R.id.btnWorkspaceSave);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new JournalAdapter();
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> openWorkspace(null));
        btnBack.setOnClickListener(v -> closeWorkspace());
        btnSave.setOnClickListener(v -> saveWorkspaceData());

        loadJournalData();
        return view;
    }

    private void loadJournalData() {
        entries.clear();
        entries.addAll(db.appDao().getAllJournalEntries());
        adapter.notifyDataSetChanged();
    }

    private void openWorkspace(@Nullable JournalEntry entry) {
        selectedEntry = entry;
        if (entry != null) {
            etTitle.setText(entry.title);
            etContent.setText(entry.content);
        } else {
            etTitle.setText("");
            etContent.setText("");
        }
        panelList.setVisibility(View.GONE);
        panelEdit.setVisibility(View.VISIBLE);
    }

    private void closeWorkspace() {
        panelEdit.setVisibility(View.GONE);
        panelList.setVisibility(View.VISIBLE);
        loadJournalData();
    }

    private void saveWorkspaceData() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        if(title.isEmpty()) return;

        if (selectedEntry == null) {
            JournalEntry entry = new JournalEntry();
            entry.title = title;
            entry.content = content;
            entry.timestamp = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(new Date());
            db.appDao().insertJournal(entry);
        } else {
            selectedEntry.title = title;
            selectedEntry.content = content;
            db.appDao().updateJournal(selectedEntry);
        }
        closeWorkspace();
    }

    private class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {
        @NonNull @Override
        public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal_card, parent, false);
            return new JournalViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull JournalViewHolder holder, int pos) {
            JournalEntry current = entries.get(pos);
            holder.tvTitle.setText(current.title);
            holder.tvDate.setText(current.timestamp);
            holder.itemView.setOnClickListener(v -> openWorkspace(current));
            holder.btnDel.setOnClickListener(v -> {
                db.appDao().deleteJournal(current);
                loadJournalData();
            });
        }
        @Override public int getItemCount() { return entries.size(); }

        class JournalViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvTitle; ImageButton btnDel;
            public JournalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDateBadge);
                tvTitle = itemView.findViewById(R.id.tvJournalRowTitle);
                btnDel = itemView.findViewById(R.id.btnJournalDelete);
            }
        }
    }
}