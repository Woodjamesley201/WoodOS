package com.woodos.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import com.woodos.core.Prefs

/** One launchable application. */
data class AppEntry(
    val component: ComponentName,
    val label: String,
    /** Test seam: fixed key for JVM unit tests, where ComponentName is a stub. */
    val keyOverride: String? = null,
) {
    val key: String get() = keyOverride ?: component.flattenToShortString()

    fun launchIntent(): Intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setComponent(this@AppEntry.component)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/**
 * Loads the launchable apps on the device. Icons are cached in a small
 * LruCache keyed by component so scrolling the drawer stays smooth
 * without holding every icon in memory.
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val iconCache = LruCache<String, Drawable>(64)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var cachedApps: List<AppEntry> = emptyList()
    @Volatile private var cachedAt: Long = 0

    fun iconFor(entry: AppEntry): Drawable? {
        iconCache.get(entry.key)?.let { return it }
        return runCatching {
            pm.getActivityIcon(entry.component)
        }.getOrNull()?.also { iconCache.put(entry.key, it) }
    }

    /**
     * Returns visible (not hidden) launchable apps, sorted by label.
     * Result is cached for [CACHE_MS]; the cache is invalidated when the
     * package set changes (PACKAGE_ADDED / REMOVED broadcasts are
     * received by the system and we simply re-query after resume).
     */
    fun loadApps(prefs: Prefs, force: Boolean = false): List<AppEntry> {
        val now = System.currentTimeMillis()
        if (!force && now - cachedAt < CACHE_MS && cachedApps.isNotEmpty()) {
            return cachedApps.filterVisible(prefs)
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = runCatching {
            pm.queryIntentActivities(intent, 0)
                .asSequence()
                .mapNotNull { ri ->
                    val activity = ri.activityInfo ?: return@mapNotNull null
                    val cn = ComponentName(activity.packageName, activity.name)
                    // Never show WoodOS's own activities in the drawer.
                    if (cn.packageName == context.packageName) return@mapNotNull null
                    val label = runCatching { activity.loadLabel(pm).toString() }
                        .getOrDefault(activity.name)
                    AppEntry(cn, label)
                }
                .distinctBy { it.key }
                .sortedBy { it.label.lowercase() }
                .toList()
        }.getOrDefault(emptyList())
        cachedApps = apps
        cachedAt = now
        return apps.filterVisible(prefs)
    }

    private fun List<AppEntry>.filterVisible(prefs: Prefs): List<AppEntry> {
        val hidden = prefs.stringList("hidden_json").toSet()
        return if (hidden.isEmpty()) this else filter { it.component.packageName !in hidden }
    }

    fun find(key: String): AppEntry? =
        loadApps(Prefs(context)).firstOrNull { it.key == key }

    /** Runs [block] on the main thread after [delayMs]. Used for debounced UI refresh. */
    fun postDelayed(delayMs: Long, block: () -> Unit) {
        mainHandler.postDelayed(block, delayMs)
    }

    companion object {
        private const val CACHE_MS = 10_000L
    }
}
