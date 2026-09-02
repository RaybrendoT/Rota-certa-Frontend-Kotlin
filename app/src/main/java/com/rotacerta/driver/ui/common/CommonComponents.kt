package com.rotacerta.driver.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rotacerta.driver.ui.theme.BoardingAbsent
import com.rotacerta.driver.ui.theme.BoardingConfirmed
import com.rotacerta.driver.ui.theme.BoardingPending
import com.rotacerta.driver.ui.theme.StatusDelayed
import com.rotacerta.driver.ui.theme.StatusInRoute
import com.rotacerta.driver.ui.theme.StatusWaiting

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Box(Modifier.padding(start = 8.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50)),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

fun routeStatusColor(status: String): Color = when (status) {
    "in_route" -> StatusInRoute
    "delayed" -> StatusDelayed
    else -> StatusWaiting
}

fun routeStatusLabel(status: String): String = when (status) {
    "in_route" -> "Em rota"
    "delayed" -> "Atrasada"
    else -> "Aguardando"
}

fun boardingStatusColor(status: String): Color = when (status) {
    "confirmed" -> BoardingConfirmed
    "absent" -> BoardingAbsent
    else -> BoardingPending
}
