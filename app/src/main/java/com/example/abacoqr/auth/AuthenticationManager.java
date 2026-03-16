package com.example.abacoqr.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.abacoqr.model.Usuario;

/**
 * Clase de autenticación centralizada.
 * Fácilmente reemplazable para integrar con API real en el futuro.
 */
public class AuthenticationManager {

    private static final String TAG = "AuthenticationManager";
    private static final String PREFS_NAME = "SesionApp";
    
    // Modos de autenticación
    public enum AuthMode {
        DEVELOPMENT,  // Credenciales hardcodeadas (actual)
        API_REMOTE,   // API remota (futuro)
        OFFLINE       // Solo modo offline
    }
    
    private static AuthMode currentMode = AuthMode.DEVELOPMENT;
    
    /**
     * Autentica un usuario con las credenciales proporcionadas
     */
    public static AuthResult authenticate(Context context, String username, String password) {
        switch (currentMode) {
            case DEVELOPMENT:
                return authenticateDevelopment(context, username, password);
            case API_REMOTE:
                return authenticateWithApi(context, username, password);
            case OFFLINE:
                return authenticateOffline(context, username, password);
            default:
                return new AuthResult(false, "Modo de autenticación no configurado", null);
        }
    }
    
    /**
     * Autenticación en modo desarrollo (credenciales hardcodeadas)
     * Nota: Sesión de 1 hora base, 24 horas con "Recordarme"
     */
    private static AuthResult authenticateDevelopment(Context context, String username, String password) {
        // TODO: Reemplazar con validación real en producción
        if (username.equals("admin") && password.equals("admin")) {
            Usuario user = new Usuario(username, "Administrador", "admin@abacoqr.com");
            return new AuthResult(true, "Autenticación exitosa", user);
        }
        
        // Usuarios adicionales para testing
        if (username.equals("user1") && password.equals("test123")) {
            Usuario user = new Usuario(username, "Usuario Test", "user1@abacoqr.com");
            return new AuthResult(true, "Autenticación exitosa", user);
        }
        
        if (username.equals("supervisor") && password.equals("sup2024")) {
            Usuario user = new Usuario(username, "Supervisor", "supervisor@abacoqr.com");
            return new AuthResult(true, "Autenticación exitosa", user);
        }
        
        return new AuthResult(false, "Credenciales incorrectas", null);
    }
    
    /**
     * Autenticación con API remota (placeholder para futuro)
     */
    private static AuthResult authenticateWithApi(Context context, String username, String password) {
        // TODO: Implementar llamada a API real
        // Ejemplo de estructura:
        /*
        try {
            // Llamada HTTP a endpoint de login
            LoginRequest request = new LoginRequest(username, password);
            LoginResponse response = apiService.login(request);
            
            if (response.isSuccess()) {
                // Guardar token JWT
                saveAuthToken(context, response.getToken());
                
                // Crear usuario desde respuesta
                Usuario user = new Usuario(
                    response.getUser().getUsername(),
                    response.getUser().getDisplayName(),
                    response.getUser().getEmail()
                );
                
                return new AuthResult(true, "Login exitoso", user);
            } else {
                return new AuthResult(false, response.getErrorMessage(), null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en autenticación API", e);
            return new AuthResult(false, "Error de conexión", null);
        }
        */
        
        // Temporal: fallback a desarrollo
        Log.w(TAG, "Modo API no implementado, usando desarrollo");
        return authenticateDevelopment(context, username, password);
    }
    
    /**
     * Autenticación offline (solo usuarios locales)
     */
    private static AuthResult authenticateOffline(Context context, String username, String password) {
        // TODO: Implementar validación contra base de datos local
        SharedPreferences prefs = context.getSharedPreferences("OfflineUsers", Context.MODE_PRIVATE);
        String savedPassword = prefs.getString("user_" + username, null);
        
        if (savedPassword != null && savedPassword.equals(password)) {
            String displayName = prefs.getString("display_" + username, username);
            Usuario user = new Usuario(username, displayName, "");
            return new AuthResult(true, "Autenticación offline exitosa", user);
        }
        
        return new AuthResult(false, "Usuario no encontrado en modo offline", null);
    }
    
    /**
     * Cierra la sesión del usuario
     */
    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Limpiar datos de sesión
        editor.remove("isLoggedIn");
        editor.remove("username");
        editor.remove("loginTime");
        editor.remove("lastActivityTime");
        editor.remove("rememberMe");
        editor.remove("extendedLoginTime");
        
        // Limpiar token si existe
        editor.remove("authToken");
        editor.remove("tokenExpiry");
        
        editor.apply();
        
        Log.d(TAG, "Sesión cerrada exitosamente");
    }
    
    /**
     * Verifica si hay una sesión activa
     */
    public static boolean hasActiveSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }
    
    /**
     * Obtiene el usuario actual
     */
    public static String getCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("username", "");
    }
    
    /**
     * Establece el modo de autenticación
     */
    public static void setAuthMode(AuthMode mode) {
        currentMode = mode;
        Log.i(TAG, "Modo de autenticación cambiado a: " + mode.name());
    }
    
    /**
     * Obtiene el modo de autenticación actual
     */
    public static AuthMode getAuthMode() {
        return currentMode;
    }
    
    /**
     * Guarda token de autenticación (para API real)
     */
    public static void saveAuthToken(Context context, String token, long expiryTime) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("authToken", token);
        editor.putLong("tokenExpiry", expiryTime);
        editor.apply();
    }
    
    /**
     * Verifica si el token es válido
     */
    public static boolean isTokenValid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long expiryTime = prefs.getLong("tokenExpiry", 0);
        return System.currentTimeMillis() < expiryTime;
    }
    
    /**
     * Resultado de autenticación
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final Usuario user;
        
        public AuthResult(boolean success, String message, Usuario user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Usuario getUser() { return user; }
    }
}
