package com.rotacerta.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Item de GET /me/routes/: vínculo do colaborador (RoutePassenger) com uma rota. */
@Serializable
data class CollaboratorRouteDto(
    /** ID do vínculo (route_passenger_id), usado no PATCH de presença. */
    val id: Long,
    @SerialName("boarding_status") val boardingStatus: String,
    val route: CollaboratorRouteInfoDto,
    val stop: CollaboratorStopDto? = null,
    @SerialName("last_location") val lastLocation: VehicleLocationDto? = null,
    val schedules: List<ScheduleDto> = emptyList(),
)

@Serializable
data class CollaboratorRouteInfoDto(
    val id: Long,
    val name: String,
    @SerialName("company_name") val companyName: String,
    @SerialName("route_status") val routeStatus: String = RouteStatus.WAITING,
    val vehicle: CollaboratorVehicleDto? = null,
    @SerialName("driver_name") val driverName: String? = null,
)

@Serializable
data class CollaboratorVehicleDto(
    @SerialName("plate_number") val plateNumber: String,
    val model: String,
)

@Serializable
data class CollaboratorStopDto(
    val id: Long,
    val sequence: Int,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
)

/** Horário de operação da empresa: dias ("MON".."SUN") e hora "HH:mm". */
@Serializable
data class ScheduleDto(val weekdays: List<String> = emptyList(), val time: String)
