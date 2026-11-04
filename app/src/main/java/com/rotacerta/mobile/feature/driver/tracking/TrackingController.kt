package com.rotacerta.mobile.feature.driver.tracking

import android.content.Context
import androidx.core.content.ContextCompat
import com.rotacerta.mobile.core.location.GeoPoint
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TrackingState {
    data object Idle : TrackingState

    data class Active(
        val routeId: Long,
        val routeName: String,
        val lastLocation: GeoPoint? = null,
        val heading: Float? = null,
        /** false quando o último envio ao backend falhou (sem internet, por exemplo). */
        val lastSendOk: Boolean = true,
    ) : TrackingState
}

/** Liga/desliga o envio do GPS do motorista (LocationTrackingService) e expõe o estado para a UI. */
@Singleton
class TrackingController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    sessionStore: SessionStore,
    @ApplicationScope scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    init {
        // Saiu da conta: para de enviar a posição.
        scope.launch { sessionStore.session.collect { if (it == null && _state.value is TrackingState.Active) stop() } }
    }

    fun activeRouteId(): Long? = (_state.value as? TrackingState.Active)?.routeId

    /** Exige permissão de localização já concedida (o serviço encerra sem ela). */
    fun start(routeId: Long, routeName: String) {
        _state.value = TrackingState.Active(routeId, routeName)
        ContextCompat.startForegroundService(context, LocationTrackingService.startIntent(context, routeId, routeName))
    }

    fun stop() {
        // stopService (e não startService com ação STOP): funciona mesmo com o app em segundo plano.
        context.stopService(LocationTrackingService.stopIntent(context))
        _state.value = TrackingState.Idle
    }

    internal fun onServiceStarted(routeId: Long, routeName: String) {
        if ((_state.value as? TrackingState.Active)?.routeId != routeId) {
            _state.value = TrackingState.Active(routeId, routeName)
        }
    }

    internal fun onLocation(point: GeoPoint, heading: Float?) = _state.update { current ->
        if (current is TrackingState.Active) current.copy(lastLocation = point, heading = heading) else current
    }

    internal fun onSendResult(ok: Boolean) = _state.update { current ->
        if (current is TrackingState.Active) current.copy(lastSendOk = ok) else current
    }

    internal fun onServiceStopped() {
        _state.value = TrackingState.Idle
    }
}
