package dev.woodos.launcher.wooddesk.apps

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Perf
import dev.woodos.launcher.wooddesk.DeskWM
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Clock & calendar window. */
@Composable
fun ClockApp() {
    val now = remember { Date() }
    val timeFmt = remember { SimpleDateFormat("h:mm:ss a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(timeFmt.format(now), fontSize = 44.sp, fontWeight = FontWeight.Light)
        Text(dateFmt.format(now), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        val cal = remember { Calendar.getInstance() }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val today = cal.get(Calendar.DAY_OF_MONTH)

        val monthCal = remember(year, month) {
            Calendar.getInstance().apply { set(year, month, 1) }
        }
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDow = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

        Text(
            monthCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " $year",
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        var day = 1
        var cells = 0
        while (day <= daysInMonth) {
            Row {
                repeat(7) { dow ->
                    val d: Int? = if (cells >= firstDow && day <= daysInMonth) {
                        val v = day; day++; v
                    } else null
                    Box(
                        Modifier.weight(1f).height(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (d != null) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    d.toString(),
                                    fontSize = 12.sp,
                                    color = if (d == today) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (d == today) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                    cells++
                }
            }
        }
    }
}

/** Task manager: list of open WoodDesk windows + memory info. */
@Composable
fun TaskManagerApp() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Open windows", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val windows = DeskWM.windows.toList()
        if (windows.isEmpty()) {
            Text("No open windows", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        } else {
            windows.forEach { w ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${w.title}  •  ${if (w.minimized) "minimized" else if (w.maximized) "maximized" else "windowed"}",
                        fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { DeskWM.focus(w.id) }) { Text("Focus", fontSize = 12.sp) }
                    TextButton(onClick = { DeskWM.close(w.id) }) { Text("Close", fontSize = 12.sp) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Memory, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = 8.dp))
            Text(Perf.deviceSummary(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { DeskWM.cycleWindows() }) { Text("Cycle (Alt+Tab)") }
            TextButton(onClick = { DeskWM.closeAll() }) { Text("Close all") }
        }
    }
}

/** About window. */
@Composable
fun AboutApp() {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("WoodOS 1.0 \"Timber\"", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "WoodOS tablet environment + WoodDesk desktop.\n" +
                    "Built with Kotlin & Jetpack Compose.\n\n" +
                    "Designed and optimised for the Samsung Galaxy Tab A9 (SM-X110): " +
                    "8.7\" WXGA+ (1340x800), Helio G99, Android 13+.\n\n" +
                    "External Android apps open full screen because Android does not allow " +
                    "third-party launchers to embed other apps in windows. WoodDesk's own " +
                    "apps run in true desktop windows with drag, resize and snapping.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Text(Perf.deviceSummary(), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
    }
}
