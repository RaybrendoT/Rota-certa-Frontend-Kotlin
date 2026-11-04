package com.rotacerta.mobile.feature.collaborator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DoNotDisturbAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.rotacerta.mobile.core.designsystem.BusMarker
import com.rotacerta.mobile.core.designsystem.EmptyState
import com.rotacerta.mobile.core.designsystem.ErrorState
import com.rotacerta.mobile.core.designsystem.FallbackMapCenter
import com.rotacerta.mobile.core.designsystem.LoadingState
import com.rotacerta.mobile.core.designsystem.MessageSnackbar
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcLinkButton
import com.rotacerta.mobile.core.designsystem.RcOutlinedButton
import com.rotacerta.mobile.core.designsystem.RcPrimaryButton
import com.rotacerta.mobile.core.designsystem.StatusPill
import com.rotacerta.mobile.core.designsystem.StopMarker
import com.rotacerta.mobile.core.designsystem.rememberDarkMapStyle
import com.rotacerta.mobile.core.designsystem.routeStatusColor
import com.rotacerta.mobile.core.designsystem.routeStatusLabel
import com.rotacerta.mobile.core.location.Geo
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.BoardingStatus
import com.rotacerta.mobile.data.dto.CollaboratorRouteDto
import com.rotacerta.mobile.data.dto.RouteStatus
import kotlinx.coroutines.launch

@Composable
fun CollaboratorMapScreen(viewModel: CollaboratorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val routes = state.routes) {
            UiState.Idle, UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(routes.message, { viewModel.refresh() })
            is UiState.Success -> {
                val selected = state.selected
                if (selected == null) {
                    EmptyState(
                        Icons.Outlined.Route,
                        "Você ainda não está em uma rota",
                        "Quando o gestor vincular você a uma rota, seu ponto de embarque e o ônibus aparecem aqui.",
                    )
                } else {
                    RouteMap(selected)
                    Column(Modifier.statusBarsPadding().padding(12.dp)) {
                        if (routes.data.size > 1) RouteSelector(routes.data, selected.id, viewModel::select)
                        BusStatusBanner(selected, state.proximity)
                    }
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        val answered = selected.boardingStatus != BoardingStatus.PENDING
                        if (!answered || state.editingAnswer) {
                            ConfirmPresenceCard(
                                route = selected,
                                sending = state.sendingId == selected.id,
                                canCancel = answered,
                                onRespond = { viewModel.respond(selected.id, it) },
                                onCancel = { viewModel.editAnswer(false) },
                            )
                        } else {
                            AnswerSummary(selected, onEdit = { viewModel.editAnswer(true) })
                        }
                    }
                }
            }
        }
        MessageSnackbar(viewModel.messages, Modifier.padding(bottom = 180.dp))
    }
}

@Composable
private fun RouteMap(route: CollaboratorRouteDto) {
    val stop = route.stop?.let { LatLng(it.latitude, it.longitude) }
    val bus = route.lastLocation?.let { LatLng(it.latitude, it.longitude) }
    val cameraState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Enquadra parada + ônibus ao abrir e ao trocar de rota (não a cada posição, para não brigar com o gesto).
    LaunchedEffect(mapLoaded, route.id) {
        if (mapLoaded) fitCamera(cameraState, stop, bus)
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            onMapLoaded = { mapLoaded = true },
            properties = MapProperties(mapStyleOptions = rememberDarkMapStyle()),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false),
        ) {
            stop?.let {
                MarkerComposable(
                    state = rememberUpdatedMarkerState(position = it),
                    title = route.stop.title,
                ) { StopMarker() }
            }
            bus?.let {
                MarkerComposable(
                    state = rememberUpdatedMarkerState(position = it),
                    title = "Ônibus",
                ) { BusMarker() }
            }
        }
        SmallFloatingActionButton(
            onClick = { scope.launch { fitCamera(cameraState, stop, bus) } },
            containerColor = RcColors.Card,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp),
        ) { Icon(Icons.Outlined.MyLocation, contentDescription = "Centralizar") }
    }
}

