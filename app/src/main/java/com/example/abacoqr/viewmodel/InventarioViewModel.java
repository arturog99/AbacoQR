package com.example.abacoqr.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.abacoqr.data.repository.InventarioRepository;
import com.example.abacoqr.data.parser.CsvReader;
import com.example.abacoqr.model.Dispositivo;
import com.example.abacoqr.data.local.entities.DispositivoEntity;
import com.example.abacoqr.model.HistorialCambio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ViewModel profesionalizado.
 * Gestiona hilos mediante ExecutorService y asegura que la comunicación con la UI sea en el Main Thread.
 */
public class InventarioViewModel extends AndroidViewModel {

    private final InventarioRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Estadisticas> estadisticas = new MutableLiveData<>(new Estadisticas(0, 0, 0));
    private final MutableLiveData<String> mensaje = new MutableLiveData<>();
    private final MutableLiveData<List<HistorialCambio>> historial = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Dispositivo>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> estaCargando = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hayDatosCargados = new MutableLiveData<>(false);
    
    private final MutableLiveData<Dispositivo> dispositivoEncontrado = new MutableLiveData<>();
    private final MutableLiveData<List<Dispositivo>> resultadosPdf = new MutableLiveData<>();

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
    public LiveData<Dispositivo> getDispositivoEncontrado() { return dispositivoEncontrado; }
    public LiveData<List<Dispositivo>> getResultadosPdf() { return resultadosPdf; }

