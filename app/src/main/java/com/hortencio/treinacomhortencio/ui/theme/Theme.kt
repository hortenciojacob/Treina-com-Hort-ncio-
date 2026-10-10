package com.hortencio.treinacomhortencio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(primary = Color(0xFF087A4A), secondary = Color(0xFF14B86E))
private val DarkColors = darkColorScheme(primary = Color(0xFF55D99B), secondary = Color(0xFF14B86E))

@Composable
fun TreinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
