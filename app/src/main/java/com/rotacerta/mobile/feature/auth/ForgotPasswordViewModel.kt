package com.rotacerta.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.BuildConfig
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.util.PasswordRules
import com.rotacerta.mobile.core.util.Validators
import com.rotacerta.mobile.data.dto.PasswordResetIdentifier
import com.rotacerta.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ResetStep { IDENTIFY, VERIFY, NEW_PASSWORD, SUCCESS }

data class ForgotPasswordUiState(
    val step: ResetStep = ResetStep.IDENTIFY,
    val email: String = "",
    val phone: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    /** Código devolvido pelo backend em DEBUG; só exibido em build debug, para testes. */
    val demoCode: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun onCodeChange(v: String) = _state.update { it.copy(code = v, error = null) }
    fun onNewPasswordChange(v: String) = _state.update { it.copy(newPassword = v, error = null) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, error = null) }

    fun back() = _state.update {
        val previous = when (it.step) {
            ResetStep.NEW_PASSWORD -> ResetStep.VERIFY
            else -> ResetStep.IDENTIFY
        }
        it.copy(step = previous, error = null)
    }

    /** E-mail tem prioridade sobre telefone, como no app Flutter. */
    private fun identifier(): PasswordResetIdentifier? {
        val s = _state.value
        return when {
            s.email.isNotBlank() -> PasswordResetIdentifier(email = s.email.trim())
            s.phone.isNotBlank() -> PasswordResetIdentifier(phone = s.phone.trim())
            else -> null
        }
    }

    private fun identifierError(id: PasswordResetIdentifier?): String? = when {
        id == null -> "Informe seu e-mail ou telefone."
        id.email != null && !Validators.isValidEmail(id.email) -> "Informe um e-mail válido."
        id.phone != null && !Validators.isValidPhone(id.phone) -> "Informe um telefone válido."
        else -> null
    }

    fun requestCode() {
        val id = identifier()
        identifierError(id)?.let { msg -> _state.update { it.copy(error = msg) }; return }
        launchStep {
            when (val result = authRepository.requestPasswordReset(id!!)) {
                is ApiResult.Success -> _state.update {
                    it.copy(step = ResetStep.VERIFY, demoCode = result.data?.takeIf { BuildConfig.DEBUG })
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun verifyCode() {
        val id = identifier() ?: return
        val code = _state.value.code.trim()
        if (code.isEmpty()) { _state.update { it.copy(error = "Informe o código de verificação.") }; return }
        launchStep {
            when (val result = authRepository.verifyPasswordReset(id, code)) {
                is ApiResult.Success ->
                    if (result.data) _state.update { it.copy(step = ResetStep.NEW_PASSWORD) }
                    else _state.update { it.copy(error = "Código inválido ou expirado.") }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun savePassword() {
        val s = _state.value
        val id = identifier() ?: return
        val error = when {
            s.newPassword.isEmpty() || s.confirmPassword.isEmpty() -> "Preencha a nova senha e a confirmação."
            s.newPassword != s.confirmPassword -> "As senhas não coincidem."
            !PasswordRules.isValid(s.newPassword) -> PasswordRules.REQUIREMENTS
            else -> null
        }
        if (error != null) { _state.update { it.copy(error = error) }; return }
        launchStep {
            when (val result = authRepository.completePasswordReset(id, s.code, s.newPassword)) {
                is ApiResult.Success -> _state.update { it.copy(step = ResetStep.SUCCESS) }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    private fun launchStep(block: suspend () -> Unit) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            block()
            _state.update { it.copy(loading = false) }
        }
    }
}
