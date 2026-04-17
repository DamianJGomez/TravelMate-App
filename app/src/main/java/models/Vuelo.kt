package com.travelmate.app.models

import java.io.Serializable

data class Vuelo(
    val name: String,
    val source_airport: String,
    val destination_airport: String,
    val date_of_departure: String,
    val time_of_departure: String,
    val duration: Int,
    val flight_price: Double,
    val status: String
) : Serializable