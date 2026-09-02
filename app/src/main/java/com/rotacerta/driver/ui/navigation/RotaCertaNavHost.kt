package com.rotacerta.driver.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rotacerta.driver.ui.changepassword.ChangePasswordScreen
import com.rotacerta.driver.ui.forgotpassword.ForgotPasswordScreen
import com.rotacerta.driver.ui.login.LoginScreen
import com.rotacerta.driver.ui.myroutes.MyRoutesScreen
import com.rotacerta.driver.ui.navigationmap.NavigationMapScreen
import com.rotacerta.driver.ui.routedetail.RouteDetailScreen
import com.rotacerta.driver.ui.session.SessionStatus
import com.rotacerta.driver.ui.session.SessionViewModel

@Composable
fun RotaCertaNavHost() {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionStatus by sessionViewModel.status.collectAsState()

    when (sessionStatus) {
        SessionStatus.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {
            val startDestination = when (sessionStatus) {
                SessionStatus.LoggedOut -> Screen.Login.route
                SessionStatus.MustChangePassword -> Screen.ChangePassword.route
                SessionStatus.Ready -> Screen.MyRoutes.route
                SessionStatus.Loading -> Screen.Login.route
            }

            NavHost(navController = navController, startDestination = startDestination) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoggedIn = { mustChangePassword ->
                            val destination = if (mustChangePassword) {
                                Screen.ChangePassword.route
                            } else {
                                Screen.MyRoutes.route
                            }
                            navController.navigate(destination) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    )
                }

                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.ChangePassword.route) {
                    ChangePasswordScreen(
                        onPasswordChanged = {
                            navController.navigate(Screen.MyRoutes.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Screen.MyRoutes.route) {
                    MyRoutesScreen(
                        onOpenRoute = { routeId ->
                            navController.navigate(Screen.RouteDetail.build(routeId))
                        },
                        onLoggedOut = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }

                composable(
                    route = Screen.RouteDetail.route,
                    arguments = listOf(navArgument(Screen.ARG_ROUTE_ID) { type = NavType.LongType }),
                ) {
                    RouteDetailScreen(
                        onBack = { navController.popBackStack() },
                        onOpenNavigation = { routeId ->
                            navController.navigate(Screen.NavigationMap.build(routeId))
                        },
                    )
                }

                composable(
                    route = Screen.NavigationMap.route,
                    arguments = listOf(navArgument(Screen.ARG_ROUTE_ID) { type = NavType.LongType }),
                ) {
                    NavigationMapScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
