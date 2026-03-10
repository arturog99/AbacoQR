package com.example.abacoqr.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.example.abacoqr.model.Dispositivo;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.OutputStream;
import java.util.List;

/**
 * Clase de utilidad encargada de la generación técnica de documentos PDF.
 * Gestiona la creación de Bitmaps de códigos QR y su posicionamiento en un lienzo (Canvas)
 * para crear hojas de etiquetas listas para imprimir con paginación automática.
 */
public class PdfManager {

    /**
     * Genera una imagen Bitmap que contiene un código QR a partir de un texto.
     * 
     * @param texto El contenido que se codificará en el QR (generalmente el Número de Serie).
     * @return Un Bitmap con el código QR generado o null si ocurre un error.
     */
    private static Bitmap generarQR(String texto) {
        try {
            // Configuramos la codificación del QR con formato ZXing
            BitMatrix bitMatrix = new MultiFormatWriter().encode(texto, BarcodeFormat.QR_CODE, 200, 200);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            
            // Creamos el Bitmap en formato ARGB_8888 para mayor calidad de impresión
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Crea un documento PDF con etiquetas QR organizadas en cuadrícula.
     * Implementa lógica de paginación automática: cuando una página se llena, se crea una nueva.
     * 
     * @param inventario Lista de dispositivos ya filtrados que se deben imprimir.
     * @param titulo Título o identificador del proceso de generación.
     * @param outputStream Flujo de salida donde se escribirá el archivo PDF físicamente.
     */
    public static void crearPdfEtiquetas(List<Dispositivo> inventario, String titulo, OutputStream outputStream) {
        if (inventario == null || inventario.isEmpty()) return;

        PdfDocument document = new PdfDocument();
        // Definimos el tamaño de página A4 estándar (595x842 puntos)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Configuración del estilo de texto (Fuente, Color, Suavizado)
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12f);
        textPaint.setAntiAlias(true);

        // Coordenadas iniciales para la primera etiqueta
        int x = 50, y = 50;

        for (Dispositivo d : inventario) {
            // Generamos el QR único para este dispositivo
            Bitmap qrBitmap = generarQR(d.getNumeroSerie());

            if (qrBitmap != null) {
                // Dibujamos el QR y la información de texto acompañante
                canvas.drawBitmap(qrBitmap, x, y, null);
                canvas.drawText("N/S: " + d.getNumeroSerie(), x + 5, y + 215, textPaint);
                
                // Formateamos la descripción (Marca - Modelo) acortándola si es necesario
                String desc = d.getMarca() + " - " + d.getModelo();
                if (desc.length() > 30) desc = desc.substring(0, 27) + "...";
                canvas.drawText(desc, x + 5, y + 230, textPaint);

                // Lógica de cuadrícula: nos movemos a la derecha o saltamos de fila
                x += 250;
                if (x > 300) {
                    x = 50;
                    y += 270;
                }
                
                // Lógica de Paginación: si llegamos al límite vertical, saltamos de página
                if (y > 700) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    x = 50; y = 50;
                }
            }
        }

        // Finalizamos el documento y lo volcamos al archivo
        document.finishPage(page);
        try {
            document.writeTo(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }
}
