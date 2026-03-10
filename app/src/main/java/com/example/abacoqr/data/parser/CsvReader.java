package com.example.abacoqr.data.parser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.example.abacoqr.model.Dispositivo;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase de utilidad encargada de leer y procesar archivos CSV.
 * Implementa una lectura eficiente mediante streaming para soportar archivos de gran tamaño (100k+ registros)
 * sin agotar la memoria RAM del dispositivo.
 */
public class CsvReader {

    /** Almacena los nombres de las columnas del último archivo CSV procesado */
    private static List<String> header;
    
    /** Mapa que vincula el nombre de una columna con su índice (posición) en el archivo */
    private static Map<String, Integer> lastHeaderMap;
    
    private static final String PREFS_NAME = "InventarioPrefs";
    private static final String KEY_HEADER_MAP = "last_header_map";

    /**
     * Interfaz de callback para procesar cada dispositivo de forma individual
     * a medida que se lee cada línea del archivo CSV.
     */
    public interface OnDeviceParsedListener {
        void onDeviceParsed(Dispositivo dispositivo);
    }

    /**
     * Lee un archivo CSV desde una URI y procesa cada fila línea a línea.
     * 
     * @param context Contexto de la aplicación para acceder al ContentResolver.
     * @param uri URI del archivo CSV seleccionado por el usuario.
     * @param listener Callback que se ejecuta cada vez que se parsea un dispositivo con éxito.
     */
    public static void leerYProcesar(Context context, Uri uri, OnDeviceParsedListener listener) {
        header = new ArrayList<>();
        Map<String, Integer> headerMap = new HashMap<>();

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            // LEER LA CABECERA (Primera línea)
            String headerLine = reader.readLine();
            if (headerLine == null) return;

            // Eliminar el carácter invisible BOM si existe (común en archivos de Excel)
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] rawHeaders = headerLine.split(",");
            boolean esCabeceraReal = false;

            // Identificar la posición de cada columna clave basándose en su nombre
            for (int i = 0; i < rawHeaders.length; i++) {
                String cleanHeader = rawHeaders[i].trim();
                header.add(cleanHeader);
                
                // Si encontramos palabras clave, confirmamos que es una línea de cabecera
                if (cleanHeader.equalsIgnoreCase("Nº Serie") || cleanHeader.equalsIgnoreCase("Estado") || cleanHeader.equalsIgnoreCase("Artículo")) {
                    esCabeceraReal = true;
                }

                if (cleanHeader.equalsIgnoreCase("Nº Serie")) headerMap.put("Nº Serie", i);
                else if (cleanHeader.equalsIgnoreCase("Estado")) headerMap.put("Estado", i);
                else if (cleanHeader.equalsIgnoreCase("Artículo")) headerMap.put("Artículo", i);
                else if (cleanHeader.equalsIgnoreCase("Marca")) headerMap.put("Marca", i);
                else if (cleanHeader.equalsIgnoreCase("Modelo")) headerMap.put("Modelo", i);
                else if (cleanHeader.equalsIgnoreCase("Descripción Espacio")) headerMap.put("Descripción Espacio", i);
                else if (cleanHeader.equalsIgnoreCase("Subsede")) headerMap.put("Subsede", i);
                else if (cleanHeader.equalsIgnoreCase("Pabellón")) headerMap.put("Pabellón", i);
                else if (cleanHeader.equalsIgnoreCase("Planta")) headerMap.put("Planta", i);
                else if (cleanHeader.equalsIgnoreCase("Espacio")) headerMap.put("Espacio", i);
                else if (cleanHeader.equalsIgnoreCase("Observaciones")) headerMap.put("Observaciones", i);
                else if (cleanHeader.equalsIgnoreCase("Usuario")) headerMap.put("Usuario", i);
                else if (cleanHeader.equalsIgnoreCase("Propietario")) headerMap.put("Propietario", i);
                else if (cleanHeader.equalsIgnoreCase("Verificado CAU")) headerMap.put("Verificado CAU", i);
            }
            
            lastHeaderMap = headerMap;
            // Guardar el mapa de forma persistente para futuros inicios de la app
            guardarHeaderMapEnPrefs(context, headerMap);

            // Si la primera línea no era cabecera sino datos, la procesamos como el primer registro
            if (!esCabeceraReal) {
                List<String> firstRowData = new ArrayList<>();
                for (String col : rawHeaders) firstRowData.add(col.trim());
                listener.onDeviceParsed(new Dispositivo(firstRowData, headerMap));
            }

            // LEER EL RESTO DE FILAS (Procesamiento por streaming)
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);
                List<String> cleanRowData = new ArrayList<>();
                for (String col : columns) {
                    cleanRowData.add(col.trim());
                }
                listener.onDeviceParsed(new Dispositivo(cleanRowData, headerMap));
            }
        } catch (Exception e) {
            Log.e("CsvReader", "Error leyendo CSV: " + e.getMessage());
        }
    }

    /**
     * Guarda el mapa de cabeceras en SharedPreferences.
     * Esto permite que la app sepa interpretar los datos de Room tras un reinicio.
     */
    private static void guardarHeaderMapEnPrefs(Context context, Map<String, Integer> map) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(map);
        prefs.edit().putString(KEY_HEADER_MAP, json).apply();
    }

    /**
     * Recupera el mapa de cabeceras guardado anteriormente.
     * Se debe llamar al arrancar la aplicación.
     */
    public static void cargarHeaderMapDesdePrefs(Context context) {
        if (lastHeaderMap != null) return; 

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HEADER_MAP, null);
        if (json != null) {
            Type type = new com.google.gson.reflect.TypeToken<Map<String, Integer>>(){}.getType();
            lastHeaderMap = new Gson().fromJson(json, type);
        }
    }

    /**
     * Devuelve los nombres de las columnas del archivo original.
     */
    public static List<String> getHeader() { return header; }

    /**
     * Devuelve el mapa de índices de las columnas.
     */
    public static Map<String, Integer> getHeaderMap() { return lastHeaderMap; }
}
