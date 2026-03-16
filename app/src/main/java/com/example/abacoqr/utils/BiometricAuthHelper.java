package com.example.abacoqr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/**
 * Utilidad para manejar autenticación biométrica (huella, face ID)
 */
public class BiometricAuthHelper {

    public interface BiometricCallback {
        void onBiometricAuthenticationSuccess();
        void onBiometricAuthenticationError(int errorCode, String errorMessage);
        void onBiometricAuthenticationFailed();
    }

    /**
     * Verifica si el dispositivo soporta autenticación biométrica
     */
    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
            default:
                return false;
        }
    }

    /**
     * Muestra el diálogo de autenticación biométrica
     */
    public static void showBiometricPrompt(FragmentActivity activity, String title, String subtitle, BiometricCallback callback) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onBiometricAuthenticationSuccess();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onBiometricAuthenticationError(errorCode, errString.toString());
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onBiometricAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Guarda que el usuario ha habilitado el login biométrico
     */
    public static void enableBiometricLogin(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("biometricEnabled", true);
        editor.putString("biometricUsername", username);
        editor.apply();
    }

    /**
     * Verifica si el usuario tiene habilitado el login biométrico
     */
    public static boolean isBiometricLoginEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        return prefs.getBoolean("biometricEnabled", false);
    }

    /**
     * Obtiene el nombre de usuario asociado al login biométrico
     */
    public static String getBiometricUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        return prefs.getString("biometricUsername", "");
    }

    /**
     * Deshabilita el login biométrico
     */
    public static void disableBiometricLogin(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SesionApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("biometricEnabled");
        editor.remove("biometricUsername");
        editor.apply();
    }
}
