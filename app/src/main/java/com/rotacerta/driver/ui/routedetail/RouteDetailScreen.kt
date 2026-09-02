package com.rotacerta.driver.ui.routedetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rotacerta.driver.data.remote.dto.BoardingStatus
import com.rotacerta.driver.data.remote.dto.DriverRouteBoardDto
import com.rotacerta.driver.data.remote.dto.RouteEventDto
import com.rotacerta.driver.data.remote.dto.RouteEventType
import com.rotacerta.driver.data.remote.dto.RoutePassengerBoardDto
import com.rotacerta.driver.data.remote.dto.RouteStopBoardDto
import com.rotacerta.driver.ui.common.ErrorBanner
import com.rotacerta.driver.ui.common.StatusChip
import com.rotacerta.driver.ui.common.boardingStatusColor
import com.rotacerta.driver.ui.common.routeStatusColor
import com.rotacerta.driver.ui.common.routeStatusLabel
import com.rotacerta.driver.util.ActionState
import com.rotacerta.driver.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    onBack: () -> Unit,
    onOpenNavigation: (Long) -> Unit,
    viewModel: RouteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = (uiState.routeState as? UiState.Success)?.data?.name ?: "Rota"
                    Text(name)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenNavigation(viewModel.routeId) }) {
                        Icon(Icons.Filled.Navigation, contentDescription = "Navegar")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState.routeState) {
                is UiState.Loading, UiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Column(Modifier.fillMaxSize().padding(24.dp)) {
                        ErrorBanner(state.message)
                    }
                }

                is UiState.Success -> {
                    RouteDetailContent(
                        route = state.data,
                        uiState = uiState,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteDetailContent(
    route: DriverRouteBoardDto,
    uiState: RouteDetailUiState,
    viewModel: RouteDetailViewModel,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { RouteHeaderCard(route) }
        item { EventQuickActions(onSelect = viewModel::openEventForm) }

        item { SectionTitle("Paradas (${route.stops.size})") }
        items(route.stops, key = { "stop-${it.id}" }) { stop ->
            StopRow(stop)
        }

        item { SectionTitle("Passageiros (${route.passengers.size})") }
        items(route.passengers, key = { "pax-${it.id}" }) { passenger ->
            PassengerRow(
                passenger = passenger,
                isUpdating = uiState.passengerActionInProgressId == passenger.id,
                onStatusSelected = { status -> viewModel.updateBoardingStatus(passenger.id, status) },
            )
        }

        item {
            TextButton(onClick = viewModel::toggleHistory) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (uiState.historyVisible) "Ocultar histórico de eventos" else "Ver histórico de eventos")
            }
        }

        if (uiState.historyVisible) {
            val history = uiState.history
            if (history == null) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (history.isEmpty()) {
                item {
                    Text(
                        "Nenhum evento registrado para esta rota ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(history, key = { "event-${it.id}" }) { event ->
                    EventHistoryRow(event)
                }
            }
        }
    }

    if (uiState.eventForm.isVisible) {
        RegisterEventDialog(
            state = uiState.eventForm,
            onDescriptionChange = viewModel::onEventDescriptionChange,
            onDismiss = viewModel::closeEventForm,
            onConfirm = viewModel::submitEvent,
        )
    }
}

@Composable
private fun RouteHeaderCard(route: DriverRouteBoardDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(routeStatusLabel(route.routeStatus), routeStatusColor(route.routeStatus))
                Spacer(Modifier.weight(1f))
                Text(route.companyName, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            route.vehicle?.let { vehicle ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${vehicle.model} · placa ${vehicle.plateNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } ?: Text(
                "Nenhum veículo vinculado a esta rota.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EventQuickActions(onSelect: (String) -> Unit) {
    Column {
        Text("Registrar evento", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RouteEventType.ALL.forEach { type ->
                OutlinedButton(onClick = { onSelect(type) }) {
                    Text(RouteEventType.label(type))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun StopRow(stop: RouteStopBoardDto) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("${stop.sequence + 1}", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stop.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                stop.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${stop.passengerCount} pax",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PassengerRow(
    passenger: RoutePassengerBoardDto,
    isUpdating: Boolean,
    onStatusSelected: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(passenger.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                passenger.point,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(BoardingStatus.PENDING, BoardingStatus.CONFIRMED, BoardingStatus.ABSENT).forEach { status ->
                        FilterChip(
                            selected = passenger.status == status,
                            onClick = { onStatusSelected(status) },
                            label = { Text(BoardingStatus.label(status)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventHistoryRow(event: RouteEventDto) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (event.eventType in listOf(RouteEventType.DELAY, RouteEventType.INCIDENT)) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(RouteEventType.label(event.eventType), style = MaterialTheme.typography.bodyMedium)
            if (event.description.isNotBlank()) {
                Text(
                    event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            event.timestamp.take(16).replace("T", " "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RegisterEventDialog(
    state: EventFormState,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val requiresDescription = RouteEventType.requiresDescription(state.eventType)
    val submitting = state.submitState == ActionState.InProgress

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("Registrar: ${RouteEventType.label(state.eventType)}") },
        text = {
            Column {
                Text(
                    "Sua localização atual será enviada junto com o evento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = {
                        Text(if (requiresDescription) "Descrição (obrigatória)" else "Descrição (opcional)")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                val error = state.submitState as? ActionState.Error
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    ErrorBanner(error.message)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !submitting && (!requiresDescription || state.description.isNotBlank()),
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancelar") }
        },
    )
}
