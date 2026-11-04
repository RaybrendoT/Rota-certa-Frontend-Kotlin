package com.rotacerta.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.notification.SystemNotifier
import com.rotacerta.mobile.data.dto.NotificationDto
import com.rotacerta.mobile.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    /** Mostra o contador de avisos não lidos nesta aba. */
    val showsUnreadBadge: Boolean = false,
    val content: @Composable () -> Unit,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    notificationRepository: NotificationRepository,
    private val systemNotifier: SystemNotifier,
) : ViewModel() {
    val unreadCount: StateFlow<Int> = notificationRepository.state
        .map { it.unreadCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val alerts: SharedFlow<NotificationDto> = notificationRepository.foregroundAlerts

    fun needsNotificationPermission() = !systemNotifier.canPost()
}

@Composable
fun HomeScaffold(tabs: List<HomeTab>, viewModel: HomeViewModel = hiltViewModel()) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    // Android 13+: pede uma vez a permissão para os avisos da rota virarem notificação.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && viewModel.needsNotificationPermission()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.alerts.collect { snackbarHost.showSnackbar("${it.title}\n${it.message}".trim()) }
    }

    Scaffold(
        containerColor = RcColors.Background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            NavigationBar(containerColor = RcColors.NavBar) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selected
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selected = index },
                        label = { Text(tab.label) },
                        icon = {
                            BadgedBox(badge = {
                                if (tab.showsUnreadBadge && unread > 0) Badge { Text(if (unread > 99) "99+" else "$unread") }
                            }) {
                                Icon(if (isSelected) tab.selectedIcon else tab.icon, contentDescription = tab.label)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RcColors.Primary,
                            selectedTextColor = RcColors.Primary,
                            unselectedIconColor = RcColors.TextSecondary,
                            unselectedTextColor = RcColors.TextSecondary,
                            indicatorColor = RcColors.Primary.copy(alpha = 0.15f),
                        ),
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            tabs[selected.coerceIn(tabs.indices)].content()
        }
    }
}
