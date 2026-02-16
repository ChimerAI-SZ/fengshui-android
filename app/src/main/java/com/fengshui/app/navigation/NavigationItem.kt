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
    CASE_OPS("case_ops", R.string.nav_case_ops, Icons.Default.Settings),
    ANALYSIS("analysis", R.string.nav_analysis, Icons.Default.Search),
    SETTINGS("settings", R.string.nav_settings, Icons.Default.Info)
}
