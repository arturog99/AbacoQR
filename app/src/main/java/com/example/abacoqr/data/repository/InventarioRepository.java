package com.example.abacoqr.data.repository;

import android.content.Context;
import android.net.Uri;
import androidx.sqlite.db.SimpleSQLiteQuery;
import com.example.abacoqr.data.local.AppDatabase;
import com.example.abacoqr.data.parser.CsvReader;
import com.example.abacoqr.data.local.DispositivoDao;
import com.example.abacoqr.data.local.entities.DispositivoEntity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repositorio central de la aplicación AbacoQR.
 * Mediador de datos sincronizado con el modelo simplificado del CSV.
 */
public class InventarioRepository {

    private final DispositivoDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public InventarioRepository(Context context) {
        dao = AppDatabase.getDatabase(context).dispositivoDao();
    }

    public interface Callback<T> {
        void onComplete(T result);
        void onError(Exception e);
    }

    public void verificarDatos(Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                callback.onComplete(dao.getCountTotal());
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void cargarCsv(Context context, Uri uri, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                dao.borrarTodo();
                List<DispositivoEntity> batch = new ArrayList<>();
                final int[] posicion = {0};

                CsvReader.leerYProcesar(context, uri, d -> {
                    DispositivoEntity entity = new DispositivoEntity(
                        d.getNumeroSerie(), d.getEstado(), d.getArticulo(), d.getMarca(),
                        d.getModelo(), d.getUbicacion(), d.getSubsede(), d.getPabellon(),
                        d.getPlanta(), d.getEspacio(), d.getObservaciones(),
                        d.getUsuario() != null ? d.getUsuario() : "",
                        d.getPropietario(),
                        d.getVerificadoCau() != null ? d.getVerificadoCau() : "",
                        d.getFullRowData(),
                        posicion[0]
                    );
                    batch.add(entity);
                    posicion[0]++;
                    if (batch.size() >= 1000) {
                        dao.insertarTodos(new ArrayList<>(batch));
                        batch.clear();
                    }
                });
                if (!batch.isEmpty()) dao.insertarTodos(batch);
                callback.onComplete(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void obtenerEstadisticas(String estado, Callback<int[]> callback) {
        executor.execute(() -> {
            int total = dao.getCountTotal();
            int filtrados = dao.getCountPorEstado(estado);
            callback.onComplete(new int[]{total, filtrados, total - filtrados});
        });
    }

    public void buscarConFiltrosCampana(String sn, String est, String prop, String art, String sub, String pab, String pla, String esp, int verifMode, String fechaCorte, Callback<List<DispositivoEntity>> callback) {
        executor.execute(() -> {
            StringBuilder sb = new StringBuilder("SELECT * FROM dispositivos WHERE 1=1");
            List<Object> args = new ArrayList<>();

            if (!TextUtilsIsEmpty(sn)) { sb.append(" AND numeroSerie = ? COLLATE NOCASE"); args.add(sn.trim()); }
            if (!TextUtilsIsEmpty(est)) { sb.append(" AND estado = ? COLLATE NOCASE"); args.add(est.trim()); }
            if (!TextUtilsIsEmpty(prop)) { sb.append(" AND propietario = ? COLLATE NOCASE"); args.add(prop.trim()); }
            if (!TextUtilsIsEmpty(art)) { sb.append(" AND articulo LIKE ? COLLATE NOCASE"); args.add("%" + art.trim() + "%"); }
            if (!TextUtilsIsEmpty(sub)) { sb.append(" AND subsede LIKE ? COLLATE NOCASE"); args.add("%" + sub.trim() + "%"); }
            if (!TextUtilsIsEmpty(pab)) { sb.append(" AND pabellon = ? COLLATE NOCASE"); args.add(pab.trim()); }
            if (!TextUtilsIsEmpty(pla)) { sb.append(" AND planta = ? COLLATE NOCASE"); args.add(pla.trim()); }
            if (!TextUtilsIsEmpty(esp)) { sb.append(" AND espacio = ? COLLATE NOCASE"); args.add(esp.trim()); }

            sb.append(" ORDER BY posicionCsv ASC");
            List<DispositivoEntity> iniciales = dao.buscarConFiltros(new SimpleSQLiteQuery(sb.toString(), args.toArray()));

            if (verifMode == 0 || TextUtilsIsEmpty(fechaCorte)) {
                callback.onComplete(iniciales);
                return;
            }

            List<DispositivoEntity> filtradosFinales = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                Date dCorte = sdf.parse(fechaCorte);
                for (DispositivoEntity entity : iniciales) {
                    boolean esValido = false;
                    String fEntity = entity.verificadoCau;
                    if (fEntity == null || fEntity.isEmpty() || fEntity.equals("N/A")) {
                        if (verifMode == 2) esValido = true;
                    } else {
                        try {
                            Date dEntity = sdf.parse(fEntity.substring(0, 10));
                            if (verifMode == 1 && !dEntity.before(dCorte)) esValido = true;
                            else if (verifMode == 2 && dEntity.before(dCorte)) esValido = true;
                        } catch (Exception ex) { if (verifMode == 2) esValido = true; }
                    }
                    if (esValido) filtradosFinales.add(entity);
                }
                callback.onComplete(filtradosFinales);
            } catch (Exception e) {
                callback.onComplete(iniciales);
            }
        });
    }

    public void buscar(String campo, String query, Callback<List<DispositivoEntity>> callback) {
        buscarConFiltrosCampana("", "", "", "", "", "", "", "", 0, "", callback);
    }

    private boolean TextUtilsIsEmpty(String str) { return str == null || str.trim().isEmpty(); }

    /**
     * MEJORA: Realiza un UPDATE real en SQL para equipos existentes.
     */
    public void actualizarDispositivo(DispositivoEntity entity, Callback<Void> callback) {
        executor.execute(() -> {
            dao.actualizar(entity); // ¡CORREGIDO! Usa @Update
            callback.onComplete(null);
        });
    }

    /**
     * Inserta un equipo totalmente nuevo.
     */
    public void insertarNuevo(DispositivoEntity entity, Callback<Void> callback) {
        executor.execute(() -> {
            dao.insertar(entity); // Usa @Insert IGNORE
            callback.onComplete(null);
        });
    }

    public void obtenerDispositivoPorSerie(String sn, Callback<DispositivoEntity> callback) {
        executor.execute(() -> callback.onComplete(dao.obtenerPorSerie(sn)));
    }

    public void obtenerTodoParaCsv(Callback<List<DispositivoEntity>> callback) {
        executor.execute(() -> callback.onComplete(dao.obtenerTodos()));
    }

    public void verificarExistencia(String sn, Callback<Boolean> callback) {
        executor.execute(() -> callback.onComplete(dao.existePorSerie(sn)));
    }
}
