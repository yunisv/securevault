package com.securevault.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = Color(0xFF1E3A5F),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD6E4F7),
    secondary        = Color(0xFF1A7F8E),
    secondaryContainer = Color(0xFFD0F4F7),
    error            = Color(0xFFB00020),
    errorContainer   = Color(0xFFFFDAD6),
)

@Composable
fun SecureVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}
