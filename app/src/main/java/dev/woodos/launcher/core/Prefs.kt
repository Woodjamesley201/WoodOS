package dev.woodos.launcher.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class WoodMode { WOODOS, WOODDESK }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentColor { WOOD, OCEAN, AURORA, EMBER, VIOLET, TEAL, ROSE }
enum class ClockStyle { MODERN, CLASSIC, MINIMAL }
enum class TaskbarSize { COMPACT, NORMAL, LARGE }

/** Folder shown on the WoodOS home screen. */
data class HomeFolder(val id: String, val name: String, val apps: List<AppRef>)

/** Folder shown on the WoodDesk desktop. */
data class DeskFolder(val id: String, val name: String, val apps: List<AppRef>)

/**
 * Central WoodOS preference store. Backed by SharedPreferences, exposed to
 * Compose via mutableStateOf so every screen reacts instantly to changes.
 */
object Prefs {

    lateinit var sharedPreferences: SharedPreferences
        private set

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("woodos_prefs", Context.MODE_PRIVATE)
        RecentTracker.init(sharedPreferences)

        _mode.raw.value = enum(sharedPreferences.getString("mode", null), WoodMode.WOODOS)
        _themeMode.raw.value = enum(sharedPreferences.getString("themeMode", null), ThemeMode.SYSTEM)
        _accent.raw.value = enum(sharedPreferences.getString("accent", null), AccentColor.WOOD)
        _clockStyle.raw.value = enum(sharedPreferences.getString("clockStyle", null), ClockStyle.MODERN)
        _taskbarSize.raw.value = enum(sharedPreferences.getString("taskbarSize", null), TaskbarSize.NORMAL)
        _wallpaperId.raw.value = sharedPreferences.getInt("wallpaperId", 0)
        _customWallpaperUri.raw.value = sharedPreferences.getString("customWallpaperUri", null)
        _blurIntensity.raw.value = sharedPreferences.getFloat("blurIntensity", 1f)
        _animationScale.raw.value = sharedPreferences.getFloat("animationScale", 1f)
        _transparency.raw.value = sharedPreferences.getFloat("transparency", 0.55f)
        _showWeatherWidget.raw.value = sharedPreferences.getBoolean("showWeatherWidget", true)
        _showSearchBar.raw.value = sharedPreferences.getBoolean("showSearchBar", true)
        _homeColumns.raw.value = sharedPreferences.getInt("homeColumns", 5)
        _weatherCityName.raw.value = sharedPreferences.getString("weatherCityName", "Newark, New Jersey")
        _weatherLat.raw.value = sharedPreferences.getFloat("weatherLat", 40.7357f)
        _weatherLon.raw.value = sharedPreferences.getFloat("weatherLon", -74.1724f)
        _tryFreeform.raw.value = sharedPreferences.getBoolean("tryFreeform", false)
        _rotationLocked.raw.value = sharedPreferences.getBoolean("rotationLocked", false)
        _deskShowIcons.raw.value = sharedPreferences.getBoolean("deskShowIcons", true)
        _darkIcons.raw.value = sharedPreferences.getBoolean("darkIcons", false)
        _dockApps.raw.value = refsFromJson(sharedPreferences.getString("dockApps", null))
        _homeApps.raw.value = refsFromJson(sharedPreferences.getString("homeApps", null))
        _favorites.raw.value = refsFromJson(sharedPreferences.getString("favorites", null))
        _startPins.raw.value = refsFromJson(sharedPreferences.getString("startPins", null))
        _deskShortcuts.raw.value = refsFromJson(sharedPreferences.getString("deskShortcuts", null))
        _homeFolders.raw.value = foldersFromJson(sharedPreferences.getString("homeFolders", null))
        _deskFolders.raw.value = deskFoldersFromJson(sharedPreferences.getString("deskFolders", null))
        _notes.raw.value = notesFromJson(sharedPreferences.getString("notes", null))
    }

    // ---- mode & appearance ----
    private val _mode = Pref(WoodMode.WOODOS, write = {  it.name  }, read = {  enum(it, WoodMode.WOODOS)  })
    var mode by _mode
    private val _themeMode = Pref(ThemeMode.SYSTEM, write = {  it.name  }, read = {  enum(it, ThemeMode.SYSTEM)  })
    var themeMode by _themeMode
    private val _accent = Pref(AccentColor.WOOD, write = {  it.name  }, read = {  enum(it, AccentColor.WOOD)  })
    var accent by _accent
    private val _clockStyle = Pref(ClockStyle.MODERN, write = {  it.name  }, read = {  enum(it, ClockStyle.MODERN)  })
    var clockStyle by _clockStyle
    private val _wallpaperId = Pref(0, write = {  it.toString()  }, read = {  it.toInt()  })
    var wallpaperId by _wallpaperId
    private val _customWallpaperUri = Pref<String?>(null, write = {  it ?: ""  }, read = {  s -> if (s.isNullOrBlank()) null else s  })
    var customWallpaperUri by _customWallpaperUri
    private val _blurIntensity = Pref(1f, write = {  it.toString()  }, read = {  it.toFloat()  })
    var blurIntensity by _blurIntensity
    private val _animationScale = Pref(1f, write = {  it.toString()  }, read = {  it.toFloat()  })
    var animationScale by _animationScale
    private val _transparency = Pref(0.55f, write = {  it.toString()  }, read = {  it.toFloat()  })
    var transparency by _transparency
    private val _darkIcons = Pref(false, write = {  it.toString()  }, read = {  it.toBoolean()  })
    var darkIcons by _darkIcons

    // ---- home screen ----
    private val _showWeatherWidget = Pref(true, write = {  it.toString()  }, read = {  it.toBoolean()  })
    var showWeatherWidget by _showWeatherWidget
    private val _showSearchBar = Pref(true, write = {  it.toString()  }, read = {  it.toBoolean()  })
    var showSearchBar by _showSearchBar
    private val _homeColumns = Pref(5, write = {  it.toString()  }, read = {  it.toInt()  })
    var homeColumns by _homeColumns
    private val _dockApps = Pref<List<AppRef>>(emptyList(), write = {  refsToJson(it)  }, read = {  refsFromJson(it)  })
    var dockApps by _dockApps
    private val _homeApps = Pref<List<AppRef>>(emptyList(), write = {  refsToJson(it)  }, read = {  refsFromJson(it)  })
    var homeApps by _homeApps
    private val _homeFolders = Pref<List<HomeFolder>>(emptyList(), write = {  foldersToJson(it)  }, read = {  foldersFromJson(it)  })
    var homeFolders by _homeFolders
    private val _favorites = Pref<List<AppRef>>(emptyList(), write = {  refsToJson(it)  }, read = {  refsFromJson(it)  })
    var favorites by _favorites

    // ---- WoodDesk ----
    private val _taskbarSize = Pref(TaskbarSize.NORMAL, write = {  it.name  }, read = {  enum(it, TaskbarSize.NORMAL)  })
    var taskbarSize by _taskbarSize
    private val _startPins = Pref<List<AppRef>>(emptyList(), write = {  refsToJson(it)  }, read = {  refsFromJson(it)  })
    var startPins by _startPins
    private val _deskShortcuts = Pref<List<AppRef>>(emptyList(), write = {  refsToJson(it)  }, read = {  refsFromJson(it)  })
    var deskShortcuts by _deskShortcuts
    private val _deskFolders = Pref<List<DeskFolder>>(emptyList(), write = {  deskFoldersToJson(it)  }, read = {  deskFoldersFromJson(it)  })
    var deskFolders by _deskFolders
    private val _deskShowIcons = Pref(true, write = {  it.toString()  }, read = {  it.toBoolean()  })
    var deskShowIcons by _deskShowIcons
    private val _tryFreeform = Pref(false, write = {  it.toString()  }, read = {  it.toBoolean()  })
    var tryFreeform by _tryFreeform
    private val _rotationLocked = Pref(false, write = {  it.toString()  }, read = {  it.toBoolean()  })
    var rotationLocked by _rotationLocked

    // ---- weather ----
    private val _weatherCityName = Pref<String?>("Newark, New Jersey", write = { it ?: "" }, read = { s -> s.ifBlank { null } })
    var weatherCityName by _weatherCityName
    private val _weatherLat = Pref(40.7357f, write = {  it.toString()  }, read = {  it.toFloat()  })
    var weatherLat by _weatherLat
    private val _weatherLon = Pref(-74.1724f, write = {  it.toString()  }, read = {  it.toFloat()  })
    var weatherLon by _weatherLon

    // ---- notes ----
    private val _notes = Pref<List<Note>>(emptyList(), write = {  notesToJson(it)  }, read = {  notesFromJson(it)  })
    var notes by _notes

    fun resetDefaults() {
        sharedPreferences.edit().clear().apply()
    }

    /** First-run defaults, seeded once the installed app list is known. */
    fun seedDefaults(all: List<AppEntry>) {
        if (sharedPreferences.getBoolean("seeded", false)) return
        sharedPreferences.edit().putBoolean("seeded", true).apply()
        val knownDock = listOf(
            "com.android.chrome", "com.google.android.youtube", "com.google.android.gm",
            "com.google.android.apps.photos", "com.spotify.music", "com.whatsapp", "com.discord",
        )
        val dock = knownDock.mapNotNull { k -> all.find { it.ref.pkg == k }?.ref }.toMutableList()
        if (dock.size < 6) {
            dock += all.filter { !it.isSystem && dock.none { d -> d.key == it.ref.key } }.take(6 - dock.size).map { it.ref }
        }
        if (dockApps.isEmpty() && dock.isNotEmpty()) dockApps = dock.take(6)
        if (homeApps.isEmpty()) {
            homeApps = all.filter { a -> dock.none { d -> d.key == a.ref.key } }.take(20).map { it.ref }
        }
        if (favorites.isEmpty()) favorites = dock.take(3)
        if (startPins.isEmpty()) startPins = dock
        if (deskShortcuts.isEmpty()) deskShortcuts = dock.take(4)
    }

    // ---------- helpers ----------

    private inline fun <reified E : Enum<E>> enum(s: String?, def: E): E =
        s?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: def

    class Pref<T>(default: T, private val write: (T) -> String, private val read: (String) -> T) {
        val raw = mutableStateOf(default)
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = raw.value
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            raw.value = value
            sharedPreferences.edit().putString(property.name, write(value)).apply()
        }
    }

    private fun refsToJson(refs: List<AppRef>): String {
        val a = JSONArray()
        refs.forEach { a.put(JSONObject().put("p", it.pkg).put("c", it.cls)) }
        return a.toString()
    }

    private fun refsFromJson(s: String?): List<AppRef> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val a = JSONArray(s)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                AppRef(o.optString("p"), o.optString("c"))
            }
        }.getOrDefault(emptyList())
    }

    private fun foldersToJson(list: List<HomeFolder>): String {
        val a = JSONArray()
        list.forEach {
            val o = JSONObject().put("id", it.id).put("name", it.name)
            o.put("apps", JSONArray().apply { it.apps.forEach { r -> put(JSONObject().put("p", r.pkg).put("c", r.cls)) } })
            a.put(o)
        }
        return a.toString()
    }

    private fun foldersFromJson(s: String?): List<HomeFolder> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val a = JSONArray(s)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                HomeFolder(
                    o.optString("id"), o.optString("name"),
                    refsFromJson(o.optJSONArray("apps")?.toString())
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun deskFoldersToJson(list: List<DeskFolder>): String {
        val a = JSONArray()
        list.forEach {
            val o = JSONObject().put("id", it.id).put("name", it.name)
            o.put("apps", JSONArray().apply { it.apps.forEach { r -> put(JSONObject().put("p", r.pkg).put("c", r.cls)) } })
            a.put(o)
        }
        return a.toString()
    }

    private fun deskFoldersFromJson(s: String?): List<DeskFolder> {
        if (s.isNullOrBlank()) return emptyList()
        return runCatching {
            val a = JSONArray(s)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                DeskFolder(
                    o.optString("id"), o.optString("name"),
                    refsFromJson(o.optJSONArray("apps")?.toString())
                )
            }
        }.getOrDefault(emptyList())
    }
}

data class Note(val id: String, val title: String, val body: String, val updatedAt: Long)

private fun notesToJson(notes: List<Note>): String {
    val a = JSONArray()
    notes.forEach { a.put(JSONObject().put("id", it.id).put("t", it.title).put("b", it.body).put("u", it.updatedAt)) }
    return a.toString()
}

private fun notesFromJson(s: String?): List<Note> {
    if (s.isNullOrBlank()) return emptyList()
    return runCatching {
        val a = JSONArray(s)
        (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            Note(o.optString("id"), o.optString("t"), o.optString("b"), o.optLong("u"))
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())
}
