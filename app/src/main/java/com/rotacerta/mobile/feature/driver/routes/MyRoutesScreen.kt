package com.rotacerta.mobile.feature.driver.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.core.designsystem.EmptyState
import com.rotacerta.mobile.core.designsystem.ErrorState
import com.rotacerta.mobile.core.designsystem.LoadingState
import com.rotacerta.mobile.core.designsystem.RcCard
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.ScreenTitle
import com.rotacerta.mobile.core.designsystem.StatusPill
import com.rotacerta.mobile.core.designsystem.routeStatusColor
import com.rotacerta.mobile.core.designsystem.routeStatusLabel
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.realtime.RealtimeClient
import com.rotacerta.mobile.core.realtime.RealtimeEvent
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.DriverRouteDto
import com.rotacerta.mobile.data.repository.DriverRepository
import com.rotacerta.mobile.feature.driver.tracking.TrackingController
import com.rotacerta.mobile.feature.driver.tracking.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyRoutesUiState(
    val routes: UiState<List<DriverRouteDto>> = UiState.Loading,
    val refreshing: Boolean = false,
)

@HiltViewModel
class MyRoutesViewModel @Inject constructor(
    private val repository: DriverRepository,
    realtimeClient: RealtimeClient,
    trackingController: TrackingController,
) : ViewModel() {
    private val _state = MutableStateFlow(MyRoutesUiState())
    val state: StateFlow<MyRoutesUiState> = _state.asStateFlow()
    val tracking: StateFlow<TrackingState> = trackingController.state

    init {
        load()
        viewModelScope.launch { realtimeClient.events.collect(::apply) }
    }

    fun load(refreshing: Boolean = false) = viewModelScope.launch {
        _state.update { it.copy(refreshing = refreshing) }
        when (val result = repository.myRoutes()) {
            is ApiResult.Success -> _state.update { MyRoutesUiState(UiState.Success(result.data)) }
            is ApiResult.Failure -> _state.update {
                if (it.routes is UiState.Success) it.copy(refreshing = false) else MyRoutesUiState(UiState.Error(result.message))
            }
        }
    }

    /** Status da rota e confirmações chegam pelo WebSocket, sem polling. */
    private fun apply(event: RealtimeEvent) = _state.update { current ->
        val routes = (current.routes as? UiState.Success)?.data ?: return@update current
        val updated = when (event) {
            is RealtimeEvent.RouteEventOccurred -> routes.map {
                if (it.id == event.routeId) it.copy(routeStatus = event.routeStatus) else it
            }
            is RealtimeEvent.BoardingStatusChanged -> routes.map { route ->
                if (route.id != event.routeId) route
                else route.copy(passengers = route.passengers.map { p -> if (p.id == event.routePassengerId) p.copy(status = event.status) else p })
            }
            else -> routes
        }
        current.copy(routes = UiState.Success(updated))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRoutesScreen(
    onOpenRoute: (Long) -> Unit,
    viewModel: MyRoutesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Minhas rotas", subtitle = "Toque em uma rota para ver paradas e passageiros")
        (tracking as? TrackingState.Active)?.let { TrackingBanner(it) { onOpenRoute(it.routeId) } }
        when (val routes = state.routes) {
            UiState.Idle, UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(routes.message, { viewModel.load() })
            is UiState.Success -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.load(refreshing = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (routes.data.isEmpty()) {
                    EmptyState(
                        Icons.AutoMirrored.Outlined.AltRoute,
                        "Nenhuma rota atribuída",
                        "As rotas do veículo vinculado a você aparecem aqui. Fale com o gestor se esperava ver alguma.",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(routes.data, key = { it.id }) { route ->
                            RouteCard(route, isTracking = (tracking as? TrackingState.Active)?.routeId == route.id) {
                                onOpenRoute(route.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingBanner(active: TrackingState.Active, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(RcColors.Success.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.RadioButtonChecked, contentDescription = null, tint = RcColors.Success)
        Spacer(Modifier.width(10.dp))
        Text(
            "Em andamento: ${Formatters.beforeDash(active.routeName)}" + if (!active.lastSendOk) " · sem conexão" else "",
            color = RcColors.Success,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = RcColors.Success)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun RouteCard(route: DriverRouteDto, isTracking: Boolean, onClick: () -> Unit) {
    RcCard(
        modifier = Modifier.clickable(onClick = onClick),
        borderColor = if (isTracking) RcColors.Success.copy(alpha = 0.6f) else null,
    ) {
        route.schedule?.let {
            val days = route.serviceDate?.let { date -> "Rota extra ${Formatters.isoDateShort(date)}" }
                ?: Formatters.weekdays(it.weekdays)
            Metric(
                Icons.Outlined.Schedule,
                "Chegada às ${it.time} · $days",
                if (route.serviceDate != null) RcColors.Warning else RcColors.Primary,
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Formatters.beforeDash(route.name),
                style = MaterialTheme.typography.titleMedium,
                color = RcColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            StatusPill(routeStatusLabel(route.routeStatus), routeStatusColor(route.routeStatus))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric(Icons.Outlined.Place, "${route.stops.size} paradas")
            Metric(Icons.Outlined.People, "${route.confirmedCount}/${route.passengers.size} confirmados", RcColors.Success)
        }
        route.vehicle?.let {
            Spacer(Modifier.height(6.dp))
            Metric(Icons.Outlined.DirectionsBus, "${it.model} · ${it.plateNumber}")
        }
    }
}

@Composable
private fun Metric(icon: ImageVector, text: String, color: Color = RcColors.TextSecondary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
