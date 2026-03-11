package com.example.abacoqr.data;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de utilidad para Room que permite persistir tipos de datos complejos.
 * Room no puede guardar listas de forma nativa en SQLite, por lo que esta clase
 * convierte las listas en cadenas JSON para su almacenamiento y viceversa.
 */
public class Converters {

    /**
     * Convierte una cadena JSON almacenada en la base de datos de nuevo a una lista de Strings.
     * 
     * @param value Cadena JSON recuperada de la base de datos.
     * @return Lista de Strings reconstruida.
     */
    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null) return new ArrayList<>(); // Evita NullPointerException
        Type listType = new TypeToken<List<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    /**
     * Convierte una lista de Strings a una representación JSON para ser guardada en SQLite.
     * 
     * @param list Lista de Strings a persistir.
     * @return Cadena JSON lista para ser almacenada en una columna de texto.
     */
    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) return "[]"; // Evita NullPointerException
        return new Gson().toJson(list);
    }
}
