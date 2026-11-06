package com.woodos.desk

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.woodos.R
import com.woodos.core.DeviceInfo
import com.woodos.core.Palette
import com.woodos.core.Prefs
import com.woodos.perf.PerformanceMonitor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Built-in WoodDesk applications. These run inside real WoodDesk windows
 * (drag, resize, minimize, maximize) because they are part of the WoodOS
 * process. Third-party apps are launched via Android's multi-window /
 * freeform support from the taskbar - see WoodDeskActivity.
 */
object DeskApps {

    // ------------------------------------------------------------------ Notes

    fun notesApp(context: Context, prefs: Prefs): View {
        val colors = Palette.colors(prefs, context)
        val pad = dp(context, 14)

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad, pad, pad)

        val editor = EditText(context)
        editor.hint = "Write something..."
        editor.setTextColor(colors.text)
        editor.setHintTextColor(colors.textDim)
        editor.gravity = Gravity.TOP
        editor.background = null

        val bar = LinearLayout(context)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.END or Gravity.CENTER_VERTICAL

        fun button(label: String, onClick: (View) -> Unit): Button {
            val b = Button(context, null, android.R.attr.borderlessButtonStyle)
            b.text = label
            b.setTextColor(prefs.accent)
            b.setOnClickListener(onClick)
            return b
        }

        val filename = TextView(context).apply {
            text = "note.txt"
            setTextColor(colors.textDim)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        }

        fun dir(): File = File(context.filesDir, "notes").apply { mkdirs() }

        fun load(name: String) {
            val f = File(dir(), name)
            editor.setText(if (f.exists()) f.readText() else "")
            filename.text = name
        }

        fun save() {
            val name = filename.text.toString().ifBlank { "note.txt" }
            File(dir(), name).writeText(editor.text.toString())
            Toast.makeText(context, "Saved $name", Toast.LENGTH_SHORT).show()
        }

        bar.addView(filename)
        bar.addView(button("Save") { save() })
        bar.addView(button("New") {
            editor.setText("")
            filename.text = "note-${SimpleDateFormat("HHmmss", Locale.US).format(Date())}.txt"
        })
        bar.addView(button("Open") {
            val files = dir().listFiles()?.map { it.name } ?: emptyList()
            if (files.isEmpty()) {
                Toast.makeText(context, "No saved notes yet", Toast.LENGTH_SHORT).show()
            } else {
                android.app.AlertDialog.Builder(context)
                    .setTitle("Open note")
                    .setItems(files.toTypedArray()) { _, which -> load(files[which]) }
                    .show()
            }
        })

        root.addView(bar)
        root.addView(
            editor,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        load("note.txt")
        return root
    }

    // ------------------------------------------------------------------ Calculator

