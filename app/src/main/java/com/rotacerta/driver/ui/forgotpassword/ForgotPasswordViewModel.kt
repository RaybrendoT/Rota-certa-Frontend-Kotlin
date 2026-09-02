package com.rotacerta.driver.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.driver.data.repository.AuthRepository
import com.rotacerta.driver.data.repository.SimpleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ForgotPasswordStep { REQUEST_CODE, RESET_PASSWORD, DONE }

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.REQUEST_CODE,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value, errorMessage = null) }
    fun onNewPasswordChange(value: String) =
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }

    fun requestCode() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe seu e-mail.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.requestPasswordReset(email)) {
                is SimpleResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = ForgotPasswordStep.RESET_PASSWORD,
                        infoMessage = "Se o e-mail existir, um código de verificação foi enviado.",
                    )
                }
                is SimpleResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.code.isBlank() || state.newPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o código e a nova senha.") }
            return
        }
        if (state.newPassword.length < 8) {
            _uiState.update { it.copy(errorMessage = "A nova senha deve ter ao menos 8 caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = authRepository.completePasswordReset(state.email, state.code, state.newPassword)
            ) {
                is SimpleResult.Success -> _uiState.update {
                    it.copy(isLoading = false, step = ForgotPasswordStep.DONE)
                }
                is SimpleResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
