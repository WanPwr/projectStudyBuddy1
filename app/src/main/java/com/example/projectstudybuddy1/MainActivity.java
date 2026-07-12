package com.example.projectstudybuddy1;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable screen configurations to fit modern device form factors cleanly
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // UI View Binding Targets
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Bind Window Inset Listeners to prevent system UI status lines from overlapping core features
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Jetpack Navigation Host Engine
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // Binds bottom layout menu click actions automatically to the active graph fragments
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }
}