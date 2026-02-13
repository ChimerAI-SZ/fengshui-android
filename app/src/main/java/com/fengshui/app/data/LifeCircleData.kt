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
    val shanName: String
)

enum class LifeCirclePointType(
    val label: String,
    val icon: String,
    val compassSize: Int
) {
    HOME("家", "🏠", 1000),
    WORK("公司", "💼", 750),
    ENTERTAINMENT("日常场所", "🍽️", 500)
}
