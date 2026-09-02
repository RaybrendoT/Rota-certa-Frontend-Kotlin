package com.rotacerta.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espelha o payload de DriverRouteBoardSerializer (core/driver_serializers.py):
 * usado por GET /routes/my-routes/ (lista) e GET /routes/{id}/driver-detail/ (detalhe).
 */
@Serializable
data class DriverRouteBoardDto(
    val id: Long,
    val name: String,
    @SerialName("company_id") val companyId: Long,
    @SerialName("company_name") val companyName: String,
    val vehicle: RouteVehicleDto? = null,
    @SerialName("route_status") val routeStatus: String, // waiting | in_route | delayed
    @SerialName("eta_next_stop") val etaNextStop: String? = null,
    val stops: List<RouteStopBoardDto> = emptyList(),
    val passengers: List<RoutePassengerBoardDto> = emptyList(),
)

@Serializable
data class RouteVehicleDto(
    val id: Long,
    @SerialName("plate_number") val plateNumber: String,
    val model: String,
)

@Serializable
data class RouteStopBoardDto(
    val id: Long,
    val sequence: Int,
    val title: String,
    val subtitle: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("passenger_count") val passengerCount: Int = 0,
)

@Serializable
data class RoutePassengerBoardDto(
    val id: Long,
    @SerialName("person_id") val personId: Long,
    val name: String,
    val point: String,
    val status: String, // pending | confirmed | absent
    @SerialName("route_stop_id") val routeStopId: Long? = null,
)
