package com.example.abacoqr.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.abacoqr.R;
import com.example.abacoqr.auth.AuthenticationManager;
import com.example.abacoqr.model.Usuario;
import com.example.abacoqr.ui.main.MainActivity;
import com.example.abacoqr.utils.BiometricAuthHelper;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity implements BiometricAuthHelper.BiometricCallback {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private CheckBox cbRememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        cbRememberMe = findViewById(R.id.cb_remember_me);

        // Verificar si hay login biométrico disponible y habilitado
        checkAndShowBiometricOption();

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean rememberMe = cbRememberMe.isChecked();

            // Usar AuthenticationManager centralizado
            AuthenticationManager.AuthResult result = AuthenticationManager.authenticate(this, username, password);
            
            if (result.isSuccess()) {
                Usuario user = result.getUser();
                SharedPreferences prefs = getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putBoolean("isLoggedIn", true);
                editor.putString("username", user.getUsername());
                editor.putString("displayName", user.getDisplayName());
                editor.putString("userRole", user.getRole());
                editor.putLong("loginTime", System.currentTimeMillis());
                editor.putLong("lastActivityTime", System.currentTimeMillis());
                
                // Guardar preferencia de "recordarme"
                editor.putBoolean("rememberMe", rememberMe);
                
                // Si "recordarme" está activo, guardar timestamp extendido
                if (rememberMe) {
                    editor.putLong("extendedLoginTime", System.currentTimeMillis());
                }

                // Si "recordarme" está activo, ofrecer habilitar biometría
                if (rememberMe && BiometricAuthHelper.isBiometricAvailable(this)) {
                    BiometricAuthHelper.enableBiometricLogin(this, username);
                }

                editor.apply();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("USERNAME", user.getUsername());
                intent.putExtra("DISPLAY_NAME", user.getDisplayName());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndShowBiometricOption() {
        // Si el login biométrico está habilitado y el dispositivo lo soporta
        if (BiometricAuthHelper.isBiometricLoginEnabled(this) && 
            BiometricAuthHelper.isBiometricAvailable(this)) {
            
            String savedUsername = BiometricAuthHelper.getBiometricUsername(this);
            if (!savedUsername.isEmpty()) {
                // Mostrar diálogo biométrico automáticamente
                showBiometricLoginDialog(savedUsername);
            }
        }
    }

    private void showBiometricLoginDialog(String username) {
        BiometricAuthHelper.showBiometricPrompt(
            this,
            "Iniciar sesión",
            "Usa tu huella o Face ID para iniciar como " + username,
            this
        );
    }

    // Implementación de BiometricCallback
    @Override
    public void onBiometricAuthenticationSuccess() {
        // Login exitoso con biometría
        String username = BiometricAuthHelper.getBiometricUsername(this);
        
        SharedPreferences prefs = getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.putLong("loginTime", System.currentTimeMillis());
        editor.putLong("lastActivityTime", System.currentTimeMillis());
        editor.putBoolean("rememberMe", true); // El login biométrico implica recordarme
        editor.putLong("extendedLoginTime", System.currentTimeMillis());

        editor.apply();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBiometricAuthenticationError(int errorCode, String errorMessage) {
        // Error en autenticación biométrica - mostrar login normal
        Toast.makeText(this, "Error biométrico: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBiometricAuthenticationFailed() {
        // Fallo en autenticación biométrica - mostrar login normal
        Toast.makeText(this, "Autenticación biométrica fallida", Toast.LENGTH_SHORT).show();
    }
}
