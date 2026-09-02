package com.rotacerta.driver.ui.navigationmap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.driver.data.remote.dto.NavigationResponseDto
import com.rotacerta.driver.data.repository.DataResult
import com.rotacerta.driver.data.repository.RouteRepository
import com.rotacerta.driver.ui.navigation.Screen
import com.rotacerta.driver.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
) : ViewModel() {

    val routeId: Long = checkNotNull(savedStateHandle[Screen.ARG_ROUTE_ID])

    private val _uiState = MutableStateFlow<UiState<NavigationResponseDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<NavigationResponseDto>> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            when (val result = routeRepository.navigation(routeId)) {
                is DataResult.Success -> _uiState.update { UiState.Success(result.data) }
                is DataResult.Failure -> _uiState.update { UiState.Error(result.message) }
            }
        }
    }
}
