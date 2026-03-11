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
 * Implementa una lectura eficiente mediante streaming y un parser seguro
 * para soportar comas dentro de los campos de texto.
 */
public class CsvReader {

    private static List<String> header;
    private static Map<String, Integer> lastHeaderMap;

    private static final String PREFS_NAME = "InventarioPrefs";
    private static final String KEY_HEADER_MAP = "last_header_map";

    public interface OnDeviceParsedListener {
        void onDeviceParsed(Dispositivo dispositivo);
    }

    public static void leerYProcesar(Context context, Uri uri, OnDeviceParsedListener listener) {
        header = new ArrayList<>();
        Map<String, Integer> headerMap = new HashMap<>();

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            // LEER LA CABECERA (Primera línea)
            String headerLine = reader.readLine();
            if (headerLine == null) return;

            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            // CORRECCIÓN: Usamos el parser seguro en lugar de split(",")
            List<String> rawHeaders = parsearLineaCsv(headerLine);
            boolean esCabeceraReal = false;

            for (int i = 0; i < rawHeaders.size(); i++) {
                String cleanHeader = rawHeaders.get(i).trim();
                header.add(cleanHeader);

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
            guardarHeaderMapEnPrefs(context, headerMap);

            if (!esCabeceraReal) {
                listener.onDeviceParsed(new Dispositivo(rawHeaders, headerMap));
            }

            // LEER EL RESTO DE FILAS (Procesamiento por streaming)
            String line;
            while ((line = reader.readLine()) != null) {
                // CORRECCIÓN: Usamos el parser seguro para evitar que las comas en el texto rompan las columnas
                List<String> cleanRowData = parsearLineaCsv(line);
                listener.onDeviceParsed(new Dispositivo(cleanRowData, headerMap));
            }
        } catch (Exception e) {
            Log.e("CsvReader", "Error leyendo CSV: " + e.getMessage());
        }
    }

    /**
     * Nuevo método: Lee una línea CSV respetando las comillas dobles.
     * Si encuentra comas dentro de unas comillas, NO las corta.
     */
    private static List<String> parsearLineaCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                inQuotes = !inQuotes; // Entramos o salimos de una zona protegida por comillas
            } else if (c == ',' && !inQuotes) {
                // Solo cortamos si hay una coma Y NO estamos dentro de unas comillas
                result.add(currentToken.toString().trim());
                currentToken.setLength(0);
            } else {
                currentToken.append(c);
            }
        }
        // Añadimos la última columna
        result.add(currentToken.toString().trim());
        return result;
    }

    private static void guardarHeaderMapEnPrefs(Context context, Map<String, Integer> map) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(map);
        prefs.edit().putString(KEY_HEADER_MAP, json).apply();
    }

    public static void cargarHeaderMapDesdePrefs(Context context) {
        if (lastHeaderMap != null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HEADER_MAP, null);
        if (json != null) {
            Type type = new com.google.gson.reflect.TypeToken<Map<String, Integer>>(){}.getType();
            lastHeaderMap = new Gson().fromJson(json, type);
        }
    }

    public static List<String> getHeader() { return header; }

    public static Map<String, Integer> getHeaderMap() { return lastHeaderMap; }
}