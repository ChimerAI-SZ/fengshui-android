package com.fengshui.app.map.abstraction.googlemaps

import com.fengshui.app.map.abstraction.*

/**
 * GoogleMapProvider placeholder implementation.
 * 在实际工程中，这里会封装 Google Maps SDK 的具体调用。
 */
class GoogleMapProvider : MapProvider {
    override fun addMarker(position: UniversalLatLng, title: String?): UniversalMarker {
        return UniversalMarker("gmarker_${position.latitude}_${position.longitude}")
    }

    override fun addPolyline(start: UniversalLatLng, end: UniversalLatLng, width: Float, color: Int): UniversalPolyline {
        return UniversalPolyline("gpoly_${start.latitude}_${start.longitude}_${end.latitude}_${end.longitude}")
    }

    override fun animateCamera(target: UniversalLatLng, zoom: Float) {
        // TODO: Google Maps animate
    }

    override fun animateCameraToBounds(bounds: UniversalLatLngBounds, padding: Int) {
        // TODO
    }

    override fun screenLocationToLatLng(x: Float, y: Float): UniversalLatLng {
        return UniversalLatLng(0.0, 0.0)
    }

    override fun onCameraChangeFinish(callback: (CameraPosition) -> Unit) {
        // TODO
    }

    override fun setMapType(type: MapType) {
        // TODO
    }

    override fun zoomIn() {}

    override fun zoomOut() {}
}
