package com.rotacerta.mobile.feature.driver.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RampLeft
import androidx.compose.material.icons.filled.RampRight
import androidx.compose.material.icons.filled.RoundaboutLeft
import androidx.compose.material.icons.filled.RoundaboutRight
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.UTurnRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.rotacerta.mobile.core.designsystem.BusMarker
import com.rotacerta.mobile.core.designsystem.DestinationMarker
import com.rotacerta.mobile.core.designsystem.ErrorState
import com.rotacerta.mobile.core.designsystem.LoadingState
import com.rotacerta.mobile.core.designsystem.MessageSnackbar
import com.rotacerta.mobile.core.designsystem.NumberedStopMarker
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcOutlinedButton
import com.rotacerta.mobile.core.designsystem.RcPrimaryButton
import com.rotacerta.mobile.core.designsystem.StopMarker
import com.rotacerta.mobile.core.designsystem.StopMarkerState
import com.rotacerta.mobile.core.designsystem.rememberDarkMapStyle
import com.rotacerta.mobile.core.location.Geo
import com.rotacerta.mobile.core.location.LocationHelper
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.NavigationResponseDto
import com.rotacerta.mobile.data.dto.NavigationStepDto
import com.rotacerta.mobile.feature.driver.tracking.TrackingState
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun NavigationMapScreen(
    onBack: () -> Unit,
    viewModel: NavigationMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()
    val sendingToPassengers = (tracking as? TrackingState.Active)?.let { viewModel.isTrackingThisRoute } == true

    // GPS só enquanto a tela está visível; reinicia depois que a permissão é concedida.
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGrants by remember { mutableIntStateOf(0) }
    LaunchedEffect(lifecycleOwner, permissionGrants) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followLocation() }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) permissionGrants++
    }

    Box(Modifier.fillMaxSize().background(RcColors.Background)) {
        when (val navigation = state.navigation) {
            UiState.Idle, UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(navigation.message, { viewModel.recalculate() })
            is UiState.Success -> NavigationContent(
                navigation = navigation.data,
                state = state,
                sendingToPassengers = sendingToPassengers,
                onBack = onBack,
                onRecalculate = { viewModel.recalculate() },
                onRegisterStop = { viewModel.registerStop() },
                onResumeTracking = { viewModel.resumeTracking() },
                onRequestPermission = { permissionLauncher.launch(LocationHelper.LOCATION_PERMISSIONS) },
            )
        }
        if (state.navigation !is UiState.Success) {
            MapButton(onBack, Modifier.statusBarsPadding().padding(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
        }
        MessageSnackbar(viewModel.messages, Modifier.navigationBarsPadding().padding(bottom = 240.dp))
    }
}

@Composable
private fun NavigationContent(
    navigation: NavigationResponseDto,
    state: NavigationUiState,
    sendingToPassengers: Boolean,
    onBack: () -> Unit,
    onRecalculate: () -> Unit,
    onRegisterStop: () -> Unit,
    onResumeTracking: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val cameraState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    var mapLoaded by remember { mutableStateOf(false) }
    var following by remember { mutableStateOf(true) }
    val bus = state.position?.point?.let { LatLng(it.latitude, it.longitude) }
    // Mantém a última direção conhecida: parado, o GPS não informa direção.
    var bearing by remember { mutableStateOf(0f) }
    state.position?.heading?.takeIf { (state.position.speed ?: 0f) > 1.5f }?.let { bearing = it }

    val overviewPoints = remember(state.polyline) {
        state.polyline.ifEmpty {
            listOf(navigation.waypoints.origin, navigation.waypoints.destination).map { LatLng(it.latitude, it.longitude) }
        }
    }
    LaunchedEffect(mapLoaded) {
        if (mapLoaded && bus == null) fitRoute(cameraState, overviewPoints)
    }
    // Segue o ônibus enquanto o motorista não mexe no mapa.
    LaunchedEffect(mapLoaded, bus, following, bearing) {
        if (mapLoaded && following && bus != null) {
            runCatching {
                cameraState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder().target(bus).zoom(17f).tilt(50f).bearing(bearing).build(),
                    ),
                    durationMs = 800,
                )
            }
        }
    }
    LaunchedEffect(cameraState.isMoving, cameraState.cameraMoveStartedReason) {
        if (cameraState.isMoving && cameraState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            following = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            onMapLoaded = { mapLoaded = true },
            properties = MapProperties(mapStyleOptions = rememberDarkMapStyle()),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = false),
        ) {
            RouteLines(state.polyline, state.progress, bus)
            navigation.stops.forEachIndexed { index, stop ->
                val markerState = when {
                    stop.done -> StopMarkerState.DONE
                    stop.id == (state.progress?.nextStop?.id ?: navigation.stops.firstOrNull { !it.done }?.id) -> StopMarkerState.NEXT
                    else -> StopMarkerState.PENDING
                }
                MarkerComposable(
                    keys = arrayOf<Any>(stop.id, markerState),
                    state = rememberUpdatedMarkerState(LatLng(stop.latitude, stop.longitude)),
                    title = "${index + 1}. ${stop.title}",
                    anchor = Offset(0.5f, 0.5f),
                ) { NumberedStopMarker(index + 1, markerState) }
            }
            val destination = navigation.waypoints.destination
            MarkerComposable(
                state = rememberUpdatedMarkerState(LatLng(destination.latitude, destination.longitude)),
                title = navigation.destinationName,
            ) { DestinationMarker() }
            if (navigation.originSource == "home" && bus == null) {
                val origin = navigation.waypoints.origin
                MarkerComposable(state = rememberUpdatedMarkerState(LatLng(origin.latitude, origin.longitude)), title = "Partida") {
                    StopMarker(RcColors.Success)
                }
            }
            bus?.let {
                MarkerComposable(
                    state = rememberUpdatedMarkerState(it),
                    title = "Você",
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 10f,
                ) { BusMarker() }
            }
        }

        Column(Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                MapButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                Spacer(Modifier.width(10.dp))
                ManeuverBanner(navigation, state, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.align(Alignment.End), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MapButton(onRecalculate) {
                    if (state.recalculating) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, "Recalcular a partir daqui")
                    }
                }
                MapButton({
                    following = false
                    scope.launch { fitRoute(cameraState, overviewPoints) }
                }) { Icon(Icons.Outlined.ZoomOutMap, "Ver a rota inteira") }
                if (bus != null && !following) {
                    MapButton({ following = true }) { Icon(Icons.Outlined.MyLocation, "Seguir o ônibus") }
                }
            }
        }

        BottomPanel(
            navigation = navigation,
            state = state,
            sendingToPassengers = sendingToPassengers,
            onRegisterStop = onRegisterStop,
            onResumeTracking = onResumeTracking,
            onRequestPermission = onRequestPermission,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Trecho já percorrido em cinza, o que falta em azul. */
@Composable
private fun RouteLines(polyline: List<LatLng>, progress: NavigationProgress?, bus: LatLng?) {
    if (polyline.size < 2) return
    if (progress == null || bus == null || progress.isOffRoute) {
        Polyline(points = polyline, color = RcColors.Primary, width = 16f)
        return
    }
    val split = (progress.segmentIndex + 1).coerceIn(1, polyline.size)
    Polyline(points = polyline.take(split) + bus, color = RcColors.TextMuted, width = 12f)
    Polyline(points = listOf(bus) + polyline.drop(split), color = RcColors.Primary, width = 16f)
}

@Composable
private fun ManeuverBanner(navigation: NavigationResponseDto, state: NavigationUiState, modifier: Modifier = Modifier) {
    val progress = state.progress
    val (icon, title, subtitle) = when {
        state.recalculating && progress?.isOffRoute == true ->
            Triple(Icons.Outlined.Refresh, "Recalculando rota…", "Você saiu do trajeto")
        progress == null ->
            Triple(Icons.Filled.Navigation, navigation.instruction, if (state.hasLocation) null else "Aguardando o GPS…")
        progress.nextStep != null ->
            Triple(maneuverIcon(progress.nextStep.maneuver), progress.nextStep.mainInstruction(), "Em ${Geo.formatDistance(progress.metersToNextStep)}")
        else ->
            Triple(Icons.Filled.Flag, "Siga até ${navigation.destinationName}", "Em ${Geo.formatDistance(progress.metersToNextStep)}")
    }
    Column(
        modifier
            .background(RcColors.Brand, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                subtitle?.let {
                    Text(it, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                Text(title, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            }
        }
        progress?.followingStep?.let { following ->
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Depois", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Icon(maneuverIcon(following.maneuver), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(following.mainInstruction(), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun BottomPanel(
    navigation: NavigationResponseDto,
    state: NavigationUiState,
    sendingToPassengers: Boolean,
    onRegisterStop: () -> Unit,
    onResumeTracking: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress
    val uriHandler = LocalUriHandler.current
    Column(
        modifier
            .fillMaxWidth()
            .background(RcColors.Card, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            !state.hasLocation -> Notice(
                "Sem sinal de GPS. Permita a localização para acompanhar a rota.",
                action = "Permitir" to onRequestPermission,
            )
            state.tripInProgress && !sendingToPassengers -> Notice(
                "Os passageiros não estão vendo o ônibus.",
                action = "Retomar envio" to onResumeTracking,
            )
            !state.tripInProgress -> Notice("Rota não iniciada. Toque em Iniciar rota no roteiro para avisar os passageiros.")
        }

        NextStopRow(navigation, progress)

        if (progress?.arrivingAtStop == true && progress.nextStop != null) {
            RcPrimaryButton(
                text = "Registrar parada em ${progress.nextStop.title}",
                onClick = onRegisterStop,
                loading = state.registeringStop,
                icon = Icons.Outlined.CheckCircle,
                containerColor = RcColors.Success,
            )
        }

        HorizontalDivider(color = RcColors.Divider)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val remainingSeconds = progress?.remainingSeconds ?: navigation.legs.sumOf { it.durationSeconds }
            val remainingMeters = progress?.remainingMeters ?: navigation.legs.sumOf { it.distanceMeters }.toDouble()
            Metric(arrivalClock(remainingSeconds), "chegada", Modifier.weight(1f))
            Metric(formatMinutes(remainingSeconds), "restante", Modifier.weight(1f))
            Metric(Geo.formatDistance(remainingMeters), "distância", Modifier.weight(1f))
            RcOutlinedButton(
                text = "Maps",
                onClick = { uriHandler.openUri(googleMapsUri(navigation)) },
                icon = Icons.Outlined.Map,
            )
        }
    }
}

@Composable
private fun NextStopRow(navigation: NavigationResponseDto, progress: NavigationProgress?) {
    val nextStop = progress?.nextStop ?: navigation.stops.firstOrNull { !it.done }
    val number = nextStop?.let { stop -> navigation.stops.indexOfFirst { it.id == stop.id } + 1 }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (nextStop != null && number != null) {
            NumberedStopMarker(number, StopMarkerState.NEXT)
        } else {
            DestinationMarker()
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (nextStop != null) "Próxima parada" else "Destino final",
                color = RcColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                nextStop?.title ?: navigation.destinationName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (progress != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMinutes(progress.secondsToNextStop), color = Color.White, fontWeight = FontWeight.Bold)
                Text(Geo.formatDistance(progress.metersToNextStop), color = RcColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Notice(text: String, action: Pair<String, () -> Unit>? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(RcColors.Warning.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.GpsOff, contentDescription = null, tint = RcColors.Warning, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        action?.let { (label, onClick) ->
            Spacer(Modifier.width(8.dp))
            RcOutlinedButton(label, onClick, color = RcColors.Warning)
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = RcColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MapButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = RcColors.Card, contentColor = Color.White),
        content = content,
    )
}

private suspend fun fitRoute(state: CameraPositionState, points: List<LatLng>) {
    if (points.isEmpty()) return
    val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
    runCatching { state.animate(CameraUpdateFactory.newLatLngBounds(bounds, 160)) }
}

/** Ícone da manobra da Routes API (NavigationInstruction.maneuver). */
private fun maneuverIcon(maneuver: String): ImageVector = when (maneuver) {
    "TURN_LEFT" -> Icons.Filled.TurnLeft
    "TURN_RIGHT" -> Icons.Filled.TurnRight
    "TURN_SLIGHT_LEFT", "FORK_LEFT" -> Icons.Filled.TurnSlightLeft
    "TURN_SLIGHT_RIGHT", "FORK_RIGHT" -> Icons.Filled.TurnSlightRight
    "TURN_SHARP_LEFT" -> Icons.Filled.TurnSharpLeft
    "TURN_SHARP_RIGHT" -> Icons.Filled.TurnSharpRight
    "UTURN_LEFT" -> Icons.Filled.UTurnLeft
    "UTURN_RIGHT" -> Icons.Filled.UTurnRight
    "RAMP_LEFT" -> Icons.Filled.RampLeft
    "RAMP_RIGHT" -> Icons.Filled.RampRight
    "ROUNDABOUT_LEFT" -> Icons.Filled.RoundaboutLeft
    "ROUNDABOUT_RIGHT" -> Icons.Filled.RoundaboutRight
    "MERGE" -> Icons.Filled.Merge
    "STRAIGHT", "NAME_CHANGE" -> Icons.Filled.Straight
    else -> Icons.Filled.Navigation
}

/** A Routes API às vezes acrescenta uma segunda linha com ponto de referência ("Você verá..."). */
private fun NavigationStepDto.mainInstruction(): String = instruction.lineSequence().first()

private fun formatMinutes(seconds: Int): String {
    val minutes = maxOf(1, (seconds + 30) / 60)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}

private fun arrivalClock(secondsFromNow: Int): String =
    LocalTime.now().plusSeconds(secondsFromNow.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))

/** Navegação por voz no Google Maps com as paradas que faltam como waypoints. */
private fun googleMapsUri(navigation: NavigationResponseDto): String {
    val destination = navigation.waypoints.destination
    val waypoints = navigation.waypoints.intermediates.joinToString("|") { "${it.latitude},${it.longitude}" }
    return buildString {
        append("https://www.google.com/maps/dir/?api=1&travelmode=driving&dir_action=navigate")
        append("&destination=${destination.latitude},${destination.longitude}")
        if (waypoints.isNotEmpty()) append("&waypoints=").append(Uri.encode(waypoints))
    }
}
