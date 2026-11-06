@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.woodos.launcher.woodos.parts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.UiState.PickerPurpose
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.GlassPanel

/**
 * Glass dock: favorite/quick apps, the app-drawer button and an add button.
 * Long-press a dock app for options (remove / info / uninstall).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock() {
    val context = LocalContext.current
    val dockApps = Prefs.dockApps
    var menuFor by remember { mutableStateOf<String?>(null) }

    val bottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = bottomPad + 10.dp)
    ) {
        GlassPanel(corner = 30.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // App drawer button
                Box(
                    Modifier
                        .size(52.dp)
                        .clickable { UiState.open(UiState.Overlay.DRAWER) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Apps, contentDescription = "App drawer",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(Modifier.padding(start = 4.dp))

                dockApps.forEach { ref ->
                    val entry = remember(ref.key) { Apps.find(ref) }
                    Box {
                        AppIcon(
                            ref, 48.dp,
                            Modifier
                                .combinedClickable(
                                    onClick = { entry?.let { Apps.launch(context, it) } },
                                    onLongClick = { menuFor = ref.key },
                                )
                        )
                        DropdownMenu(expanded = menuFor == ref.key, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(
                                text = { Text("Remove from dock") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                onClick = {
                                    menuFor = null
                                    Prefs.dockApps = Prefs.dockApps.filter { it.key != ref.key }
                                },
                            )
                            entry?.let {
                                DropdownMenuItem(
                                    text = { Text("App info") },
                                    onClick = { menuFor = null; Apps.appInfo(context, it) },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Add to dock
                Box(
                    Modifier
                        .size(44.dp)
                        .clickable {
                            UiState.pickerPurpose = PickerPurpose.DOCK
                            UiState.open(UiState.Overlay.APP_PICKER)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add, contentDescription = "Add to dock",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
