package com.rotacerta.mobile.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.core.session.SessionStore
import com.rotacerta.mobile.core.session.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Qual "app" mostrar; muda sozinho no login, logout, troca de senha ou sessão expirada. */
sealed interface RootState {
    data object Loading : RootState
    data object LoggedOut : RootState
    data object MustChangePassword : RootState
    data class Ready(val role: UserRole) : RootState
}

@HiltViewModel
class SessionViewModel @Inject constructor(sessionStore: SessionStore) : ViewModel() {
    val root: StateFlow<RootState> = sessionStore.session
        .map { session ->
            when {
                session == null -> RootState.LoggedOut
                session.mustChangePassword -> RootState.MustChangePassword
                else -> RootState.Ready(session.role)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState.Loading)
}
