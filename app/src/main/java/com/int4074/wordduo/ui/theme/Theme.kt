package com.int4074.wordduo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CiGuangColors = lightColorScheme(
    primary = Color(0xFF8F86FF),
    onPrimary = Color.White,
    secondary = Color(0xFFF1B87F),
    background = Color(0xFFFFFAF2),
    surface = Color(0xFFFFFCF8),
    onSurface = Color(0xFF2D241E),
    onBackground = Color(0xFF2D241E)
)

@Composable
fun WordDuoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CiGuangColors,
        content = content
    )
}
