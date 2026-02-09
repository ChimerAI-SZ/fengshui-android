package com.fengshui.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.fengshui.app.map.poi.MockPoiProvider
import com.fengshui.app.map.poi.MapPoiProvider
import com.fengshui.app.map.poi.PoiResult
import com.fengshui.app.map.poi.AmapPoiProvider
import com.fengshui.app.map.poi.GooglePlacesProvider
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.PointType
import com.fengshui.app.utils.ApiKeyConfig

/**
 * SearchScreen - 地址搜索界面
 *
 * 功能（Phase 3 MVP）：
 * - 输入地址搜索
 * - 显示搜索历史
 *
 * 功能（Phase 4+）：
 * - 集成 Google Maps Places API / 高德地图搜索
 * - 结果列表展示
 * - 选中后跳转地图并保存点位
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onNavigateToMap: (PoiResult) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<PoiResult>()) }
    var showCaseSelectDialog by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<PoiResult?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { PointRepository(context) }
    
    // Phase 4: 动态选择 POI 提供者
    // 优先级：Google Maps > Amap > Mock（开发模式）
    val provider: MapPoiProvider = remember {
        val googleKey = ApiKeyConfig.getGoogleMapsApiKey(context)
        val amapKey = ApiKeyConfig.getAmapApiKey(context)
        
        when {
            ApiKeyConfig.isValidKey(googleKey) -> GooglePlacesProvider(googleKey!!)
            ApiKeyConfig.isValidKey(amapKey) -> AmapPoiProvider(amapKey!!)
            else -> MockPoiProvider() // 开发模式（如果没有配置 API Key）
        }
    }
    val providerName = remember(provider) {
        when (provider) {
            is GooglePlacesProvider -> "Google Places"
            is AmapPoiProvider -> "高德地图"
            else -> "Mock"
        }
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(350)
        results = provider.searchByKeyword(searchQuery.trim())
        loading = false
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "地址搜索",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("输入地址、地点或坐标") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                )
                Button(onClick = {
                    if (searchQuery.isNotBlank()) {
                        loading = true
                        scope.launch {
                            results = provider.searchByKeyword(searchQuery.trim())
                            loading = false
                        }
                    }
                }) {
                    Text("搜索")
                }
            }

            // 搜索提示区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFE3F2FD),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    "搜索提示：",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text("• 当前使用：$providerName", fontSize = 10.sp, color = Color.Gray)
                Text("• 输入文字会自动搜索，可点击结果定位到地图", fontSize = 10.sp, color = Color.Gray)
            }

            // 结果列表
            if (loading) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text("搜索中...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
            } else if (results.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("搜索结果将在此显示", fontSize = 14.sp, color = Color.Gray)
                    Text("当前提供方：$providerName", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(results) { poi ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(poi.name, fontWeight = FontWeight.Bold)
                                Text(poi.address ?: "", fontSize = 12.sp, color = Color.Gray)
                                Text("${String.format("%.4f", poi.lat)}, ${String.format("%.4f", poi.lng)}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Button(onClick = { onNavigateToMap(poi) }) {
                                    Text("定位到地图")
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                                Button(onClick = {
                                    selectedPoi = poi
                                    showCaseSelectDialog = true
                                }) {
                                    Text("添加到案例")
                                }
                            }
                        }
                    }
                }
            }

            // 案例选择对话框
            if (showCaseSelectDialog && selectedPoi != null) {
                val projects = repo.loadProjects()
                AlertDialog(
                    onDismissRequest = { showCaseSelectDialog = false; selectedPoi = null },
                    title = { Text("选择案例以保存") },
                    text = {
                        Column {
                            if (projects.isEmpty()) {
                                Text("暂无案例，请先创建案例")
                            } else {
                                projects.forEach { p ->
                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)) {
                                        Text(p.name, modifier = Modifier.weight(1f))
                                        Button(onClick = {
                                            // 保存为终点（默认）
                                            scope.launch {
                                                repo.createPoint(
                                                    selectedPoi!!.name,
                                                    selectedPoi!!.lat,
                                                    selectedPoi!!.lng,
                                                    PointType.DESTINATION,
                                                    p.id,
                                                    selectedPoi!!.address
                                                )
                                                showCaseSelectDialog = false
                                                selectedPoi = null
                                            }
                                        }) {
                                            Text("保存到 '${p.name}'")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCaseSelectDialog = false; selectedPoi = null }) { Text("取消") }
                    }
                )
            }
        }
    }
}

/**
 * InfoScreen - 应用说明和帮助界面
 *
 * 显示：
 * - 版本号
 * - 功能说明
 * - 使用教程
 * - 技术信息
 */
@Composable
fun InfoScreen(
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "应用说明",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 版本信息
            InfoSection(
                title = "版本信息",
                content = """
                    应用名：24 山风水罗盘
                    版本：1.0.0 (Phase 3)
                    发布时间：2026-02-06
                """.trimIndent()
            )

            // 功能说明
            InfoSection(
                title = "功能说明",
                content = """
                    ✓ 风水罗盘：实时显示 24 山和八卦方位
                    ✓ 点位管理：标记并管理堪舆点位
                    ✓ 连线计算：自动计算方位角和距离
                    ✓ 多案例：支持创建多个案例独立管理
                    ✓ 数据持久化：所有数据自动保存
                """.trimIndent()
            )

            // 使用技巧
            InfoSection(
                title = "使用技巧",
                content = """
                    1. 地图界面添加点位后，左上角可查看点位列表
                    2. 长按点位可重命名或删除
                    3. 创建多个案例来隔离不同的堪舆分析
                    4. 在堪舆管理中查看所有案例的详细信息
                """.trimIndent()
            )

            // 技术信息
            InfoSection(
                title = "技术架构",
                content = """
                    • 框架：Jetpack Compose + Kotlin
                    • 地图：Google Maps SDK (可切换高德地图)
                    • 数据：SharedPreferences + JSON
                    • 算法：大地测量学 (Geodesic)
                """.trimIndent()
            )

            Text(
                "© 2026 风水罗盘应用 | 单一事实来源 (SSOT) 文档",
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

@Composable
private fun InfoSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(
                color = Color(0xFFFAFAFA),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            content,
            fontSize = 10.sp,
            color = Color.Gray,
            lineHeight = 16.sp
        )
    }
}
