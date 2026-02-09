package com.fengshui.app.map.poi

import com.fengshui.app.map.abstraction.UniversalLatLng
import kotlinx.coroutines.delay

/**
 * MockPoiProvider - 开发模式下的本地 POI 提供者
 */
class MockPoiProvider: MapPoiProvider {
    private val sample = listOf(
        PoiResult(
            id = "1",
            name = "天安门",
            lat = 39.9087,
            lng = 116.3975,
            address = "北京市东城区",
            provider = "mock"
        ),
        PoiResult(
            id = "2",
            name = "故宫",
            lat = 39.9163,
            lng = 116.3972,
            address = "北京市东城区",
            provider = "mock"
        ),
        PoiResult(
            id = "3",
            name = "王府井",
            lat = 39.9141,
            lng = 116.4068,
            address = "北京市东城区",
            provider = "mock"
        )
    )
    override suspend fun searchByKeyword(keyword: String, location: UniversalLatLng?, radiusMeters: Int): List<PoiResult> {
        delay(200) // 模拟网络延迟
        return sample.filter { it.name.contains(keyword) || it.address?.contains(keyword) == true }
    }

    override suspend fun searchInBounds(bounds: Any): List<PoiResult> {
        delay(200)
        return sample
    }

    override suspend fun reverseGeocode(location: UniversalLatLng): String? {
        delay(50)
        return "Mock Address at ${location.latitude}, ${location.longitude}"
    }
}
