@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.woodos.launcher.woodos.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.AppRef
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.HomeFolder
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.UiState.PickerPurpose
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.GlassPanel
import dev.woodos.launcher.ui.glassColor
import kotlin.math.roundToInt

private sealed class GridItem {
    data class App(val ref: AppRef) : GridItem()
    data class Folder(val folder: HomeFolder) : GridItem()
}

/**
 * Paged home-screen app grid with folders, long-press context menus and an
 * iOS-style edit mode: drag icons to reorder, or drag them below the grid
 * to pin them to the dock.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGrid() {
    val context = LocalContext.current
    val cols = Prefs.homeColumns.coerceIn(4, 6)
    val rows = 4
    val pageSize = cols * rows

    val items = remember(Prefs.homeApps, Prefs.homeFolders) {
        val list = ArrayList<GridItem>()
        Prefs.homeFolders.forEach { list.add(GridItem.Folder(it)) }
        Prefs.homeApps.forEach { list.add(GridItem.App(it)) }
        list
    }

    var editMode by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf<HomeFolder?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<HomeFolder?>(null) }
    var newFolderFor by remember { mutableStateOf<AppRef?>(null) }

    val pagerState = rememberPagerState { ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1) }
    val cellH = 92.dp
    var gridWpx by remember { mutableStateOf(1f) }

    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragStartIdx by remember { mutableStateOf(0) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    fun persistOrder(newItems: List<GridItem>) {
        Prefs.homeFolders = newItems.filterIsInstance<GridItem.Folder>().map { it.folder }
        Prefs.homeApps = newItems.filterIsInstance<GridItem.App>().map { it.ref }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { gridWpx = it.width.toFloat() }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !editMode,
        ) { page ->
            val pageItems = items.drop(page * pageSize).take(pageSize)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                pageItems.chunked(cols).forEachIndexed { rowIndex, rowItems ->
                    Row(Modifier.fillMaxWidth().height(cellH)) {
                        rowItems.forEachIndexed { colIndex, item ->
                            val globalIndex = page * pageSize + rowIndex * cols + colIndex
                            val key = when (item) {
                                is GridItem.App -> item.ref.key
                                is GridItem.Folder -> item.folder.id
                            }
                            val isDragging = dragKey == key

                            val dragModifier = if (editMode) {
                                Modifier.pointerInput(key) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragKey = key
                                            dragStartIdx = globalIndex
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                        },
                                        onDragEnd = {
                                            val density = this
                                            val cellWpx = (gridWpx - 28.dp.toPx()) / cols
                                            val startCol = dragStartIdx % cols
                                            val startRow = dragStartIdx / cols
                                            val dCol = (dragOffset.x / cellWpx).roundToInt()
                                            val dRow = (dragOffset.y / cellH.toPx()).roundToInt()
                                            val targetRow = (startRow + dRow).coerceIn(0, rows - 1)
                                            val targetCol = (startCol + dCol).coerceIn(0, cols - 1)
                                            val target = targetRow * cols + targetCol
                                            when {
                                                dRow > (rows - startRow) && item is GridItem.App -> {
                                                    // dropped below the grid -> pin to dock
                                                    if (Prefs.dockApps.none { it.key == item.ref.key }) {
                                                        Prefs.dockApps = Prefs.dockApps + item.ref
                                                    }
                                                    val mutable = items.toMutableList()
                                                    mutable.removeAt(dragStartIdx)
                                                    persistOrder(mutable)
                                                }
                                                target != dragStartIdx && target < items.size -> {
                                                    val mutable = items.toMutableList()
                                                    val moved = mutable.removeAt(dragStartIdx)
                                                    mutable.add(target, moved)
                                                    persistOrder(mutable)
                                                }
                                            }
                                            dragKey = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            dragKey = null
                                            dragOffset = Offset.Zero
                                        },
                                    )
                                }
                            } else Modifier

                            Box(Modifier.weight(1f).fillMaxWidth().then(dragModifier)) {
                                Box(
                                    Modifier
                                        .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                                        .graphicsLayer {
                                            if (isDragging) {
                                                scaleX = 1.12f
                                                scaleY = 1.12f
                                                alpha = 0.9f
                                            }
                                        }
                                ) {
                                    when (item) {
                                        is GridItem.App -> AppCell(
                                            ref = item.ref,
                                            menuOpen = menuFor == key,
                                            onMenu = { menuFor = if (it) key else null },
                                            onNewFolder = { newFolderFor = item.ref },
                                            onReorder = { editMode = true },
                                        )
                                        is GridItem.Folder -> FolderCell(
                                            folder = item.folder,
                                            menuOpen = menuFor == key,
                                            onMenu = { menuFor = if (it) key else null },
                                            onOpen = { openFolder = item.folder },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Page dots
        if (pagerState.pageCount > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(pagerState.pageCount) { i ->
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }

        // Edit-mode "Done" banner
        if (editMode) {
            GlassPanel(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .clickable { editMode = false },
                corner = 999.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Check, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.padding(start = 6.dp))
                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else if (items.isEmpty()) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.Apps, contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Home screen is empty.\nOpen the app drawer and long-press apps to customise.",
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                )
            }
        }

        // Long-press on empty area -> enter edit mode
        if (!editMode) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { editMode = true },
                        )
                    }
            )
        }
    }

    openFolder?.let { folder ->
        FolderOverlay(
            folder = folder,
            onClose = { openFolder = null },
            onRename = { renameTarget = it },
        )
    }

    renameTarget?.let { folder ->
        WoodTextInputDialog(
            title = "Rename folder",
            initial = folder.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                Prefs.homeFolders = Prefs.homeFolders.map {
                    if (it.id == folder.id) it.copy(name = name) else it
                }
                renameTarget = null
            },
        )
    }
    newFolderFor?.let { ref ->
        WoodTextInputDialog(
            title = "New folder name",
            initial = "Folder",
            onDismiss = { newFolderFor = null },
            onConfirm = { name ->
                val id = "f" + System.currentTimeMillis()
                Prefs.homeFolders = Prefs.homeFolders + HomeFolder(id, name, listOf(ref))
                Prefs.homeApps = Prefs.homeApps.filter { it.key != ref.key }
                newFolderFor = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    ref: AppRef,
    menuOpen: Boolean,
    onMenu: (Boolean) -> Unit,
    onNewFolder: () -> Unit,
    onReorder: () -> Unit = {},
) {
    val context = LocalContext.current
    val entry = remember(ref.key) { Apps.find(ref) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = { entry?.let { Apps.launch(context, it) } },
                onLongClick = { onMenu(true) },
            )
            .padding(horizontal = 6.dp),
    ) {
        AppIcon(ref, 52.dp)
        Spacer(Modifier.height(5.dp))
        Text(
            entry?.label ?: ref.pkg.substringAfterLast('.'),
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenu(false) }) {
            DropdownMenuItem(
                text = { Text("Open") },
                leadingIcon = { Icon(Icons.Rounded.Apps, null) },
                onClick = { onMenu(false); entry?.let { Apps.launch(context, it) } },
            )
            DropdownMenuItem(
                text = { Text("Pin to dock") },
                leadingIcon = { Icon(Icons.Rounded.Star, null) },
                onClick = {
                    onMenu(false)
                    if (Prefs.dockApps.none { it.key == ref.key }) Prefs.dockApps = Prefs.dockApps + ref
                },
            )
            DropdownMenuItem(
                text = { Text("Add to favorites") },
                leadingIcon = { Icon(Icons.Rounded.Star, null) },
                onClick = {
                    onMenu(false)
                    if (Prefs.favorites.none { it.key == ref.key }) Prefs.favorites = Prefs.favorites + ref
                },
            )
            Prefs.homeFolders.forEach { folder ->
                if (folder.apps.none { it.key == ref.key }) {
                    DropdownMenuItem(
                        text = { Text("Add to ${folder.name}") },
                        leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                        onClick = {
                            onMenu(false)
                            Prefs.homeFolders = Prefs.homeFolders.map {
                                if (it.id == folder.id) it.copy(apps = it.apps + ref) else it
                            }
                            Prefs.homeApps = Prefs.homeApps.filter { it.key != ref.key }
                        },
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("New folder") },
                leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                onClick = { onMenu(false); onNewFolder() },
            )
            DropdownMenuItem(
                text = { Text("App info") },
                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                onClick = { onMenu(false); entry?.let { Apps.appInfo(context, it) } },
            )
            DropdownMenuItem(
                text = { Text("Uninstall") },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = { onMenu(false); entry?.let { Apps.uninstall(context, it) } },
            )
            DropdownMenuItem(
                text = { Text("Reorder home") },
                onClick = { onMenu(false); onReorder() },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCell(
    folder: HomeFolder,
    menuOpen: Boolean,
    onMenu: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val apps = folder.apps.take(4)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(onClick = onOpen, onLongClick = { onMenu(true) })
            .padding(horizontal = 6.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassColor()),
            contentAlignment = Alignment.Center,
        ) {
            if (apps.isEmpty()) {
                Icon(Icons.Rounded.Folder, contentDescription = folder.name, tint = Color.White)
            } else {
                Column {
                    Row { apps.take(2).forEach { AppIcon(it, 23.dp, Modifier.padding(1.dp)) } }
                    Row { apps.drop(2).take(2).forEach { AppIcon(it, 23.dp, Modifier.padding(1.dp)) } }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            folder.name,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenu(false) }) {
            DropdownMenuItem(
                text = { Text("Remove folder (keep apps)") },
                onClick = {
                    onMenu(false)
                    Prefs.homeApps = Prefs.homeApps + folder.apps
                    Prefs.homeFolders = Prefs.homeFolders.filter { it.id != folder.id }
                },
            )
            DropdownMenuItem(
                text = { Text("Delete folder and apps") },
                onClick = {
                    onMenu(false)
                    Prefs.homeFolders = Prefs.homeFolders.filter { it.id != folder.id }
                },
            )
        }
    }
}

/** Overlay shown when a folder is tapped. */
@Composable
fun FolderOverlay(folder: HomeFolder, onClose: () -> Unit, onRename: (HomeFolder) -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(modifier = Modifier.padding(30.dp), corner = 30.dp) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(start = 8.dp))
                    Text(folder.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Rename",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onRename(folder) },
                    )
                }
                Spacer(Modifier.height(16.dp))
                val entries = folder.apps.mapNotNull { Apps.find(it) }
                if (entries.isEmpty()) {
                    Text("Empty folder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column {
                        entries.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                row.forEach { e ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = { Apps.launch(context, e) },
                                                onLongClick = {
                                                    Prefs.homeFolders = Prefs.homeFolders.map {
                                                        if (it.id == folder.id)
                                                            it.copy(apps = it.apps.filter { a -> a.key != e.ref.key })
                                                        else it
                                                    }
                                                },
                                            )
                                            .padding(6.dp),
                                    ) {
                                        AppIcon(e.ref, 46.dp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            e.label, fontSize = 10.sp, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Add apps",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        UiState.pickerFolderId = folder.id
                        UiState.pickerPurpose = PickerPurpose.FOLDER_HOME
                        UiState.open(UiState.Overlay.APP_PICKER)
                        onClose()
                    },
                )
            }
        }
    }
}