    private String getFechaInicioMesActual() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
    }

    public void cargarCsv(Uri uri) {
        estaCargando.setValue(true);
        this.csvUri = uri;
        repository.cargarCsv(getApplication(), uri, new InventarioRepository.Callback<Void>() {
            @Override public void onComplete(Void result) {
                hayDatosCargados.postValue(true);
                actualizarEstadisticas("Activo");
                estaCargando.postValue(false);
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    public void actualizarEstadisticas(String estadoObjetivo) {
        repository.obtenerEstadisticas(estadoObjetivo, new InventarioRepository.Callback<int[]>() {
            @Override public void onComplete(int[] res) {
                estadisticas.postValue(new Estadisticas(res[0], res[1], res[2]));
            }
            @Override public void onError(Exception e) { }
        });
    }

    public void realizarBusquedaConVerif(String campo, String query, int verifMode) {
        estaCargando.setValue(true);
        String fechaCorte = getFechaInicioMesActual();
        repository.buscarConFiltrosCampana(query, "", "", "", "", "", "", "", verifMode, fechaCorte, new InventarioRepository.Callback<List<DispositivoEntity>>() {
            @Override public void onComplete(List<DispositivoEntity> resultados) {
                List<Dispositivo> lista = resultados.stream().map(InventarioViewModel.this::mapEntityToDispositivo).collect(Collectors.toList());
                searchResults.postValue(lista);
                estaCargando.postValue(false);
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    public void buscarPorSerie(String sn) {
        repository.obtenerDispositivoPorSerie(sn, new InventarioRepository.Callback<DispositivoEntity>() {
            @Override public void onComplete(DispositivoEntity result) {
                dispositivoEncontrado.postValue(result != null ? mapEntityToDispositivo(result) : null);
            }
            @Override public void onError(Exception e) { dispositivoEncontrado.postValue(null); }
        });
    }

    public void filtrarParaPdf(String est, String prop, String art, String pla, String esp, int verifMode) {
        estaCargando.setValue(true);
        String fechaCorte = getFechaInicioMesActual();
        repository.buscarConFiltrosCampana("", est, prop, art, "", "", pla, esp, verifMode, fechaCorte, new InventarioRepository.Callback<List<DispositivoEntity>>() {
            @Override public void onComplete(List<DispositivoEntity> resultados) {
                List<Dispositivo> lista = resultados.stream().map(InventarioViewModel.this::mapEntityToDispositivo).collect(Collectors.toList());
                resultadosPdf.postValue(lista);
                estaCargando.postValue(false);
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    private Dispositivo mapEntityToDispositivo(DispositivoEntity e) {
        List<String> rowData = (e.fullRowData != null) ? new ArrayList<>(e.fullRowData) : new ArrayList<>();
        Dispositivo d = new Dispositivo(rowData, CsvReader.getHeaderMap());
        d.forceSetDataFull(e.propietario, e.subsede, e.pabellon, e.planta, e.espacio, e.marca, e.modelo, e.numeroSerie, e.estado, e.articulo, e.observaciones, e.verificadoCau);
        return d;
    }

    public void actualizarDispositivoCompleto(Dispositivo d, String estadoAnt, String obsAnt, boolean esVerificacion) {
        repository.obtenerDispositivoPorSerie(d.getNumeroSerie(), new InventarioRepository.Callback<DispositivoEntity>() {
            @Override public void onComplete(DispositivoEntity entity) {
                if (entity != null) {
                    entity.estado = d.getEstado();
                    entity.observaciones = d.getObservaciones();
                    entity.propietario = d.getPropietario();
                    entity.subsede = d.getSubsede();
                    entity.pabellon = d.getPabellon();
                    entity.planta = d.getPlanta();
                    entity.espacio = d.getEspacio();
                    entity.usuario = usuario;
                    d.actualizarFechaVerificacion();
                    entity.verificadoCau = d.getVerificadoCau();
                    entity.fullRowData = d.getFullRowData();

                    repository.actualizarDispositivo(entity, new InventarioRepository.Callback<Void>() {
                        @Override public void onComplete(Void result) {
                            actualizarEstadisticas("Activo");
                            mensaje.postValue("Inventario actualizado.");

                            List<HistorialCambio> listaActual = historial.getValue();
                            if (listaActual == null) {
                                listaActual = new ArrayList<>();
                            }
                            // Añadimos el registro nuevo en la posición 0 (arriba del todo)
                            listaActual.add(0, new HistorialCambio(
                                    d.getNumeroSerie(),
                                    estadoAnt,
                                    d.getEstado(),
                                    obsAnt,
                                    d.getObservaciones(),
                                    usuario
                            ));
                            historial.postValue(listaActual);
                            // 👆 --------------------------------------------- 👆
                        }
                        @Override public void onError(Exception e) {
                            mensaje.postValue("Error al guardar.");
                        }
                    });
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    public void crearNuevoDispositivo(Map<String, String> datosFormulario, String sn, String art, String est) {
        estaCargando.postValue(true);
        repository.verificarExistencia(sn, new InventarioRepository.Callback<Boolean>() {
            @Override public void onComplete(Boolean existe) {
                if (Boolean.TRUE.equals(existe)) {
                    estaCargando.postValue(false);
                    mensaje.postValue("Error: El Nº Serie ya existe.");
                    return;
                }
                Map<String, Integer> map = CsvReader.getHeaderMap();
                List<String> header = CsvReader.getHeader();
                List<String> rowData = new ArrayList<>();
                if (header != null) for (int i = 0; i < header.size(); i++) rowData.add("");
                Dispositivo d = new Dispositivo(rowData, map);
                d.setEstado(est); d.setArticulo(art);
                d.setMarca(datosFormulario.get("Marca")); d.setModelo(datosFormulario.get("Modelo"));
                d.setPropietario(datosFormulario.get("Propietario")); d.setSubsede(datosFormulario.get("Subsede"));
                d.setPabellon(datosFormulario.get("Pabellón")); d.setPlanta(datosFormulario.get("Planta"));
                d.setEspacio(datosFormulario.get("Espacio"));
                d.setObservaciones("[ALTA] " + (datosFormulario.get("Observaciones") != null ? datosFormulario.get("Observaciones") : ""));
                Integer idxSn = map.get("Nº Serie"); if (idxSn != null) rowData.set(idxSn, sn);
                d.setUsuario(usuario); d.actualizarFechaVerificacion();
                DispositivoEntity entity = new DispositivoEntity(sn, est, art, d.getMarca(), d.getModelo(), "", d.getSubsede(), d.getPabellon(), d.getPlanta(), d.getEspacio(), d.getObservaciones(), usuario, d.getPropietario(), d.getVerificadoCau(), d.getFullRowData(), 999999);
                repository.insertarNuevo(entity, new InventarioRepository.Callback<Void>() {
                    @Override public void onComplete(Void result) {
                        actualizarEstadisticas("Activo");
                        estaCargando.postValue(false);
                        mensaje.postValue("✅ Nuevo equipo añadido.");
                    }
                    @Override public void onError(Exception e) { estaCargando.postValue(false); }
                });
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    public void guardarCambios() {
        if (csvUri == null) return;
        estaCargando.postValue(true);
        repository.obtenerTodoParaCsv(new InventarioRepository.Callback<List<DispositivoEntity>>() {
            @Override public void onComplete(List<DispositivoEntity> todasEntidades) {
                executor.execute(() -> {
                    try (OutputStream os = getApplication().getContentResolver().openOutputStream(csvUri, "wt");
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Objects.requireNonNull(os)))) {
                        List<String> header = CsvReader.getHeader();
                        if (header != null) { writer.write(joinCsvLine(header)); writer.newLine(); }
                        for (DispositivoEntity e : todasEntidades) { writer.write(joinCsvLine(e.fullRowData)); writer.newLine(); }
                        writer.flush();
                        mainHandler.post(() -> {
                            estaCargando.setValue(false);
                            mensaje.setValue("¡CSV guardado con éxito!");
                        });
                    } catch (Exception ex) {
                        mainHandler.post(() -> { estaCargando.setValue(false); mensaje.setValue("Error al guardar."); });
                    }
                });
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    public void exportarParaCompartir(OnExportReadyListener listener) {
        estaCargando.postValue(true);
        repository.obtenerTodoParaCsv(new InventarioRepository.Callback<List<DispositivoEntity>>() {
            @Override public void onComplete(List<DispositivoEntity> todasEntidades) {
                executor.execute(() -> {
                    try {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        String fileName = "Inventario_Abaco_" + timeStamp + ".csv";
                        File file = new File(getApplication().getCacheDir(), fileName);
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                            List<String> header = CsvReader.getHeader();
                            if (header != null) { writer.write(joinCsvLine(header)); writer.newLine(); }
                            for (DispositivoEntity e : todasEntidades) { writer.write(joinCsvLine(e.fullRowData)); writer.newLine(); }
                            writer.flush();
                        }
                        Uri contentUri = FileProvider.getUriForFile(getApplication(), getApplication().getPackageName() + ".fileprovider", file);
                        mainHandler.post(() -> {
                            estaCargando.setValue(false);
                            listener.onExportReady(contentUri);
                        });
                    } catch (Exception ex) {
                        mainHandler.post(() -> { estaCargando.setValue(false); mensaje.setValue("Error al exportar."); });
                    }
                });
            }
            @Override public void onError(Exception e) { estaCargando.postValue(false); }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    public interface OnExportReadyListener { void onExportReady(Uri uri); }

    private String joinCsvLine(List<String> fields) {
        if (fields == null) return "";
        return fields.stream().map(f -> {
            String c = (f == null) ? "" : f;
            if (c.contains(",") || c.contains("\"") || c.contains("\n")) return "\"" + c.replace("\"", "\"\"") + "\"";
            return c;
        }).collect(Collectors.joining(","));
    }

    public static class Estadisticas {
        public final int total; public final int validos; public final int descartados;
        public Estadisticas(int total, int validos, int descartados) { this.total = total; this.validos = validos; this.descartados = descartados; }
    }
}
