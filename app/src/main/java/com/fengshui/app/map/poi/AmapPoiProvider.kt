package com.fengshui.app.map.poi

import com.fengshui.app.map.abstraction.UniversalLatLng
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale

/**
 * AmapPoiProvider - 高德地图 POI 搜索客户端（Retrofit + REST API）
 *
 * 需要：
 * 1. 在 local.properties 中配置 AMAP_API_KEY
 * 2. build.gradle 中已有 retrofit 和 gson 依赖
 */
class AmapPoiProvider(private val apiKey: String) : MapPoiProvider {
    companion object {
        private const val TAG = "AmapPoiProvider"
    }
    private var lastStats: ProviderSearchStats = ProviderSearchStats(0, 0, null)

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://restapi.amap.com/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(AmapApiService::class.java)

    override suspend fun searchByKeyword(
        keyword: String,
        location: UniversalLatLng?,
        radiusMeters: Int
    ): List<PoiResult> = withContext(Dispatchers.IO) {
        try {
            lastStats = ProviderSearchStats(0, 0, null)
            val mappedTypeCode = PoiTypeMapper.toAmapTypeCode(keyword)
            val languageCode = if (Locale.getDefault().language.startsWith("zh", ignoreCase = true)) "zh_cn" else "en"
            val locationStr = if (location != null) {
                "${location.longitude},${location.latitude}"
            } else {
                null
            }
            val fallbackQueries = PoiTypeMapper.fallbackQueries(keyword)

            val response = if (mappedTypeCode != null && locationStr != null) {
                val maxRadius = radiusMeters.coerceAtLeast(100).coerceAtMost(50_000)
                val radiusAttempts = linkedSetOf(
                    maxRadius,
                    5_000,
                    10_000,
                    20_000,
                    50_000
                ).filter { it <= maxRadius }.ifEmpty { listOf(maxRadius) }

                val aggregated = mutableListOf<AmapPoi>()
                for (r in radiusAttempts) {
                    val resp = apiService.aroundSearch(
                        location = locationStr,
                        key = apiKey,
                        radius = r,
                        types = mappedTypeCode,
                        language = languageCode
                    )
                    if (resp.status == "1" && !resp.pois.isNullOrEmpty()) {
                        aggregated.addAll(resp.pois)
                    }
                }
                // typed + text fallback round for broader recall
                if (aggregated.isEmpty()) {
                    for (q in fallbackQueries) {
                        val resp = apiService.textSearch(
                            keywords = q,
                            key = apiKey,
                            location = locationStr,
                            radius = maxRadius,
                            language = languageCode
                        )
                        if (resp.status == "1" && !resp.pois.isNullOrEmpty()) {
                            aggregated.addAll(resp.pois)
                            if (aggregated.size >= 30) break
                        }
                    }
                }
                if (aggregated.isNotEmpty()) {
                    AmapTextSearchResponse(status = "1", pois = aggregated.distinctBy { "${it.id}|${it.location}" })
                } else {
                    // Secondary typed fallback: text search then verify by returned POI type labels.
                    apiService.textSearch(
                        keywords = keyword,
                        key = apiKey,
                        location = locationStr,
                        radius = maxRadius,
                        language = languageCode
                    )
                }
            } else {
                val maxRadius = if (radiusMeters > 0) radiusMeters.coerceAtMost(50_000) else 20_000
                var chosen: AmapTextSearchResponse? = null
                for (q in fallbackQueries) {
                    val resp = apiService.textSearch(
                        keywords = q,
                        key = apiKey,
                        location = locationStr,
                        radius = maxRadius,
                        language = languageCode
                    )
                    if (resp.status == "1" && !resp.pois.isNullOrEmpty()) {
                        chosen = resp
                        break
                    }
                }
                chosen ?: apiService.textSearch(
                    keywords = keyword,
                    key = apiKey,
                    location = locationStr,
                    radius = if (radiusMeters > 0) radiusMeters else null,
                    language = languageCode
                )
            }

            if (response.status == "1" && response.pois != null) {
                val rawCount = response.pois.size
                val normalized = response.pois
                    .filter { poi ->
                        if (mappedTypeCode == null) true
                        else PoiTypeMapper.matchesAmapTypeLabel(keyword, poi.type)
                    }
                lastStats = ProviderSearchStats(
                    rawCount = rawCount,
                    typeFilteredCount = normalized.size,
                    debugStatus = "OK"
                )
                if (mappedTypeCode != null && normalized.isEmpty()) {
                    Log.w(TAG, "typed search got POIs but none matched type-label filter, keyword=$keyword")
                }
                normalized.map { poi ->
                    PoiResult(
                        id = poi.id ?: "",
                        name = poi.name ?: "",
                        lat = poi.location?.split(",")?.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
                        lng = poi.location?.split(",")?.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
                        address = poi.address,
                        provider = "amap"
                    )
                }
            } else {
                val info = response.info ?: "unknown"
                val code = response.infocode ?: "-"
                val coverageHint = if (location != null && isLikelyOutsideChina(location)) " (可能为高德海外覆盖不足)" else ""
                lastStats = ProviderSearchStats(0, 0, "AMap ${response.status}/$code $info$coverageHint")
                emptyList()
            }
        } catch (e: Exception) {
            lastStats = ProviderSearchStats(0, 0, "AMap EXCEPTION: ${e.message ?: "unknown"}")
            emptyList()
        }
    }

