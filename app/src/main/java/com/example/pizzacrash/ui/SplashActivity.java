package com.example.pizzacrash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.data.SaveRepository;

/**
 * Branded splash/loading screen. Phase 1: shows the splash theme briefly,
 * then navigates to Home. Real asset pre-loading will be added in later phases.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> next = new SaveRepository(this).isOnboardingCompleted()
                    ? HomeActivity.class
                    : OnboardingActivity.class;
            startActivity(new Intent(this, next));
            finish();
        }, 900L);
    }
}
