package com.woodos.desk

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.woodos.R
import com.woodos.apps.AppEntry
import com.woodos.apps.AppRepository
import com.woodos.core.IconView
import com.woodos.core.Palette
import com.woodos.core.Prefs

/**
 * The WoodDesk start menu: search, favorites, all apps, and quick actions.
 * Shown/hidden by WoodDeskActivity.
 */
class StartMenu(
    private val context: Context,
    private val prefs: Prefs,
    private val repo: AppRepository,
    private val onLaunch: (AppEntry) -> Unit,
    private val onAction: (String) -> Unit,
) {

    val view: View
    private val search: EditText
    private val grid: GridView
    private val favoritesRow: LinearLayout
    private var apps: List<AppEntry> = emptyList()
    private var gridAdapter: GridAdapter

    init {
        view = View.inflate(context, R.layout.view_start_menu, null)
        search = view.findViewById(R.id.menuSearch)
        grid = view.findViewById(R.id.menuGrid)
        favoritesRow = view.findViewById(R.id.menuFavoritesRow)
        gridAdapter = GridAdapter()
        grid.adapter = gridAdapter

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
        })

        grid.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            gridAdapter.items.getOrNull(position)?.let { onLaunch(it) }
        }

        view.findViewById<View>(R.id.menuSettingsBtn).setOnClickListener { onAction("settings") }
        view.findViewById<View>(R.id.menuSysInfoBtn).setOnClickListener { onAction("sysinfo") }
        view.findViewById<View>(R.id.menuPowerBtn).setOnClickListener { onAction("power") }
    }

    fun refresh(apps: List<AppEntry>, favoriteKeys: List<String>) {
        this.apps = apps
        val favorites = favoriteKeys.mapNotNull { key -> apps.firstOrNull { it.key == key } }
        favoritesRow.removeAllViews()
        favorites.forEach { entry -> favoritesRow.addView(favoriteCell(entry)) }
        filter(search.text?.toString().orEmpty())
    }

    fun focusSearch() {
        search.requestFocus()
    }

    private fun favoriteCell(entry: AppEntry): View {
        val colors = Palette.colors(prefs, context)
        val cell = LinearLayout(context)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER_HORIZONTAL
        cell.setPadding(0, 0, 0, 0)
        val icon = IconView(context)
        icon.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        icon.shape = prefs.iconShape
        repo.iconFor(entry)?.let { icon.setImageDrawable(it) }
        cell.addView(icon)
        val label = TextView(context)
        label.text = entry.label
        label.textSize = 10f
        label.setTextColor(colors.textDim)
        label.maxWidth = dp(72)
        label.maxLines = 1
        label.ellipsize = android.text.TextUtils.TruncateAt.END
        label.gravity = Gravity.CENTER
        cell.addView(label)
        cell.setOnClickListener { onLaunch(entry) }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(14)
        cell.layoutParams = lp
        return cell
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        gridAdapter.items = if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) }
        gridAdapter.notifyDataSetChanged()
    }

    private inner class GridAdapter : BaseAdapter() {
        var items: List<AppEntry> = emptyList()
        override fun getCount() = items.size
        override fun getItem(i: Int) = items[i]
        override fun getItemId(i: Int) = i.toLong()

        override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
            val colors = Palette.colors(prefs, context)
            val view = convertView ?: LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(2), dp(8), dp(2), dp(8))
            }
            val cell = view as LinearLayout
            if (cell.childCount == 0) {
                val icon = IconView(context)
                icon.layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                cell.addView(icon)
                val label = TextView(context)
                label.textSize = 10f
                label.gravity = Gravity.CENTER
                label.maxWidth = dp(84)
                label.maxLines = 1
                label.ellipsize = android.text.TextUtils.TruncateAt.END
                label.setPadding(0, dp(6), 0, 0)
                cell.addView(label)
            }
            val entry = items[i]
            (cell.getChildAt(0) as IconView).apply {
                shape = prefs.iconShape
                repo.iconFor(entry)?.let { setImageDrawable(it) }
            }
            (cell.getChildAt(1) as TextView).apply {
                text = entry.label
                setTextColor(colors.text)
            }
            return cell
        }
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density + 0.5f).toInt()
}
