package com.example.abacoqr.data.local;import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.example.abacoqr.data.local.entities.DispositivoEntity;
import java.util.List;

@Dao
public interface DispositivoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodos(List<DispositivoEntity> dispositivos);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertar(DispositivoEntity dispositivo);

    @Update
    void actualizar(DispositivoEntity dispositivo);

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

    @Query("SELECT * FROM dispositivos WHERE propietario = :propietario COLLATE NOCASE ORDER BY posicionCsv ASC")
    List<DispositivoEntity> buscarPorPropietario(String propietario);

    @Query("SELECT * FROM dispositivos WHERE articulo LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY posicionCsv ASC")
    List<DispositivoEntity> buscarPorArticulo(String query);

    @Query("SELECT * FROM dispositivos WHERE estado = :estado COLLATE NOCASE ORDER BY posicionCsv ASC")
    List<DispositivoEntity> buscarPorEstado(String estado);

    @Query("SELECT * FROM dispositivos WHERE subsede LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY posicionCsv ASC")
    List<DispositivoEntity> buscarPorSubsede(String query);

    @RawQuery
    List<DispositivoEntity> buscarConFiltros(SupportSQLiteQuery query);

    @Query("SELECT EXISTS(SELECT 1 FROM dispositivos WHERE numeroSerie = :sn COLLATE NOCASE)")
    boolean existePorSerie(String sn);
}