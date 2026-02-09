package com.fengshui.app.map

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import com.fengshui.app.map.ui.CompassOverlay
import com.fengshui.app.map.CompassManager
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType
import com.fengshui.app.utils.RhumbLineUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.android.gms.maps.GoogleMap
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import com.fengshui.app.map.abstraction.MapProvider
import com.fengshui.app.map.abstraction.MapType
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.ui.MapControlButtons
import com.fengshui.app.map.ui.RegistrationDialog
import com.fengshui.app.map.GoogleMapView

/**
 * 简易 MapScreen 示例：
 * - 占位地图区域（后续替换为真正的 MapView/MapCompose）
 * - 屏幕中心十字准心
 * - 右侧放大/缩小/图层切换控件（使用 `MapControlButtons`）
 */
@Composable
fun MapScreen(
    mapProvider: MapProvider,
    modifier: Modifier = Modifier,
    onCenterCrossClicked: (() -> Unit)? = null,
    focusLocation: UniversalLatLng? = null,
    onFocusConsumed: (() -> Unit)? = null
) {
    var currentMapType by remember { mutableStateOf(MapType.VECTOR) }
    var compassLocked by remember { mutableStateOf(false) }  // 罗盘锁定状态
    var compassScreenPos by remember { mutableStateOf(Offset(0f, 0f)) }  // 锁定时罗盘在屏幕上的位置
    var lockedLat by remember { mutableStateOf<Double?>(null) }  // 锁定位置的纬度
    var lockedLng by remember { mutableStateOf<Double?>(null) }  // 锁定位置的经度
    var lastCompassUpdateMs by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    val density = LocalDensity.current

    // GPS location state - 等待真实GPS定位
    // 默认位置：北京天安门广场 (39.9042, 116.4074)，确保罗盘始终可见
    var realGpsLat by remember { mutableStateOf<Double?>(39.9042) }  // 真实GPS纬度，默认北京
    var realGpsLng by remember { mutableStateOf<Double?>(116.4074) }  // 真实GPS经度，默认北京
    var hasRealGps by remember { mutableStateOf(false) }  // 是否已获取真实GPS
    var azimuth by remember { mutableStateOf(0f) }

    // repository
    val repo = remember { PointRepository(context) }
    
    // 多案例管理
    var projects by remember { mutableStateOf(listOf<com.fengshui.app.data.Project>()) }
    var currentProject by remember { mutableStateOf<com.fengshui.app.data.Project?>(null) }
    val originPoints = remember { mutableStateListOf<FengShuiPoint>() }  // 当前案例的原点列表
    val destPoints = remember { mutableStateListOf<FengShuiPoint>() }    // 当前案例的终点列表
    var selectedOriginPoint by remember { mutableStateOf<FengShuiPoint?>(null) }  // 选中的原点（用于显示连线）
    
    // 连线数据结构：保存原点和终点的配对
    data class LineData(val origin: FengShuiPoint, val destination: FengShuiPoint)
    val linesList = remember { mutableStateListOf<LineData>() }
    
    var originPoint by remember { mutableStateOf<FengShuiPoint?>(null) }
    var destPoint by remember { mutableStateOf<FengShuiPoint?>(null) }
    val lines = remember { mutableStateListOf<Pair<FengShuiPoint, FengShuiPoint>>() }
    var showLineInfo by remember { mutableStateOf(false) }
    var lineInfoText by remember { mutableStateOf("") }
    val lineByPolylineId = remember { mutableStateMapOf<String, LineData>() }
    var showTrialDialog by remember { mutableStateOf(false) }
    var trialMessage by remember { mutableStateOf("") }
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var showCrossClickDialog by remember { mutableStateOf(false) }  // 十字指示点击对话框
    var showOriginSelectDialog by remember { mutableStateOf(false) }  // 原点选择对话框
    var showProjectSelectDialog by remember { mutableStateOf(false) }  // 案例选择对话框
    val scope = rememberCoroutineScope()
    
    // 保存GoogleMap对象供后续使用
    val gMapInstance = remember { mutableStateOf<GoogleMap?>(null) }
    
    // 从数据库加载指定项目的原点和终点
    fun loadProjectData(project: com.fengshui.app.data.Project) {
        scope.launch {
            try {
                val points = repo.getPointsByCase(project.id)  // 使用案例ID获取点位
                android.util.Log.d("MapScreen", "Loading project: ${project.name}, found ${points.size} points")
                
                originPoints.clear()
                destPoints.clear()
                linesList.clear()
                
                originPoints.addAll(points.filter { it.type == PointType.ORIGIN })
                destPoints.addAll(points.filter { it.type == PointType.DESTINATION })
                
                android.util.Log.d("MapScreen", "Origins: ${originPoints.size}, Destinations: ${destPoints.size}")
                
                // 自动生成连线：每个原点与每个终点
                for (origin in originPoints) {
                    for (dest in destPoints) {
                        linesList.add(LineData(origin, dest))
                        android.util.Log.d("MapScreen", "Generated line from ${origin.name} to ${dest.name}")
                    }
                }
                
                android.util.Log.d("MapScreen", "Total lines: ${linesList.size}")
                
                // 如果有原点，选中第一个
                if (originPoints.isNotEmpty()) {
                    selectedOriginPoint = originPoints[0]
                }
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Error loading project data: ${e.message}", e)
            }
        }
    }
    
    // 更新罗盘在屏幕上的位置（锁定模式下使用）
    fun updateCompassScreenPosition() {
        if (compassLocked && lockedLat != null && lockedLng != null) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastCompassUpdateMs < 16) {
                return
            }
            lastCompassUpdateMs = now
            val screenPos = mapProvider.latLngToScreenLocation(
                com.fengshui.app.map.abstraction.UniversalLatLng(
                    lockedLat!!,
                    lockedLng!!
                )
            )
            compassScreenPos = Offset(screenPos.x, screenPos.y)
        }
    }
    
    // 初始化：加载所有项目
    LaunchedEffect(Unit) {
        scope.launch {
            projects = repo.loadProjects()
            if (projects.isNotEmpty()) {
                currentProject = projects[0]
                loadProjectData(currentProject!!)
            }
        }
    }
    
    // 当linesList改变时，重新绘制所有连线
    LaunchedEffect(linesList.size) {
        // 如果GoogleMap已初始化
        if (gMapInstance.value != null) {
            // 清除旧的polylines
            val provider = mapProvider as? com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
            provider?.clearPolylines()
            lineByPolylineId.clear()
            
            // 添加所有新的连线
            for (line in linesList) {
                try {
                    android.util.Log.d("MapScreen", "Adding polyline from (${line.origin.latitude}, ${line.origin.longitude}) to (${line.destination.latitude}, ${line.destination.longitude})")
                    val polyline = mapProvider.addPolyline(
                        com.fengshui.app.map.abstraction.UniversalLatLng(line.origin.latitude, line.origin.longitude),
                        com.fengshui.app.map.abstraction.UniversalLatLng(line.destination.latitude, line.destination.longitude),
                        width = 5f,
                        color = 0xFF0000FF.toInt()  // 蓝色线条
                    )
                    lineByPolylineId[polyline.id] = line
                } catch (e: Exception) {
                    android.util.Log.e("MapScreen", "Error adding polyline: ${e.message}", e)
                }
            }
        } else {
            android.util.Log.w("MapScreen", "GoogleMap not ready, cannot add polylines")
        }
    }
    
    LaunchedEffect(focusLocation?.latitude, focusLocation?.longitude, gMapInstance.value) {
        val target = focusLocation
        if (target != null && gMapInstance.value != null) {
            mapProvider.animateCamera(target, 16f)
            onFocusConsumed?.invoke()
        }
    }
    
    // LocationHelper - 获取真实GPS位置
    val locationHelper = remember {
        com.fengshui.app.utils.LocationHelper(context) { lat, lng ->
            realGpsLat = lat
            realGpsLng = lng
            hasRealGps = true  // 标记已获取真实GPS
            // 首次获取GPS位置后，移动地图到当前位置
            if (!compassLocked && originPoint == null) {
                mapProvider.animateCamera(com.fengshui.app.map.abstraction.UniversalLatLng(lat, lng), 15f)
            }
        }
    }

    // 罗盘显示的坐标（根据锁定状态决定）
    // 已删除旧的 compassLat/compassLng 逻辑，改用 lockedLat/lockedLng

    val compassManager = remember {
        CompassManager(context) { lat, lng, deg ->
            // 只更新方向角，位置信息保持不变
            azimuth = deg
        }
    }

    DisposableEffect(Unit) {
        locationHelper.start()  // 启动GPS定位
        compassManager.start()
        
        // 注册地图相机移动监听，用于更新锁定模式下罗盘位置
        mapProvider.onCameraChange {
            if (compassLocked && lockedLat != null && lockedLng != null) {
                updateCompassScreenPosition()
            }
        }
        mapProvider.onCameraChangeFinish {
            if (compassLocked && lockedLat != null && lockedLng != null) {
                updateCompassScreenPosition()
            }
        }
        
        onDispose {
            locationHelper.stop()  // 停止GPS定位
            compassManager.stop()
        }
    }

    fun showLineInfoFor(line: LineData) {
        val bearing = RhumbLineUtils.calculateRhumbBearing(
            line.origin.latitude, line.origin.longitude,
            line.destination.latitude, line.destination.longitude
        )
        val shan = RhumbLineUtils.getShanName(bearing)
        val bagua = RhumbLineUtils.getBaGua(bearing)
        val wuxing = RhumbLineUtils.getWuXing(bearing)
        val dist = RhumbLineUtils.haversineDistanceMeters(
            line.origin.latitude, line.origin.longitude,
            line.destination.latitude, line.destination.longitude
        )
        lineInfoText = "原点: ${line.origin.name}\n终点: ${line.destination.name}\n经纬: ${line.origin.latitude}, ${line.origin.longitude} → ${line.destination.latitude}, ${line.destination.longitude}\n方位角: ${"%.1f".format(bearing)}°\n24山: $shan\n八卦: $bagua\n五行: $wuxing\n直线距离: ${"%.1f".format(dist)} m"
        showLineInfo = true
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 底层：真实 Google Maps 区域
            GoogleMapView(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f),  // 设置最低层级
                initialZoom = 15f,
                initialCenter = com.google.android.gms.maps.model.LatLng(realGpsLat ?: 39.9042, realGpsLng ?: 116.4074),
                onMapReady = { gMap ->
                    gMapInstance.value = gMap  // ⭐ 保存GoogleMap实例
                    // 将 GoogleMap 对象传递给 mapProvider
                    val provider = mapProvider as? com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
                    provider?.setGoogleMap(gMap)
                    provider?.setOnPolylineClickListener { polyline ->
                        val line = lineByPolylineId[polyline.id]
                        if (line != null) {
                            showLineInfoFor(line)
                        }
                    }
                }
            )
            
            // 连线绘制层（使用Canvas）
            Canvas(modifier = Modifier
                .fillMaxSize()
                .zIndex(0.5f)) {
                // 在这里绘制连线（需要将经纬度转换为屏幕坐标）
                // 暂时在GoogleMapProvider中处理，这里保留备用
            }

            // 连线点击由地图 SDK 回调处理

            // 上层：所有交互元素
            
            // 屏幕中心十字准心
            Box(modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .clickable { showCrossClickDialog = true }  // 点击时显示对话框
                .zIndex(2f)  // 高于地图
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    // 横线
                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 2f
                    )
                    // 竖线
                    drawLine(
                        color = Color.Red,
                        start = Offset(w / 2, 0f),
                        end = Offset(w / 2, h),
                        strokeWidth = 2f
                    )
                }
            ) {}

            // 右侧控制按钮
            Column(modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .zIndex(2f)) {
                // 罗盘锁定/解锁按钮
                Button(onClick = { 
                    if (!compassLocked) {
                        // 切换到锁定模式：保存当前位置
                        val currentPos = mapProvider.getCameraPosition()?.target
                        if (currentPos != null) {
                            lockedLat = currentPos.latitude
                            lockedLng = currentPos.longitude
                            compassLocked = true
                            updateCompassScreenPosition()
                        } else {
                            trialMessage = "无法获取当前位置"
                            showTrialDialog = true
                        }
                    } else {
                        // 切换到解锁模式：清除锁定位置数据
                        compassLocked = false
                        lockedLat = null
                        lockedLng = null
                    }
                }) {
                    Text(if (compassLocked) "🔒 锁定" else "🔓 解锁", fontSize = 12.sp)
                }

                SpacerSmall()
                
                // 定位按钮
                Button(onClick = {
                    // 移动到当前GPS位置
                    if (realGpsLat != null && realGpsLng != null) {
                        mapProvider.animateCamera(
                            com.fengshui.app.map.abstraction.UniversalLatLng(
                                realGpsLat!!,
                                realGpsLng!!
                            ),
                            15f
                        )
                        // 解锁罗盘并清除锁定位置数据
                        compassLocked = false
                        lockedLat = null
                        lockedLng = null
                    } else {
                        trialMessage = "正在获取GPS位置..."
                        showTrialDialog = true
                    }
                }) {
                    Text("📍 定位", fontSize = 12.sp)
                }

                SpacerSmall()

                MapControlButtons(
                    currentMapType = currentMapType,
                    onZoomIn = { mapProvider.zoomIn() },
                    onZoomOut = { mapProvider.zoomOut() },
                    onToggleMapType = { type ->
                        currentMapType = type
                        mapProvider.setMapType(type)
                    }
                )

                SpacerSmall()

                // 案例选择按钮
                Button(onClick = { 
                    showProjectSelectDialog = true 
                }) {
                    Text("📋 案例: ${currentProject?.name ?: "无"}", fontSize = 11.sp)
                }

                SpacerSmall()

                // 原点选择按钮
                Button(onClick = { 
                    if (originPoints.isEmpty()) {
                        trialMessage = "暂无原点，请先添加"
                        showTrialDialog = true
                    } else {
                        showOriginSelectDialog = true
                    }
                }) {
                    Text("📍 原点", fontSize = 12.sp)
                }

                SpacerSmall()

                // Add origin / destination buttons for V0 single-case flow
                Button(onClick = {
                    // 在当前项目中添加原点
                    if (currentProject == null) {
                        trialMessage = "请先选择或创建案例"
                        showTrialDialog = true
                        return@Button
                    }
                    
                    val mapCenter = mapProvider.getCameraPosition()?.target
                    if (mapCenter != null) {
                        scope.launch {
                            try {
                                val p = repo.createPoint(
                                    "原点${originPoints.size + 1}",
                                    mapCenter.latitude,
                                    mapCenter.longitude,
                                    PointType.ORIGIN,
                                    currentProject!!.id  // 关联到当前项目
                                )
                                originPoints.add(p)
                                android.util.Log.d("MapScreen", "Created origin: ${p.name} at (${p.latitude}, ${p.longitude})")
                                
                                // 如果已有终点，自动生成新连线
                                for (dest in destPoints) {
                                    linesList.add(LineData(p, dest))
                                    android.util.Log.d("MapScreen", "Adding line from origin (${p.latitude}, ${p.longitude}) to dest (${dest.latitude}, ${dest.longitude})")
                                    try {
                                        val polyline = mapProvider.addPolyline(
                                            com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude),
                                            com.fengshui.app.map.abstraction.UniversalLatLng(dest.latitude, dest.longitude),
                                            width = 5f,
                                            color = 0xFF0000FF.toInt()
                                        )
                                        lineByPolylineId[polyline.id] = LineData(p, dest)
                                    } catch (polylineEx: Exception) {
                                        android.util.Log.e("MapScreen", "Failed to add polyline: ${polylineEx.message}")
                                    }
                                }
                                
                                // 更新选中的原点
                                selectedOriginPoint = p
                                
                                // 新建原点后自动解锁罗盘
                                compassLocked = false
                                lockedLat = null
                                lockedLng = null
                                mapProvider.animateCamera(
                                    com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude),
                                    15f
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("MapScreen", "Error adding origin point: ${e.message}", e)
                                trialMessage = e.message ?: "添加原点失败"
                                showTrialDialog = true
                            }
                        }
                    }
                }) { Text("➕原点") }

                Button(onClick = {
                    // 在当前项目中添加终点
                    if (currentProject == null) {
                        trialMessage = "请先选择或创建案例"
                        showTrialDialog = true
                        return@Button
                    }
                    
                    val mapCenter = mapProvider.getCameraPosition()?.target
                    if (mapCenter != null) {
                        scope.launch {
                            try {
                                val p = repo.createPoint(
                                    "终点${destPoints.size + 1}",
                                    mapCenter.latitude,
                                    mapCenter.longitude,
                                    PointType.DESTINATION,
                                    currentProject!!.id  // 关联到当前项目
                                )
                                destPoints.add(p)
                                android.util.Log.d("MapScreen", "Created destination: ${p.name} at (${p.latitude}, ${p.longitude})")
                                
                                // 如果已有原点，自动生成新连线
                                for (origin in originPoints) {
                                    linesList.add(LineData(origin, p))
                                    android.util.Log.d("MapScreen", "Adding line from origin (${origin.latitude}, ${origin.longitude}) to dest (${p.latitude}, ${p.longitude})")
                                    try {
                                        val polyline = mapProvider.addPolyline(
                                            com.fengshui.app.map.abstraction.UniversalLatLng(origin.latitude, origin.longitude),
                                            com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude),
                                            width = 5f,
                                            color = 0xFF0000FF.toInt()
                                        )
                                        lineByPolylineId[polyline.id] = LineData(origin, p)
                                    } catch (polylineEx: Exception) {
                                        android.util.Log.e("MapScreen", "Failed to add polyline: ${polylineEx.message}")
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MapScreen", "Error adding destination point: ${e.message}", e)
                                trialMessage = e.message ?: "添加终点失败"
                                showTrialDialog = true
                            }
                        }
                    }
                }) { Text("➕终点") }

                if (originPoint != null && destPoint != null) {
                    SpacerSmall()
                    Button(onClick = {
                        // compute and show line info
                        val bearing = RhumbLineUtils.calculateRhumbBearing(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                        val shan = RhumbLineUtils.getShanName(bearing)
                        val bagua = RhumbLineUtils.getBaGua(bearing)
                        val wuxing = RhumbLineUtils.getWuXing(bearing)
                        val dist = RhumbLineUtils.haversineDistanceMeters(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                        lineInfoText = "原点: ${originPoint!!.name}\n终点: ${destPoint!!.name}\n经纬: ${originPoint!!.latitude}, ${originPoint!!.longitude} → ${destPoint!!.latitude}, ${destPoint!!.longitude}\n方位角: ${"%.1f".format(bearing)}°\n24山: $shan\n八卦: $bagua\n五行: $wuxing\n直线距离: ${"%.1f".format(dist)} m"
                        showLineInfo = true
                    }) { Text("显示连线信息") }
                }
            }

            // Compass overlay
            // 解锁模式：罗盘固定在屏幕中央，跟随GPS位置
            // 锁定模式：罗盘固定在地图上的锁定位置，随地图移动
            
            if (!compassLocked) {
                // 解锁模式：罗盘在屏幕中央，显示当前GPS位置
                if (realGpsLat != null && realGpsLng != null) {
                    Box(modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(3f)) {
                        CompassOverlay(azimuthDegrees = azimuth, latitude = realGpsLat!!, longitude = realGpsLng!!, sizeDp = 220.dp)
                    }
                    
                    // GPS状态指示器
                    if (!hasRealGps) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .background(Color(0xFFFF9800).copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .zIndex(4f)
                        ) {
                            Text(
                                text = "正在定位GPS...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // 锁定模式：罗盘锁定在指定位置，随地图移动
                if (lockedLat != null && lockedLng != null) {
                    // 初始化屏幕位置
                    LaunchedEffect(lockedLat, lockedLng, compassLocked) {
                        updateCompassScreenPosition()
                    }
                    
                    val compassRadiusPx = with(density) { 110.dp.toPx() }  // 罗盘半径
                    
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f)) {
                        Box(modifier = Modifier
                            .offset { 
                                IntOffset(
                                    (compassScreenPos.x - compassRadiusPx).toInt(),
                                    (compassScreenPos.y - compassRadiusPx).toInt()
                                )
                            }) {
                            CompassOverlay(azimuthDegrees = azimuth, latitude = lockedLat!!, longitude = lockedLng!!, sizeDp = 220.dp)
                        }
                    }
                }
            }

            if (showCrossClickDialog) {
                AlertDialog(
                    onDismissRequest = { showCrossClickDialog = false },
                    title = { Text("添加点位") },
                    text = { Text("请选择要添加的点位类型") },
                    confirmButton = {
                        TextButton(onClick = {
                            showCrossClickDialog = false
                            val mapCenter = mapProvider.getCameraPosition()?.target
                            if (mapCenter != null) {
                                scope.launch {
                                    try {
                                        val proj = repo.loadProjects().firstOrNull() ?: repo.createProject("默认案例")
                                        val p = repo.createPoint("原点", mapCenter.latitude, mapCenter.longitude, PointType.ORIGIN, proj.id)
                                        originPoint = p
                                        mapProvider.addMarker(com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude), p.name)
                                        // 新建原点后自动解锁罗盘
                                        compassLocked = false
                                        lockedLat = null
                                        lockedLng = null
                                        if (destPoint != null) {
                                            val polyline = mapProvider.addPolyline(
                                                com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude),
                                                com.fengshui.app.map.abstraction.UniversalLatLng(destPoint!!.latitude, destPoint!!.longitude)
                                            )
                                            lineByPolylineId[polyline.id] = LineData(p, destPoint!!)
                                            lines.add(Pair(p, destPoint!!))
                                        }
                                        mapProvider.animateCamera(com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude), 15f)
                                    } catch (e: com.fengshui.app.TrialLimitException) {
                                        trialMessage = e.message ?: "达到试用限制"
                                        showTrialDialog = true
                                    }
                                }
                            }
                        }) { Text("原点") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCrossClickDialog = false
                            val mapCenter = mapProvider.getCameraPosition()?.target
                            if (mapCenter != null) {
                                scope.launch {
                                    try {
                                        val proj = repo.loadProjects().firstOrNull() ?: repo.createProject("默认案例")
                                        val p = repo.createPoint("终点", mapCenter.latitude, mapCenter.longitude, PointType.DESTINATION, proj.id)
                                        destPoint = p
                                        if (originPoint == null) {
                                            return@launch
                                        }
                                        mapProvider.addMarker(com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude), p.name)
                                        val polyline = mapProvider.addPolyline(
                                            com.fengshui.app.map.abstraction.UniversalLatLng(originPoint!!.latitude, originPoint!!.longitude),
                                            com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude)
                                        )
                                        lineByPolylineId[polyline.id] = LineData(originPoint!!, p)
                                        lines.add(Pair(originPoint!!, p))
                                        // 新建终点后自动解锁罗盘
                                        compassLocked = false
                                        lockedLat = null
                                        lockedLng = null
                                        mapProvider.animateCamera(com.fengshui.app.map.abstraction.UniversalLatLng(originPoint!!.latitude, originPoint!!.longitude), 15f)
                                    } catch (e: com.fengshui.app.TrialLimitException) {
                                        trialMessage = e.message ?: "达到试用限制"
                                        showTrialDialog = true
                                    }
                                }
                            }
                        }) { Text("终点") }
                    }
                )
            }

            if (showLineInfo) {
                AlertDialog(
                    onDismissRequest = { showLineInfo = false },
                    confirmButton = {
                        TextButton(onClick = { showLineInfo = false }) { Text("确定") }
                    },
                    text = { Text(lineInfoText) }
                )
            }

            if (showTrialDialog) {
                AlertDialog(
                    onDismissRequest = { showTrialDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showTrialDialog = false }) { Text("取消") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRegistrationDialog = true
                            showTrialDialog = false
                        }) { Text("注册") }
                    },
                    text = { Text(trialMessage) }
                )
            }

            if (showRegistrationDialog) {
                RegistrationDialog(onDismissRequest = { showRegistrationDialog = false }) { code ->
                    scope.launch {
                        val ok = com.fengshui.app.TrialManager.registerWithCode(context, code)
                        if (ok) {
                            trialMessage = "注册成功，已解锁完整功能"
                            showRegistrationDialog = false
                            showTrialDialog = true
                        } else {
                            trialMessage = "注册码无效"
                            showRegistrationDialog = false
                            showTrialDialog = true
                        }
                    }
                }
            }

            // 案例选择对话框
            if (showProjectSelectDialog && projects.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showProjectSelectDialog = false },
                    title = { Text("选择堪舆案例") },
                    text = {
                        Column {
                            projects.forEach { project ->
                                Text(
                                    text = project.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentProject = project
                                            loadProjectData(project)
                                            showProjectSelectDialog = false
                                        }
                                        .padding(12.dp),
                                    color = if (project.id == currentProject?.id) Color.Blue else Color.Black
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showProjectSelectDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            // 原点选择对话框
            if (showOriginSelectDialog && originPoints.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showOriginSelectDialog = false },
                    title = { Text("选择原点") },
                    text = {
                        Column {
                            originPoints.forEach { point: FengShuiPoint ->
                                Text(
                                    text = "${point.name} (${point.latitude.format(4)}, ${point.longitude.format(4)})",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedOriginPoint = point
                                            // 锁定罗盘到原点位置
                                            lockedLat = point.latitude
                                            lockedLng = point.longitude
                                            compassLocked = true
                                            updateCompassScreenPosition()
                                            mapProvider.animateCamera(
                                                com.fengshui.app.map.abstraction.UniversalLatLng(point.latitude, point.longitude),
                                                15f
                                            )
                                            showOriginSelectDialog = false
                                        }
                                        .padding(12.dp),
                                    color = if (point.id == selectedOriginPoint?.id) Color.Blue else Color.Black
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showOriginSelectDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }
        }
    }
}

// 扩展函数：格式化Double
private fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Composable
private fun SpacerSmall() {
    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.size(8.dp))
}


