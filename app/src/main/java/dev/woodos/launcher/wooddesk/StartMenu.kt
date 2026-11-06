@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.woodos.launcher.wooddesk

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.RecentTracker
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.UiState.PickerPurpose
import dev.woodos.launcher.core.WoodMode
import dev.woodos.launcher.notifications.WoodNotificationListener.Companion as NListener
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.MicaPanel
import dev.woodos.launcher.ui.rightClickable

/**
 * Windows 11-inspired Start menu: search, pinned grid, recommended (recent)
 * apps, all-apps shortcut, user profile and power menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartMenu() {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    MicaPanel(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(560.dp),
        corner = 18.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            // search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search for apps and settings") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))

            val filtered = if (query.isBlank()) Apps.apps.value
            else Apps.apps.value.filter { it.label.contains(query, ignoreCase = true) }

            if (query.isNotBlank()) {
                // search results
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(86.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(filtered.take(24), key = { it.ref.key }) { entry ->
                        StartApp(entry.ref, entry.label) {
                            DeskWM.openExternal(context, entry.ref, entry.label, entry)
                            UiState.close()
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(86.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) { MenuLabel("Pinned") }
                    val pins = Prefs.startPins
                    items(pins, key = { "p_" + it.key }) { ref ->
                        StartApp(ref, Apps.find(ref)?.label ?: "") {
                            val entry = Apps.find(ref)
                            DeskWM.openExternal(context, ref, "", entry)
                            UiState.close()
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    UiState.pickerPurpose = PickerPurpose.START_PIN
                                    UiState.open(UiState.Overlay.APP_PICKER)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.padding(start = 4.dp))
                                Text("Pin more", fontSize = 11.sp)
                            }
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) { MenuLabel("Recommended") }
                    val recents = RecentTracker.recent().mapNotNull { Apps.find(it) }
                    items(
                        (recents.ifEmpty { Apps.apps.value.take(6) }).take(6),
                        key = { "r_" + it.ref.key },
                    ) { entry ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .combinedClickable(onClick = {
                                    DeskWM.openExternal(context, entry.ref, entry.label, entry)
                                    UiState.close()
                                })
                                .padding(6.dp),
                        ) {
                            AppIcon(entry.ref, 34.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                entry.label, fontSize = 9.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { UiState.open(UiState.Overlay.DRAWER) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Apps, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.padding(start = 8.dp))
                            Text("All apps", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // bottom bar: user + power
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Person, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(5.dp),
                )
                Spacer(Modifier.padding(start = 8.dp))
                Column {
                    Text("Owner", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("WoodOS user", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Switch to WoodOS",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { Prefs.mode = WoodMode.WOODOS; UiState.close() },
                )
                Spacer(Modifier.padding(start = 12.dp))
                Icon(
                    Icons.Rounded.PowerSettingsNew, contentDescription = "Power",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { UiState.open(UiState.Overlay.POWER) }
                        .padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartApp(ref: dev.woodos.launcher.core.AppRef, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = { Prefs.startPins = Prefs.startPins.filter { it.key != ref.key } },
            )
            .padding(6.dp),
    ) {
        AppIcon(ref, 42.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            label.ifBlank { ref.pkg.substringAfterLast('.') },
            fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Power menu. Android does not let a normal app shut down or restart the
 * device, so WoodDesk offers what is possible plus a clear explanation.
 */
@Composable
fun PowerMenu(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    MicaPanel(
        modifier = Modifier.fillMaxWidth(0.4f),
        corner = 18.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Power", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            PowerItem("Close all WoodDesk windows") {
                DeskWM.closeAll()
                onDismiss()
            }
            PowerItem("Switch to WoodOS") {
                Prefs.mode = WoodMode.WOODOS
                onDismiss()
            }
            PowerItem("Open system power options") {
                try {
                    ctx.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
                onDismiss()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Android does not allow third-party apps to power off or restart the " +
                        "device. Use the physical power button, or long-press it in WoodOS.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PowerItem(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.DesktopWindows, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.padding(start = 8.dp))
        Text(label, fontSize = 13.sp)
    }
}
