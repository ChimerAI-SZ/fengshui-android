package com.fengshui.app.map.poi

import com.fengshui.app.map.abstraction.UniversalLatLng

/**
 * MapPoiProvider - POI 搜索抽象层接口
 */
interface MapPoiProvider {
    /**
     * 根据关键字搜索 POI
     * @param keyword 关键字
     * @param location 可选中心点，用于加权排序
     * @param radiusMeters 搜索半径（米），0 为不限
     */
    suspend fun searchByKeyword(keyword: String, location: UniversalLatLng? = null, radiusMeters: Int = 0): List<PoiResult>

    /**
     * 在给定矩形/边界内搜索（SDK 可能支持）
     */
    suspend fun searchInBounds(bounds: Any): List<PoiResult>

    /**
     * 逆地理编码：坐标 -> 地址
     */
    suspend fun reverseGeocode(location: UniversalLatLng): String?
}

// 简单的数据模型
data class PoiResult(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val provider: String
)