    fun calculatorApp(context: Context, prefs: Prefs): View {
        val colors = Palette.colors(prefs, context)
        val display = TextView(context).apply {
            text = "0"
            textSize = 34f
            setTextColor(colors.text)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            typeface = Typeface.MONOSPACE
            setPadding(dp(context, 8), dp(context, 16), dp(context, 8), dp(context, 16))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.START
        }
        val expression = TextView(context).apply {
            text = ""
            textSize = 13f
            setTextColor(colors.textDim)
            gravity = Gravity.END
            setPadding(dp(context, 8), dp(context, 8), dp(context, 8), 0)
            maxLines = 1
        }

        val keys = arrayOf(
            "C", "(", ")", "/",
            "7", "8", "9", "*",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            ".", "0", "⌫", "=",
        )
        val grid = GridLayout(context)
        grid.columnCount = 4
        grid.rowCount = 5

        keys.forEach { key ->
            val btn = TextView(context).apply {
                text = key
                textSize = 19f
                gravity = Gravity.CENTER
                setTextColor(if (key == "=") colors.text else colors.text)
                background = gradient(colors.surfaceAlt)
                setOnClickListener {
                    when (key) {
                        "C" -> { display.text = "0"; expression.text = "" }
                        "⌫" -> display.text = display.text.dropLast(1).ifEmpty { "0" }
                        "=" -> {
                            val expr = display.text.toString()
                            val result = CalculatorEngine.evaluate(expr)
                            if (result == null) {
                                expression.text = expr
                                display.text = "Error"
                            } else {
                                expression.text = "$expr ="
                                display.text = CalculatorEngine.format(result)
                            }
                        }
                        else -> {
                            if (display.text.toString() == "0" && key != ".") display.text = key
                            else display.append(key)
                        }
                    }
                }
            }
            val lp = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            btn.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(context, 56)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))
            }
            grid.addView(btn)
        }

        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
        root.addView(expression)
        root.addView(display)
        root.addView(
            grid,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        return root
    }

    // ------------------------------------------------------------------ Files

    fun filesApp(context: Context, prefs: Prefs): View {
        val colors = Palette.colors(prefs, context)
        val list = ListView(context)
        list.divider = null
        list.setPadding(0, dp(context, 8), 0, dp(context, 8))

        val roots = mutableListOf<File>()
        context.filesDir?.let { roots.add(it) }
        context.getExternalFilesDirs(null)
            ?.filterNotNull()?.forEach { roots.add(it) }

        fun filesText(): List<Pair<String, String>> =
            roots.flatMap { root ->
                root.walkTopDown().take(200)
                    .filter { it.isFile }
                    .map { it.name to "${human(it.length())}  ·  ${it.parentFile?.name ?: ""}" }
            }.sortedBy { it.first.lowercase() }

        fun refresh() {
            val rows = filesText()
            list.adapter = object : ArrayAdapter<String>(
                context, android.R.layout.simple_list_item_2, android.R.id.text1,
                rows.map { it.first }
            ) {
                override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                    val v = super.getView(pos, cv, parent)
                    val t1 = v.findViewById<TextView>(android.R.id.text1)
                    val t2 = v.findViewById<TextView>(android.R.id.text2)
                    t1.text = rows[pos].first
                    t1.setTextColor(colors.text)
                    t2.text = rows[pos].second
                    t2.setTextColor(colors.textDim)
                    return v
                }
            }
        }

        list.setOnItemClickListener { _, _, pos, _ ->
            val rows = filesText()
            val name = rows.getOrNull(pos)?.first ?: return@setOnItemClickListener
            val file = roots.asSequence()
                .flatMap { it.walkTopDown() }
                .firstOrNull { it.isFile && it.name == name }
            if (file == null) return@setOnItemClickListener
            if (file.extension.lowercase() in setOf("txt", "md", "json", "log")) {
                val text = runCatching { file.readText().take(20_000) }.getOrDefault("")
                android.app.AlertDialog.Builder(context)
                    .setTitle(file.name)
                    .setMessage(text.ifBlank { "(empty file)" })
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            } else {
                Toast.makeText(context, "${file.name} (${human(file.length())})", Toast.LENGTH_SHORT).show()
            }
        }

        refresh()
        return list
    }

    private fun human(bytes: Long): String {
        val kb = bytes / 1024.0
        return when {
            bytes < 1024 -> "$bytes B"
            kb < 1024 -> String.format(Locale.US, "%.1f KB", kb)
            else -> String.format(Locale.US, "%.1f MB", kb / 1024)
        }
    }

    // ------------------------------------------------------------------ System info

    fun systemInfoApp(context: Context, prefs: Prefs): View {
        val colors = Palette.colors(prefs, context)
        val info = PerformanceMonitor.report(context)
        val device = DeviceInfo(context).snapshot()
        val ver = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("?")

        val lines = listOf(
            "WoodOS" to "version $ver",
            "Device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            "Android" to "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
            "Display" to "${context.resources.displayMetrics.widthPixels} × ${context.resources.displayMetrics.heightPixels}",
            "Device RAM" to "%.0f MB total · %.0f MB free".format(
                Locale.US, info.deviceTotalRamMb, info.deviceAvailableRamMb
            ),
            "WoodOS memory" to "%.1f MB Java heap · %.1f MB native".format(
                Locale.US, info.javaHeapUsedMb, info.nativeHeapMb
            ),
            "Storage" to if (device.storageTotalBytes > 0)
                "${DeviceInfo.formatBytes(device.storageFreeBytes)} free of ${DeviceInfo.formatBytes(device.storageTotalBytes)}"
            else "n/a",
            "Battery" to if (device.batteryPercent >= 0) "${device.batteryPercent}%${if (device.charging) " (charging)" else ""}" else "n/a",
            "Wi-Fi" to if (device.wifiConnected) "Connected${device.wifiSsid?.let { " · $it" } ?: ""}" else "Not connected",
            "Freeform windows" to if (context.packageManager.hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT
                )
            ) "Supported by this device" else "Not supported by this device",
        )

        val text = TextView(context).apply {
            textSize = 13f
            setLineSpacing(dp(context, 4).toFloat(), 1f)
            setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18))
        }
        val sb = StringBuilder()
        lines.forEach { (k, v) ->
            sb.append(k.uppercase(Locale.US)).append('\n')
                .append(v).append("\n\n")
        }
        text.text = sb.trim()
        text.setTextColor(colors.text)

        val scroll = android.widget.ScrollView(context)
        scroll.addView(text)
        return scroll
    }

    // ------------------------------------------------------------------ helpers

    fun gradient(color: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = 12f
        }

    fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density + 0.5f).toInt()
}
