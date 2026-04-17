package com.travelmate.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.travelmate.app.api.RetrofitClient
import com.travelmate.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBotones()
        verificarSesion()
    }

    private fun configurarBotones() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun login(email: String, password: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.login(email, password).execute()
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
                                var fullName = ""
                                var firstName = ""
                                var lastName = ""
                                var emailRespuesta = email
                                var passengerId: String? = null

                                if (jsonRaiz.has("message")) {
                                    val messageObj = jsonRaiz.getJSONObject("message")
                                    exito = messageObj.optBoolean("success", false)
                                    mensaje = messageObj.optString("message", "Sin mensaje")
                                    fullName = messageObj.optString("full_name", "")
                                    firstName = if (messageObj.has("first_name") && !messageObj.isNull("first_name"))
                                        messageObj.getString("first_name") else ""
                                    lastName = if (messageObj.has("last_name") && !messageObj.isNull("last_name"))
                                        messageObj.getString("last_name") else ""
                                    emailRespuesta = messageObj.optString("email", email)
                                    passengerId = if (messageObj.has("passenger_id") && !messageObj.isNull("passenger_id"))
                                        messageObj.getString("passenger_id") else null
                                }

                                if (exito) {
                                    guardarSesion(emailRespuesta, passengerId, firstName, lastName, password)
                                    android.util.Log.d("LoginActivity", "Credenciales establecidas: email=$emailRespuesta, password=$password")
                                    Toast.makeText(this@LoginActivity, "¡Bienvenido $fullName!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this@LoginActivity, mensaje.ifEmpty { "Error desconocido" }, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@LoginActivity, "Error al procesar respuesta: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Respuesta vacía del servidor", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(this@LoginActivity, "Error ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarSesion(email: String, passengerId: String?, firstName: String, lastName: String, password: String) {
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

        with(sharedPreferences.edit()) {
            putString("email", email)
            putString("first_name", firstName)
            putString("last_name", lastName)
            putString("password", password)  // ← AÑADIR ESTA LÍNEA
            if (passengerId != null) {
                putString("passenger_id", passengerId)
            }
            putBoolean("logueado", true)
            apply()
        }
    }

    private fun verificarSesion() {
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

        if (sharedPreferences.getBoolean("logueado", false)) {
            // CAMBIAR ESTA LÍNEA:
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}