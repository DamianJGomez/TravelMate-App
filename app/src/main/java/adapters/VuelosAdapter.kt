package com.travelmate.app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.travelmate.app.DetalleVueloActivity
import com.travelmate.app.databinding.ItemVueloBinding
import com.travelmate.app.models.Vuelo

class VuelosAdapter(
    private var vuelos: List<Vuelo>,
    private val onClick: (Vuelo) -> Unit
) : RecyclerView.Adapter<VuelosAdapter.VueloViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VueloViewHolder {
        val binding = ItemVueloBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VueloViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: VueloViewHolder, position: Int) {
        holder.bind(vuelos[position])
    }

    override fun getItemCount() = vuelos.size

    fun actualizarLista(nuevaLista: List<Vuelo>) {
        vuelos = nuevaLista
        notifyDataSetChanged()
    }

    class VueloViewHolder(
        private val binding: ItemVueloBinding,
        private val onClick: (Vuelo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vuelo: Vuelo) {
            binding.tvRuta.text = "${vuelo.source_airport} → ${vuelo.destination_airport}"
            binding.tvFecha.text = "Fecha: ${vuelo.date_of_departure} ${vuelo.time_of_departure}"
            binding.tvPrecio.text = "Precio: $${vuelo.flight_price}"

            // El botón "Ver vuelo" es el que dispara el clic
            binding.btnVerDetalle.setOnClickListener {
                onClick(vuelo)
            }

            // Opcional: también se puede mantener el clic en toda la tarjeta
            // binding.root.setOnClickListener { onClick(vuelo) }
        }
    }
}