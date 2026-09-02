package com.rotacerta.driver.data.remote.api

import com.rotacerta.driver.data.remote.dto.DriverRouteBoardDto
import com.rotacerta.driver.data.remote.dto.NavigationResponseDto
import com.rotacerta.driver.data.remote.dto.RouteEventDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface RouteApi {

    /** Rotas do motorista logado (RouteViewSet.my_routes). */
    @GET("routes/my-routes/")
    suspend fun myRoutes(): Response<List<DriverRouteBoardDto>>

    /** Detalhe completo de uma rota do motorista logado (RouteViewSet.driver_detail). */
    @GET("routes/{id}/driver-detail/")
    suspend fun driverDetail(@Path("id") routeId: Long): Response<DriverRouteBoardDto>

    /** Histórico de eventos da rota, mais recentes primeiro (RouteViewSet.events). */
    @GET("routes/{id}/events/")
    suspend fun routeEvents(@Path("id") routeId: Long): Response<List<RouteEventDto>>

    /** Traçado/instrução de navegação até a rota (RouteViewSet.navigation). */
    @GET("routes/{id}/navigation/")
    suspend fun navigation(@Path("id") routeId: Long): Response<NavigationResponseDto>
}
