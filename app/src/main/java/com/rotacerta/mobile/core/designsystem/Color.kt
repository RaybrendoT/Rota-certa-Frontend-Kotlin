package com.rotacerta.mobile.core.designsystem

import androidx.compose.ui.graphics.Color

/** Paleta da identidade visual Rota Certa (herdada do app Flutter). */
object RcColors {
    /** Fundo das telas de entrada (splash, login, recuperação de senha). */
    val AuthBackground = Color(0xFF020B1A)

    /** Fundo das telas internas (dashboards). */
    val Background = Color(0xFF0F1923)
    val Card = Color(0xFF1A2535)
    val CardHighlight = Color(0xFF23364D)
    val Input = Color(0xFF13294B)
    val NavBar = Color(0xFF0D1821)
    val Divider = Color(0xFF1E2D3D)

    /** Azul da marca ("certa" do logotipo). */
    val Brand = Color(0xFF1E90FF)
    val Primary = Color(0xFF4DA6FF)
    val Button = Color(0xFF2EA7FF)

    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF8A9BB0)
    val TextMuted = Color(0xFF4A6070)

    val Success = Color(0xFF2ECC71)
    val Warning = Color(0xFFE67E22)
    val Danger = Color(0xFFE74C3C)
}
