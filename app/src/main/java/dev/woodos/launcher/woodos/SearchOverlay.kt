package dev.woodos.launcher.woodos

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.ui.AppIcon
import dev.woodos.launcher.ui.GlassPanel

/**
 * Global search (home search bar and Ctrl+Space): installed apps + web search
 * via the system web-search intent.
 */
@Composable
fun SearchOverlay() {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = false) { }
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.86f)
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
        ) {
            GlassPanel(corner = 30.dp) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search apps and the web") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    val matches = if (query.isBlank()) Apps.apps.value.take(6)
                    else Apps.apps.value.filter { it.label.contains(query, ignoreCase = true) }.take(8)

                    if (query.isNotBlank()) {
                        Text(
                            "Apps",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        matches.forEach { entry ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { Apps.launch(context, entry) }
                                    .padding(6.dp),
                            ) {
                                AppIcon(entry.ref, 36.dp)
                                Spacer(Modifier.padding(start = 12.dp))
                                Text(entry.label, fontSize = 14.sp)
                            }
                        }
                    }

                    if (query.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_WEB_SEARCH)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                .putExtra(Intent.EXTRA_PROCESS_TEXT, query)
                                        )
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_WEB_SEARCH)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                                .padding(6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Language, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.padding(start = 12.dp))
                            Text("Search the web for \"$query\"", fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Close",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { UiState.close() },
                    )
                }
            }
        }
    }
}
