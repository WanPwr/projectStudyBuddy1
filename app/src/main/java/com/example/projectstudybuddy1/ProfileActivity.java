package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale; // FIXED: Added import statement for lowercase operations

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

        // FIXED: Extract the active account profile strings using dynamic multi-account lookup paths
        String currentSessionUsername = prefs.getString("username", "user");
        String lookupKey = currentSessionUsername.toLowerCase(Locale.US);
        String currentSessionPassword = prefs.getString("user_password_" + lookupKey, prefs.getString("password", ""));

        // Pre-populate input configurations with active account details
        etUsername.setText(currentSessionUsername);
        etPassword.setText(currentSessionPassword);

        findViewById(R.id.btnProfileBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnProfileSave).setOnClickListener(v -> {
            String updatedUser = etUsername.getText().toString().trim();
            String updatedPass = etPassword.getText().toString().trim();

            if (updatedUser.isEmpty() || updatedPass.isEmpty()) {
                Toast.makeText(this, "Fields cannot be blank!", Toast.LENGTH_SHORT).show();
                return;
            }

            String oldSessionUser = prefs.getString("username", "user");
            String oldLookupKey = oldSessionUser.toLowerCase(Locale.US);
            String newLookupKey = updatedUser.toLowerCase(Locale.US);

            int activeUserId = prefs.getInt("userId", 1);

            SharedPreferences.Editor editor = prefs.edit();

            // FIXED: If user edits their account name, handle the dynamic index swap seamlessly
            if (!oldSessionUser.equalsIgnoreCase(updatedUser)) {
                // Remove legacy pointers so data doesn't get orphaned
                editor.remove("user_id_" + oldLookupKey);
                editor.remove("user_password_" + oldLookupKey);
            }

            // FIXED: Apply updates onto the user-specific keys matching the LoginActivity scheme
            editor.putInt("user_id_" + newLookupKey, activeUserId);
            editor.putString("user_password_" + newLookupKey, updatedPass);

            // Sync the changes back into current session state variables
            editor.putString("username", updatedUser);
            editor.putString("password", updatedPass); // Kept as an optional layout safety backup
            editor.apply();

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