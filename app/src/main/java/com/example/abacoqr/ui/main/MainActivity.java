package com.example.abacoqr.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.abacoqr.R;
import com.example.abacoqr.model.Dispositivo;
import com.example.abacoqr.model.HistorialCambio;
import com.example.abacoqr.ui.dialogs.AddDispositivoDialog;
import com.example.abacoqr.ui.dialogs.EditDispositivoDialog;
import com.example.abacoqr.ui.dialogs.FiltrosQrDialog;
import com.example.abacoqr.ui.scanner.CapturaVertical;
import com.example.abacoqr.ui.search.SearchResultsActivity;
import com.example.abacoqr.utils.PdfManager;
import com.example.abacoqr.viewmodel.InventarioViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Actividad principal refactorizada. 
 * Cumple con Clean UI: delega diálogos y lógica de negocio.
 */
public class MainActivity extends AppCompatActivity {

    private InventarioViewModel viewModel;
    private AlertDialog progressDialog;

    private final ActivityResultLauncher<String[]> selectorDeArchivos = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) viewModel.cargarCsv(uri); }
    );

    private final ActivityResultLauncher<Intent> searchResultsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String sn = result.getData().getStringExtra("SELECTED_SERIAL_NUMBER");
                    if (sn != null) viewModel.buscarPorSerie(sn);
                }
            }
    );

    private final ActivityResultLauncher<String> creadorDePdfs = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> {
                if (uri != null) {
                    List<Dispositivo> data = viewModel.getResultadosPdf().getValue();
                    if (data != null && !data.isEmpty()) guardarPdfFisico(uri, data);
                }
            }
    );

    private final ActivityResultLauncher<ScanOptions> lanzadorEscaner = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) viewModel.buscarPorSerie(result.getContents());
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
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void setupUI() {
        findViewById(R.id.btnCargarCsv).setOnClickListener(v -> selectorDeArchivos.launch(new String[]{"text/csv", "text/comma-separated-values"}));
        findViewById(R.id.btnEscanearQr).setOnClickListener(v -> {
            ScanOptions opt = new ScanOptions();
            opt.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            opt.setPrompt("Apunta al QR del dispositivo");
            opt.setCaptureActivity(CapturaVertical.class);
            lanzadorEscaner.launch(opt);
        });
        findViewById(R.id.btnVerHistorial).setOnClickListener(v -> mostrarHistorial());
        findViewById(R.id.btnIrABusqueda).setOnClickListener(v -> searchResultsLauncher.launch(new Intent(this, SearchResultsActivity.class)));
        findViewById(R.id.btnGenerarPdf).setOnClickListener(v -> new FiltrosQrDialog().show(getSupportFragmentManager(), "filtros_dialog"));
        findViewById(R.id.btnGuardarFinal).setOnClickListener(v -> viewModel.guardarCambios());
        findViewById(R.id.btnExportarCompartir).setOnClickListener(v -> viewModel.exportarParaCompartir(this::compartirArchivo));
        findViewById(R.id.fab_add_dispositivo).setOnClickListener(v -> new AddDispositivoDialog().show(getSupportFragmentManager(), "add_dialog"));
    }

    private void observeViewModel() {
        final TextView tvTotal = findViewById(R.id.tvTotal);
        final TextView tvActivos = findViewById(R.id.tvActivos);
        final TextView tvInoperativos = findViewById(R.id.tvInoperativos);
        final Button btnGenerarPdf = findViewById(R.id.btnGenerarPdf);
        final Button btnVerHistorial = findViewById(R.id.btnVerHistorial);
        final Button btnIrABusqueda = findViewById(R.id.btnIrABusqueda);
        final Button btnGuardarFinal = findViewById(R.id.btnGuardarFinal);
        final Button btnExportar = findViewById(R.id.btnExportarCompartir);
        final FloatingActionButton fabAdd = findViewById(R.id.fab_add_dispositivo);

        viewModel.getHayDatosCargados().observe(this, hay -> {
            boolean isEnabled = hay != null && hay;
            btnVerHistorial.setEnabled(isEnabled);
            btnIrABusqueda.setEnabled(isEnabled);
            btnGenerarPdf.setEnabled(isEnabled);
            btnGuardarFinal.setEnabled(isEnabled);
            btnExportar.setEnabled(isEnabled);
            fabAdd.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
            if (!isEnabled) {
                tvTotal.setText(getString(R.string.total_leidos, 0));
                tvActivos.setText(getString(R.string.filtrados_para_qr, 0));
                tvInoperativos.setText(getString(R.string.descartados, 0));
            }
        });

        viewModel.getEstadisticas().observe(this, s -> {
            if (s == null) return;
            tvTotal.setText(getString(R.string.total_leidos, s.total));
            tvActivos.setText(getString(R.string.filtrados_para_qr_con_estado, "Activo", s.validos));
            tvInoperativos.setText(getString(R.string.descartados, s.descartados));
        });

        viewModel.getMensaje().observe(this, m -> { if (m != null && !m.isEmpty()) Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); });
        viewModel.getEstaCargando().observe(this, cargando -> {
            if (cargando != null && progressDialog != null) {
                if (cargando) progressDialog.show(); else progressDialog.dismiss();
            }
        });

        viewModel.getDispositivoEncontrado().observe(this, d -> {
            if (d != null) EditDispositivoDialog.newInstance(d).show(getSupportFragmentManager(), "edit_dialog");
        });

        viewModel.getResultadosPdf().observe(this, lista -> {
            if (lista != null && !lista.isEmpty()) {
                creadorDePdfs.launch("Etiquetas_QR_Filtrado.pdf"); // ¡CORREGIDO!
            }
        });
    }

    private void guardarPdfFisico(Uri uri, List<Dispositivo> data) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            PdfManager.crearPdfEtiquetas(data, "FILTRADO", os);
            Toast.makeText(this, "¡PDF generado!", Toast.LENGTH_LONG).show();
        } catch (Exception e) { Toast.makeText(this, "Error al guardar.", Toast.LENGTH_SHORT).show(); }
    }

    private void compartirArchivo(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Compartir Inventario"));
    }

    private void mostrarHistorial() {
        List<HistorialCambio> hist = viewModel.getHistorial().getValue();
        if (hist == null || hist.isEmpty()) { Toast.makeText(this, "Vacío.", Toast.LENGTH_SHORT).show(); return; }
        String f = hist.stream().map(HistorialCambio::toString).collect(Collectors.joining("\n\n"));
        new AlertDialog.Builder(this).setTitle("Historial de Sesión").setMessage(f).setPositiveButton("Cerrar", null).show();
    }
}
