package dev.woodos.launcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.woodos.launcher.core.AccentColor
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.ThemeMode

data class WoodPalette(
    val primary: Color,
    val primaryDim: Color,
    val onPrimary: Color,
    val secondary: Color,
    val container: Color,
)

fun paletteFor(accent: AccentColor): WoodPalette = when (accent) {
    AccentColor.WOOD -> WoodPalette(Color(0xFFB5834A), Color(0xFF8D6E4C), Color(0xFFFFFFFF), Color(0xFF6D4C2F), Color(0xFF3A2A1B))
    AccentColor.OCEAN -> WoodPalette(Color(0xFF42A5F5), Color(0xFF1E88E5), Color(0xFFFFFFFF), Color(0xFF1565C0), Color(0xFF10263E))
    AccentColor.AURORA -> WoodPalette(Color(0xFF26C6A6), Color(0xFF00897B), Color(0xFF002B22), Color(0xFF00796B), Color(0xFF0E3B33))
    AccentColor.EMBER -> WoodPalette(Color(0xFFFF8A50), Color(0xFFF4511E), Color(0xFFFFFFFF), Color(0xFFBF360C), Color(0xFF42210B))
    AccentColor.VIOLET -> WoodPalette(Color(0xFF9575CD), Color(0xFF673AB7), Color(0xFFFFFFFF), Color(0xFF4527A0), Color(0xFF251A44))
    AccentColor.TEAL -> WoodPalette(Color(0xFF26A69A), Color(0xFF00897B), Color(0xFFFFFFFF), Color(0xFF00695C), Color(0xFF0B3733))
    AccentColor.ROSE -> WoodPalette(Color(0xFFEC6B9B), Color(0xFFD81B60), Color(0xFFFFFFFF), Color(0xFFAD1457), Color(0xFF3E1027))
}

@Composable
fun isWoodDark(): Boolean = when (Prefs.themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun WoodPalette.composed(dark: Boolean) = this

@Composable
fun WoodTheme(content: @Composable () -> Unit) {
    val dark = isWoodDark()
    val palette = paletteFor(Prefs.accent)
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.secondary,
            secondary = palette.secondary,
            secondaryContainer = palette.container,
            background = Color(0xFF0B0E14),
            onBackground = Color(0xFFE8E4DC),
            surface = Color(0xFF141821),
            onSurface = Color(0xFFE8E4DC),
            surfaceVariant = Color(0xFF1D222E),
            onSurfaceVariant = Color(0xFFB8B2A6),
            outline = Color(0xFF3A4150),
        )
    } else {
        lightColorScheme(
            primary = palette.primaryDim,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFF3E0),
            secondary = palette.secondary,
            background = Color(0xFFF7F3EC),
            onBackground = Color(0xFF241E16),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF241E16),
            surfaceVariant = Color(0xFFEDE7DD),
            onSurfaceVariant = Color(0xFF5C564B),
            outline = Color(0xFFC9C2B6),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
