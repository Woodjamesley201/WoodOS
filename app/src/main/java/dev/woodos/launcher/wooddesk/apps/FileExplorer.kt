package dev.woodos.launcher.wooddesk.apps

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.ui.rightClickable
import androidx.documentfile.provider.DocumentFile
import dev.woodos.launcher.core.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileRow(
    val name: String,
    val size: Long,
    val date: Long,
    val uri: String,
    val isDir: Boolean,
    val mime: String,
)

private enum class FileLoc { HOME, RECENT, DOWNLOADS, IMAGES, VIDEOS, TREE }

/**
 * WoodDesk File Manager. Home shows quick access + granted locations.
 * Categories are populated from MediaStore (visible to any app); full
 * folder browsing uses the Storage Access Framework (Android's supported
 * way for third-party apps).
 */
@Composable
fun FileExplorerApp() {
    val context = LocalContext.current
    var loc by remember { mutableStateOf(FileLoc.HOME) }
    var query by remember { mutableStateOf("") }
    var grid by remember { mutableStateOf(true) }
    var sort by remember { mutableStateOf(1) } // 0 name, 1 date, 2 size
    var sortMenu by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<FileRow>>(emptyList()) }
    var treeBase by remember { mutableStateOf<String?>(null) }
    var treePath by remember { mutableStateOf<List<String>>(emptyList()) }
    var treeName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            Prefs.sharedPreferences.edit()
                .putString("fileTree", uri.toString())
                .putString("fileTreeName", DocumentFile.fromTreeUri(context, uri)?.name ?: "Location")
                .apply()
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // refresh after delete
        loading = true
    }

    LaunchedEffect(loc, treeBase, treePath, loading) {
        loading = true
        files = withContext(Dispatchers.IO) {
            when (loc) {
                FileLoc.RECENT -> queryMedia(context, recent = true)
                FileLoc.DOWNLOADS -> queryMedia(context, downloads = true)
                FileLoc.IMAGES -> queryMedia(context, images = true)
                FileLoc.VIDEOS -> queryMedia(context, videos = true)
                FileLoc.TREE -> treeBase?.let { listTree(context, it, treePath) } ?: emptyList()
                FileLoc.HOME -> emptyList()
            }
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // toolbar
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loc != FileLoc.HOME) {
                Icon(
                    Icons.Rounded.ArrowBack, contentDescription = "Back",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (loc == FileLoc.TREE && treePath.isNotEmpty()) {
                                treePath = treePath.dropLast(1)
                                loading = true
                            } else {
                                loc = FileLoc.HOME
                                treeBase = null
                            }
                        }
                        .padding(6.dp),
                )
                Spacer(Modifier.padding(start = 6.dp))
            } else {
                Icon(Icons.Rounded.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(start = 6.dp))
            }
            Text(
                when (loc) {
                    FileLoc.HOME -> "File Manager"
                    FileLoc.RECENT -> "Recent"
                    FileLoc.DOWNLOADS -> "Downloads"
                    FileLoc.IMAGES -> "Images"
                    FileLoc.VIDEOS -> "Videos"
                    FileLoc.TREE -> treeName.ifBlank { "Location" }
                },
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                contentDescription = "Layout",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { grid = !grid }
                    .padding(6.dp),
            )
            Box {
                Icon(
                    Icons.Rounded.Sort, contentDescription = "Sort",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { sortMenu = true }
                        .padding(6.dp),
                )
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text("Name") }, onClick = { sort = 0; sortMenu = false })
                    DropdownMenuItem(text = { Text("Date") }, onClick = { sort = 1; sortMenu = false })
                    DropdownMenuItem(text = { Text("Size") }, onClick = { sort = 2; sortMenu = false })
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search files") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        if (loc == FileLoc.HOME) {
            HomeView(
                onCategory = { loc = it },
                onOpenTree = { uri, name ->
                    treeBase = uri
                    treePath = emptyList()
                    treeName = name
                    loc = FileLoc.TREE
                },
                onAddLocation = { treePicker.launch(null) },
            )
        } else {
            val visible = if (query.isBlank()) files else files.filter { it.name.contains(query, true) }
            val sorted = when (sort) {
                0 -> visible.sortedBy { it.name.lowercase() }
                2 -> visible.sortedByDescending { it.size }
                else -> visible.sortedByDescending { it.date }
            }
            if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(sorted, key = { it.uri }) { f ->
                        FileCell(f, context, deleteLauncher) { name ->
                            treePath = treePath + name
                            treeName = name
                            loading = true
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sorted, key = { it.uri }) { f ->
                        FileRowUi(f, context, deleteLauncher) { name ->
                            treePath = treePath + name
                            treeName = name
                            loading = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeView(
    onCategory: (FileLoc) -> Unit,
    onOpenTree: (String, String) -> Unit,
    onAddLocation: () -> Unit,
) {
    val context = LocalContext.current
    val savedTree = Prefs.sharedPreferences.getString("fileTree", null)
    val savedName = Prefs.sharedPreferences.getString("fileTreeName", "") ?: ""

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeCard("Recent", Icons.Rounded.InsertDriveFile, Modifier.weight(1f)) { onCategory(FileLoc.RECENT) }
            HomeCard("Downloads", Icons.Rounded.Download, Modifier.weight(1f)) { onCategory(FileLoc.DOWNLOADS) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeCard("Images", Icons.Rounded.Image, Modifier.weight(1f)) { onCategory(FileLoc.IMAGES) }
            HomeCard("Videos", Icons.Rounded.Movie, Modifier.weight(1f)) { onCategory(FileLoc.VIDEOS) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Storage locations", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        if (savedTree != null) {
            HomeCard(
                savedName.ifBlank { "Internal storage" },
                Icons.Rounded.Folder,
                Modifier.fillMaxWidth(),
            ) { onOpenTree(savedTree, savedName) }
            Spacer(Modifier.height(8.dp))
        }
        HomeCard("Add a location (SAF)", Icons.Rounded.Add, Modifier.fillMaxWidth()) { onAddLocation() }
        Spacer(Modifier.height(12.dp))
        Text(
            "Android only lets third-party apps browse folders the user grants via the " +
                    "system picker — that is what \"Add a location\" does.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.padding(start = 10.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FileRowUi(f: FileRow, context: Context, deleteLauncher: androidx.activity.result.ActivityResultLauncher<Intent>, onDir: (String) -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .rightClickable { openDelete(context, f, deleteLauncher) }
            .clickable { if (f.isDir) onDir(f.name) else openFile(context, f) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (f.isDir) Icons.Rounded.Folder
            else if (f.mime.startsWith("image")) Icons.Rounded.Image
            else Icons.Rounded.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.padding(start = 10.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatSize(f.size)} • ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(f.date))}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileCell(f: FileRow, context: Context, deleteLauncher: androidx.activity.result.ActivityResultLauncher<Intent>, onDir: (String) -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .rightClickable { openDelete(context, f, deleteLauncher) }
            .clickable { if (f.isDir) onDir(f.name) else openFile(context, f) }
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (f.isDir) Icons.Rounded.Folder
                else if (f.mime.startsWith("image")) Icons.Rounded.Image
                else Icons.Rounded.Description,
                contentDescription = f.name,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(f.name, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun openFile(context: Context, f: FileRow) {
    if (f.isDir) return
    try {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(f.uri), f.mime.ifBlank { "*/*" })
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun openDelete(context: Context, f: FileRow, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    if (f.isDir) return
    try {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val pi = MediaStore.createDeleteRequest(
                context.contentResolver, listOf(Uri.parse(f.uri))
            )
            context.startIntentSender(pi.intentSender, null as Intent?, 0, 0, 0)
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(f.uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    } catch (_: Exception) {
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

private fun queryMedia(
    context: Context,
    recent: Boolean = false,
    downloads: Boolean = false,
    images: Boolean = false,
    videos: Boolean = false,
): List<FileRow> {
    val out = ArrayList<FileRow>()
    try {
        val collection: Uri = when {
            downloads && android.os.Build.VERSION.SDK_INT >= 29 -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            images -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            videos -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val sortOrder = when {
            recent -> "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            else -> "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        }
        val limit = if (recent) 30 else 200
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { c ->
            var n = 0
            while (c.moveToNext() && n < limit) {
                val id = c.getLong(0)
                val name = c.getString(1) ?: continue
                val mime = c.getString(4) ?: ""
                val uri = ContentUris.withAppendedId(collection, id)
                out.add(FileRow(name, c.getLong(2), c.getLong(3) * 1000L, uri.toString(), false, mime))
                n++
            }
        }
    } catch (_: Exception) {
    }
    return out
}

private fun listTree(context: Context, treeUriString: String, path: List<String>): List<FileRow> {
    val out = ArrayList<FileRow>()
    try {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return out
        var dir = root
        for (name in path) {
            dir = dir.findFile(name) ?: return out
        }
        dir.listFiles().forEach { doc ->
            out.add(
                FileRow(
                    doc.name ?: "?",
                    if (doc.isDirectory) 0 else doc.length(),
                    doc.lastModified(),
                    doc.uri.toString(),
                    doc.isDirectory,
                    doc.type ?: "",
                )
            )
        }
    } catch (_: Exception) {
    }
    return out
}
