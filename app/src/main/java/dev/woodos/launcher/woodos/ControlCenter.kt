package dev.woodos.launcher.woodos

import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.ThemeMode
import dev.woodos.launcher.core.WoodMode
import dev.woodos.launcher.services.ScreenRecordService
import dev.woodos.launcher.ui.GlassPanel
import dev.woodos.launcher.ui.isWoodDark

/**
 * WoodOS Control Center (iOS/One UI inspired).
 *
 * Android restrictions that apply to any third-party launcher are clearly
 * marked: Wi-Fi/Airplane toggles fall back to the system connectivity panel
 * on Android 13+, and brightness requires the WRITE_SETTINGS special grant.
 */
@Composable
fun ControlCenterOverlay() {
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = false) { }
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            GlassPanel(corner = 32.dp) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    ControlCenterHeader()
                    Spacer(Modifier.height(14.dp))
                    QuickTiles()
                    Spacer(Modifier.height(16.dp))
                    BrightnessSection()
                    Spacer(Modifier.height(8.dp))
                    VolumeSection()
                    Spacer(Modifier.height(8.dp))
                    BatterySection()
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { BigTile(Icons.Rounded.DesktopWindows, "WoodDesk") { Prefs.mode = WoodMode.WOODDESK } }
                        Box(Modifier.weight(1f)) { BigTile(Icons.Rounded.Settings, "Settings") { UiState.open(UiState.Overlay.SETTINGS) } }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Wi-Fi, Bluetooth and Airplane shortcuts open the system panel when Android " +
                                "does not allow direct control (Android 13+).",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlCenterHeader() {
    val context = LocalContext.current
    val now = rememberNow()
    val timeFmt = remember { java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()) }
    val dateFmt = remember { java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault()) }
    val battery = rememberBatteryPercent(context)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(timeFmt.format(now), fontSize = 26.sp, fontWeight = FontWeight.Light)
            Text(dateFmt.format(now), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.BatteryFull, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(start = 4.dp))
        Text("$battery%", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Rounded.Close, contentDescription = "Close",
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(5.dp)
                .clickable { UiState.close() },
        )
    }
}

