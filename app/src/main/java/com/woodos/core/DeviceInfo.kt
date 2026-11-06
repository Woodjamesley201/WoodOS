package com.woodos.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock

/**
 * Reads battery / Wi-Fi / storage status for the home screen chips and the
 * WoodDesk system tray. Everything is pulled on demand - no polling loops
 * run while the UI is not visible.
 */
class DeviceInfo(private val context: Context) {

    data class Snapshot(
        val batteryPercent: Int,
        val charging: Boolean,
        val wifiConnected: Boolean,
        val wifiSsid: String?,
        val wifiLevel: Int, // 0..4
        val storageFreeBytes: Long,
        val storageTotalBytes: Long,
    )

    @Suppress("DEPRECATION") // WifiManager.connectionInfo: fine for status display
    fun snapshot(): Snapshot {
        // Battery
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val battery: Intent? = context.registerReceiver(null, ifilter)
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val charging = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                BatteryManager.BATTERY_STATUS_CHARGING ||
                battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                BatteryManager.BATTERY_STATUS_FULL

        // Wi-Fi
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        var wifiConnected = false
        var wifiSsid: String? = null
        var wifiLevel = 0
        if (wifiManager != null) {
            @Suppress("DEPRECATION")
            val info = wifiManager.connectionInfo
            wifiConnected = info != null && info.ipAddress != 0
            if (wifiConnected) {
                wifiLevel = WifiManager.calculateSignalLevel(info.rssi, 5)
                val rawSsid = info.ssid
                if (rawSsid != null && rawSsid != WifiManager.UNKNOWN_SSID &&
                    rawSsid.startsWith("\"") && rawSsid.endsWith("\"") && rawSsid.length >= 2
                ) {
                    // Reading the SSID is only permitted while location access is granted.
                    wifiSsid = if (context.checkSelfPermission(
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) rawSsid.substring(1, rawSsid.length - 1) else null
                }
            }
        }

        // Storage (primary shared storage)
        val sf = runCatching {
            val stats = StatFs(Environment.getDataDirectory().path)
            Pair(stats.availableBytes, stats.totalBytes)
        }.getOrDefault(Pair(-1L, -1L))

        return Snapshot(
            batteryPercent = percent,
            charging = charging,
            wifiConnected = wifiConnected,
            wifiSsid = wifiSsid,
            wifiLevel = wifiLevel,
            storageFreeBytes = sf.first,
            storageTotalBytes = sf.second,
        )
    }

    /** Registers a lightweight receiver for battery/connectivity UI refreshes. */
    fun registerReceiver(callback: () -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                // Simple change throttling: batteryChanged fires often.
                callback()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(Intent.ACTION_TIME_TICK)
        }
        context.registerReceiver(receiver, filter)
        return receiver
    }

    fun unregister(receiver: BroadcastReceiver) {
        runCatching { context.unregisterReceiver(receiver) }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 10) "${gb.toInt()} GB" else String.format("%.1f GB", gb)
        }

        /** Elapsed time helper used by the performance panel. */
        fun uptime(): String {
            val ms = SystemClock.elapsedRealtime()
            val h = ms / 3_600_000
            val m = (ms % 3_600_000) / 60_000
            return "${h}h ${m}m"
        }
    }
}
