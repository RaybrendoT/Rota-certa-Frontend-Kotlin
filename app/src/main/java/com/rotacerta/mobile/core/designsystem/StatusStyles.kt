package com.rotacerta.mobile.core.designsystem

import androidx.compose.ui.graphics.Color
import com.rotacerta.mobile.data.dto.BoardingStatus
import com.rotacerta.mobile.data.dto.RouteStatus

fun routeStatusLabel(status: String): String = when (status) {
    RouteStatus.IN_ROUTE -> "Em rota"
    RouteStatus.DELAYED -> "Atrasada"
    else -> "Aguardando"
}

fun routeStatusColor(status: String): Color = when (status) {
    RouteStatus.IN_ROUTE -> RcColors.Success
    RouteStatus.DELAYED -> RcColors.Warning
    else -> RcColors.TextSecondary
}

fun boardingStatusLabel(status: String): String = when (status) {
    BoardingStatus.CONFIRMED -> "confirmado"
    BoardingStatus.ABSENT -> "ausente"
    else -> "pendente"
}

fun boardingStatusColor(status: String): Color = when (status) {
    BoardingStatus.CONFIRMED -> RcColors.Success
    BoardingStatus.ABSENT -> RcColors.Danger
    else -> RcColors.Warning
}
