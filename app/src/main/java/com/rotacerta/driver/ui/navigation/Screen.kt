package com.rotacerta.driver.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object ForgotPassword : Screen("forgot-password")
    data object ChangePassword : Screen("change-password")
    data object MyRoutes : Screen("my-routes")

    data object RouteDetail : Screen("route-detail/{routeId}") {
        fun build(routeId: Long) = "route-detail/$routeId"
    }

    data object NavigationMap : Screen("navigation-map/{routeId}") {
        fun build(routeId: Long) = "navigation-map/$routeId"
    }

    companion object {
        const val ARG_ROUTE_ID = "routeId"
    }
}
