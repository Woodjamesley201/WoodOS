@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.woodos.launcher.woodos.parts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.GlassPanel
import dev.woodos.launcher.ui.pillColor

/**
 * WoodOS app drawer: search, alphabetical sections, recently installed,
 * favorites and recently used. Long-press apps to place them on the home
 * screen, dock or favorites.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerOverlay() {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        GlassPanel(corner = 34.dp) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                // header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
                    leadingIcon = { Icon(Icons.Rounded.Star, null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))

                // tabs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Recent", "Favorites").forEachIndexed { i, label ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(pillColor(tab == i))
                                .clickable { tab = i }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (tab == i) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                val all = Apps.apps.value
                val filtered = if (query.isBlank()) all
                else all.filter { it.label.contains(query, ignoreCase = true) }

                when (tab) {
                    0 -> AlphabeticalGrid(filtered, menuFor) { menuFor = it }
                    1 -> RecentGrid(menuFor) { menuFor = it }
                    2 -> FavoritesGrid(menuFor) { menuFor = it }
                }
            }
        }
    }
}

@Composable
private fun DrawerCell(entry: dev.woodos.launcher.core.AppEntry, menuFor: String?, setMenu: (String?) -> Unit) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = { Apps.launch(context, entry) },
                onLongClick = { setMenu(entry.ref.key) },
            )
            .padding(6.dp),
    ) {
        AppIcon(entry.ref, 48.dp)
        Spacer(Modifier.height(5.dp))
        Text(entry.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        DropdownMenu(expanded = menuFor == entry.ref.key, onDismissRequest = { setMenu(null) }) {
            DropdownMenuItem(
                text = { Text("Add to home screen") },
                leadingIcon = { Icon(Icons.Rounded.Add, null) },
                onClick = {
                    setMenu(null)
                    if (Prefs.homeApps.none { it.key == entry.ref.key }) Prefs.homeApps = Prefs.homeApps + entry.ref
                },
            )
            DropdownMenuItem(
                text = { Text("Pin to dock") },
                leadingIcon = { Icon(Icons.Rounded.Add, null) },
                onClick = {
                    setMenu(null)
                    if (Prefs.dockApps.none { it.key == entry.ref.key }) Prefs.dockApps = Prefs.dockApps + entry.ref
                },
            )
            DropdownMenuItem(
                text = { Text("Add to favorites") },
                leadingIcon = { Icon(Icons.Rounded.Star, null) },
                onClick = {
                    setMenu(null)
                    if (Prefs.favorites.none { it.key == entry.ref.key }) Prefs.favorites = Prefs.favorites + entry.ref
                },
            )
            DropdownMenuItem(
                text = { Text("Remove from favorites") },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = {
                    setMenu(null)
                    Prefs.favorites = Prefs.favorites.filter { it.key != entry.ref.key }
                },
            )
        }
    }
}

@Composable
private fun AlphabeticalGrid(
    filtered: List<dev.woodos.launcher.core.AppEntry>,
    menuFor: String?,
    setMenu: (String?) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(84.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        var lastLetter = ' '
        filtered.forEach { entry ->
            val letter = entry.label.firstOrNull()?.uppercaseChar() ?: '?'
            if (letter != lastLetter) {
                lastLetter = letter
                item(span = { GridItemSpan(maxLineSpan) }, key = "hdr_$letter") {
                    Text(
                        letter.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
            }
            item(key = entry.ref.key) { DrawerCell(entry, menuFor, setMenu) }
        }
    }
}

@Composable
private fun RecentGrid(menuFor: String?, setMenu: (String?) -> Unit) {
    val recent = RecentTracker.recent()
        .mapNotNull { ref -> Apps.find(ref) }
    val recentInstalled = Apps.apps.value.sortedByDescending { it.firstInstallTime }.take(12)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(84.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent_used") {
                SectionLabel("Recently used")
            }
            items(recent, key = { "ru_" + it.ref.key }) { DrawerCell(it, menuFor, setMenu) }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "recent_installed") {
            SectionLabel("Recently installed")
        }
        items(recentInstalled, key = { "ri_" + it.ref.key }) { DrawerCell(it, menuFor, setMenu) }
    }
}

@Composable
private fun FavoritesGrid(menuFor: String?, setMenu: (String?) -> Unit) {
    val favs = Prefs.favorites.mapNotNull { Apps.find(it) }
    if (favs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No favorites yet.\nLong-press an app to add it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(84.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(favs, key = { it.ref.key }) { DrawerCell(it, menuFor, setMenu) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}
