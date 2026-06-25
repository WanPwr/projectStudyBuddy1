package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

public class TimerEndedActivity extends AppCompatActivity {

    private SeekBar slideToDismiss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_timerended);

        slideToDismiss = findViewById(R.id.slideToDismiss);

        slideToDismiss.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //If the user slides it roughly to the end (e.g., 90% or more)
                if (progress >= 90) {
                    finish(); //This closes the activity and returns to the previous one
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Not needed for this
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //If they let go before reaching the end, snap it back to 0
                if (seekBar.getProgress() < 90) {
                    seekBar.setProgress(0);
                }
            }
        });
    }
}
