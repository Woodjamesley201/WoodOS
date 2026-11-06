package com.woodos.desk

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.woodos.R
import com.woodos.core.Palette
import com.woodos.core.Prefs

/**
 * One floating window on the WoodDesk desktop. Windows can be dragged by
 * their title bar, resized from the bottom-right corner, minimized to the
 * taskbar, maximimized to fill the desk area and closed. Everything is
 * plain framework widgets - no window library needed.
 */
class DeskWindow(
    context: Context,
    val id: String,
    val title: String,
    val iconRes: Int,
    content: View,
    private val manager: DeskWindowManager,
    private val accent: Int,
    private val surface: Int,
    private val surfaceAlt: Int,
    private val textColor: Int,
) : FrameLayout(context) {

    private val titleBar: LinearLayout
    private val winTitle: TextView
    private val btnMinimize: ImageView
    private val btnMaximize: ImageView
    private val btnClose: ImageView
    private val resizeHandle: ImageView
    private val contentContainer: FrameLayout

    var isMinimized = false
        private set
    var isMaximized = false
        private set
    private var restoreLeft = 0
    private var restoreTop = 0
    private var restoreWidth = 0
    private var restoreHeight = 0

    init {
        View.inflate(context, R.layout.view_desk_window, this)
        background = GradientDrawable().apply {
            setColor(surface)
            cornerRadius = 18f * resources.displayMetrics.density
            setStroke(1, 0x22FFFFFF)
        }
        elevation = dp(10).toFloat()

        val root = findViewById<FrameLayout>(R.id.windowRoot)
        titleBar = root.findViewById(R.id.titleBar)
        winTitle = root.findViewById(R.id.winTitle)
        btnMinimize = root.findViewById(R.id.btnMinimize)
        btnMaximize = root.findViewById(R.id.btnMaximize)
        btnClose = root.findViewById(R.id.btnClose)
        resizeHandle = root.findViewById(R.id.resizeHandle)
        contentContainer = root.findViewById(R.id.winContent)

        winTitle.text = title
        winTitle.setTextColor(textColor)
        (root.findViewById<ImageView>(R.id.winIcon)).apply {
            setImageResource(iconRes)
            setColorFilter(accent)
        }
        listOf(btnMinimize, btnMaximize, btnClose).forEach {
            it.setColorFilter(textColor)
        }
        (titleBar.background as? GradientDrawable)?.setColor(surfaceAlt)

        contentContainer.addView(
            content,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        btnMinimize.setOnClickListener { manager.minimize(this) }
        btnMaximize.setOnClickListener { toggleMaximize() }
        btnClose.setOnClickListener { manager.close(this) }
        resizeHandle.setOnTouchListener(ResizeTouch())
        titleBar.setOnTouchListener(DragTouch())

        setOnTouchListener { _, _ ->
            manager.focus(this)
            false
        }
    }

    /** Places the window at [left]/[top] with the given size. */
    fun place(left: Int, top: Int, width: Int, height: Int) {
        layoutParams = FrameLayout.LayoutParams(width, height).apply {
            this.leftMargin = left
            this.topMargin = top
        }
        restoreLeft = left
        restoreTop = top
        restoreWidth = width
        restoreHeight = height
    }

    fun setMaximized(max: Boolean) {
        isMaximized = max
        btnMaximize.setImageResource(if (max) R.drawable.ic_restore else R.drawable.ic_maximize)
        val parent = parent as? FrameLayout ?: return
        if (max) {
            restoreLeft = left
            restoreTop = top
            restoreWidth = width
            restoreHeight = height
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        } else {
            place(restoreLeft, restoreTop, restoreWidth, restoreHeight)
        }
        requestLayout()
    }

    private fun toggleMaximize() {
        manager.focus(this)
        setMaximized(!isMaximized)
    }

    fun setMinimized(min: Boolean) {
        isMinimized = min
        visibility = if (min) View.GONE else View.VISIBLE
        if (!min) manager.focus(this)
    }

    fun flashTitle() {
        winTitle.setTextColor(accent)
        postDelayed({ winTitle.setTextColor(textColor) }, 400)
    }

    /** Drag-to-move on the title bar. */
    private inner class DragTouch : OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startLeft = 0
        private var startTop = 0
        private var moved = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startLeft = left
                    startTop = top
                    moved = false
                    manager.focus(this@DeskWindow)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) > 2 || kotlin.math.abs(dy) > 2) moved = true
                    if (isMaximized) return true
                    val parent = parent as? FrameLayout ?: return true
                    val newLeft = (startLeft + dx).toInt().coerceIn(-width * 2 / 3, parent.width - width / 3)
                    val newTop = (startTop + dy).toInt().coerceIn(0, parent.height - dp(44))
                    layoutParams = (layoutParams as FrameLayout.LayoutParams).apply {
                        leftMargin = newLeft
                        topMargin = newTop
                    }
                    requestLayout()
                }
            }
            return true
        }
    }

    /** Corner resize. */
    private inner class ResizeTouch : OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startW = 0
        private var startH = 0

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startW = width
                    startH = height
                    manager.focus(this@DeskWindow)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isMaximized) return true
                    val w = (startW + (event.rawX - downX)).toInt().coerceAtLeast(dp(240))
                    val h = (startH + (event.rawY - downY)).toInt().coerceAtLeast(dp(180))
                    layoutParams = (layoutParams as FrameLayout.LayoutParams).apply {
                        width = w
                        height = h
                    }
                    requestLayout()
                }
            }
            return true
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
}

