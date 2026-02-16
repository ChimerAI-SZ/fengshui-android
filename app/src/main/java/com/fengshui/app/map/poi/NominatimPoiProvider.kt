package com.fengshui.app.map.poi

import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.utils.AppLanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.cos

/**
 * OpenStreetMap Nominatim fallback provider.
 * Works without vendor API keys and is used as last-resort real search channel.
 */
class NominatimPoiProvider : MapPoiProvider {
    private val client = OkHttpClient()

    override suspend fun searchByKeyword(
        keyword: String,
        location: UniversalLatLng?,
        radiusMeters: Int
    ): List<PoiResult> = withContext(Dispatchers.IO) {
        try {
            val acceptLanguage = AppLanguageManager.nominatimAcceptLanguage()
            val encoded = URLEncoder.encode(keyword, "UTF-8")
            val base = StringBuilder("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=50")
                .append("&accept-language=")
                .append(URLEncoder.encode(acceptLanguage, "UTF-8"))
                .append("&q=")
                .append(encoded)

            if (location != null && radiusMeters > 0) {
                val latDelta = radiusMeters.toDouble() / 111_000.0
                val safeCos = cos(Math.toRadians(location.latitude)).coerceAtLeast(0.1)
                val lonDelta = radiusMeters.toDouble() / (111_000.0 * safeCos)
                val left = location.longitude - lonDelta
                val right = location.longitude + lonDelta
                val top = location.latitude + latDelta
                val bottom = location.latitude - latDelta
                base.append("&viewbox=$left,$top,$right,$bottom&bounded=1")
            }

            val request = Request.Builder()
                .url(base.toString())
                .header("User-Agent", "fengshui-tool/2.1 (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                val body = response.body?.string().orEmpty()
                val array = JSONArray(body)
                val list = mutableListOf<PoiResult>()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val lat = item.optString("lat").toDoubleOrNull() ?: continue
                    val lon = item.optString("lon").toDoubleOrNull() ?: continue
                    val display = item.optString("display_name")
                    val name = item.optString("name").ifBlank {
                        display.split(",").firstOrNull()?.trim().orEmpty()
                    }
                    list.add(
                        PoiResult(
                            id = item.optString("osm_id", i.toString()),
                            name = name.ifBlank { "POI ${i + 1}" },
                            lat = lat,
                            lng = lon,
                            address = display,
                            provider = "nominatim"
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun searchInBounds(bounds: Any): List<PoiResult> = emptyList()

    override suspend fun reverseGeocode(location: UniversalLatLng): String? = withContext(Dispatchers.IO) {
        try {
            val acceptLanguage = AppLanguageManager.nominatimAcceptLanguage()
            val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${location.latitude}&lon=${location.longitude}&accept-language=${URLEncoder.encode(acceptLanguage, "UTF-8")}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "fengshui-tool/2.1 (Android)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val obj = JSONObject(response.body?.string().orEmpty())
                obj.optString("display_name").ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }
}
