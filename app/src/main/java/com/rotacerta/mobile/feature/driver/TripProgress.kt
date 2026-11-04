package com.rotacerta.mobile.feature.driver

import com.rotacerta.mobile.core.location.Geo
import com.rotacerta.mobile.data.dto.RouteEventDto
import com.rotacerta.mobile.data.dto.RouteEventType
import com.rotacerta.mobile.data.dto.RouteStopDto

/** Progresso da viagem atual a partir dos eventos da rota (a lista vem do mais novo ao mais antigo). */
object TripProgress {
    /** Mesmo raio que o backend usa para "ônibus no ponto" (core/notifications.py). */
    const val STOP_DONE_RADIUS_METERS = 200.0

    /** Há uma partida mais recente que a última chegada. */
    fun inProgress(events: List<RouteEventDto>): Boolean {
        val departure = events.indexOfFirst { it.eventType == RouteEventType.DEPARTURE }
        val arrival = events.indexOfFirst { it.eventType == RouteEventType.ARRIVAL }
        return departure >= 0 && (arrival < 0 || departure < arrival)
    }

    /** Paradas com evento "stop" registrado perto delas depois da última partida. */
    fun completedStopIds(stops: List<RouteStopDto>, events: List<RouteEventDto>): Set<Long> {
        if (events.none { it.eventType == RouteEventType.DEPARTURE }) return emptySet()
        val stopEvents = events.takeWhile { it.eventType != RouteEventType.DEPARTURE }
            .filter { it.eventType == RouteEventType.STOP }
        return stops.filter { stop ->
            val lat = stop.latitude ?: return@filter false
            val lng = stop.longitude ?: return@filter false
            stopEvents.any { Geo.distanceMeters(lat, lng, it.latitude, it.longitude) <= STOP_DONE_RADIUS_METERS }
        }.map { it.id }.toSet()
    }
}
