package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GhostColorScheme = darkColorScheme(
    primary = GhostPrimary,
    secondary = GhostSecondary,
    tertiary = GhostTertiary,
    background = GhostBackground,
    surface = GhostSurface,
    surfaceVariant = GhostSurfaceVariant,
    onPrimary = GhostBackground,
    onSecondary = GhostBackground,
    onBackground = GhostTextPrimary,
    onSurface = GhostTextPrimary,
    onSurfaceVariant = GhostTextSecondary,
    outline = GhostBorder,
    error = GhostError
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GhostColorScheme,
        typography = Typography,
        content = content
    )
}
