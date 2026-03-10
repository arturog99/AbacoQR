package com.example.abacoqr.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.abacoqr.R;
import com.example.abacoqr.data.repository.InventarioRepository;
import com.example.abacoqr.data.local.entities.DispositivoEntity;
import com.example.abacoqr.model.HistorialCambio;
import com.example.abacoqr.ui.search.SearchResultsActivity;
import com.example.abacoqr.ui.scanner.CapturaVertical;
import com.example.abacoqr.viewmodel.InventarioViewModel;
import com.example.abacoqr.utils.PdfManager;
import com.example.abacoqr.model.Dispositivo;
import com.example.abacoqr.data.parser.CsvReader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Actividad principal que coordina el inventario y la función de añadir equipos.
 */
public class MainActivity extends AppCompatActivity {

    private InventarioViewModel viewModel;
    private String estadoObjetivo = "Activo"; 
    private List<Dispositivo> listaParaPdf = new ArrayList<>();

    private TextView tvTotal, tvActivos, tvInoperativos;
    private Button btnGenerarPdf, btnVerHistorial, btnIrABusqueda, btnCargarCsv, btnEscanearQr, btnGuardarFinal;
    private FloatingActionButton fabAdd;
    private AlertDialog progressDialog;

    private final String[] opcionesEstado = {"Activo", "Inoperativo", "STOCK", "Prestamo"};
    private final String[] opcionesVerif = {"Todos", "Verificados", "No Verificados"};

