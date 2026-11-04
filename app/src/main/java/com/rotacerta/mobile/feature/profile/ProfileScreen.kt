package com.rotacerta.mobile.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.core.designsystem.InitialsAvatar
import com.rotacerta.mobile.core.designsystem.RcCard
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcOutlinedButton
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.core.session.UserSession
import com.rotacerta.mobile.core.util.Formatters
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.MyAddressDto
import com.rotacerta.mobile.data.repository.AddressRepository
import com.rotacerta.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    addressRepository: AddressRepository,
) : ViewModel() {
    val session: StateFlow<UserSession?> =
        sessionStore.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val address: StateFlow<UiState<MyAddressDto>> = addressRepository.current

    init {
        viewModelScope.launch { addressRepository.refresh() }
    }

    /** Encerra a sessão; WebSocket, rastreamento GPS e caches param sozinhos. */
    fun logout() = viewModelScope.launch { authRepository.logout() }
}

@Composable
fun ProfileScreen(
    onChangePassword: () -> Unit,
    onEditAddress: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    extraInfo: @Composable ColumnScope.() -> Unit = {},
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val address by viewModel.address.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val name = session?.name.orEmpty().ifBlank { "Usuário" }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        InitialsAvatar(name, size = 90)
        Text(Formatters.shortName(name), style = MaterialTheme.typography.titleLarge, color = RcColors.TextPrimary)
        Text(session?.companyName.orEmpty(), color = RcColors.TextSecondary)
        Spacer(Modifier.height(12.dp))

        extraInfo()

        ProfileRow(Icons.Outlined.Home, "Meu endereço", addressSummary(address), onClick = onEditAddress)
        ProfileRow(Icons.Outlined.Lock, "Segurança", "Trocar senha", onClick = onChangePassword)
        Spacer(Modifier.height(12.dp))
        RcOutlinedButton(
            "Sair",
            onClick = { confirmLogout = true },
            color = RcColors.Danger,
            icon = Icons.AutoMirrored.Outlined.Logout,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Sair da conta?") },
            text = { Text("Se houver uma rota em andamento, o envio da sua localização será interrompido.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; viewModel.logout() }) { Text("Sair") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } },
            containerColor = RcColors.Card,
        )
    }
}

@Composable
fun ProfileRow(icon: ImageVector, title: String, value: String, onClick: (() -> Unit)? = null) {
    RcCard(Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = RcColors.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(value, color = RcColors.TextPrimary)
            }
            if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = RcColors.TextMuted)
        }
    }
}

private fun addressSummary(state: UiState<MyAddressDto>): String = when (state) {
    is UiState.Success -> {
        val data = state.data
        val current = data.address?.let { "${it.street}, ${it.number}" } ?: "Não cadastrado"
        when {
            data.pending != null -> "$current · novo endereço aguardando aprovação"
            data.rejected != null -> "$current · último envio recusado"
            data.address == null -> "Não cadastrado · toque para cadastrar"
            else -> current
        }
    }
    is UiState.Error -> "Não foi possível carregar"
    else -> "Carregando…"
}
