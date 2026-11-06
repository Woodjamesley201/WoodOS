package dev.woodos.launcher.woodos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.WoodMode
import dev.woodos.launcher.ui.GlassPanel
import dev.woodos.launcher.woodos.parts.AppGrid
import dev.woodos.launcher.woodos.parts.Dock
import androidx.compose.foundation.gestures.detectTapGestures

/**
 * WoodOS tablet home screen (iOS / One UI inspired): large clock + weather
 * widgets, a paged app grid with folders, search bar, and a glass dock.
 * Swipe down near the top opens the Notification Center.
 */
@Composable
fun HomeScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    if (amount.y > 18f && change.position.y < size.height * 0.15f) {
                        UiState.open(UiState.Overlay.SHADE)
                    }
                }
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        ) {
            // ---- widget row ----
            if (Prefs.showWeatherWidget) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.weight(1.25f)) { ClockWidget() }
                    Box(Modifier.weight(1f)) { WeatherWidget() }
                }
            }

            // ---- app grid pages ----
            Box(Modifier.weight(1f)) { AppGrid() }

            // ---- search bar ----
            if (Prefs.showSearchBar) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                        .clickable { UiState.open(UiState.Overlay.SEARCH) },
                    corner = 999.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Search, contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(start = 10.dp))
                        Text(
                            "Search apps, notes and web",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            // ---- dock ----
            Dock()
        }

        // Floating WoodDesk switch (top-right)
        if (Prefs.mode == WoodMode.WOODOS) {
            GlassPanel(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp)
                    .clickable { Prefs.mode = WoodMode.WOODDESK },
                corner = 999.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.DesktopWindows, contentDescription = "WoodDesk",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.padding(start = 6.dp))
                    Text("WoodDesk", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
