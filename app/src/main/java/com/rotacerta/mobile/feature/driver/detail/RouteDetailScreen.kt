package com.rotacerta.mobile.feature.driver.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotacerta.mobile.core.designsystem.EmptyState
import com.rotacerta.mobile.core.designsystem.ErrorState
import com.rotacerta.mobile.core.designsystem.InitialsAvatar
import com.rotacerta.mobile.core.designsystem.LoadingState
import com.rotacerta.mobile.core.designsystem.RcCard
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcOutlinedButton
import com.rotacerta.mobile.core.designsystem.RcPrimaryButton
import com.rotacerta.mobile.core.designsystem.StatusPill
import com.rotacerta.mobile.core.designsystem.boardingStatusColor
import com.rotacerta.mobile.core.designsystem.boardingStatusLabel
import com.rotacerta.mobile.core.designsystem.routeStatusColor
import com.rotacerta.mobile.core.designsystem.routeStatusLabel
import com.rotacerta.mobile.core.location.LocationHelper
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.BoardingStatus
import com.rotacerta.mobile.data.dto.DriverRouteDto
import com.rotacerta.mobile.data.dto.RouteEventDto
import com.rotacerta.mobile.data.dto.RouteEventType
import com.rotacerta.mobile.data.dto.RoutePassengerDto
import com.rotacerta.mobile.feature.driver.tracking.TrackingState

