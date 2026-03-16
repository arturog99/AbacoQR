package com.example.abacoqr.ui.dialogs;

import android.app.Dialog;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.abacoqr.R;
import com.example.abacoqr.viewmodel.InventarioViewModel;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Diálogo modular para configurar los filtros de generación de etiquetas QR.
 */
public class FiltrosQrDialog extends DialogFragment {

    private InventarioViewModel viewModel;
    private final String[] opcionesEstado = {"Activo", "Inoperativo", "STOCK", "Prestamo"};
    private final String[] opcionesVerif = {"Todos", "Verificados", "No Verificados"};

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(InventarioViewModel.class);
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_filtros_pdf, null);

        final Spinner spinnerVerif = view.findViewById(R.id.spinner_filtro_verificacion);
        final Spinner spinnerEstado = view.findViewById(R.id.spinner_filtro_estado);
        final TextInputEditText etCentro = view.findViewById(R.id.et_filtro_centro);
        final TextInputEditText etArticulo = view.findViewById(R.id.et_filtro_articulo);
        final TextInputEditText etPlanta = view.findViewById(R.id.et_filtro_planta);
        final TextInputEditText etAula = view.findViewById(R.id.et_filtro_aula);

        spinnerVerif.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opcionesVerif));
        spinnerEstado.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opcionesEstado));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Generar Etiquetas QR")
                .setView(view)
                .setPositiveButton("Generar", (d, which) -> {
                    viewModel.filtrarParaPdf(
                        spinnerEstado.getSelectedItem().toString(),
                        etCentro.getText().toString(),
                        etArticulo.getText().toString(),
                        etPlanta.getText().toString(),
                        etAula.getText().toString(),
                        spinnerVerif.getSelectedItemPosition()
                    );
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(di -> {
            if (dialog.getWindow() != null) {
                // Usar color de fondo del tema
                TypedValue typedValue = new TypedValue();
                requireContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(typedValue.data));
            }
        });

        return dialog;
    }
}
