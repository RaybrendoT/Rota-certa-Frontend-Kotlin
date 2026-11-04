package com.rotacerta.mobile.feature.driver.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rotacerta.mobile.core.location.GeoPoint
import com.rotacerta.mobile.core.location.LocationHelper
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.realtime.RealtimeClient
import com.rotacerta.mobile.core.realtime.RealtimeEvent
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.DriverRouteDto
import com.rotacerta.mobile.data.dto.RouteEventDto
import com.rotacerta.mobile.data.dto.RouteEventType
import com.rotacerta.mobile.data.repository.DriverRepository
import com.rotacerta.mobile.feature.driver.TripProgress
import com.rotacerta.mobile.feature.driver.tracking.TrackingController
import com.rotacerta.mobile.feature.driver.tracking.TrackingState
import com.rotacerta.mobile.navigation.RouteDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: UiState<DriverRouteDto> = UiState.Loading,
    val events: List<RouteEventDto> = emptyList(),
    /** Paradas com evento "stop" registrado perto delas. */
    val completedStopIds: Set<Long> = emptySet(),
    val updatingPassengerId: Long? = null,
    /** Iniciar/finalizar/registrar evento em andamento. */
    val busy: Boolean = false,
) {
    val tripInProgress: Boolean get() = TripProgress.inProgress(events)
}

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DriverRepository,
    private val locationHelper: LocationHelper,
    private val trackingController: TrackingController,
    realtimeClient: RealtimeClient,
) : ViewModel() {

    val routeId: Long = savedStateHandle.toRoute<RouteDetailRoute>().routeId

    private val _state = MutableStateFlow(RouteDetailUiState())
    val state: StateFlow<RouteDetailUiState> = _state.asStateFlow()
    val tracking: StateFlow<TrackingState> = trackingController.state

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        load()
        viewModelScope.launch { realtimeClient.events.collect(::apply) }
    }

    fun load() = viewModelScope.launch {
        when (val result = repository.routeDetail(routeId)) {
            is ApiResult.Success -> _state.update { it.copy(route = UiState.Success(result.data)) }
            is ApiResult.Failure -> _state.update {
                if (it.route is UiState.Success) it else it.copy(route = UiState.Error(result.message))
            }
        }
        loadEvents()
    }

    private suspend fun loadEvents() {
        val events = (repository.routeEvents(routeId) as? ApiResult.Success)?.data ?: return
        _state.update { it.copy(events = events, completedStopIds = completedStops(it.route, events)) }
    }

    val isTrackingThisRoute: Boolean get() = trackingController.activeRouteId() == routeId

    /** Chamar só com a permissão de localização concedida. */
    fun startRoute() = runBusy {
        val route = currentRoute() ?: return@runBusy
        val otherRoute = trackingController.activeRouteId()
        if (otherRoute != null && otherRoute != routeId) {
            _messages.tryEmit("Finalize a rota em andamento antes de iniciar outra.")
            return@runBusy
        }
        val location = currentLocationOrWarn() ?: return@runBusy
        when (val result = repository.registerEvent(routeId, RouteEventType.DEPARTURE, location)) {
            is ApiResult.Success -> {
                trackingController.start(routeId, route.name)
                _messages.tryEmit("Rota iniciada. Os passageiros estão vendo o ônibus no mapa.")
                loadEvents()
            }
            is ApiResult.Failure -> _messages.tryEmit(result.message)
        }
    }

    /** Rota já iniciada (ex.: o app foi fechado): volta a enviar o GPS sem registrar outra partida. */
    fun resumeTracking() {
        val route = currentRoute() ?: return
        trackingController.start(routeId, route.name)
        _messages.tryEmit("Envio da localização retomado.")
    }

    fun finishRoute() = runBusy {
        val location = (tracking.value as? TrackingState.Active)?.lastLocation ?: currentLocationOrWarn() ?: return@runBusy
        when (val result = repository.registerEvent(routeId, RouteEventType.ARRIVAL, location)) {
            is ApiResult.Success -> {
                trackingController.stop()
                _messages.tryEmit("Rota finalizada.")
                loadEvents()
            }
            is ApiResult.Failure -> _messages.tryEmit(result.message)
        }
    }

    fun registerEvent(type: String, description: String) = runBusy {
        if (RouteEventType.requiresDescription(type) && description.isBlank()) {
            _messages.tryEmit("Descreva o ${RouteEventType.label(type).lowercase()}.")
            return@runBusy
        }
        val location = (tracking.value as? TrackingState.Active)?.lastLocation ?: currentLocationOrWarn() ?: return@runBusy
        when (val result = repository.registerEvent(routeId, type, location, description)) {
            is ApiResult.Success -> {
                _messages.tryEmit("${RouteEventType.label(type)} registrado.")
                loadEvents()
            }
            is ApiResult.Failure -> _messages.tryEmit(result.message)
        }
    }

    fun updateBoardingStatus(routePassengerId: Long, status: String) = viewModelScope.launch {
        val before = currentRoute() ?: return@launch
        _state.update { it.copy(updatingPassengerId = routePassengerId) }
        setPassengerStatus(routePassengerId, status) // otimista; o WebSocket confirma
        when (val result = repository.updateBoardingStatus(routePassengerId, status)) {
            is ApiResult.Success -> Unit
            is ApiResult.Failure -> {
                _state.update { it.copy(route = UiState.Success(before)) }
                _messages.tryEmit(result.message)
            }
        }
        _state.update { it.copy(updatingPassengerId = null) }
    }

    private fun apply(event: RealtimeEvent) {
        when (event) {
            is RealtimeEvent.BoardingStatusChanged ->
                if (event.routeId == routeId) setPassengerStatus(event.routePassengerId, event.status)
            is RealtimeEvent.RouteEventOccurred -> if (event.routeId == routeId) {
                _state.update { s ->
                    val route = (s.route as? UiState.Success)?.data ?: return@update s
                    s.copy(route = UiState.Success(route.copy(routeStatus = event.routeStatus)))
                }
            }
            is RealtimeEvent.Connected -> load() // pode ter perdido mensagens enquanto estava desconectado
            else -> Unit
        }
    }

    private fun setPassengerStatus(routePassengerId: Long, status: String) = _state.update { s ->
        val route = (s.route as? UiState.Success)?.data ?: return@update s
        s.copy(
            route = UiState.Success(
                route.copy(passengers = route.passengers.map { if (it.id == routePassengerId) it.copy(status = status) else it }),
            ),
        )
    }

    private fun currentRoute(): DriverRouteDto? = (_state.value.route as? UiState.Success)?.data

    private suspend fun currentLocationOrWarn(): GeoPoint? =
        locationHelper.currentLocation() ?: null.also {
            _messages.tryEmit("Não foi possível obter sua localização. Verifique o GPS e a permissão de localização.")
        }

    private fun runBusy(block: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                block()
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun completedStops(route: UiState<DriverRouteDto>, events: List<RouteEventDto>): Set<Long> {
        val stops = (route as? UiState.Success)?.data?.stops ?: return emptySet()
        return TripProgress.completedStopIds(stops, events)
    }
}
