package com.example.projectstudybuddy1;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation; // ADDED: Navigation import

import com.example.projectstudybuddy1.data.AppDatabase;
import com.example.projectstudybuddy1.data.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Locale;

public class RoutineAddFragment extends Fragment {

    private AppDatabase db;

    // UI Elements
    private EditText etRoutineTitle;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private Switch switchReminder;
    private RadioGroup rgReminderType;
    private RadioButton rbAlarm, rbPush;
    private LinearLayout layoutTimeSettings;
    private LinearLayout btnOpenTimePicker;
    private TextView tvTimeDisplay, tvAmPm;

    // Variables to hold the selected time
    private int selectedHour = 20;
    private int selectedMinute = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine_add, container, false);
        db = AppDatabase.getDatabase(requireContext());

        // 1. Initialize Views
        etRoutineTitle = view.findViewById(R.id.etRoutineTitle);

        cbMon = view.findViewById(R.id.cbMon);
        cbTue = view.findViewById(R.id.cbTue);
        cbWed = view.findViewById(R.id.cbWed);
        cbThu = view.findViewById(R.id.cbThu);
        cbFri = view.findViewById(R.id.cbFri);
        cbSat = view.findViewById(R.id.cbSat);
        cbSun = view.findViewById(R.id.cbSun);

        switchReminder = view.findViewById(R.id.switchReminder);
        rgReminderType = view.findViewById(R.id.rgReminderType);
        rbAlarm = view.findViewById(R.id.rbAlarm);
        rbPush = view.findViewById(R.id.rbPush);

        layoutTimeSettings = view.findViewById(R.id.layoutTimeSettings);
        btnOpenTimePicker = view.findViewById(R.id.btnOpenTimePicker);
        tvTimeDisplay = view.findViewById(R.id.tvTimeDisplay);
        tvAmPm = view.findViewById(R.id.tvAmPm);

        // 2. Handle Cancel (Clicking To-do tab)
        view.findViewById(R.id.rbTodo).setOnClickListener(v -> {
            // CHANGED: Use Jetpack Navigation to go back
            Navigation.findNavController(v).popBackStack();
        });

        // 3. Handle Reminder Toggle
        // Hide the radio buttons and time picker if the switch is off
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rgReminderType.setVisibility(View.VISIBLE);
                layoutTimeSettings.setVisibility(View.VISIBLE);
            } else {
                rgReminderType.setVisibility(View.GONE);
                layoutTimeSettings.setVisibility(View.GONE);
            }
        });

        // Ensure initial state matches the switch (off by default usually)
        rgReminderType.setVisibility(switchReminder.isChecked() ? View.VISIBLE : View.GONE);
        layoutTimeSettings.setVisibility(switchReminder.isChecked() ? View.VISIBLE : View.GONE);

        // 4. Handle Time Picker
        btnOpenTimePicker.setOnClickListener(v -> openTimePicker());

        // 5. Handle Save
        FloatingActionButton fabConfirm = view.findViewById(R.id.fabConfirmRoutine);
        // CHANGED: Pass the view 'v' into the saveRoutine method so Navigation can use it
        fabConfirm.setOnClickListener(v -> saveRoutine(v));

        return view;
    }

    private void openTimePicker() {
        // Use Android's native TimePickerDialog
        TimePickerDialog dialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            selectedHour = hourOfDay;
            selectedMinute = minute;
            updateTimeDisplay();
        }, selectedHour, selectedMinute, false); // false = 12 hour format (AM/PM)

        dialog.show();
    }

    private void updateTimeDisplay() {
        String amPm = selectedHour >= 12 ? "PM" : "AM";
        int hourIn12Format = selectedHour % 12;
        if (hourIn12Format == 0) hourIn12Format = 12;

        tvTimeDisplay.setText(String.format(Locale.getDefault(), "%02d : %02d", hourIn12Format, selectedMinute));
        tvAmPm.setText(amPm);
    }

    // CHANGED: Added 'View v' to the method parameters
    private void saveRoutine(View v) {
        String title = etRoutineTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a routine title", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gather selected days into a comma-separated string (e.g., "mo,we,fr")
        StringBuilder daysBuilder = new StringBuilder();
        if (cbMon.isChecked()) daysBuilder.append("mo,");
        if (cbTue.isChecked()) daysBuilder.append("tu,");
        if (cbWed.isChecked()) daysBuilder.append("we,");
        if (cbThu.isChecked()) daysBuilder.append("th,");
        if (cbFri.isChecked()) daysBuilder.append("fr,");
        if (cbSat.isChecked()) daysBuilder.append("sa,");
        if (cbSun.isChecked()) daysBuilder.append("su,");

        String selectedDays = daysBuilder.toString();
        if (selectedDays.endsWith(",")) {
            selectedDays = selectedDays.substring(0, selectedDays.length() - 1);
        }

        // Prepare the new database item
        TaskItem newRoutine = new TaskItem();
        newRoutine.title = title;
        newRoutine.isRoutine = true;
        newRoutine.isCompleted = false;

        newRoutine.daysToRepeat = selectedDays;
        newRoutine.hasReminder = switchReminder.isChecked();
        newRoutine.reminderType = rbAlarm.isChecked() ? "Alarm" : "Push";
        newRoutine.reminderHour = selectedHour;
        newRoutine.reminderMinute = selectedMinute;

        // Save to database and close screen
        db.appDao().insertTask(newRoutine);
        Toast.makeText(getContext(), "Routine Saved", Toast.LENGTH_SHORT).show();

        // CHANGED: Use Jetpack Navigation with the view 'v' to go back
        Navigation.findNavController(v).popBackStack();
    }
}