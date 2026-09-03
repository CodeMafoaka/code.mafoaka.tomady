package com.tomady.nutrition.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Builds the Material3 [androidx.compose.material3.ColorScheme] from whatever
 * [TomadyColors] currently holds — reading these `mutableStateOf` fields
 * means this recomposes automatically when [TomadyColors.applyTheme] runs, so
 * the app-wide theme and every screen's own `TomadyColors.*` references never
 * drift apart (previously the Material3 scheme followed the system's dark
 * mode setting while every screen's background/text hardcoded fixed light
 * values — that mismatch caused text to render in a light-mode color while
 * sitting on an unrelated background, i.e. invisible white-on-white text).
 */
@Composable
fun TomadyTheme(content: @Composable () -> Unit) {
    val colorScheme = if (TomadyColors.isNight) {
        darkColorScheme(
            primary = TomadyColors.green,
            onPrimary = TomadyColors.ink,
            secondary = TomadyColors.violet,
            onSecondary = TomadyColors.ink,
            tertiary = TomadyColors.amber,
            background = TomadyColors.canvas,
            onBackground = TomadyColors.ink,
            surface = TomadyColors.card,
            onSurface = TomadyColors.ink,
            surfaceVariant = TomadyColors.card,
            onSurfaceVariant = TomadyColors.muted,
            outline = TomadyColors.line,
            error = TomadyColors.coral,
            onError = TomadyColors.ink,
            errorContainer = TomadyColors.coralTint,
            onErrorContainer = TomadyColors.coralDeep,
        )
    } else {
        lightColorScheme(
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TomadyTypography,
        content = content
    )
}
