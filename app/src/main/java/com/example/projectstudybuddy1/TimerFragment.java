package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
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

    private TextView tvEditHours, tvEditMinutes, tvEditSeconds;
    private TextView tvUpperHint, tvLowerHint, tvActiveUnitLabel;
    private Button btnStepMinus, btnStepPlus, btnToggleAction, btnRestart;
    private Button btn5Min, btn10Min, btn30Min, btn45Min;

    private TimeUnit activeSelectedUnit = TimeUnit.MINUTE;
    private CountDownTimer countDownTimer = null;

    private long initialSetTimeMillis = 300000L;
    private long totalTimerMillis = 300000L;
    private boolean isTimerRunning = false;
    private boolean isTimerPaused = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        tvEditHours = view.findViewById(R.id.tvEditHours);
        tvEditMinutes = view.findViewById(R.id.tvEditMinutes);
        tvEditSeconds = view.findViewById(R.id.tvEditSeconds);
        tvUpperHint = view.findViewById(R.id.tvTimerUpperHint);
        tvLowerHint = view.findViewById(R.id.tvTimerLowerHint);
        tvActiveUnitLabel = view.findViewById(R.id.tvActiveUnitLabel);

        btnStepMinus = view.findViewById(R.id.btnTimerStepMinus);
        btnStepPlus = view.findViewById(R.id.btnTimerStepPlus);
        btnToggleAction = view.findViewById(R.id.btnTimerToggleAction);
        btnRestart = view.findViewById(R.id.btnTimerRestart);

        btn5Min = view.findViewById(R.id.btnPreset5Min);
        btn10Min = view.findViewById(R.id.btnPreset10Min);
        btn30Min = view.findViewById(R.id.btnPreset30Min);
        btn45Min = view.findViewById(R.id.btnPreset45Min);

        setupClickListeners();

        totalTimerMillis = initialSetTimeMillis;
        refreshTimerInterfaceStrings(totalTimerMillis);

        return view;
    }

    private void setupClickListeners() {
        tvEditHours.setOnClickListener(v -> changeActiveEditingUnit(TimeUnit.HOUR));
        tvEditMinutes.setOnClickListener(v -> changeActiveEditingUnit(TimeUnit.MINUTE));
        tvEditSeconds.setOnClickListener(v -> changeActiveEditingUnit(TimeUnit.SECOND));

        btnStepPlus.setOnClickListener(v -> adjustActiveUnit(1));
        btnStepMinus.setOnClickListener(v -> adjustActiveUnit(-1));

        btn5Min.setOnClickListener(v -> applyPresetTime(300000L));
        btn10Min.setOnClickListener(v -> applyPresetTime(600000L));
        btn30Min.setOnClickListener(v -> applyPresetTime(1800000L));
        btn45Min.setOnClickListener(v -> applyPresetTime(2700000L));

        btnRestart.setOnClickListener(v -> resetToLastConfiguredTime());

        btnToggleAction.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseChronometerTimer();
            } else {
                startChronometerTimer();
            }
        });
    }

    private void changeActiveEditingUnit(TimeUnit unit) {
        if (isTimerRunning) return;
        activeSelectedUnit = unit;
        tvActiveUnitLabel.setText("Editing: " + unit.name());
        refreshTimerInterfaceStrings(totalTimerMillis);
    }

    private void adjustActiveUnit(int amount) {
        if (isTimerRunning) return;

        long stepDelta = 60000L;
        if (activeSelectedUnit == TimeUnit.HOUR) stepDelta = 3600000L;
        if (activeSelectedUnit == TimeUnit.SECOND) stepDelta = 1000L;

        totalTimerMillis += (stepDelta * amount);
        if (totalTimerMillis < 0) totalTimerMillis = 0;

        initialSetTimeMillis = totalTimerMillis;
        resetControlStateViews();
    }

    private void applyPresetTime(long timeMillis) {
        if (isTimerRunning) return;
        totalTimerMillis = timeMillis;
        initialSetTimeMillis = timeMillis;
        resetControlStateViews();
    }

    private void resetControlStateViews() {
        isTimerPaused = false;
        btnRestart.setVisibility(View.GONE);
        btnToggleAction.setText("▶ Start");
        btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF81B29A));
        refreshTimerInterfaceStrings(totalTimerMillis);
    }

    private void refreshTimerInterfaceStrings(long workingMillis) {
        int hours = (int) (workingMillis / 3600000);
        int minutes = (int) ((workingMillis % 3600000) / 60000);
        int seconds = (int) ((workingMillis % 60000) / 1000);

        tvEditHours.setText(String.format(Locale.getDefault(), "%02d", hours));
        tvEditMinutes.setText(String.format(Locale.getDefault(), "%02d", minutes));
        tvEditSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));

        long unitOffset = 60000L;
        if (activeSelectedUnit == TimeUnit.HOUR) unitOffset = 3600000L;
        else if (activeSelectedUnit == TimeUnit.SECOND) unitOffset = 1000L;

        long upperOffset = workingMillis + unitOffset;
        int uH = (int) (upperOffset / 3600000);
        int uM = (int) ((upperOffset % 3600000) / 60000);
        int uS = (int) ((upperOffset % 60000) / 1000);
        tvUpperHint.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", uH, uM, uS));

        long lowerOffset = workingMillis - unitOffset;
        if (lowerOffset < 0) lowerOffset = 0;
        int lH = (int) (lowerOffset / 3600000);
        int lM = (int) ((lowerOffset % 3600000) / 60000);
        int lS = (int) ((lowerOffset % 60000) / 1000);
        tvLowerHint.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", lH, lM, lS));
    }

    private void startChronometerTimer() {
        if (totalTimerMillis <= 0) {
            Toast.makeText(getContext(), "Please set a valid time first!", Toast.LENGTH_SHORT).show();
            return;
        }

        isTimerRunning = true;
        isTimerPaused = false;

        btnRestart.setVisibility(View.GONE);
        btnToggleAction.setText("❚❚ Pause");
        btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE07A5F));

        tvUpperHint.setVisibility(View.INVISIBLE);
        tvLowerHint.setVisibility(View.INVISIBLE);

        countDownTimer = new CountDownTimer(totalTimerMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                totalTimerMillis = millisUntilFinished;
                refreshTimerInterfaceStrings(totalTimerMillis);
            }

            @Override
            public void onFinish() {
                // FIXED CRASH: Structural Fragment context safety check layer
                if (getActivity() == null || !isAdded()) return;

                // Enforce all execution processes back onto the UI Main Loop Thread
                getActivity().runOnUiThread(() -> {
                    isTimerRunning = false;
                    isTimerPaused = false;

                    totalTimerMillis = initialSetTimeMillis;
                    refreshTimerInterfaceStrings(totalTimerMillis);

                    btnRestart.setVisibility(View.GONE);
                    btnToggleAction.setText("▶ Start");
                    btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF81B29A));

                    tvUpperHint.setVisibility(View.VISIBLE);
                    tvLowerHint.setVisibility(View.VISIBLE);

                    // Safe Alarm Ringtone Player
                    try {
                        Context safeContext = requireContext();
                        android.net.Uri alertSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
                        if (alertSoundUri == null) {
                            alertSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
                        }
                        android.media.Ringtone ringtoneEngine = android.media.RingtoneManager.getRingtone(safeContext, alertSoundUri);
                        if (ringtoneEngine != null) {
                            ringtoneEngine.play();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Safe Intent Activity Switch
                    try {
                        Intent intent = new Intent(requireContext(), TimerEndedActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }.start();
    }

    private void pauseChronometerTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        isTimerPaused = true;

        btnRestart.setVisibility(View.VISIBLE);
        btnToggleAction.setText("▶ Resume");
        btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF81B29A));

        tvUpperHint.setVisibility(View.VISIBLE);
        tvLowerHint.setVisibility(View.VISIBLE);
    }

    private void resetToLastConfiguredTime() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        isTimerPaused = false;

        totalTimerMillis = initialSetTimeMillis;
        btnRestart.setVisibility(View.GONE);
        btnToggleAction.setText("▶ Start");
        btnToggleAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF81B29A));

        refreshTimerInterfaceStrings(totalTimerMillis);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}