package com.example.abacoqr.model;

import java.io.Serializable;

/**
 * Modelo de usuario para el sistema de autenticación
 */
public class Usuario implements Serializable {

    private String username;
    private String displayName;
    private String email;
    private String role;
    private long lastLogin;

    public Usuario(String username, String displayName, String email) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.role = "USER"; // Rol por defecto
        this.lastLogin = System.currentTimeMillis();
    }

    public Usuario(String username, String displayName, String email, String role) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
        this.lastLogin = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Actualiza el timestamp de último login
     */
    public void updateLastLogin() {
        this.lastLogin = System.currentTimeMillis();
    }

    /**
     * Verifica si el usuario tiene rol de administrador
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role);
    }

    /**
     * Verifica si el usuario tiene rol de supervisor
     */
    public boolean isSupervisor() {
        return "SUPERVISOR".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return username != null ? username.equals(usuario.username) : usuario.username == null;
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
}
