package com.fengshui.app.map.abstraction.amap

import android.content.Context
import com.fengshui.app.map.abstraction.*
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.CameraUpdateFactory
import android.graphics.Color

/**
 * AMapProvider - 封装高德地图 SDK 的实现
 * 
 * 处理所有高德地图 API 的调用，业务代码仅通过 MapProvider 接口交互
 */
class AMapProvider(
    private val context: Context,
    private val aMap: AMap? = null
) : MapProvider {
    
    companion object {
        private const val DEFAULT_ZOOM = 15f
        private const val ANIMATION_DURATION_MS = 800
    }
    
    private var mAMap: AMap? = aMap
    private val markers = mutableMapOf<String, com.amap.api.maps.model.Marker>()
    private val markerIdByRef = mutableMapOf<com.amap.api.maps.model.Marker, String>()
    private val polylines = mutableMapOf<String, com.amap.api.maps.model.Polyline>()
    private val polylineIdByRef = mutableMapOf<com.amap.api.maps.model.Polyline, String>()
    private var cameraMoveCallback: ((CameraPosition) -> Unit)? = null
    private var cameraChangeCallback: ((CameraPosition) -> Unit)? = null
    private var polylineClickCallback: ((UniversalPolyline) -> Unit)? = null
    private var markerClickCallback: ((UniversalMarker) -> Unit)? = null
    
    /**
     * 设置底层 AMap 对象（由 MapViewWrapper 在地图加载完成后调用）
     */
    fun setAMap(map: AMap) {
        mAMap = map
        registerCameraChangeListener()
        registerPolylineClickListener()
    }
    
    /**
     * 添加标记
     */
    override fun addMarker(position: UniversalLatLng, title: String?): UniversalMarker {
        requireNotNull(mAMap) { "AMap not initialized" }
        val rawTitle = title ?: "Marker"
        val isPoiMarker = rawTitle.startsWith("[POI] ")
        val isActiveOrigin = rawTitle.startsWith("[ORIGIN_ACTIVE] ")
        val colorMatch = Regex("^\\[(DEST_C[0-4])\\]\\s*").find(rawTitle)
        val colorCode = colorMatch?.groupValues?.getOrNull(1)
        val displayTitle = when {
            isPoiMarker -> rawTitle.removePrefix("[POI] ").ifBlank { "Marker" }
            isActiveOrigin -> rawTitle.removePrefix("[ORIGIN_ACTIVE] ").ifBlank { "Marker" }
            colorMatch != null -> rawTitle.removePrefix(colorMatch.value).ifBlank { "Marker" }
            else -> rawTitle
        }
        
        val markerOptions = MarkerOptions()
            .position(LatLng(position.latitude, position.longitude))
            .title(displayTitle)
        if (isPoiMarker) {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        } else if (isActiveOrigin) {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
        } else if (colorCode != null) {
            val hue = when (colorCode) {
                "DEST_C0" -> BitmapDescriptorFactory.HUE_RED
                "DEST_C1" -> BitmapDescriptorFactory.HUE_AZURE
                "DEST_C2" -> BitmapDescriptorFactory.HUE_GREEN
                "DEST_C3" -> BitmapDescriptorFactory.HUE_ORANGE
                else -> BitmapDescriptorFactory.HUE_ROSE
            }
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
        }
        
        val marker = mAMap!!.addMarker(markerOptions)
        val markerId = "am_marker_${System.currentTimeMillis()}_${position.latitude}"
        markers[markerId] = marker
        markerIdByRef[marker] = markerId
        
        return UniversalMarker(markerId)
    }
    
    /**
     * 添加折线（连接两个点）
     */
    override fun addPolyline(
        start: UniversalLatLng,
        end: UniversalLatLng,
        width: Float,
        color: Int
    ): UniversalPolyline {
        requireNotNull(mAMap) { "AMap not initialized" }
        
        val polylineOptions = PolylineOptions()
            .addAll(listOf(
                LatLng(start.latitude, start.longitude),
                LatLng(end.latitude, end.longitude)
            ))
            .width(width)
            .color(color)
            .setDottedLine(false)
        
        val polyline = mAMap!!.addPolyline(polylineOptions)
        val polylineId = "am_poly_${System.currentTimeMillis()}_${start.latitude}"
        polylines[polylineId] = polyline
        polylineIdByRef[polyline] = polylineId
        
        return UniversalPolyline(polylineId)
    }
    
    /**
     * 动画平移到指定位置
     */
    override fun animateCamera(target: UniversalLatLng, zoom: Float) {
        requireNotNull(mAMap) { "AMap not initialized" }
        
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            LatLng(target.latitude, target.longitude),
            zoom
        )
        mAMap!!.animateCamera(cameraUpdate)
    }
    
    /**
     * 动画调整视图以适应边界（Fit Bounds）
     */
    override fun animateCameraToBounds(bounds: UniversalLatLngBounds, padding: Int) {
        requireNotNull(mAMap) { "AMap not initialized" }
        
        val latLngBounds = com.amap.api.maps.model.LatLngBounds(
            LatLng(bounds.southwest.latitude, bounds.southwest.longitude),
            LatLng(bounds.northeast.latitude, bounds.northeast.longitude)
        )
        
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, padding)
        mAMap!!.animateCamera(cameraUpdate)
    }
    
    /**
     * 将屏幕坐标转换为地理坐标
     */
    override fun screenLocationToLatLng(x: Float, y: Float): UniversalLatLng {
        requireNotNull(mAMap) { "AMap not initialized" }
        
        // 高德地图提供了 getProjection() 方法
        val projection = mAMap!!.projection
        val point = android.graphics.Point(x.toInt(), y.toInt())
        val latLng = projection.fromScreenLocation(point)
        
        return UniversalLatLng(latLng.latitude, latLng.longitude)
    }

    /**
     * 将地理坐标转换为屏幕坐标
     */
    override fun latLngToScreenLocation(position: UniversalLatLng): ScreenPoint {
        requireNotNull(mAMap) { "AMap not initialized" }

        val projection = mAMap!!.projection
        val point = projection.toScreenLocation(LatLng(position.latitude, position.longitude))
        return ScreenPoint(point.x.toFloat(), point.y.toFloat())
    }

    /**
     * 注册相机变化监听（移动中）
     */
    override fun onCameraChange(callback: (CameraPosition) -> Unit) {
        cameraMoveCallback = callback
        registerCameraChangeListener()
    }
    
    /**
     * 注册相机变化监听
     */
    override fun onCameraChangeFinish(callback: (CameraPosition) -> Unit) {
        cameraChangeCallback = callback
        registerCameraChangeListener()
    }
    
    /**
     * 切换地图类型
     */
    override fun setMapType(type: MapType) {
        requireNotNull(mAMap) { "AMap not initialized" }
        
        val mapType = when (type) {
            MapType.VECTOR -> AMap.MAP_TYPE_NORMAL
            MapType.SATELLITE -> AMap.MAP_TYPE_SATELLITE
        }
        mAMap!!.mapType = mapType
    }
    
    /**
     * 放大
     */
    override fun zoomIn() {
        requireNotNull(mAMap) { "AMap not initialized" }
        mAMap!!.animateCamera(CameraUpdateFactory.zoomIn())
    }
    
    /**
     * 缩小
     */
    override fun zoomOut() {
        requireNotNull(mAMap) { "AMap not initialized" }
        mAMap!!.animateCamera(CameraUpdateFactory.zoomOut())
    }
    
    /**
     * 获取当前相机位置（地图中心）
     */
    override fun getCameraPosition(): CameraPosition? {
        if (mAMap == null) return null
        
        val amapPosition = mAMap!!.cameraPosition
        return CameraPosition(
            target = UniversalLatLng(amapPosition.target.latitude, amapPosition.target.longitude),
            zoom = amapPosition.zoom,
            bearing = amapPosition.bearing
        )
    }
    
    /**
     * 清除所有标记
     */
    fun clearMarkers() {
        markers.values.forEach { it.remove() }
        markers.clear()
        markerIdByRef.clear()
    }
    
    /**
     * 清除所有折线
     */
    fun clearPolylines() {
        polylines.values.forEach { it.remove() }
        polylines.clear()
        polylineIdByRef.clear()
    }

    /**
     * 注册折线点击监听
     */
    fun setOnPolylineClickListener(callback: (UniversalPolyline) -> Unit) {
        polylineClickCallback = callback
        registerPolylineClickListener()
    }

    fun setOnMarkerClickListener(callback: (UniversalMarker) -> Unit) {
        markerClickCallback = callback
        registerMarkerClickListener()
    }
    
    /**
     * 清除所有地图覆盖物
     */
    fun clearAll() {
        clearMarkers()
        clearPolylines()
    }
    
    /**
     * 私有方法：注册相机变化监听
     */
    private fun registerCameraChangeListener() {
        if (mAMap == null) return
        
        mAMap!!.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(cameraPosition: com.amap.api.maps.model.CameraPosition?) {
                if (cameraPosition != null) {
                    cameraMoveCallback?.invoke(
                        CameraPosition(
                            target = UniversalLatLng(cameraPosition.target.latitude, cameraPosition.target.longitude),
                            zoom = cameraPosition.zoom,
                            bearing = cameraPosition.bearing
                        )
                    )
                }
            }
            
            override fun onCameraChangeFinish(cameraPosition: com.amap.api.maps.model.CameraPosition?) {
                if (cameraPosition != null) {
                    cameraChangeCallback?.invoke(
                        CameraPosition(
                            target = UniversalLatLng(cameraPosition.target.latitude, cameraPosition.target.longitude),
                            zoom = cameraPosition.zoom,
                            bearing = cameraPosition.bearing
                        )
                    )
                }
            }
        })
    }

    /**
     * 私有方法：注册折线点击监听
     */
    private fun registerPolylineClickListener() {
        if (mAMap == null) return

        if (polylineClickCallback != null) {
            mAMap!!.setOnPolylineClickListener { polyline ->
                val id = polylineIdByRef[polyline]
                if (id != null) {
                    polylineClickCallback?.invoke(UniversalPolyline(id))
                }
            }
        } else {
            mAMap!!.setOnPolylineClickListener(null)
        }
    }

    private fun registerMarkerClickListener() {
        if (mAMap == null) return

        if (markerClickCallback != null) {
            mAMap!!.setOnMarkerClickListener { marker ->
                val id = markerIdByRef[marker]
                if (id != null) {
                    markerClickCallback?.invoke(UniversalMarker(id))
                    true
                } else {
                    false
                }
            }
        } else {
            mAMap!!.setOnMarkerClickListener(null)
        }
    }
}
