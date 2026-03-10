package com.example.abacoqr.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.abacoqr.R;
import com.example.abacoqr.model.Dispositivo;
import com.example.abacoqr.model.DispositivoDTO;
import com.example.abacoqr.viewmodel.InventarioViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Actividad de búsqueda con soporte para filtros de verificación.
 */
public class SearchResultsActivity extends AppCompatActivity {

    private InventarioViewModel viewModel;
    private SearchResultsAdapter adapter;
    private final List<DispositivoDTO> listaItemsResultados = new ArrayList<>();

    private Spinner spinnerSearchField, spinnerVerif;
    private TextInputEditText etSearchQuery;
    private Button btnSearch;
    private ProgressBar loadingIndicator;
    private TextView tvSummary;

    private final String[] opcionesBusqueda = {"Nº Serie", "Artículo", "Estado", "Subsede", "Propietario"};
    private final String[] opcionesVerif = {"Todos", "Verificados", "No Verificados"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        viewModel = new ViewModelProvider(this).get(InventarioViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar_search_results);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupUI();
        observeViewModel();
    }

    private void setupUI() {
        spinnerSearchField = findViewById(R.id.spinner_search_field_activity);
        spinnerVerif = findViewById(R.id.spinner_search_verif_activity);
        etSearchQuery = findViewById(R.id.et_search_query_activity);
        btnSearch = findViewById(R.id.btn_search_activity);
        loadingIndicator = findViewById(R.id.search_loading_indicator);
        tvSummary = findViewById(R.id.tv_search_summary);

        // Spinner de Campos
        ArrayAdapter<String> adapterB = new ArrayAdapter<>(this, R.layout.spinner_item, opcionesBusqueda);
        adapterB.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchField.setAdapter(adapterB);

        // Spinner de Verificación
        ArrayAdapter<String> adapterV = new ArrayAdapter<>(this, R.layout.spinner_item, opcionesVerif);
        adapterV.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVerif.setAdapter(adapterV);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_search_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SearchResultsAdapter(listaItemsResultados, item -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_SERIAL_NUMBER", item.getNumeroSerie());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String campo = spinnerSearchField.getSelectedItem().toString();
            String query = etSearchQuery.getText().toString();
            int verifMode = spinnerVerif.getSelectedItemPosition(); // 0=Todos, 1=Verif, 2=No Verif
            
            tvSummary.setVisibility(View.GONE);
            // Llamamos a la búsqueda que ahora admite verificación
            viewModel.realizarBusquedaConVerif(campo, query, verifMode);
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(this, resultados -> {
            listaItemsResultados.clear();
            if (resultados != null && !resultados.isEmpty()) {
                for (Dispositivo d : resultados) {
                    listaItemsResultados.add(new DispositivoDTO(
                        d.getPropietario(), d.getSubsede(), d.getPabellon(), d.getPlanta(),
                        d.getEspacio(), d.getMarca(), d.getModelo(), d.getNumeroSerie(), d.getEstado()
                    ));
                }
                tvSummary.setText("Se han encontrado " + resultados.size() + " coincidencias");
                tvSummary.setVisibility(View.VISIBLE);
            } else {
                tvSummary.setVisibility(View.GONE);
                Toast.makeText(this, "No se encontraron resultados.", Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();
        });

        viewModel.getEstaCargando().observe(this, cargando -> {
            if (cargando != null) {
                loadingIndicator.setVisibility(cargando ? View.VISIBLE : View.GONE);
                btnSearch.setEnabled(!cargando);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
