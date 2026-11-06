package com.woodos.perf

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process

/**
 * Lightweight, on-demand memory measurements for the Performance panel.
 * Nothing here runs in the background; values are computed only when
 * the panel is visible.
 */
object PerformanceMonitor {

    data class MemoryReport(
        val javaHeapUsedMb: Double,
        val javaHeapMaxMb: Double,
        val nativeHeapMb: Double,
        val deviceTotalRamMb: Double,
        val deviceAvailableRamMb: Double,
    )

    fun report(context: Context): MemoryReport {
        val rt = Runtime.getRuntime()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        return MemoryReport(
            javaHeapUsedMb = (rt.totalMemory() - rt.freeMemory()) / MB,
            javaHeapMaxMb = rt.maxMemory() / MB,
            nativeHeapMb = Debug.getNativeHeapAllocatedSize() / MB,
            deviceTotalRamMb = (memInfo?.totalMem ?: 0L) / MB,
            deviceAvailableRamMb = (memInfo?.availMem ?: 0L) / MB,
        )
    }

    /** Rough PSS estimate for this process, in MB (best effort). */
    fun approximatePssMb(context: Context): Double {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return runCatching {
            val pids = intArrayOf(Process.myPid())
            val infos = am?.getProcessMemoryInfo(pids)
            val pss = infos?.firstOrNull()?.totalPss ?: 0
            pss / 1024.0
        }.getOrDefault(0.0)
    }

    private const val MB = 1024.0 * 1024.0
}
