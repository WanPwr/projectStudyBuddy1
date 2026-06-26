package com.example.projectstudybuddy1;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class TimerFragment extends Fragment {

    private enum TimeUnit { HOUR, MINUTE, SECOND }
    private TimeUnit activeSelectedUnit = TimeUnit.MINUTE;

    private TextView tvUpperHint, tvLowerHint, tvActiveUnitLabel;
    private TextView tvEditHours, tvEditMinutes, tvEditSeconds;
    private Button btnToggleAction;

    private CountDownTimer countDownTimer;
    private long totalTimeInMillis = 300000;
    private long timeRemainingInMillis = 300000;
    private boolean isTimerRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);

        // Bind layout views
        tvUpperHint = v.findViewById(R.id.tvTimerUpperHint);
        tvLowerHint = v.findViewById(R.id.tvTimerLowerHint);
        tvActiveUnitLabel = v.findViewById(R.id.tvActiveUnitLabel);

        tvEditHours = v.findViewById(R.id.tvEditHours);
        tvEditMinutes = v.findViewById(R.id.tvEditMinutes);
        tvEditSeconds = v.findViewById(R.id.tvEditSeconds);

        // FIX: Points to btnTimerToggleAction matching the XML definition id layout parameters
        btnToggleAction = v.findViewById(R.id.btnTimerToggleAction);

        Button btnMinus = v.findViewById(R.id.btnTimerStepMinus);
        Button btnPlus = v.findViewById(R.id.btnTimerStepPlus);

        // Unit click listeners
        tvEditHours.setOnClickListener(view -> updateActiveEditUnit(TimeUnit.HOUR));
        tvEditMinutes.setOnClickListener(view -> updateActiveEditUnit(TimeUnit.MINUTE));
        tvEditSeconds.setOnClickListener(view -> updateActiveEditUnit(TimeUnit.SECOND));

        // Step buttons
        btnMinus.setOnClickListener(view -> performUnitStepTweak(false));
        btnPlus.setOnClickListener(view -> performUnitStepTweak(true));

        // Core start/pause action trigger
        btnToggleAction.setOnClickListener(view -> {
            if (isTimerRunning) {
                pauseCountDownEngine();
            } else {
                startCountDownEngine();
            }
        });

        // Quick presets
        v.findViewById(R.id.btnPreset5Min).setOnClickListener(view -> updateTimerDurationBudget(300000));
        v.findViewById(R.id.btnPreset10Min).setOnClickListener(view -> updateTimerDurationBudget(600000));
        v.findViewById(R.id.btnPreset30Min).setOnClickListener(view -> updateTimerDurationBudget(1800000));
        v.findViewById(R.id.btnPreset45Min).setOnClickListener(view -> updateTimerDurationBudget(2700000));

        updateTimerDurationBudget(totalTimeInMillis);
        updateActiveEditUnit(TimeUnit.MINUTE);

        return v;
    }

    @SuppressWarnings("AndroidLintHardcodedText")
    private void updateActiveEditUnit(TimeUnit unit) {
        if (isTimerRunning) return;
        activeSelectedUnit = unit;

        tvEditHours.setTextColor(Color.parseColor("#8E8E93"));
        tvEditMinutes.setTextColor(Color.parseColor("#8E8E93"));
        tvEditSeconds.setTextColor(Color.parseColor("#8E8E93"));

        switch (unit) {
            case HOUR:
                tvEditHours.setTextColor(Color.BLACK);
                tvActiveUnitLabel.setText("Editing: HOURS");
                break;
            case MINUTE:
                tvEditMinutes.setTextColor(Color.BLACK);
                tvActiveUnitLabel.setText("Editing: MINUTES");
                break;
            case SECOND:
                tvEditSeconds.setTextColor(Color.BLACK);
                tvActiveUnitLabel.setText("Editing: SECONDS");
                break;
        }
    }

    private void performUnitStepTweak(boolean isIncrement) {
        if (isTimerRunning) return;

        long factor = isIncrement ? 1L : -1L;
        long deltaMillis = 0;

        switch (activeSelectedUnit) {
            case HOUR:
                deltaMillis = factor * 3600000L;
                break;
            case MINUTE:
                deltaMillis = factor * 60000L;
                break;
            case SECOND:
                deltaMillis = factor * 1000L;
                break;
        }

        long tentativeTime = totalTimeInMillis + deltaMillis;
        if (tentativeTime < 0) tentativeTime = 0;
        updateTimerDurationBudget(tentativeTime);
    }

    private void updateTimerDurationBudget(long durationMillis) {
        totalTimeInMillis = durationMillis;
        timeRemainingInMillis = durationMillis;
        refreshTimerInterfaceStrings(durationMillis);
    }

    private void refreshTimerInterfaceStrings(long workingMillis) {
        int hours = (int) (workingMillis / 3600000);
        int minutes = (int) ((workingMillis % 3600000) / 60000);
        int seconds = (int) ((workingMillis % 60000) / 1000);

        tvEditHours.setText(String.format(Locale.getDefault(), "%02d", hours));
        tvEditMinutes.setText(String.format(Locale.getDefault(), "%02d", minutes));
        tvEditSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));

        long upperOffset = workingMillis + 61000;
        int uH = (int) (upperOffset / 3600000);
        int uM = (int) ((upperOffset % 3600000) / 60000);
        int uS = (int) ((upperOffset % 60000) / 1000);
        tvUpperHint.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", uH, uM, uS));

        long lowerOffset = workingMillis - 61000;
        if (lowerOffset < 0) lowerOffset = 86399000;
        int lH = (int) (lowerOffset / 3600000);
        int lM = (int) ((lowerOffset % 3600000) / 60000);
        int lS = (int) ((lowerOffset % 60000) / 1000);
        tvLowerHint.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", lH, lM, lS));
    }

    @SuppressWarnings("AndroidLintHardcodedText")
    private void startCountDownEngine() {
        if (timeRemainingInMillis <= 0) {
            Toast.makeText(getContext(), "Please set a duration first!", Toast.LENGTH_SHORT).show();
            return;
        }

        isTimerRunning = true;
        btnToggleAction.setText("⏸ Pause");
        btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E07A5F")));

        tvEditHours.setTextColor(Color.BLACK);
        tvEditMinutes.setTextColor(Color.BLACK);
        tvEditSeconds.setTextColor(Color.BLACK);

        countDownTimer = new CountDownTimer(timeRemainingInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingInMillis = millisUntilFinished;
                refreshTimerInterfaceStrings(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                updateTimerDurationBudget(totalTimeInMillis);
                btnToggleAction.setText("▶ Start");
                btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#007AFF")));
                updateActiveEditUnit(activeSelectedUnit);
                Toast.makeText(getContext(), "Time is up!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    @SuppressWarnings("AndroidLintHardcodedText")
    private void pauseCountDownEngine() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        btnToggleAction.setText("▶ Start");
        btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#007AFF")));
        updateActiveEditUnit(activeSelectedUnit);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}