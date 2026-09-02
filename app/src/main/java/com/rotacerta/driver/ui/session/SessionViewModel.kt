package com.rotacerta.driver.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.driver.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SessionStatus {
    Loading,
    LoggedOut,
    MustChangePassword,
    Ready,
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val status: StateFlow<SessionStatus> = authRepository.session
        .map { session ->
            when {
                session == null -> SessionStatus.LoggedOut
                session.mustChangePassword -> SessionStatus.MustChangePassword
                else -> SessionStatus.Ready
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionStatus.Loading)
}
