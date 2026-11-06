package dev.woodos.launcher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Detects right-clicks from a Bluetooth/USB mouse (secondary button press).
 * Used by WoodDesk context menus.
 */
fun Modifier.rightClickable(onRightClick: (Offset) -> Unit): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (currentEvent.buttons.isSecondaryPressed) {
            waitForUpOrCancellation()
            onRightClick(down.position)
        }
    }
}
