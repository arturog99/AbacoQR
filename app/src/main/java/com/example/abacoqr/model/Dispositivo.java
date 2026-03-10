package com.example.abacoqr.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modelo de negocio que representa un dispositivo del inventario.
 */
public class Dispositivo implements Serializable {

    private final List<String> rowData;
    private final Map<String, Integer> headerMap;

    private static final String KEY_SERIE = "Nº Serie";
    private static final String KEY_ESTADO = "Estado";
    private static final String KEY_ARTICULO = "Artículo";
    private static final String KEY_MARCA = "Marca";
    private static final String KEY_MODELO = "Modelo";
    private static final String KEY_UBICACION = "Descripción Espacio";
    private static final String KEY_SUBSEDE = "Subsede";
    private static final String KEY_PABELLON = "Pabellón";
    private static final String KEY_PLANTA = "Planta";
    private static final String KEY_ESPACIO = "Espacio";
    private static final String KEY_OBSERVACIONES = "Observaciones";
    private static final String KEY_USUARIO = "Usuario";
    private static final String KEY_PROPIETARIO = "Propietario";
    private static final String KEY_VERIFICADO_CAU = "Verificado CAU";

    private String backupSerie, backupEstado, backupArticulo, backupMarca, backupModelo, 
                   backupUbicacion, backupSubsede, backupPabellon, backupPlanta, 
                   backupEspacio, backupObservaciones, backupPropietario, backupVerificadoCau;

    public Dispositivo(List<String> rowData, Map<String, Integer> headerMap) {
        this.rowData = rowData;
        this.headerMap = headerMap;
    }

    public void forceSetData(String prop, String sub, String pab, String pla, String esp, 
                            String mar, String mod, String sn, String est, String art, String obs) {
        this.backupPropietario = prop; this.backupSubsede = sub; this.backupPabellon = pab;
        this.backupPlanta = pla; this.backupEspacio = esp; this.backupMarca = mar;
        this.backupModelo = mod; this.backupSerie = sn; this.backupEstado = est;
        this.backupArticulo = art; this.backupObservaciones = obs;
    }

    public void forceSetDataFull(String prop, String sub, String pab, String pla, String esp, 
                               String mar, String mod, String sn, String est, String art, 
                               String obs, String verificado) {
        forceSetData(prop, sub, pab, pla, esp, mar, mod, sn, est, art, obs);
        this.backupVerificadoCau = verificado;
    }

    private String getData(String key, String backup) {
        if (headerMap != null) {
            Integer index = headerMap.get(key);
            if (index != null && index < rowData.size()) {
                String val = rowData.get(index);
                if (val != null && !val.isEmpty()) return val;
            }
        }
        return backup != null ? backup : "N/A";
    }

    private void setData(String key, String value) {
        if (headerMap != null) {
            Integer index = headerMap.get(key);
            if (index != null) {
                while (rowData.size() <= index) rowData.add("");
                rowData.set(index, value);
            }
        }
    }

    public String getNumeroSerie() { return getData(KEY_SERIE, backupSerie); }
    public String getEstado() { return getData(KEY_ESTADO, backupEstado); }
    public void setEstado(String estado) { setData(KEY_ESTADO, estado); this.backupEstado = estado; }
    public String getArticulo() { return getData(KEY_ARTICULO, backupArticulo); }
    public void setArticulo(String a) { setData(KEY_ARTICULO, a); this.backupArticulo = a; } // <-- ¡AÑADIDO!
    public String getMarca() { return getData(KEY_MARCA, backupMarca); }
    public void setMarca(String m) { setData(KEY_MARCA, m); this.backupMarca = m; } // <-- ¡AÑADIDO!
    public String getModelo() { return getData(KEY_MODELO, backupModelo); }
    public void setModelo(String m) { setData(KEY_MODELO, m); this.backupModelo = m; } // <-- ¡AÑADIDO!
    public String getUbicacion() { return getData(KEY_UBICACION, backupUbicacion); }
    public String getSubsede() { return getData(KEY_SUBSEDE, backupSubsede); }
    public void setSubsede(String s) { setData(KEY_SUBSEDE, s); this.backupSubsede = s; }
    public String getPabellon() { return getData(KEY_PABELLON, backupPabellon); }
    public void setPabellon(String p) { setData(KEY_PABELLON, p); this.backupPabellon = p; }
    public String getPlanta() { return getData(KEY_PLANTA, backupPlanta); }
    public void setPlanta(String p) { setData(KEY_PLANTA, p); this.backupPlanta = p; }
    public String getEspacio() { return getData(KEY_ESPACIO, backupEspacio); }
    public void setEspacio(String e) { setData(KEY_ESPACIO, e); this.backupEspacio = e; }
    public String getObservaciones() { return getData(KEY_OBSERVACIONES, backupObservaciones); }
    public void setObservaciones(String obs) { setData(KEY_OBSERVACIONES, obs); this.backupObservaciones = obs; }
    public String getUsuario() { return getData(KEY_USUARIO, null); }
    public void setUsuario(String u) { setData(KEY_USUARIO, u); }
    public String getPropietario() { return getData(KEY_PROPIETARIO, backupPropietario); }
    public void setPropietario(String p) { setData(KEY_PROPIETARIO, p); this.backupPropietario = p; }
    public String getVerificadoCau() { return getData(KEY_VERIFICADO_CAU, backupVerificadoCau); }
    
    public void actualizarFechaVerificacion() {
        String fechaHoy = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        setData(KEY_VERIFICADO_CAU, fechaHoy);
        this.backupVerificadoCau = fechaHoy;
    }

    public List<String> getFullRowData() { return rowData; }
}
