package com.rotacerta.driver.data.remote.dto

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
    @SerialName("is_active") val isActive: Boolean = true,
)

/** Corpo enviado em POST /route-events/. */
@Serializable
data class RouteEventCreateRequest(
    val route: Long,
    @SerialName("event_type") val eventType: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
)

/** Corpo enviado em PATCH /route-passengers/{id}/ para embarcar/desembarcar/ausentar. */
@Serializable
data class RoutePassengerStatusUpdateRequest(
    @SerialName("boarding_status") val boardingStatus: String,
)

/** Tipos de evento aceitos pelo backend (RouteEvent.EVENT_TYPE_CHOICES). */
object RouteEventType {
    const val DEPARTURE = "departure"
    const val ARRIVAL = "arrival"
    const val STOP = "stop"
    const val DELAY = "delay"
    const val INCIDENT = "incident"

    val ALL = listOf(DEPARTURE, ARRIVAL, STOP, DELAY, INCIDENT)

    /** delay e incident exigem descrição obrigatória (regra do RouteEventSerializer). */
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

/** Status de embarque aceitos (RoutePassenger.BoardingStatus). */
object BoardingStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
    const val ABSENT = "absent"

    fun label(status: String) = when (status) {
        PENDING -> "Pendente"
        CONFIRMED -> "Embarcado"
        ABSENT -> "Ausente"
        else -> status
    }
}
