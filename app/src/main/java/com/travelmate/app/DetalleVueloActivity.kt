package com.travelmate.app

import android.content.Intent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.travelmate.app.databinding.ActivityDetalleVueloBinding
import com.travelmate.app.models.Vuelo

class DetalleVueloActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleVueloBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleVueloBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAtras.setOnClickListener {
            finish()
        }

        // Recibir el vuelo del intent
        val vuelo = intent.getSerializableExtra("vuelo") as? Vuelo

        if (vuelo != null) {
            mostrarDetalle(vuelo)
        } else {
            Toast.makeText(this, "Error al cargar los detalles", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun mostrarDetalle(vuelo: Vuelo) {
        binding.tvRutaDetalle.text = "${vuelo.source_airport} → ${vuelo.destination_airport}"
        binding.tvVueloNombre.text = "Vuelo: ${vuelo.name}"
        binding.tvFecha.text = vuelo.date_of_departure
        binding.tvHora.text = vuelo.time_of_departure

        // Formatear duración (asumiendo que está en segundos)
        val horas = vuelo.duration / 3600
        val minutos = (vuelo.duration % 3600) / 60
        binding.tvDuracion.text = if (horas > 0) "${horas}h ${minutos}m" else "${minutos}m"

        binding.tvPrecio.text = "$${vuelo.flight_price}"

        // Traducir estado
        val estadoTexto = when (vuelo.status) {
            "Scheduled" -> "Programado"
            "Completed" -> "Completado"
            "Cancelled" -> "Cancelado"
            else -> vuelo.status
        }
        binding.tvEstado.text = estadoTexto

        // Botón de reserva
        binding.btnReservar.setOnClickListener {
            // Obtener el passenger_id de las preferencias
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "sesion",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val passengerId = sharedPreferences.getString("passenger_id", null)

            if (passengerId == null) {
                Toast.makeText(this, "Debes iniciar sesión para reservar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Abrir la pantalla de reserva
            val intent = Intent(this, ReservaActivity::class.java)
            intent.putExtra("vuelo", vuelo)
            intent.putExtra("passenger_id", passengerId)
            startActivity(intent)
        }
    }
}