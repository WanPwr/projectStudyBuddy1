package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

public class TimerEndedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // FORCE HOOK TO MATCH YOUR CLEAN XML FILE NAME:
        setContentView(R.layout.activity_timer_ended);

        SeekBar slideToDismiss = findViewById(R.id.slideToDismiss);

        if (slideToDismiss != null) {
            slideToDismiss.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress >= 90) {
                        finish(); // Closes the overlay screen and goes back to the timer
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getProgress() < 90) {
                        seekBar.setProgress(0); // Snap back to start if let go early
                    }
                }
            });
        }
    }
}