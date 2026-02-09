package com.fengshui.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航栏的Tab定义
 * 对应产品规格中的 V1: "底部Tab栏 [地图] | [堪舆管理] | [搜索] | [说明]"
 */
enum class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    MAP("map", "地图", Icons.Default.Home),
    CASE_MANAGEMENT("cases", "堪舆管理", Icons.Default.Settings),
    SEARCH("search", "搜索", Icons.Default.Search),
    INFO("info", "说明", Icons.Default.Info)
}
