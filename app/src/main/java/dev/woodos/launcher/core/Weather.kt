package dev.woodos.launcher.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeatherInfo(
    val tempC: Double,
    val feelsLike: Double,
    val code: Int,
    val isDay: Boolean,
    val windKmh: Double,
    val humidity: Int,
    val locationName: String,
    val updatedAt: Long,
)

data class DayForecast(val date: Long, val minC: Double, val maxC: Double, val code: Int)

data class GeoResult(val name: String, val country: String, val admin: String, val lat: Double, val lon: Double)

/**
 * Weather via the free, key-less Open-Meteo API. Cached for 30 minutes.
 */
object Weather {
    const val TAG = "WoodWeather"
    private const val CACHE_MS = 30 * 60 * 1000L

    var current: WeatherInfo? = null
        private set
    var forecast: List<DayForecast> = emptyList()
        private set
    var lastError: String? = null
        private set

    suspend fun refresh(force: Boolean = false): WeatherInfo? = withContext(Dispatchers.IO) {
        val c = current
        if (!force && c != null && System.currentTimeMillis() - c.updatedAt < CACHE_MS) return@withContext c
        try {
            val lat = Prefs.weatherLat.toDouble()
            val lon = Prefs.weatherLon.toDouble()
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=5&timezone=auto"
            val body = httpGet(url)
            val json = JSONObject(body)
            val cur = json.getJSONObject("current")
            val info = WeatherInfo(
                tempC = cur.optDouble("temperature_2m"),
                feelsLike = cur.optDouble("apparent_temperature"),
                code = cur.optInt("weather_code"),
                isDay = cur.optInt("is_day") == 1,
                windKmh = cur.optDouble("wind_speed_10m"),
                humidity = cur.optInt("relative_humidity_2m"),
                locationName = Prefs.weatherCityName ?: "",
                updatedAt = System.currentTimeMillis(),
            )
            current = info
            lastError = null
            val daily = json.optJSONObject("daily")
            if (daily != null) {
                val times = daily.optJSONArray("time")
                val codes = daily.optJSONArray("weather_code")
                val maxs = daily.optJSONArray("temperature_2m_max")
                val mins = daily.optJSONArray("temperature_2m_min")
                val f = ArrayList<DayForecast>()
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                for (i in 0 until (times?.length() ?: 0)) {
                    val t = fmt.parse(times.optString(i))?.time ?: continue
                    f.add(DayForecast(t, mins.optDouble(i), maxs.optDouble(i), codes.optInt(i)))
                }
                forecast = f
            }
            info
        } catch (e: Exception) {
            Log.w(TAG, "weather refresh failed", e)
            lastError = e.message
            current
        }
    }

    suspend fun geocode(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        try {
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                    java.net.URLEncoder.encode(query, "UTF-8") + "&count=6&language=en&format=json"
            val json = JSONObject(httpGet(url))
            val arr = json.optJSONArray("results") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                GeoResult(
                    o.optString("name"),
                    o.optString("country"),
                    o.optString("admin1"),
                    o.optDouble("latitude"),
                    o.optDouble("longitude"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun httpGet(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun timeLabel(): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
}

/** WMO weather code -> human label. Shared with unit tests. */
object Wmo {
    fun describe(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45 -> "Fog"
        48 -> "Rime fog"
        51 -> "Light drizzle"
        53 -> "Drizzle"
        55 -> "Dense drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Light showers"
        81 -> "Showers"
        82 -> "Violent showers"
        85 -> "Snow showers"
        86 -> "Heavy snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    fun isSevere(code: Int): Boolean = code >= 95
}
