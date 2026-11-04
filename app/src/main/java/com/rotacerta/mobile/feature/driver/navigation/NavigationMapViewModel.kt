package com.rotacerta.mobile.feature.driver.navigation

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.android.gms.maps.model.LatLng
import com.rotacerta.mobile.core.location.GeoPoint
import com.rotacerta.mobile.core.location.LocationFix
import com.rotacerta.mobile.core.location.LocationHelper
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.util.PolylineDecoder
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.NavigationResponseDto
import com.rotacerta.mobile.data.dto.RouteEventType
import com.rotacerta.mobile.data.repository.DriverRepository
import com.rotacerta.mobile.feature.driver.TripProgress
import com.rotacerta.mobile.feature.driver.tracking.TrackingController
import com.rotacerta.mobile.feature.driver.tracking.TrackingState
import com.rotacerta.mobile.navigation.NavigationMapRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NavigationUiState(
    val navigation: UiState<NavigationResponseDto> = UiState.Loading,
    val polyline: List<LatLng> = emptyList(),
    val position: LocationFix? = null,
    val progress: NavigationProgress? = null,
    val routeName: String = "",
    /** Há partida registrada sem chegada: a rota está em andamento no backend. */
    val tripInProgress: Boolean = false,
    val recalculating: Boolean = false,
    val registeringStop: Boolean = false,
    /** false enquanto não há leitura do GPS (sem permissão ou sem sinal). */
    val hasLocation: Boolean = false,
)

@HiltViewModel
class NavigationMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DriverRepository,
    private val locationHelper: LocationHelper,
    private val trackingController: TrackingController,
) : ViewModel() {
    private val routeId = savedStateHandle.toRoute<NavigationMapRoute>().routeId

    private val _state = MutableStateFlow(NavigationUiState())
    val state: StateFlow<NavigationUiState> = _state.asStateFlow()

    /** Envio do GPS aos passageiros (serviço de tracking) ligado para esta rota. */
    val tracking: StateFlow<TrackingState> = trackingController.state
    val isTrackingThisRoute: Boolean get() = trackingController.activeRouteId() == routeId

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var guide: NavigationGuide? = null
    private var offRouteFixes = 0
    private var lastCalculationAt = 0L
    private var recalculatedFromGps = false

    init {
        viewModelScope.launch {
            val origin = (tracking.value as? TrackingState.Active)?.lastLocation ?: locationHelper.currentLocation()
            calculate(origin)
        }
    }

    /** Coleta o GPS enquanto a tela está visível (chamado com repeatOnLifecycle). */
    suspend fun followLocation() {
        locationHelper.locationUpdates().collect(::onFix)
    }

    fun recalculate() = viewModelScope.launch { calculate(_state.value.position?.point ?: locationHelper.currentLocation()) }

    /** Registra o evento "parada" na posição atual e recalcula a partir dela, já sem essa parada. */
    fun registerStop() = viewModelScope.launch {
        if (_state.value.registeringStop) return@launch
        val stop = _state.value.progress?.nextStop
        val location = _state.value.position?.point ?: locationHelper.currentLocation()
        if (location == null) {
            _messages.tryEmit("Não foi possível obter sua localização.")
            return@launch
        }
        _state.update { it.copy(registeringStop = true) }
        when (val result = repository.registerEvent(routeId, RouteEventType.STOP, location)) {
            is ApiResult.Success -> {
                _messages.tryEmit(stop?.let { "Parada em ${it.title} registrada." } ?: "Parada registrada.")
                calculate(location)
            }
            is ApiResult.Failure -> _messages.tryEmit(result.message)
        }
        _state.update { it.copy(registeringStop = false) }
    }

    /** Volta a enviar o GPS aos passageiros (a permissão já foi concedida, senão não haveria posição). */
    fun resumeTracking() {
        trackingController.start(routeId, _state.value.routeName)
        _messages.tryEmit("Envio da localização retomado.")
    }

    private fun onFix(fix: LocationFix) {
        val previous = _state.value
        val progress = guide?.progress(fix.point, previous.progress?.alongMeters)
        _state.update { it.copy(position = fix, progress = progress, hasLocation = true) }

        val navigation = (previous.navigation as? UiState.Success)?.data ?: return
        // Calculada a partir do endereço do motorista (sem GPS na abertura): refaz uma vez a partir da posição real.
        if (navigation.originSource != "gps") {
            if (!recalculatedFromGps) {
                recalculatedFromGps = true
                recalculateFrom(fix.point)
            }
            return
        }
        offRouteFixes = if (progress?.isOffRoute == true) offRouteFixes + 1 else 0
        if (offRouteFixes >= OFF_ROUTE_FIXES_TO_RECALCULATE &&
            SystemClock.elapsedRealtime() - lastCalculationAt >= MIN_RECALCULATION_INTERVAL_MS
        ) {
            _messages.tryEmit("Você saiu da rota. Recalculando…")
            recalculateFrom(fix.point)
        }
    }

    private fun recalculateFrom(origin: GeoPoint) {
        if (_state.value.recalculating) return
        viewModelScope.launch { calculate(origin) }
    }

    private suspend fun calculate(origin: GeoPoint?) {
        _state.update { it.copy(recalculating = true) }
        lastCalculationAt = SystemClock.elapsedRealtime()
        offRouteFixes = 0
        val (detail, events) = coroutineScope {
            val detail = async { repository.routeDetail(routeId) }
            val events = async { repository.routeEvents(routeId) }
            detail.await() to events.await()
        }
        val route = (detail as? ApiResult.Success)?.data
        val eventList = (events as? ApiResult.Success)?.data.orEmpty()
        val doneStops = route?.let { TripProgress.completedStopIds(it.stops, eventList) }.orEmpty()

        when (val result = repository.navigation(routeId, origin, doneStops)) {
            is ApiResult.Success -> {
                val geoPolyline = PolylineDecoder.decode(result.data.encodedPolyline)
                val newGuide = NavigationGuide(result.data, geoPolyline.map { GeoPoint(it.latitude, it.longitude) })
                guide = newGuide
                _state.update { s ->
                    s.copy(
                        navigation = UiState.Success(result.data),
                        polyline = geoPolyline,
                        progress = s.position?.let { newGuide.progress(it.point) },
                        routeName = route?.name ?: s.routeName,
                        tripInProgress = TripProgress.inProgress(eventList),
                    )
                }
            }
            is ApiResult.Failure ->
                if (_state.value.navigation is UiState.Success) {
                    _messages.tryEmit("Não foi possível recalcular a rota. ${result.message}")
                } else {
                    _state.update { it.copy(navigation = UiState.Error(result.message)) }
                }
        }
        _state.update { it.copy(recalculating = false) }
    }

    private companion object {
        /** Leituras seguidas fora da linha antes de recalcular (evita recalcular por um GPS ruim). */
        const val OFF_ROUTE_FIXES_TO_RECALCULATE = 3

        /** Cada recálculo é uma chamada paga à Routes API. */
        const val MIN_RECALCULATION_INTERVAL_MS = 30_000L
    }
}
