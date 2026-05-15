package com.audicontrol.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AudiColorScheme = darkColorScheme(
    primary = AudiRed,
    onPrimary = AudiWhite,
    primaryContainer = AudiRedDim,
    onPrimaryContainer = AudiWhite,
    secondary = AudiGreyMid,
    onSecondary = AudiWhite,
    background = AudiBlack,
    onBackground = AudiWhite,
    surface = AudiDarkSurface,
    onSurface = AudiWhite,
    surfaceVariant = AudiCardSurface,
    onSurfaceVariant = AudiGreyLight,
    outline = AudiDivider,
    error = AudiRed,
    onError = AudiWhite
)

@Composable
fun AudiControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AudiColorScheme,
        typography = AudiTypography,
        shapes = AudiShapes,
        content = content
    )
}
