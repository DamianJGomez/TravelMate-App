package com.travelmate.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.travelmate.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificarTipoUsuario()
        configurarBotones()
    }

    private fun verificarTipoUsuario() {
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

            val email = sharedPreferences.getString("email", "") ?: ""

            // Si el email es Administrator, mostramos más opciones
            if (email.equals("Administrator", ignoreCase = true)) {
                isAdmin = true
                mostrarOpcionesAdmin()
            } else {
                isAdmin = false
                mostrarOpcionesUsuarioNormal()
            }
        } catch (e: Exception) {
            isAdmin = false
            mostrarOpcionesUsuarioNormal()
        }
    }

    private fun mostrarOpcionesAdmin() {
        binding.btnVuelos.text = "✈️ Ver Vuelos"
        binding.btnMisVuelos.text = "📋 Gestionar Usuarios"
        binding.btnPerfil.text = "⚙️ Gestionar Vuelos"
        binding.btnPerfil.isEnabled = true
        binding.btnMisVuelos.isEnabled = true

        // Cambiar colores para destacar modo admin
        binding.btnMisVuelos.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_purple))
        binding.btnPerfil.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))

        Toast.makeText(this, "Modo Administrador activado", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarOpcionesUsuarioNormal() {
        binding.btnVuelos.text = "✈️ Vuelos Disponibles"
        binding.btnMisVuelos.text = "📋 Mis Vuelos"
        binding.btnPerfil.text = "👤 Mi Perfil"

        binding.btnMisVuelos.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        binding.btnPerfil.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
    }

    private fun configurarBotones() {
        // Botón Vuelos (primera opción)
        binding.btnVuelos.setOnClickListener {
            startActivity(Intent(this, ListaVuelosActivity::class.java))
        }

        // Botón Mis Vuelos / Gestionar Usuarios (segunda opción)
        binding.btnMisVuelos.setOnClickListener {
            if (isAdmin) {
                // Aquí irá la pantalla de gestión de usuarios
                Toast.makeText(this, "Gestión de Usuarios - Próximamente", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, HistorialActivity::class.java))
            }
        }

        // Botón Perfil / Gestionar Vuelos (tercera opción)
        binding.btnPerfil.setOnClickListener {
            if (isAdmin) {
                // Aquí irá la pantalla de gestión de vuelos
                Toast.makeText(this, "Gestión de Vuelos - Próximamente", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, PerfilActivity::class.java))
            }
        }

        // Botón Cerrar sesión
        binding.btnLogout.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
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

            with(sharedPreferences.edit()) {
                clear()
                apply()
            }

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}