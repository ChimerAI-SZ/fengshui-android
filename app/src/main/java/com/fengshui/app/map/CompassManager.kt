package com.fengshui.app.map

import android.content.Context
import com.fengshui.app.utils.SensorHelper

/**
 * CompassManager: 管理传感器朝向与 GPS 位置（GPS 位置可由外部设置）。
 * onUpdate 回调在任一数据变化时触发，提供 (lat, lng, azimuthDegrees)
 */
class CompassManager(
    private val context: Context,
    private val onUpdate: (Double?, Double?, Float) -> Unit
) {
    private var gpsLat: Double? = null
    private var gpsLng: Double? = null
    private var azimuth: Float = 0f

    private val sensorHelper = SensorHelper(context) { degrees ->
        azimuth = degrees
        onUpdate(gpsLat, gpsLng, azimuth)
    }

    fun start() {
        sensorHelper.start()
    }

    fun stop() {
        sensorHelper.stop()
    }

    fun setGpsLocation(lat: Double, lng: Double) {
        gpsLat = lat
        gpsLng = lng
        onUpdate(gpsLat, gpsLng, azimuth)
    }
}
