package com.example.abacoqr.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.example.abacoqr.viewmodel.InventarioViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Actividad de búsqueda optimizada. 
 * Refactorizada para usar directamente modelos de dominio 'Dispositivo'.
 */
public class SearchResultsActivity extends AppCompatActivity {

    private InventarioViewModel viewModel;
    private SearchResultsAdapter adapter;
    private final List<Dispositivo> listaItemsResultados = new ArrayList<>();

    private Spinner spinnerSearchField, spinnerVerif;
    private TextInputEditText etSearchQuery;
    private ProgressBar loadingIndicator;
    private TextView tvSummary;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

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
        }

        setupUI();
        setupSearchLogic();
        observeViewModel();
    }

    private void setupUI() {
        spinnerSearchField = findViewById(R.id.spinner_search_field_activity);
        spinnerVerif = findViewById(R.id.spinner_search_verif_activity);
        etSearchQuery = findViewById(R.id.et_search_query_activity);
        loadingIndicator = findViewById(R.id.search_loading_indicator);
        tvSummary = findViewById(R.id.tv_search_summary);

        ArrayAdapter<String> adapterSearchField = new ArrayAdapter<>(this, R.layout.spinner_item, opcionesBusqueda);
        adapterSearchField.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSearchField.setAdapter(adapterSearchField);
        
        ArrayAdapter<String> adapterVerif = new ArrayAdapter<>(this, R.layout.spinner_item, opcionesVerif);
        adapterVerif.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerVerif.setAdapter(adapterVerif);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_search_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // El adaptador ahora trabaja directamente con objetos Dispositivo
        adapter = new SearchResultsAdapter(listaItemsResultados, item -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_SERIAL_NUMBER", item.getNumeroSerie());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchLogic() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { scheduleSearch(); }
        });

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { scheduleSearch(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spinnerSearchField.setOnItemSelectedListener(listener);
        spinnerVerif.setOnItemSelectedListener(listener);
    }

    private void scheduleSearch() {
        searchHandler.removeCallbacks(searchRunnable);
        searchRunnable = () -> {
            String campo = spinnerSearchField.getSelectedItem().toString();
            String query = etSearchQuery.getText().toString();
            int verifMode = spinnerVerif.getSelectedItemPosition();
            viewModel.realizarBusquedaConVerif(campo, query, verifMode);
        };
        searchHandler.postDelayed(searchRunnable, 400);
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(this, resultados -> {
            listaItemsResultados.clear();
            if (resultados != null && !resultados.isEmpty()) {
                // MEJORA: Ya no necesitamos el bucle manual para mapear a DTO
                listaItemsResultados.addAll(resultados);
                tvSummary.setText("Coincidencias: " + resultados.size());
                tvSummary.setVisibility(View.VISIBLE);
            } else {
                tvSummary.setVisibility(View.GONE);
            }
            adapter.notifyDataSetChanged();
        });

        viewModel.getEstaCargando().observe(this, cargando -> {
            if (cargando != null) loadingIndicator.setVisibility(cargando ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
