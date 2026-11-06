package dev.woodos.launcher.root

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.WoodMode
import dev.woodos.launcher.ui.WallpaperLayer
import dev.woodos.launcher.ui.WoodTheme
import dev.woodos.launcher.ui.isWoodDark
import dev.woodos.launcher.woodos.HomeScreen
import dev.woodos.launcher.woodos.OverlayHost

/**
 * Root of the whole WoodOS experience: wallpaper, mode switching
 * (WoodOS tablet UI <-> WoodDesk desktop) and global overlays.
 */
@Composable
fun WoodRoot() {
    WoodTheme {
        val overlayOpen = UiState.overlay.value != UiState.Overlay.NONE
        val blur by animateFloatAsState(
            (if (overlayOpen) 26 else 0).toFloat(),
            animationSpec = tween(durationMillis = 350),
            label = "wallBlur",
        )
        val scale by animateFloatAsState(
            if (overlayOpen) 0.97f else 1f,
            tween(350), label = "modeScale"
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(if (isWoodDark()) Color(0xFF0B0E14) else Color(0xFFF2EDE4))
        ) {
            WallpaperLayer(blurDp = blur.toInt())

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                when (Prefs.mode) {
                    WoodMode.WOODOS -> HomeScreen()
                    WoodMode.WOODDESK -> dev.woodos.launcher.wooddesk.DeskEnvironment()
                }
            }

            OverlayHost()
        }
    }
}