/**
 * Owns the set of open windows on the desk: z-order, focus, minimize /
 * restore, close, and Alt+Tab cycling.
 */
class DeskWindowManager(
    private val context: Context,
    private val deskArea: FrameLayout,
    private val prefs: Prefs,
    private val onWindowsChanged: () -> Unit,
) {
    val windows = mutableListOf<DeskWindow>()
    var activeWindow: DeskWindow? = null
        private set
    private var nextId = 0
    private var cascade = 0

    fun addWindow(
        title: String,
        iconRes: Int,
        content: View,
        widthDp: Int = 520,
        heightDp: Int = 400,
    ): DeskWindow {
        val colors = Palette.colors(prefs, context)
        val window = DeskWindow(
            context, "w${nextId++}", title, iconRes, content,
            this, prefs.accent, colors.surface, colors.surfaceAlt, colors.text,
        )
        val w = dp(widthDp).coerceAtMost(deskArea.width - dp(16))
        val h = dp(heightDp).coerceAtMost(deskArea.height - dp(16))
        val left = (dp(24) + cascade * dp(36)).coerceAtMost((deskArea.width - w).coerceAtLeast(0))
        val top = (dp(16) + cascade * dp(32)).coerceAtMost((deskArea.height - h).coerceAtLeast(0))
        cascade = (cascade + 1) % 6
        deskArea.addView(
            window,
            FrameLayout.LayoutParams(w, h).apply {
                leftMargin = left
                topMargin = top
            }
        )
        window.place(left, top, w, h)
        windows.add(window)
        focus(window)
        onWindowsChanged()
        return window
    }

    fun focus(window: DeskWindow) {
        activeWindow = window
        // Bring to front of z-order.
        deskArea.removeView(window)
        deskArea.addView(window)
        windows.remove(window)
        windows.add(window)
        onWindowsChanged()
    }

    fun close(window: DeskWindow) {
        windows.remove(window)
        deskArea.removeView(window)
        if (activeWindow == window) activeWindow = windows.lastOrNull { !it.isMinimized }
        onWindowsChanged()
    }

    fun minimize(window: DeskWindow) {
        window.setMinimized(true)
        if (activeWindow == window) activeWindow = windows.lastOrNull { !it.isMinimized }
        onWindowsChanged()
    }

    fun restore(window: DeskWindow) {
        window.setMinimized(false)
        onWindowsChanged()
    }

    fun toggle(window: DeskWindow) {
        if (window.isMinimized) restore(window) else minimize(window)
    }

    /** Alt+Tab: cycles focus through open (non-minimized) windows. */
    fun cycle() {
        val visible = windows.filter { !it.isMinimized }
        if (visible.isEmpty()) return
        val idx = visible.indexOf(activeWindow)
        val next = visible[(idx + 1) % visible.size]
        focus(next)
        next.flashTitle()
    }

    fun closeActive() {
        activeWindow?.let { close(it) }
    }

    fun closeAll() {
        windows.toList().forEach { close(it) }
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density + 0.5f).toInt()
}
