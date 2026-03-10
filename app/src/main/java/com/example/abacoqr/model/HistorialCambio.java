package com.example.abacoqr.model;

import androidx.annotation.NonNull;
import java.io.Serializable;

/**
 * Representa un registro de modificación realizado durante la sesión actual.
 * Se utiliza para alimentar la lista del historial, permitiendo al usuario revisar
 * qué cambios ha efectuado antes de realizar el volcado final al CSV.
 */
public class HistorialCambio implements Serializable {

    private final String numeroSerie;
    private final String estadoAnterior;
    private final String estadoNuevo;
    private final String observacionesAnteriores;
    private final String observacionesNuevas;
    private final String usuario;

    /**
     * Constructor para registrar un evento de cambio.
     */
    public HistorialCambio(String numeroSerie, String estadoAnterior, String estadoNuevo, 
                           String obsAnterior, String obsNueva, String usuario) {
        this.numeroSerie = numeroSerie;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.observacionesAnteriores = obsAnterior;
        this.observacionesNuevas = obsNueva;
        this.usuario = usuario;
    }

    /**
     * Devuelve una representación legible del cambio para ser mostrada en diálogos de la UI.
     * Formatea los datos indicando el equipo, el cambio de estado y quién lo realizó.
     */
    @NonNull
    @Override
    public String toString() {
        return "📦 Equipo: " + numeroSerie + 
               "\n🔄 Cambio: " + estadoAnterior + " -> " + estadoNuevo +
               "\n✍️ Obs: " + observacionesNuevas +
               "\n👤 Técnico: " + usuario;
    }
}
