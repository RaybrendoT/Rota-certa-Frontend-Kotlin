package com.rotacerta.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Aviso (GET /notifications/ e mensagem "notification" do WebSocket). */
@Serializable
data class NotificationDto(
    val id: Long,
    val kind: String,
    val title: String,
    val message: String = "",
    @SerialName("route_id") val routeId: Long? = null,
    @SerialName("created_at") val createdAt: String,
    val read: Boolean = false,
)

@Serializable
data class NotificationPageDto(
    val count: Int = 0,
    val next: String? = null,
    val results: List<NotificationDto> = emptyList(),
)

@Serializable
data class ReadAllResponse(val updated: Int = 0)

/** Notification.Kind do backend. */
object NotificationKind {
    const val ROUTE_DEPARTURE = "route_departure"
    const val ROUTE_ARRIVAL = "route_arrival"
    const val ROUTE_DELAY = "route_delay"
    const val ROUTE_INCIDENT = "route_incident"
    const val BUS_APPROACHING = "bus_approaching"
    const val BUS_AT_STOP = "bus_at_stop"
    const val BOARDING_CONFIRMED = "boarding_confirmed"
    const val BOARDING_ABSENT = "boarding_absent"
    const val ROUTE_ASSIGNED = "route_assigned"
    const val ADDRESS_CHANGED = "address_changed"
    const val ADDRESS_APPROVED = "address_approved"
    const val ADDRESS_REJECTED = "address_rejected"
    const val PICKUP_CHANGED = "pickup_changed"
    const val ROUTE_UPDATED = "route_updated"
}
