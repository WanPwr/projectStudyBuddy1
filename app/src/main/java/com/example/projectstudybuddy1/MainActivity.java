package com.example.projectstudybuddy1;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Bottom navigation view interaction routing
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_dashboard) {
                    // Dashboard is already active view state
                    return true;
                } else if (itemId == R.id.nav_timer) {
                    Toast.makeText(MainActivity.this, "Routing to Timer Module...", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_tasks) {
                    Toast.makeText(MainActivity.this, "Routing to Task & Routine Manager...", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_journal) {
                    Toast.makeText(MainActivity.this, "Routing to Personal Journal...", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_flashcards) {
                    Toast.makeText(MainActivity.this, "Routing to Active Recall Cards...", Toast.LENGTH_SHORT).show();
                    return true;
                }

                return false;
            }
        });
    }
}