package com.rotacerta.mobile.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RcColorScheme = darkColorScheme(
    primary = RcColors.Primary,
    onPrimary = Color.White,
    primaryContainer = RcColors.Input,
    onPrimaryContainer = RcColors.TextPrimary,
    secondary = RcColors.Brand,
    background = RcColors.Background,
    onBackground = RcColors.TextPrimary,
    surface = RcColors.Background,
    onSurface = RcColors.TextPrimary,
    surfaceVariant = RcColors.Card,
    onSurfaceVariant = RcColors.TextSecondary,
    surfaceContainer = RcColors.Card,
    surfaceContainerHigh = RcColors.CardHighlight,
    outline = RcColors.Divider,
    error = RcColors.Danger,
    onError = Color.White,
    errorContainer = RcColors.Danger.copy(alpha = 0.15f),
    onErrorContainer = RcColors.Danger,
)

private val RcTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)

/** O app é sempre escuro, como a identidade visual da marca. */
@Composable
fun RotaCertaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RcColorScheme, typography = RcTypography, content = content)
}
