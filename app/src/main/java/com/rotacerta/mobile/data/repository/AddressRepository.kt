package com.rotacerta.mobile.data.repository

import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.network.apiCall
import com.rotacerta.mobile.core.realtime.RealtimeClient
import com.rotacerta.mobile.core.realtime.RealtimeEvent
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.api.AddressApi
import com.rotacerta.mobile.data.dto.AddressPreviewDto
import com.rotacerta.mobile.data.dto.AddressPreviewRequest
import com.rotacerta.mobile.data.dto.CepDto
import com.rotacerta.mobile.data.dto.MyAddressDto
import com.rotacerta.mobile.data.dto.NotificationKind
import com.rotacerta.mobile.data.dto.SaveAddressRequest
import com.rotacerta.mobile.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Endereço residencial do usuário: o Perfil mostra, a tela de atualização confere e grava. */
@Singleton
class AddressRepository @Inject constructor(
    private val api: AddressApi,
    realtimeClient: RealtimeClient,
    sessionStore: SessionStore,
    @ApplicationScope scope: CoroutineScope,
) {
    private val _current = MutableStateFlow<UiState<MyAddressDto>>(UiState.Idle)
    val current: StateFlow<UiState<MyAddressDto>> = _current.asStateFlow()

    init {
        scope.launch { sessionStore.session.collect { if (it == null) _current.value = UiState.Idle } }
        // Gestor aprovou ou recusou no painel: atualiza o Perfil sem precisar reabrir o app.
        scope.launch {
            realtimeClient.events.collect { event ->
                val kind = (event as? RealtimeEvent.NotificationReceived)?.notification?.kind
                if (kind == NotificationKind.ADDRESS_APPROVED || kind == NotificationKind.ADDRESS_REJECTED) refresh()
            }
        }
    }

    suspend fun refresh() {
        if (_current.value !is UiState.Success) _current.value = UiState.Loading
        when (val result = apiCall("Não foi possível carregar seu endereço.") { api.current() }) {
            is ApiResult.Success -> _current.value = UiState.Success(result.data)
            is ApiResult.Failure -> if (_current.value !is UiState.Success) _current.value = UiState.Error(result.message)
        }
    }

    suspend fun preview(request: AddressPreviewRequest): ApiResult<AddressPreviewDto> =
        apiCall("Não foi possível conferir o endereço.") { api.preview(request) }

    suspend fun lookupCep(cep: String): ApiResult<CepDto> =
        apiCall("CEP não encontrado.") { api.cep(cep) }

    suspend fun save(request: SaveAddressRequest): ApiResult<MyAddressDto> =
        apiCall("Não foi possível salvar o endereço.") { api.save(request) }.also { result ->
            if (result is ApiResult.Success) _current.value = UiState.Success(result.data)
        }
}
