package com.rotacerta.mobile.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.core.designsystem.ErrorMessage
import com.rotacerta.mobile.core.designsystem.RcLinkButton
import com.rotacerta.mobile.core.designsystem.RcPrimaryButton
import com.rotacerta.mobile.core.designsystem.RcTextField
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.core.util.PasswordRules
import com.rotacerta.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(v: String) = _state.update { it.copy(current = v, error = null) }
    fun onNewChange(v: String) = _state.update { it.copy(new = v, error = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirm = v, error = null) }

    fun submit() {
        val s = _state.value
        val error = when {
            s.current.isEmpty() || s.new.isEmpty() -> "Preencha a senha atual e a nova senha."
            s.new != s.confirm -> "As senhas não coincidem."
            s.new == s.current -> "A nova senha deve ser diferente da atual."
            !PasswordRules.isValid(s.new) -> PasswordRules.REQUIREMENTS
            else -> null
        }
        if (error != null) { _state.update { it.copy(error = error) }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.changePassword(s.current, s.new)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, done = true) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun logout() = viewModelScope.launch { authRepository.logout() }
}

/**
 * @param mandatory primeiro acesso (must_change_password): sem voltar, só "Sair";
 * ao concluir, a sessão atualizada leva o app para a área da pessoa.
 */
@Composable
fun ChangePasswordScreen(
    mandatory: Boolean,
    onBack: (() -> Unit)?,
    onDone: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    AuthScaffold {
        AuthHeader(
            "Trocar senha",
            if (mandatory) "No primeiro acesso é preciso definir uma senha pessoal." else "Defina uma nova senha de acesso",
        )
        RcTextField(state.current, viewModel::onCurrentChange, "Senha atual", leadingIcon = Icons.Outlined.Lock, isPassword = true)
        Spacer(Modifier.height(16.dp))
        RcTextField(state.new, viewModel::onNewChange, "Nova senha", leadingIcon = Icons.Outlined.Lock, isPassword = true)
        Spacer(Modifier.height(16.dp))
        RcTextField(
            state.confirm, viewModel::onConfirmChange, "Confirmar nova senha",
            leadingIcon = Icons.Outlined.Lock, isPassword = true, imeAction = ImeAction.Done,
        )
        PasswordStrengthMeter(state.new)
        state.error?.let {
            Spacer(Modifier.height(16.dp))
            ErrorMessage(it)
        }
        Spacer(Modifier.height(28.dp))
        RcPrimaryButton("Salvar senha", viewModel::submit, loading = state.loading)
        Spacer(Modifier.height(8.dp))
        if (mandatory) {
            RcLinkButton("Sair", { viewModel.logout() })
        } else if (onBack != null) {
            RcLinkButton("Cancelar", onBack)
        }
    }
}
