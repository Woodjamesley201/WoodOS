package dev.woodos.launcher.woodos

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.notifications.WoodNotificationListener
import dev.woodos.launcher.notifications.WoodNotificationListener.Companion as NListener
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.GlassPanel

/**
 * WoodOS Notification Center: grouped, expandable cards, clear-all and a
 * session notification history. Requires notification access (prompted).
 */
@Composable
fun NotificationShadeOverlay() {
    val context = LocalContext.current
    var showHistory by remember { mutableStateOf(false) }
    var expandedKeys by remember { mutableStateOf(setOf<String>()) }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = false) { }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .align(Alignment.TopCenter)
                .padding(12.dp)
        ) {
            GlassPanel(corner = 32.dp) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    // header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text("Notifications", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        if (NListener.connected.value) {
                            TextButton(onClick = { showHistory = !showHistory }) {
                                Icon(Icons.Rounded.History, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.padding(start = 4.dp))
                                Text(if (showHistory) "Active" else "History", fontSize = 12.sp)
                            }
                            TextButton(onClick = { NListener.clearAll() }) { Text("Clear all", fontSize = 12.sp) }
                        }
                        Icon(
                            Icons.Rounded.Close, contentDescription = "Close",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { UiState.close() }
                                .padding(4.dp),
                        )
                    }

                    if (!NListener.connected.value) {
                        // permission prompt
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Rounded.Notifications, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Allow notification access",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "WoodOS needs Android's notification-listener permission to show " +
                                        "your notifications here. This is the standard, Google-approved way.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = { NListener.openAccessSettings(context) }) {
                                Text("Open settings")
                            }
                        }
                    } else if (showHistory) {
                        val hist = NListener.history
                        if (hist.isEmpty()) {
                            EmptyHint("No dismissed notifications this session")
                        } else {
                            NotifList(hist, emptySet(), onExpand = {})
                        }
                    } else {
                        val active = NListener.notifications
                        if (active.isEmpty()) {
                            EmptyHint("No notifications")
                        } else {
                            NotifList(active, expandedKeys) { key ->
                                expandedKeys = if (key in expandedKeys) expandedKeys - key else expandedKeys + key
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun NotifList(
    list: List<dev.woodos.launcher.notifications.WoodNotif>,
    expandedKeys: Set<String>,
    onExpand: (String) -> Unit,
) {
    val context = LocalContext.current
    val grouped = remember(list) { list.groupBy { it.pkg } }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grouped.forEach { (pkg, notifs) ->
            item(key = "grp_$pkg") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ref = notifs.firstOrNull()?.ref
                    if (ref != null) AppIcon(ref, 22.dp) else Box(Modifier.size(22.dp))
                    Spacer(Modifier.padding(start = 8.dp))
                    Text(
                        notifs.first().appName,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text("${notifs.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(notifs, key = { it.key }) { n ->
                val expanded = n.key in expandedKeys
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onExpand(n.key) }
                        .padding(12.dp),
                ) {
                    Text(n.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        n.text,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) 10 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row {
                        Text(
                            java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                .format(java.util.Date(n.timestamp)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Open",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clickable { NListener.open(context, n) },
                        )
                        Text(
                            "Dismiss",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clickable { NListener.dismiss(n) },
                        )
                    }
                }
            }
        }
    }
}
