package com.travelmate.app.models

data class ApiResponse<T>(
    val data: List<T>? = null,
    val message: String? = null
)