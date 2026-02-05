package com.fengshui.app.utils

import kotlin.math.*

object GeometryUtils {
    // 近似将经纬差转换为米：每度纬度约111132m， 经度按纬度折算
    private const val METERS_PER_DEGREE_LAT = 111132.0
    private const val METERS_PER_DEGREE_LON_AT_EQ = 111319.0

    fun pointToLineDistanceMeters(
        px: Double, py: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double
    ): Double {
        // px,py = point lat,lon
        // x1,y1 = line start lat,lon
        // x2,y2 = line end lat,lon
        val avgLat = Math.toRadians((x1 + x2 + px) / 3.0)
        val mx = (py - y1) * cos(avgLat) * METERS_PER_DEGREE_LON_AT_EQ
        val my = (px - x1) * METERS_PER_DEGREE_LAT
        val x1m = 0.0
        val y1m = 0.0
        val x2m = (y2 - y1) * cos(avgLat) * METERS_PER_DEGREE_LON_AT_EQ
        val y2m = (x2 - x1) * METERS_PER_DEGREE_LAT

        val dx = x2m - x1m
        val dy = y2m - y1m
        val l2 = dx * dx + dy * dy
        if (l2 == 0.0) {
            return sqrt(mx * mx + my * my)
        }
        val t = ((mx - x1m) * dx + (my - y1m) * dy) / l2
        val tt = t.coerceIn(0.0, 1.0)
        val projx = x1m + tt * dx
        val projy = y1m + tt * dy
        val dxp = mx - projx
        val dyp = my - projy
        return sqrt(dxp * dxp + dyp * dyp)
    }
}
