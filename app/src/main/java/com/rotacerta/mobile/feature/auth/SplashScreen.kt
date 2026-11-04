package com.rotacerta.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotacerta.mobile.core.designsystem.BrandTitle
import com.rotacerta.mobile.core.designsystem.RcColors

/** Marca do app; aparece enquanto a sessão salva é lida e no topo do login. */
@Composable
fun BrandLogo(iconSize: Int = 95, titleSize: Int = 40) {
    Box(
        Modifier.size(iconSize.dp).background(RcColors.Brand, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((iconSize * 0.63f).dp),
        )
    }
    Spacer(Modifier.height(24.dp))
    BrandTitle(fontSize = titleSize.sp)
    Spacer(Modifier.height(8.dp))
    Text(
        "transporte corporativo\ninteligente",
        color = RcColors.TextSecondary,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun SplashScreen() {
    AuthScaffold { BrandLogo() }
}
