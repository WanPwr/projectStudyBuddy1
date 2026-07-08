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

        prefs = getSharedPreferences("StudyBuddyPrefs", Context.MODE_PRIVATE);

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

    private void handleLogin() {
        String username = etLoginUser.getText().toString().trim();
        String password = etLoginPass.getText().toString().trim();

        String savedUser = prefs.getString("username", "");
        String savedPass = prefs.getString("password", "");

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please complete all inputs!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Match case-insensitive validation criteria safely
        if (username.equalsIgnoreCase(savedUser) && password.equals(savedPass)) {
            // Keep the active logging flags persistent
            prefs.edit().putBoolean("isLoggedIn", true).apply();
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

        // AUTO USER ID GENERATOR: Generates a persistent int key to feed database foreign keys
        int generatedUserId = prefs.getInt("lastMaxUserId", 0) + 1;

        // Save session details cleanly to local storage file blocks
        prefs.edit()
                .putInt("userId", generatedUserId)
                .putInt("lastMaxUserId", generatedUserId) // Track global user increments
                .putString("username", username)
                .putString("password", password)
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