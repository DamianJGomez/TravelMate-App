package com.travelmate.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.travelmate.app.databinding.ActivityPerfilBinding

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAtras.setOnClickListener { finish() }

        cargarDatosUsuario()
        cargarPreferenciaModoOscuro()

        binding.btnDarkMode.setOnClickListener {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                guardarPreferenciaModoOscuro(true)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                guardarPreferenciaModoOscuro(false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            recreate()
        }
    }

    private fun guardarPreferenciaModoOscuro(activado: Boolean) {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("dark_mode", activado).apply()
    }

    private fun cargarPreferenciaModoOscuro() {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun cargarDatosUsuario() {
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

            val firstName = sharedPreferences.getString("first_name", "") ?: ""
            val lastName = sharedPreferences.getString("last_name", "") ?: ""
            val email = sharedPreferences.getString("email", "") ?: ""

            binding.tvNombre.text = "Nombre: $firstName"
            binding.tvApellido.text = "Apellido: $lastName"
            binding.tvEmail.text = "Email: $email"
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}