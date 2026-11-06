package com.woodos.core

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView

/**
 * App icon view that clips its drawable to a circle / rounded square /
 * square depending on the user preference. Uses ViewOutlineProvider
 * (hardware-accelerated) instead of bitmap masks, so no extra bitmaps
 * are allocated per icon.
 */
class IconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ImageView(context, attrs, defStyleAttr) {

    var shape: String = "rounded"
        set(value) {
            field = value
            applyOutline()
        }

    init {
        clipToOutline = true
        applyOutline()
    }

    private fun applyOutline() {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val w = view.width
                val h = view.height
                if (w == 0 || h == 0) return
                when (shape) {
                    "circle" -> outline.setOval(0, 0, w, h)
                    "square" -> outline.setRect(0, 0, w, h)
                    else -> outline.setRoundRect(0, 0, w, h, w * 0.24f)
                }
            }
        }
        // Re-apply when size changes.
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyOutline() }
    }
}
