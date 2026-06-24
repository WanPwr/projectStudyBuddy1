package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class TimerFragment extends Fragment {
    private TextView tvDisplay;
    private Button btnToggle;
    private CountDownTimer countDownTimer;
    private boolean isRunning = false;

    private long defaultPresetTime = 300000; // Track the baseline choice (5 Mins)
    private long timeLeftInMillis = 300000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);
        tvDisplay = v.findViewById(R.id.tvCountdownDisplay);
        btnToggle = v.findViewById(R.id.btnTimerToggle);
        Button btnReset = v.findViewById(R.id.btnTimerReset);

        v.findViewById(R.id.btnPreset5).setOnClickListener(view -> setPresetTime(300000));
        v.findViewById(R.id.btnPreset10).setOnClickListener(view -> setPresetTime(600000));
        v.findViewById(R.id.btnPreset30).setOnClickListener(view -> setPresetTime(1800000));
        v.findViewById(R.id.btnPreset45).setOnClickListener(view -> setPresetTime(2700000));

        btnToggle.setOnClickListener(view -> {
            if (isRunning) stopTimer(); else startTimer();
        });

        // Setup the restart execution handler
        btnReset.setOnClickListener(view -> {
            stopTimer();
            timeLeftInMillis = defaultPresetTime; // Roll back time sequence to initial selection
            updateCountdownText();
        });

        updateCountdownText();
        return v;
    }

    private void setPresetTime(long millis) {
        stopTimer();
        defaultPresetTime = millis;
        timeLeftInMillis = millis;
        updateCountdownText();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
            }

            @Override
            public void onFinish() {
                isRunning = false;
                btnToggle.setText("▶ Start");
            }
        }.start();
        isRunning = true;
        btnToggle.setText("⏸ Pause"); // Toggles visual icon to hint pause state
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;
        btnToggle.setText("▶ Start");
    }

    private void updateCountdownText() {
        int hours = (int) (timeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((timeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
    }
}