package dev.woodos.launcher.wooddesk

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.woodos.launcher.wooddesk.apps.AboutApp
import dev.woodos.launcher.wooddesk.apps.CalculatorApp
import dev.woodos.launcher.wooddesk.apps.ClockApp
import dev.woodos.launcher.wooddesk.apps.FileExplorerApp
import dev.woodos.launcher.wooddesk.apps.NotesApp
import dev.woodos.launcher.wooddesk.apps.TaskManagerApp

/** A WoodDesk built-in application that can open inside a real window. */
data class DeskApp(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val defaultW: Dp,
    val defaultH: Dp,
    val content: @Composable () -> Unit,
)

object DeskApps {
    val files = DeskApp("files", "File Manager", Icons.Rounded.Folder, 620.dp, 460.dp) { FileExplorerApp() }
    val notes = DeskApp("notes", "Notes", Icons.Rounded.StickyNote2, 540.dp, 420.dp) { NotesApp() }
    val calculator = DeskApp("calculator", "Calculator", Icons.Rounded.Calculate, 380.dp, 520.dp) { CalculatorApp() }
    val clock = DeskApp("clock", "Clock & Calendar", Icons.Rounded.CalendarMonth, 520.dp, 440.dp) { ClockApp() }
    val weather = DeskApp("weather", "Weather", Icons.Rounded.WbSunny, 480.dp, 420.dp) {
        dev.woodos.launcher.woodos.WeatherDetailsPanel()
    }
    val taskmgr = DeskApp("taskmgr", "Task Manager", Icons.Rounded.TaskAlt, 460.dp, 380.dp) { TaskManagerApp() }
    val about = DeskApp("about", "About WoodOS", Icons.Rounded.Info, 460.dp, 380.dp) { AboutApp() }
    val settings = DeskApp("settings", "Settings", Icons.Rounded.Settings, 560.dp, 520.dp) {
        dev.woodos.launcher.woodos.SettingsScreen(onClose = {})
    }

    val all = listOf(files, notes, calculator, clock, weather, taskmgr, about, settings)

    fun byId(id: String): DeskApp? = all.find { it.id == id }
}
