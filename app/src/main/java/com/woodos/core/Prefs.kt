package com.woodos.core

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single, tiny persistence layer for WoodOS.
 * Everything is stored in one SharedPreferences file; JSON is used only
 * for small lists (favorites, hidden apps, recents) so no database or
 * extra dependency is needed.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("woodos_prefs", Context.MODE_PRIVATE)

    // --- Theme ------------------------------------------------------------
    var theme: String // "dark" | "light" | "system"
        get() = sp.getString(K_THEME, "dark") ?: "dark"
        set(v) = sp.edit().putString(K_THEME, v).apply()

    var accent: Int
        get() = sp.getInt(K_ACCENT, 0xFFE8A15D.toInt())
        set(v) = sp.edit().putInt(K_ACCENT, v).apply()

    // --- Wallpaper ---------------------------------------------------------
    var wallpaperId: String // "wp_xxx" built-in or "custom" + wallpaperUri
        get() = sp.getString(K_WALLPAPER, "wp_slate") ?: "wp_slate"
        set(v) = sp.edit().putString(K_WALLPAPER, v).apply()

    var wallpaperUri: String?
        get() = sp.getString(K_WALLPAPER_URI, null)
        set(v) = sp.edit().putString(K_WALLPAPER_URI, v).apply()

    // --- Icons -------------------------------------------------------------
    var iconShape: String // "circle" | "rounded" | "square"
        get() = sp.getString(K_ICON_SHAPE, "rounded") ?: "rounded"
        set(v) = sp.edit().putString(K_ICON_SHAPE, v).apply()

    var iconSizeDp: Int
        get() = sp.getInt(K_ICON_SIZE, 52)
        set(v) = sp.edit().putInt(K_ICON_SIZE, v).apply()

    // --- Home layout -------------------------------------------------------
    /** JSON array of favorite items (apps and folders). See [com.woodos.apps.Favorites]. */
    var favoritesJson: String
        get() = sp.getString(K_FAVORITES, "[]") ?: "[]"
        set(v) = sp.edit().putString(K_FAVORITES, v).apply()

    /** JSON array of hidden package names. */
    var hiddenJson: String
        get() = sp.getString(K_HIDDEN, "[]") ?: "[]"
        set(v) = sp.edit().putString(K_HIDDEN, v).apply()

    /** JSON array of recently launched component names, most recent first. */
    var recentsJson: String
        get() = sp.getString(K_RECENTS, "[]") ?: "[]"
        set(v) = sp.edit().putString(K_RECENTS, v).apply()

    // --- WoodDesk ----------------------------------------------------------
    /** Launch third-party apps in freeform desktop windows when the device supports it. */
    var deskFreeform: Boolean
        get() = sp.getBoolean(K_DESK_FREEFORM, true)
        set(v) = sp.edit().putBoolean(K_DESK_FREEFORM, v).apply()

    /** Mirror WoodDesk onto an external display when one is connected. */
    var deskExtendDisplay: Boolean
        get() = sp.getBoolean(K_DESK_EXTEND, false)
        set(v) = sp.edit().putBoolean(K_DESK_EXTEND, v).apply()

    fun stringList(key: String): List<String> {
        val arr = sp.getString(key, null) ?: return emptyList()
        return runCatching { JSONArray(arr).let { (0 until it.length()).map { i -> it.optString(i) } } }
            .getOrDefault(emptyList())
    }

    fun putStringList(key: String, values: List<String>) {
        sp.edit().putString(key, JSONArray(values).toString()).apply()
    }

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.unregisterOnSharedPreferenceChangeListener(l)

    companion object {
        private const val K_THEME = "theme"
        private const val K_ACCENT = "accent"
        private const val K_WALLPAPER = "wallpaper_id"
        private const val K_WALLPAPER_URI = "wallpaper_uri"
        private const val K_ICON_SHAPE = "icon_shape"
        private const val K_ICON_SIZE = "icon_size_dp"
        private const val K_FAVORITES = "favorites_json"
        private const val K_HIDDEN = "hidden_json"
        private const val K_RECENTS = "recents_json"
        private const val K_DESK_FREEFORM = "desk_freeform"
        private const val K_DESK_EXTEND = "desk_extend_display"

        /** Keys that require a UI refresh on the home screen / desk. */
        val UI_KEYS = setOf(
            K_THEME, K_ACCENT, K_WALLPAPER, K_WALLPAPER_URI,
            K_ICON_SHAPE, K_ICON_SIZE, K_FAVORITES, K_HIDDEN
        )
    }
}
