package com.rotacerta.mobile.feature.notifications

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DoNotDisturbAlt
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcLinkButton
import com.rotacerta.mobile.core.designsystem.ScreenTitle
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.data.dto.NotificationDto
import com.rotacerta.mobile.data.dto.NotificationKind
import com.rotacerta.mobile.data.repository.NotificationRepository
import com.rotacerta.mobile.data.repository.NotificationsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {
    val state: StateFlow<NotificationsState> = repository.state

    fun refresh() = viewModelScope.launch { repository.refresh() }
    fun loadMore() = viewModelScope.launch { repository.loadMore() }
    fun markRead(id: Long) = viewModelScope.launch { repository.markRead(id) }
    fun markAllRead() = viewModelScope.launch { repository.markAllRead() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (!state.loaded) viewModel.refresh() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle("Avisos", Modifier.weight(1f))
            if (state.unreadCount > 0) RcLinkButton("Marcar todos como lidos", viewModel::markAllRead, RcColors.Primary)
        }
        when {
            !state.loaded && state.error != null -> ErrorState(state.error!!, viewModel::refresh)
            !state.loaded -> LoadingState()
            else -> PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.items.isEmpty()) {
                    EmptyState(
                        Icons.Outlined.NotificationsNone,
                        "Nenhum aviso por enquanto",
                        "Saída do ônibus, atrasos e chegada ao seu ponto aparecem aqui em tempo real.",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            NotificationCard(item, onClick = { viewModel.markRead(item.id) })
                        }
                        if (state.nextPage != null) {
                            item { RcLinkButton("Carregar mais", viewModel::loadMore, RcColors.Primary) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationDto, onClick: () -> Unit) {
    val (icon, color) = styleFor(item.kind)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (item.read) RcColors.Card else RcColors.CardHighlight, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = RcColors.TextPrimary,
                fontWeight = if (item.read) FontWeight.Normal else FontWeight.SemiBold,
            )
            if (item.message.isNotBlank()) {
                Text(item.message, color = RcColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Text(Formatters.relativeTime(item.createdAt), color = RcColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (!item.read) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(8.dp).background(RcColors.Primary, CircleShape))
        }
    }
}

private fun styleFor(kind: String): Pair<ImageVector, Color> = when (kind) {
    NotificationKind.ROUTE_DEPARTURE, NotificationKind.BUS_APPROACHING -> Icons.Outlined.DirectionsBus to RcColors.Primary
    NotificationKind.BUS_AT_STOP -> Icons.Outlined.LocationOn to RcColors.Success
    NotificationKind.ROUTE_DELAY -> Icons.Outlined.WarningAmber to RcColors.Warning
    NotificationKind.ROUTE_INCIDENT -> Icons.Outlined.ReportProblem to RcColors.Danger
    NotificationKind.ROUTE_ARRIVAL -> Icons.Outlined.Flag to RcColors.Success
    NotificationKind.BOARDING_CONFIRMED -> Icons.Outlined.CheckCircleOutline to RcColors.Success
    NotificationKind.BOARDING_ABSENT -> Icons.Outlined.DoNotDisturbAlt to RcColors.Warning
    NotificationKind.ROUTE_ASSIGNED -> Icons.Outlined.EventAvailable to RcColors.Primary
    NotificationKind.ADDRESS_CHANGED -> Icons.Outlined.Home to RcColors.Primary
    NotificationKind.ADDRESS_APPROVED -> Icons.Outlined.Home to RcColors.Success
    NotificationKind.ADDRESS_REJECTED -> Icons.Outlined.Home to RcColors.Danger
    NotificationKind.PICKUP_CHANGED -> Icons.Outlined.LocationOn to RcColors.Warning
    NotificationKind.ROUTE_UPDATED -> Icons.Outlined.DirectionsBus to RcColors.Warning
    else -> Icons.Outlined.NotificationsNone to RcColors.Primary
}
