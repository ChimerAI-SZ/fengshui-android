package com.fengshui.app.map.poi

import com.fengshui.app.map.abstraction.UniversalLatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * GooglePlacesProvider - Google Places API 客户端（Retrofit + REST API）
 *
 * 需要：
 * 1. 在 local.properties 中配置 GOOGLE_PLACES_API_KEY
 * 2. build.gradle 中已有 retrofit 和 gson 依赖
 */
class GooglePlacesProvider(private val apiKey: String) : MapPoiProvider {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/place/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GooglePlacesApiService::class.java)

    override suspend fun searchByKeyword(
        keyword: String,
        location: UniversalLatLng?,
        radiusMeters: Int
    ): List<PoiResult> = withContext(Dispatchers.IO) {
        try {
            val locationStr = if (location != null && radiusMeters > 0) {
                "${location.latitude},${location.longitude}"
            } else {
                null
            }

            val response = apiService.textSearch(
                query = keyword,
                key = apiKey,
                location = locationStr,
                radius = if (radiusMeters > 0) radiusMeters else null
            )

            if (response.status == "OK" && response.results != null) {
                response.results.map { place ->
                    PoiResult(
                        id = place.place_id ?: "",
                        name = place.name ?: "",
                        lat = place.geometry?.location?.lat ?: 0.0,
                        lng = place.geometry?.location?.lng ?: 0.0,
                        address = place.formatted_address,
                        provider = "google"
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
        // TODO: Phase 4.1 - 实现边界搜索
        emptyList()
    }

    override suspend fun reverseGeocode(location: UniversalLatLng): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.reverseGeocode(
                latlng = "${location.latitude},${location.longitude}",
                key = apiKey
            )

            if (response.status == "OK" && response.results != null && response.results.isNotEmpty()) {
                response.results[0].formatted_address
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// ===== Google Places API 接口与数据模型 =====

interface GooglePlacesApiService {
    @GET("textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("key") key: String,
        @Query("location") location: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("language") language: String = "zh-CN"
    ): GoogleTextSearchResponse

    @GET("geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") key: String,
        @Query("language") language: String = "zh-CN"
    ): GoogleGeocodeResponse
}

data class GoogleTextSearchResponse(
    val status: String,
    val results: List<GooglePlace>? = null
)

data class GooglePlace(
    val place_id: String? = null,
    val name: String? = null,
    val formatted_address: String? = null,
    val geometry: GoogleGeometry? = null
)

data class GoogleGeometry(
    val location: GoogleLatLng? = null
)

data class GoogleLatLng(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class GoogleGeocodeResponse(
    val status: String,
    val results: List<GoogleGeocodeResult>? = null
)

data class GoogleGeocodeResult(
    val formatted_address: String? = null,
    val geometry: GoogleGeometry? = null
)
