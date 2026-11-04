package com.rotacerta.mobile.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rotacerta.mobile.core.designsystem.ErrorMessage
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcLinkButton
import com.rotacerta.mobile.core.designsystem.RcPrimaryButton
import com.rotacerta.mobile.core.designsystem.RcTextField
import com.rotacerta.mobile.core.network.ApiResult
import com.rotacerta.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit get() = email.isNotBlank() && password.isNotEmpty() && !loading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    /** Sucesso não navega daqui: a sessão salva troca a raiz do app (AppRoot). */
    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = authRepository.login(current.email, current.password)
            _state.update {
                it.copy(loading = false, error = (result as? ApiResult.Failure)?.message)
            }
        }
    }
}

@Composable
fun LoginScreen(
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AuthScaffold {
        BrandLogo(iconSize = 72, titleSize = 34)
        Spacer(Modifier.height(40.dp))

        RcTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "E-mail corporativo",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(16.dp))
        RcTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "Senha",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeDone = { if (state.canSubmit && !state.loading) viewModel.submit() },
        )

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            ErrorMessage(it)
        }

        Spacer(Modifier.height(28.dp))
        RcPrimaryButton(
            text = "Entrar",
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            loading = state.loading,
        )
        Spacer(Modifier.height(16.dp))
        RcLinkButton("Esqueci minha senha", onForgotPassword, color = RcColors.Brand)
    }
}