private val TABS = listOf("Roteiro", "Passageiros", "Eventos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    onOpenNavigation: (Long) -> Unit,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var eventDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    // Localização é obrigatória para iniciar; notificação (Android 13+) mostra a rota em andamento.
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val locationOk = LocationHelper.LOCATION_PERMISSIONS.any { granted[it] == true }
        if (locationOk) pendingAction?.invoke()
        pendingAction = null
    }
    fun withLocationPermission(action: () -> Unit) {
        pendingAction = action
        val permissions = LocationHelper.LOCATION_PERMISSIONS.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val route = (state.route as? UiState.Success)?.data
    val trackingThis = (tracking as? TrackingState.Active)?.routeId == viewModel.routeId

    Scaffold(
        containerColor = RcColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(route?.name?.let(Formatters::beforeDash) ?: "Rota", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = {
                    route?.let { StatusPill(routeStatusLabel(it.routeStatus), routeStatusColor(it.routeStatus)) }
                    IconButton(onClick = { viewModel.load() }) { Icon(Icons.Outlined.Refresh, "Atualizar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RcColors.Background),
            )
        },
        bottomBar = {
            if (route != null) {
                ActionBar(
                    trackingThis = trackingThis,
                    tripInProgress = state.tripInProgress,
                    busy = state.busy,
                    onStart = { withLocationPermission(viewModel::startRoute) },
                    onResume = { withLocationPermission(viewModel::resumeTracking) },
                    onFinish = { confirmFinish = true },
                    onNavigate = { onOpenNavigation(viewModel.routeId) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val r = state.route) {
                UiState.Idle, UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(r.message, { viewModel.load() })
                is UiState.Success -> Column(Modifier.fillMaxSize()) {
                    ConfirmedHeader(r.data)
                    PrimaryTabRow(selectedTabIndex = tab, containerColor = RcColors.Background) {
                        TABS.forEachIndexed { i, title -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) }) }
                    }
                    when (tab) {
                        0 -> StopsTimeline(r.data, state.completedStopIds)
                        1 -> PassengersTab(r.data, state.updatingPassengerId, viewModel::updateBoardingStatus)
                        else -> EventsTab(state.events, onRegister = { eventDialog = it })
                    }
                }
            }
        }
    }

    eventDialog?.let { type ->
        EventDialog(
            type = type,
            onDismiss = { eventDialog = null },
            onConfirm = { description ->
                eventDialog = null
                withLocationPermission { viewModel.registerEvent(type, description) }
            },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finalizar rota?") },
            text = { Text("A chegada será registrada e o envio da sua localização será encerrado.") },
            confirmButton = { TextButton(onClick = { confirmFinish = false; viewModel.finishRoute() }) { Text("Finalizar") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Cancelar") } },
            containerColor = RcColors.Card,
        )
    }
}

@Composable
private fun ConfirmedHeader(route: DriverRouteDto) {
    Row(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(RcColors.Primary, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.People, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${route.confirmedCount} de ${route.passengers.size} passageiros confirmados", color = Color.White, fontWeight = FontWeight.Bold)
            route.vehicle?.let { Text("${it.model} · ${it.plateNumber}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ActionBar(
    trackingThis: Boolean,
    tripInProgress: Boolean,
    busy: Boolean,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onNavigate: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(RcColors.NavBar).navigationBarsPadding().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            when {
                trackingThis -> RcPrimaryButton("Finalizar rota", onFinish, loading = busy, icon = Icons.Filled.Stop, containerColor = RcColors.Danger)
                tripInProgress -> RcPrimaryButton("Retomar envio do GPS", onResume, loading = busy, icon = Icons.Filled.PlayArrow, containerColor = RcColors.Success)
                else -> RcPrimaryButton("Iniciar rota", onStart, loading = busy, icon = Icons.Filled.PlayArrow)
            }
        }
        RcOutlinedButton("Navegação", onNavigate, icon = Icons.Outlined.Navigation, modifier = Modifier.height(56.dp))
    }
}

@Composable
private fun StopsTimeline(route: DriverRouteDto, completed: Set<Long>) {
    if (route.stops.isEmpty()) {
        EmptyState(Icons.Outlined.DirectionsBus, "Rota sem paradas", "O gestor ainda não cadastrou paradas nesta rota.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        items(route.stops.sortedBy { it.sequence }, key = { it.id }) { stop ->
            val done = stop.id in completed
            val isLast = stop == route.stops.maxBy { it.sequence }
            val confirmed = route.passengersAt(stop.id).count { it.status == BoardingStatus.CONFIRMED }
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(16.dp).background(if (done) RcColors.Success else RcColors.Primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp)) }
                    if (!isLast) Box(Modifier.width(2.dp).height(70.dp).background(if (done) RcColors.Success.copy(alpha = 0.4f) else RcColors.Divider))
                }
                Spacer(Modifier.width(14.dp))
                RcCard(Modifier.padding(bottom = 10.dp), borderColor = if (done) RcColors.Success.copy(alpha = 0.25f) else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Formatters.beforeDash(stop.title),
                                color = if (done) RcColors.TextSecondary else RcColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Parada ${stop.sequence + 1} · ${stop.subtitle}", color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        if (done) StatusPill("concluída", RcColors.Success) else StatusPill("$confirmed/${stop.passengerCount} pass.", RcColors.Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PassengersTab(route: DriverRouteDto, updatingId: Long?, onChangeStatus: (Long, String) -> Unit) {
    if (route.passengers.isEmpty()) {
        EmptyState(Icons.Outlined.People, "Nenhum passageiro", "Ainda não há colaboradores vinculados a esta rota.")
        return
    }
    val byStatus = route.passengers.groupingBy { it.status }.eachCount()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                StatBadge(byStatus[BoardingStatus.CONFIRMED] ?: 0, "confirmados", RcColors.Success, Modifier.weight(1f))
                StatBadge(byStatus[BoardingStatus.PENDING] ?: 0, "pendentes", RcColors.Warning, Modifier.weight(1f))
                StatBadge(byStatus[BoardingStatus.ABSENT] ?: 0, "ausentes", RcColors.Danger, Modifier.weight(1f))
            }
        }
        val stopOrder = route.stops.associate { it.id to it.sequence }
        route.passengers.groupBy { it.point }
            .toList()
            .sortedBy { (_, list) -> list.firstOrNull()?.routeStopId?.let(stopOrder::get) ?: Int.MAX_VALUE }
            .forEach { (point, passengers) ->
                item(key = "header-$point") {
                    Text(Formatters.beforeDash(point), color = RcColors.TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                }
                items(passengers, key = { it.id }) { p ->
                    PassengerCard(p, updating = updatingId == p.id) { onChangeStatus(p.id, it) }
                }
            }
    }
}

@Composable
private fun StatBadge(value: Int, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", color = color, style = MaterialTheme.typography.titleLarge)
        Text(label, color = color.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PassengerCard(passenger: RoutePassengerDto, updating: Boolean, onChangeStatus: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val color = boardingStatusColor(passenger.status)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .background(RcColors.Card, RoundedCornerShape(12.dp))
                .clickable(enabled = !updating) { menu = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(passenger.name, color = color, size = 36)
            Spacer(Modifier.width(12.dp))
            Text(passenger.name, color = RcColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusPill(boardingStatusLabel(passenger.status), color)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            listOf(BoardingStatus.CONFIRMED, BoardingStatus.PENDING, BoardingStatus.ABSENT)
                .filter { it != passenger.status }
                .forEach { status ->
                    DropdownMenuItem(
                        text = { Text("Marcar como ${boardingStatusLabel(status)}") },
                        onClick = { menu = false; onChangeStatus(status) },
                    )
                }
        }
    }
}

@Composable
private fun EventsTab(events: List<RouteEventDto>, onRegister: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                RouteEventType.MANUAL.forEach { type ->
                    RcOutlinedButton(
                        RouteEventType.label(type),
                        onClick = { onRegister(type) },
                        color = when (type) {
                            RouteEventType.DELAY -> RcColors.Warning
                            RouteEventType.INCIDENT -> RcColors.Danger
                            else -> RcColors.Primary
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (events.isEmpty()) {
            item { Text("Nenhum evento registrado nesta rota.", color = RcColors.TextSecondary) }
        }
        items(events, key = { it.id }) { event ->
            RcCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(RouteEventType.label(event.eventType), color = RcColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(Formatters.relativeTime(event.timestamp), color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                if (event.description.isNotBlank()) Text(event.description, color = RcColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun EventDialog(type: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var description by rememberSaveable { mutableStateOf("") }
    val required = RouteEventType.requiresDescription(type)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar ${RouteEventType.label(type).lowercase()}") },
        text = {
            Column {
                Text("Sua localização atual será anexada ao evento.", color = RcColors.TextSecondary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (required) "Descrição (obrigatória)" else "Descrição (opcional)") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(description) }, enabled = !required || description.isNotBlank()) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = RcColors.Card,
    )
}
