package com.tomady.nutrition.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Tomady design tokens. Every field is a Compose observable `var` so switching
 * the active theme (see [ThemeManager]/[applyTheme]) recomposes every screen
 * that reads `TomadyColors.*` without needing a `CompositionLocal` threaded
 * through the whole tree. Defaults below match the "light" theme and are only
 * used until the real persisted choice is loaded at startup.
 */
object TomadyColors {
    var themeName by mutableStateOf("light")
    var isNight by mutableStateOf(false)

    var green by mutableStateOf(Color(0xFF2ECC71))
    var greenDeep by mutableStateOf(Color(0xFF1E8449))
    var greenTint by mutableStateOf(Color(0xFFEAFBF1))
    var greenLine by mutableStateOf(Color(0xFFCDEFDC))

    var amber by mutableStateOf(Color(0xFFF39C12))
    var amberTint by mutableStateOf(Color(0xFFFEF3E2))
    var amberDeep by mutableStateOf(Color(0xFF9A6208))
    var amberDeep2 by mutableStateOf(Color(0xFF7A4E05))

    var coral by mutableStateOf(Color(0xFFE74C3C))
    var coralTint by mutableStateOf(Color(0xFFFDEDEA))
    var coralDeep by mutableStateOf(Color(0xFFB23324))
    var coralDeep2 by mutableStateOf(Color(0xFF8C2A1E))

    var ink by mutableStateOf(Color(0xFF16241C))
    var inkSoft by mutableStateOf(Color(0xFF2D3C34))
    var muted by mutableStateOf(Color(0xFF4F5F56))
    var line by mutableStateOf(Color(0xFFE7EDE9))
    var white by mutableStateOf(Color(0xFFFFFFFF))
    var canvas by mutableStateOf(Color(0xFFF0F5F2))
    var card by mutableStateOf(Color(0xFFF8FAF9))

    var blue by mutableStateOf(Color(0xFF5FA8E0))
    var violet by mutableStateOf(Color(0xFF8B5CF6))
    var violetTint by mutableStateOf(Color(0xFFF0EDFF))
    var violetDeep by mutableStateOf(Color(0xFF6D3ADE))

    /** Applies a freshly-loaded [ThemeColorSet], updating every token in place. */
    fun applyTheme(t: ThemeColorSet) {
        themeName = t.name
        isNight = t.name != "light"
        green = t.green
        greenDeep = t.greenDeep
        greenTint = t.greenTint
        greenLine = t.greenLine
        amber = t.amber
        amberTint = t.amberTint
        amberDeep = t.amberDeep
        amberDeep2 = t.amberDeep2
        coral = t.coral
        coralTint = t.coralTint
        coralDeep = t.coralDeep
        coralDeep2 = t.coralDeep2
        ink = t.ink
        inkSoft = t.inkSoft
        muted = t.muted
        line = t.line
        white = t.white
        canvas = t.canvas
        card = t.card
        blue = t.blue
        violet = t.violet
        violetTint = t.violetTint
        violetDeep = t.violetDeep
    }
}
