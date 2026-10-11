package com.hortencio.treinacomhortencio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val Forest = Color(0xFF123D2B)
private val Leaf = Color(0xFF087A4A)
private val Mint = Color(0xFFB5D9C5)
private val Terracotta = Color(0xFFC47C43)

private val LightColors = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    secondary = Terracotta,
    onSecondary = Color.White,
    tertiary = Forest,
    background = Color(0xFFF6F8F5),
    surface = Color(0xFFFFFEFC),
    onSurface = Forest
)
private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Forest,
    secondary = Color(0xFFE3A56F),
    onSecondary = Forest,
    tertiary = Color(0xFF78B795),
    background = Color(0xFF101A14),
    surface = Color(0xFF17241B),
    onSurface = Color(0xFFE6EFE9)
)

@Composable
fun TreinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
