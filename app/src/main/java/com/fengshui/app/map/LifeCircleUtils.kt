package com.fengshui.app.map

import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.LifeCircleConnection
import com.fengshui.app.data.LifeCircleData
import com.fengshui.app.data.LifeCirclePointType
import com.fengshui.app.utils.RhumbLineUtils

object LifeCircleUtils {
    private val homeKeywords = setOf("家", "住宅", "小区", "公寓", "楼盘", "房", "宅", "居")
    private val workKeywords = setOf("公司", "办公", "工作", "单位", "企业", "写字楼", "厂", "店")
    private val entertainmentKeywords = setOf("餐厅", "商场", "健身", "娱乐", "咖啡", "超市", "饭店")

    fun recommendRoles(points: List<FengShuiPoint>): Map<String, LifeCirclePointType> {
        val assignments = mutableMapOf<String, LifeCirclePointType>()
        val scores = points.associateWith { point ->
            val name = point.name
            val homeScore = homeKeywords.count { name.contains(it) }
            val workScore = workKeywords.count { name.contains(it) }
            val entertainmentScore = entertainmentKeywords.count { name.contains(it) }
            mapOf(
                LifeCirclePointType.HOME to homeScore,
                LifeCirclePointType.WORK to workScore,
                LifeCirclePointType.ENTERTAINMENT to entertainmentScore
            )
        }

        val remainingTypes = LifeCirclePointType.values().toMutableList()

        for ((point, pointScores) in scores) {
            val sorted = pointScores.entries.sortedByDescending { it.value }
            if (sorted.first().value > 0 && sorted[0].value > sorted[1].value) {
                assignments[point.id] = sorted.first().key
                remainingTypes.remove(sorted.first().key)
            }
        }

        for (point in points) {
            if (!assignments.containsKey(point.id) && remainingTypes.isNotEmpty()) {
                assignments[point.id] = remainingTypes.removeAt(0)
            }
        }

        return assignments
    }

    fun buildConnections(data: LifeCircleData): List<LifeCircleConnection> {
        val connections = mutableListOf<LifeCircleConnection>()
        val points = listOf(data.homePoint, data.workPoint, data.entertainmentPoint)

        fun addConnection(from: FengShuiPoint, to: FengShuiPoint) {
            val bearing = RhumbLineUtils.calculateRhumbBearing(
                from.latitude,
                from.longitude,
                to.latitude,
                to.longitude
            )
            val distance = RhumbLineUtils.calculateRhumbDistance(
                com.fengshui.app.map.abstraction.UniversalLatLng(from.latitude, from.longitude),
                com.fengshui.app.map.abstraction.UniversalLatLng(to.latitude, to.longitude)
            )
            val shanName = RhumbLineUtils.getShanName(bearing)
            connections.add(
                LifeCircleConnection(
                    fromPoint = from,
                    toPoint = to,
                    distance = distance,
                    bearing = bearing,
                    shanName = shanName
                )
            )
        }

        // "指入"逻辑：每个点显示指向它的连线
        for (target in points) {
            for (source in points) {
                if (source.id != target.id) {
                    addConnection(source, target)
                }
            }
        }

        return connections
    }
}
