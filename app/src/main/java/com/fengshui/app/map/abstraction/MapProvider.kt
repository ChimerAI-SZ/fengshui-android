package com.fengshui.app.map.abstraction

// 统一数据类型（最小化实现，后续可扩展）
data class UniversalLatLng(val latitude: Double, val longitude: Double)
data class UniversalLatLngBounds(val southwest: UniversalLatLng, val northeast: UniversalLatLng)
data class CameraPosition(val target: UniversalLatLng, val zoom: Float, val bearing: Float = 0f)
data class ScreenPoint(val x: Float, val y: Float)

enum class MapType {
    VECTOR,
    SATELLITE
}

// 简单占位对象，实际 provider 可返回对应 SDK 对象
class UniversalMarker(val id: String)
class UniversalPolyline(val id: String)

interface MapProvider {
    fun addMarker(position: UniversalLatLng, title: String? = null): UniversalMarker
    fun addPolyline(start: UniversalLatLng, end: UniversalLatLng, width: Float = 8f, color: Int = 0xFF0000): UniversalPolyline
    fun animateCamera(target: UniversalLatLng, zoom: Float)
    fun animateCamera(position: CameraPosition) {
        animateCamera(position.target, position.zoom)
    }
    fun animateCameraToBounds(bounds: UniversalLatLngBounds, padding: Int = 0)
    fun screenLocationToLatLng(x: Float, y: Float): UniversalLatLng
    fun latLngToScreenLocation(position: UniversalLatLng): ScreenPoint
    fun onCameraChange(callback: (CameraPosition) -> Unit)
    fun onCameraChangeFinish(callback: (CameraPosition) -> Unit)
    fun setMapType(type: MapType)
    fun zoomIn()
    fun zoomOut()
    fun getCameraPosition(): CameraPosition?  // 获取当前相机位置（地图中心）
}
