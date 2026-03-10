package com.example.abacoqr.ui.scanner;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * Clase controladora para la actividad de captura del escáner.
 * Se hereda de CaptureActivity (ZXing) para permitir personalizar el comportamiento
 * del escaneo, específicamente forzando la orientación vertical de la cámara
 * para una mejor experiencia de usuario en dispositivos móviles.
 */
public class CapturaVertical extends CaptureActivity {
    // Esta clase se mantiene vacía ya que su propósito principal es servir como 
    // punto de referencia en el AndroidManifest para definir la orientación de la cámara.
}
