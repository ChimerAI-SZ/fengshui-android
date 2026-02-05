package com.fengshui.app.map.abstraction.amap

import com.fengshui.app.map.abstraction.*

/**
 * AMapProvider placeholder implementation.
 * 在实际工程中，这里会封装高德地图 SDK 的具体调用。
 */
class AMapProvider : MapProvider {
    override fun addMarker(position: UniversalLatLng, title: String?): UniversalMarker {
        // TODO: 实际接入高德 SDK
        return UniversalMarker("amarker_${position.latitude}_${position.longitude}")
    }

    override fun addPolyline(start: UniversalLatLng, end: UniversalLatLng, width: Float, color: Int): UniversalPolyline {
        // TODO: 实际接入高德 Polyline
        return UniversalPolyline("apoly_${start.latitude}_${start.longitude}_${end.latitude}_${end.longitude}")
    }

    override fun animateCamera(target: UniversalLatLng, zoom: Float) {
        // TODO: 调用高德 animateCamera
    }

    override fun animateCameraToBounds(bounds: UniversalLatLngBounds, padding: Int) {
        // TODO
    }

    override fun screenLocationToLatLng(x: Float, y: Float): UniversalLatLng {
        // TODO: convert screen point to latlng
        return UniversalLatLng(0.0, 0.0)
    }

    override fun onCameraChangeFinish(callback: (CameraPosition) -> Unit) {
        // TODO: 注册回调
    }

    override fun setMapType(type: MapType) {
        // TODO: map type switch
    }

    override fun zoomIn() {
        // TODO
    }

    override fun zoomOut() {
        // TODO
    }
}
