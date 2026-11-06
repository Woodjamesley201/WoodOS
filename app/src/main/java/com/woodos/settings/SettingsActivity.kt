package com.woodos.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.woodos.R
import com.woodos.apps.AppRepository
import com.woodos.core.DeviceInfo
import com.woodos.core.Palette
import com.woodos.core.Prefs
import com.woodos.core.Palette.ThemeColors
import com.woodos.core.Wallpapers
import com.woodos.perf.PerformanceMonitor

/**
 * WoodOS settings. Sections are built programmatically to keep the
 * layout flat and cheap. Changes take effect on the home screen and
 * in WoodDesk the next time they resume.
 */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var colors: ThemeColors
    private lateinit var list: LinearLayout
    private lateinit var repo: AppRepository

    private val perfHandler = Handler(Looper.getMainLooper())
    private var perfText: TextView? = null
    private val perfTicker = object : Runnable {
        override fun run() {
            updatePerf()
            perfHandler.postDelayed(this, 2000)
        }
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        colors = Palette.colors(prefs, this)
        setTheme(if (colors.isDark) R.style.Theme_WoodOS else R.style.Theme_WoodOS_Light)
        super.onCreate(savedInstanceState)
        repo = AppRepository(this)
        setContentView(R.layout.activity_settings)
        list = findViewById(R.id.settingsList)
        findViewById<View>(R.id.settingsBack).setOnClickListener { finish() }
        build()
    }

    override fun onResume() {
        super.onResume()
        perfHandler.post(perfTicker)
    }

    override fun onPause() {
        perfHandler.removeCallbacks(perfTicker)
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_WALLPAPER && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            prefs.wallpaperUri = uri.toString()
            prefs.wallpaperId = "custom"
            Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------------ builders

    private fun build() {
        list.removeAllViews()
        colors = Palette.colors(prefs, this)
        buildAppearance()
        buildHomeSection()
        buildDeskSection()
        buildPerformance()
        buildAbout()
    }

    private fun header(text: String) {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 12f
        tv.letterSpacing = 0.08f
        tv.setTextColor(colors.textDim)
        tv.setPadding(0, dp(28), 0, dp(8))
        list.addView(tv)
    }

    /** A settings row with a title, optional subtitle and an optional trailing view. */
    private fun row(title: String, subtitle: String? = null, trailing: View? = null) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(10), 0, dp(10))
        val textCol = LinearLayout(this)
        textCol.orientation = LinearLayout.VERTICAL
        val t1 = TextView(this)
        t1.text = title
        t1.textSize = 15f
        t1.setTextColor(colors.text)
        textCol.addView(t1)
        subtitle?.let {
            val t2 = TextView(this)
            t2.text = it
            t2.textSize = 12f
            t2.setTextColor(colors.textDim)
            t2.setPadding(0, dp(2), 0, 0)
            textCol.addView(t2)
        }
        row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        trailing?.let { row.addView(it) }
        list.addView(row)
    }

    private fun textButton(label: String, color: Int, onClick: () -> Unit): Button {
        val b = Button(this, null, android.R.attr.borderlessButtonStyle)
        b.text = label
        b.setTextColor(color)
        b.setOnClickListener { onClick() }
        return b
    }

    // ------------------------------------------------------------------ appearance

    private fun buildAppearance() {
        header("Appearance")

        // Theme
        row("Theme", "Dark is the WoodOS default", segmented(
            listOf("Dark" to "dark", "Light" to "light", "System" to "system"),
            prefs.theme
        ) {
            prefs.theme = it
            recreate()
        })

        // Accent color
        row("Accent color", null, null)
        val swatches = LinearLayout(this)
        Palette.ACCENTS.forEach { accent ->
            val dot = ImageView(this)
            dot.layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                marginEnd = dp(10)
            }
            dot.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(accent.color)
                if (prefs.accent == accent.color) setStroke(dp(2), this@SettingsActivity.colors.text)
            }
            dot.setOnClickListener {
                prefs.accent = accent.color
                build()
            }
            swatches.addView(dot)
        }
        list.addView(swatches)

        // Wallpaper
        header("Wallpaper")
        val wallRow = LinearLayout(this)
        Wallpapers.BUILT_IN.forEach { wp ->
            val iv = ImageView(this)
            iv.layoutParams = LinearLayout.LayoutParams(dp(60), dp(84)).apply {
                marginEnd = dp(10)
            }
            val bg = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(wp.top, wp.bottom)
            )
            if (prefs.wallpaperId == wp.id) bg.setStroke(dp(2), prefs.accent)
            iv.background = bg
            iv.setOnClickListener {
                prefs.wallpaperId = wp.id
                Toast.makeText(this, "Wallpaper: ${wp.name}", Toast.LENGTH_SHORT).show()
                build()
            }
            wallRow.addView(iv)
        }
        wallRow.addView(
            textButton("Choose image...", prefs.accent) {
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    },
                    REQ_WALLPAPER
                )
            }
        )
        list.addView(wallRow)

        // Icons
        row("Icon shape", null, segmented(
            listOf("Circle" to "circle", "Rounded" to "rounded", "Square" to "square"),
            prefs.iconShape
        ) { prefs.iconShape = it })

        row("Icon size", "${prefs.iconSizeDp} dp", null)
        val seek = SeekBar(this)
        seek.max = 32 // maps to 40..72 dp
        seek.progress = (prefs.iconSizeDp - 40).coerceIn(0, 32)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                prefs.iconSizeDp = 40 + value
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        list.addView(seek)
    }

    /** A row of small segmented buttons. */
    private fun segmented(
        options: List<Pair<String, String>>,
        current: String,
        onPick: (String) -> Unit,
    ): LinearLayout {
        val box = LinearLayout(this)
        options.forEach { (label, value) ->
            val b = TextView(this)
            b.text = label
            b.textSize = 13f
            b.gravity = Gravity.CENTER
            b.setPadding(dp(14), dp(8), dp(14), dp(8))
            val selected = value == current
            b.setTextColor(if (selected) 0xFF101014.toInt() else colors.text)
            b.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (selected) prefs.accent else this@SettingsActivity.colors.surfaceAlt)
                cornerRadius = dp(18).toFloat()
            }
            b.setOnClickListener { onPick(value) }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            b.layoutParams = lp
            box.addView(b)
        }
        return box
    }

    // ------------------------------------------------------------------ home

    private fun buildHomeSection() {
        header("Home screen")
        row("Hidden apps", "Choose which apps never appear in the drawer",
            textButton("Edit", prefs.accent) { showHideAppsDialog() })
        row("Reset favorites", "Remove all favorites and folders",
            textButton("Reset", 0xFFE06C6C.toInt()) {
                prefs.favoritesJson = "[]"
                Toast.makeText(this, "Favorites cleared", Toast.LENGTH_SHORT).show()
            })
    }

    private fun showHideAppsDialog() {
        val apps = repo.loadApps(prefs, force = true)
        val hidden = prefs.stringList("hidden_json").toMutableSet()
        val names = apps.map { it.label }.toTypedArray()
        val checks = apps.map { it.component.packageName in hidden }.toBooleanArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Hidden apps")
            .setMultiChoiceItems(names, checks) { _, which, isChecked ->
                val pkg = apps[which].component.packageName
                if (isChecked) hidden.add(pkg) else hidden.remove(pkg)
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefs.putStringList("hidden_json", hidden.toList())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ------------------------------------------------------------------ WoodDesk

    private fun buildDeskSection() {
        header("WoodDesk")
        val freeformSupported = packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT
        )
        row(
            "Desktop windows for other apps",
            if (freeformSupported)
                "Your device supports freeform windows; other apps open in desktop-style windows"
            else
                "Not supported by this device - other apps will open full screen",
            switch(prefs.deskFreeform && freeformSupported, freeformSupported) {
                prefs.deskFreeform = it
            }
        )
        row("Extend to external display", "Show WoodDesk on a connected display (HDMI / USB-C)",
            switch(prefs.deskExtendDisplay, true) {
                prefs.deskExtendDisplay = it
            })
    }

    private fun switch(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit): Switch {
        val sw = Switch(this)
        sw.isChecked = checked
        sw.isEnabled = enabled
        sw.setOnCheckedChangeListener { _, value -> onChange(value) }
        return sw
    }

    // ------------------------------------------------------------------ performance

    private fun buildPerformance() {
        header("Performance")
        val info = PerformanceMonitor.report(this)
        val device = DeviceInfo(this).snapshot()
        val pss = PerformanceMonitor.approximatePssMb(this)
        perfText = TextView(this).apply {
            text = listOf(
                "WoodOS Java heap: %.1f MB (max %.0f MB)".format(info.javaHeapUsedMb, info.javaHeapMaxMb),
                "WoodOS native heap: %.1f MB".format(info.nativeHeapMb),
                "WoodOS total memory (approx. PSS): %.1f MB".format(pss),
                "Device RAM: %.0f MB total, %.0f MB available".format(info.deviceTotalRamMb, info.deviceAvailableRamMb),
                "Storage: %s free of %s".format(
                    if (device.storageTotalBytes > 0) DeviceInfo.formatBytes(device.storageFreeBytes) else "n/a",
                    if (device.storageTotalBytes > 0) DeviceInfo.formatBytes(device.storageTotalBytes) else "n/a"
                ),
                "Uptime: ${DeviceInfo.uptime()}",
                "",
                "WoodOS runs no background services. The optional notification",
                "listener only runs while the system binds it, and clocks",
                "update on the system TIME_TICK broadcast.",
            ).joinToString("\n")
            textSize = 13f
            setLineSpacing(dp(3).toFloat(), 1f)
            setTextColor(colors.text)
        }
        list.addView(perfText)
    }

    private fun updatePerf() {
        val info = PerformanceMonitor.report(this)
        perfText?.text = listOf(
            "WoodOS Java heap: %.1f MB (max %.0f MB)".format(info.javaHeapUsedMb, info.javaHeapMaxMb),
            "WoodOS native heap: %.1f MB".format(info.nativeHeapMb),
            "WoodOS total memory (approx. PSS): %.1f MB".format(PerformanceMonitor.approximatePssMb(this)),
            "Device RAM: %.0f MB total, %.0f MB available".format(info.deviceTotalRamMb, info.deviceAvailableRamMb),
            "Uptime: ${DeviceInfo.uptime()}",
        ).joinToString("\n")
    }

    // ------------------------------------------------------------------ about

    private fun buildAbout() {
        header("About")
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrDefault("1.0.0")
        val tv = TextView(this)
        tv.text = listOf(
            "WoodOS $version",
            "",
            "A lightweight, minimalist Android tablet experience with",
            "WoodDesk, a custom desktop environment.",
            "",
            "WoodOS is a launcher app. It does not replace Android firmware",
            "or modify protected system functions, and it needs no root",
            "access. Everything it does uses public Android APIs.",
            "",
            "Licensed under the MIT License. See the project repository",
            "for source code and build instructions.",
        ).joinToString("\n")
        tv.textSize = 13f
        tv.setLineSpacing(dp(3).toFloat(), 1f)
        tv.setTextColor(colors.text)
        tv.setPadding(0, dp(10), 0, dp(10))
        list.addView(tv)
    }

    // ------------------------------------------------------------------ misc

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private const val REQ_WALLPAPER = 1001
    }
}
