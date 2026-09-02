package com.rotacerta.driver.ui.myroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.driver.data.local.DriverSession
import com.rotacerta.driver.data.remote.dto.DriverRouteBoardDto
import com.rotacerta.driver.data.repository.AuthRepository
import com.rotacerta.driver.data.repository.DataResult
import com.rotacerta.driver.data.repository.RouteRepository
import com.rotacerta.driver.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRoutesViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<DriverRouteBoardDto>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<DriverRouteBoardDto>>> = _uiState

    val driverSession: StateFlow<DriverSession?> = authRepository.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            when (val result = routeRepository.myRoutes()) {
                is DataResult.Success -> _uiState.update { UiState.Success(result.data) }
                is DataResult.Failure -> _uiState.update { UiState.Error(result.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
