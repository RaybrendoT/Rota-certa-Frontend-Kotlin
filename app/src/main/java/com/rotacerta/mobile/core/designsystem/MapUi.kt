package com.rotacerta.mobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.rotacerta.mobile.R
import kotlinx.coroutines.flow.SharedFlow

/** Centro de Manaus, usado quando ainda não há nenhum ponto para mostrar no mapa. */
val FallbackMapCenter = LatLng(-3.1190, -60.0217)

/** Marcador do ônibus (desenhado em Compose dentro do mapa). */
@Composable
fun BusMarker() {
    Box(
        Modifier
            .size(40.dp)
            .background(RcColors.Primary, RoundedCornerShape(12.dp))
            .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.DirectionsBus, contentDescription = "Ônibus", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/** Ponto de embarque no mapa. */
@Composable
fun StopMarker(color: Color = RcColors.Danger) {
    Box(
        Modifier.size(22.dp).background(color, CircleShape).border(3.dp, Color.White, CircleShape),
    )
}

enum class StopMarkerState { PENDING, NEXT, DONE }

/** Parada numerada na ordem do roteiro: a próxima em destaque, as feitas apagadas com check. */
@Composable
fun NumberedStopMarker(number: Int, state: StopMarkerState) {
    val size = if (state == StopMarkerState.NEXT) 34.dp else 28.dp
    val color = when (state) {
        StopMarkerState.NEXT -> RcColors.Warning
        StopMarkerState.PENDING -> RcColors.Primary
        StopMarkerState.DONE -> RcColors.TextMuted
    }
    Box(
        Modifier.size(size).background(color, CircleShape).border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (state == StopMarkerState.DONE) {
            Icon(Icons.Filled.Check, contentDescription = "Parada feita", tint = Color.White, modifier = Modifier.size(16.dp))
        } else {
            Text("$number", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/** Destino final da rota (empresa). */
@Composable
fun DestinationMarker() {
    Box(
        Modifier.size(34.dp).background(RcColors.Danger, RoundedCornerShape(10.dp)).border(2.dp, Color.White, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Flag, contentDescription = "Destino", tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Estilo escuro do mapa, nas cores do app (res/raw/map_style_dark.json). */
@Composable
fun rememberDarkMapStyle(): MapStyleOptions {
    val context = LocalContext.current
    return remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) }
}

/** Mostra cada mensagem do fluxo como snackbar no rodapé do Box. */
@Composable
fun BoxScope.MessageSnackbar(messages: SharedFlow<String>, modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(messages) { messages.collect { host.showSnackbar(it) } }
    SnackbarHost(host, modifier.align(Alignment.BottomCenter))
}
