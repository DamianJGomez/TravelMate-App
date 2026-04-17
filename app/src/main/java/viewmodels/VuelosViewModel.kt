package com.travelmate.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelmate.app.api.RetrofitClient
import com.travelmate.app.models.Vuelo
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import com.travelmate.app.models.ApiResponse
import retrofit2.Response

class VuelosViewModel : ViewModel() {

    private val _vuelos = MutableLiveData<List<Vuelo>>()
    val vuelos: LiveData<List<Vuelo>> = _vuelos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun cargarVuelos() {
        _isLoading.value = true

        RetrofitClient.instance.getVuelos().enqueue(object : Callback<ApiResponse<Vuelo>> {
            override fun onResponse(
                call: Call<ApiResponse<Vuelo>>,
                response: Response<ApiResponse<Vuelo>>
            ) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _vuelos.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = "Error ${response.code()}: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<ApiResponse<Vuelo>>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Error de conexión: ${t.message}"
            }
        })
    }
}