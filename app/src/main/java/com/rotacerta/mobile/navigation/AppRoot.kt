package com.rotacerta.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rotacerta.mobile.core.session.UserRole
import com.rotacerta.mobile.feature.address.AddressScreen
import com.rotacerta.mobile.feature.auth.ChangePasswordScreen
import com.rotacerta.mobile.feature.auth.ForgotPasswordScreen
import com.rotacerta.mobile.feature.auth.LoginScreen
import com.rotacerta.mobile.feature.auth.SplashScreen
import com.rotacerta.mobile.feature.collaborator.CollaboratorHome
import com.rotacerta.mobile.feature.driver.DriverHome
import com.rotacerta.mobile.feature.driver.detail.RouteDetailScreen
import com.rotacerta.mobile.feature.driver.navigation.NavigationMapScreen
import kotlinx.serialization.Serializable

@Serializable data object LoginRoute
@Serializable data object ForgotPasswordRoute
@Serializable data object HomeRoute
@Serializable data object ChangePasswordRoute
@Serializable data object AddressRoute
@Serializable data class RouteDetailRoute(val routeId: Long)
@Serializable data class NavigationMapRoute(val routeId: Long)

/**
 * Cada estado da sessão tem o próprio NavHost: ao logar, sair ou expirar a sessão,
 * a pilha de telas é recriada do zero e nunca sobra uma tela da conta anterior.
 */
@Composable
fun AppRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val root by viewModel.root.collectAsStateWithLifecycle()
    when (val state = root) {
        RootState.Loading -> SplashScreen()
        RootState.LoggedOut -> AuthNavHost()
        RootState.MustChangePassword -> ChangePasswordScreen(mandatory = true, onBack = null, onDone = {})
        is RootState.Ready -> MainNavHost(state.role)
    }
}

@Composable
private fun AuthNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(onForgotPassword = { navController.navigate(ForgotPasswordRoute) })
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(onFinish = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainNavHost(role: UserRole) {
    val navController = rememberNavController()
    val openChangePassword = { navController.navigate(ChangePasswordRoute) }
    val openAddress = { navController.navigate(AddressRoute) }
    NavHost(navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            when (role) {
                UserRole.DRIVER -> DriverHome(
                    onOpenRoute = { navController.navigate(RouteDetailRoute(it)) },
                    onChangePassword = openChangePassword,
                    onEditAddress = openAddress,
                )
                UserRole.COLLABORATOR -> CollaboratorHome(onChangePassword = openChangePassword, onEditAddress = openAddress)
            }
        }
        composable<RouteDetailRoute> {
            RouteDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenNavigation = { navController.navigate(NavigationMapRoute(it)) },
            )
        }
        composable<NavigationMapRoute> {
            NavigationMapScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<AddressRoute> {
            AddressScreen(onBack = { navController.popBackStack() })
        }
        composable<ChangePasswordRoute> {
            ChangePasswordScreen(
                mandatory = false,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
    }
}
