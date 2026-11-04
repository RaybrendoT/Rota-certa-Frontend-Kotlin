package com.rotacerta.mobile.feature.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotacerta.mobile.core.designsystem.RcColors
import com.rotacerta.mobile.core.util.PasswordRules
import com.rotacerta.mobile.core.util.PasswordStrength

/** Fundo escuro da marca com o conteúdo centralizado e rolável (cabe o teclado). */
@Composable
fun AuthScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(RcColors.AuthBackground)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun AuthHeader(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, color = RcColors.Brand, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = RcColors.TextSecondary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(36.dp))
}

/** Barra de força da senha (só aparece depois que a pessoa começa a digitar). */
@Composable
fun PasswordStrengthMeter(password: String) {
    if (password.isEmpty()) return
    val strength = PasswordRules.strength(password)
    val progress by animateFloatAsState(PasswordRules.score(password) / 5f, label = "password-strength")
    val color = when (strength) {
        PasswordStrength.WEAK -> RcColors.Danger
        PasswordStrength.MEDIUM -> Color(0xFFFFC107)
        PasswordStrength.GOOD -> Color(0xFF8BC34A)
        PasswordStrength.STRONG -> RcColors.Success
    }
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Força da senha: ${strength.label}", color = RcColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