    private final ActivityResultLauncher<String[]> selectorDeArchivos = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) viewModel.cargarCsv(uri); }
    );

    private final ActivityResultLauncher<Intent> searchResultsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String sn = result.getData().getStringExtra("SELECTED_SERIAL_NUMBER");
                    if (sn != null) buscarYMostrarDispositivo(sn);
                }
            }
    );

    private final ActivityResultLauncher<String> creadorDePdfs = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> { if (uri != null && !listaParaPdf.isEmpty()) guardarPdfFisico(uri); }
    );

    private final ActivityResultLauncher<ScanOptions> lanzadorEscaner = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) buscarYMostrarDispositivo(result.getContents());
                else Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        viewModel = new ViewModelProvider(this).get(InventarioViewModel.class);
        String user = getIntent().getStringExtra("USERNAME");
        if (user != null) viewModel.setUsuario(user);
        setupUI();
        createProgressDialog();
        observeViewModel();
    }

    private void createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.layout_loading_dialog);
        progressDialog = builder.create();
        if (progressDialog.getWindow() != null) progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void setupUI() {
        btnGenerarPdf = findViewById(R.id.btnGenerarPdf);
        btnVerHistorial = findViewById(R.id.btnVerHistorial);
        btnIrABusqueda = findViewById(R.id.btnIrABusqueda);
        btnCargarCsv = findViewById(R.id.btnCargarCsv);
        btnEscanearQr = findViewById(R.id.btnEscanearQr);
        btnGuardarFinal = findViewById(R.id.btnGuardarFinal);
        fabAdd = findViewById(R.id.fab_add_dispositivo);
        
        tvTotal = findViewById(R.id.tvTotal);
        tvActivos = findViewById(R.id.tvActivos);
        tvInoperativos = findViewById(R.id.tvInoperativos);

        btnCargarCsv.setOnClickListener(v -> selectorDeArchivos.launch(new String[]{"text/csv", "text/comma-separated-values"}));
        btnEscanearQr.setOnClickListener(v -> {
            ScanOptions opt = new ScanOptions();
            opt.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            opt.setPrompt("Apunta al QR del dispositivo");
            opt.setCaptureActivity(CapturaVertical.class);
            lanzadorEscaner.launch(opt);
        });
        btnVerHistorial.setOnClickListener(v -> mostrarHistorial());
        btnIrABusqueda.setOnClickListener(v -> searchResultsLauncher.launch(new Intent(this, SearchResultsActivity.class)));
        btnGenerarPdf.setOnClickListener(v -> mostrarDialogoFiltrosPdf());
        btnGuardarFinal.setOnClickListener(v -> viewModel.guardarCambios());
        
        fabAdd.setOnClickListener(v -> mostrarDialogoAnadirDispositivo());
    }

    private void observeViewModel() {
        viewModel.getHayDatosCargados().observe(this, hay -> {
            if (hay != null && hay) {
                viewModel.actualizarEstadisticas(estadoObjetivo);
                btnVerHistorial.setEnabled(true);
                btnIrABusqueda.setEnabled(true);
                btnGenerarPdf.setEnabled(true);
                btnGuardarFinal.setEnabled(true);
                fabAdd.setVisibility(View.VISIBLE);
            } else {
                btnVerHistorial.setEnabled(false); btnIrABusqueda.setEnabled(false);
                btnGenerarPdf.setEnabled(false); btnGuardarFinal.setEnabled(false);
                fabAdd.setVisibility(View.GONE);
                tvTotal.setText("Total leídos: 0"); tvActivos.setText("✅ Filtrados para QR: 0"); tvInoperativos.setText("❌ Descartados: 0");
            }
        });

        viewModel.getEstadisticas().observe(this, s -> {
            tvTotal.setText("Total leídos: " + s.total);
            tvActivos.setText("✅ Filtrados para QR (" + estadoObjetivo + "): " + s.validos);
            tvInoperativos.setText("❌ Descartados: " + s.descartados);
        });

        viewModel.getMensaje().observe(this, m -> { if (m != null && !m.isEmpty()) Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); });

        viewModel.getEstaCargando().observe(this, cargando -> {
            if (cargando != null) {
                if (cargando) progressDialog.show(); else progressDialog.dismiss();
                btnCargarCsv.setEnabled(!cargando); btnEscanearQr.setEnabled(!cargando);
                btnGuardarFinal.setEnabled(!cargando && viewModel.getHayDatosCargados().getValue());
            }
        });
    }

    private void mostrarDialogoAnadirDispositivo() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_dispositivo, null);

        final TextInputEditText etSerie = view.findViewById(R.id.et_add_serie);
        final TextInputEditText etArticulo = view.findViewById(R.id.et_add_articulo);
        final Spinner spinnerEstadoAdd = view.findViewById(R.id.spinner_add_estado);
        
        final TextInputEditText etInventario = view.findViewById(R.id.et_add_inventario);
        final TextInputEditText etExpediente = view.findViewById(R.id.et_add_expediente);
        final TextInputEditText etMarca = view.findViewById(R.id.et_add_marca);
        final TextInputEditText etModelo = view.findViewById(R.id.et_add_modelo);
        final TextInputEditText etPropietario = view.findViewById(R.id.et_add_propietario);
        final TextInputEditText etSubsede = view.findViewById(R.id.et_add_subsede);
        final TextInputEditText etPabellon = view.findViewById(R.id.et_add_pabellon);
        final TextInputEditText etPlanta = view.findViewById(R.id.et_add_planta);
        final TextInputEditText etEspacio = view.findViewById(R.id.et_add_espacio);
        final TextInputEditText etObservaciones = view.findViewById(R.id.et_add_observaciones);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcionesEstado);
        spinnerEstadoAdd.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo Activo")
                .setView(view)
                .setPositiveButton("Crear", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String sn = etSerie.getText().toString().trim();
                String art = etArticulo.getText().toString().trim();
                String est = spinnerEstadoAdd.getSelectedItem().toString();
                
                if (sn.isEmpty() || art.isEmpty()) {
                    Toast.makeText(this, "Nº Serie y Artículo son obligatorios.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, String> datos = new HashMap<>();
                datos.put("Inventario", etInventario.getText().toString().trim());
                datos.put("Expediente", etExpediente.getText().toString().trim());
                datos.put("Marca", etMarca.getText().toString().trim());
                datos.put("Modelo", etModelo.getText().toString().trim());
                datos.put("Propietario", etPropietario.getText().toString().trim());
                datos.put("Subsede", etSubsede.getText().toString().trim());
                datos.put("Pabellón", etPabellon.getText().toString().trim());
                datos.put("Planta", etPlanta.getText().toString().trim());
                datos.put("Espacio", etEspacio.getText().toString().trim());
                datos.put("Observaciones", etObservaciones.getText().toString().trim());

                viewModel.crearNuevoDispositivo(datos, sn, art, est);
                dialog.dismiss();
            });
            
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FAF4F4")));
        });

        dialog.show();
    }

    private void mostrarDialogoFiltrosPdf() {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_filtros_pdf, null);
        final Spinner spinnerVerif = view.findViewById(R.id.spinner_filtro_verificacion);
        final Spinner spinnerEstadoF = view.findViewById(R.id.spinner_filtro_estado);
        final TextInputEditText etCentro = view.findViewById(R.id.et_filtro_centro);
        final TextInputEditText etArticulo = view.findViewById(R.id.et_filtro_articulo);
        final TextInputEditText etPlanta = view.findViewById(R.id.et_filtro_planta);
        final TextInputEditText etAula = view.findViewById(R.id.et_filtro_aula);

        ArrayAdapter<String> adapterV = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcionesVerif);
        spinnerVerif.setAdapter(adapterV);
        ArrayAdapter<String> adapterE = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcionesEstado);
        spinnerEstadoF.setAdapter(adapterE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Generar Etiquetas QR")
                .setView(view)
                .setPositiveButton("Generar", (d, which) -> {
                    int verifMode = spinnerVerif.getSelectedItemPosition(); 
                    String est = spinnerEstadoF.getSelectedItem().toString();
                    String prop = etCentro.getText().toString();
                    String art = etArticulo.getText().toString();
                    String pla = etPlanta.getText().toString();
                    String esp = etAula.getText().toString();
                    
                    Calendar cal = Calendar.getInstance(); cal.set(Calendar.DAY_OF_MONTH, 1);
                    String inicioMes = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
                    
                    prepararListaYGenerarPdf(est, prop, art, "", pla, esp, verifMode, inicioMes);
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(di -> {
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FAF4F4")));
        });
        dialog.show();
    }

    private void prepararListaYGenerarPdf(String est, String prop, String art, String pab, String pla, String esp, int verif, String fechaCorte) {
        progressDialog.show();
        InventarioRepository repo = new InventarioRepository(this);
        // ¡CORREGIDO! Pasamos los 11 parámetros que requiere ahora el repositorio
        repo.buscarConFiltrosCampana("", est, prop, art, "", pab, pla, esp, verif, fechaCorte, new InventarioRepository.Callback<List<DispositivoEntity>>() {
            @Override public void onComplete(List<DispositivoEntity> resultados) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (resultados == null || resultados.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No hay equipos.", Toast.LENGTH_LONG).show();
                    } else {
                        listaParaPdf = resultados.stream().map(e -> {
                            List<String> rowData = new ArrayList<>(e.fullRowData);
                            Dispositivo d = new Dispositivo(rowData, CsvReader.getHeaderMap());
                            d.forceSetDataFull(e.propietario, e.subsede, e.pabellon, e.planta, e.espacio, e.marca, e.modelo, e.numeroSerie, e.estado, e.articulo, e.observaciones, e.verificadoCau);
                            return d;
                        }).collect(Collectors.toList());
                        creadorDePdfs.launch("Etiquetas_QR_Filtrado.pdf");
                    }
                });
            }
            @Override public void onError(Exception e) { runOnUiThread(() -> { progressDialog.dismiss(); Toast.makeText(MainActivity.this, "Error.", Toast.LENGTH_SHORT).show(); }); }
        });
    }

    private void guardarPdfFisico(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            PdfManager.crearPdfEtiquetas(listaParaPdf, "FILTRADO", os);
            Toast.makeText(this, "¡PDF generado!", Toast.LENGTH_LONG).show();
        } catch (Exception e) { Toast.makeText(this, "Error.", Toast.LENGTH_SHORT).show(); }
    }

    private void buscarYMostrarDispositivo(String sn) {
        InventarioRepository repo = new InventarioRepository(this);
        repo.obtenerDispositivoPorSerie(sn, new InventarioRepository.Callback<DispositivoEntity>() {
            @Override public void onComplete(DispositivoEntity e) {
                runOnUiThread(() -> { if (e != null) mostrarDialogoEdicion(e); else Toast.makeText(MainActivity.this, "No encontrado.", Toast.LENGTH_SHORT).show(); });
            }
            @Override public void onError(Exception e) { runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error.", Toast.LENGTH_SHORT).show()); }
        });
    }

    private void mostrarDialogoEdicion(DispositivoEntity dispositivo) {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_dispositivo, null);
        final Button btnQuickVerify = view.findViewById(R.id.btn_quick_verify);
        final TextInputEditText etCentro = view.findViewById(R.id.et_edit_centro);
        final TextInputEditText etSubsede = view.findViewById(R.id.et_edit_subsede);
        final TextInputEditText etPabellon = view.findViewById(R.id.et_edit_pabellon);
        final TextInputEditText etPlanta = view.findViewById(R.id.et_edit_planta);
        final TextInputEditText etAula = view.findViewById(R.id.et_edit_aula);
        TextView tvDatos = view.findViewById(R.id.tv_datos_detalle);
        final Spinner spinnerDialogo = view.findViewById(R.id.spinner_estado_dialogo);
        final TextInputEditText etObs = view.findViewById(R.id.et_observaciones);

        etCentro.setText(dispositivo.propietario); etSubsede.setText(dispositivo.subsede); etPabellon.setText(dispositivo.pabellon);
        etPlanta.setText(dispositivo.planta); etAula.setText(dispositivo.espacio);
        tvDatos.setText("Marca: " + dispositivo.marca + "\nModelo: " + dispositivo.modelo + "\nN/S: " + dispositivo.numeroSerie);
        etObs.setText(dispositivo.observaciones);

        ArrayAdapter<String> adp = new ArrayAdapter<>(this, R.layout.dialog_spinner_item, opcionesEstado);
        spinnerDialogo.setAdapter(adp);
        spinnerDialogo.setSelection(Math.max(0, adp.getPosition(dispositivo.estado)));

        String estAnt = dispositivo.estado; String obsAnt = dispositivo.observaciones;





        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("✅ Detalle del Equipo").setView(view).setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create();

        dialog.setOnShowListener(di -> {
            // BOTÓN GUARDAR (POSITIVO)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // 1. Asignar valores de los campos de texto a la entidad
                dispositivo.propietario = etCentro.getText().toString();
                dispositivo.subsede = etSubsede.getText().toString();
                dispositivo.pabellon = etPabellon.getText().toString();
                dispositivo.planta = etPlanta.getText().toString();
                dispositivo.espacio = etAula.getText().toString();
                dispositivo.estado = spinnerDialogo.getSelectedItem().toString();
                dispositivo.observaciones = TextUtils.isEmpty(etObs.getText()) ? "" : etObs.getText().toString();


                Dispositivo dTemp = new Dispositivo(new ArrayList<>(dispositivo.fullRowData), CsvReader.getHeaderMap());
                dTemp.setEstado(dispositivo.estado);
                dTemp.setObservaciones(dispositivo.observaciones);
                dTemp.setPropietario(dispositivo.propietario);
                dTemp.setSubsede(dispositivo.subsede);
                dTemp.setPabellon(dispositivo.pabellon);
                dTemp.setPlanta(dispositivo.planta);
                dTemp.setEspacio(dispositivo.espacio);

                // Guardamos la lista actualizada de vuelta en la entidad
                dispositivo.fullRowData = dTemp.getFullRowData();

                // 3. Mandar al ViewModel (con el flag isQuickVerify en false)
                viewModel.actualizarDispositivoCompleto(dispositivo, estAnt, obsAnt, false);
                dialog.dismiss();
            });

            // BOTÓN VERIFICACIÓN RÁPIDA (QUICK VERIFY)
            btnQuickVerify.setOnClickListener(v -> {
                // En este caso, el ViewModel debería encargarse de poner la fecha de hoy
                // tanto en el campo verificadoCau como en la lista fullRowData
                viewModel.actualizarDispositivoCompleto(dispositivo, estAnt, obsAnt, true);
                dialog.dismiss();
                Toast.makeText(this, "Equipo verificado correctamente.", Toast.LENGTH_SHORT).show();
            });

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FAF4F4")));
            }
        });
    }

    private void mostrarHistorial() {
        List<HistorialCambio> hist = viewModel.getHistorial().getValue();
        if (hist == null || hist.isEmpty()) { Toast.makeText(this, "Vacío.", Toast.LENGTH_SHORT).show(); return; }
        String f = hist.stream().map(HistorialCambio::toString).collect(Collectors.joining("\n\n"));
        new AlertDialog.Builder(this).setTitle("Historial").setMessage(f).setPositiveButton("Cerrar", null).show();
    }
}
