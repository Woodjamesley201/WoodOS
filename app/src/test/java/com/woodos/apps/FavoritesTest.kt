package com.woodos.apps

import android.content.ComponentName
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesTest {

    private fun entry(pkg: String, cls: String) =
        AppEntry(ComponentName(pkg, "$pkg.$cls"), "$pkg/$cls", keyOverride = "$pkg/$pkg.$cls")

    @Test fun folderRoundTrip() {
        val items = listOf(
            FavItem.App(entry("com.a", "Main")),
            FavItem.Folder("f1", "Games", mutableListOf(entry("com.b", "Play"), entry("com.c", "Go"))),
        )
        val json = FavItem.toJson(items)
        // Structure check on raw JSON.
        val arr = JSONArray(json)
        assertEquals("a", arr.getJSONObject(0).optString("t"))
        assertEquals("f", arr.getJSONObject(1).optString("t"))
        assertEquals("Games", arr.getJSONObject(1).optString("n"))

        val back = FavItem.fromJson(json) { key ->
            mapOf(
                "com.a/com.a.Main" to entry("com.a", "Main"),
                "com.b/com.b.Play" to entry("com.b", "Play"),
                "com.c/com.c.Go" to entry("com.c", "Go"),
            )[key]
        }
        assertEquals(2, back.size)
        val folder = back[1] as FavItem.Folder
        assertEquals("Games", folder.name)
        assertEquals(2, folder.items.size)
    }

    @Test fun unresolvedEntriesAreDropped() {
        val json = JSONArray()
            .put(JSONObject().put("t", "a").put("c", "com.gone/.Main"))
            .toString()
        val back = FavItem.fromJson(json) { null }
        assertTrue(back.isEmpty())
    }
}
