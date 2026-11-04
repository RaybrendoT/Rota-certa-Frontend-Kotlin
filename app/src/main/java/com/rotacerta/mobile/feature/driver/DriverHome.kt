package com.rotacerta.mobile.feature.driver

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.runtime.Composable
import com.rotacerta.mobile.feature.driver.routes.MyRoutesScreen
import com.rotacerta.mobile.feature.home.HomeScaffold
import com.rotacerta.mobile.feature.home.HomeTab
import com.rotacerta.mobile.feature.notifications.NotificationsScreen
import com.rotacerta.mobile.feature.profile.ProfileScreen

/** Área do motorista: Rotas · Avisos · Perfil. Paradas, passageiros e navegação ficam no detalhe da rota. */
@Composable
fun DriverHome(onOpenRoute: (Long) -> Unit, onChangePassword: () -> Unit, onEditAddress: () -> Unit) {
    HomeScaffold(
        tabs = listOf(
            HomeTab("Rotas", Icons.AutoMirrored.Outlined.AltRoute, Icons.AutoMirrored.Filled.AltRoute) { MyRoutesScreen(onOpenRoute = onOpenRoute) },
            HomeTab("Avisos", Icons.Outlined.NotificationsNone, Icons.Filled.Notifications, showsUnreadBadge = true) {
                NotificationsScreen()
            },
            HomeTab("Perfil", Icons.Outlined.PersonOutline, Icons.Filled.Person) {
                ProfileScreen(onChangePassword = onChangePassword, onEditAddress = onEditAddress)
            },
        ),
    )
}
