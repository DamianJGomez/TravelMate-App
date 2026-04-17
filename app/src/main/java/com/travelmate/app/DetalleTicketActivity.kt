package com.travelmate.app

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.travelmate.app.api.RetrofitClient
import com.travelmate.app.databinding.ActivityDetalleTicketBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DetalleTicketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleTicketBinding
    private var ticketId: String = ""
    private var userPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAtras.setOnClickListener { finish() }

        ticketId = intent.getStringExtra("ticket_id") ?: run {
            Toast.makeText(this, "Error: ticket no especificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        obtenerContraseñaUsuario()
        cargarDetalleTicket()

        binding.btnCancelar.setOnClickListener {
            mostrarDialogoCancelacion()
        }
    }

    private fun obtenerContraseñaUsuario() {
        try {
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

            userPassword = sharedPreferences.getString("password", "") ?: ""
        } catch (e: Exception) {
            userPassword = ""
        }
    }

    private fun cargarDetalleTicket() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTicketDetail(ticketId).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            try {
                                val json = JSONObject(responseBody)
                                val data = json.getJSONObject("data")

                                binding.tvNumeroTicket.text = "Nº Ticket: ${data.optString("name", "")}"
                                binding.tvVuelo.text = data.optString("flight", "")
                                binding.tvAsiento.text = data.optString("seat", "")
                                binding.tvPrecio.text = "$${data.optDouble("total_amount", 0.0)}"

                                val sourceAirport = data.optString("source_airport_code", "")
                                val destAirport = data.optString("destination_airport_code", "")
                                binding.tvRuta.text = "$sourceAirport → $destAirport"

                                binding.tvFecha.text = data.optString("departure_date", "")
                                binding.tvHora.text = data.optString("departure_time", "")

                                if (data.has("add_ons")) {
                                    val addonsArray = data.optJSONArray("add_ons")
                                    if (addonsArray != null && addonsArray.length() > 0) {
                                        val addonsList = StringBuilder()
                                        for (i in 0 until addonsArray.length()) {
                                            val addon = addonsArray.getJSONObject(i)
                                            addonsList.append("• ${addon.optString("item", "")} - $${addon.optDouble("amount", 0.0)}\n")
                                        }
                                        binding.tvAddons.text = addonsList.toString()
                                    } else {
                                        binding.tvAddons.text = "Ninguno"
                                    }
                                } else {
                                    binding.tvAddons.text = "Ninguno"
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@DetalleTicketActivity, "Error al procesar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@DetalleTicketActivity, "Error al cargar detalles", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleTicketActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoCancelacion() {
        val input = EditText(this).apply {
            hint = "Escribe tu contraseña para confirmar"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(resources.getColor(android.R.color.black, null))
            setHintTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        AlertDialog.Builder(this)
            .setTitle("Cancelar reserva")
            .setMessage("¿Estás seguro de que quieres cancelar este vuelo?")
            .setView(input)
            .setPositiveButton("Cancelar reserva") { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    if (password == userPassword) {
                        cancelarReserva()
                    } else {
                        Toast.makeText(this, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Debes introducir tu contraseña", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun cancelarReserva() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.cancelarReserva(ticketId).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            try {
                                val json = JSONObject(responseBody)
                                val success = if (json.has("message")) {
                                    json.getJSONObject("message").optBoolean("success", false)
                                } else {
                                    json.optBoolean("success", false)
                                }
                                if (success) {
                                    Toast.makeText(this@DetalleTicketActivity, "✅ Reserva cancelada correctamente", Toast.LENGTH_LONG).show()
                                    finish()
                                } else {
                                    val errorMsg = if (json.has("message")) {
                                        json.getJSONObject("message").optString("message", "Error desconocido")
                                    } else {
                                        json.optString("message", "Error desconocido")
                                    }
                                    Toast.makeText(this@DetalleTicketActivity, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@DetalleTicketActivity, "Error al cancelar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@DetalleTicketActivity, "Error al cancelar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleTicketActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}