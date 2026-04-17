package com.travelmate.app

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.travelmate.app.api.RetrofitClient
import com.travelmate.app.databinding.ActivityReservaBinding
import com.travelmate.app.models.Vuelo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// Data class para los add-ons
data class AddonItem(
    val name: String,
    val description: String,
    val price: Double,
    var isSelected: Boolean = false
)

class ReservaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservaBinding
    private lateinit var vuelo: Vuelo
    private lateinit var passengerId: String
    private var asientoSeleccionado: String = ""
    private var asientoOriginal: String = ""
    private var precioBase: Double = 0.0
    private var addonsTotal: Double = 0.0
    private lateinit var addonsList: MutableList<AddonItem>

    // Lanzador para el selector de asientos
    private val selectorAsientoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val asiento = result.data?.getStringExtra("asiento_seleccionado")
            if (asiento != null && asiento != asientoOriginal) {
                asientoSeleccionado = asiento
                binding.tvAsiento.text = "Asiento: $asientoSeleccionado"
                actualizarPrecioTotal()
                Toast.makeText(this, "Asiento cambiado a $asiento (+3€)", Toast.LENGTH_SHORT).show()
            } else if (asiento != null) {
                Toast.makeText(this, "Mismo asiento, sin coste adicional", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón Atrás
        binding.btnAtras.setOnClickListener {
            finish()
        }

        // Recibir datos del intent
        vuelo = intent.getSerializableExtra("vuelo") as? Vuelo ?: run {
            Toast.makeText(this, "Error al cargar el vuelo", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        passengerId = intent.getStringExtra("passenger_id") ?: run {
            Toast.makeText(this, "Error: usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        precioBase = vuelo.flight_price

        // Generar asiento aleatorio
        CoroutineScope(Dispatchers.IO).launch {
            val asiento = generarAsientoAleatorio()
            withContext(Dispatchers.Main) {
                asientoOriginal = asiento
                asientoSeleccionado = asientoOriginal
                mostrarInformacion()
            }
        }

        // Cargar servicios adicionales
        cargarAddons()

        // Botón Elegir asiento
        binding.btnElegirAsiento.setOnClickListener {
            val intent = Intent(this, SelectorAsientoActivity::class.java)
            intent.putExtra("flight_name", vuelo.name)
            intent.putExtra("passenger_id", passengerId)
            intent.putExtra("flight_price", vuelo.flight_price)
            selectorAsientoLauncher.launch(intent)
        }

        // Botón Confirmar reserva
        binding.btnConfirmar.setOnClickListener {
            crearReserva()
        }
    }

    private suspend fun generarAsientoAleatorio(): String {
        // Obtener asientos ocupados para este vuelo
        val filtro = "[[\"flight\", \"=\", \"${vuelo.name}\"]]"
        val response = RetrofitClient.instance.getTicketsByFlight(filtro).execute()
        val ocupados = mutableListOf<String>()

        if (response.isSuccessful) {
            val responseBody = response.body()?.string()
            if (responseBody != null) {
                val json = JSONObject(responseBody)
                val data = json.optJSONArray("data")
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val ticket = data.getJSONObject(i)
                        val seat = ticket.optString("seat", "")
                        if (seat.isNotEmpty()) {
                            ocupados.add(seat)
                        }
                    }
                }
            }
        }

        // Obtener capacidad del avión
        val vueloResponse = RetrofitClient.instance.getFlight(vuelo.name).execute()
        var capacidad = 150 // valor por defecto
        if (vueloResponse.isSuccessful) {
            val vueloJson = JSONObject(vueloResponse.body()?.string() ?: "{}")
            val flightData = vueloJson.getJSONObject("data")
            val airplaneName = flightData.getString("airplane")
            val avionCall = RetrofitClient.instance.getAirplane(airplaneName).execute()
            if (avionCall.isSuccessful) {
                val avionJson = JSONObject(avionCall.body()?.string() ?: "{}")
                capacidad = avionJson.getJSONObject("data").getInt("capacity")
            }
        }

        // Generar asiento aleatorio dentro de la capacidad
        val columnas = listOf("A", "B", "C", "D", "E", "F")
        val filas = capacidad / 6
        val resto = capacidad % 6
        var asiento: String

        if (capacidad == 0) {
            return "Sin asientos disponibles"
        }

        do {
            if (capacidad < 6) {
                // Para capacidades menores a 6, solo hay una fila
                val fila = 1
                val columna = columnas.random()
                asiento = "$fila$columna"
            } else {
                val fila = (1..filas).random()
                val columna = columnas.random()
                asiento = "$fila$columna"
            }
        } while (ocupados.contains(asiento))

        return asiento
    }

    private fun mostrarInformacion() {
        binding.tvVuelo.text = "Vuelo: ${vuelo.name}"
        binding.tvPasajero.text = "Pasajero ID: $passengerId"
        binding.tvAsiento.text = "Asiento: $asientoSeleccionado"
        binding.tvPrecio.text = "Precio total: $$precioBase"
    }

    private fun actualizarPrecioTotal() {
        var total = precioBase

        // Sumar coste del asiento si es diferente
        if (asientoSeleccionado != asientoOriginal) {
            total += 3.0
        }

        // Sumar add-ons seleccionados
        addonsTotal = 0.0
        if (::addonsList.isInitialized) {
            for (addon in addonsList) {
                if (addon.isSelected) {
                    addonsTotal += addon.price
                }
            }
        }
        total += addonsTotal

        binding.tvPrecio.text = "Precio total: $$total"
    }

    private fun cargarAddons() {
        android.util.Log.d("Reserva", "=== INICIO cargarAddons ===")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getAddons().execute()
                android.util.Log.d("Reserva", "Response code: ${response.code()}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        android.util.Log.d("Reserva", "Response body: $responseBody")

                        if (responseBody != null) {
                            try {
                                val json = JSONObject(responseBody)

                                val success = if (json.has("message")) {
                                    json.getJSONObject("message").getBoolean("success")
                                } else {
                                    json.getBoolean("success")
                                }

                                if (success) {
                                    val data = if (json.has("message")) {
                                        json.getJSONObject("message").getJSONArray("data")
                                    } else {
                                        json.getJSONArray("data")
                                    }

                                    addonsList = mutableListOf()
                                    for (i in 0 until data.length()) {
                                        val item = data.getJSONObject(i)
                                        addonsList.add(
                                            AddonItem(
                                                name = item.getString("name"),
                                                description = item.getString("description"),
                                                price = item.getDouble("standard_price")
                                            )
                                        )
                                    }
                                    android.util.Log.d("Reserva", "Addons cargados: ${addonsList.size}")
                                    mostrarAddons()
                                } else {
                                    android.util.Log.e("Reserva", "Success false en respuesta")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("Reserva", "Error parse: ${e.message}")
                            }
                        } else {
                            android.util.Log.e("Reserva", "Response body vacío")
                        }
                    } else {
                        android.util.Log.e("Reserva", "Error HTTP: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Reserva", "Excepción: ${e.message}")
            }
        }
    }

    private fun mostrarAddons() {
        val addonsContainer = binding.llAddons
        addonsContainer.removeAllViews()

        android.util.Log.d("Reserva", "Mostrando ${addonsList.size} add-ons")

        for (addon in addonsList) {
            val checkBox = android.widget.CheckBox(this).apply {
                text = "${addon.description} - $${addon.price}"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 8, 16, 8)

                setOnCheckedChangeListener { _, isChecked ->
                    addon.isSelected = isChecked
                    actualizarPrecioTotal()
                }
            }
            addonsContainer.addView(checkBox)
            android.util.Log.d("Reserva", "Añadido checkbox: ${addon.description}")
        }
    }

    private fun crearReserva() {
        binding.btnConfirmar.isEnabled = false
        binding.btnConfirmar.text = "Procesando..."

        // Calcular total final
        var totalAmount = precioBase
        if (asientoSeleccionado != asientoOriginal) {
            totalAmount += 3.0
        }
        if (::addonsList.isInitialized) {
            for (addon in addonsList) {
                if (addon.isSelected) {
                    totalAmount += addon.price
                }
            }
        }

        // Construir JSON de add-ons seleccionados
        val addonsJson = JSONArray()
        if (::addonsList.isInitialized) {
            for (addon in addonsList) {
                if (addon.isSelected) {
                    val addonObj = JSONObject()
                    addonObj.put("item", addon.name)
                    addonObj.put("amount", addon.price)
                    addonsJson.put(addonObj)
                }
            }
        }
        val addonsString = if (addonsJson.length() > 0) addonsJson.toString() else null

        // LOGS para depurar
        android.util.Log.d("Reserva", "=== DATOS DE RESERVA ===")
        android.util.Log.d("Reserva", "flight: ${vuelo.name}")
        android.util.Log.d("Reserva", "passenger: $passengerId")
        android.util.Log.d("Reserva", "seat: $asientoSeleccionado")
        android.util.Log.d("Reserva", "flight_price: ${vuelo.flight_price}")
        android.util.Log.d("Reserva", "total_amount: $totalAmount")
        android.util.Log.d("Reserva", "addons: $addonsString")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.crearReserva(
                    flight = vuelo.name,
                    passenger = passengerId,
                    seat = asientoSeleccionado,
                    flight_price = vuelo.flight_price,
                    total_amount = totalAmount,
                    addons = addonsString
                ).execute()

                withContext(Dispatchers.Main) {
                    binding.btnConfirmar.isEnabled = true
                    binding.btnConfirmar.text = "Confirmar reserva"

                    android.util.Log.d("Reserva", "response.code: ${response.code()}")

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        android.util.Log.d("Reserva", "responseBody: $responseBody")

                        if (responseBody != null) {
                            try {
                                val json = JSONObject(responseBody)
                                val success = if (json.has("message")) {
                                    json.getJSONObject("message").optBoolean("success", false)
                                } else {
                                    json.optBoolean("success", false)
                                }

                                if (success) {
                                    Toast.makeText(this@ReservaActivity, "¡Reserva confirmada!", Toast.LENGTH_LONG).show()
                                    finish()
                                } else {
                                    val errorMsg = if (json.has("message")) {
                                        val msgObj = json.getJSONObject("message")
                                        msgObj.optString("message", "Error desconocido")
                                    } else {
                                        json.optString("message", "Error desconocido")
                                    }
                                    mostrarError("Error: $errorMsg")
                                }
                            } catch (e: Exception) {
                                mostrarError("Error al procesar respuesta: ${e.message}")
                            }
                        } else {
                            mostrarError("Respuesta vacía del servidor")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("Reserva", "Error ${response.code()}: $errorBody")
                        mostrarError("Error ${response.code()}: $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("Reserva", "Excepción: ${e.message}")
                    mostrarError("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.btnConfirmar.isEnabled = true
        binding.btnConfirmar.text = "Confirmar reserva"
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }
}