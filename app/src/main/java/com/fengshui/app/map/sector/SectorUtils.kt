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
        maxDistanceMeters: Float
    ): List<PoiResult> {
        return pois.filter { poi ->
            val bearing = RhumbLineUtils.calculateRhumbBearing(
                origin.latitude,
                origin.longitude,
                poi.lat,
                poi.lng
            )
            val inAngleRange = isAngleInRange(bearing, startAngle, endAngle)
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
}
