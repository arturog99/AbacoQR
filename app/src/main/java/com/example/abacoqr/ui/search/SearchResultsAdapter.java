package com.example.abacoqr.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.abacoqr.R;
import com.example.abacoqr.model.DispositivoDTO;
import java.util.List;

/**
 * Adaptador encargado de gestionar y mostrar la lista de resultados de búsqueda.
 * Utiliza el patrón ViewHolder para el reciclaje eficiente de vistas, permitiendo
 * navegar fluidamente por miles de registros.
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private final List<DispositivoDTO> resultados;
    private final OnItemClickListener listener;

    /**
     * Interfaz para manejar los clics en los elementos de la lista.
     */
    public interface OnItemClickListener {
        /** Se dispara cuando el usuario selecciona un equipo de la lista */
        void onItemClick(DispositivoDTO item);
    }

    /**
     * Constructor del adaptador.
     * @param resultados Lista de objetos ligeros DTO a mostrar.
     * @param listener Callback para la acción de clic.
     */
    public SearchResultsAdapter(List<DispositivoDTO> resultados, OnItemClickListener listener) {
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
        DispositivoDTO item = resultados.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return resultados.size();
    }

    /**
     * Clase interna que representa la vista de una sola fila de la lista.
     */
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

        /**
         * Vincula los datos del objeto DTO con los elementos visuales de la tarjeta.
         */
        void bind(final DispositivoDTO item, final OnItemClickListener listener) {
            // Combinación de Marca, Modelo y Nº Serie para la cabecera del item
            String equipo = item.getMarca() + " " + item.getModelo() + "\nN/S: " + item.getNumeroSerie();
            tvEquipo.setText(equipo);

            // Resumen detallado de la ubicación
            String ubicacion = "Centro: " + item.getPropietario() + "\n" +
                               "Subsede: " + item.getSubsede() + "\n" +
                               "Pabellón: " + item.getPabellon() + " | Planta: " + item.getPlanta() + "\n" +
                               "Aula: " + item.getEspacio();
            tvUbicacion.setText(ubicacion);

            // Estado del dispositivo
            tvEstado.setText("ESTADO: " + item.getEstado());
            
            // Evento de selección
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
