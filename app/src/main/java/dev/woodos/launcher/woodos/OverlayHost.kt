package dev.woodos.launcher.woodos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.woodos.launcher.core.Perf
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.woodos.parts.AppDrawerOverlay
import dev.woodos.launcher.woodos.parts.AppPickerOverlay

/**
 * Hosts the global WoodOS overlays (Control Center, Notification Center,
 * search, drawer, app picker, weather details, settings) with smooth
 * slide/fade transitions. Duration is scaled by the animation preference
 * and the adaptive performance layer.
 */
@Composable
fun OverlayHost() {
    val overlay = UiState.overlay.value
    val dur = (Perf.anim(320)).toInt()

    // scrim that closes on tap
    AnimatedVisibility(
        visible = overlay != UiState.Overlay.NONE,
        enter = fadeIn(tween(dur / 2)),
        exit = fadeOut(tween(dur / 2)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable { UiState.close() }
        )
    }

    AnimatedVisibility(
        visible = overlay == UiState.Overlay.CONTROL_CENTER,
        enter = slideInHorizontally(tween(dur)) { it / 2 } + fadeIn(tween(dur)),
        exit = slideOutHorizontally(tween(dur)) { it / 2 } + fadeOut(tween(dur)),
    ) { ControlCenterOverlay() }

    AnimatedVisibility(
        visible = overlay == UiState.Overlay.SHADE,
        enter = slideInVertically(tween(dur)) { -it / 3 } + fadeIn(tween(dur)),
        exit = slideOutVertically(tween(dur)) { -it / 3 } + fadeOut(tween(dur)),
    ) { NotificationShadeOverlay() }

    AnimatedVisibility(
        visible = overlay == UiState.Overlay.SEARCH,
        enter = scaleIn(tween(dur), initialScale = 0.96f) + fadeIn(tween(dur)),
        exit = scaleOut(tween(dur), targetScale = 0.96f) + fadeOut(tween(dur)),
    ) { SearchOverlay() }

    AnimatedVisibility(
        visible = overlay == UiState.Overlay.DRAWER,
        enter = slideInVertically(tween(dur)) { it / 3 } + fadeIn(tween(dur)),
        exit = slideOutVertically(tween(dur)) { it / 3 } + fadeOut(tween(dur)),
    ) { AppDrawerOverlay() }

    AnimatedVisibility(
        visible = overlay == UiState.Overlay.APP_PICKER,
        enter = scaleIn(tween(dur), initialScale = 0.94f) + fadeIn(tween(dur)),
        exit = scaleOut(tween(dur), targetScale = 0.94f) + fadeOut(tween(dur)),
    ) { AppPickerOverlay() }

    AnimatedVisibility(
        visible = overlay == UiState.Overlay.WEATHER,
        enter = scaleIn(tween(dur), initialScale = 0.94f) + fadeIn(tween(dur)),
        exit = scaleOut(tween(dur), targetScale = 0.94f) + fadeOut(tween(dur)),
    ) { WeatherOverlay() }

    AnimatedVisibility(
        visible = overlay == UiState.Overlay.SETTINGS,
        enter = fadeIn(tween(dur)) + scaleIn(tween(dur), initialScale = 0.97f),
        exit = fadeOut(tween(dur)) + scaleOut(tween(dur), targetScale = 0.97f),
    ) { SettingsScreen() }
}

@Composable
private fun WeatherOverlay() {
    Box(Modifier.fillMaxSize().clickable(enabled = false) { }) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            WeatherDetailsPanel()
        }
    }
}
