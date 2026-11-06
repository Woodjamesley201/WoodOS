package dev.woodos.launcher.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Reference to a launchable activity, persisted in preferences. */
data class AppRef(val pkg: String, val cls: String) {
    val key: String get() = "$pkg/$cls"
    override fun toString(): String = key
}

/** A launchable application installed on the device. */
data class AppEntry(
    val ref: AppRef,
    val label: String,
    val isSystem: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
)

object Apps {
    /** All launchable apps, sorted by label. */
    val apps = mutableStateOf<List<AppEntry>>(emptyList())
    private val byKey = HashMap<String, AppEntry>()

    fun initAsync(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            val list = load(context)
            withContext(Dispatchers.Main) {
                apps.value = list
                Prefs.seedDefaults(list)
            }
        }
    }

    fun reload(context: Context) = initAsync(context)

    private fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = try {
            pm.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }
        val out = ArrayList<AppEntry>(results.size)
        for (ri in results) {
            val activity = ri.activityInfo ?: continue
            val pkg = activity.applicationInfo?.packageName ?: activity.packageName ?: continue
            val label = try {
                pm.getApplicationLabel(activity.applicationInfo).toString()
            } catch (_: Exception) {
                activity.name
            }
            val isSystem = try {
                (activity.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            } catch (_: Exception) {
                false
            }
            val (fit, lut) = try {
                val pi = pm.getPackageInfo(pkg, 0)
                pi.firstInstallTime to pi.lastUpdateTime
            } catch (_: Exception) {
                0L to 0L
            }
            val entry = AppEntry(AppRef(pkg, activity.name), label, isSystem, fit, lut)
            out.add(entry)
            byKey[entry.ref.key] = entry
        }
        return out.sortedBy { it.label.lowercase() }
    }

    fun find(ref: AppRef): AppEntry? = byKey[ref.key]

    fun launch(context: Context, entry: AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(entry.ref.pkg, entry.ref.cls))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            RecentTracker.onLaunched(entry.ref)
        } catch (_: Exception) {
            try {
                // Fall back to package-level launch intent
                context.startActivity(
                    context.packageManager.getLaunchIntentForPackage(entry.ref.pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
        }
    }

    fun uninstall(context: Context, entry: AppEntry) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:${entry.ref.pkg}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    fun appInfo(context: Context, entry: AppEntry) {
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${entry.ref.pkg}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }
}

/** Tracks recently used apps (for the WoodOS drawer and WoodDesk recommendations). */
object RecentTracker {
    private const val KEY = "recentApps"
    private val list = ArrayList<AppRef>()

    fun init(prefs: android.content.SharedPreferences) {
        list.clear()
        val raw = prefs.getString(KEY, null) ?: return
        val arr = runCatching { org.json.JSONArray(raw) }.getOrNull() ?: return
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(AppRef(o.optString("p"), o.optString("c")))
        }
    }

    fun onLaunched(ref: AppRef) {
        synchronized(list) {
            list.removeAll { it.key == ref.key }
            list.add(0, ref)
            while (list.size > 20) list.removeAt(list.size - 1)
            persist()
        }
    }

    private fun persist() {
        val sp = Prefs.sharedPreferences ?: return
        val arr = JSONArray()
        synchronized(list) {
            list.take(20).forEach {
                arr.put(org.json.JSONObject().put("p", it.pkg).put("c", it.cls))
            }
        }
        sp.edit().putString(KEY, arr.toString()).apply()
    }

    fun recent(): List<AppRef> = synchronized(list) { ArrayList(list) }
}