private suspend fun fitCamera(state: CameraPositionState, stop: LatLng?, bus: LatLng?) {
    val points = listOfNotNull(stop, bus)
    val update = when (points.size) {
        0 -> CameraUpdateFactory.newLatLngZoom(FallbackMapCenter, 12f)
        1 -> CameraUpdateFactory.newLatLngZoom(points.first(), 16f)
        else -> CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.builder().apply { points.forEach(::include) }.build(),
            220,
        )
    }
    runCatching { state.animate(update) }
}

@Composable
private fun RouteSelector(routes: List<CollaboratorRouteDto>, selectedId: Long, onSelect: (Long) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        routes.forEach { route ->
            FilterChip(
                selected = route.id == selectedId,
                onClick = { onSelect(route.id) },
                label = { Text(Formatters.beforeDash(route.route.name)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = RcColors.Card,
                    labelColor = RcColors.TextSecondary,
                    selectedContainerColor = RcColors.Primary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun BusStatusBanner(route: CollaboratorRouteDto, proximity: BusProximity?) {
    val text = when {
        route.stop == null -> "Seu ponto de embarque ainda não foi definido. Fale com o gestor."
        proximity != null -> buildString {
            append("Ônibus a ${Geo.formatDistance(proximity.distanceMeters)} do seu ponto")
            proximity.etaMinutes?.let { append(" · ~$it min") }
        }
        route.route.routeStatus == RouteStatus.WAITING -> "O ônibus ainda não saiu."
        else -> "Aguardando a localização do ônibus…"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(RcColors.Card.copy(alpha = 0.94f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.DirectionsBus, contentDescription = null, tint = RcColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = RcColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            route.lastLocation?.let {
                Text(
                    "Atualizado às ${Formatters.localTime(it.recordedAt)}",
                    color = RcColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        StatusPill(routeStatusLabel(route.route.routeStatus), routeStatusColor(route.route.routeStatus))
    }
}

@Composable
private fun ConfirmPresenceCard(
    route: CollaboratorRouteDto,
    sending: Boolean,
    canCancel: Boolean,
    onRespond: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(RcColors.Card, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(20.dp),
    ) {
        Text("Confirmar presença", style = MaterialTheme.typography.titleMedium, color = RcColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RcColors.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                listOfNotNull(route.stop?.title?.let(Formatters::beforeDash), Formatters.beforeDash(route.route.name)).joinToString(" · "),
                color = RcColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RcPrimaryButton(
                "Vou embarcar",
                onClick = { onRespond(true) },
                loading = sending,
                icon = Icons.Filled.Check,
                modifier = Modifier.weight(1f),
            )
            RcOutlinedButton(
                "Não irei hoje",
                onClick = { onRespond(false) },
                enabled = !sending,
                color = RcColors.Warning,
                icon = Icons.Filled.Close,
                modifier = Modifier.weight(1f).height(56.dp),
            )
        }
        if (canCancel) {
            Spacer(Modifier.height(4.dp))
            RcLinkButton("Manter resposta atual", onCancel)
        }
    }
}

@Composable
private fun AnswerSummary(route: CollaboratorRouteDto, onEdit: () -> Unit) {
    val confirmed = route.boardingStatus == BoardingStatus.CONFIRMED
    val color = if (confirmed) RcColors.Primary else RcColors.Warning
    Column(
        Modifier
            .fillMaxWidth()
            .background(RcColors.Card, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (confirmed) Icons.Outlined.DirectionsBus else Icons.Outlined.DoNotDisturbAlt,
                contentDescription = null,
                tint = color,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (confirmed) "Você confirmou embarque · ${Formatters.beforeDash(route.route.name)}"
                else "Você não irá hoje · motorista avisado",
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(14.dp))
        RcOutlinedButton("Alterar resposta", onEdit, icon = Icons.Outlined.Edit, modifier = Modifier.fillMaxWidth())
    }
}
