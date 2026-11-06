package com.woodos.home

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
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
import com.woodos.core.Prefs
import com.woodos.core.Palette.ThemeColors
import com.woodos.core.Wallpapers
import com.woodos.notifications.WoodNotificationListener
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

/**
 * WoodOS Home - the launcher home screen. Minimal by design: a clock,
 * status chips, favorites and an app drawer. No services, no polling
 * loops (the clock ticks on the system TIME_TICK broadcast only).
 */
class HomeActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var colors: ThemeColors

    private var apps: List<AppEntry> = emptyList()
    private val favorites = mutableListOf<FavItem>()
    private var drawerOpen = false
    private var folderSeed: AppEntry? = null
    private var dragFrom = -1
    private var appliedTheme = "dark"

    private lateinit var root: android.widget.FrameLayout
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var logoIcon: ImageView
    private lateinit var statusChips: LinearLayout
    private lateinit var favGrid: GridView
    private lateinit var favAdapter: FavoritesAdapter
    private lateinit var emptyFavorites: TextView
    private lateinit var drawerPanel: LinearLayout
    private lateinit var drawerGrid: GridView
    private lateinit var drawerAdapter: DrawerAdapter
    private lateinit var drawerSearch: android.widget.EditText
    private lateinit var recentLabel: TextView
    private lateinit var recentScroll: android.widget.HorizontalScrollView
    private lateinit var recentRow: LinearLayout
    private lateinit var trashTarget: View

    private val timeTick = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            updateClock()
            refreshStatus()
        }
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        repo = AppRepository(this)
        deviceInfo = DeviceInfo(this)
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        root = findViewById(R.id.homeRoot)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        logoIcon = findViewById(R.id.logoIcon)
        statusChips = findViewById(R.id.statusChips)
        favGrid = findViewById(R.id.favGrid)
        emptyFavorites = findViewById(R.id.emptyFavorites)
        drawerPanel = findViewById(R.id.drawerPanel)
        drawerGrid = findViewById(R.id.drawerGrid)
        drawerSearch = findViewById(R.id.drawerSearch)
        recentLabel = findViewById(R.id.recentLabel)
        recentScroll = findViewById(R.id.recentScroll)
        recentRow = findViewById(R.id.recentRow)
        trashTarget = findViewById(R.id.trashTarget)

        colors = Palette.colors(prefs, this)
        Wallpapers.apply(root, prefs, this)

        setupBar()
        setupFavorites()
        setupDrawer()
        setupChips()
        updateClock()

        // Size the drawer after the first layout pass.
        root.post { sizeDrawer() }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.theme != appliedTheme) {
            // The activity theme changed in Settings - restart to apply it.
            appliedTheme = prefs.theme
            recreate()
            return
        }
        registerReceiver(timeTick, IntentFilter(Intent.ACTION_TIME_TICK))
        reload()
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(timeTick) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Coming "home" again simply closes the drawer.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) closeDrawer()
    }

    override fun onBackPressed() {
        if (drawerOpen) closeDrawer() else super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-apply theme-dependent colors; layout adapts automatically.
        colors = Palette.colors(prefs, this)
        applyColors()
        root.post { sizeDrawer() }
    }

    // ------------------------------------------------------------------ theme

    private fun applyTheme() {
        appliedTheme = prefs.theme
        val c = Palette.colors(prefs, this)
        setTheme(if (c.isDark) R.style.Theme_WoodOS else R.style.Theme_WoodOS_Light)
    }

    private fun applyColors() {
        timeText.setTextColor(colors.text)
        dateText.setTextColor(colors.textDim)
        logoIcon.setColorFilter(prefs.accent)
        (drawerSearch as TextView).setTextColor(colors.text)
        emptyFavorites.setTextColor(colors.textDim)
    }

    // ------------------------------------------------------------------ data

    private fun reload() {
        colors = Palette.colors(prefs, this)
        applyColors()
        Wallpapers.apply(root, prefs, this)
        apps = repo.loadApps(prefs, force = true)
        favorites.clear()
        favorites.addAll(FavItem.fromJson(prefs.favoritesJson) { key -> apps.firstOrNull { it.key == key } })
        favAdapter.notifyDataSetChanged()
        emptyFavorites.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        filterDrawer()
        buildRecents()
        refreshStatus()
    }

    private fun persistFavorites() {
        prefs.favoritesJson = FavItem.toJson(favorites)
    }

    private fun recordRecent(entry: AppEntry) {
        val recents = prefs.stringList("recents_json").toMutableList()
        recents.removeAll { it == entry.key }
        recents.add(0, entry.key)
        while (recents.size > 6) recents.removeAt(recents.size - 1)
        prefs.putStringList("recents_json", recents)
    }

    private fun launchApp(entry: AppEntry) {
        recordRecent(entry)
        runCatching {
            startActivity(entry.launchIntent())
        }.onFailure {
            Toast.makeText(this, "Could not open app", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------------ bottom bar

    private fun setupBar() {
        findViewById<View>(R.id.appsBtn).setOnClickListener { toggleDrawer() }
        findViewById<View>(R.id.deskBtn).setOnClickListener {
            startActivity(Intent(this, com.woodos.desk.WoodDeskActivity::class.java))
        }
        findViewById<View>(R.id.settingsBtn).setOnClickListener {
            startActivity(Intent(this, com.woodos.settings.SettingsActivity::class.java))
        }
    }

    // ------------------------------------------------------------------ clock & status

    private fun updateClock() {
        val now = Date()
        timeText.text = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
    }

    private fun setupChips() {
        statusChips.removeAllViews()
        statusChips.addView(chip(null, null) { openWifiPanel() }.apply { tag = "wifi" })
        statusChips.addView(chip(null, null) { }.apply { tag = "battery" })
        statusChips.addView(chip(null, null) { }.apply { tag = "storage" })
        statusChips.addView(chip(null, null) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }.apply { tag = "notif" })
    }

    /** Creates a status chip. [icon] and [text] are set later by refreshStatus(). */
    private fun chip(icon: Int?, text: String?, onClick: () -> Unit): TextView {
        val tv = TextView(this)
        tv.textSize = 12f
        tv.setPadding(dp(14), dp(7), dp(14), dp(7))
        tv.background = getDrawable(R.drawable.bg_chip)
        tv.setOnClickListener { onClick() }
        text?.let { tv.text = it }
        icon?.let { tv.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0) }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(8)
        tv.layoutParams = lp
        return tv
    }

    private fun openWifiPanel() {
        val intent = if (Build.VERSION.SDK_INT >= 29) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        runCatching { startActivity(intent) }
    }

    private fun refreshStatus() {
        val s = deviceInfo.snapshot()
        chipText("wifi", if (s.wifiConnected) "Wi-Fi" else "Off", if (s.wifiConnected) R.drawable.ic_wifi else R.drawable.ic_wifi_off)
        chipText("battery", if (s.batteryPercent >= 0) "Battery ${s.batteryPercent}%" else "Battery", R.drawable.ic_battery)
        chipText(
            "storage",
            if (s.storageTotalBytes > 0)
                "Free ${DeviceInfo.formatBytes(s.storageFreeBytes)}" else "Storage",
            R.drawable.ic_storage
        )
        val notifCount = WoodNotificationListener.countOrNull(this)
        chipText("notif", notifCount?.let { "$it" } ?: "Alerts", R.drawable.ic_bell)
    }

    private fun chipText(tag: String, text: String, icon: Int) {
        val v = statusChips.findViewWithTag<TextView>(tag) ?: return
        v.text = text
        v.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        v.compoundDrawablePadding = dp(6)
    }

    // ------------------------------------------------------------------ favorites

    private fun setupFavorites() {
        favAdapter = FavoritesAdapter()
        favGrid.adapter = favAdapter
        favGrid.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (val item = favorites.getOrNull(position)) {
                is FavItem.App -> launchApp(item.entry)
                is FavItem.Folder -> showFolderPopup(item)
                null -> {}
            }
        }
        favGrid.setOnItemLongClickListener { _, view, position, _ ->
            if (position in favorites.indices) {
                dragFrom = position
                trashTarget.visibility = View.VISIBLE
                view.startDragAndDrop(
                    ClipData.newPlainText("woodos-fav", position.toString()),
                    View.DragShadowBuilder(view),
                    null,
                    0
                )
            }
            true
        }
        favGrid.setOnDragListener(dragListener)
        trashTarget.setOnDragListener(trashListener)
    }

    private val dragListener = View.OnDragListener { _, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED ->
                event.clipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
            DragEvent.ACTION_DRAG_LOCATION -> {
                val pos = favGrid.pointToPosition(event.x.toInt(), event.y.toInt())
                if (pos != AdapterView.INVALID_POSITION && pos != dragFrom && dragFrom in favorites.indices && pos in favorites.indices) {
                    Collections.swap(favorites, dragFrom, pos)
                    dragFrom = pos
                    favAdapter.notifyDataSetChanged()
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                trashTarget.visibility = View.GONE
                persistFavorites()
                emptyFavorites.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
                true
            }
            DragEvent.ACTION_DROP -> {
                persistFavorites()
                true
            }
            else -> false
        }
    }

    private val trashListener = View.OnDragListener { v, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                v.alpha = 1f
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                v.alpha = 0.6f
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                v.alpha = 0.6f
                true
            }
            DragEvent.ACTION_DROP -> {
                if (dragFrom in favorites.indices) {
                    favorites.removeAt(dragFrom)
                    persistFavorites()
                    favAdapter.notifyDataSetChanged()
                }
                true
            }
            else -> false
        }
    }

    private fun showFolderPopup(folder: FavItem.Folder) {
        val grid = GridView(this).apply {
            numColumns = 4
            adapter = object : BaseAdapter() {
                override fun getCount() = folder.items.size
                override fun getItem(i: Int) = folder.items[i]
                override fun getItemId(i: Int) = i.toLong()
                override fun getView(i: Int, cv: View?, p: ViewGroup?): View =
                    appIconView(folder.items[i], allowLongPress = true) {
                        // long press removes from folder
                        folder.items.removeAt(i)
                        persistFavorites()
                        reload()
                    }
            }
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setView(grid)
            .setPositiveButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.menu_rename_folder) { _, _ -> renameFolder(folder) }
            .setNegativeButton(R.string.menu_delete_folder) { _, _ ->
                favorites.removeAll { it is FavItem.Folder && it.id == folder.id }
                persistFavorites()
                reload()
            }
            .show()
    }

    private fun renameFolder(folder: FavItem.Folder) {
        val input = android.widget.EditText(this).apply { setText(folder.name) }
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.menu_rename_folder)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                folder.name = input.text.toString().ifBlank { "Folder" }
                persistFavorites()
                reload()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ------------------------------------------------------------------ drawer

    private fun sizeDrawer() {
        val h = (root.height * 0.78f).toInt().coerceAtLeast(dp(320))
        drawerPanel.layoutParams.height = h
        drawerPanel.translationY = if (drawerOpen) 0f else h.toFloat()
        drawerPanel.requestLayout()
    }

    private fun setupDrawer() {
        drawerAdapter = DrawerAdapter()
        drawerGrid.adapter = drawerAdapter
        drawerSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { filterDrawer() }
        })
        drawerGrid.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val entry = drawerAdapter.items.getOrNull(position) ?: return@OnItemClickListener
            // Second tap of the "create folder" flow?
            folderSeed?.let { seed ->
                if (seed.key != entry.key) {
                    favorites.add(
                        FavItem.Folder(
                            FavItem.newFolderId(),
                            "Folder",
                            mutableListOf(seed, entry)
                        )
                    )
                    persistFavorites()
                    reload()
                    Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show()
                }
                folderSeed = null
                return@OnItemClickListener
            }
            launchApp(entry)
        }
        drawerGrid.setOnItemLongClickListener { _, _, position, _ ->
            drawerAdapter.items.getOrNull(position)?.let { showAppMenu(it) }
            true
        }
        drawerPanel.setOnClickListener { /* swallow taps so the panel doesn't leak them */ }
    }

    private fun filterDrawer() {
        val q = drawerSearch.text?.toString()?.trim().orEmpty().lowercase()
        drawerAdapter.items = if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) }
        drawerAdapter.notifyDataSetChanged()
    }

    private fun showAppMenu(entry: AppEntry) {
        val popup = android.widget.PopupMenu(this, drawerGrid)
        popup.menu.add(0, 1, 0, R.string.menu_add_favorite)
        popup.menu.add(0, 2, 1, R.string.menu_create_folder)
        popup.menu.add(0, 3, 2, R.string.menu_hide_app)
        popup.menu.add(0, 4, 3, R.string.menu_app_info)
        popup.setOnMenuItemClickListener { mi ->
            when (mi.itemId) {
                1 -> {
                    if (favorites.none { it is FavItem.App && it.entry.key == entry.key }) {
                        favorites.add(FavItem.App(entry))
                        persistFavorites()
                        reload()
                    } else {
                        Toast.makeText(this, "Already in favorites", Toast.LENGTH_SHORT).show()
                    }
                }
                2 -> {
                    folderSeed = entry
                    Toast.makeText(this, "Now tap a second app to create the folder", Toast.LENGTH_LONG).show()
                }
                3 -> {
                    val hidden = prefs.stringList("hidden_json").toMutableSet()
                    hidden.add(entry.component.packageName)
                    prefs.putStringList("hidden_json", hidden.toList())
                    reload()
                }
                4 -> runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${entry.component.packageName}")
                        )
                    )
                }
            }
            true
        }
        popup.show()
    }

    private fun toggleDrawer() {
        if (drawerOpen) closeDrawer() else openDrawer()
    }

    private fun openDrawer() {
        drawerOpen = true
        filterDrawer()
        drawerPanel.animate().translationY(0f).setDuration(180).start()
    }

    private fun closeDrawer() {
        drawerOpen = false
        folderSeed = null
        drawerPanel.animate()
            .translationY(drawerPanel.height.toFloat())
            .setDuration(180)
            .start()
    }

    // ------------------------------------------------------------------ recents

    private fun buildRecents() {
        recentRow.removeAllViews()
        val recents = prefs.stringList("recents_json")
            .mapNotNull { key -> apps.firstOrNull { it.key == key } }
        recentLabel.visibility = if (recents.isEmpty()) View.GONE else View.VISIBLE
        recentScroll.visibility = recentLabel.visibility
        recents.forEach { entry ->
            val v = appIconView(entry, small = true, allowLongPress = false)
            recentRow.addView(v)
        }
    }

    // ------------------------------------------------------------------ shared views

    /**
     * Builds one app icon cell (icon + label). Used by the drawer, the
     * recents row and folder popups.
     */
    private fun appIconView(
        entry: AppEntry,
        small: Boolean = false,
        allowLongPress: Boolean = false,
        onLongPress: (() -> Unit)? = null,
    ): View {
        val size = if (small) dp(40) else dp(prefs.iconSizeDp)
        val cell = LinearLayout(this)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER_HORIZONTAL
        val pad = if (small) dp(4) else dp(6)
        cell.setPadding(pad, pad, pad, pad)

        val icon = IconView(this)
        icon.layoutParams = LinearLayout.LayoutParams(size, size)
        icon.shape = prefs.iconShape
        repo.iconFor(entry)?.let { icon.setImageDrawable(it) }
        cell.addView(icon)

        if (!small) {
            val label = TextView(this)
            label.text = entry.label
            label.textSize = 11f
            label.setTextColor(colors.text)
            label.gravity = Gravity.CENTER
            label.maxWidth = dp(84)
            label.ellipsize = android.text.TextUtils.TruncateAt.END
            label.maxLines = 1
            label.setPadding(0, dp(5), 0, 0)
            cell.addView(label)
        }

        cell.setOnClickListener { launchApp(entry) }
        if (allowLongPress && onLongPress != null) {
            cell.setOnLongClickListener {
                onLongPress()
                true
            }
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(8)
        cell.layoutParams = lp
        return cell
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    // ------------------------------------------------------------------ adapters

    inner class FavoritesAdapter : BaseAdapter() {
        override fun getCount() = favorites.size
        override fun getItem(i: Int) = favorites[i]
        override fun getItemId(i: Int) = i.toLong()

        override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.view_app_icon, parent, false)
            val icon = view.findViewById<IconView>(R.id.icon)
            val label = view.findViewById<TextView>(R.id.label)
            icon.layoutParams.width = dp(prefs.iconSizeDp)
            icon.layoutParams.height = dp(prefs.iconSizeDp)
            icon.shape = prefs.iconShape
            when (val item = favorites[i]) {
                is FavItem.App -> {
                    repo.iconFor(item.entry)?.let { icon.setImageDrawable(it) }
                    label.text = item.entry.label
                }
                is FavItem.Folder -> {
                    icon.setImageResource(R.drawable.ic_folder)
                    icon.setColorFilter(prefs.accent)
                    label.text = item.name
                }
            }
            label.setTextColor(colors.text)
            return view
        }
    }

    inner class DrawerAdapter : BaseAdapter() {
        var items: List<AppEntry> = emptyList()
        override fun getCount() = items.size
        override fun getItem(i: Int) = items[i]
        override fun getItemId(i: Int) = i.toLong()

        override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.view_app_icon, parent, false)
            val icon = view.findViewById<IconView>(R.id.icon)
            val label = view.findViewById<TextView>(R.id.label)
            val entry = items[i]
            icon.layoutParams.width = dp(prefs.iconSizeDp)
            icon.layoutParams.height = dp(prefs.iconSizeDp)
            icon.shape = prefs.iconShape
            repo.iconFor(entry)?.let { icon.setImageDrawable(it) }
            label.text = entry.label
            label.setTextColor(colors.text)
            return view
        }
    }
}
