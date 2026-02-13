package com.fengshui.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.fengshui.app.R

/**
 * 底部导航栏的Tab定义
 * 对应产品规格中的 V1: "底部Tab栏 [地图] | [堪舆管理] | [搜索] | [说明]"
 */
enum class NavigationItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    MAP("map", R.string.nav_map, Icons.Default.Home),
    CASE_MANAGEMENT("cases", R.string.nav_case_management, Icons.Default.Settings),
    SEARCH("search", R.string.nav_search, Icons.Default.Search),
    INFO("info", R.string.nav_info, Icons.Default.Info)
}
