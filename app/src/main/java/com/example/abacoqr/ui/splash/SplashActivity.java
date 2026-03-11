package com.example.abacoqr.ui.splash;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.abacoqr.ui.auth.LoginActivity;
import com.example.abacoqr.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // La API de Splash mantiene el icono en pantalla automáticamente hasta que
        // la primera actividad dibuje su primer fotograma.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String savedUsername = prefs.getString("username", "");
        long loginTime = prefs.getLong("loginTime", 0);

        long currentTime = System.currentTimeMillis();
        long tiempoLimite = 24 * 60 * 60 * 1000; // 24 horas

        Intent intent;

        // Resolvemos la ruta inmediatamente sin retrasos
        if (isLoggedIn && !savedUsername.isEmpty() && (currentTime - loginTime < tiempoLimite)) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("USERNAME", savedUsername);
        } else {
            prefs.edit().clear().apply();
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}