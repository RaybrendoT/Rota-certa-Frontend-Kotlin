package com.rotacerta.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espelha DriverRouteBoardSerializer (core/driver_serializers.py):
 * GET /routes/my-routes/ (lista) e GET /routes/{id}/driver-detail/ (detalhe).
 */
@Serializable
data class DriverRouteDto(
    val id: Long,
    val name: String,
    @SerialName("company_id") val companyId: Long,
    @SerialName("company_name") val companyName: String,
    val vehicle: RouteVehicleDto? = null,
    @SerialName("route_status") val routeStatus: String = RouteStatus.WAITING,
    /** Rota extra de um dia só ("AAAA-MM-DD"); null = rota fixa do turno. */
    @SerialName("service_date") val serviceDate: String? = null,
    /** Turno: horário de chegada na empresa (a API já ordena as rotas por ele). */
    val schedule: RouteScheduleDto? = null,
    val stops: List<RouteStopDto> = emptyList(),
    val passengers: List<RoutePassengerDto> = emptyList(),
) {
    val confirmedCount: Int get() = passengers.count { it.status == BoardingStatus.CONFIRMED }

    fun passengersAt(stopId: Long): List<RoutePassengerDto> = passengers.filter { it.routeStopId == stopId }
}

@Serializable
data class RouteScheduleDto(
    val id: Long,
    /** "HH:MM" */
    val time: String,
    val weekdays: List<String> = emptyList(),
)

@Serializable
data class RouteVehicleDto(
    val id: Long,
    @SerialName("plate_number") val plateNumber: String,
    val model: String,
)

@Serializable
data class RouteStopDto(
    val id: Long,
    val sequence: Int,
    val title: String,
    val subtitle: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("passenger_count") val passengerCount: Int = 0,
)

@Serializable
data class RoutePassengerDto(
    val id: Long,
    @SerialName("person_id") val personId: Long,
    val name: String,
    val point: String,
    val status: String,
    @SerialName("route_stop_id") val routeStopId: Long? = null,
)

/** route_status derivado do último evento da rota (RN-20). */
object RouteStatus {
    const val WAITING = "waiting"
    const val IN_ROUTE = "in_route"
    const val DELAYED = "delayed"
}

/** RoutePassenger.BoardingStatus. */
object BoardingStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
    const val ABSENT = "absent"
}

/** Corpo de PATCH /route-passengers/{id}/ e /me/routes/{id}/. */
@Serializable
data class BoardingStatusRequest(@SerialName("boarding_status") val boardingStatus: String)
