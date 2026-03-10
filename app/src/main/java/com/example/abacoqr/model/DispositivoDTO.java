package com.example.abacoqr.model;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) para dispositivos.
 * Representa una versión simplificada y ligera del dispositivo, ideal para ser transportada
 * entre actividades o mostrada en listas de búsqueda (RecyclerView) sin la carga 
 * de manejar filas completas de CSV.
 */
public class DispositivoDTO implements Serializable {

    private String propietario;
    private String subsede;
    private String pabellon;
    private String planta;
    private String espacio;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String estado;

    /**
     * Constructor para inicializar un objeto de transporte de datos.
     */
    public DispositivoDTO(String propietario, String subsede, String pabellon, String planta, 
                          String espacio, String marca, String modelo, String numeroSerie, String estado) {
        this.propietario = propietario;
        this.subsede = subsede;
        this.pabellon = pabellon;
        this.planta = planta;
        this.espacio = espacio;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.estado = estado;
    }

    // Getters básicos para la visualización en la interfaz
    public String getPropietario() { return propietario; }
    public String getSubsede() { return subsede; }
    public String getPabellon() { return pabellon; }
    public String getPlanta() { return planta; }
    public String getEspacio() { return espacio; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public String getEstado() { return estado; }
}
