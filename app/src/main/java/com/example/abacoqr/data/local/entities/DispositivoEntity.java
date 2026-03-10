package com.example.abacoqr.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import java.util.List;

/**
 * Entidad simplificada compatible 100% con el CSV.
 */
@Entity(tableName = "dispositivos")
public class DispositivoEntity {

    @PrimaryKey
    @NonNull
    public String numeroSerie;
    
    public String estado;
    public String articulo;
    public String marca;
    public String modelo;
    public String ubicacion; 
    public String subsede;
    public String pabellon;
    public String planta;
    public String espacio;
    public String observaciones;
    public String usuario;
    public String propietario;
    
    /** Fecha de revisión en formato texto (dd/MM/yyyy HH:mm:ss) */
    public String verificadoCau;
    
    public List<String> fullRowData;
    public int posicionCsv;

    public DispositivoEntity(@NonNull String numeroSerie, String estado, String articulo, String marca, 
                             String modelo, String ubicacion, String subsede, String pabellon, 
                             String planta, String espacio, String observaciones, String usuario, 
                             String propietario, String verificadoCau, List<String> fullRowData, int posicionCsv) {
        this.numeroSerie = numeroSerie;
        this.estado = estado;
        this.articulo = articulo;
        this.marca = marca;
        this.modelo = modelo;
        this.ubicacion = ubicacion;
        this.subsede = subsede;
        this.pabellon = pabellon;
        this.planta = planta;
        this.espacio = espacio;
        this.observaciones = observaciones;
        this.usuario = usuario;
        this.propietario = propietario;
        this.verificadoCau = verificadoCau;
        this.fullRowData = fullRowData;
        this.posicionCsv = posicionCsv;
    }
}
