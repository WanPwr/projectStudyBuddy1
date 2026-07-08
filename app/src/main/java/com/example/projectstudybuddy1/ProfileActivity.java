package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

        etUsername = findViewById(R.id.etProfileUsername);
        etPassword = findViewById(R.id.etProfilePassword);

        // Pre-populate input configurations with active account details
        etUsername.setText(prefs.getString("username", "user"));
        etPassword.setText(prefs.getString("password", ""));

        findViewById(R.id.btnProfileBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnProfileSave).setOnClickListener(v -> {
            String updatedUser = etUsername.getText().toString().trim();
            String updatedPass = etPassword.getText().toString().trim();

            if (updatedUser.isEmpty() || updatedPass.isEmpty()) {
                Toast.makeText(this, "Fields cannot be blank!", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putString("username", updatedUser)
                    .putString("password", updatedPass)
                    .apply();

            Toast.makeText(this, "Profile Saved Changes Successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            // Wipe active session authorization flags completely
            prefs.edit().putBoolean("isLoggedIn", false).apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}