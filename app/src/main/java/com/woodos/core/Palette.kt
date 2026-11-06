package com.woodos.core

import android.content.res.Configuration
import android.content.Context

/**
 * WoodOS color system: a small set of accents plus dark/light surface
 * palettes. Dark is the default.
 */
object Palette {

    data class Accent(val name: String, val color: Int)

    val ACCENTS = listOf(
        Accent("Ember", 0xFFE8A15D.toInt()),   // warm wood amber (default)
        Accent("Moss", 0xFF8FBF7F.toInt()),
        Accent("Sky", 0xFF6FB4D8.toInt()),
        Accent("Rose", 0xFFD98BA6.toInt()),
        Accent("Violet", 0xFF9E8FD8.toInt()),
        Accent("Teal", 0xFF6FC8B8.toInt()),
    )

    data class ThemeColors(
        val isDark: Boolean,
        val background: Int,
        val surface: Int,
        val surfaceAlt: Int,
        val text: Int,
        val textDim: Int,
        val divider: Int,
    )

    fun colors(prefs: Prefs, context: Context): ThemeColors {
        val dark = when (prefs.theme) {
            "light" -> false
            "system" ->
                (context.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO
            else -> true
        }
        return if (dark) ThemeColors(
            isDark = true,
            background = 0xFF0B0B0E.toInt(),
            surface = 0xFF16161A.toInt(),
            surfaceAlt = 0xFF1F1F24.toInt(),
            text = 0xFFF2F2F4.toInt(),
            textDim = 0xFF9A9AA2.toInt(),
            divider = 0x1FFFFFFF,
        ) else ThemeColors(
            isDark = false,
            background = 0xFFF4F3F1.toInt(),
            surface = 0xFFFCFBFA.toInt(),
            surfaceAlt = 0xFFEDEBE7.toInt(),
            text = 0xFF1C1B1A.toInt(),
            textDim = 0xFF6E6C68.toInt(),
            divider = 0x14000000,
        )
    }
}
