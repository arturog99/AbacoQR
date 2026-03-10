package com.example.abacoqr.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.abacoqr.data.Converters;
import com.example.abacoqr.data.local.entities.DispositivoEntity;

/**
 * Clase principal de la base de datos Room que actúa como el punto de acceso central
 * para la persistencia de datos de la aplicación.
 * 
 * Se encarga de gestionar la conexión con la base de datos SQLite local y define
 * las entidades que la componen y sus conversores de tipos.
 */
@Database(entities = {DispositivoEntity.class}, version = 3)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Proporciona el DAO (Data Access Object) para realizar operaciones sobre la tabla de dispositivos.
     * @return Instancia del DispositivoDao.
     */
    public abstract DispositivoDao dispositivoDao();

    /** Instancia única de la base de datos (Patrón Singleton) */
    private static volatile AppDatabase INSTANCE;

    /**
     * Obtiene la instancia única de la base de datos.
     * Implementa el patrón Singleton para asegurar que solo exista una conexión abierta
     * en toda la aplicación, evitando fugas de memoria y conflictos.
     * 
     * @param context Contexto de la aplicación.
     * @return La instancia de AppDatabase.
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "abaco_qr_db")
                            /* 
                             * Permite a Room recrear las tablas si se cambia la versión del esquema
                             * sin haber definido una migración específica. Útil durante el desarrollo
                             * para reflejar cambios en las entidades rápidamente.
                             */
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
