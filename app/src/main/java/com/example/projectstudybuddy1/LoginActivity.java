package com.example.projectstudybuddy1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private View panelLogin, panelSignup;
    private EditText etLoginUser, etLoginPass;
    private EditText etSignUser, etSignPass, etSignConfirm;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getGetSharedPreferencesWrapper();

        // Auto-bypass registration screen gateway if a session token is active
        if (prefs.getBoolean("isLoggedIn", false)) {
            navigateToMainDashboard();
            return;
        }

        // View Lookup Initializations
        panelLogin = findViewById(R.id.panelLogin);
        panelSignup = findViewById(R.id.panelSignup);

        etLoginUser = findViewById(R.id.etLoginUsername);
        etLoginPass = findViewById(R.id.etLoginPassword);
        etSignUser = findViewById(R.id.etSignupUsername);
        etSignPass = findViewById(R.id.etSignupPassword);
        etSignConfirm = findViewById(R.id.etSignupConfirmPassword);

        // Dual container layout panel visibility toggles
        findViewById(R.id.btnSwitchToSignup).setOnClickListener(v -> {
            panelLogin.setVisibility(View.GONE);
            panelSignup.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btnSwitchToLogin).setOnClickListener(v -> {
            panelSignup.setVisibility(View.GONE);
            panelLogin.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btnLoginSubmit).setOnClickListener(v -> handleLogin());
        findViewById(R.id.btnSignupSubmit).setOnClickListener(v -> handleSignup());
    }

    private SharedPreferences getGetSharedPreferencesWrapper() {
        return getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);
    }

    private void handleLogin() {
        String username = etLoginUser.getText().toString().trim();
        String password = etLoginPass.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please complete all inputs!", Toast.LENGTH_SHORT).show();
            return;
        }

        // FIXED: Retrieve account specific info using the unique username as part of the key lookup
        String lowerUsername = username.toLowerCase(Locale.US);
        String savedPass = prefs.getString("user_password_" + lowerUsername, null);
        int savedUserId = prefs.getInt("user_id_" + lowerUsername, -1);

        if (savedPass != null && savedPass.equals(password)) {
            // Log in the user and save their specific active session properties
            prefs.edit()
                    .putBoolean("isLoggedIn", true)
                    .putInt("userId", savedUserId)
                    .putString("username", username)
                    .apply();
            navigateToMainDashboard();
        } else {
            Toast.makeText(this, "Invalid Username or Password!", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSignup() {
        String username = etSignUser.getText().toString().trim();
        String password = etSignPass.getText().toString().trim();
        String confirm = etSignConfirm.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        String lowerUsername = username.toLowerCase(Locale.US);

        // FIXED: Check if the username is already taken by looking for a pre-existing id key flag
        if (prefs.contains("user_id_" + lowerUsername)) {
            Toast.makeText(this, "Username already exists!", Toast.LENGTH_SHORT).show();
            return;
        }

        int generatedUserId = prefs.getInt("lastMaxUserId", 0) + 1;

        // FIXED: Save the account fields using user-specific dynamic keys so they don't overwrite each other
        prefs.edit()
                .putInt("user_id_" + lowerUsername, generatedUserId)
                .putString("user_password_" + lowerUsername, password)
                .putInt("lastMaxUserId", generatedUserId)
                // Set these to set the current active session state variables
                .putInt("userId", generatedUserId)
                .putString("username", username)
                .putBoolean("isLoggedIn", true)
                .apply();

        Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
        navigateToMainDashboard();
    }

    private void navigateToMainDashboard() {
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}