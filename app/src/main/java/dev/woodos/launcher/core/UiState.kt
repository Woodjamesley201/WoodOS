package dev.woodos.launcher.core

import androidx.compose.runtime.mutableStateOf

/** Global overlay/UI state shared between the activity, keyboard handling and Compose. */
object UiState {
    enum class Overlay { NONE, CONTROL_CENTER, SHADE, SEARCH, START_MENU, CALENDAR, POWER, APP_PICKER, WEATHER, DRAWER, SETTINGS }

    val overlay = mutableStateOf(Overlay.NONE)

    /** Which app-picker flow is active (used by "add to dock/folder" flows). */
    enum class PickerPurpose { DOCK, HOME, FAVORITES, START_PIN, DESKTOP, FOLDER_HOME, FOLDER_DESK }
    var pickerPurpose: PickerPurpose = PickerPurpose.DOCK
    var pickerFolderId: String? = null

    fun open(overlay: Overlay) {
        if (this.overlay.value != overlay) this.overlay.value = overlay
    }

    fun close() {
        overlay.value = Overlay.NONE
    }

    fun toggle(overlay: Overlay) {
        this.overlay.value = if (this.overlay.value == overlay) Overlay.NONE else overlay
    }

    /** Escape / back handling: dismiss the topmost overlay. */
    fun dismissTop(): Boolean =
        if (overlay.value != Overlay.NONE) {
            overlay.value = Overlay.NONE
            true
        } else false
}
