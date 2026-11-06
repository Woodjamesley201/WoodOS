package dev.woodos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.woodos.launcher.core.Prefs

/**
 * Frosted-glass panel: translucent surface + hairline border, designed to sit
 * on top of a (possibly blurred) wallpaper layer for the glassmorphism look.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    tint: Color = glassColor(),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = tint.alpha), tint.copy(alpha = (tint.alpha * 0.8f).coerceIn(0f, 1f)))
                ),
                shape
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.22f), shape)
    ) {
        content()
    }
}

/** Surface used by WoodDesk windows (mica-style). */
@Composable
fun MicaPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Surface(
        modifier = modifier.border(0.6.dp, Color.White.copy(alpha = 0.14f), shape),
        color = micaColor(),
        shape = shape,
        shadowElevation = 10.dp,
    ) {
        Box { content() }
    }
}

@Composable
fun glassColor(): Color {
    val dark = isWoodDark()
    val alpha = (0.25f + 0.45f * Prefs.transparency).coerceIn(0.2f, 0.86f)
    return if (dark) Color(0xFF181C26).copy(alpha = alpha) else Color(0xFFFFFFFF).copy(alpha = alpha)
}

@Composable
fun micaColor(): Color {
    val dark = isWoodDark()
    val alpha = (0.78f + 0.18f * Prefs.transparency).coerceIn(0.7f, 0.98f)
    return if (dark) Color(0xFF1B2030).copy(alpha = alpha) else Color(0xFFF6F2FA).copy(alpha = alpha)
}

@Composable
fun pillColor(selected: Boolean): Color = if (selected) {
    Material3Accent
} else {
    if (isWoodDark()) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
}

private val Material3Accent: Color
    @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.primary

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassPanel(modifier = modifier, corner = 22.dp) {
        androidx.compose.foundation.layout.Column(Modifier.padding(18.dp)) {
            androidx.compose.material3.Text(
                title,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            content()
        }
    }
}
