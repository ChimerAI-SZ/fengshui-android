package com.fengshui.app.map.abstraction.googlemaps

import android.content.Context
import com.fengshui.app.map.abstraction.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.CameraUpdateFactory
import android.graphics.Color

/**
 * GoogleMapProvider - 封装 Google Maps SDK 的实现
 * 
 * 处理所有 GoogleMap API 的调用，业务代码仅通过 MapProvider 接口交互
 */
class GoogleMapProvider(
    private val context: Context,
    private val googleMap: GoogleMap? = null
) : MapProvider {
    
    companion object {
        private const val DEFAULT_ZOOM = 15f
        private const val ANIMATION_DURATION_MS = 800
    }
    
    private var mGoogleMap: GoogleMap? = googleMap
    private val markers = mutableMapOf<String, com.google.android.gms.maps.model.Marker>()
    private val polylines = mutableMapOf<String, com.google.android.gms.maps.model.Polyline>()
    private val polylineIdByRef = mutableMapOf<com.google.android.gms.maps.model.Polyline, String>()
    private var cameraMoveCallback: ((CameraPosition) -> Unit)? = null
    private var cameraChangeCallback: ((CameraPosition) -> Unit)? = null
    private var polylineClickCallback: ((UniversalPolyline) -> Unit)? = null
    
    /**
     * 设置底层 GoogleMap 对象（由 MapViewWrapper 在地图加载完成后调用）
     */
    fun setGoogleMap(map: GoogleMap) {
        mGoogleMap = map
        registerCameraChangeListener()
        registerPolylineClickListener()
    }
    
    /**
     * 添加标记
     */
    override fun addMarker(position: UniversalLatLng, title: String?): UniversalMarker {
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        
        val markerOptions = MarkerOptions()
            .position(LatLng(position.latitude, position.longitude))
            .title(title ?: "Marker")
        
        val marker = mGoogleMap!!.addMarker(markerOptions)
        val markerId = "gm_marker_${System.currentTimeMillis()}_${position.latitude}"
        markers[markerId] = marker!!
        
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
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        
        val polylineOptions = PolylineOptions()
            .add(LatLng(start.latitude, start.longitude))
            .add(LatLng(end.latitude, end.longitude))
            .width(width)
            .color(color)
            .clickable(true)
        
        val polyline = mGoogleMap!!.addPolyline(polylineOptions)
        val polylineId = "gm_poly_${System.currentTimeMillis()}_${start.latitude}"
        polylines[polylineId] = polyline
        polylineIdByRef[polyline] = polylineId
        
        return UniversalPolyline(polylineId)
    }
    
    /**
     * 动画平移到指定位置
     */
    override fun animateCamera(target: UniversalLatLng, zoom: Float) {
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            LatLng(target.latitude, target.longitude),
            zoom
        )
        mGoogleMap!!.animateCamera(cameraUpdate, ANIMATION_DURATION_MS, null)
    }
    
    /**
     * 动画调整视图以适应边界（Fit Bounds）
     */
    override fun animateCameraToBounds(bounds: UniversalLatLngBounds, padding: Int) {
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        
        val latLngBounds = LatLngBounds(
            LatLng(bounds.southwest.latitude, bounds.southwest.longitude),
            LatLng(bounds.northeast.latitude, bounds.northeast.longitude)
        )
        
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, padding)
        mGoogleMap!!.animateCamera(cameraUpdate, ANIMATION_DURATION_MS, null)
    }
    
    /**
     * 将屏幕坐标转换为地理坐标
     */
    override fun screenLocationToLatLng(x: Float, y: Float): UniversalLatLng {
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        
        // Google Maps 提供了 getProjection() 方法
        val projection = mGoogleMap!!.projection
        val point = android.graphics.Point(x.toInt(), y.toInt())
        val latLng = projection.fromScreenLocation(point)
        
        return UniversalLatLng(latLng.latitude, latLng.longitude)
    }

    /**
     * 将地理坐标转换为屏幕坐标
     */
    override fun latLngToScreenLocation(position: UniversalLatLng): ScreenPoint {
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }

        val projection = mGoogleMap!!.projection
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
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        
        val mapType = when (type) {
            MapType.VECTOR -> GoogleMap.MAP_TYPE_NORMAL
            MapType.SATELLITE -> GoogleMap.MAP_TYPE_SATELLITE
        }
        mGoogleMap!!.mapType = mapType
    }
    
    /**
     * 放大
     */
    override fun zoomIn() {
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        mGoogleMap!!.animateCamera(CameraUpdateFactory.zoomIn())
    }
    
    /**
     * 缩小
     */
    override fun zoomOut() {
        requireNotNull(mGoogleMap) { "GoogleMap not initialized" }
        mGoogleMap!!.animateCamera(CameraUpdateFactory.zoomOut())
    }
    
    /**
     * 获取当前相机位置（地图中心）
     */
    override fun getCameraPosition(): CameraPosition? {
        if (mGoogleMap == null) return null
        
        val cameraPosition = mGoogleMap!!.cameraPosition
        return CameraPosition(
            target = UniversalLatLng(cameraPosition.target.latitude, cameraPosition.target.longitude),
            zoom = cameraPosition.zoom
        )
    }
    
    /**
     * 清除所有标记
     */
    fun clearMarkers() {
        markers.values.forEach { it.remove() }
        markers.clear()
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
        if (mGoogleMap == null) return

        if (cameraMoveCallback != null) {
            mGoogleMap!!.setOnCameraMoveListener {
                val cameraPosition = mGoogleMap!!.cameraPosition
                cameraMoveCallback?.invoke(
                    CameraPosition(
                        target = UniversalLatLng(cameraPosition.target.latitude, cameraPosition.target.longitude),
                        zoom = cameraPosition.zoom
                    )
                )
            }
        } else {
            mGoogleMap!!.setOnCameraMoveListener(null)
        }

        if (cameraChangeCallback != null) {
            mGoogleMap!!.setOnCameraIdleListener {
                val cameraPosition = mGoogleMap!!.cameraPosition
                cameraChangeCallback?.invoke(
                    CameraPosition(
                        target = UniversalLatLng(cameraPosition.target.latitude, cameraPosition.target.longitude),
                        zoom = cameraPosition.zoom
                    )
                )
            }
        } else {
            mGoogleMap!!.setOnCameraIdleListener(null)
        }
    }

    /**
     * 私有方法：注册折线点击监听
     */
    private fun registerPolylineClickListener() {
        if (mGoogleMap == null) return

        if (polylineClickCallback != null) {
            mGoogleMap!!.setOnPolylineClickListener { polyline ->
                val id = polylineIdByRef[polyline]
                if (id != null) {
                    polylineClickCallback?.invoke(UniversalPolyline(id))
                }
            }
        } else {
            mGoogleMap!!.setOnPolylineClickListener(null)
        }
    }
}
