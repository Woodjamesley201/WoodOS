@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.woodos.launcher.wooddesk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.key
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wallpaper
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.Perf
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.UiState.PickerPurpose
import dev.woodos.launcher.core.WoodMode
import dev.woodos.launcher.ui.MicaPanel
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.rightClickable
import dev.woodos.launcher.wooddesk.apps.ClockApp

/**
 * WoodDesk desktop environment: wallpaper, desktop icons with context menus,
 * real in-app windows (drag/resize/snap), centered taskbar, Start menu,
 * calendar flyout and power menu. Right-click (mouse) or long-press (touch)
 * opens the desktop context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeskEnvironment() {
    val context = LocalContext.current
    val density = LocalDensity.current
    var desktopMenu by remember { mutableStateOf(false) }
    var iconMenuFor by remember { mutableStateOf<String?>(null) }

    val statusPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                val taskbarPx = with(density) { 64.dp.toPx() }
                DeskWM.configure(size.width.toFloat(), size.height.toFloat(), taskbarPx, statusPx)
                DeskWM.setDensityFactor(density.density)
            }
            .rightClickable { desktopMenu = true },
    ) {
        // ---- desktop icons (left column grid) ----
        if (Prefs.deskShowIcons) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .padding(start = 14.dp, top = 14.dp, bottom = 90.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // built-in WoodDesk app shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    DeskApps.files.let { DeskIconItem(it.id, it.title, null, iconMenuFor) { iconMenuFor = it } }
                    DeskApps.notes.let { DeskIconItem(it.id, it.title, null, iconMenuFor) { iconMenuFor = it } }
                }
                Spacer(Modifier.height(14.dp))
                // user shortcuts (external apps)
                Prefs.deskShortcuts.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { ref ->
                            DeskIconItem(
                                ref.key,
                                Apps.find(ref)?.label ?: "",
                                ref,
                                iconMenuFor,
                            ) { iconMenuFor = it }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                // add shortcut
                Box(
                    Modifier
                        .size(76.dp)
                        .combinedClickable(onClick = {
                            UiState.pickerPurpose = PickerPurpose.DESKTOP
                            UiState.open(UiState.Overlay.APP_PICKER)
                        }),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Add, contentDescription = "Add shortcut",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Add", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        // ---- windows ----
        DeskWM.windows.forEachIndexed { idx, win ->
            if (!win.minimized) {
                key(win.id) {
                    Box(Modifier.zIndex(idx.toFloat())) {
                        DeskWindow(win, focused = DeskWM.focusedId.value == win.id)
                    }
                }
            }
        }

        // ---- snap hint ----
        when (DeskWM.snapHint.value) {
            DeskWM.SnapHint.LEFT -> SnapHintBox(0f, 0.5f)
            DeskWM.SnapHint.RIGHT -> SnapHintBox(0.5f, 0.5f)
            DeskWM.SnapHint.MAX -> SnapHintBox(0f, 1f)
            DeskWM.SnapHint.NONE -> Unit
        }

        // ---- taskbar ----
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .zIndex(50f)
        ) {
            Taskbar()
        }

        // ---- start menu ----
        AnimatedVisibility(
            visible = UiState.overlay.value == UiState.Overlay.START_MENU,
            enter = slideInVertically(tween(Perf.anim(260).toInt())) { it / 2 } + fadeIn(tween(Perf.anim(260).toInt())),
            exit = slideOutVertically(tween(Perf.anim(220).toInt())) { it / 2 } + fadeOut(tween(Perf.anim(220).toInt())),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(60f),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { UiState.close() }
                    .padding(bottom = 86.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(Modifier.padding(start = 14.dp)) { StartMenu() }
            }
        }

        // ---- calendar flyout ----
        AnimatedVisibility(
            visible = UiState.overlay.value == UiState.Overlay.CALENDAR,
            enter = slideInVertically(tween(Perf.anim(240).toInt())) { it / 2 } + fadeIn(),
            exit = slideOutVertically(tween(Perf.anim(200).toInt())) { it / 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(60f),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { UiState.close() }
                    .padding(bottom = 86.dp, end = 14.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                MicaPanel(corner = 18.dp) {
                    Box(Modifier.fillMaxWidth(0.55f).height(430.dp)) { ClockApp() }
                }
            }
        }

        // ---- power menu ----
        AnimatedVisibility(
            visible = UiState.overlay.value == UiState.Overlay.POWER,
            enter = scaleIn(tween(Perf.anim(220).toInt()), initialScale = 0.95f) + fadeIn(),
            exit = scaleOut(tween(Perf.anim(180).toInt())) + fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(70f),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { UiState.close() },
                contentAlignment = Alignment.Center,
            ) {
                PowerMenu { UiState.close() }
            }
        }

        // ---- desktop context menu ----
        DropdownMenu(expanded = desktopMenu, onDismissRequest = { desktopMenu = false }) {
            DropdownMenuItem(
                text = { Text("Add shortcut") },
                leadingIcon = { Icon(Icons.Rounded.Add, null) },
                onClick = {
                    desktopMenu = false
                    UiState.pickerPurpose = PickerPurpose.DESKTOP
                    UiState.open(UiState.Overlay.APP_PICKER)
                },
            )
            DropdownMenuItem(
                text = { Text("Refresh icons") },
                leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                onClick = {
                    desktopMenu = false
                    Apps.reload(context)
                },
            )
            DropdownMenuItem(
                text = { Text("Change wallpaper") },
                leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) },
                onClick = {
                    desktopMenu = false
                    UiState.open(UiState.Overlay.SETTINGS)
                },
            )
            DropdownMenuItem(
                text = { Text("Task Manager") },
                leadingIcon = { Icon(Icons.Rounded.DesktopWindows, null) },
                onClick = {
                    desktopMenu = false
                    DeskWM.openApp("taskmgr")
                },
            )
            DropdownMenuItem(
                text = { Text("About WoodOS") },
                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                onClick = {
                    desktopMenu = false
                    DeskWM.openApp("about")
                },
            )
            DropdownMenuItem(
                text = { Text("Switch to WoodOS") },
                onClick = {
                    desktopMenu = false
                    Prefs.mode = WoodMode.WOODOS
                },
            )
        }
    }
}

@Composable
private fun SnapHintBox(startFraction: Float, widthFraction: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(40f)
    ) {
        Box(
            Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxHeight()
                .align(if (startFraction == 0f) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(vertical = 0.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeskIconItem(
    key: String,
    label: String,
    ref: dev.woodos.launcher.core.AppRef?,
    menuFor: String?,
    onMenu: (String?) -> Unit,
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(76.dp)
            .combinedClickable(
                onClick = {
                    if (ref != null) {
                        Apps.find(ref)?.let { DeskWM.openExternal(context, ref, label, it) }
                    } else {
                        DeskWM.openApp(key)
                    }
                },
                onLongClick = { onMenu(key) },
            ),
    ) {
        if (ref != null) {
            AppIcon(ref, 40.dp)
        } else {
            val app = DeskApps.byId(key)
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(app?.icon ?: Icons.Rounded.DesktopWindows, contentDescription = label, tint = Color.White)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label.ifBlank { "App" },
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        DropdownMenu(expanded = menuFor == key, onDismissRequest = { onMenu(null) }) {
            if (ref != null) {
                DropdownMenuItem(
                    text = { Text("Remove from desktop") },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                    onClick = {
                        onMenu(null)
                        Prefs.deskShortcuts = Prefs.deskShortcuts.filter { it.key != ref.key }
                    },
                )
            }
        }
    }
}
