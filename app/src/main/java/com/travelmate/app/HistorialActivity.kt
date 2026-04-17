package com.travelmate.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.travelmate.app.api.RetrofitClient
import com.travelmate.app.databinding.ActivityHistorialBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.travelmate.app.adapters.HistorialAdapter
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog

class HistorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialBinding
    private lateinit var passengerId: String
    private lateinit var adapter: HistorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener passenger_id de la sesión
        obtenerPassengerId()

        // Botón atrás
        binding.btnAtras.setOnClickListener {
            finish()
        }

        // Cargar historial
        cargarHistorial()
    }

    private fun obtenerPassengerId() {
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

            passengerId = sharedPreferences.getString("passenger_id", null) ?: ""
        } catch (e: Exception) {
            passengerId = ""
        }
    }

    private fun cargarHistorial() {
        if (passengerId.isEmpty()) {
            Toast.makeText(this, "Error: usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        Log.d("Historial", "passengerId: $passengerId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getPassengerTickets(passengerId).execute()

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        Log.d("Historial", "Response Body: $responseBody")

                        if (responseBody != null) {
                            try {
                                val json = JSONObject(responseBody)

                                // CORRECCIÓN: La respuesta tiene la estructura {"message": {"success": true, "data": [...]}}
                                val messageObj = json.optJSONObject("message")
                                val success = if (messageObj != null) {
                                    messageObj.optBoolean("success", false)
                                } else {
                                    json.optBoolean("success", false)
                                }

                                if (success) {
                                    val data = if (messageObj != null) {
                                        messageObj.optJSONArray("data")
                                    } else {
                                        json.optJSONArray("data")
                                    }

                                    val tickets = mutableListOf<HistorialItem>()

                                    if (data != null && data.length() > 0) {
                                        for (i in 0 until data.length()) {
                                            val item = data.getJSONObject(i)
                                            tickets.add(HistorialItem(
                                                ticketName = item.optString("name", ""),
                                                flightName = item.optString("flight_name", item.optString("flight", "")),
                                                seat = item.optString("seat", ""),
                                                totalAmount = item.optDouble("total_amount", 0.0),
                                                flightDate = item.optString("date_of_departure", ""),
                                                sourceAirport = item.optString("source_airport", ""),
                                                destinationAirport = item.optString("destination_airport", "")
                                            ))
                                        }
                                    }

                                    if (tickets.isEmpty()) {
                                        binding.tvVacio.visibility = android.view.View.VISIBLE
                                        Toast.makeText(this@HistorialActivity, "No tienes reservas", Toast.LENGTH_SHORT).show()
                                    } else {
                                        setupRecyclerView(tickets)
                                    }
                                } else {
                                    val errorMsg = if (messageObj != null) {
                                        messageObj.optString("message", "Error desconocido")
                                    } else {
                                        json.optString("message", "Error desconocido")
                                    }
                                    Toast.makeText(this@HistorialActivity, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("Historial", "Error parse: ${e.message}")
                                Toast.makeText(this@HistorialActivity, "Error al procesar datos", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            binding.tvVacio.visibility = android.view.View.VISIBLE
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Historial", "Error ${response.code()}: $errorBody")
                        Toast.makeText(this@HistorialActivity, "Error al cargar historial", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    Log.e("Historial", "Excepción: ${e.message}")
                    Toast.makeText(this@HistorialActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView(tickets: List<HistorialItem>) {
        adapter = HistorialAdapter(tickets) { ticketId ->
            val intent = Intent(this, DetalleTicketActivity::class.java)
            intent.putExtra("ticket_id", ticketId)
            startActivity(intent)
        }
        binding.rvHistorial.layoutManager = LinearLayoutManager(this)
        binding.rvHistorial.adapter = adapter

    }
}

data class HistorialItem(
    val ticketName: String,
    val flightName: String,
    val seat: String,
    val totalAmount: Double,
    val flightDate: String,
    val sourceAirport: String,
    val destinationAirport: String
)