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
 * Optimizada para no saturar la memoria RAM con Bitmaps.
 */
public class PdfManager {

    private static Bitmap generarQR(String texto) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(texto, BarcodeFormat.QR_CODE, 200, 200);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();

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

    public static void crearPdfEtiquetas(List<Dispositivo> inventario, String titulo, OutputStream outputStream) {
        if (inventario == null || inventario.isEmpty()) return;

        PdfDocument document = new PdfDocument();
        int pageNumber = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12f);
        textPaint.setAntiAlias(true);

        int x = 50, y = 50;

        for (int i = 0; i < inventario.size(); i++) {
            Dispositivo d = inventario.get(i);
            Bitmap qrBitmap = generarQR(d.getNumeroSerie());

            if (qrBitmap != null) {
                canvas.drawBitmap(qrBitmap, x, y, null);

                // LIBERAMOS RAM INMEDIATAMENTE: vital para PDFs largos
                qrBitmap.recycle();

                canvas.drawText("N/S: " + d.getNumeroSerie(), x + 5, y + 215, textPaint);

                // Protegemos contra nulos
                String marca = d.getMarca() != null ? d.getMarca() : "";
                String modelo = d.getModelo() != null ? d.getModelo() : "";
                String desc = marca + " - " + modelo;

                if (desc.length() > 30) desc = desc.substring(0, 27) + "...";
                canvas.drawText(desc, x + 5, y + 230, textPaint);

                x += 250;
                if (x > 300) {
                    x = 50;
                    y += 270;
                }

                // Paginación: Solo creamos página nueva si AÚN QUEDAN elementos por dibujar
                if (y > 700 && i < inventario.size() - 1) {
                    document.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    x = 50; y = 50;
                }
            }
        }

        // Cerramos la última página activa
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