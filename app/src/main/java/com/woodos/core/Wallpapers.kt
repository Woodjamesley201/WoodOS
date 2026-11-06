package com.woodos.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import java.io.InputStream

/**
 * Built-in WoodOS wallpapers are lightweight runtime-drawn gradients:
 * no bitmap assets ship inside the APK, keeping the download small and
 * memory usage low. Users can also pick any image from their gallery.
 */
object Wallpapers {

    data class WoodWallpaper(val id: String, val name: String, val top: Int, val bottom: Int)

    val BUILT_IN = listOf(
        WoodWallpaper("wp_slate", "Slate", 0xFF141419.toInt(), 0xFF26262C.toInt()),
        WoodWallpaper("wp_ember", "Ember", 0xFF15100D.toInt(), 0xFF3A2314.toInt()),
        WoodWallpaper("wp_forest", "Deep Forest", 0xFF0C130E.toInt(), 0xFF1C2E20.toInt()),
        WoodWallpaper("wp_ocean", "Midnight Ocean", 0xFF081018.toInt(), 0xFF10283A.toInt()),
        WoodWallpaper("wp_plum", "Dusk Plum", 0xFF140C16.toInt(), 0xFF2E1828.toInt()),
        WoodWallpaper("wp_wood", "Woodgrain", 0xFF171208.toInt(), 0xFF33271277.toInt()),
    )

    fun byId(id: String): WoodWallpaper = BUILT_IN.firstOrNull { it.id == id } ?: BUILT_IN[0]

    /** Applies the current wallpaper to [view]'s background. */
    fun apply(view: View, prefs: Prefs, context: Context) {
        if (prefs.wallpaperId == "custom" && prefs.wallpaperUri != null) {
            val bmp = decodeSampled(context, prefs.wallpaperUri!!, 1280)
            if (bmp != null) {
                view.background = BitmapDrawable(context.resources, bmp)
                return
            }
            // Fall through to a gradient if the image can no longer be opened.
        }
        val wp = byId(prefs.wallpaperId)
        view.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(wp.top, wp.bottom)
        )
    }

    private fun decodeSampled(context: Context, uriString: String, target: Int): Bitmap? {
        return runCatching {
            val uri = Uri.parse(uriString)
            val (w, h) = bounds(context, uri) ?: return null
            var sample = 1
            while (maxOf(w, h) / (sample * 2) >= target) sample *= 2
            context.contentResolver.openInputStream(uri)?.use { ins: InputStream ->
                BitmapFactory.decodeStream(ins, null, BitmapFactory.Options().apply {
                    inSampleSize = sample
                })
            }
        }.getOrNull()
    }

    private fun bounds(context: Context, uri: Uri): Pair<Int, Int>? {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            Pair(opts.outWidth, opts.outHeight)
        }.getOrNull()
    }

    /** Solid color fallback. */
    fun solid(color: Int): ColorDrawable = ColorDrawable(color)
}
