package com.example.projectstudybuddy1;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class TimerFragment extends AppCompatActivity {

    private TextView tvDisplay;
    private Button btnToggle;
    private Button btnReset;
    private CountDownTimer countDownTimer;
    private boolean isRunning = false;

    //Start at 0 so it displays 00:00:00 initially
    private long timeLeftInMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_timer);

        //Link UI elements
        tvDisplay = findViewById(R.id.tvCountdownDisplay);
        btnToggle = findViewById(R.id.btnTimerToggle);
        btnReset = findViewById(R.id.btnTimerReset);

        //Preset Time Buttons
        findViewById(R.id.btnPreset5).setOnClickListener(view -> setPresetTime(300000));
        findViewById(R.id.btnPreset10).setOnClickListener(view -> setPresetTime(600000));
        findViewById(R.id.btnPreset30).setOnClickListener(view -> setPresetTime(1800000));
        findViewById(R.id.btnPreset45).setOnClickListener(view -> setPresetTime(2700000));

        //Start/Pause Toggle
        btnToggle.setOnClickListener(view -> {
            if (isRunning) {
                stopTimer();
            } else {
                startTimer();
            }
        });

        //Reset Button Logic - Forces timer to 00:00:00
        btnReset.setOnClickListener(view -> {
            stopTimer();
            timeLeftInMillis = 0; // Set back to 0
            updateCountdownText();
        });

        updateCountdownText();
    }

    private void setPresetTime(long millis) {
        stopTimer();
        timeLeftInMillis = millis;
        updateCountdownText();
    }

    private void startTimer() {
        if (timeLeftInMillis == 0) return; // Prevent starting if time is 00:00:00

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
                timeLeftInMillis = 0;
                updateCountdownText();

                //Automatically navigate to Timer Ended Screen
                Intent intent = new Intent(TimerFragment.this, TimerEndedActivity.class);
                startActivity(intent);
            }
        }.start();

        isRunning = true;
        btnToggle.setText("⏸ Pause");
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
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
    protected void onDestroy() {
        super.onDestroy();
        //Prevent memory leaks if the activity is destroyed while running
        stopTimer();
    }
}