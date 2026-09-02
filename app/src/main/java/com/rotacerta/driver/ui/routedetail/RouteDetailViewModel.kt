package com.rotacerta.driver.ui.routedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.driver.data.remote.dto.DriverRouteBoardDto
import com.rotacerta.driver.data.remote.dto.RouteEventDto
import com.rotacerta.driver.data.repository.DataResult
import com.rotacerta.driver.data.repository.RouteRepository
import com.rotacerta.driver.ui.navigation.Screen
import com.rotacerta.driver.util.ActionState
import com.rotacerta.driver.util.LocationHelper
import com.rotacerta.driver.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventFormState(
    val isVisible: Boolean = false,
    val eventType: String = "",
    val description: String = "",
    val submitState: ActionState = ActionState.Idle,
)

data class RouteDetailUiState(
    val routeState: UiState<DriverRouteBoardDto> = UiState.Loading,
    val eventForm: EventFormState = EventFormState(),
    val history: List<RouteEventDto>? = null,
    val historyVisible: Boolean = false,
    val passengerActionInProgressId: Long? = null,
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val locationHelper: LocationHelper,
) : ViewModel() {

    val routeId: Long = checkNotNull(savedStateHandle[Screen.ARG_ROUTE_ID])

    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(routeState = UiState.Loading) }
            when (val result = routeRepository.routeDetail(routeId)) {
                is DataResult.Success -> _uiState.update { it.copy(routeState = UiState.Success(result.data)) }
                is DataResult.Failure -> _uiState.update { it.copy(routeState = UiState.Error(result.message)) }
            }
        }
    }

    fun updateBoardingStatus(routePassengerId: Long, status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(passengerActionInProgressId = routePassengerId) }
            routeRepository.updateBoardingStatus(routePassengerId, status)
            _uiState.update { it.copy(passengerActionInProgressId = null) }
            load()
        }
    }

    fun toggleHistory() {
        val show = !_uiState.value.historyVisible
        _uiState.update { it.copy(historyVisible = show) }
        if (show && _uiState.value.history == null) {
            viewModelScope.launch {
                when (val result = routeRepository.routeEvents(routeId)) {
                    is DataResult.Success -> _uiState.update { it.copy(history = result.data) }
                    is DataResult.Failure -> Unit // histórico é secundário; ignora silenciosamente
                }
            }
        }
    }

    fun openEventForm(eventType: String) {
        _uiState.update {
            it.copy(eventForm = EventFormState(isVisible = true, eventType = eventType))
        }
    }

    fun closeEventForm() {
        _uiState.update { it.copy(eventForm = EventFormState()) }
    }

    fun onEventDescriptionChange(value: String) {
        _uiState.update { it.copy(eventForm = it.eventForm.copy(description = value)) }
    }

    fun submitEvent() {
        val form = _uiState.value.eventForm
        viewModelScope.launch {
            _uiState.update { it.copy(eventForm = it.eventForm.copy(submitState = ActionState.InProgress)) }

            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _uiState.update {
                    it.copy(
                        eventForm = it.eventForm.copy(
                            submitState = ActionState.Error(
                                "Não foi possível obter sua localização. Verifique o GPS e a permissão de localização.",
                            ),
                        ),
                    )
                }
                return@launch
            }

            val result = routeRepository.registerEvent(
                routeId = routeId,
                eventType = form.eventType,
                description = form.description,
                latitude = location.latitude,
                longitude = location.longitude,
            )

            when (result) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(eventForm = EventFormState(), history = null) }
                    load()
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(eventForm = it.eventForm.copy(submitState = ActionState.Error(result.message)))
                }
            }
        }
    }
}
