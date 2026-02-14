package com.fengshui.app.map.sector

import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.poi.PoiResult
import com.fengshui.app.utils.RhumbLineUtils

object SectorUtils {
    fun filterPOIsInSector(
        origin: UniversalLatLng,
        pois: List<PoiResult>,
        startAngle: Float,
        endAngle: Float,
        maxDistanceMeters: Float,
        bearingOffsetDegrees: Float = 0f,
        angleToleranceDegrees: Float = 0f
    ): List<PoiResult> {
        val adjustedStart = normalizeAngle(startAngle - angleToleranceDegrees)
        val adjustedEnd = normalizeAngle(endAngle + angleToleranceDegrees)
        return pois.filter { poi ->
            val bearingRaw = RhumbLineUtils.calculateRhumbBearing(
                origin.latitude,
                origin.longitude,
                poi.lat,
                poi.lng
            )
            val bearing = normalizeAngle(bearingRaw + bearingOffsetDegrees)
            val inAngleRange = isAngleInRange(bearing, adjustedStart, adjustedEnd)
            val distance = RhumbLineUtils.calculateRhumbDistance(
                origin,
                UniversalLatLng(poi.lat, poi.lng)
            )
            val inDistanceRange = distance <= maxDistanceMeters
            inAngleRange && inDistanceRange
        }
    }

    fun isAngleInRange(angle: Float, start: Float, end: Float): Boolean {
        return if (start <= end) {
            angle in start..end
        } else {
            angle >= start || angle <= end
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var value = angle % 360f
        if (value < 0f) value += 360f
        return value
    }
}
