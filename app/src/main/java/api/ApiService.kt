package com.travelmate.app.api

import com.travelmate.app.models.ApiResponse
import com.travelmate.app.models.Vuelo
import okhttp3.ResponseBody
import retrofit2.http.Path
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @GET("api/resource/Airplane Flight")
    fun getVuelos(
        @Query("fields") fields: String = "[\"name\",\"source_airport\",\"destination_airport\",\"date_of_departure\",\"time_of_departure\",\"duration\",\"flight_price\",\"status\"]",
        @Query("filters") filters: String = "[[\"is_published\", \"=\", 1]]"
    ): Call<ApiResponse<Vuelo>>

    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.get_passenger_tickets")
    fun getPassengerTickets(
        @Field("passenger_id") passengerId: String
    ): Call<ResponseBody>

    @GET("api/resource/Airplane Flight/{name}")
    fun getFlight(
        @Path("name") name: String
    ): Call<ResponseBody>

    @GET("api/resource/Airplane/{name}")
    fun getAirplane(
        @Path("name") name: String
    ): Call<ResponseBody>

    @GET("api/resource/Airplane Ticket")
    fun getTicketsByFlight(
        @Query("filters") filters: String,
        @Query("fields") fields: String = "[\"name\", \"seat\"]"
    ): Call<ResponseBody>

    @GET("api/method/airplane_mode.airplane_mode.api.get_addons")
    fun getAddons(): Call<ResponseBody>

    @GET("api/resource/Airplane Ticket/{name}")
    fun getTicketDetail(
        @Path("name") name: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/method/frappe.client.get_list")
    fun getTicketsByPassenger(
        @Field("doctype") doctype: String,
        @Field("filters") filters: String
    ): Call<ResponseBody>

    // NUEVO: Obtener perfil usando el método personalizado del backend
    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.get_passenger_profile")
    fun getPassengerProfile(
        @Field("passenger_id") passengerId: String
    ): Call<ResponseBody>

    // NUEVO: Actualizar perfil usando el método personalizado del backend
    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.update_passenger_profile")
    fun updatePassengerProfile(
        @Field("passenger_id") passengerId: String,
        @Field("name") name: String,
        @Field("last_name") lastName: String,
        @Field("phone") phone: String?,
        @Field("date_of_birth") dateOfBirth: String?,
        @Field("passport_number") passportNumber: String?
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.update_user_profile")
    fun updateUserProfile(
        @Field("first_name") firstName: String,
        @Field("last_name") lastName: String,
        @Field("password") password: String?
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.login")
    fun login(
        @Field("usr") usr: String,
        @Field("pwd") pwd: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.registrar_usuario")
    fun registrarUsuario(
        @Field("email") email: String,
        @Field("nombre") nombre: String,
        @Field("apellidos") apellidos: String,
        @Field("password") password: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.crear_reserva")
    fun crearReserva(
        @Field("flight") flight: String,
        @Field("passenger") passenger: String,
        @Field("seat") seat: String,
        @Field("flight_price") flight_price: Double,
        @Field("total_amount") total_amount: Double,
        @Field("addons") addons: String?
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/method/airplane_mode.airplane_mode.api.cancelar_reserva")
    fun cancelarReserva(
        @Field("ticket_id") ticketId: String
    ): Call<ResponseBody>
}