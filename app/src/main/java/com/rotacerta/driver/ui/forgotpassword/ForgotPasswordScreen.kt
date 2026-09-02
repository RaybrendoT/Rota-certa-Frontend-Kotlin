package com.rotacerta.driver.ui.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rotacerta.driver.ui.common.ErrorBanner

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
        }

        when (uiState.step) {
            ForgotPasswordStep.REQUEST_CODE -> RequestCodeStep(uiState, viewModel)
            ForgotPasswordStep.RESET_PASSWORD -> ResetPasswordStep(uiState, viewModel)
            ForgotPasswordStep.DONE -> DoneStep(onBack)
        }
    }
}

@Composable
private fun RequestCodeStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Text("Esqueci minha senha", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Informe o e-mail cadastrado para receber um código de verificação.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
    )
    OutlinedTextField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChange,
        label = { Text("E-mail") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    uiState.errorMessage?.let {
        Spacer(Modifier.height(12.dp))
        ErrorBanner(it)
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = viewModel::requestCode,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
        } else {
            Text("Enviar código")
        }
    }
}

@Composable
private fun ResetPasswordStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Text("Digite o código recebido", style = MaterialTheme.typography.headlineSmall)
    uiState.infoMessage?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
    }
    OutlinedTextField(
        value = uiState.code,
        onValueChange = viewModel::onCodeChange,
        label = { Text("Código de verificação") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = uiState.newPassword,
        onValueChange = viewModel::onNewPasswordChange,
        label = { Text("Nova senha") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    uiState.errorMessage?.let {
        Spacer(Modifier.height(12.dp))
        ErrorBanner(it)
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = viewModel::resetPassword,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
        } else {
            Text("Redefinir senha")
        }
    }
}

@Composable
private fun DoneStep(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(56.dp),
        )
        Text("Senha redefinida com sucesso!", style = MaterialTheme.typography.titleLarge)
        Text(
            "Volte para a tela de login e entre com sua nova senha.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onBack) { Text("Voltar ao login") }
    }
}
