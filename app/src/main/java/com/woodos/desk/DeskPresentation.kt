package com.woodos.desk

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.view.Display
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.woodos.core.Prefs
import com.woodos.core.Wallpapers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timerTask

/**
 * Optional WoodDesk presence on an external display (HDMI / DisplayPort /
 * USB-C). Shows wallpaper, the WoodOS mark and a clock. This uses the
 * public [android.app.Presentation] API which works on any Android 5+
 * device - no vendor-specific desktop-mode APIs required.
 */
class DeskPresentation(
    outerContext: Context,
    display: Display,
    private val prefs: Prefs,
) : Presentation(outerContext, display) {

    private var clock: TextView? = null
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(context)
        Wallpapers.apply(root, prefs, context)

        val center = TextView(context)
        center.text = "WoodDesk"
        center.textSize = 34f
        center.setTextColor(0xFFE8A15D.toInt())
        center.gravity = Gravity.CENTER
        center.letterSpacing = 0.15f

        clock = TextView(context)
        clock?.textSize = 16f
        clock?.setTextColor(Color.WHITE)
        clock?.gravity = Gravity.CENTER
        clock?.setPadding(0, 0, 0, 60)

        val column = FrameLayout(context)
        column.addView(
            center,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        column.addView(
            clock,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        root.addView(
            column,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
        tick()
    }

    override fun onStart() {
        super.onStart()
        timer = Timer()
        timer?.scheduleAtFixedRate(timerTask { tick() }, 30_000L, 30_000L)
    }

    override fun onStop() {
        timer?.cancel()
        timer = null
        super.onStop()
    }

    private fun tick() {
        val now = SimpleDateFormat("h:mm  ·  EEEE, MMMM d", Locale.getDefault()).format(Date())
        clock?.post { clock?.text = now }
    }
}
