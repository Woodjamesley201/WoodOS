package com.woodos.desk

import android.app.Activity
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.woodos.R
import com.woodos.apps.AppEntry
import com.woodos.apps.AppRepository
import com.woodos.apps.FavItem
import com.woodos.core.DeviceInfo
import com.woodos.core.IconView
import com.woodos.core.Palette
import com.woodos.core.Palette.ThemeColors
import com.woodos.core.Prefs
import com.woodos.core.Wallpapers
import com.woodos.notifications.WoodNotificationListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WoodDesk - the desktop environment. A taskbar, start menu, desktop
 * shortcuts and floating windows for the built-in WoodOS apps. Third-party
 * apps open through Android's own windowing support: if the device
 * supports freeform windows they are launched with a window bounds hint;
 * otherwise they open full-screen (a plain launcher app cannot force
 * arbitrary apps into custom windows).
 */
class WoodDeskActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var colors: ThemeColors

    private lateinit var root: FrameLayout
    private lateinit var deskArea: FrameLayout
    private lateinit var desktopIcons: LinearLayout
    private lateinit var taskbar: LinearLayout
    private lateinit var pinnedRow: LinearLayout
    private lateinit var windowsRow: LinearLayout
    private lateinit var trayWifi: ImageView
    private lateinit var trayBattery: TextView
    private lateinit var trayBell: ImageView
    private lateinit var trayClock: TextView
    private lateinit var startMenuHost: FrameLayout

    private lateinit var manager: DeskWindowManager
    private lateinit var startMenu: StartMenu
    private var apps: List<AppEntry> = emptyList()
    private var favorites: List<FavItem> = emptyList()

    private var presentation: DeskPresentation? = null
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) maybeShowPresentation(displayId)
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (presentation?.display?.displayId == displayId) {
                presentation?.dismiss()
                presentation = null
            }
        }

        override fun onDisplayChanged(displayId: Int) {}
    }

    private val timeTick = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            updateClock()
        }
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        repo = AppRepository(this)
        deviceInfo = DeviceInfo(this)
        setTheme(if (Palette.colors(prefs, this).isDark) R.style.Theme_WoodOS else R.style.Theme_WoodOS_Light)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_desk)

        root = findViewById(R.id.deskRoot)
        deskArea = findViewById(R.id.deskArea)
        desktopIcons = findViewById(R.id.desktopIcons)
        taskbar = findViewById(R.id.taskbar)
        pinnedRow = findViewById(R.id.pinnedRow)
        windowsRow = findViewById(R.id.windowsRow)
        trayWifi = findViewById(R.id.trayWifi)
        trayBattery = findViewById(R.id.trayBattery)
        trayBell = findViewById(R.id.trayBell)
        trayClock = findViewById(R.id.trayClock)
        startMenuHost = FrameLayout(this)
        startMenuHost.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        root.addView(startMenuHost)

        manager = DeskWindowManager(this, deskArea, prefs) { rebuildWindowsRow() }
        startMenu = StartMenu(this, prefs, repo, ::launchExternal, ::onStartMenuAction)
        startMenuHost.addView(startMenu.view)
        startMenu.view.visibility = View.GONE

        findViewById<View>(R.id.startBtn).setOnClickListener { toggleStartMenu() }
        root.setOnTouchListener { _, _ ->
            if (startMenu.view.visibility == View.VISIBLE) {
                hideStartMenu()
                true
            } else false
        }

        (getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.registerDisplayListener(displayListener, null)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(timeTick, IntentFilter(Intent.ACTION_TIME_TICK))
        reload()
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(timeTick) }
    }

    override fun onDestroy() {
        (getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.unregisterDisplayListener(displayListener)
        presentation?.dismiss()
        super.onDestroy()
    }

    override fun onBackPressed() {
        when {
            startMenu.view.visibility == View.VISIBLE -> hideStartMenu()
            manager.activeWindow != null -> manager.closeActive()
            else -> super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        colors = Palette.colors(prefs, this)
        reload()
    }

    // ------------------------------------------------------------------ keyboard shortcuts

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_TAB ->
                    if (event.isAltPressed) {
                        manager.cycle(); return true
                    }
                KeyEvent.KEYCODE_ESCAPE ->
                    if (startMenu.view.visibility == View.VISIBLE) {
                        hideStartMenu(); return true
                    }
                KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT, KeyEvent.KEYCODE_BUTTON_START -> {
                    toggleStartMenu(); return true
                }
                KeyEvent.KEYCODE_F4 ->
                    if (event.isAltPressed) {
                        manager.closeActive(); return true
                    }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ------------------------------------------------------------------ data & UI

    private fun reload() {
        colors = Palette.colors(prefs, this)
        Wallpapers.apply(root, prefs, this)
        apps = repo.loadApps(prefs, force = true)
        favorites = FavItem.fromJson(prefs.favoritesJson) { key -> apps.firstOrNull { it.key == key } }
        buildDesktopIcons()
        buildPinnedRow()
        rebuildWindowsRow()
        startMenu.refresh(apps, favorites.filterIsInstance<FavItem.App>().map { it.entry.key })
        updateClock()
        refreshTray()
        maybeShowPresentation()
    }

    private fun buildDesktopIcons() {
        desktopIcons.removeAllViews()
        favorites.take(12).forEach { item ->
            when (item) {
                is FavItem.App -> desktopIcons.addView(deskIcon(
                    label = item.entry.label,
                    drawable = { icon -> repo.iconFor(item.entry)?.let { icon.setImageDrawable(it) } },
                    onClick = { launchExternal(item.entry) },
                    onLongPress = { removeFavorite(item) },
                ))
                is FavItem.Folder -> desktopIcons.addView(deskIcon(
                    label = item.name,
                    drawable = { icon ->
                        icon.setImageResource(R.drawable.ic_folder)
                        icon.setColorFilter(prefs.accent)
                    },
                    onClick = { showFolderWindow(item) },
                    onLongPress = { removeFavorite(item) },
                ))
            }
        }
    }

    private fun deskIcon(
        label: String,
        drawable: (IconView) -> Unit,
        onClick: () -> Unit,
        onLongPress: () -> Unit,
    ): View {
        val colors = Palette.colors(prefs, this)
        val cell = LinearLayout(this)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER_HORIZONTAL
        cell.setPadding(dp(6), dp(8), dp(6), dp(8))
        val icon = IconView(this)
        icon.layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        icon.shape = prefs.iconShape
        drawable(icon)
        cell.addView(icon)
        val tv = TextView(this)
        tv.text = label
        tv.textSize = 11f
        tv.setTextColor(colors.text)
        tv.maxWidth = dp(92)
        tv.maxLines = 1
        tv.ellipsize = android.text.TextUtils.TruncateAt.END
        tv.gravity = Gravity.CENTER
        tv.setPadding(0, dp(6), 0, 0)
        cell.addView(tv)
        cell.setOnClickListener { onClick() }
        cell.setOnLongClickListener {
            onLongPress()
            true
        }
        return cell
    }

    private fun removeFavorite(item: FavItem) {
        val mutable = favorites.toMutableList()
        mutable.removeAll { (it is FavItem.App && it.entry.key == (item as? FavItem.App)?.entry?.key) || it === item }
        prefs.favoritesJson = FavItem.toJson(mutable)
        reload()
    }

    private fun buildPinnedRow() {
        pinnedRow.removeAllViews()
        // Built-in WoodOS apps are always pinned.
        pinnedApps().forEach { (title, iconRes, open) ->
            pinnedRow.addView(pinnedButton(iconRes) {
                manager.addWindow(title, iconRes, open())
            })
        }
        // Followed by the first few favorite apps.
        favorites.filterIsInstance<FavItem.App>().take(3).forEach { fav ->
            val iconView = IconView(this)
            iconView.layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            iconView.shape = prefs.iconShape
            repo.iconFor(fav.entry)?.let { iconView.setImageDrawable(it) }
            stylePinned(iconView)
            iconView.setOnClickListener { launchExternal(fav.entry) }
            pinnedRow.addView(iconView)
        }
    }

    private fun pinnedApps(): List<Triple<String, Int, () -> View>> = listOf(
        Triple("Notes", R.drawable.ic_notes) { DeskApps.notesApp(this, prefs) },
        Triple("Calculator", R.drawable.ic_calc) { DeskApps.calculatorApp(this, prefs) },
        Triple("Files", R.drawable.ic_files) { DeskApps.filesApp(this, prefs) },
        Triple("System info", R.drawable.ic_info) { DeskApps.systemInfoApp(this, prefs) },
    )

    private fun pinnedButton(iconRes: Int, onClick: () -> Unit): ImageView {
        val iv = ImageView(this)
        iv.layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
        iv.setImageResource(iconRes)
        stylePinned(iv)
        iv.setOnClickListener { onClick() }
        return iv
    }

    private fun stylePinned(v: View) {
        v.setPadding(dp(6), dp(6), dp(6), dp(6))
        v.background = getDrawable(R.drawable.bg_pill)
        val lp = LinearLayout.LayoutParams(dp(42), dp(42))
        lp.marginEnd = dp(6)
        v.layoutParams = lp
    }

    private fun rebuildWindowsRow() {
        windowsRow.removeAllViews()
        manager.windows.forEach { win ->
            val btn = TextView(this)
            btn.text = win.title
            btn.textSize = 12f
            btn.setTextColor(if (win === manager.activeWindow && !win.isMinimized) prefs.accent else colors.text)
            btn.maxLines = 1
            btn.ellipsize = android.text.TextUtils.TruncateAt.END
            btn.setPadding(dp(10), dp(6), dp(10), dp(6))
            btn.background = getDrawable(R.drawable.bg_pill)
            btn.setOnClickListener { manager.toggle(win) }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = dp(6)
            btn.layoutParams = lp
            windowsRow.addView(btn)
        }
    }

    // ------------------------------------------------------------------ start menu

    private fun toggleStartMenu() {
        if (startMenu.view.visibility == View.VISIBLE) hideStartMenu() else showStartMenu()
    }

    private fun showStartMenu() {
        startMenu.refresh(apps, favorites.filterIsInstance<FavItem.App>().map { it.entry.key })
        startMenu.view.alpha = 0f
        startMenu.view.translationY = dp(24).toFloat()
        startMenu.view.visibility = View.VISIBLE
        startMenu.view.animate().alpha(1f).translationY(0f).setDuration(140).start()
    }

    private fun hideStartMenu() {
        startMenu.view.animate()
            .alpha(0f).translationY(dp(24).toFloat()).setDuration(120)
            .withEndAction { startMenu.view.visibility = View.GONE }
            .start()
    }

    private fun onStartMenuAction(action: String) {
        hideStartMenu()
        when (action) {
            "settings" -> startActivity(Intent(this, com.woodos.settings.SettingsActivity::class.java))
            "sysinfo" -> manager.addWindow(
                getString(R.string.app_sysinfo), R.drawable.ic_info,
                DeskApps.systemInfoApp(this, prefs), 420, 460
            )
            "power" -> showPowerDialog()
        }
    }

    private fun showPowerDialog() {
        val options = arrayOf(
            getString(R.string.start_power_exit),
            getString(R.string.start_power_home),
            getString(R.string.start_power_sys),
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.start_power)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> finish()
                    1 -> {
                        finish()
                        startActivity(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    2 -> runCatching { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
                }
            }
            .setMessage(R.string.power_note)
            .setPositiveButton(android.R.string.cancel, null)
            .show()
    }

    // ------------------------------------------------------------------ launching apps

    /** Launches a third-party app, using freeform windows where supported. */
    private fun launchExternal(entry: AppEntry) {
        hideStartMenu()
        recordRecent(entry)
        val freeform = packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT
        )
        runCatching {
            if (freeform && prefs.deskFreeform) {
                // Ask the system to open the app in a desktop-style window.
                val r = Rect()
                deskArea.getHitRect(r)
                @Suppress("DEPRECATION") // setLaunchBounds: the modern desktop-windowing path
                val opts = ActivityOptions.makeBasic().setLaunchBounds(
                    Rect(dp(120), dp(60), dp(120 + 700), dp(60 + 520))
                )
                startActivity(entry.launchIntent(), opts.toBundle())
            } else {
                startActivity(entry.launchIntent())
                if (!freeform) {
                    Toast.makeText(
                        this,
                        "This device does not support desktop windows for other apps - opening full screen",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.onFailure {
            Toast.makeText(this, "Could not open app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun recordRecent(entry: AppEntry) {
        val recents = prefs.stringList("recents_json").toMutableList()
        recents.removeAll { it == entry.key }
        recents.add(0, entry.key)
        while (recents.size > 6) recents.removeAt(recents.size - 1)
        prefs.putStringList("recents_json", recents)
    }

    private fun showFolderWindow(folder: FavItem.Folder) {
        val grid = GridView(this).apply {
            numColumns = 3
            adapter = object : BaseAdapter() {
                override fun getCount() = folder.items.size
                override fun getItem(i: Int) = folder.items[i]
                override fun getItemId(i: Int) = i.toLong()
                override fun getView(i: Int, cv: View?, p: ViewGroup?): View {
                    val v = cv ?: layoutInflater.inflate(R.layout.view_app_icon, p, false)
                    val icon = v.findViewById<IconView>(R.id.icon)
                    val label = v.findViewById<TextView>(R.id.label)
                    val entry = folder.items[i]
                    icon.shape = prefs.iconShape
                    repo.iconFor(entry)?.let { icon.setImageDrawable(it) }
                    label.text = entry.label
                    label.setTextColor(colors.text)
                    return v
                }
            }
            onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
                folder.items.getOrNull(i)?.let { launchExternal(it) }
            }
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setView(grid)
            .setPositiveButton(android.R.string.cancel, null)
            .show()
    }

    // ------------------------------------------------------------------ tray

    private fun updateClock() {
        trayClock.text = SimpleDateFormat("h:mm  ·  MMM d", Locale.getDefault()).format(Date())
        refreshTray()
    }

    private fun refreshTray() {
        val s = deviceInfo.snapshot()
        trayWifi.setImageResource(if (s.wifiConnected) R.drawable.ic_wifi else R.drawable.ic_wifi_off)
        trayBattery.text = if (s.batteryPercent >= 0) "${s.batteryPercent}%" else "--"
        val notifCount = WoodNotificationListener.countOrNull(this)
        trayBell.setColorFilter(
            if (notifCount != null && notifCount > 0) prefs.accent else colors.textDim
        )
        trayWifi.setColorFilter(if (s.wifiConnected) colors.text else colors.textDim)
    }

    // ------------------------------------------------------------------ external display

    private fun maybeShowPresentation() {
        if (!prefs.deskExtendDisplay) {
            presentation?.dismiss()
            presentation = null
            return
        }
        val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
        val display = dm.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY } ?: return
        if (presentation?.display?.displayId == display.displayId) return
        presentation?.dismiss()
        presentation = runCatching { DeskPresentation(this, display, prefs) }
            .getOrNull()?.also { it.show() }
    }

    private fun maybeShowPresentation(displayId: Int) {
        val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
        dm.getDisplay(displayId)?.let { display ->
            presentation?.dismiss()
            presentation = runCatching { DeskPresentation(this, display, prefs) }
                .getOrNull()?.also { it.show() }
        }
    }

    // ------------------------------------------------------------------ misc

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && startMenu.view.visibility == View.VISIBLE) {
            hideStartMenu()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
}
