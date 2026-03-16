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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.abacoqr.R;
import com.example.abacoqr.model.Dispositivo;
import com.example.abacoqr.viewmodel.InventarioViewModel;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Diálogo modular para editar o verificar un equipo existente.
 * Refactorizado: Ya no conoce la capa de persistencia (Entities).
 */
public class EditDispositivoDialog extends DialogFragment {

    private static final String ARG_DISPOSITIVO = "arg_dispositivo";
    private InventarioViewModel viewModel;
    private final String[] opcionesEstado = {"Activo", "Inoperativo", "STOCK", "Prestamo"};

    public static EditDispositivoDialog newInstance(Dispositivo dispositivo) {
        EditDispositivoDialog fragment = new EditDispositivoDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DISPOSITIVO, dispositivo);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(InventarioViewModel.class);
        Dispositivo dispositivo = (Dispositivo) getArguments().getSerializable(ARG_DISPOSITIVO);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_dispositivo, null);

        final Button btnQuickVerify = view.findViewById(R.id.btn_quick_verify);
        final TextInputEditText etCentro = view.findViewById(R.id.et_edit_centro);
        final TextInputEditText etSubsede = view.findViewById(R.id.et_edit_subsede);
        final TextInputEditText etPabellon = view.findViewById(R.id.et_edit_pabellon);
        final TextInputEditText etPlanta = view.findViewById(R.id.et_edit_planta);
        final TextInputEditText etAula = view.findViewById(R.id.et_edit_aula);
        TextView tvDatos = view.findViewById(R.id.tv_datos_detalle);
        final Spinner spinnerEstado = view.findViewById(R.id.spinner_estado_dialogo);
        final TextInputEditText etObs = view.findViewById(R.id.et_observaciones);

        // Poblar datos iniciales
        if (dispositivo != null) {
            etCentro.setText(dispositivo.getPropietario());
            etSubsede.setText(dispositivo.getSubsede());
            etPabellon.setText(dispositivo.getPabellon());
            etPlanta.setText(dispositivo.getPlanta());
            etAula.setText(dispositivo.getEspacio());
            tvDatos.setText("Marca: " + dispositivo.getMarca() + "\nModelo: " + dispositivo.getModelo() + "\nN/S: " + dispositivo.getNumeroSerie());
            etObs.setText(dispositivo.getObservaciones());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opcionesEstado);
            spinnerEstado.setAdapter(adapter);
            spinnerEstado.setSelection(Math.max(0, adapter.getPosition(dispositivo.getEstado())));
        }

        final String estAnt = dispositivo != null ? dispositivo.getEstado() : "";
        final String obsAnt = dispositivo != null ? dispositivo.getObservaciones() : "";

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("✅ Detalle del Equipo")
                .setView(view)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                actualizarYGuardar(dispositivo, etCentro, etSubsede, etPabellon, etPlanta, etAula, spinnerEstado, etObs, estAnt, obsAnt, false);
                dismiss();
            });
            btnQuickVerify.setOnClickListener(v -> {
                actualizarYGuardar(dispositivo, etCentro, etSubsede, etPabellon, etPlanta, etAula, spinnerEstado, etObs, estAnt, obsAnt, true);
                Toast.makeText(getContext(), "Equipo verificado con éxito", Toast.LENGTH_SHORT).show();
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

    /**
     * MEJORA: Actualiza el modelo de dominio y lo pasa al ViewModel.
     * La UI ya no crea ni maneja Entities.
     */
    private void actualizarYGuardar(Dispositivo d, TextInputEditText c, TextInputEditText s, TextInputEditText p, 
                                   TextInputEditText pl, TextInputEditText a, Spinner st, 
                                   TextInputEditText o, String ea, String oa, boolean verif) {
        if (d == null) return;

        // Actualizamos los datos en el objeto de negocio
        d.setPropietario(c.getText().toString().trim());
        d.setSubsede(s.getText().toString().trim());
        d.setPabellon(p.getText().toString().trim());
        d.setPlanta(pl.getText().toString().trim());
        d.setEspacio(a.getText().toString().trim());
        d.setEstado(st.getSelectedItem().toString());
        d.setObservaciones(o.getText().toString().trim());
        
        // El ViewModel se encarga de la persistencia
        viewModel.actualizarDispositivoCompleto(d, ea, oa, verif);
    }
}
