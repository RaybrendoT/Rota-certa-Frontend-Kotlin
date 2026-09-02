package com.rotacerta.driver.data.repository

import com.rotacerta.driver.data.remote.api.RouteApi
import com.rotacerta.driver.data.remote.api.RouteEventApi
import com.rotacerta.driver.data.remote.api.RoutePassengerApi
import com.rotacerta.driver.data.remote.dto.DriverRouteBoardDto
import com.rotacerta.driver.data.remote.dto.NavigationResponseDto
import com.rotacerta.driver.data.remote.dto.RouteEventCreateRequest
import com.rotacerta.driver.data.remote.dto.RouteEventDto
import com.rotacerta.driver.data.remote.dto.RoutePassengerStatusUpdateRequest
import com.rotacerta.driver.util.extractErrorMessage
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val message: String) : DataResult<Nothing>
}

@Singleton
class RouteRepository @Inject constructor(
    private val routeApi: RouteApi,
    private val routeEventApi: RouteEventApi,
    private val routePassengerApi: RoutePassengerApi,
) {
    suspend fun myRoutes(): DataResult<List<DriverRouteBoardDto>> = safeCall(
        fallback = "Não foi possível carregar suas rotas.",
    ) { routeApi.myRoutes() }

    suspend fun routeDetail(routeId: Long): DataResult<DriverRouteBoardDto> = safeCall(
        fallback = "Não foi possível carregar a rota.",
    ) { routeApi.driverDetail(routeId) }

    suspend fun routeEvents(routeId: Long): DataResult<List<RouteEventDto>> = safeCall(
        fallback = "Não foi possível carregar o histórico de eventos.",
    ) { routeApi.routeEvents(routeId) }

    suspend fun navigation(routeId: Long): DataResult<NavigationResponseDto> = safeCall(
        fallback = "Não foi possível calcular a navegação para esta rota.",
    ) { routeApi.navigation(routeId) }

    suspend fun registerEvent(
        routeId: Long,
        eventType: String,
        description: String,
        latitude: Double,
        longitude: Double,
    ): DataResult<RouteEventDto> = safeCall(
        fallback = "Não foi possível registrar o evento.",
    ) {
        routeEventApi.createEvent(
            RouteEventCreateRequest(
                route = routeId,
                eventType = eventType,
                description = description,
                latitude = latitude,
                longitude = longitude,
            ),
        )
    }

    suspend fun updateBoardingStatus(routePassengerId: Long, status: String): DataResult<Unit> = safeCall(
        fallback = "Não foi possível atualizar o status do passageiro.",
    ) { routePassengerApi.updateBoardingStatus(routePassengerId, RoutePassengerStatusUpdateRequest(status)) }
        .let { result ->
            when (result) {
                is DataResult.Success -> DataResult.Success(Unit)
                is DataResult.Failure -> result
            }
        }

    private suspend fun <T> safeCall(
        fallback: String,
        block: suspend () -> retrofit2.Response<T>,
    ): DataResult<T> {
        return try {
            val response = block()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                DataResult.Success(body)
            } else {
                DataResult.Failure(response.extractErrorMessage(fallback))
            }
        } catch (e: Exception) {
            DataResult.Failure("Não foi possível conectar ao servidor. Verifique sua internet.")
        }
    }
}
