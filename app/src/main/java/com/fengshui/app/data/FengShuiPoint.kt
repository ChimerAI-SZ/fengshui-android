package com.fengshui.app.data

enum class PointType {
    ORIGIN,
    DESTINATION
}

data class FengShuiPoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: PointType,
    val groupId: String? = null,
    val groupName: String? = null,
    val address: String? = null,
    val isActive: Boolean = false,
    val isGPSOrigin: Boolean = false,
    val isVisible: Boolean = true,
    val createTime: Long = System.currentTimeMillis()
)
