package com.tomady.nutrition.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TomadyLightColorScheme = lightColorScheme(
    primary = TomadyColors.green,
    onPrimary = TomadyColors.white,
    secondary = TomadyColors.violet,
    onSecondary = TomadyColors.white,
    tertiary = TomadyColors.amber,
    background = TomadyColors.canvas,
    onBackground = TomadyColors.ink,
    surface = TomadyColors.card,
    onSurface = TomadyColors.ink,
    surfaceVariant = TomadyColors.card,
    onSurfaceVariant = TomadyColors.muted,
    outline = TomadyColors.line,
    error = TomadyColors.coral,
    onError = TomadyColors.white,
    errorContainer = TomadyColors.coralTint,
    onErrorContainer = TomadyColors.coralDeep,
)

// Same token set for dark — Tomady's brand palette is intentionally used as-is;
// only background/surface roles shift towards ink so text stays legible.
private val TomadyDarkColorScheme = darkColorScheme(
    primary = TomadyColors.green,
    onPrimary = TomadyColors.ink,
    secondary = TomadyColors.violet,
    onSecondary = TomadyColors.white,
    tertiary = TomadyColors.amber,
    background = TomadyColors.ink,
    onBackground = TomadyColors.white,
    surface = TomadyColors.inkSoft,
    onSurface = TomadyColors.white,
    surfaceVariant = TomadyColors.inkSoft,
    onSurfaceVariant = TomadyColors.line,
    outline = TomadyColors.muted,
    error = TomadyColors.coral,
    onError = TomadyColors.white,
    errorContainer = TomadyColors.coralDeep2,
    onErrorContainer = TomadyColors.coralTint,
)

@Composable
fun TomadyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TomadyDarkColorScheme else TomadyLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TomadyTypography,
        content = content
    )
}
