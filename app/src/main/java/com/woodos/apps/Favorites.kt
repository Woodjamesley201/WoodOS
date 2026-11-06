package com.woodos.apps

import org.json.JSONArray
import org.json.JSONObject

/**
 * Model for the user's favorite shortcuts. A favorite is either a single
 * app or a folder containing several apps. Serialized to JSON in Prefs.
 */
sealed class FavItem {
    abstract val label: String

    data class App(val entry: AppEntry) : FavItem() {
        override val label: String get() = entry.label
    }

    data class Folder(
        val id: String,
        var name: String,
        val items: MutableList<AppEntry> = mutableListOf(),
    ) : FavItem() {
        override val label: String get() = name
    }

    companion object {
        fun fromJson(json: String, resolve: (String) -> AppEntry?): List<FavItem> {
            return runCatching {
                val arr = JSONArray(json)
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    when (o.optString("t")) {
                        "a" -> {
                            val entry = resolve(o.optString("c")) ?: return@mapNotNull null
                            App(entry)
                        }
                        "f" -> {
                            val items = o.optJSONArray("i") ?: JSONArray()
                            val apps = (0 until items.length())
                                .mapNotNull { resolve(items.optString(it)) }
                            if (apps.isEmpty()) return@mapNotNull null
                            Folder(o.optString("id"), o.optString("n", "Folder"), apps.toMutableList())
                        }
                        else -> null
                    }
                }
            }.getOrDefault(emptyList())
        }

        fun toJson(items: List<FavItem>): String {
            val arr = JSONArray()
            items.forEach { item ->
                when (item) {
                    is App -> arr.put(JSONObject().put("t", "a").put("c", item.entry.key))
                    is Folder -> arr.put(
                        JSONObject()
                            .put("t", "f")
                            .put("id", item.id)
                            .put("n", item.name)
                            .put("i", JSONArray(item.items.map { it.key }))
                    )
                }
            }
            return arr.toString()
        }

        fun newFolderId(): String = "f" + System.currentTimeMillis().toString(36)
    }
}
