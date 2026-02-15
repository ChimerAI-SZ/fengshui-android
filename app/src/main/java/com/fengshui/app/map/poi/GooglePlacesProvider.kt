package com.fengshui.app.map.poi

import com.fengshui.app.map.abstraction.UniversalLatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

/**
 * GooglePlacesProvider based on Places API (New): places:searchText
 * This avoids legacy Text Search endpoint dependency.
 */
class GooglePlacesProvider(private val apiKey: String) : MapPoiProvider {
    private val client = OkHttpClient()
    private var lastStats: ProviderSearchStats = ProviderSearchStats(0, 0, null)

    override suspend fun searchByKeyword(
        keyword: String,
        location: UniversalLatLng?,
        radiusMeters: Int
    ): List<PoiResult> = withContext(Dispatchers.IO) {
        try {
            lastStats = ProviderSearchStats(0, 0, null)
            val mappedType = PoiTypeMapper.toGoogleType(keyword)
            val languageCode = if (Locale.getDefault().language.startsWith("zh", ignoreCase = true)) "zh-CN" else "en"
            val attempts = mutableListOf<Pair<String, String?>>()
            if (mappedType != null) {
                attempts += keyword to mappedType
                attempts += keyword to null
                PoiTypeMapper.fallbackQueries(keyword).forEach { q ->
                    attempts += q to mappedType
                    attempts += q to null
                }
            } else {
                attempts += keyword to null
            }

            val all = mutableListOf<Pair<PoiResult, List<String>>>()
            var lastErr = "Google no result"
            for ((query, typeOpt) in attempts.distinct()) {
                val reqBody = JSONObject().apply {
                    put("textQuery", query)
                    put("languageCode", languageCode)
                    put("maxResultCount", 20)
                    if (typeOpt != null) {
                        put("includedType", typeOpt)
                        put("strictTypeFiltering", true)
                    }
                    if (location != null && radiusMeters > 0) {
                        val effectiveRadius = radiusMeters.coerceIn(100, 50_000)
                        put(
                            "locationBias",
                            JSONObject().put(
                                "circle",
                                JSONObject()
                                    .put(
                                        "center",
                                        JSONObject()
                                            .put("latitude", location.latitude)
                                            .put("longitude", location.longitude)
                                    )
                                    .put("radius", effectiveRadius.toDouble())
                            )
                        )
                    }
                }

                val request = Request.Builder()
                    .url("https://places.googleapis.com/v1/places:searchText")
                    .header("X-Goog-Api-Key", apiKey)
                    .header(
                        "X-Goog-FieldMask",
                        "places.id,places.displayName,places.formattedAddress,places.location,places.types"
                    )
                    .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                var shouldSkip = false
                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        val msg = runCatching {
                            JSONObject(body).optJSONObject("error")?.optString("message")
                        }.getOrNull().orEmpty().ifBlank { body.take(180) }
                        lastErr = "Google HTTP ${resp.code}: $msg"
                        shouldSkip = true
                        return@use
                    }

                    val root = JSONObject(body)
                    val places = root.optJSONArray("places")
                    if (places == null) {
                        shouldSkip = true
                        return@use
                    }
                    for (i in 0 until places.length()) {
                        val item = places.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        val name = item.optJSONObject("displayName")?.optString("text").orEmpty()
                        val address = item.optString("formattedAddress")
                        val loc = item.optJSONObject("location")
                        val lat = loc?.optDouble("latitude") ?: Double.NaN
                        val lng = loc?.optDouble("longitude") ?: Double.NaN
                        if (!lat.isFinite() || !lng.isFinite()) continue
                        val types = mutableListOf<String>()
                        val arr = item.optJSONArray("types")
                        if (arr != null) {
                            for (t in 0 until arr.length()) {
                                arr.optString(t)?.takeIf { it.isNotBlank() }?.let(types::add)
                            }
                        }
                        all += PoiResult(
                            id = id,
                            name = name.ifBlank { "POI ${i + 1}" },
                            lat = lat,
                            lng = lng,
                            address = address,
                            provider = "google"
                        ) to types
                    }
                }
                if (shouldSkip) continue
                if (all.isNotEmpty()) break
            }

            if (all.isEmpty()) {
                lastStats = ProviderSearchStats(0, 0, lastErr)
                emptyList()
            } else {
                val dedup = all.distinctBy { (p, _) ->
                    val lat = String.format("%.5f", p.lat)
                    val lng = String.format("%.5f", p.lng)
                    "${p.name}|$lat|$lng"
                }
                val rawCount = dedup.size
                val filtered = if (mappedType == null) {
                    dedup.map { it.first }
                } else {
                    dedup.filter { pair ->
                        val poi = pair.first
                        val types = pair.second
                        PoiTypeMapper.matchesGoogleTypes(keyword, types) ||
                            PoiTypeMapper.matchesTextByCategory(keyword, poi.name, poi.address)
                    }.map { it.first }
                }
                lastStats = ProviderSearchStats(
                    rawCount = rawCount,
                    typeFilteredCount = filtered.size,
                    debugStatus = "OK"
                )
                filtered
            }
        } catch (e: Exception) {
            lastStats = ProviderSearchStats(0, 0, "Google EXCEPTION: ${e.message ?: "unknown"}")
            emptyList()
        }
    }

    override suspend fun searchInBounds(bounds: Any): List<PoiResult> = emptyList()

    override suspend fun reverseGeocode(location: UniversalLatLng): String? = null

    override fun lastSearchStats(): ProviderSearchStats = lastStats
}
