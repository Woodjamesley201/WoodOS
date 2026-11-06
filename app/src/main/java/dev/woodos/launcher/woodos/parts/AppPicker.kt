package dev.woodos.launcher.woodos.parts

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.UiState.PickerPurpose
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.GlassPanel

/**
 * Full-screen app picker used by "add to dock / home / favorites / start /
 * desktop / folder" flows. Selecting an app applies the pending purpose.
 */
@Composable
fun AppPickerOverlay() {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = false) { }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            GlassPanel(corner = 30.dp) {
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Add app", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Close",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { UiState.close() },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search apps") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))

                    val all = Apps.apps.value
                    val filtered = if (query.isBlank()) all else all.filter {
                        it.label.contains(query, ignoreCase = true)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(86.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filtered, key = { it.ref.key }) { entry ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        applyPick(entry.ref)
                                    }
                                    .padding(6.dp),
                            ) {
                                AppIcon(entry.ref, 46.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    entry.label, fontSize = 10.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun applyPick(ref: dev.woodos.launcher.core.AppRef) {
    when (UiState.pickerPurpose) {
        PickerPurpose.DOCK -> if (Prefs.dockApps.none { it.key == ref.key }) Prefs.dockApps = Prefs.dockApps + ref
        PickerPurpose.HOME -> if (Prefs.homeApps.none { it.key == ref.key }) Prefs.homeApps = Prefs.homeApps + ref
        PickerPurpose.FAVORITES -> if (Prefs.favorites.none { it.key == ref.key }) Prefs.favorites = Prefs.favorites + ref
        PickerPurpose.START_PIN -> if (Prefs.startPins.none { it.key == ref.key }) Prefs.startPins = Prefs.startPins + ref
        PickerPurpose.DESKTOP -> if (Prefs.deskShortcuts.none { it.key == ref.key }) Prefs.deskShortcuts = Prefs.deskShortcuts + ref
        PickerPurpose.FOLDER_HOME -> {
            val id = UiState.pickerFolderId ?: return
            Prefs.homeFolders = Prefs.homeFolders.map {
                if (it.id == id && it.apps.none { a -> a.key == ref.key }) it.copy(apps = it.apps + ref) else it
            }
            Prefs.homeApps = Prefs.homeApps.filter { it.key != ref.key }
        }
        PickerPurpose.FOLDER_DESK -> {
            val id = UiState.pickerFolderId ?: return
            Prefs.deskFolders = Prefs.deskFolders.map {
                if (it.id == id && it.apps.none { a -> a.key == ref.key }) it.copy(apps = it.apps + ref) else it
            }
        }
    }
    UiState.close()
}
