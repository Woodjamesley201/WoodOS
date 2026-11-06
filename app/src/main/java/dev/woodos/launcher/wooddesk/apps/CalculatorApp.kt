package dev.woodos.launcher.wooddesk.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.CalcEngine

/**
 * WoodDesk calculator with expression display and history.
 */
@Composable
fun CalculatorApp() {
    var display by remember { mutableStateOf("0") }
    var history by remember { mutableStateOf(listOf<String>()) }

    fun press(key: String) {
        when (key) {
            "C" -> display = "0"
            "⌫" -> display = if (display.length <= 1) "0" else display.dropLast(1)
            "=" -> {
                val result = CalcEngine.evaluateOrNull(display)
                if (result != null) {
                    history = (listOf("$display = ${CalcEngine.format(result)}") + history).take(5)
                    display = CalcEngine.format(result)
                } else {
                    display = "Error"
                }
            }
            else -> {
                if (display == "0" || display == "Error") {
                    if (key in "0123456789.") display = key else display = "0$key"
                } else {
                    display += key
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // history
        Column(Modifier.height(64.dp)) {
            history.take(3).forEach {
                Text(
                    it, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        // display
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Text(
                display,
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                maxLines = 2,
            )
        }
        Spacer(Modifier.height(8.dp))
        val rows = listOf(
            listOf("C", "(", ")", "⌫"),
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "%", "+"),
            listOf("^", "="),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        Box(
                            Modifier
                                .weight(if (key == "=" && row.size == 2) 2.6f else 1f)
                                .aspectRatio(if (key == "=" && row.size == 2) 2.6f else 1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { press(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                key,
                                fontSize = 20.sp,
                                fontWeight = if (key in listOf("=", "+", "-", "*", "/")) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (key == "=") Color.White
                                else if (key in listOf("C", "⌫")) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
