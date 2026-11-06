package dev.woodos.launcher.wooddesk.apps

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Note
import dev.woodos.launcher.core.Prefs
import java.util.UUID

/**
 * WoodDesk Notes: a simple notes app stored in preferences.
 */
@Composable
fun NotesApp() {
    var notes by remember { mutableStateOf(Prefs.notes) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { notes = Prefs.notes }

    fun save(list: List<Note>) {
        notes = list
        Prefs.notes = list
    }

    if (editing != null) {
        NoteEditor(
            note = editing!!,
            onSave = { n ->
                save(notes.filter { it.id != n.id } + n)
                editing = null
            },
            onDelete = {
                save(notes.filter { it.id != editing!!.id })
                editing = null
            },
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.Create, contentDescription = "New note",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(7.dp)
                    .clickable {
                        editing = Note(UUID.randomUUID().toString(), "", "", System.currentTimeMillis())
                    },
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search notes") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        val visible = if (query.isBlank()) notes
        else notes.filter { it.title.contains(query, true) || it.body.contains(query, true) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (visible.isEmpty()) {
                Text(
                    "No notes yet. Tap + to create one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                )
            }
            visible.forEach { note ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { editing = note }
                        .padding(12.dp),
                ) {
                    Text(note.title.ifBlank { "Untitled" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        note.body.take(80),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoteEditor(note: Note, onSave: (Note) -> Unit, onDelete: () -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.ArrowBack, contentDescription = "Back",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onSave(Note(note.id, title, body, System.currentTimeMillis())) }
                    .padding(7.dp),
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.padding(start = 4.dp))
                Text("Delete", fontSize = 12.sp)
            }
            TextButton(onClick = { onSave(Note(note.id, title, body, System.currentTimeMillis())) }) {
                Text("Save", fontSize = 12.sp)
            }
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            singleLine = true,
            placeholder = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            placeholder = { Text("Write something…") },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}
