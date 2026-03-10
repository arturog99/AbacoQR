package com.example.abacoqr.viewmodel;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.abacoqr.data.repository.InventarioRepository;
import com.example.abacoqr.data.parser.CsvReader;
import com.example.abacoqr.model.Dispositivo;
import com.example.abacoqr.data.local.entities.DispositivoEntity;
import com.example.abacoqr.model.HistorialCambio;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ViewModel optimizado para entornos de producción.
 * Implementa filtrado eficiente en SQL, seguridad de datos en CSV y gestión de estados de UI.
 */
public class InventarioViewModel extends AndroidViewModel {

    private final InventarioRepository repository;
    private final MutableLiveData<Estadisticas> estadisticas = new MutableLiveData<>(new Estadisticas(0, 0, 0));
    private final MutableLiveData<String> mensaje = new MutableLiveData<>();
    private final MutableLiveData<List<HistorialCambio>> historial = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Dispositivo>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> estaCargando = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hayDatosCargados = new MutableLiveData<>(false);

    private Uri csvUri;
    private String usuario;
    private static final String PREFS_NAME = "InventarioPrefs";
    private static final String KEY_CSV_URI = "last_csv_uri";

    public InventarioViewModel(Application application) {
        super(application);
        this.repository = new InventarioRepository(application);
        CsvReader.cargarHeaderMapDesdePrefs(application);

        SharedPreferences prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_CSV_URI, null);
        if (uriString != null) this.csvUri = Uri.parse(uriString);
    }

    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getUsuario() { return usuario; }
    public LiveData<Estadisticas> getEstadisticas() { return estadisticas; }
    public LiveData<String> getMensaje() { return mensaje; }
    public LiveData<List<HistorialCambio>> getHistorial() { return historial; }
    public LiveData<List<Dispositivo>> getSearchResults() { return searchResults; }
    public LiveData<Boolean> getEstaCargando() { return estaCargando; }
    public LiveData<Boolean> getHayDatosCargados() { return hayDatosCargados; }

    private String getFechaInicioMesActual() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
    }

    public void cargarCsv(Uri uri) {
        estaCargando.setValue(true);
        this.csvUri = uri;
        try {
            getApplication().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) { Log.e("ViewModel", "Permisos", e); }

        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CSV_URI, uri.toString()).apply();

        repository.cargarCsv(getApplication(), uri, new InventarioRepository.Callback<Void>() {
            @Override public void onComplete(Void result) {
                hayDatosCargados.postValue(true);
                historial.postValue(new ArrayList<>());
                actualizarEstadisticas("Activo");
                estaCargando.postValue(false);
                mensaje.postValue("¡CSV Cargado correctamente!");
            }
            @Override public void onError(Exception e) {
                estaCargando.postValue(false);
                mensaje.postValue("Error al importar el archivo.");
            }
        });
    }

    public void actualizarEstadisticas(String estadoObjetivo) {
        if (!Boolean.TRUE.equals(hayDatosCargados.getValue())) {
            estadisticas.postValue(new Estadisticas(0, 0, 0));
            return;
        }
        repository.obtenerEstadisticas(estadoObjetivo, new InventarioRepository.Callback<int[]>() {
            @Override public void onComplete(int[] res) {
                estadisticas.postValue(new Estadisticas(res[0], res[1], res[2]));
            }
            @Override public void onError(Exception e) { }
        });
    }

    /**
     * Realiza una búsqueda manual delegando el 100% del filtrado a SQL.
     * Implementa lógica de seguridad para evitar sobrecarga de RAM.
     */
    public void realizarBusquedaConVerif(String campo, String query, int verifMode) {
        String q = query.trim();

        // Evitamos búsquedas vacías innecesarias
        if (q.isEmpty() && verifMode == 0) {
            mensaje.postValue("Escribe algo para buscar.");
            return;
        }

        estaCargando.setValue(true);
        String fechaCorte = getFechaInicioMesActual();

        String sn = "", prop = "", art = "", est = "", sub = "";

        switch (campo) {
            case "Nº Serie": sn = q; break;
            case "Propietario": prop = q; break;
            case "Artículo": art = q; break;
            case "Estado": est = q; break;
            case "Subsede": sub = q; break;
        }

        repository.buscarConFiltrosCampana(sn, est, prop, art, sub, "", "", "", verifMode, fechaCorte, new InventarioRepository.Callback<List<DispositivoEntity>>() {
            @Override public void onComplete(List<DispositivoEntity> resultados) {
                List<Dispositivo> listaParaUI = resultados.stream()
                        .map(InventarioViewModel.this::mapEntityToDispositivo)
                        .collect(Collectors.toList());
                searchResults.postValue(listaParaUI);
                estaCargando.postValue(false);
            }
            @Override public void onError(Exception e) {
                estaCargando.postValue(false);
                mensaje.postValue("Error en la búsqueda.");
            }
        });
    }

    public void realizarBusqueda(String campo, String query) {
        realizarBusquedaConVerif(campo, query, 0);
    }

    private Dispositivo mapEntityToDispositivo(DispositivoEntity e) {
        List<String> rowData = (e.fullRowData != null) ? new ArrayList<>(e.fullRowData) : new ArrayList<>();
        Dispositivo d = new Dispositivo(rowData, CsvReader.getHeaderMap());
        d.forceSetDataFull(e.propietario, e.subsede, e.pabellon, e.planta, e.espacio,
                e.marca, e.modelo, e.numeroSerie, e.estado, e.articulo, e.observaciones, e.verificadoCau);
        return d;
    }

    public void actualizarDispositivoCompleto(DispositivoEntity entity, String estadoAnt, String obsAnt, boolean esVerificacion) {
        Dispositivo dTemp = mapEntityToDispositivo(entity);
        dTemp.setEstado(entity.estado);
        dTemp.setObservaciones(entity.observaciones);
        dTemp.setPropietario(entity.propietario);
        dTemp.setSubsede(entity.subsede);
        dTemp.setPabellon(entity.pabellon);
        dTemp.setPlanta(entity.planta);
        dTemp.setEspacio(entity.espacio);
        dTemp.setUsuario(usuario);
        dTemp.actualizarFechaVerificacion();

        entity.verificadoCau = dTemp.getVerificadoCau();
        entity.fullRowData = dTemp.getFullRowData();
        entity.usuario = usuario;

        repository.actualizarDispositivo(entity, new InventarioRepository.Callback<Void>() {
            @Override public void onComplete(Void result) {
                mensaje.postValue("Inventario actualizado.");
                if (Boolean.TRUE.equals(hayDatosCargados.getValue())) {
                    actualizarEstadisticas("Activo");
                }
            }
            @Override public void onError(Exception e) { mensaje.postValue("Error al guardar."); }
        });

        List<HistorialCambio> listaActual = historial.getValue();
        if (listaActual != null) {
            listaActual.add(0, new HistorialCambio(entity.numeroSerie, estadoAnt, entity.estado, obsAnt, entity.observaciones, usuario));
            historial.postValue(listaActual);
        }
    }

    public void crearNuevoDispositivo(Map<String, String> datosFormulario, String sn, String art, String est) {
        estaCargando.postValue(true);
        repository.verificarExistencia(sn, new InventarioRepository.Callback<Boolean>() {
            @Override public void onComplete(Boolean existe) {
                if (Boolean.TRUE.equals(existe)) {
                    estaCargando.postValue(false);
                    mensaje.postValue("Error: El número de serie " + sn + " ya existe.");
                    return;
                }

                Map<String, Integer> map = CsvReader.getHeaderMap();
                List<String> header = CsvReader.getHeader();
                List<String> rowData = new ArrayList<>();
                if (header != null) for (int i = 0; i < header.size(); i++) rowData.add("");

                Dispositivo d = new Dispositivo(rowData, map);
                d.setEstado(est); d.setArticulo(art);
                d.setMarca(datosFormulario.get("Marca"));
                d.setModelo(datosFormulario.get("Modelo"));
                d.setPropietario(datosFormulario.get("Propietario"));
                d.setSubsede(datosFormulario.get("Subsede"));
                d.setPabellon(datosFormulario.get("Pabellón"));
                d.setPlanta(datosFormulario.get("Planta"));
                d.setEspacio(datosFormulario.get("Espacio"));

                // Marca de ALTA para Power BI
                String obsBase = datosFormulario.get("Observaciones");
                d.setObservaciones("[ALTA] " + (obsBase != null ? obsBase : ""));

                Integer idxSn = map.get("Nº Serie");
                if (idxSn != null) rowData.set(idxSn, sn);

                d.setUsuario(usuario);
                d.actualizarFechaVerificacion();

                DispositivoEntity entity = new DispositivoEntity(
                        sn, est, art, d.getMarca(), d.getModelo(), "",
                        d.getSubsede(), d.getPabellon(), d.getPlanta(), d.getEspacio(),
                        d.getObservaciones(), usuario, d.getPropietario(),
                        d.getVerificadoCau(), d.getFullRowData(), 999999
                );

                repository.actualizarDispositivo(entity, new InventarioRepository.Callback<Void>() {
                    @Override public void onComplete(Void result) {
                        mensaje.postValue("✅ Nuevo equipo añadido con éxito.");
                        actualizarEstadisticas("Activo");
                        estaCargando.postValue(false);
                    }
                    @Override public void onError(Exception e) { estaCargando.postValue(false); }
                });
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    public void guardarCambios() {
        if (csvUri == null) { mensaje.postValue("Error: No hay archivo."); return; }
        estaCargando.postValue(true);
        repository.obtenerTodoParaCsv(new InventarioRepository.Callback<List<DispositivoEntity>>() {
            @Override public void onComplete(List<DispositivoEntity> todasEntidades) {
                new Thread(() -> {
                    try (OutputStream os = getApplication().getContentResolver().openOutputStream(csvUri, "wt");
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Objects.requireNonNull(os)))) {

                        List<String> header = CsvReader.getHeader();
                        if (header != null && !header.isEmpty()) {
                            writer.write(joinCsvLine(header));
                            writer.newLine();
                        }
                        for (DispositivoEntity e : todasEntidades) {
                            writer.write(joinCsvLine(e.fullRowData));
                            writer.newLine();
                        }
                        writer.flush();
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            estaCargando.setValue(false);
                            mensaje.setValue("¡Archivo CSV actualizado!");
                        });
                    } catch (Exception ex) {
                        estaCargando.postValue(false);
                        mensaje.postValue("Error crítico al escribir en el archivo.");
                    }
                }).start();
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    /**
     * MEJORA DE SEGURIDAD: Une campos escapando caracteres especiales (comas, comillas)
     * para garantizar que el CSV sea compatible con Excel y Power BI.
     */
    private String joinCsvLine(List<String> fields) {
        if (fields == null) return "";
        return fields.stream()
                .map(field -> {
                    String clean = (field == null) ? "" : field;
                    if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) {
                        return "\"" + clean.replace("\"", "\"\"") + "\"";
                    }
                    return clean;
                })
                .collect(Collectors.joining(","));
    }



    public static class Estadisticas {
        public final int total; public final int validos; public final int descartados;
        public Estadisticas(int total, int validos, int descartados) {
            this.total = total; this.validos = validos; this.descartados = descartados;
        }
    }
}