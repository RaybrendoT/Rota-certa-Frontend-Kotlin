package com.rotacerta.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /routes/{id}/navigation/?origin=lat,lng&done_stops=1,2 (Google Routes API calculada pelo backend).
 * Sem origin, o backend parte do endereço do motorista.
 */
@Serializable
data class NavigationResponseDto(
    val route: Long,
    @SerialName("encoded_polyline") val encodedPolyline: String = "",
    @SerialName("duration_text") val durationText: String? = null,
    @SerialName("distance_text") val distanceText: String? = null,
    val instruction: String = "Siga a rota",
    /** "gps" (posição atual) ou "home" (endereço do motorista). */
    @SerialName("origin_source") val originSource: String = "home",
    val waypoints: NavigationWaypointsDto,
    @SerialName("destination_name") val destinationName: String = "Destino",
    /** Todas as paradas da rota, com done = já atendida nesta viagem (fora do trajeto). */
    val stops: List<NavigationStopDto> = emptyList(),
    /** Trecho i termina na i-ésima parada pendente (stopId); o último, no destino (stopId null). */
    val legs: List<NavigationLegDto> = emptyList(),
    val steps: List<NavigationStepDto> = emptyList(),
)

@Serializable
data class NavigationStopDto(
    val id: Long,
    val sequence: Int,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val done: Boolean = false,
)

@Serializable
data class NavigationLegDto(
    @SerialName("stop_id") val stopId: Long? = null,
    @SerialName("distance_meters") val distanceMeters: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
)

@Serializable
data class NavigationStepDto(
    @SerialName("leg_index") val legIndex: Int,
    val instruction: String,
    /** Manobra da Routes API (TURN_LEFT, ROUNDABOUT_RIGHT...), vazia quando é "siga em frente". */
    val maneuver: String = "",
    @SerialName("distance_meters") val distanceMeters: Int = 0,
    val start: LatLngDto,
    val end: LatLngDto,
)

@Serializable
data class NavigationWaypointsDto(
    val origin: LatLngDto,
    val intermediates: List<LatLngDto> = emptyList(),
    val destination: LatLngDto,
)

@Serializable
data class LatLngDto(val latitude: Double, val longitude: Double)

/** Corpo de POST /routes/{id}/location/ (GPS do motorista). */
@Serializable
data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val heading: Double? = null,
    val speed: Double? = null,
    val accuracy: Double? = null,
)

/** Última posição do ônibus (em /me/routes/ e na mensagem vehicle_location do WebSocket). */
@Serializable
data class VehicleLocationDto(
    @SerialName("route_id") val routeId: Long,
    val latitude: Double,
    val longitude: Double,
    val heading: Double? = null,
    val speed: Double? = null,
    @SerialName("recorded_at") val recordedAt: String,
)
