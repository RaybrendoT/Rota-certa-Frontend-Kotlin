package com.rotacerta.mobile.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotacerta.mobile.core.designsystem.ErrorMessage
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.designsystem.RcLinkButton
import com.rotacerta.mobile.core.designsystem.RcPrimaryButton
import com.rotacerta.mobile.core.designsystem.RcTextField

@Composable
fun ForgotPasswordScreen(
    onFinish: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler {
        if (state.step == ResetStep.IDENTIFY || state.step == ResetStep.SUCCESS) onFinish() else viewModel.back()
    }

    AuthScaffold {
        AnimatedContent(targetState = state.step, label = "reset-step") { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                when (step) {
                    ResetStep.IDENTIFY -> IdentifyStep(state, viewModel, onFinish)
                    ResetStep.VERIFY -> VerifyStep(state, viewModel)
                    ResetStep.NEW_PASSWORD -> NewPasswordStep(state, viewModel)
                    ResetStep.SUCCESS -> SuccessStep(onFinish)
                }
                if (step != ResetStep.SUCCESS) {
                    state.error?.let {
                        Spacer(Modifier.height(16.dp))
                        ErrorMessage(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentifyStep(state: ForgotPasswordUiState, vm: ForgotPasswordViewModel, onBack: () -> Unit) {
    AuthHeader("Recuperar acesso", "Informe seu e-mail corporativo ou telefone")
    RcTextField(state.email, vm::onEmailChange, "E-mail corporativo", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
    Spacer(Modifier.height(16.dp))
    RcTextField(
        state.phone, vm::onPhoneChange, "ou telefone",
        leadingIcon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done,
    )
    Spacer(Modifier.height(28.dp))
    RcPrimaryButton("Continuar", vm::requestCode, loading = state.loading)
    Spacer(Modifier.height(8.dp))
    RcLinkButton("Voltar", onBack)
    Spacer(Modifier.height(8.dp))
    Hint("Você receberá um código para redefinir sua senha.")
}

@Composable
private fun VerifyStep(state: ForgotPasswordUiState, vm: ForgotPasswordViewModel) {
    AuthHeader("Confirme o código", "Digite o código de verificação recebido")
    state.demoCode?.let {
        Text("Código de teste (debug): $it", color = RcColors.Brand, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
    }
    RcTextField(
        state.code, vm::onCodeChange, "Código de verificação",
        leadingIcon = Icons.Outlined.ConfirmationNumber, keyboardType = KeyboardType.Number, imeAction = ImeAction.Done,
    )
    Spacer(Modifier.height(28.dp))
    RcPrimaryButton("Verificar", vm::verifyCode, loading = state.loading)
    Spacer(Modifier.height(8.dp))
    RcLinkButton("Voltar", vm::back)
    Hint("Não recebeu? Volte e solicite um novo código.")
}

@Composable
private fun NewPasswordStep(state: ForgotPasswordUiState, vm: ForgotPasswordViewModel) {
    AuthHeader("Criar nova senha", "Defina sua nova senha de acesso")
    RcTextField(state.newPassword, vm::onNewPasswordChange, "Nova senha", leadingIcon = Icons.Outlined.Lock, isPassword = true)
    Spacer(Modifier.height(16.dp))
    RcTextField(
        state.confirmPassword, vm::onConfirmPasswordChange, "Confirmar senha",
        leadingIcon = Icons.Outlined.Lock, isPassword = true, imeAction = ImeAction.Done,
    )
    PasswordStrengthMeter(state.newPassword)
    Spacer(Modifier.height(28.dp))
    RcPrimaryButton("Salvar senha", vm::savePassword, loading = state.loading)
    Spacer(Modifier.height(8.dp))
    RcLinkButton("Voltar", vm::back)
}

@Composable
private fun SuccessStep(onFinish: () -> Unit) {
    Box(
        Modifier.size(120.dp).background(RcColors.Brand.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = RcColors.Brand, modifier = Modifier.size(70.dp))
    }
    Spacer(Modifier.height(32.dp))
    Text(
        "Senha redefinida\ncom sucesso",
        style = MaterialTheme.typography.headlineMedium,
        color = RcColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Hint("Sua conta já pode ser acessada novamente.")
    Spacer(Modifier.height(36.dp))
    RcPrimaryButton("Voltar ao login", onFinish)
}

@Composable
private fun Hint(text: String) {
    Text(text, color = RcColors.TextMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
}
