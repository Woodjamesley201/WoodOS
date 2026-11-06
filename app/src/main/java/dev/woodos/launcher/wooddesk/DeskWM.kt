package dev.woodos.launcher.wooddesk

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import dev.woodos.launcher.core.AppRef

/**
 * WoodDesk window manager: real in-app windows with focus/z-order, drag,
 * resize, minimize, maximize, snapping and Alt+Tab cycling.
 * (External Android apps cannot be hosted inside these windows — that is an
 * OS-level restriction; WoodDesk launches them full screen instead.)
 */
object DeskWM {

    data class Win(
        val id: Int,
        val appId: String,
        val title: String,
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val minimized: Boolean = false,
        val maximized: Boolean = false,
    )

    val windows: SnapshotStateList<Win> = mutableStateListOf()
    val focusedId = mutableStateOf<Int?>(null)
    val snapHint = mutableStateOf<SnapHint>(SnapHint.NONE)

    enum class SnapHint { NONE, LEFT, RIGHT, MAX }

    // screen metrics in px (configured by DeskEnvironment)
    var screenW = 1080f
    var screenH = 1920f
    var taskbarH = 132f
    var statusBarH = 60f

    private var nextId = 1

    fun configure(w: Float, h: Float, taskbarPx: Float, statusPx: Float) {
        screenW = w
        screenH = h
        taskbarH = taskbarPx
        statusBarH = statusPx
    }

    fun openApp(appId: String): Int {
        val existing = windows.firstOrNull { it.appId == appId }
        if (existing != null) {
            focus(existing.id)
            if (existing.minimized) windows[windows.indexOf(existing)] = existing.copy(minimized = false)
            return existing.id
        }
        val app = DeskApps.byId(appId) ?: return -1
        val w = app.defaultW.value * densityFactor
        val h = app.defaultH.value * densityFactor
        val count = windows.size
        val x = ((screenW - w) / 2f + count * 34f).coerceIn(0f, (screenW - w).coerceAtLeast(0f))
        val y = ((screenH - taskbarH - h) / 2.4f + count * 26f).coerceIn(statusBarH, (screenH - taskbarH - h).coerceAtLeast(statusBarH))
        val id = nextId++
        windows.add(Win(id, appId, app.title, x, y, w, h))
        focusedId.value = id
        return id
    }

    fun close(id: Int) {
        windows.removeAll { it.id == id }
        if (focusedId.value == id) focusedId.value = windows.lastOrNull()?.id
    }

    fun closeFocused() {
        focusedId.value?.let { close(it) }
    }

    fun focus(id: Int) {
        val idx = windows.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val w = windows.removeAt(idx)
            windows.add(w)
        }
        focusedId.value = id
    }

    fun minimize(id: Int) {
        val idx = windows.indexOfFirst { it.id == id }
        if (idx >= 0) windows[idx] = windows[idx].copy(minimized = true)
        if (focusedId.value == id) focusedId.value = windows.lastOrNull { !it.minimized }?.id
    }

    fun toggleMaximize(id: Int) {
        val idx = windows.indexOfFirst { it.id == id }
        if (idx >= 0) windows[idx] = windows[idx].copy(maximized = !windows[idx].maximized)
        focus(id)
    }

    fun update(id: Int, transform: (Win) -> Win) {
        val idx = windows.indexOfFirst { it.id == id }
        if (idx >= 0) windows[idx] = transform(windows[idx])
    }

    fun drag(id: Int, dx: Float, dy: Float) {
        update(id) {
            it.copy(
                x = (it.x + dx).coerceIn(-it.w * 0.6f, screenW - it.w * 0.4f),
                y = (it.y + dy).coerceIn(statusBarH, screenH - taskbarH - 30f),
            )
        }
        val win = windows.firstOrNull { it.id == id } ?: return
        snapHint.value = when {
            win.x < 70f -> SnapHint.LEFT
            win.x + win.w > screenW - 70f -> SnapHint.RIGHT
            win.y < statusBarH + 40f -> SnapHint.MAX
            else -> SnapHint.NONE
        }
    }

    fun endDrag(id: Int) {
        when (snapHint.value) {
            SnapHint.LEFT -> snapLeft(id)
            SnapHint.RIGHT -> snapRight(id)
            SnapHint.MAX -> update(id) { it.copy(maximized = true) }
            SnapHint.NONE -> Unit
        }
        snapHint.value = SnapHint.NONE
    }

    fun snapLeft(id: Int) = update(id) {
        it.copy(x = 0f, y = statusBarH, w = screenW / 2f, h = screenH - taskbarH - statusBarH, maximized = false)
    }

    fun snapRight(id: Int) = update(id) {
        it.copy(x = screenW / 2f, y = statusBarH, w = screenW / 2f, h = screenH - taskbarH - statusBarH, maximized = false)
    }

    fun resize(id: Int, dw: Float, dh: Float) {
        update(id) {
            it.copy(
                w = (it.w + dw).coerceIn(320f * densityFactor, screenW),
                h = (it.h + dh).coerceIn(220f * densityFactor, screenH - taskbarH - statusBarH),
            )
        }
    }

    fun cycleWindows() {
        val visible = windows.filter { !it.minimized }
        if (visible.isEmpty()) return
        val currentIdx = visible.indexOfFirst { it.id == focusedId.value }
        val next = visible[(currentIdx + 1) % visible.size]
        focus(next.id)
    }

    fun closeAll() {
        windows.clear()
        focusedId.value = null
    }

    /** Open an external Android app. Tries freeform if enabled, else full screen. */
    fun openExternal(context: android.content.Context, ref: AppRef, label: String, entry: dev.woodos.launcher.core.AppEntry?) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            .setComponent(android.content.ComponentName(ref.pkg, ref.cls))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        // Optional freeform-window attempt: setLaunchWindowingMode is not part of the
        // public SDK, so we try it through reflection and silently fall back to a
        // normal full-screen launch on devices where the system does not allow it.
        if (dev.woodos.launcher.core.Prefs.tryFreeform) {
            try {
                val opts = android.app.ActivityOptions.makeBasic()
                val m = android.app.ActivityOptions::class.java.methods.firstOrNull {
                    it.name == "setLaunchWindowingMode" && it.parameterCount == 1
                }
                m?.invoke(opts, 5) // WINDOWING_MODE_FREEFORM
                context.startActivity(intent, opts.toBundle())
                return
            } catch (_: Exception) {
                // fall through to a normal launch
            }
        }
        context.startActivity(intent)
        entry?.let { dev.woodos.launcher.core.RecentTracker.onLaunched(ref) }
    }

    fun setDensityFactor(f: Float) { densityFactor = f }
    private var densityFactor: Float = 2.6f
}
