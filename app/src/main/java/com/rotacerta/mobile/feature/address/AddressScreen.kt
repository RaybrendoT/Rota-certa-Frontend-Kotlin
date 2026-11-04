package com.rotacerta.mobile.feature.address

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.rotacerta.mobile.core.designsystem.ErrorMessage
import com.rotacerta.mobile.core.designsystem.RcCard
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcPrimaryButton
import com.rotacerta.mobile.core.designsystem.RcTextField
import com.rotacerta.mobile.core.designsystem.rememberDarkMapStyle
import com.rotacerta.mobile.core.location.Geo
import com.rotacerta.mobile.core.location.LocationHelper
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.AddressPreviewDto
import com.rotacerta.mobile.data.dto.MyAddressDto

/** Atualização do endereço de casa: localização atual ou digitado, sempre com confirmação no mapa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(onBack: () -> Unit, viewModel: AddressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val goBack = { if (!viewModel.back()) onBack() }
    BackHandler(onBack = goBack)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) viewModel.useCurrentLocation() else viewModel.permissionDenied()
    }

    Scaffold(
        containerColor = RcColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Meu endereço") },
                navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RcColors.Background,
                    titleContentColor = RcColors.TextPrimary,
                    navigationIconContentColor = RcColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.step) {
                AddressStep.Choose -> ChooseStep(
                    current = (current as? UiState.Success)?.data,
                    onUseLocation = {
                        if (viewModel.hasLocationPermission()) viewModel.useCurrentLocation()
                        else permissionLauncher.launch(LocationHelper.LOCATION_PERMISSIONS)
                    },
                    onType = viewModel::typeAddress,
                )
                AddressStep.Locating -> Locating()
                AddressStep.Typing -> TypingStep(state, viewModel)
                AddressStep.Confirm -> state.preview?.let { ConfirmStep(state, it, viewModel) }
                AddressStep.Done -> DoneStep(state.doneMessage.orEmpty(), onBack)
            }
            state.error?.let { ErrorMessage(it) }
        }
    }
}

@Composable
private fun ChooseStep(current: MyAddressDto?, onUseLocation: () -> Unit, onType: () -> Unit) {
    val address = current?.address
    RcCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Home, null, tint = RcColors.Primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Endereço atual", color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                if (address == null) {
                    Text("Nenhum endereço cadastrado", color = RcColors.Warning)
                    Text(
                        "Sem endereço você fica fora do planejamento de rotas.",
                        color = RcColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text("${address.street}, ${address.number}", color = RcColors.TextPrimary)
                    Text(
                        listOf(address.neighborhood, "${address.city}/${address.state}").filter(String::isNotBlank).joinToString(" · "),
                        color = RcColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    current.nearestPoint?.let {
                        Text(
                            "Ponto mais próximo: ${it.name} (${it.distanceMeters} m)",
                            color = RcColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    current?.pending?.let {
        StatusBanner(
            Icons.Outlined.HourglassTop,
            RcColors.Warning,
            "Novo endereço aguardando aprovação",
            "${it.street}, ${it.number}. Até o gestor de transporte aprovar, vale o endereço atual. " +
                "Se você enviar outro, ele substitui este.",
        )
    }
    current?.rejected?.let {
        StatusBanner(
            Icons.Outlined.Block,
            RcColors.Danger,
            "Último envio recusado",
            "${it.street}, ${it.number}. Motivo: ${it.reviewNote.ifBlank { "não informado" }}",
        )
    }

    Text("Como você quer atualizar?", color = RcColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
    OptionCard(
        icon = Icons.Outlined.MyLocation,
        title = "Usar minha localização atual",
        subtitle = "Use quando estiver em casa. Precisamos de precisão de até ${MAX_GPS_ACCURACY_METERS.toInt()} m.",
        onClick = onUseLocation,
    )
    OptionCard(
        icon = Icons.Outlined.EditLocationAlt,
        title = "Digitar endereço",
        subtitle = "Informe o CEP e o número da casa.",
        onClick = onType,
    )
    current?.let {
        Text(
            "Você pode alterar o endereço mais ${it.changesLeftThisWeek} vez(es) nesta semana.",
            color = RcColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Text(
        "Sua localização é usada só para registrar o endereço de casa, visível para você e para o gestor de " +
            "transporte da sua empresa. Não guardamos seu trajeto.",
        color = RcColors.TextMuted,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun OptionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    RcCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RcColors.Primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = RcColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Locating() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(color = RcColors.Brand)
        Text("Buscando sua localização com precisão…", color = RcColors.TextPrimary)
        Text("Isso pode levar até 20 segundos. Fique perto de uma janela.", color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TypingStep(state: AddressUiState, viewModel: AddressViewModel) {
    val form = state.form
    Text("Digite o CEP: rua, bairro e cidade são preenchidos sozinhos.", color = RcColors.TextSecondary)
    RcTextField(form.postalCode, { v -> viewModel.updateForm { it.copy(postalCode = v.take(9)) } }, "CEP", keyboardType = KeyboardType.Number)
    RcTextField(form.street, { v -> viewModel.updateForm { it.copy(street = v) } }, "Rua")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RcTextField(form.number, { v -> viewModel.updateForm { it.copy(number = v) } }, "Número", Modifier.weight(1f), keyboardType = KeyboardType.Number)
        RcTextField(form.complement, { v -> viewModel.updateForm { it.copy(complement = v) } }, "Complemento", Modifier.weight(1.4f))
    }
    RcTextField(form.neighborhood, { v -> viewModel.updateForm { it.copy(neighborhood = v) } }, "Bairro")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RcTextField(form.city, { v -> viewModel.updateForm { it.copy(city = v) } }, "Cidade", Modifier.weight(2f))
        RcTextField(
            form.state, { v -> viewModel.updateForm { it.copy(state = v.take(2).uppercase()) } }, "UF", Modifier.weight(1f),
            imeAction = ImeAction.Done, onImeDone = viewModel::checkTypedAddress,
        )
    }
    RcPrimaryButton("Conferir endereço", onClick = viewModel::checkTypedAddress, loading = state.busy)
}

@Composable
private fun ConfirmStep(state: AddressUiState, preview: AddressPreviewDto, viewModel: AddressViewModel) {
    val found = LatLng(preview.latitude, preview.longitude)
    // A tela de confirmação sai da composição a cada nova prévia, então o estado nasce na posição certa.
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(found, 18f)
    }
    val pin = camera.position.target
    val shift = Geo.distanceMeters(pin.latitude, pin.longitude, found.latitude, found.longitude)
    val form = state.form

    Text("Arraste o mapa até o alfinete ficar em cima da sua casa.", color = RcColors.TextPrimary)
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(RcColors.Card, RoundedCornerShape(16.dp)),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(mapStyleOptions = rememberDarkMapStyle()),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false),
        )
        // Alfinete fixo no centro: a ponta marca o centro do mapa, que é o ponto gravado.
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = "Sua casa",
            tint = RcColors.Danger,
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .offset(y = (-22).dp),
        )
    }
    if (shift > MAX_PIN_SHIFT_METERS) {
        Warning("O alfinete está a ${shift.toInt()} m do endereço encontrado; o máximo é ${MAX_PIN_SHIFT_METERS.toInt()} m.")
    }
    preview.warnings.forEach { Warning(it) }

    RcTextField(form.street, { v -> viewModel.updateForm { it.copy(street = v) } }, "Rua")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RcTextField(form.number, { v -> viewModel.updateForm { it.copy(number = v) } }, "Número", Modifier.weight(1f), keyboardType = KeyboardType.Number)
        RcTextField(form.complement, { v -> viewModel.updateForm { it.copy(complement = v) } }, "Complemento", Modifier.weight(1.4f))
    }
    RcTextField(form.neighborhood, { v -> viewModel.updateForm { it.copy(neighborhood = v) } }, "Bairro")
    Text(
        listOf(form.city + "/" + form.state, form.postalCode).filter { it.isNotBlank() && it != "/" }.joinToString(" · "),
        color = RcColors.TextSecondary,
    )
    preview.nearestPoint?.let {
        Text("Ponto de embarque mais próximo: ${it.name} (${it.distanceMeters} m)", color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
    RcPrimaryButton(
        "Confirmar endereço",
        onClick = { viewModel.confirm(pin.latitude, pin.longitude) },
        loading = state.busy,
        enabled = shift <= MAX_PIN_SHIFT_METERS,
    )
}

@Composable
private fun StatusBanner(icon: ImageVector, color: Color, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = RcColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(text, color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Warning(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(RcColors.Warning.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.WarningAmber, null, tint = RcColors.Warning)
        Spacer(Modifier.width(8.dp))
        Text(text, color = RcColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DoneStep(message: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Outlined.CheckCircle, null, tint = RcColors.Success, modifier = Modifier.size(64.dp))
        Text(message, color = RcColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        RcPrimaryButton("Voltar ao perfil", onClick = onBack)
    }
}
