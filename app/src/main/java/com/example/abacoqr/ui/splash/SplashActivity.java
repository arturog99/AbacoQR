package com.example.abacoqr.ui.splash;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.abacoqr.ui.auth.LoginActivity;
import com.example.abacoqr.ui.main.MainActivity;
import com.example.abacoqr.utils.BiometricAuthHelper;

public class SplashActivity extends AppCompatActivity implements BiometricAuthHelper.BiometricCallback {

    // Constantes para timeout de sesión
    private static final long SESSION_TIMEOUT = 1 * 60 * 60 * 1000; // 1 hora normal
    private static final long EXTENDED_SESSION_TIMEOUT = 24 * 60 * 60 * 1000; // 24 horas extendido
    private static final long INACTIVITY_TIMEOUT = 7L * 24 * 60 * 60 * 1000; // 7 días de inactividad

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
        boolean rememberMe = prefs.getBoolean("rememberMe", false);
        long extendedLoginTime = prefs.getLong("extendedLoginTime", 0);
        long lastActivityTime = prefs.getLong("lastActivityTime", loginTime);

        long currentTime = System.currentTimeMillis();
        long tiempoLimite = rememberMe ? EXTENDED_SESSION_TIMEOUT : SESSION_TIMEOUT;
        long tiempoBase = rememberMe ? extendedLoginTime : loginTime;
        
        // Verificar inactividad (7 días sin usar la app)
        boolean isInactive = (currentTime - lastActivityTime) > INACTIVITY_TIMEOUT;

        Intent intent;

        // Resolvemos la ruta inmediatamente sin retrasos
        if (isLoggedIn && !savedUsername.isEmpty() && (currentTime - tiempoBase < tiempoLimite) && !isInactive) {
            // Actualizar timestamp de última actividad
            prefs.edit().putLong("lastActivityTime", currentTime).apply();
            
            // Si la sesión es válida, ir directamente a MainActivity
            intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("USERNAME", savedUsername);
        } else if (isLoggedIn && !savedUsername.isEmpty() && isInactive) {
            // Si hay inactividad de 7 días, limpiar y ir a login normal
            prefs.edit().clear().apply();
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        } else if (isLoggedIn && !savedUsername.isEmpty() && (currentTime - tiempoBase >= tiempoLimite)) {
            // La sesión ha caducado - verificar si es sesión de 1h y ofrecer huella
            
            if (!rememberMe && BiometricAuthHelper.isBiometricAvailable(this) && BiometricAuthHelper.isBiometricLoginEnabled(this)) {
                // Sesión de 1h caducada + biometría disponible = ofrecer login con huella
                mostrarLoginBiometrico(savedUsername);
                return; // No continuar con el intent normal
            } else {
                // Sesión extendida o sin biometría = ir a login normal
                prefs.edit().clear().apply();
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
        } else {
            // No hay sesión activa = ir a login normal
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }

    /**
     * Muestra el diálogo de login biométrico para sesión caducada de 1h
     */
    private void mostrarLoginBiometrico(String username) {
        BiometricAuthHelper.showBiometricPrompt(
            this,
            "Iniciar Sesión",
            "Tu sesión ha expirado. ¿Deseas iniciar sesión con huella como " + username + "?",
            this
        );
    }

    // Implementación de BiometricCallback
    @Override
    public void onBiometricAuthenticationSuccess() {
        // Login exitoso con biometría - restaurar sesión
        String username = BiometricAuthHelper.getBiometricUsername(this);
        
        // Restaurar sesión como si fuera login normal
        SharedPreferences prefs = getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.putLong("loginTime", System.currentTimeMillis());
        editor.putLong("lastActivityTime", System.currentTimeMillis());
        editor.putBoolean("rememberMe", false); // Mantener como sesión de 1h
        editor.putBoolean("extendedLoginTime", false);
        editor.apply();

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBiometricAuthenticationError(int errorCode, String errorMessage) {
        // Error en autenticación biométrica - ir a login normal
        SharedPreferences prefs = getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBiometricAuthenticationFailed() {
        // Fallo en autenticación biométrica - ir a login normal
        SharedPreferences prefs = getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
