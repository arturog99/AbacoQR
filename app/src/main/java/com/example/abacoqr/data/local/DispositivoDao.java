package com.example.abacoqr.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.example.abacoqr.data.local.entities.DispositivoEntity;
import java.util.List;

/**
 * DAO optimizado para rendimiento. 
 * Las búsquedas se realizan directamente en SQLite para ahorrar RAM.
 */
@Dao
public interface DispositivoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodos(List<DispositivoEntity> dispositivos);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(DispositivoEntity dispositivo);

    @Query("DELETE FROM dispositivos")
    void borrarTodo();

    @Query("SELECT COUNT(*) FROM dispositivos")
    int getCountTotal();

    @Query("SELECT COUNT(*) FROM dispositivos WHERE estado = :estado COLLATE NOCASE")
    int getCountPorEstado(String estado);

    @Query("SELECT * FROM dispositivos WHERE numeroSerie = :nSerie COLLATE NOCASE LIMIT 1")
    DispositivoEntity obtenerPorSerie(String nSerie);

    @Query("SELECT * FROM dispositivos ORDER BY posicionCsv ASC")
    List<DispositivoEntity> obtenerTodos();

    @Query("SELECT EXISTS(SELECT 1 FROM dispositivos WHERE numeroSerie = :sn COLLATE NOCASE)")
    boolean existePorSerie(String sn);

    /**
     * Búsqueda dinámica ultra-rápida. 
     * Room ejecuta la consulta filtrada en el motor SQLite, devolviendo solo lo necesario.
     */
    @RawQuery
    List<DispositivoEntity> buscarConFiltros(SupportSQLiteQuery query);
}
