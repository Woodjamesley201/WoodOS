package dev.woodos.launcher.core

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap

/**
 * LRU cache of app launcher icons as ImageBitmaps, keyed by "pkg/cls".
 * Icons are drawn at the requested pixel size so the cache stays small.
 */
object IconCache {
    private const val CACHE_KB = 8 * 1024 // 8 MB of bitmaps
    private lateinit var pm: PackageManager
    private val cache = object : LruCache<String, ImageBitmap>(CACHE_KB) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4) / 1024
    }

    fun init(context: Context) {
        pm = context.packageManager
    }

    fun iconFor(ref: AppRef, sizePx: Int): ImageBitmap? {
        val key = "${ref.key}@$sizePx"
        cache.get(key)?.let { return it }
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                .setComponent(android.content.ComponentName(ref.pkg, ref.cls))
            val ri = pm.queryIntentActivities(intent, 0).firstOrNull() ?: return null
            val drawable = ri.loadIcon(pm) ?: return null
            val bmp = drawableToBitmap(drawable, sizePx)
            val ib = bmp.asImageBitmap()
            cache.put(key, ib)
            ib
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val b = drawable.bitmap
            if (b.width == sizePx) return b
            return Bitmap.createScaledBitmap(b, sizePx, sizePx, true)
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bmp
    }
}
