package com.example.projectstudybuddy1;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TimerEndedActivity extends AppCompatActivity {

    private Ringtone alarmRingtone;
    private MediaPlayer backupPlayer;
    private boolean isAlarmPlaying = false;
    private final Object stopLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_ended);

        // 1. Start the looping audio immediately when the screen opens
        startAlarmSound();

        SeekBar slideToDismiss = findViewById(R.id.slideToDismiss);

        if (slideToDismiss != null) {
            slideToDismiss.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress >= 90) {
                        // Freeze the slider to prevent duplicate multiple firings
                        seekBar.setEnabled(false);

                        // 2. Stop the audio cleanly before closing the screen
                        stopAlarmSound();
                        finish();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.isEnabled() && seekBar.getProgress() < 90) {
                        seekBar.setProgress(0); // Snap back to start if let go early
                    }
                }
            });
        }
    }

    private void startAlarmSound() {
        if (isAlarmPlaying) return;

        // FIXED: Pointing directly to your custom raw sound resource asset
        Uri customSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.my_custom_alarm);

        // FORCE FIXED: Using MediaPlayer directly as the primary engine for custom resources.
        // This ensures the 3-second clip loops continuously without gaps or clipping.
        try {
            backupPlayer = new MediaPlayer();
            backupPlayer.setDataSource(this, customSoundUri);
            backupPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());

            backupPlayer.setLooping(true); // FIXED: Forces the 3-second audio to loop infinitely!
            backupPlayer.prepare();
            backupPlayer.start();
            isAlarmPlaying = true;
            return; // Exit early since primary player successfully engaged
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // FALLBACK: System default audio stream engine if custom audio parsing fails
        try {
            if (customSoundUri != null) {
                alarmRingtone = RingtoneManager.getRingtone(this, customSoundUri);
                if (alarmRingtone != null) {
                    alarmRingtone.play();
                    isAlarmPlaying = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not engage audio driver channels.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAlarmSound() {
        synchronized (stopLock) {
            isAlarmPlaying = false;

            // Teardown Local Media Player Engine cleanly
            try {
                if (backupPlayer != null) {
                    if (backupPlayer.isPlaying()) {
                        backupPlayer.stop();
                    }
                    backupPlayer.release();
                    backupPlayer = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Teardown Ringtone Fallback Engine cleanly
            try {
                if (alarmRingtone != null) {
                    if (alarmRingtone.isPlaying()) {
                        alarmRingtone.stop();
                    }
                    alarmRingtone = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cut the sound loops off immediately if the application loses focus or window closes
        stopAlarmSound();
    }

    @Override
    protected void onDestroy() {
        stopAlarmSound();
        super.onDestroy();
    }
}