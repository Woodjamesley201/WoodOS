package dev.woodos.launcher.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Adaptive performance for the Galaxy Tab A9 (SM-X110: Helio G99, 4/8 GB RAM,
 * 60 Hz 1340x800 TFT). Detects memory pressure and scales expensive effects
 * (blur radius, animation complexity, wallpaper drift) so the UI stays smooth.
 */
object Perf {
    var isLowRamDevice = false
        private set
    var memoryClassMb = 128
        private set
    var totalRamMb = 0L
        private set

    fun init(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        isLowRamDevice = am.isLowRamDevice
        memoryClassMb = am.memoryClass
        totalRamMb = try {
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            mi.totalMem / (1024 * 1024)
        } catch (_: Exception) { 0L }
    }

    /** Scale factor for blur radii (0.4 on constrained devices, 1.0 otherwise). */
    val blurScale: Float get() = if (isLowRamDevice || memoryClassMb <= 128) 0.4f else 1f

    /** Whether animated wallpaper drift is allowed. */
    val allowWallpaperMotion: Boolean get() = !isLowRamDevice

    /** Scaled animation duration in ms. */
    fun anim(ms: Long): Long = (ms * (0.4f + 0.6f * Prefs.animationScale)).toLong().coerceIn(60, 1200)

    fun deviceSummary(): String =
        "${Build.MODEL ?: "Android device"} • Android ${Build.VERSION.RELEASE} • ${if (totalRamMb > 0) "$totalRamMb MB RAM" else "$memoryClassMb MB memclass"}"
}
