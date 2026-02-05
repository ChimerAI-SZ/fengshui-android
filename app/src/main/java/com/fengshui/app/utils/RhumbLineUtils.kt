package com.fengshui.app.utils

import kotlin.math.*

object RhumbLineUtils {
    private val SHAN_NAMES = arrayOf(
        "子", "癸", "丑", "艮", "寅", "甲",
        "卯", "乙", "辰", "巽", "巳", "丙",
        "午", "丁", "未", "坤", "申", "庚",
        "酉", "辛", "戌", "乾", "亥", "壬"
    )

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

    // 近似直线距离（使用球面大圆近似；Rhumb 距离略有不同，但对于短距离可用 Haversine）
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000.0 // m
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (R * c).toFloat()
    }

    fun getShanIndex(angle: Float): Int {
        val normalizedAngle = ((angle % 360) + 360) % 360
        return (((normalizedAngle + 7.5f) / 15f).toInt() % 24)
    }

    fun getShanName(angle: Float): String = SHAN_NAMES[getShanIndex(angle)]

    // 简单八卦映射（基于中心角）
    fun getBaGua(angle: Float): String {
        val index = getShanIndex(angle)
        val ba = when (index / 3) {
            0 -> "坎"
            1 -> "艮"
            2 -> "震"
            3 -> "巽"
            4 -> "离"
            5 -> "坤"
            6 -> "兑"
            else -> "乾"
        }
        return ba
    }

    // 五行简单映射（示意）
    fun getWuXing(angle: Float): String {
        val shan = getShanIndex(angle)
        return when (shan) {
            in 0..2, in 18..20 -> "水"
            in 3..5, in 15..17 -> "木"
            in 6..8, in 12..14 -> "火"
            in 9..11 -> "土"
            in 21..23 -> "金"
            else -> "土"
        }
    }
}
