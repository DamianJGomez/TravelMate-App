package com.travelmate.app

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.travelmate.app.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log

class SelectorAsientoActivity : AppCompatActivity() {

    private lateinit var flightName: String
    private lateinit var passengerId: String
    private var flightPrice: Double = 0.0
    private var capacidadAvion: Int = 0
    private var asientosOcupados: MutableList<String> = mutableListOf()
    private var asientoSeleccionado: String? = null
    private lateinit var llMapaAsientos: LinearLayout
    private lateinit var tvAsientoSeleccionado: TextView
    private lateinit var btnConfirmarAsiento: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        android.widget.Toast.makeText(this, "SelectorAsientoActivity se abrió", android.widget.Toast.LENGTH_LONG).show()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector_asientos)

        // Recibir datos
        flightName = intent.getStringExtra("flight_name") ?: ""
        passengerId = intent.getStringExtra("passenger_id") ?: ""
        flightPrice = intent.getDoubleExtra("flight_price", 0.0)

        llMapaAsientos = findViewById(R.id.llMapaAsientos)
        tvAsientoSeleccionado = findViewById(R.id.tvAsientoSeleccionado)
        btnConfirmarAsiento = findViewById(R.id.btnConfirmarAsiento)

        findViewById<Button>(R.id.btnAtras).setOnClickListener { finish() }

        btnConfirmarAsiento.setOnClickListener {
            if (asientoSeleccionado != null) {
                val intent = intent
                intent.putExtra("asiento_seleccionado", asientoSeleccionado)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Selecciona un asiento", Toast.LENGTH_SHORT).show()
            }
        }

        cargarDatosVuelo()
    }

    private fun cargarDatosVuelo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("Selector", "=== INICIO cargarDatosVuelo ===")
                Log.d("Selector", "flightName: $flightName")

                // Obtener el vuelo
                val vueloResponse = RetrofitClient.instance.getFlight(flightName).execute()
                Log.d("Selector", "vueloResponse.code: ${vueloResponse.code()}")

                if (vueloResponse.isSuccessful) {
                    val vueloJson = JSONObject(vueloResponse.body()?.string() ?: "{}")
                    Log.d("Selector", "vueloJson: $vueloJson")
                    val flightData = vueloJson.getJSONObject("data")
                    val airplaneName = flightData.getString("airplane")
                    Log.d("Selector", "airplaneName: $airplaneName")

                    // Obtener el avión
                    val avionCall = RetrofitClient.instance.getAirplane(airplaneName).execute()
                    Log.d("Selector", "avionCall.code: ${avionCall.code()}")

                    if (avionCall.isSuccessful) {
                        val avionJson = JSONObject(avionCall.body()?.string() ?: "{}")
                        Log.d("Selector", "avionJson: $avionJson")
                        capacidadAvion = avionJson.getJSONObject("data").getInt("capacity")
                        Log.d("Selector", "capacidadAvion: $capacidadAvion")
                    }
                }

                // CORREGIDO: filtro en formato JSON válido
                val filtro = "[ [\"flight\", \"=\", \"$flightName\"] ]"
                Log.d("Selector", "Filtro para tickets: $filtro")

                val ticketsResponse = RetrofitClient.instance.getTicketsByFlight(filtro,  "[\"name\", \"seat\"]").execute()
                Log.d("Selector", "ticketsResponse.code: ${ticketsResponse.code()}")

                if (ticketsResponse.isSuccessful) {
                    val ticketsJson = JSONObject(ticketsResponse.body()?.string() ?: "{}")
                    Log.d("Selector", "ticketsJson: $ticketsJson")
                    val data = ticketsJson.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val ticket = data.getJSONObject(i)
                            val seat = ticket.optString("seat", "")
                            if (seat.isNotEmpty()) {
                                asientosOcupados.add(seat)
                                Log.d("Selector", "Asiento ocupado: $seat")
                            }
                        }
                        // LOG AÑADIDO - muestra el total de asientos ocupados
                        Log.d(
                            "Selector",
                            "TOTAL ASIENTOS OCUPADOS: ${asientosOcupados.size} - Lista: ${asientosOcupados.joinToString()}"
                        )
                    }
                } else {
                    Log.e("Selector", "Error en ticketsResponse: ${ticketsResponse.code()}")
                    val errorBody = ticketsResponse.errorBody()?.string()
                    Log.e("Selector", "Error body: $errorBody")
                }

                withContext(Dispatchers.Main) {
                    Log.d("Selector", "capacidadAvion final: $capacidadAvion")
                    Log.d("Selector", "asientosOcupados: $asientosOcupados")

                    // Si no se pudo obtener la capacidad, usar un valor por defecto
                    if (capacidadAvion == 0) {
                        capacidadAvion = 150
                        Log.d("Selector", "Usando capacidad por defecto: 150")
                    }

                    generarMapaAsientos()
                }
            } catch (e: Exception) {
                Log.e("Selector", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SelectorAsientoActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generarMapaAsientos() {
        llMapaAsientos.removeAllViews()

        Log.d("Selector", "=== GENERAR MAPA ===")
        Log.d("Selector", "capacidadAvion: $capacidadAvion")
        Log.d("Selector", "asientosOcupados: $asientosOcupados")

        val columnas = listOf("A", "B", "C", "D", "E", "F")

        // Para capacidad 1, mostrar solo 1A
        if (capacidadAvion == 1) {
            Log.d("Selector", "Mostrando solo asiento 1A")
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            row.addView(createTextView("1", 60, true))
            val asiento = "1A"
            val ocupado = asientosOcupados.contains(asiento)
            row.addView(createAsientoButton(asiento, ocupado))
            llMapaAsientos.addView(row)
            findViewById<TextView>(R.id.tvInfo).text = "Asientos disponibles en verde | Ocupados en rojo"
            return
        }

        // Para capacidades normales
        val filasCompletas = capacidadAvion / 6
        val asientosUltimaFila = capacidadAvion % 6

        // Cabecera
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        headerRow.addView(createTextView("", 60))
        for (col in columnas) {
            headerRow.addView(createTextView(col, 80, true))
        }
        llMapaAsientos.addView(headerRow)

        // Filas completas
        for (fila in 1..filasCompletas) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            row.addView(createTextView(fila.toString(), 60, true))
            for (col in columnas) {
                val asiento = "$fila$col"
                val ocupado = asientosOcupados.contains(asiento)
                row.addView(createAsientoButton(asiento, ocupado))
            }
            llMapaAsientos.addView(row)
        }

        // Última fila
        if (asientosUltimaFila > 0) {
            val fila = filasCompletas + 1
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            row.addView(createTextView(fila.toString(), 60, true))
            for (i in 0 until asientosUltimaFila) {
                val asiento = "$fila${columnas[i]}"
                val ocupado = asientosOcupados.contains(asiento)
                row.addView(createAsientoButton(asiento, ocupado))
            }
            llMapaAsientos.addView(row)
        }

        findViewById<TextView>(R.id.tvInfo).text = "Asientos disponibles en verde | Ocupados en rojo"
    }

    private fun createAsientoButton(asiento: String, ocupado: Boolean): Button {
        val button = Button(this).apply {
            text = asiento
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(8, 8, 8, 8)
            textSize = 12f

            if (ocupado) {
                isEnabled = false
                setTextColor(resources.getColor(android.R.color.white, null))
                setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
            } else {
                setTextColor(resources.getColor(android.R.color.white, null))
                setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
                setOnClickListener {
                    seleccionarAsiento(asiento)
                }
            }
        }
        return button
    }

    private fun seleccionarAsiento(asiento: String) {
        asientoSeleccionado = asiento
        tvAsientoSeleccionado.text = "Asiento seleccionado: $asiento"
        tvAsientoSeleccionado.setTextColor(resources.getColor(android.R.color.holo_purple, null))
        Toast.makeText(this, "Asiento $asiento seleccionado", Toast.LENGTH_SHORT).show()
    }

    private fun createTextView(texto: String, widthDp: Int, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = texto
            layoutParams = LinearLayout.LayoutParams(dpToPx(widthDp), LinearLayout.LayoutParams.WRAP_CONTENT)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            if (bold) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}