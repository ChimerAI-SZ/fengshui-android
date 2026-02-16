package com.fengshui.app.data

data class LifeCircleData(
    val projectId: String,
    val homePoint: FengShuiPoint,
    val workPoint: FengShuiPoint,
    val entertainmentPoint: FengShuiPoint,
    val createTime: Long = System.currentTimeMillis()
)

data class LifeCircleConnection(
    val fromPoint: FengShuiPoint,
    val toPoint: FengShuiPoint,
    val distance: Float,
    val bearing: Float,
    val shanIndex: Int
)

enum class LifeCirclePointType(
    val compassSize: Int
) {
    HOME(1000),
    WORK(750),
    ENTERTAINMENT(500)
}
