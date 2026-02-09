package com.fengshui.app.map.abstraction.mock

import com.fengshui.app.map.abstraction.*
import android.util.Log

/**
 * MockMapProvider - 用于开发中不依赖 Google Maps API Key 的虚拟地图实现
 * 
 * 可用于：
 * - UI/功能测试（不需要真实地图）
 * - 地图 API 未配置时的开发继续
 * - 单元测试和集成测试
 * 
 * 功能：
 * - 模拟所有 MapProvider 接口方法
 * - 记录日志便于调试
 * - 支持标记和折线的虚拟管理
 */
class MockMapProvider : MapProvider {
    companion object {
        private const val TAG = "MockMapProvider"
    }
    
    private val markers = mutableMapOf<String, UniversalMarker>()
    private val polylines = mutableMapOf<String, UniversalPolyline>()
    private var cameraPosition: CameraPosition? = null
    private var cameraMoveCallback: ((CameraPosition) -> Unit)? = null
    private var cameraChangeCallback: ((CameraPosition) -> Unit)? = null
    private var currentMapType = MapType.VECTOR
    
    // 模拟的中心位置（北京）
    private var centerLat = 39.9042
    private var centerLng = 116.4074
    
    override fun addMarker(position: UniversalLatLng, title: String?): UniversalMarker {
        val markerId = "mock_marker_${System.currentTimeMillis()}_${position.latitude}"
        val marker = UniversalMarker(markerId)
        markers[markerId] = marker
        
        Log.d(TAG, "Added marker: $title at (${position.latitude}, ${position.longitude})")
        return marker
    }

    override fun addPolyline(
        start: UniversalLatLng,
        end: UniversalLatLng,
        width: Float,
        color: Int
    ): UniversalPolyline {
        val polylineId = "mock_polyline_${System.currentTimeMillis()}"
        val polyline = UniversalPolyline(polylineId)
        polylines[polylineId] = polyline
        
        Log.d(TAG, "Added polyline from (${start.latitude}, ${start.longitude}) " +
                   "to (${end.latitude}, ${end.longitude}), width=$width, color=$color")
        return polyline
    }

    override fun animateCamera(target: UniversalLatLng, zoom: Float) {
        centerLat = target.latitude
        centerLng = target.longitude
        
        cameraPosition = CameraPosition(target = target, zoom = zoom)
        cameraMoveCallback?.invoke(cameraPosition!!)
        cameraChangeCallback?.invoke(cameraPosition!!)
        
        Log.d(TAG, "Animate camera to: (${target.latitude}, ${target.longitude}), zoom=$zoom")
    }

    override fun animateCameraToBounds(bounds: UniversalLatLngBounds, padding: Int) {
        val centerLat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2
        val centerLng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2
        
        // 计算适合边界的缩放级别（虚拟计算）
        val latDiff = Math.abs(bounds.northeast.latitude - bounds.southwest.latitude)
        val lngDiff = Math.abs(bounds.northeast.longitude - bounds.southwest.longitude)
        val maxDiff = Math.max(latDiff, lngDiff)
        val zoom = (12 - Math.log(maxDiff * 1.2) / Math.log(2.0)).toFloat()
        
        cameraPosition = CameraPosition(
            target = UniversalLatLng(centerLat, centerLng),
            zoom = zoom.coerceIn(1f, 20f)
        )
        cameraMoveCallback?.invoke(cameraPosition!!)
        cameraChangeCallback?.invoke(cameraPosition!!)
        
        Log.d(TAG, "Animate camera to bounds: " +
                   "(${bounds.southwest.latitude}, ${bounds.southwest.longitude}) -> " +
                   "(${bounds.northeast.latitude}, ${bounds.northeast.longitude}), " +
                   "padding=$padding")
    }

    override fun screenLocationToLatLng(x: Float, y: Float): UniversalLatLng {
        // 模拟屏幕坐标转地理坐标
        // 假设屏幕宽度 1080px, 高度 2200px 对应经纬度范围
        val screenWidth = 1080f
        val screenHeight = 2200f
        val latPerPixel = 0.0001f  // 每像素约0.0001度
        val lngPerPixel = 0.0001f
        
        val lat = centerLat + (screenHeight / 2 - y) * latPerPixel
        val lng = centerLng + (x - screenWidth / 2) * lngPerPixel
        
        Log.d(TAG, "Screen location to LatLng: ($x, $y) -> ($lat, $lng)")
        return UniversalLatLng(lat, lng)
    }

    override fun latLngToScreenLocation(position: UniversalLatLng): ScreenPoint {
        val screenWidth = 1080f
        val screenHeight = 2200f
        val latPerPixel = 0.0001f
        val lngPerPixel = 0.0001f

        val x = (position.longitude - centerLng) / lngPerPixel + screenWidth / 2f
        val y = screenHeight / 2f - (position.latitude - centerLat) / latPerPixel

        Log.d(TAG, "LatLng to screen location: (${position.latitude}, ${position.longitude}) -> ($x, $y)")
        return ScreenPoint(x.toFloat(), y.toFloat())
    }

    override fun onCameraChangeFinish(callback: (CameraPosition) -> Unit) {
        cameraChangeCallback = callback
        Log.d(TAG, "Registered camera change callback")
    }

    override fun onCameraChange(callback: (CameraPosition) -> Unit) {
        cameraMoveCallback = callback
        Log.d(TAG, "Registered camera move callback")
    }

    override fun setMapType(type: MapType) {
        currentMapType = type
        Log.d(TAG, "Map type changed to: $type")
    }

    override fun zoomIn() {
        Log.d(TAG, "Zoom in")
        if (cameraPosition != null) {
            val newZoom = (cameraPosition!!.zoom + 1).coerceAtMost(20f)
            cameraPosition = cameraPosition!!.copy(zoom = newZoom)
            cameraMoveCallback?.invoke(cameraPosition!!)
            cameraChangeCallback?.invoke(cameraPosition!!)
        }
    }

    override fun zoomOut() {
        Log.d(TAG, "Zoom out")
        if (cameraPosition != null) {
            val newZoom = (cameraPosition!!.zoom - 1).coerceAtLeast(1f)
            cameraPosition = cameraPosition!!.copy(zoom = newZoom)
            cameraMoveCallback?.invoke(cameraPosition!!)
            cameraChangeCallback?.invoke(cameraPosition!!)
        }
    }
    
    override fun getCameraPosition(): CameraPosition? = cameraPosition
    
    // 调试辅助方法
    fun getMarkerCount(): Int = markers.size
    fun getPolylineCount(): Int = polylines.size
}

// 辅助函数：扩展 CameraPosition 以支持 copy 方法
private fun CameraPosition.copy(
    target: UniversalLatLng = this.target,
    zoom: Float = this.zoom
): CameraPosition = CameraPosition(target = target, zoom = zoom)
