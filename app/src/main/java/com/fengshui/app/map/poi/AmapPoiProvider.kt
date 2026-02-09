package com.fengshui.app.map.poi

import com.fengshui.app.map.abstraction.UniversalLatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * AmapPoiProvider - 高德地图 POI 搜索客户端（Retrofit + REST API）
 *
 * 需要：
 * 1. 在 local.properties 中配置 AMAP_API_KEY
 * 2. build.gradle 中已有 retrofit 和 gson 依赖
 */
class AmapPoiProvider(private val apiKey: String) : MapPoiProvider {

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
            val locationStr = if (location != null) {
                "${location.longitude},${location.latitude}"
            } else {
                null
            }

            val response = apiService.textSearch(
                keywords = keyword,
                key = apiKey,
                location = locationStr,
                radius = if (radiusMeters > 0) radiusMeters else null
            )

            if (response.status == "1" && response.pois != null) {
                response.pois.map { poi ->
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
                emptyList()
            }
        } catch (e: Exception) {
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
                key = apiKey
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
}

// ===== 高德 API 接口与数据模型 =====

interface AmapApiService {
    @GET("place/text")
    suspend fun textSearch(
        @Query("keywords") keywords: String,
        @Query("key") key: String,
        @Query("location") location: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("offset") offset: Int = 20
    ): AmapTextSearchResponse

    @GET("geocode/regeo")
    suspend fun reverseGeocode(
        @Query("location") location: String,
        @Query("key") key: String
    ): AmapReverseGeocodeResponse
}

data class AmapTextSearchResponse(
    val status: String,
    val pois: List<AmapPoi>? = null
)

data class AmapPoi(
    val id: String? = null,
    val name: String? = null,
    val location: String? = null, // "lng,lat"
    val address: String? = null
)

data class AmapReverseGeocodeResponse(
    val status: String,
    val regeocode: AmapRegeocodeInfo? = null
)

data class AmapRegeocodeInfo(
    val formatted_address: String? = null,
    val addressComponent: Map<String, String>? = null
)
