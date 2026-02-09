package com.fengshui.app.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fengshui.app.navigation.NavigationItem
import com.fengshui.app.map.MapScreen
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
import com.fengshui.app.screens.CaseListScreen
import com.fengshui.app.screens.SearchScreen
import com.fengshui.app.screens.InfoScreen

/**
 * MainAppScreen - 主应用界面
 *
 * 包含底部导航栏（Tab）和四个主要屏幕：
 * - [地图] MapScreen
 * - [堪舆管理] CaseListScreen
 * - [搜索] SearchScreen
 * - [说明] InfoScreen
 *
 * 架构：Scaffold + BottomNavigation 模式
 * 
 * Phase 3.1 增强：
 * - 支持快速加点功能
 * - 当用户在堪舆管理中点击"快速加点"时，自动切换到地图 Tab
 */
@Composable
fun MainAppScreen(modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(NavigationItem.MAP) }
    var quickAddCaseId by remember { mutableStateOf<String?>(null) }
    var searchFocus by remember { mutableStateOf<UniversalLatLng?>(null) }
    val context = LocalContext.current
    val mapProvider = remember { GoogleMapProvider(context) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = currentTab == item,
                        onClick = { currentTab = item },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                NavigationItem.MAP -> {
                    // MapScreen with real Google Maps (API Key from local.properties)
                    MapScreen(
                        mapProvider = mapProvider,
                        modifier = Modifier.fillMaxSize(),
                        focusLocation = searchFocus,
                        onFocusConsumed = { searchFocus = null }
                    )
                }

                NavigationItem.CASE_MANAGEMENT -> {
                    CaseListScreen(
                        modifier = Modifier.fillMaxSize(),
                        onQuickAddPoint = { caseId ->
                            quickAddCaseId = caseId
                            currentTab = NavigationItem.MAP
                        }
                    )
                }

                NavigationItem.SEARCH -> {
                    SearchScreen(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToMap = { poi ->
                            searchFocus = UniversalLatLng(poi.lat, poi.lng)
                            currentTab = NavigationItem.MAP
                        }
                    )
                }

                NavigationItem.INFO -> {
                    InfoScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
