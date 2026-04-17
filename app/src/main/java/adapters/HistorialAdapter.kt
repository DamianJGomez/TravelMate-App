package com.travelmate.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.travelmate.app.HistorialItem
import com.travelmate.app.databinding.ItemHistorialBinding

class HistorialAdapter(
    private val tickets: List<HistorialItem>,
    private val onTicketClick: (String) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemHistorialBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistorialViewHolder(binding, onTicketClick)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount() = tickets.size

    class HistorialViewHolder(
        private val binding: ItemHistorialBinding,
        private val onTicketClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: HistorialItem) {
            binding.tvVuelo.text = "Vuelo: ${ticket.flightName}"
            binding.tvRuta.text = "Ruta: ${ticket.sourceAirport} → ${ticket.destinationAirport}"
            binding.tvFecha.text = "Fecha: ${ticket.flightDate}"
            binding.tvAsiento.text = "Asiento: ${ticket.seat}"
            binding.tvPrecio.text = "Precio: $${ticket.totalAmount}"

            binding.root.setOnClickListener {
                onTicketClick(ticket.ticketName)
            }
        }
    }
}