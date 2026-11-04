package com.rotacerta.mobile.core.realtime

import com.rotacerta.mobile.data.dto.NotificationDto
import com.rotacerta.mobile.data.dto.VehicleLocationDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Mensagens do WebSocket /ws/stream/ (contrato em Specs/backend/03-api-rest.md, seção 7.3). */
sealed interface RealtimeEvent {
    data class Connected(val personId: Long) : RealtimeEvent
    data class NotificationReceived(val notification: NotificationDto) : RealtimeEvent
    data class VehicleLocation(val location: VehicleLocationDto) : RealtimeEvent
    data class BoardingStatusChanged(
        val routeId: Long,
        val routePassengerId: Long,
        val personId: Long,
        val status: String,
    ) : RealtimeEvent

    data class RouteEventOccurred(
        val routeId: Long,
        val eventType: String,
        val description: String,
        val routeStatus: String,
    ) : RealtimeEvent
}

@Serializable
private data class ConnectedMessage(@SerialName("person_id") val personId: Long)

@Serializable
private data class NotificationMessage(val notification: NotificationDto)

@Serializable
private data class BoardingStatusMessage(
    @SerialName("route_id") val routeId: Long,
    @SerialName("route_passenger_id") val routePassengerId: Long,
    @SerialName("person_id") val personId: Long,
    val status: String,
)

@Serializable
private data class RouteEventMessage(
    @SerialName("route_id") val routeId: Long,
    @SerialName("event_type") val eventType: String,
    val description: String = "",
    @SerialName("route_status") val routeStatus: String,
)

object RealtimeParser {
    /** Converte o texto recebido em evento; mensagens desconhecidas ou inválidas viram null. */
    fun parse(json: Json, text: String): RealtimeEvent? = runCatching {
        val element = json.parseToJsonElement(text) as? JsonObject ?: return null
        when (element["type"]?.jsonPrimitive?.content) {
            "connected" -> RealtimeEvent.Connected(json.decodeFromJsonElement(ConnectedMessage.serializer(), element).personId)
            "notification" -> RealtimeEvent.NotificationReceived(
                json.decodeFromJsonElement(NotificationMessage.serializer(), element).notification,
            )
            "vehicle_location" -> RealtimeEvent.VehicleLocation(
                json.decodeFromJsonElement(VehicleLocationDto.serializer(), element),
            )
            "boarding_status" -> json.decodeFromJsonElement(BoardingStatusMessage.serializer(), element).let {
                RealtimeEvent.BoardingStatusChanged(it.routeId, it.routePassengerId, it.personId, it.status)
            }
            "route_event" -> json.decodeFromJsonElement(RouteEventMessage.serializer(), element).let {
                RealtimeEvent.RouteEventOccurred(it.routeId, it.eventType, it.description, it.routeStatus)
            }
            else -> null // "pong" e tipos futuros
        }
    }.getOrNull()
}
