package dev.woodos.launcher.wooddesk

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.Perf
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.ui.MicaPanel
import dev.woodos.launcher.ui.micaColor
import kotlin.math.roundToInt

/**
 * A WoodDesk window: title bar with traffic-light buttons, drag to move,
 * corner grip to resize, snap-to-edge while dragging, minimize/maximize.
 */
@Composable
fun DeskWindow(win: DeskWM.Win, focused: Boolean) {
    val density = LocalDensity.current
    val app = remember(win.appId) { DeskApps.byId(win.appId) } ?: return

    val (wDp, hDp, xDp, yDp) = if (win.maximized) {
        with(density) {
            listOf(
                DeskWM.screenW.toDp(),
                (DeskWM.screenH - DeskWM.taskbarH - DeskWM.statusBarH).toDp(),
                0.dp,
                DeskWM.statusBarH.toDp(),
            )
        }
    } else {
        with(density) {
            listOf(
                win.w.toDp(),
                win.h.toDp(),
                win.x.toDp(),
                win.y.toDp(),
            )
        }
    }

    Surface(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .width(wDp)
            .height(hDp),
        color = micaColor(),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (focused) 14.dp else 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else Color.White.copy(alpha = 0.12f),
        ),
    ) {
        Box {
            Column {
            // ---- title bar ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f))
                    .pointerInput(win.id) {
                        detectDragGestures(
                            onDragStart = { DeskWM.focus(win.id) },
                            onDrag = { change, amount ->
                                change.consume()
                                if (!win.maximized) DeskWM.drag(win.id, amount.x, amount.y)
                            },
                            onDragEnd = { DeskWM.endDrag(win.id) },
                            onDragCancel = { DeskWM.snapHint.value = DeskWM.SnapHint.NONE },
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    app.icon, contentDescription = null,
                    tint = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    win.title,
                    fontSize = 12.sp,
                    fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                WindowButton(Icons.Rounded.Minimize, "Minimize") { DeskWM.minimize(win.id) }
                WindowButton(
                    if (win.maximized) Icons.Rounded.Remove else Icons.Rounded.CropSquare,
                    "Maximize",
                ) { DeskWM.toggleMaximize(win.id) }
                WindowButton(Icons.Rounded.Close, "Close", danger = true) { DeskWM.close(win.id) }
            }

            // ---- content ----
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                app.content()
            }
            }

            // ---- resize grip (overlay, bottom-right) ----
            if (!win.maximized) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .pointerInput(win.id) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                DeskWM.resize(win.id, amount.x, amount.y)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .padding(2.dp)
            .offset { IntOffset.Zero }
            .background(if (danger) Color(0x33FF5252) else Color.White.copy(alpha = 0.10f))
            .pointerInput(desc) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription = desc,
            tint = if (danger) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(13.dp),
        )
    }
}