    override suspend fun searchInBounds(bounds: Any): List<PoiResult> = withContext(Dispatchers.IO) {
        // TODO: Phase 4.1 - 实现边界搜索（需要多边形支持）
        emptyList()
    }

    override suspend fun reverseGeocode(location: UniversalLatLng): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.reverseGeocode(
                location = "${location.longitude},${location.latitude}",
                key = apiKey,
                language = if (Locale.getDefault().language.startsWith("zh", ignoreCase = true)) "zh_cn" else "en"
            )

            if (response.status == "1" && response.regeocode != null) {
                response.regeocode.formatted_address
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun lastSearchStats(): ProviderSearchStats = lastStats

    private fun isLikelyOutsideChina(location: UniversalLatLng): Boolean {
        return location.latitude !in 3.0..54.0 || location.longitude !in 73.0..136.0
    }
}

// ===== 高德 API 接口与数据模型 =====

interface AmapApiService {
    @GET("place/text")
    suspend fun textSearch(
        @Query("keywords") keywords: String,
        @Query("key") key: String,
        @Query("location") location: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("language") language: String? = null,
        @Query("offset") offset: Int = 20
    ): AmapTextSearchResponse

    @GET("place/around")
    suspend fun aroundSearch(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("radius") radius: Int = 3000,
        @Query("types") types: String? = null,
        @Query("keywords") keywords: String? = null,
        @Query("language") language: String? = null,
        @Query("sortrule") sortrule: String = "distance",
        @Query("offset") offset: Int = 50
    ): AmapTextSearchResponse

    @GET("geocode/regeo")
    suspend fun reverseGeocode(
        @Query("location") location: String,
        @Query("key") key: String,
        @Query("language") language: String? = null
    ): AmapReverseGeocodeResponse
}

data class AmapTextSearchResponse(
    val status: String,
    val info: String? = null,
    val infocode: String? = null,
    val pois: List<AmapPoi>? = null
)

data class AmapPoi(
    val id: String? = null,
    val name: String? = null,
    val location: String? = null, // "lng,lat"
    val address: String? = null,
    val type: String? = null
)

data class AmapReverseGeocodeResponse(
    val status: String,
    val regeocode: AmapRegeocodeInfo? = null
)

data class AmapRegeocodeInfo(
    val formatted_address: String? = null,
    val addressComponent: Map<String, String>? = null
)
