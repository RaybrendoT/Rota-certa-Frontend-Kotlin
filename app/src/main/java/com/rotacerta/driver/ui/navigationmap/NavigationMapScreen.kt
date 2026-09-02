package com.rotacerta.driver.ui.navigationmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.rotacerta.driver.data.remote.dto.NavigationResponseDto
import com.rotacerta.driver.ui.common.ErrorBanner
import com.rotacerta.driver.util.PolylineDecoder
import com.rotacerta.driver.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationMapScreen(
    onBack: () -> Unit,
    viewModel: NavigationMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navegação") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Recalcular")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is UiState.Loading, UiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Column(Modifier.fillMaxSize().padding(24.dp)) {
                        ErrorBanner(state.message)
                        Spacer(Modifier.padding(top = 12.dp))
                        TextButton(onClick = viewModel::load) { Text("Tentar novamente") }
                    }
                }

                is UiState.Success -> {
                    NavigationMapContent(state.data)
                }
            }
        }
    }
}

@Composable
private fun NavigationMapContent(navigation: NavigationResponseDto) {
    val polylinePoints = remember(navigation.encodedPolyline) {
        PolylineDecoder.decode(navigation.encodedPolyline)
    }

    val origin = LatLng(navigation.waypoints.origin.latitude, navigation.waypoints.origin.longitude)
    val destination = LatLng(
        navigation.waypoints.destination.latitude,
        navigation.waypoints.destination.longitude,
    )
    val intermediates = navigation.waypoints.intermediates.map { LatLng(it.latitude, it.longitude) }

    val cameraPositionState = rememberCameraPositionState {
        val allPoints = (polylinePoints.ifEmpty { listOf(origin, destination) })
        position = if (allPoints.size == 1) {
            CameraPosition.fromLatLngZoom(allPoints.first(), 14f)
        } else {
            val bounds = LatLngBounds.builder().apply {
                allPoints.forEach { include(it) }
            }.build()
            CameraPosition.fromLatLngZoom(bounds.center, 12f)
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        ) {
            if (polylinePoints.isNotEmpty()) {
                Polyline(
                    points = polylinePoints,
                    color = MaterialTheme.colorScheme.primary,
                    width = 10f,
                )
            }
            Marker(
                state = MarkerState(position = origin),
                title = "Início",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
            )
            intermediates.forEachIndexed { index, point ->
                Marker(
                    state = MarkerState(position = point),
                    title = "Parada ${index + 1}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                )
            }
            Marker(
                state = MarkerState(position = destination),
                title = "Destino",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
            )
        }

        NavigationInfoCard(
            navigation = navigation,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
private fun NavigationInfoCard(navigation: NavigationResponseDto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    navigation.instruction,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                navigation.durationText?.let { InfoPill(Icons.Filled.Schedule, it) }
                navigation.distanceText?.let { InfoPill(Icons.Filled.Straighten, it) }
            }
        }
    }
}

@Composable
private fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
