package com.example.abacoqr.ui.dialogs;

import android.app.Dialog;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.abacoqr.R;
import com.example.abacoqr.viewmodel.InventarioViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

/**
 * Diálogo modular para añadir nuevos equipos al inventario.
 */
public class AddDispositivoDialog extends DialogFragment {

    private InventarioViewModel viewModel;
    private final String[] opcionesEstado = {"Activo", "Inoperativo", "STOCK", "Prestamo"};

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(InventarioViewModel.class);
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_dispositivo, null);

        final TextInputEditText etSerie = view.findViewById(R.id.et_add_serie);
        final TextInputEditText etArticulo = view.findViewById(R.id.et_add_articulo);
        final Spinner spinnerEstado = view.findViewById(R.id.spinner_add_estado);
        
        final TextInputEditText etMarca = view.findViewById(R.id.et_add_marca);
        final TextInputEditText etModelo = view.findViewById(R.id.et_add_modelo);
        final TextInputEditText etPropietario = view.findViewById(R.id.et_add_propietario);
        final TextInputEditText etSubsede = view.findViewById(R.id.et_add_subsede);
        final TextInputEditText etPabellon = view.findViewById(R.id.et_add_pabellon);
        final TextInputEditText etPlanta = view.findViewById(R.id.et_add_planta);
        final TextInputEditText etEspacio = view.findViewById(R.id.et_add_espacio);
        final TextInputEditText etObservaciones = view.findViewById(R.id.et_add_observaciones);

        spinnerEstado.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opcionesEstado));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Añadir Nuevo Activo")
                .setView(view)
                .setPositiveButton("Crear", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(di -> {
            Button btnCrear = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            // Usar color primario del tema para el botón
            TypedValue typedValuePrimary = new TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValuePrimary, true);
            btnCrear.setTextColor(typedValuePrimary.data);
            btnCrear.setOnClickListener(v -> {
                String sn = etSerie.getText().toString().trim();
                String art = etArticulo.getText().toString().trim();
                
                if (sn.isEmpty() || art.isEmpty()) {
                    Toast.makeText(getContext(), "Nº Serie y Artículo son obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, String> datos = new HashMap<>();
                datos.put("Marca", etMarca.getText().toString());
                datos.put("Modelo", etModelo.getText().toString());
                datos.put("Propietario", etPropietario.getText().toString());
                datos.put("Subsede", etSubsede.getText().toString());
                datos.put("Pabellón", etPabellon.getText().toString());
                datos.put("Planta", etPlanta.getText().toString());
                datos.put("Espacio", etEspacio.getText().toString());
                datos.put("Observaciones", etObservaciones.getText().toString());

                viewModel.crearNuevoDispositivo(datos, sn, art, spinnerEstado.getSelectedItem().toString());
                dismiss();
            });
            
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
