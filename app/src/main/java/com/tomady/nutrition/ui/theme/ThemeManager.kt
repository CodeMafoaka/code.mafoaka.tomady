package com.tomady.nutrition.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** One full set of design-token colors, as loaded from an `assets/themes` JSON file. */
data class ThemeColorSet(
    val name: String,
    val label: String,
    val green: Color,
    val greenDeep: Color,
    val greenTint: Color,
    val greenLine: Color,
    val amber: Color,
    val amberTint: Color,
    val amberDeep: Color,
    val amberDeep2: Color,
    val coral: Color,
    val coralTint: Color,
    val coralDeep: Color,
    val coralDeep2: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val line: Color,
    val white: Color,
    val canvas: Color,
    val card: Color,
    val blue: Color,
    val violet: Color,
    val violetTint: Color,
    val violetDeep: Color
)

/**
 * Loads theme definitions from `assets/themes/<name>.json`. Themes describe a
 * flat map of design-token names to hex colors — see [ThemeColorSet] for the
 * full token list. Only "light" and "night" ship for now, but any JSON file
 * dropped into that assets folder (with the same keys) becomes selectable.
 */
object ThemeManager {
    val availableThemes = listOf("light", "night")

    fun load(context: Context, name: String): ThemeColorSet {
        val safeName = if (name in availableThemes) name else "light"
        val json = context.assets.open("themes/$safeName.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        fun color(key: String) = Color(AndroidColor.parseColor(obj.getString(key)))

        return ThemeColorSet(
            name = obj.optString("name", safeName),
            label = obj.optString("label", safeName),
            green = color("green"),
            greenDeep = color("greenDeep"),
            greenTint = color("greenTint"),
            greenLine = color("greenLine"),
            amber = color("amber"),
            amberTint = color("amberTint"),
            amberDeep = color("amberDeep"),
            amberDeep2 = color("amberDeep2"),
            coral = color("coral"),
            coralTint = color("coralTint"),
            coralDeep = color("coralDeep"),
            coralDeep2 = color("coralDeep2"),
            ink = color("ink"),
            inkSoft = color("inkSoft"),
            muted = color("muted"),
            line = color("line"),
            white = color("white"),
            canvas = color("canvas"),
            card = color("card"),
            blue = color("blue"),
            violet = color("violet"),
            violetTint = color("violetTint"),
            violetDeep = color("violetDeep")
        )
    }
}
