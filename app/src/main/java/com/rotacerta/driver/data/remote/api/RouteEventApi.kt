package com.rotacerta.driver.data.remote.api

import com.rotacerta.driver.data.remote.dto.RouteEventCreateRequest
import com.rotacerta.driver.data.remote.dto.RouteEventDto
import com.rotacerta.driver.data.remote.dto.RoutePassengerStatusUpdateRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface RouteEventApi {

    /** Registra um evento de rota (partida, chegada, parada, atraso, incidente). */
    @POST("route-events/")
    suspend fun createEvent(@Body body: RouteEventCreateRequest): Response<RouteEventDto>
}

interface RoutePassengerApi {

    /** Atualiza o status de embarque de um passageiro (pending/confirmed/absent). */
    @PATCH("route-passengers/{id}/")
    suspend fun updateBoardingStatus(
        @Path("id") routePassengerId: Long,
        @Body body: RoutePassengerStatusUpdateRequest,
    ): Response<ResponseBody>
}