@Composable
private fun QuickTiles() {
    val context = LocalContext.current
    val wifi = remember { context.getSystemService(Context.WIFI_SERVICE) as? WifiManager }
    val btManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    var wifiOn by remember { mutableStateOf(wifi?.isWifiEnabled == true) }
    var btOn by remember { mutableStateOf(btManager?.adapter?.isEnabled == true) }
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        btOn = btManager?.adapter?.isEnabled == true
    }

    fun refreshStates() {
        wifiOn = wifi?.isWifiEnabled == true
        btOn = btManager?.adapter?.isEnabled == true
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) {
            Tile(Icons.Rounded.Wifi, "Wi-Fi", wifiOn) {
                val changed = if (Build.VERSION.SDK_INT < 33) {
                    try { wifi?.setWifiEnabled(!wifiOn) == true } catch (_: Exception) { false }
                } else false
                if (!changed) {
                    // Android 13+: open the system connectivity panel
                    try {
                        context.startActivity(
                            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                refreshStates()
            }
        }
        Box(Modifier.weight(1f)) {
            Tile(Icons.Rounded.Bluetooth, "Bluetooth", btOn) {
                val adapter = btManager?.adapter
                if (adapter != null) {
                    val hasPerm = Build.VERSION.SDK_INT < 31 ||
                            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        perms.launch(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT))
                    } else {
                        try {
                            if (adapter.isEnabled) adapter.disable() else adapter.enable()
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
                refreshStates()
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) {
            Tile(Icons.Rounded.ScreenRotation, "Rotation", Prefs.rotationLocked) {
                Prefs.rotationLocked = !Prefs.rotationLocked
            }
        }
        Box(Modifier.weight(1f)) {
            val dark = isWoodDark()
            Tile(Icons.Rounded.DarkMode, "Dark mode", dark) {
                Prefs.themeMode = if (dark) ThemeMode.LIGHT else ThemeMode.DARK
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) {
            Tile(Icons.Rounded.Videocam, "Screen rec", ScreenRecordService.isRecording) {
                if (ScreenRecordService.isRecording) {
                    ScreenRecordService.stop(context)
                } else {
                    requestScreenRecord(context)
                }
            }
        }
        Box(Modifier.weight(1f)) {
            Tile(Icons.Rounded.Close, "Airplane", false) {
                try {
                    context.startActivity(
                        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                    context.startActivity(
                        Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
    }
}

private fun requestScreenRecord(context: Context) {
    val activity = context as? dev.woodos.launcher.MainActivity
    if (activity != null) {
        activity.launchRecordRequest()
        return
    }
    try {
        context.startActivity(
            Intent(context, dev.woodos.launcher.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("request_record", true)
        )
    } catch (_: Exception) {
    }
}

@Composable
private fun BrightnessSection() {
    val context = LocalContext.current
    var value by remember { mutableFloatStateOf(getBrightness(context)) }
    val canWrite = Settings.System.canWrite(context)
    val writePerm = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.System.canWrite(context)) {
            value = getBrightness(context)
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BrightnessMedium, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = 8.dp))
            Text("Brightness", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            if (!canWrite) {
                Text(
                    "Grant access",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        writePerm.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                    },
                )
            }
        }
        Slider(
            value = value,
            onValueChange = {
                value = it
                if (Settings.System.canWrite(context)) {
                    setBrightness(context, it)
                }
            },
            valueRange = 0.05f..1f,
            enabled = canWrite,
        )
        if (!canWrite) {
            Text(
                "Android requires a one-time permission for apps to change brightness. " +
                        "Tap \"Grant access\" and allow WoodOS to modify system settings.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun getBrightness(context: Context): Float = try {
    val v = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    (v / 255f).coerceIn(0.05f, 1f)
} catch (_: Exception) {
    0.5f
}

private fun setBrightness(context: Context, f: Float) {
    try {
        Settings.System.putInt(
            context.contentResolver, Settings.System.SCREEN_BRIGHTNESS,
            (f * 255f).toInt().coerceIn(1, 255),
        )
    } catch (_: Exception) {
    }
}

@Composable
private fun VolumeSection() {
    val context = LocalContext.current
    val am = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var value by remember { mutableFloatStateOf(0.5f) }
    LaunchedEffect(Unit) {
        val max = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val cur = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        value = if (max > 0) cur.toFloat() / max else 0.5f
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = 8.dp))
            Text("Volume", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = { v ->
                value = v
                val max = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                am?.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).toInt().coerceIn(0, max), 0)
            },
        )
    }
}

@Composable
private fun BatterySection() {
    val context = LocalContext.current
    val level = rememberBatteryPercent(context)
    val charging = remember {
        try {
            val i = context.registerReceiver(null, IntentFilterCompat.batteryChanged())
            i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        } catch (_: Exception) {
            false
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.BatteryFull, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.padding(start = 10.dp))
        Column {
            Text(
                "$level% ${if (charging) "charging" else "on battery"}",
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
            Text(
                rememberDeviceString(context),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun rememberDeviceString(context: Context): String {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val mem = am?.let {
        val mi = ActivityManager.MemoryInfo()
        it.getMemoryInfo(mi)
        mi.availMem / (1024 * 1024)
    } ?: 0
    return "${android.os.Build.MODEL} • ${mem} MB free"
}

private fun rememberBatteryPercent(context: Context): Int = try {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
} catch (_: Exception) {
    -1
}.let { if (it > 0) it else 100 }

private object IntentFilterCompat {
    fun batteryChanged() = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
}

@Composable
private fun Tile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BigTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.padding(start = 10.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (label == "Screen rec" && ScreenRecordService.isRecording) {
            Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
        }
    }
}
