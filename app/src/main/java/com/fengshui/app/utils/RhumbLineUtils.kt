package com.fengshui.app.utils

import com.fengshui.app.data.ShanUtils
import com.fengshui.app.map.abstraction.UniversalLatLng
import kotlin.math.*

object RhumbLineUtils {

    // 计算恒向线方位角（Rhumb bearing）
    // 输入纬度经度为十进制度
    fun calculateRhumbBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        var dLon = Math.toRadians(lon2 - lon1)

        // 处理跨180度
        if (dLon > Math.PI) dLon -= 2 * Math.PI
        if (dLon < -Math.PI) dLon += 2 * Math.PI

        val dPhi = ln(tan(phi2 / 2.0 + Math.PI / 4.0) / tan(phi1 / 2.0 + Math.PI / 4.0))
        val bearing = atan2(dLon, dPhi)
        return ((Math.toDegrees(bearing) + 360.0) % 360.0).toFloat()
    }

    fun calculateRhumbBearing(origin: UniversalLatLng, destination: UniversalLatLng): Float {
        return calculateRhumbBearing(origin.latitude, origin.longitude, destination.latitude, destination.longitude)
    }

    // 近似直线距离（使用球面大圆近似；Rhumb 距离略有不同，但对于短距离可用 Haversine）
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000.0 // m
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (R * c).toFloat()
    }

    fun calculateRhumbDistance(origin: UniversalLatLng, destination: UniversalLatLng): Float {
        val R = 6371000.0
        val phi1 = Math.toRadians(origin.latitude)
        val phi2 = Math.toRadians(destination.latitude)
        var dLon = Math.toRadians(destination.longitude - origin.longitude)

        if (dLon > Math.PI) dLon -= 2 * Math.PI
        if (dLon < -Math.PI) dLon += 2 * Math.PI

        val dPhi = phi2 - phi1
        val dPsi = ln(tan(phi2 / 2.0 + Math.PI / 4.0) / tan(phi1 / 2.0 + Math.PI / 4.0))
        val q = if (abs(dPsi) > 1e-12) dPhi / dPsi else cos(phi1)

        val distance = sqrt(dPhi * dPhi + q * q * dLon * dLon) * R
        return distance.toFloat()
    }

    fun calculateRhumbDestination(
        start: UniversalLatLng,
        bearing: Float,
        distanceMeters: Float
    ): UniversalLatLng {
        val R = 6371000.0
        val phi1 = Math.toRadians(start.latitude)
        val lambda1 = Math.toRadians(start.longitude)
        val theta = Math.toRadians(bearing.toDouble())
        val d = distanceMeters.toDouble() / R

        val dPhi = d * cos(theta)
        var phi2 = phi1 + dPhi

        if (phi2 > Math.PI / 2) phi2 = Math.PI / 2
        if (phi2 < -Math.PI / 2) phi2 = -Math.PI / 2

        val dPsi = ln(tan(phi2 / 2.0 + Math.PI / 4.0) / tan(phi1 / 2.0 + Math.PI / 4.0))
        val q = if (abs(dPsi) > 1e-12) dPhi / dPsi else cos(phi1)
        val dLambda = d * sin(theta) / q

        var lambda2 = lambda1 + dLambda
        if (lambda2 > Math.PI) lambda2 -= 2 * Math.PI
        if (lambda2 < -Math.PI) lambda2 += 2 * Math.PI

        return UniversalLatLng(Math.toDegrees(phi2), Math.toDegrees(lambda2))
    }

    fun getShanIndex(angle: Float): Int {
        return ShanUtils.getShanIndex(angle)
    }

    fun getShanName(angle: Float): String = ShanUtils.SHAN_NAMES[getShanIndex(angle)]

    // 简单八卦映射（基于中心角）
    fun getBaGua(angle: Float): String {
        return ShanUtils.getBaGuaByIndex(getShanIndex(angle)).label
    }

    // 五行简单映射（示意）
    fun getWuXing(angle: Float): String {
        return ShanUtils.getWuXingByIndex(getShanIndex(angle)).label
    }

    fun getReverseBearing(bearing: Float): Float = ((bearing + 180f) % 360f)

    fun getOppositeShanIndex(shanIndex: Int): Int = ShanUtils.getOppositeShanIndex(shanIndex)

    fun verifySymmetry(pointA: UniversalLatLng, pointB: UniversalLatLng, epsilonDegrees: Float = 0.5f): Boolean {
        val ab = calculateRhumbBearing(pointA, pointB)
        val ba = calculateRhumbBearing(pointB, pointA)
        val sum = (ab + ba) % 360f
        val diff = abs(360f - if (sum == 0f) 360f else sum)
        return diff <= epsilonDegrees
    }
}
