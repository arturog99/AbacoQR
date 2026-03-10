package com.example.abacoqr.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.abacoqr.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ACTIVAR LA SPLASH SCREEN API (Debe ir antes de super.onCreate)
        SplashScreen.installSplashScreen(this);
        
        super.onCreate(savedInstanceState);

        // Navegamos directamente al Login (la API ya maneja el tiempo de visualización)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, 500); // Un pequeño retraso para que la transición sea suave
    }
}
