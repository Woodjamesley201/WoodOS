@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.woodos.launcher.wooddesk

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.TaskbarSize
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.notifications.WoodNotificationListener.Companion as NListener
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.MicaPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WoodDesk taskbar: centered Start + pinned + running apps, with a Windows-11
 * style system tray (Wi-Fi/BT/battery, clock, notifications). Right-click
 * running-app icons for quick actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Taskbar() {
    val context = LocalContext.current
    val itemSize = when (Prefs.taskbarSize) {
        TaskbarSize.COMPACT -> 40.dp
        TaskbarSize.NORMAL -> 46.dp
        TaskbarSize.LARGE -> 54.dp
    }
    var now by remember { mutableStateOf(Date()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30000)
            now = Date()
        }
    }

    fun batteryPercent(): Int = try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
    } catch (_: Exception) {
        100
    }

    MicaPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        corner = 999.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // left spacer keeps the group centered
            Spacer(Modifier.weight(1f))

            // centered group
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Start
                Box(
                    Modifier
                        .size(itemSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .combinedClickable(
                            onClick = { UiState.toggle(UiState.Overlay.START_MENU) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.DesktopWindows, contentDescription = "Start",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                // Search
                Box(
                    Modifier
                        .size(itemSize)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(onClick = { UiState.open(UiState.Overlay.SEARCH) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }

                // Pinned apps
                Prefs.startPins.forEach { ref ->
                    val entry = remember(ref.key) { Apps.find(ref) }
                    Box {
                        AppIcon(
                            ref, itemSize - 8.dp,
                            Modifier
                                .combinedClickable(
                                    onClick = { entry?.let { DeskWM.openExternal(context, ref, it.label, entry) } },
                                    onLongClick = {
                                        Prefs.startPins = Prefs.startPins.filter { it.key != ref.key }
                                    },
                                )
                        )
                    }
                }

                // Running WoodDesk windows
                DeskWM.windows.filter { !it.minimized }.forEach { win ->
                    val app = DeskApps.byId(win.appId) ?: return@forEach
                    Box(
                        Modifier
                            .size(itemSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (DeskWM.focusedId.value == win.id)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                            .combinedClickable(
                                onClick = { DeskWM.focus(win.id) },
                                onLongClick = { DeskWM.close(win.id) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            app.icon, contentDescription = win.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(itemSize / 2),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ---- system tray ----
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // quick settings
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = { UiState.open(UiState.Overlay.CONTROL_CENTER) })
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Wifi, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Rounded.Bluetooth, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        Icons.Rounded.BatteryFull, null,
                        modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${batteryPercent()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // notifications
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = { UiState.open(UiState.Overlay.SHADE) })
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Notifications, contentDescription = "Notifications",
                        modifier = Modifier.size(16.dp),
                    )
                    if (NListener.connected.value && NListener.notifications.size > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.error, RoundedCornerShape(50)),
                        )
                    }
                }
                // clock
                Column(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = { UiState.toggle(UiState.Overlay.CALENDAR) })
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        SimpleDateFormat("h:mm", Locale.getDefault()).format(now),
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(now),
                        fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
