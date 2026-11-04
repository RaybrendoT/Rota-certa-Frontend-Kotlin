package com.rotacerta.mobile.feature.collaborator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Route
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.feature.home.HomeScaffold
import com.rotacerta.mobile.feature.home.HomeTab
import com.rotacerta.mobile.feature.notifications.NotificationsScreen
import com.rotacerta.mobile.feature.profile.ProfileRow
import com.rotacerta.mobile.feature.profile.ProfileScreen

/** Área do colaborador: Mapa · Agenda · Avisos · Perfil (mesmas abas do app Flutter). */
@Composable
fun CollaboratorHome(onChangePassword: () -> Unit, onEditAddress: () -> Unit) {
    HomeScaffold(
        tabs = listOf(
            HomeTab("Mapa", Icons.Outlined.Map, Icons.Filled.Map) { CollaboratorMapScreen() },
            HomeTab("Agenda", Icons.Outlined.CalendarToday, Icons.Filled.CalendarToday) { AgendaScreen() },
            HomeTab("Avisos", Icons.Outlined.NotificationsNone, Icons.Filled.Notifications, showsUnreadBadge = true) {
                NotificationsScreen()
            },
            HomeTab("Perfil", Icons.Outlined.PersonOutline, Icons.Filled.Person) {
                ProfileScreen(onChangePassword = onChangePassword, onEditAddress = onEditAddress, extraInfo = { CollaboratorRouteInfo() })
            },
        ),
    )
}

/** Rota e ponto de embarque no perfil (antes buscados com várias chamadas no Flutter). */
@Composable
private fun CollaboratorRouteInfo(viewModel: CollaboratorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val routes = state.routes) {
        is UiState.Success ->
            if (routes.data.isEmpty()) {
                ProfileRow(Icons.Outlined.Route, "Rota", "Nenhuma rota vinculada")
            } else {
                routes.data.forEach { route ->
                    ProfileRow(Icons.Outlined.Route, "Rota", Formatters.beforeDash(route.route.name))
                    ProfileRow(
                        Icons.Outlined.LocationOn,
                        "Ponto de embarque",
                        route.stop?.title?.let(Formatters::beforeDash) ?: "Não definido",
                    )
                }
            }
        is UiState.Error -> ProfileRow(Icons.Outlined.Route, "Rota", "Não foi possível carregar")
        else -> ProfileRow(Icons.Outlined.Route, "Rota", "Carregando…")
    }
}
