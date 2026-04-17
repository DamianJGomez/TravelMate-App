package com.travelmate.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.travelmate.app.adapters.VuelosAdapter
import com.travelmate.app.databinding.ActivityListaVuelosBinding
import com.travelmate.app.models.Vuelo
import com.travelmate.app.viewmodels.VuelosViewModel

class ListaVuelosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaVuelosBinding
    private val viewModel: VuelosViewModel by viewModels()
    private lateinit var adapter: VuelosAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private var vuelosOriginales: List<Vuelo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaVuelosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSwipeRefresh()
        setupSearchView()
        setupRecyclerView()
        setupObservers()
        setupBotonAtras()

        viewModel.cargarVuelos()
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarVuelos(newText.orEmpty())
                return true
            }
        })
    }

    private fun filtrarVuelos(texto: String) {
        val filtrados = if (texto.isEmpty()) {
            vuelosOriginales
        } else {
            vuelosOriginales.filter { vuelo ->
                vuelo.source_airport.contains(texto, ignoreCase = true) ||
                        vuelo.destination_airport.contains(texto, ignoreCase = true) ||
                        vuelo.name.contains(texto, ignoreCase = true)
            }
        }
        adapter.actualizarLista(filtrados)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            viewModel.cargarVuelos()
        }
    }

    private fun setupBotonAtras() {
        binding.btnAtras.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = VuelosAdapter(emptyList()) { vuelo ->
            val intent = Intent(this, DetalleVueloActivity::class.java)
            intent.putExtra("vuelo", vuelo)
            startActivity(intent)
        }
        binding.rvVuelos.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.vuelos.observe(this) { vuelos ->
            vuelosOriginales = vuelos
            val nuevoAdapter = VuelosAdapter(vuelos) { vuelo ->
                val intent = Intent(this, DetalleVueloActivity::class.java)
                intent.putExtra("vuelo", vuelo)
                startActivity(intent)
            }
            binding.rvVuelos.adapter = nuevoAdapter
            adapter = nuevoAdapter
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (!isLoading) {
                swipeRefresh.isRefreshing = false
            }
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                swipeRefresh.isRefreshing = false
                binding.tvError.visibility = android.view.View.VISIBLE
                binding.tvError.text = errorMsg
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            } else {
                binding.tvError.visibility = android.view.View.GONE
            }
        }
    }
}