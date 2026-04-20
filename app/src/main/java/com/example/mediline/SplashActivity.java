package com.example.mediline;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        try {
            java.util.Map<String, String> config = new java.util.HashMap<>();
            config.put("cloud_name", "db2eubwvd");
            config.put("api_key", "517981978921165");
            config.put("api_secret", "PFNMwGtlMvQSJnY6NXqp_FbjyEI");
            com.cloudinary.android.MediaManager.init(this, config);
        } catch (Exception e) {
            // MediaManager is already initialized
        }

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Animate the progress indicator sliding from left to right
        View progressIndicator = findViewById(R.id.splash_progress_indicator);
        progressIndicator.post(() -> {
            View parent = (View) progressIndicator.getParent();
            float endX = parent.getWidth() - progressIndicator.getWidth();

            ObjectAnimator animator = ObjectAnimator.ofFloat(progressIndicator, "translationX", 0f, endX);
            animator.setDuration(SPLASH_DURATION_MS);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setRepeatCount(0);
            animator.start();
        });

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            com.example.mediline.util.SessionManager session = new com.example.mediline.util.SessionManager(this);
            Intent intent;
            if (session.isLoggedIn()) {
                String role = session.getUserRole();
                if ("DOCTOR".equals(role)) {
                    intent = new Intent(SplashActivity.this, DoctorDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, PatientHomeActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            } else {
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION_MS);
    }
}
