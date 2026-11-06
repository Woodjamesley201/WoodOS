package dev.woodos.launcher

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.root.WoodRoot
import dev.woodos.launcher.services.ScreenRecordService
import dev.woodos.launcher.wooddesk.DeskWM

class MainActivity : ComponentActivity() {

    private val recordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            ScreenRecordService.start(this, result.resultCode, result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Follow the rotation-lock preference
            LaunchedEffect(Prefs.rotationLocked) {
                requestedOrientation =
                    if (Prefs.rotationLocked) ActivityInfo.SCREEN_ORIENTATION_LOCKED
                    else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            WoodRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Coming back to the launcher: close any open overlay
        UiState.close()
        if (intent?.getBooleanExtra("request_record", false) == true) {
            launchRecordRequest()
        }
    }

    fun launchRecordRequest() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE)
                as android.media.projection.MediaProjectionManager
        recordLauncher.launch(mpm.createScreenCaptureIntent())
    }

    /**
     * Hardware/Bluetooth/USB keyboard shortcuts (WoodDesk-optimised).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val alt = event.isAltPressed
            val ctrl = event.isCtrlPressed
            val meta = event.isMetaPressed
            when (event.keyCode) {
                KeyEvent.KEYCODE_TAB -> if (alt) {
                    DeskWM.cycleWindows(); return true
                }
                KeyEvent.KEYCODE_SPACE -> if (ctrl) {
                    UiState.toggle(UiState.Overlay.SEARCH); return true
                }
                KeyEvent.KEYCODE_F4 -> if (alt) {
                    DeskWM.closeFocused(); return true
                }
                KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT ->
                    if (!alt && !ctrl) {
                        if (Prefs.mode == dev.woodos.launcher.core.WoodMode.WOODDESK) {
                            UiState.toggle(UiState.Overlay.START_MENU)
                        }
                        return true
                    }
                KeyEvent.KEYCODE_ESCAPE -> {
                    if (UiState.dismissTop()) return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!UiState.dismissTop()) {
            super.onBackPressed()
        }
    }
}
