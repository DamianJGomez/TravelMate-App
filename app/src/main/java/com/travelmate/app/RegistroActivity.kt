package com.travelmate.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.travelmate.app.api.RetrofitClient
import com.travelmate.app.databinding.ActivityRegistroBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBotones()
    }

    private fun configurarBotones() {
        // Botón atrás
        binding.btnAtras.setOnClickListener {
            finish()
        }

        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etNombre.text.toString()
            val apellidos = binding.etApellidos.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (nombre.isNotEmpty() && apellidos.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registrar(nombre, apellidos, email, password)
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun registrar(nombre: String, apellidos: String, email: String, password: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.registrarUsuario(email, nombre, apellidos, password).execute()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            try {
                                val jsonRaiz = JSONObject(responseBody)

                                // Buscar el objeto message que contiene los datos reales
                                var exito = false
                                var mensaje = ""

                                if (jsonRaiz.has("message")) {
                                    val messageObj = jsonRaiz.getJSONObject("message")
                                    exito = messageObj.optBoolean("success", false)
                                    mensaje = messageObj.optString("message", "Sin mensaje")
                                }

                                if (exito) {
                                    Toast.makeText(this@RegistroActivity, "Registro correcto", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@RegistroActivity, LoginActivity::class.java))
                                    finish()
                                } else {
                                    // Si no hay éxito, buscar en _server_messages
                                    if (jsonRaiz.has("_server_messages")) {
                                        try {
                                            val serverMessages = jsonRaiz.getString("_server_messages")
                                            // Extraer mensaje amigable (simplificado)
                                            mensaje = "El usuario ya existe o hay un error"
                                        } catch (e: Exception) {
                                            // Ignorar
                                        }
                                    }
                                    Toast.makeText(this@RegistroActivity, mensaje.ifEmpty { "Error en el registro" }, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@RegistroActivity, "Error al procesar respuesta: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this@RegistroActivity, "Respuesta vacía del servidor", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@RegistroActivity, "Error ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@RegistroActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}