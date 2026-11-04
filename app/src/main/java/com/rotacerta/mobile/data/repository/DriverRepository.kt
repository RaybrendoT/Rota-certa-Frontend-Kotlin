package com.rotacerta.mobile.data.repository

import com.rotacerta.mobile.core.location.GeoPoint
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.network.apiCall
import com.rotacerta.mobile.core.network.map
import com.rotacerta.mobile.data.api.DriverApi
import com.rotacerta.mobile.data.dto.BoardingStatusRequest
import com.rotacerta.mobile.data.dto.DriverRouteDto
import com.rotacerta.mobile.data.dto.LocationUpdateRequest
import com.rotacerta.mobile.data.dto.NavigationResponseDto
import com.rotacerta.mobile.data.dto.RouteEventCreateRequest
import com.rotacerta.mobile.data.dto.RouteEventDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriverRepository @Inject constructor(
    private val api: DriverApi,
) {
    suspend fun myRoutes(): ApiResult<List<DriverRouteDto>> =
        apiCall("Não foi possível carregar suas rotas.") { api.myRoutes() }

    suspend fun routeDetail(routeId: Long): ApiResult<DriverRouteDto> =
        apiCall("Não foi possível carregar a rota.") { api.routeDetail(routeId) }

    suspend fun routeEvents(routeId: Long): ApiResult<List<RouteEventDto>> =
        apiCall("Não foi possível carregar o histórico de eventos.") { api.routeEvents(routeId) }

    suspend fun navigation(
        routeId: Long,
        origin: GeoPoint? = null,
        doneStopIds: Set<Long> = emptySet(),
    ): ApiResult<NavigationResponseDto> = apiCall("Não foi possível calcular a navegação desta rota.") {
        api.navigation(
            routeId,
            origin = origin?.let { "${it.latitude.round6()},${it.longitude.round6()}" },
            doneStops = doneStopIds.takeIf { it.isNotEmpty() }?.joinToString(","),
        )
    }

    suspend fun registerEvent(
        routeId: Long,
        eventType: String,
        location: GeoPoint,
        description: String = "",
    ): ApiResult<RouteEventDto> = apiCall("Não foi possível registrar o evento.") {
        api.createEvent(
            RouteEventCreateRequest(
                route = routeId,
                eventType = eventType,
                description = description.trim(),
                // O backend aceita no máximo 6 casas decimais.
                latitude = location.latitude.round6(),
                longitude = location.longitude.round6(),
            ),
        )
    }

    suspend fun updateBoardingStatus(routePassengerId: Long, status: String): ApiResult<Unit> =
        apiCall("Não foi possível atualizar o passageiro.") {
            api.updateBoardingStatus(routePassengerId, BoardingStatusRequest(status))
        }

    suspend fun sendLocation(routeId: Long, request: LocationUpdateRequest): ApiResult<Unit> =
        apiCall("Falha ao enviar a localização.") { api.sendLocation(routeId, request) }.map { }

    private fun Double.round6() = Math.round(this * 1_000_000.0) / 1_000_000.0
}
