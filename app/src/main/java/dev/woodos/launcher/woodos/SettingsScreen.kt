package dev.woodos.launcher.woodos

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import dev.woodos.launcher.core.AccentColor
import dev.woodos.launcher.core.ClockStyle
import dev.woodos.launcher.core.Perf
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.ThemeMode
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.Weather
import dev.woodos.launcher.core.WoodMode
import dev.woodos.launcher.notifications.WoodNotificationListener
import dev.woodos.launcher.notifications.WoodNotificationListener.Companion as NListener
import dev.woodos.launcher.ui.GlassPanel
import dev.woodos.launcher.ui.SliderRow
import dev.woodos.launcher.ui.ToggleRow
import dev.woodos.launcher.ui.WallpaperThumb
import dev.woodos.launcher.ui.wallpaperPresets
import dev.woodos.launcher.ui.WALLPAPER_DYNAMIC

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

/**
 * WoodOS settings — shared by the Control Center shortcut and the WoodDesk
 * settings window. Extensive customisation of appearance, home, WoodDesk,
 * weather and notifications.
 */
@Composable
fun SettingsScreen(onClose: () -> Unit = { UiState.close() }) {
    val context = LocalContext.current
    var weatherQuery by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = false) { }
    ) {
        GlassPanel(corner = 34.dp) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("WoodOS Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.Close, contentDescription = "Close",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(5.dp)
                            .clickable { onClose() },
                    )
                }

                Spacer(Modifier.height(18.dp))

                // ---------------- Appearance ----------------
                SectionHeader("Appearance")
                Spacer(Modifier.height(8.dp))

                Text("Theme", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "System",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark",
                    ).forEach { (mode, label) ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(
                                    if (Prefs.themeMode == mode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { Prefs.themeMode = mode }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (Prefs.themeMode == mode) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Accent color", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentColor.entries.forEach { accent ->
                        val color = when (accent) {
                            AccentColor.WOOD -> Color(0xFFB5834A)
                            AccentColor.OCEAN -> Color(0xFF1E88E5)
                            AccentColor.AURORA -> Color(0xFF00897B)
                            AccentColor.EMBER -> Color(0xFFF4511E)
                            AccentColor.VIOLET -> Color(0xFF673AB7)
                            AccentColor.TEAL -> Color(0xFF00897B)
                            AccentColor.ROSE -> Color(0xFFD81B60)
                        }
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { Prefs.accent = accent },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (Prefs.accent == accent) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Wallpaper", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                        }
                        Prefs.customWallpaperUri = uri.toString()
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    wallpaperPresets.forEach { preset ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    Prefs.customWallpaperUri = null
                                    Prefs.wallpaperId = preset.id
                                },
                        ) {
                            WallpaperThumb(preset.id, Modifier.fillMaxSize())
                            if (Prefs.wallpaperId == preset.id && Prefs.customWallpaperUri == null) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                Prefs.customWallpaperUri = null
                                Prefs.wallpaperId = WALLPAPER_DYNAMIC
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Dynamic (time of day)", fontSize = 11.sp)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePicker.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row {
                            Icon(Icons.Rounded.Image, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.padding(start = 6.dp))
                            Text("Custom image", fontSize = 11.sp)
                        }
                    }
                }
                if (Prefs.customWallpaperUri != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Using custom image",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(14.dp))
                SliderRow(
                    label = "Glass transparency",
                    value = Prefs.transparency,
                    range = 0.2f..0.95f,
                    onChange = { Prefs.transparency = it },
                    format = { "${(it * 100).toInt()}%" },
                )
                SliderRow(
                    label = "Blur intensity",
                    value = Prefs.blurIntensity,
                    range = 0.3f..1.5f,
                    onChange = { Prefs.blurIntensity = it },
                    format = { "${(it * 100).toInt()}%" },
                )
                SliderRow(
                    label = "Animation speed",
                    value = Prefs.animationScale,
                    range = 0.5f..1.5f,
                    onChange = { Prefs.animationScale = it },
                    format = { "${(it * 100).toInt()}%" },
                )

                Spacer(Modifier.height(20.dp))

                // ---------------- Home screen ----------------
                SectionHeader("Home screen")
                Spacer(Modifier.height(8.dp))
                Text("Clock style", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ClockStyle.MODERN to "Modern",
                        ClockStyle.CLASSIC to "Classic",
                        ClockStyle.MINIMAL to "Minimal",
                    ).forEach { (style, label) ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(
                                    if (Prefs.clockStyle == style) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { Prefs.clockStyle = style }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (Prefs.clockStyle == style) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SliderRow(
                    label = "Home grid columns",
                    value = Prefs.homeColumns.toFloat(),
                    range = 4f..6f,
                    onChange = { Prefs.homeColumns = it.toInt() },
                    format = { it.toInt().toString() },
                )
                ToggleRow("Weather widget", Prefs.showWeatherWidget) { Prefs.showWeatherWidget = it }
                ToggleRow("Search bar", Prefs.showSearchBar) { Prefs.showSearchBar = it }

                Spacer(Modifier.height(20.dp))

                // ---------------- WoodDesk ----------------
                SectionHeader("WoodDesk")
                Spacer(Modifier.height(8.dp))
                ToggleRow("Show desktop icons", Prefs.deskShowIcons) { Prefs.deskShowIcons = it }
                ToggleRow("Rotation lock", Prefs.rotationLocked) { Prefs.rotationLocked = it }
                ToggleRow(
                    "Try freeform app windows",
                    Prefs.tryFreeform,
                    subtitle = "Attempts to open external apps in desktop windows. The Galaxy Tab A9 " +
                            "does not support freeform windowing, so apps will usually open full screen.",
                ) { Prefs.tryFreeform = it }
                Text("Taskbar size", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        dev.woodos.launcher.core.TaskbarSize.COMPACT to "Compact",
                        dev.woodos.launcher.core.TaskbarSize.NORMAL to "Normal",
                        dev.woodos.launcher.core.TaskbarSize.LARGE to "Large",
                    ).forEach { (size, label) ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(
                                    if (Prefs.taskbarSize == size) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { Prefs.taskbarSize = size }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (Prefs.taskbarSize == size) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---------------- Weather ----------------
                SectionHeader("Weather")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Location: ${Prefs.weatherCityName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weatherQuery,
                    onValueChange = { weatherQuery = it },
                    singleLine = true,
                    placeholder = { Text("Search a city") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                var results by remember { mutableStateOf<List<dev.woodos.launcher.core.GeoResult>>(emptyList()) }
                LaunchedEffect(weatherQuery) {
                    if (weatherQuery.length >= 3) {
                        results = Weather.geocode(weatherQuery)
                    } else {
                        results = emptyList()
                    }
                }
                results.forEach { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                Prefs.weatherCityName = "${r.name}, ${r.admin.ifBlank { r.country }}"
                                Prefs.weatherLat = r.lat.toFloat()
                                Prefs.weatherLon = r.lon.toFloat()
                                results = emptyList()
                                weatherQuery = ""
                            }
                            .padding(8.dp),
                    ) {
                        Text("${r.name} — ${r.admin}, ${r.country}", fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---------------- Notifications ----------------
                SectionHeader("Notifications")
                Spacer(Modifier.height(8.dp))
                if (NListener.connected.value) {
                    Text("Notification access: granted", fontSize = 13.sp)
                } else {
                    OutlinedButton(onClick = { NListener.openAccessSettings(context) }) {
                        Text("Grant notification access")
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---------------- About ----------------
                SectionHeader("About")
                Spacer(Modifier.height(8.dp))
                Text(
                    "WoodOS 1.0 \"Timber\"\n${Perf.deviceSummary()}\n" +
                            "Optimized for Samsung Galaxy Tab A9 (SM-X110)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = { Prefs.mode = WoodMode.WOODDESK }) {
                    Text("Switch to WoodDesk")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    Prefs.resetDefaults()
                    onClose()
                }) {
                    Text("Reset all settings", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
