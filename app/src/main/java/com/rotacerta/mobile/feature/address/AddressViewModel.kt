package com.rotacerta.mobile.feature.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.core.location.LocationFix
import com.rotacerta.mobile.core.location.LocationHelper
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.util.UiState
import com.rotacerta.mobile.data.dto.AddressPreviewDto
import com.rotacerta.mobile.data.dto.AddressPreviewRequest
import com.rotacerta.mobile.data.dto.MyAddressDto
import com.rotacerta.mobile.data.dto.SaveAddressRequest
import com.rotacerta.mobile.data.repository.AddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mesmos limites do backend (core/self_address.py); lá é que valem, aqui só orientam a tela. */
const val MAX_GPS_ACCURACY_METERS = 50f
const val MAX_PIN_SHIFT_METERS = 300.0

enum class AddressStep { Choose, Locating, Typing, Confirm, Done }

data class AddressForm(
    val postalCode: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
)

data class AddressUiState(
    val step: AddressStep = AddressStep.Choose,
    val form: AddressForm = AddressForm(),
    val preview: AddressPreviewDto? = null,
    /** Leitura do GPS usada na prévia (o alfinete não pode se afastar dela). */
    val gps: LocationFix? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val doneMessage: String? = null,
)

/**
 * Atualização do endereço residencial pelo próprio colaborador: localização atual (GPS) ou digitado
 * (CEP + número). Nos dois casos o backend confere antes (prévia) e de novo ao gravar.
 */
@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repository: AddressRepository,
    private val locationHelper: LocationHelper,
) : ViewModel() {
    private val _state = MutableStateFlow(AddressUiState())
    val state: StateFlow<AddressUiState> = _state.asStateFlow()

    val current: StateFlow<UiState<MyAddressDto>> = repository.current
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Idle)

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun hasLocationPermission(): Boolean = locationHelper.hasPermission()

    fun useCurrentLocation() {
        _state.update { it.copy(step = AddressStep.Locating, error = null) }
        viewModelScope.launch {
            val fix = locationHelper.preciseFix(MAX_GPS_ACCURACY_METERS)
            val problem = when {
                fix == null -> "Não conseguimos sua localização. Confira se o GPS está ligado ou digite o endereço."
                fix.isMock -> "A localização veio de um app de localização simulada. Desative-o e tente de novo."
                (fix.accuracy ?: Float.MAX_VALUE) > MAX_GPS_ACCURACY_METERS ->
                    "Sinal de GPS fraco (precisão de ${fix.accuracy?.toInt() ?: "?"} m; o mínimo é " +
                        "${MAX_GPS_ACCURACY_METERS.toInt()} m). Vá para perto de uma janela ou digite o endereço."
                else -> null
            }
            if (problem != null || fix == null) {
                _state.update { it.copy(step = AddressStep.Choose, error = problem) }
                return@launch
            }
            val request = AddressPreviewRequest(
                mode = "gps",
                latitude = fix.point.latitude,
                longitude = fix.point.longitude,
                accuracy = fix.accuracy,
                isMock = fix.isMock,
            )
            when (val result = repository.preview(request)) {
                is ApiResult.Success -> _state.update { state ->
                    val found = result.data.address
                    state.copy(
                        step = AddressStep.Confirm,
                        preview = result.data,
                        gps = fix,
                        form = state.form.copy(
                            postalCode = found.postalCode, street = found.street, number = found.number,
                            neighborhood = found.neighborhood, city = found.city, state = found.state,
                        ),
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(step = AddressStep.Choose, error = result.message) }
            }
        }
    }

    fun typeAddress() {
        val known = (current.value as? UiState.Success)?.data?.address
        _state.update { state ->
            state.copy(
                step = AddressStep.Typing,
                error = null,
                gps = null,
                form = if (known != null && state.form == AddressForm()) {
                    AddressForm(known.postalCode, known.street, known.number, known.complement, known.neighborhood, known.city, known.state)
                } else {
                    state.form
                },
            )
        }
    }

    fun permissionDenied() {
        _state.update { it.copy(error = "Sem permissão de localização. Você pode digitar o endereço.") }
    }

    fun updateForm(transform: (AddressForm) -> AddressForm) {
        val before = _state.value.form
        val after = transform(before)
        _state.update { it.copy(form = after, error = null) }
        val digits = after.postalCode.filter(Char::isDigit)
        if (digits.length == 8 && digits != before.postalCode.filter(Char::isDigit)) lookupCep(digits)
    }

    private fun lookupCep(digits: String) = viewModelScope.launch {
        when (val result = repository.lookupCep(digits)) {
            is ApiResult.Success -> _state.update { state ->
                val cep = result.data
                state.copy(
                    form = state.form.copy(
                        postalCode = cep.postalCode,
                        street = cep.street.ifBlank { state.form.street },
                        neighborhood = cep.neighborhood.ifBlank { state.form.neighborhood },
                        city = cep.city.ifBlank { state.form.city },
                        state = cep.state.ifBlank { state.form.state },
                    ),
                )
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
        }
    }

    fun checkTypedAddress() {
        val form = _state.value.form
        val missing = listOf(
            "CEP" to form.postalCode, "rua" to form.street, "número" to form.number,
            "cidade" to form.city, "UF" to form.state,
        ).filter { it.second.isBlank() }.map { it.first }
        if (missing.isNotEmpty()) {
            _state.update { it.copy(error = "Preencha: ${missing.joinToString()}.") }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val request = AddressPreviewRequest(
                mode = "typed", postalCode = form.postalCode, street = form.street, number = form.number,
                neighborhood = form.neighborhood, city = form.city, state = form.state,
            )
            when (val result = repository.preview(request)) {
                is ApiResult.Success -> _state.update { state ->
                    val found = result.data.address
                    state.copy(
                        busy = false,
                        step = AddressStep.Confirm,
                        preview = result.data,
                        form = state.form.copy(
                            street = found.street, neighborhood = found.neighborhood.ifBlank { state.form.neighborhood },
                            city = found.city, state = found.state,
                        ),
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    /** Grava o endereço com o alfinete em [latitude]/[longitude] (centro do mapa na confirmação). */
    fun confirm(latitude: Double, longitude: Double) {
        val state = _state.value
        val preview = state.preview ?: return
        val form = state.form
        if (form.street.isBlank() || form.number.isBlank()) {
            _state.update { it.copy(error = "Preencha a rua e o número.") }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val request = SaveAddressRequest(
                source = preview.source,
                street = form.street, number = form.number, complement = form.complement,
                neighborhood = form.neighborhood, city = form.city, state = form.state, postalCode = form.postalCode,
                latitude = latitude, longitude = longitude,
                gpsLatitude = state.gps?.point?.latitude,
                gpsLongitude = state.gps?.point?.longitude,
                gpsAccuracy = state.gps?.accuracy,
                gpsIsMock = state.gps?.isMock ?: false,
            )
            when (val result = repository.save(request)) {
                is ApiResult.Success -> _state.update {
                    it.copy(busy = false, step = AddressStep.Done, doneMessage = result.data.message ?: "Endereço salvo.")
                }
                is ApiResult.Failure -> _state.update { it.copy(busy = false, error = result.message) }
            }
        }
    }

    /** Volta um passo; devolve false quando já está no início (a tela fecha). */
    fun back(): Boolean {
        val step = _state.value.step
        if (step == AddressStep.Choose || step == AddressStep.Done || step == AddressStep.Locating) return false
        _state.update {
            it.copy(
                step = if (step == AddressStep.Confirm && it.gps == null) AddressStep.Typing else AddressStep.Choose,
                error = null,
            )
        }
        return true
    }
}
