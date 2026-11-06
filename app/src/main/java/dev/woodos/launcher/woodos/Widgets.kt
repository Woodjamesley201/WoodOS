package dev.woodos.launcher.woodos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.woodos.launcher.core.ClockStyle
import dev.woodos.launcher.core.Prefs
import dev.woodos.launcher.core.UiState
import dev.woodos.launcher.core.Weather
import dev.woodos.launcher.core.Wmo
import dev.woodos.launcher.ui.GlassPanel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun weatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.WbSunny
    1, 2 -> Icons.Rounded.WbCloudy
    3 -> Icons.Rounded.Cloud
    45, 48 -> Icons.Rounded.Grain
    in 51..67 -> Icons.Rounded.WaterDrop
    in 71..77 -> Icons.Rounded.AcUnit
    in 80..82 -> Icons.Rounded.WaterDrop
    in 85..86 -> Icons.Rounded.AcUnit
    else -> Icons.Rounded.Thunderstorm
}

/** Large customisable clock. */
@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    val now = rememberNow()
    val fmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val time = fmt.format(now)

    GlassPanel(modifier = modifier, corner = 30.dp) {
        when (Prefs.clockStyle) {
            ClockStyle.MODERN -> Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                Text(
                    time,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp,
                    color = Color.White,
                )
                Text(dateFmt.format(now), fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f))
            }
            ClockStyle.CLASSIC -> Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                Text(
                    time,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                Text(dateFmt.format(now), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
            }
            ClockStyle.MINIMAL -> Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(time, fontSize = 34.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text(dateFmt.format(now), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

/** Date + weather widget. */
@Composable
fun WeatherWidget(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { Weather.refresh() }
    val info = Weather.current

    GlassPanel(modifier = modifier.clickable { UiState.open(UiState.Overlay.WEATHER) }, corner = 30.dp) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                (Prefs.weatherCityName ?: "").split(",").firstOrNull() ?: "Weather",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    info?.let { "${it.tempC.toInt()}°" } ?: "--°",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                )
                Spacer(Modifier.padding(start = 10.dp))
                Icon(
                    weatherIcon(info?.code ?: 0, info?.isDay ?: true),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.height(34.dp),
                )
            }
            Text(
                info?.let { Wmo.describe(it.code) } ?: "Tap to refresh",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
fun rememberNow(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = Date()
        }
    }
    return now
}

/** Expanded forecast card (opened by tapping the weather widget). */
@Composable
fun WeatherDetailsPanel(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { Weather.refresh() }
    val info = Weather.current
    val forecast = Weather.forecast

    GlassPanel(modifier = modifier, corner = 28.dp) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(weatherIcon(info?.code ?: 0, info?.isDay ?: true), null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(start = 10.dp))
                Column {
                    Text(
                        "${info?.tempC?.toInt() ?: "--"}°  ${info?.let { Wmo.describe(it.code) } ?: ""}",
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        Prefs.weatherCityName ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (info != null) {
                Text(
                    "Feels like ${info.feelsLike.toInt()}° • Humidity ${info.humidity}% • Wind ${info.windKmh.toInt()} km/h",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (Weather.lastError != null && info == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Could not reach the weather service. Check your connection.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
            if (forecast.isNotEmpty()) {
                Text("5-day forecast", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    forecast.forEach { d ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(dayFmt.format(Date(d.date)), fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp))
                            Icon(weatherIcon(d.code, true), null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(18.dp))
                            Spacer(Modifier.padding(start = 8.dp))
                            Text(
                                "${d.minC.toInt()}° / ${d.maxC.toInt()}°",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
