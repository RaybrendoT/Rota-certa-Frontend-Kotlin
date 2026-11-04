package com.rotacerta.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espelha RouteEventSerializer (core/serializers.py). */
@Serializable
data class RouteEventDto(
    val id: Long,
    val route: Long,
    @SerialName("event_type") val eventType: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
)

/** Corpo de POST /route-events/. */
@Serializable
data class RouteEventCreateRequest(
    val route: Long,
    @SerialName("event_type") val eventType: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
)

/** RouteEvent.EVENT_TYPE_CHOICES. */
object RouteEventType {
    const val DEPARTURE = "departure"
    const val ARRIVAL = "arrival"
    const val STOP = "stop"
    const val DELAY = "delay"
    const val INCIDENT = "incident"

    /** Eventos que o motorista registra manualmente (partida/chegada saem de Iniciar/Finalizar). */
    val MANUAL = listOf(STOP, DELAY, INCIDENT)

    /** delay e incident exigem descrição (RouteEventSerializer). */
    fun requiresDescription(type: String) = type == DELAY || type == INCIDENT

    fun label(type: String) = when (type) {
        DEPARTURE -> "Partida"
        ARRIVAL -> "Chegada"
        STOP -> "Parada"
        DELAY -> "Atraso"
        INCIDENT -> "Incidente"
        else -> type
    }
}
