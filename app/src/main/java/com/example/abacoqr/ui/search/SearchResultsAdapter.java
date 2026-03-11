package com.example.abacoqr.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.abacoqr.R;
import com.example.abacoqr.model.Dispositivo;
import java.util.List;

/**
 * Adaptador refactorizado. 
 * Recibe directamente modelos de dominio 'Dispositivo', eliminando la necesidad de DTOs en la UI.
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private final List<Dispositivo> resultados;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Dispositivo item);
    }

    public SearchResultsAdapter(List<Dispositivo> resultados, OnItemClickListener listener) {
        this.resultados = resultados;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Dispositivo item = resultados.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return resultados.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEquipo;
        private final TextView tvUbicacion;
        private final TextView tvEstado;

        ViewHolder(View itemView) {
            super(itemView);
            tvEquipo = itemView.findViewById(R.id.tv_item_equipo);
            tvUbicacion = itemView.findViewById(R.id.tv_item_ubicacion_completa);
            tvEstado = itemView.findViewById(R.id.tv_item_estado_val);
        }

        void bind(final Dispositivo item, final OnItemClickListener listener) {
            // Datos del equipo directamente desde el modelo de dominio
            String equipo = item.getMarca() + " " + item.getModelo() + "\nN/S: " + item.getNumeroSerie();
            tvEquipo.setText(equipo);

            // Ubicación detallada
            String ubicacion = "Centro: " + item.getPropietario() + "\n" +
                               "Subsede: " + item.getSubsede() + "\n" +
                               "Pabellón: " + item.getPabellon() + " | Planta: " + item.getPlanta() + "\n" +
                               "Aula: " + item.getEspacio();
            tvUbicacion.setText(ubicacion);

            tvEstado.setText("ESTADO: " + item.getEstado());
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
