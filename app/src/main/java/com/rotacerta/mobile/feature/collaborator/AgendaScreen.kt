package com.rotacerta.mobile.feature.collaborator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotacerta.mobile.core.designsystem.EmptyState
import com.rotacerta.mobile.core.designsystem.ErrorState
import com.rotacerta.mobile.core.designsystem.LoadingState
import com.rotacerta.mobile.core.designsystem.MessageSnackbar
import com.rotacerta.mobile.core.designsystem.RcCard
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcOutlinedButton
import com.rotacerta.mobile.core.designsystem.ScreenTitle
import com.rotacerta.mobile.core.designsystem.StatusPill
import com.rotacerta.mobile.core.designsystem.boardingStatusColor
import com.rotacerta.mobile.core.designsystem.boardingStatusLabel
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.BoardingStatus
import com.rotacerta.mobile.data.dto.CollaboratorRouteDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(viewModel: CollaboratorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle("Confirmação de presença", subtitle = "Suas rotas e horários")
            when (val routes = state.routes) {
                UiState.Idle, UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(routes.message, { viewModel.refresh() })
                is UiState.Success -> PullToRefreshBox(isRefreshing = false, onRefresh = { viewModel.refresh() }) {
                    if (routes.data.isEmpty()) {
                        EmptyState(Icons.Outlined.CalendarToday, "Nenhuma rota na sua agenda", "Fale com o gestor para ser vinculado a uma rota.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(routes.data, key = { it.id }) { route ->
                                AgendaCard(
                                    route = route,
                                    sending = state.sendingId == route.id,
                                    onRespond = { viewModel.respond(route.id, it) },
                                )
                            }
                        }
                    }
                }
            }
        }
        MessageSnackbar(viewModel.messages)
    }
}

@Composable
private fun AgendaCard(route: CollaboratorRouteDto, sending: Boolean, onRespond: (Boolean) -> Unit) {
    val statusColor = boardingStatusColor(route.boardingStatus)
    RcCard(borderColor = statusColor.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Formatters.beforeDash(route.route.name),
                style = MaterialTheme.typography.titleMedium,
                color = RcColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            StatusPill(boardingStatusLabel(route.boardingStatus), statusColor)
        }
        Spacer(Modifier.height(10.dp))
        route.stop?.let { InfoLine(Icons.Outlined.LocationOn, "${Formatters.beforeDash(it.title)} — ${it.subtitle}") }
        route.schedules.forEach { InfoLine(Icons.Outlined.Schedule, "${Formatters.weekdays(it.weekdays)} · ${it.time}") }
        val vehicle = route.route.vehicle
        if (vehicle != null || route.route.driverName != null) {
            InfoLine(
                Icons.Outlined.DirectionsBus,
                listOfNotNull(vehicle?.let { "${it.model} ${it.plateNumber}" }, route.route.driverName?.let { "Motorista: $it" })
                    .joinToString(" · "),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RcOutlinedButton(
                "Confirmar",
                onClick = { onRespond(true) },
                enabled = !sending && route.boardingStatus != BoardingStatus.CONFIRMED,
                color = RcColors.Success,
                modifier = Modifier.weight(1f),
            )
            RcOutlinedButton(
                "Ausentar",
                onClick = { onRespond(false) },
                enabled = !sending && route.boardingStatus != BoardingStatus.ABSENT,
                color = RcColors.Danger,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(26.dp)) {
        Icon(icon, contentDescription = null, tint = RcColors.TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = RcColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}
