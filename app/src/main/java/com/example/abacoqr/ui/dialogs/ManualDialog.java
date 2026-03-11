package com.example.abacoqr.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.abacoqr.R;

/**
 * Diálogo modular que muestra el manual de usuario en formato HTML.
 */
public class ManualDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manual, null);
        WebView webView = view.findViewById(R.id.webview_manual);
        
        // Cargamos el manual desde la carpeta assets
        webView.loadUrl("file:///android_asset/manual.html");

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("Entendido", null)
                .create();
    }
}
