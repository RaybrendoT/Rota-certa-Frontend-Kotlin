package com.rotacerta.driver.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espelha a resposta de GET /routes/{id}/navigation/. */
@Serializable
data class NavigationResponseDto(
    val route: Long,
    @SerialName("encoded_polyline") val encodedPolyline: String,
    val duration: String? = null,
    @SerialName("duration_text") val durationText: String? = null,
    @SerialName("distance_meters") val distanceMeters: Double? = null,
    @SerialName("distance_text") val distanceText: String? = null,
    val instruction: String,
    val waypoints: NavigationWaypointsDto,
)

@Serializable
data class NavigationWaypointsDto(
    val origin: LatLngDto,
    val intermediates: List<LatLngDto> = emptyList(),
    val destination: LatLngDto,
)

@Serializable
data class LatLngDto(
    val latitude: Double,
    val longitude: Double,
)
