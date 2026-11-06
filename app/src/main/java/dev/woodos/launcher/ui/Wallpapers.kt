package dev.woodos.launcher.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.woodos.launcher.core.Perf
import dev.woodos.launcher.core.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Generated, resolution-independent WoodOS wallpapers (no bundled image assets,
 * keeping the APK tiny and the UI crisp on the 1340x800 Tab A9 panel).
 * An optional custom image (picked via SAF) overrides the generated one.
 */

data class WallpaperPreset(val id: Int, val name: String, val colors: List<Color>)

val wallpaperPresets = listOf(
    WallpaperPreset(0, "Wood Dusk", listOf(Color(0xFF2B1D33), Color(0xFF4A2E38), Color(0xFF8D5A4A), Color(0xFFD9A566))),
    WallpaperPreset(1, "Aurora", listOf(Color(0xFF081C26), Color(0xFF0E4D4A), Color(0xFF26A69A), Color(0xFF7EE8C7))),
    WallpaperPreset(2, "Deep Ocean", listOf(Color(0xFF04122E), Color(0xFF0A2A5E), Color(0xFF1565C0), Color(0xFF64B5F6))),
    WallpaperPreset(3, "Ember", listOf(Color(0xFF1D0E0A), Color(0xFF4E1B0C), Color(0xFFBF360C), Color(0xFFFFB74D))),
    WallpaperPreset(4, "Midnight Mono", listOf(Color(0xFF0A0A0C), Color(0xFF23252B), Color(0xFF4A4D57), Color(0xFF9AA0AC))),
    WallpaperPreset(5, "Dawn Light", listOf(Color(0xFFFBE8D3), Color(0xFFF6C99F), Color(0xFFE8A87C), Color(0xFFD98E73))),
    WallpaperPreset(6, "Fresh Mint", listOf(Color(0xFFEAF7F0), Color(0xFFBFE6D2), Color(0xFF8FD0B5), Color(0xFF5FB89B))),
)

const val WALLPAPER_DYNAMIC = 99

fun wallpaperById(id: Int): WallpaperPreset =
    wallpaperPresets.find { it.id == id } ?: wallpaperPresets.first()

fun dynamicColors(): List<Color> {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..8 -> listOf(Color(0xFF2B2340), Color(0xFF6A4C7C), Color(0xFFD98E73), Color(0xFFF6C99F))
        in 9..16 -> listOf(Color(0xFF0E2A3A), Color(0xFF1E6074), Color(0xFF3FA7A0), Color(0xFF9FE0D0))
        in 17..19 -> listOf(Color(0xFF241B2E), Color(0xFF5C3A54), Color(0xFFC4704F), Color(0xFFF0B27A))
        else -> listOf(Color(0xFF070A12), Color(0xFF131A2E), Color(0xFF2A3550), Color(0xFF56618A))
    }
}

@Composable
fun WallpaperLayer(blurDp: Int = 0, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val customUri = Prefs.customWallpaperUri
    val bitmap = remember(customUri) {
        if (customUri != null) {
            try {
                ctx.contentResolver.openInputStream(android.net.Uri.parse(customUri))?.use {
                    BitmapFactory.decodeStream(it)
                }?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val appliedBlur = (blurDp * Perf.blurScale).dp
    val mod = if (blurDp > 0) modifier.fillMaxSize().blur(appliedBlur) else modifier.fillMaxSize()

    if (bitmap != null) {
        Box(mod) {
            Image(
                bitmap = bitmap,
                contentDescription = "Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Color(0xFF000000).copy(alpha = if (isWoodDark()) 0.18f else 0.04f)
                )
            )
        }
    } else {
        GeneratedWallpaper(mod)
    }
}

/** Time-driven canvas wallpaper with soft drifting color blobs. */
@Composable
fun GeneratedWallpaper(modifier: Modifier = Modifier) {
    val id = Prefs.wallpaperId
    val colors = if (id == WALLPAPER_DYNAMIC) dynamicColors() else wallpaperById(id).colors
    val motion = Perf.allowWallpaperMotion
    var t by remember { mutableLongStateOf(0L) }
    if (motion) {
        LaunchedEffect(Unit) {
            while (true) {
                t = withFrameMillisCompat()
            }
        }
    }
    Canvas(modifier) {
        drawWallpaper(this, colors, if (motion) (t % 20000L) / 20000f else 0f)
    }
}

private suspend fun withFrameMillisCompat(): Long =
    androidx.compose.runtime.withFrameMillis { it }

fun drawWallpaper(scope: androidx.compose.ui.graphics.drawscope.DrawScope, colors: List<Color>, phase: Float) {
    val w = scope.size.width
    val h = scope.size.height
    if (w <= 0f || h <= 0f) return

    // Base diagonal gradient
    scope.drawRect(
        Brush.linearGradient(
            colors = listOf(colors[0], colors[1]),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )
    )

    // Drifting radial blobs
    val drift = phase * (2f * Math.PI.toFloat())
    fun blob(cx: Float, cy: Float, radius: Float, color: Color, dx: Float, dy: Float) {
        scope.drawCircle(
            Brush.radialGradient(
                listOf(color.copy(alpha = 0.85f), Color.Transparent),
                center = Offset(cx + dx, cy + dy),
                radius = radius,
            ),
            radius = radius,
            center = Offset(cx + dx, cy + dy),
        )
    }
    blob(w * 0.78f, h * 0.22f, w * 0.55f, colors[2],
        kotlin.math.sin(drift) * w * 0.04f, kotlin.math.cos(drift * 1.3f) * h * 0.04f)
    blob(w * 0.18f, h * 0.75f, w * 0.6f, colors[3],
        kotlin.math.cos(drift * 0.8f) * w * 0.05f, kotlin.math.sin(drift) * h * 0.05f)
    blob(w * 0.45f, h * 0.45f, w * 0.4f, colors[1].copy(alpha = 0.6f),
        kotlin.math.sin(drift * 1.7f) * w * 0.03f, kotlin.math.cos(drift * 0.6f) * h * 0.03f)

    // Subtle grain lines for the "wood" identity
    scope.drawStrokeLines(colors[0].copy(alpha = 0.12f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeLines(color: Color) {
    val w = size.width
    val h = size.height
    val step = h / 9f
    var y = step
    var i = 0
    while (y < h) {
        val wave = kotlin.math.sin(i * 1.7f) * w * 0.02f
        drawLine(
            color = color,
            start = Offset(0f, y + wave),
            end = Offset(w, y - wave),
            strokeWidth = 1.4f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        y += step
        i++
    }
}

/** Small preview thumbnail used by the wallpaper picker. */
@Composable
fun WallpaperThumb(id: Int, modifier: Modifier = Modifier, dynamic: Boolean = false) {
    val colors = if (dynamic) dynamicColors() else wallpaperById(id).colors
    Canvas(modifier) {
        drawRect(Brush.linearGradient(colors))
    }
}

private fun Stroke(cap: androidx.compose.ui.graphics.StrokeCap) = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f, cap = cap)
