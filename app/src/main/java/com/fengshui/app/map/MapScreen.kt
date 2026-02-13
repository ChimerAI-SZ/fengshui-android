package com.fengshui.app.map

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R
import com.fengshui.app.map.ui.CompassOverlay
import com.fengshui.app.map.CompassManager
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType
import com.fengshui.app.data.LifeCircleData
import com.fengshui.app.data.LifeCirclePointType
import com.fengshui.app.data.LifeCircleConnection
import com.fengshui.app.utils.RhumbLineUtils
import com.fengshui.app.utils.ApiKeyConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import android.os.SystemClock
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.fengshui.app.map.abstraction.MapProvider
import com.fengshui.app.map.abstraction.MapProviderType
import com.fengshui.app.map.abstraction.MapType
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.abstraction.MapProviderSelector
import com.fengshui.app.map.ui.MapControlButtons
import com.fengshui.app.map.ui.CrosshairModeUI
import com.fengshui.app.map.ui.LifeCircleOriginSelectDialog
import com.fengshui.app.map.ui.RoleAssignmentDialog
import com.fengshui.app.map.ui.LifeCircleBanner
import com.fengshui.app.map.ui.LifeCircleLabelPanel
import com.fengshui.app.map.ui.SectorConfigDialog
import com.fengshui.app.map.ui.RegistrationDialog
import com.fengshui.app.map.ui.AmapMapViewWrapper
import com.fengshui.app.map.GoogleMapView
import com.fengshui.app.map.LifeCircleUtils
import com.fengshui.app.map.poi.MapPoiProvider
import com.fengshui.app.map.poi.PoiResult
import com.fengshui.app.map.poi.AmapPoiProvider
import com.fengshui.app.map.poi.GooglePlacesProvider
import com.fengshui.app.map.poi.MockPoiProvider
import com.fengshui.app.map.abstraction.amap.AMapProvider

/**
 * 简易 MapScreen 示例：
 * - 占位地图区域（后续替换为真正的 MapView/MapCompose）
 * - 屏幕中心十字准心
 * - 右侧放大/缩小/图层切换控件（使用 `MapControlButtons`）
 */
@Composable
fun MapScreen(
    mapProvider: MapProvider,
    mapProviderType: MapProviderType,
    hasGoogleMap: Boolean,
    hasAmapMap: Boolean,
    onMapProviderSwitch: (MapProviderType) -> Unit,
    modifier: Modifier = Modifier,
    onCenterCrossClicked: (() -> Unit)? = null,
    quickAddCaseId: String? = null,
    onQuickAddConsumed: (() -> Unit)? = null,
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
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // GPS location state - 等待真实GPS定位
    // 默认位置：北京天安门广场 (39.9042, 116.4074)，确保罗盘始终可见
    var realGpsLat by remember { mutableStateOf<Double?>(39.9042) }  // 真实GPS纬度，默认北京
    var realGpsLng by remember { mutableStateOf<Double?>(116.4074) }  // 真实GPS经度，默认北京
    var hasRealGps by remember { mutableStateOf(false) }  // 是否已获取真实GPS
    var azimuth by remember { mutableStateOf(0f) }

    // repository
    val repo = remember { PointRepository(context) }

    val poiProvider: MapPoiProvider = remember {
        val googleKey = ApiKeyConfig.getGoogleMapsApiKey(context)
        val amapKey = ApiKeyConfig.getAmapApiKey(context)
        when {
            ApiKeyConfig.isValidKey(googleKey) -> GooglePlacesProvider(googleKey!!)
            ApiKeyConfig.isValidKey(amapKey) -> AmapPoiProvider(amapKey!!)
            else -> MockPoiProvider()
        }
    }
    
    // 多案例管理
    var projects by remember { mutableStateOf(listOf<com.fengshui.app.data.Project>()) }
    var currentProject by remember { mutableStateOf<com.fengshui.app.data.Project?>(null) }
    val originPoints = remember { mutableStateListOf<FengShuiPoint>() }  // 当前案例的原点列表
    val destPoints = remember { mutableStateListOf<FengShuiPoint>() }    // 当前案例的终点列表
    var selectedOriginPoint by remember { mutableStateOf<FengShuiPoint?>(null) }  // 选中的原点（用于显示连线）
    
    // 连线数据结构：保存原点和终点的配对
    data class LineData(val origin: FengShuiPoint, val destination: FengShuiPoint)
    val linesList = remember { mutableStateListOf<LineData>() }
    var lineRefreshToken by remember { mutableStateOf(0) }
    
    var originPoint by remember { mutableStateOf<FengShuiPoint?>(null) }
    var destPoint by remember { mutableStateOf<FengShuiPoint?>(null) }
    val lines = remember { mutableStateListOf<Pair<FengShuiPoint, FengShuiPoint>>() }
    var showLineInfo by remember { mutableStateOf(false) }
    var lineInfoText by remember { mutableStateOf("") }
    val lineByPolylineId = remember { mutableStateMapOf<String, LineData>() }
    var showTrialDialog by remember { mutableStateOf(false) }
    var trialMessage by remember { mutableStateOf("") }
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var showOriginSelectDialog by remember { mutableStateOf(false) }  // 原点选择对话框
    var showProjectSelectDialog by remember { mutableStateOf(false) }  // 案例选择对话框
    val scope = rememberCoroutineScope()
    var sideBarExpanded by remember { mutableStateOf(false) }
    val sidebarScrollState = rememberScrollState()

    var showAddPointDialog by remember { mutableStateOf(false) }
    var addPointName by remember { mutableStateOf("") }
    var addPointType by remember { mutableStateOf(PointType.ORIGIN) }
    var addPointProjectId by remember { mutableStateOf<String?>(null) }
    var addPointUseNewProject by remember { mutableStateOf(false) }
    var addPointNewProjectName by remember { mutableStateOf("") }
    val viewModel: MapUiStateViewModel = viewModel()
    val ui = viewModel.ui
    val crosshairSearchTitle = stringResource(id = R.string.crosshair_search_title)
    val crosshairSearchSubtitle = stringResource(id = R.string.crosshair_search_subtitle)
    val crosshairManualTitle = stringResource(id = R.string.crosshair_manual_title)
    val crosshairManualSubtitle = stringResource(id = R.string.crosshair_manual_subtitle)
    val crosshairNotLocated = stringResource(id = R.string.crosshair_not_located)
    val caseNotSelected = stringResource(id = R.string.case_not_selected)
    val caseNone = stringResource(id = R.string.case_none)
    val msgNoLocation = stringResource(id = R.string.err_no_location)
    val msgSelectCase = stringResource(id = R.string.err_select_or_create_case)
    val msgAddOriginFailed = stringResource(id = R.string.err_add_origin_failed)
    val msgAddDestinationFailed = stringResource(id = R.string.err_add_destination_failed)
    val msgGpsGetting = stringResource(id = R.string.gps_getting)
    val msgNoOrigins = stringResource(id = R.string.err_no_origin_points)
    val msgNeedThreeOrigins = stringResource(id = R.string.err_need_three_origins)
    val msgSelectOriginFirst = stringResource(id = R.string.err_select_origin_first)
    val msgEnterKeyword = stringResource(id = R.string.err_enter_keyword)
    val msgAddPointFailed = stringResource(id = R.string.err_add_point_failed)
    val msgRegisterSuccess = stringResource(id = R.string.register_success)
    val msgRegisterInvalid = stringResource(id = R.string.register_invalid)
    val msgSelectThreeOrigins = stringResource(id = R.string.err_select_three_origins)
    val msgEnterNewCaseName = stringResource(id = R.string.err_enter_new_case_name)
    val msgGoogleSatelliteFallback = stringResource(id = R.string.google_satellite_fallback_to_amap)
    
    // 地图是否已初始化
    val mapReady = remember { mutableStateOf(false) }

    LaunchedEffect(mapProviderType) {
        mapReady.value = false
        lineByPolylineId.clear()
    }
    
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

    fun requestCameraMove(target: UniversalLatLng, zoom: Float, source: CameraMoveSource) {
        if (viewModel.applyCameraMove(source)) {
            mapProvider.animateCamera(target, zoom)
        }
    }

    fun markUserManualCamera() {
        viewModel.markUserManualCamera()
    }

    fun refreshNormalLines() {
        lineRefreshToken += 1
    }

    fun drawLifeCircleLines(data: LifeCircleData) {
        val provider = mapProvider as? com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
        provider?.clearPolylines()
        lineByPolylineId.clear()

        val pairs = listOf(
            data.homePoint to data.workPoint,
            data.workPoint to data.entertainmentPoint,
            data.entertainmentPoint to data.homePoint
        )

        val colors = listOf(
            0xFF00C853.toInt(),
            0xFF2196F3.toInt(),
            0xFFFF9800.toInt()
        )

        pairs.forEachIndexed { index, pair ->
            val (from, to) = pair
            val polyline = mapProvider.addPolyline(
                com.fengshui.app.map.abstraction.UniversalLatLng(from.latitude, from.longitude),
                com.fengshui.app.map.abstraction.UniversalLatLng(to.latitude, to.longitude),
                width = 6f,
                color = colors[index]
            )
            lineByPolylineId[polyline.id] = LineData(from, to)
        }
    }

    fun activateLifeCircleMode(selectedOrigins: List<FengShuiPoint>, assignments: Map<String, LifeCirclePointType>) {
        val projectId = currentProject?.id ?: ""
        val ok = viewModel.activateLifeCircleMode(selectedOrigins, assignments, projectId)
        if (!ok) {
            trialMessage = msgSelectThreeOrigins
            showTrialDialog = true
            return
        }
        ui.lifeCircleData?.let { drawLifeCircleLines(it) }
    }

    fun exitLifeCircleMode() {
        viewModel.exitLifeCircleMode()
        refreshNormalLines()
    }

    fun clearPoiMarkers() {
        (mapProvider as? com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider)?.clearMarkers()
        (mapProvider as? AMapProvider)?.clearMarkers()
    }

    fun renderPointMarkers(clearExisting: Boolean = true) {
        if (ui.lifeCircleMode) return
        if (clearExisting) {
            (mapProvider as? com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider)?.clearMarkers()
            (mapProvider as? AMapProvider)?.clearMarkers()
        }

        originPoints.forEach { point ->
            try {
                mapProvider.addMarker(
                    UniversalLatLng(point.latitude, point.longitude),
                    context.getString(R.string.marker_origin_prefix, point.name)
                )
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Failed to add origin marker: ${e.message}")
            }
        }

        destPoints.forEach { point ->
            try {
                mapProvider.addMarker(
                    UniversalLatLng(point.latitude, point.longitude),
                    context.getString(R.string.marker_destination_prefix, point.name)
                )
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Failed to add destination marker: ${e.message}")
            }
        }
    }

    fun showPoiMarkers(results: List<PoiResult>) {
        clearPoiMarkers()
        results.forEach { poi ->
            try {
                mapProvider.addMarker(
                    UniversalLatLng(poi.lat, poi.lng),
                    poi.name
                )
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Failed to add POI marker: ${e.message}")
            }
        }
        renderPointMarkers(clearExisting = false)
    }

    fun buildLifeCircleLabels(targetId: String): List<String> {
        return viewModel.buildLifeCircleLabels(targetId)
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

    LaunchedEffect(quickAddCaseId, projects) {
        val targetCaseId = quickAddCaseId ?: return@LaunchedEffect
        val targetProject = projects.firstOrNull { it.id == targetCaseId } ?: return@LaunchedEffect
        currentProject = targetProject
        loadProjectData(targetProject)
        addPointName = ""
        addPointType = PointType.ORIGIN
        addPointProjectId = targetProject.id
        addPointUseNewProject = false
        addPointNewProjectName = ""
        showAddPointDialog = true
        sideBarExpanded = true
        onQuickAddConsumed?.invoke()
    }
    
    // 当linesList改变时，重新绘制所有连线
    LaunchedEffect(linesList.size, lineRefreshToken, ui.lifeCircleMode) {
        if (ui.lifeCircleMode) {
            return@LaunchedEffect
        }
        if (mapReady.value) {
            // 清除旧的polylines
            val provider = mapProvider as? com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
            provider?.clearPolylines()
            val amapProvider = mapProvider as? AMapProvider
            amapProvider?.clearPolylines()
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
            renderPointMarkers(clearExisting = true)
        } else {
            android.util.Log.w("MapScreen", "Map not ready, cannot add polylines")
        }
    }
    
    LaunchedEffect(focusLocation?.latitude, focusLocation?.longitude, mapReady.value) {
        val target = focusLocation
        if (target != null && mapReady.value) {
            viewModel.openCrosshair(crosshairSearchTitle, crosshairSearchSubtitle, target)
            requestCameraMove(target, 16f, CameraMoveSource.SEARCH_RESULT)
            onFocusConsumed?.invoke()
        }
    }
    
    // LocationHelper - 获取真实GPS位置
    val locationHelper = remember(mapProviderType) {
        com.fengshui.app.utils.LocationHelper(context) { lat, lng ->
            realGpsLat = lat
            realGpsLng = lng
            hasRealGps = true  // 标记已获取真实GPS
            // 首次获取GPS位置后，移动地图到当前位置
            if (mapReady.value && !compassLocked && originPoint == null) {
                requestCameraMove(
                    com.fengshui.app.map.abstraction.UniversalLatLng(lat, lng),
                    15f,
                    CameraMoveSource.GPS_AUTO_LOCATE
                )
            }
        }
    }

    // 罗盘显示的坐标（根据锁定状态决定）
    // 已删除旧的 compassLat/compassLng 逻辑，改用 lockedLat/lockedLng

    val compassManager = remember(mapProviderType) {
        CompassManager(context) { lat, lng, deg ->
            // 只更新方向角，位置信息保持不变
            azimuth = deg
        }
    }

    DisposableEffect(mapProviderType) {
        locationHelper.start()  // 启动GPS定位
        compassManager.start()
        
        // 注册地图相机移动监听，用于更新锁定模式下罗盘位置
        mapProvider.onCameraChange {
            val now = SystemClock.elapsedRealtime()
            if (now - ui.lastProgrammaticMoveTimestamp > 700) {
                markUserManualCamera()
            }
            if (compassLocked && lockedLat != null && lockedLng != null) {
                updateCompassScreenPosition()
            }
        }
        mapProvider.onCameraChangeFinish {
            if (compassLocked && lockedLat != null && lockedLng != null) {
                updateCompassScreenPosition()
            }
            if (ui.crosshairMode) {
                viewModel.updateCrosshairLocation(
                    mapProvider.screenLocationToLatLng(
                    screenWidthPx / 2f,
                    screenHeightPx / 2f
                    )
                )
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
        lineInfoText = context.getString(
            R.string.line_info_text,
            line.origin.name,
            line.destination.name,
            line.origin.latitude,
            line.origin.longitude,
            line.destination.latitude,
            line.destination.longitude,
            bearing,
            shan,
            bagua,
            wuxing,
            dist
        )
        showLineInfo = true
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (mapProvider) {
                is com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider -> {
                    GoogleMapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f),
                        initialZoom = 15f,
                        initialCenter = com.google.android.gms.maps.model.LatLng(realGpsLat ?: 39.9042, realGpsLng ?: 116.4074),
                        onMapReady = { gMap ->
                            mapReady.value = true
                            mapProvider.setGoogleMap(gMap)
                            mapProvider.setMapType(currentMapType)
                            mapProvider.setOnPolylineClickListener { polyline ->
                                val line = lineByPolylineId[polyline.id]
                                if (line != null) {
                                    showLineInfoFor(line)
                                }
                            }
                        }
                    )
                }

                is AMapProvider -> {
                    AmapMapViewWrapper(
                        context = context,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f),
                        mapProvider = mapProvider,
                        onMapReady = {
                            mapReady.value = true
                            mapProvider.setMapType(currentMapType)
                        }
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x11000000))
                            .zIndex(0f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Unsupported map provider")
                    }
                }
            }
            
            // 连线绘制层（使用Canvas）
            Canvas(modifier = Modifier
                .fillMaxSize()
                .zIndex(0.5f)) {
                // 在这里绘制连线（需要将经纬度转换为屏幕坐标）
                // 暂时在GoogleMapProvider中处理，这里保留备用
            }

            // 连线点击由地图 SDK 回调处理

            // 上层：所有交互元素
            
            if (ui.crosshairMode) {
                val locationText = ui.crosshairLocation?.let {
                    "${it.latitude.format(6)}, ${it.longitude.format(6)}"
                } ?: crosshairNotLocated

                CrosshairModeUI(
                    title = ui.crosshairTitle,
                    subtitle = ui.crosshairSubtitle.ifBlank { locationText },
                    projectName = currentProject?.name ?: caseNotSelected,
                    isLifeCircleSelection = ui.isLifeCircleSelection,
                    tempViewMode = ui.tempViewMode,
                    onSelectOrigin = {
                        val target = ui.crosshairLocation ?: mapProvider.getCameraPosition()?.target
                        if (target == null) {
                            trialMessage = msgNoLocation
                            showTrialDialog = true
                            return@CrosshairModeUI
                        }
                        if (currentProject == null) {
                            trialMessage = msgSelectCase
                            showTrialDialog = true
                            return@CrosshairModeUI
                        }
                        scope.launch {
                            try {
                                val p = repo.createPoint(
                                    context.getString(R.string.default_origin_name, originPoints.size + 1),
                                    target.latitude,
                                    target.longitude,
                                    PointType.ORIGIN,
                                    currentProject!!.id,
                                    groupName = currentProject!!.name
                                )
                                originPoints.add(p)
                                for (dest in destPoints) {
                                    linesList.add(LineData(p, dest))
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
                                selectedOriginPoint = p
                                compassLocked = false
                                lockedLat = null
                                lockedLng = null
                                requestCameraMove(
                                    com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude),
                                    15f,
                                    CameraMoveSource.USER_POINT_SELECT
                                )
                                ui.crosshairMode = false
                            } catch (e: Exception) {
                                android.util.Log.e("MapScreen", "Error adding origin point: ${e.message}", e)
                                trialMessage = e.message ?: msgAddOriginFailed
                                showTrialDialog = true
                            }
                        }
                    },
                    onSelectDestination = {
                        val target = ui.crosshairLocation ?: mapProvider.getCameraPosition()?.target
                        if (target == null) {
                            trialMessage = msgNoLocation
                            showTrialDialog = true
                            return@CrosshairModeUI
                        }
                        if (currentProject == null) {
                            trialMessage = msgSelectCase
                            showTrialDialog = true
                            return@CrosshairModeUI
                        }
                        scope.launch {
                            try {
                                val p = repo.createPoint(
                                    context.getString(R.string.default_destination_name, destPoints.size + 1),
                                    target.latitude,
                                    target.longitude,
                                    PointType.DESTINATION,
                                    currentProject!!.id,
                                    groupName = currentProject!!.name
                                )
                                destPoints.add(p)
                                for (origin in originPoints) {
                                    linesList.add(LineData(origin, p))
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
                                compassLocked = false
                                lockedLat = null
                                lockedLng = null
                                requestCameraMove(
                                    com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude),
                                    15f,
                                    CameraMoveSource.USER_POINT_SELECT
                                )
                                ui.crosshairMode = false
                            } catch (e: Exception) {
                                android.util.Log.e("MapScreen", "Error adding destination point: ${e.message}", e)
                                trialMessage = e.message ?: msgAddDestinationFailed
                                showTrialDialog = true
                            }
                        }
                    },
                    onCancel = {
                        viewModel.closeCrosshair()
                    }
                )
            } else {
                // 屏幕中心十字准心
                Box(modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clickable {
                        viewModel.openCrosshair(
                            crosshairManualTitle,
                            crosshairManualSubtitle,
                            mapProvider.getCameraPosition()?.target
                        )
                    }
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
            }

            // 右侧侧边栏
            if (sideBarExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1.5f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            sideBarExpanded = false
                        }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 48.dp)
                    .zIndex(2f)
                    .background(Color(0xCCFFFFFF), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                if (!sideBarExpanded) {
                    Button(onClick = { sideBarExpanded = true }) {
                        Text(stringResource(id = R.string.menu_open), fontSize = 12.sp)
                    }
                } else {
                    Button(onClick = { sideBarExpanded = false }) {
                        Text(stringResource(id = R.string.menu_collapse), fontSize = 12.sp)
                    }

                    SpacerSmall()
                    Column(
                        modifier = Modifier
                            .heightIn(max = 520.dp)
                            .verticalScroll(sidebarScrollState)
                    ) {
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
                                    trialMessage = msgNoLocation
                                    showTrialDialog = true
                                }
                            } else {
                                // 切换到解锁模式：清除锁定位置数据
                                compassLocked = false
                                lockedLat = null
                                lockedLng = null
                            }
                        }) {
                            Text(
                                if (compassLocked) {
                                    stringResource(id = R.string.action_unlock)
                                } else {
                                    stringResource(id = R.string.action_lock)
                                },
                                fontSize = 12.sp
                            )
                        }

                        SpacerSmall()
                        
                        // 定位按钮
                        Button(onClick = {
                            // 移动到当前GPS位置
                            if (realGpsLat != null && realGpsLng != null) {
                                requestCameraMove(
                                    com.fengshui.app.map.abstraction.UniversalLatLng(
                                        realGpsLat!!,
                                        realGpsLng!!
                                    ),
                                    15f,
                                    CameraMoveSource.USER_MANUAL
                                )
                                // 解锁罗盘并清除锁定位置数据
                                compassLocked = false
                                lockedLat = null
                                lockedLng = null
                            } else {
                                trialMessage = msgGpsGetting
                                showTrialDialog = true
                            }
                        }) {
                            Text(stringResource(id = R.string.action_locate), fontSize = 12.sp)
                        }

                        SpacerSmall()

                        MapControlButtons(
                            currentMapType = currentMapType,
                            currentProviderType = mapProviderType,
                            hasGoogleMap = hasGoogleMap,
                            hasAmapMap = hasAmapMap,
                            onZoomIn = { mapProvider.zoomIn() },
                            onZoomOut = { mapProvider.zoomOut() },
                            onToggleMapType = { type ->
                                currentMapType = type
                                if (
                                    type == MapType.SATELLITE &&
                                    mapProviderType == MapProviderType.GOOGLE &&
                                    hasAmapMap
                                ) {
                                    val center = mapProvider.getCameraPosition()?.target
                                    val inChina = center?.let {
                                        MapProviderSelector.isInChina(it.latitude, it.longitude)
                                    } == true
                                    if (inChina) {
                                        onMapProviderSwitch(MapProviderType.AMAP)
                                        trialMessage = msgGoogleSatelliteFallback
                                        showTrialDialog = true
                                        return@MapControlButtons
                                    }
                                }
                                mapProvider.setMapType(type)
                            },
                            onSwitchProvider = onMapProviderSwitch
                        )

                        SpacerSmall()

                        // 案例选择按钮
                        Button(onClick = { 
                            showProjectSelectDialog = true 
                        }) {
                            Text(
                                stringResource(
                                    id = R.string.label_case_with_name,
                                    currentProject?.name ?: caseNone
                                ),
                                fontSize = 11.sp
                            )
                        }

                        SpacerSmall()

                        // 原点选择按钮
                        Button(onClick = { 
                            if (originPoints.isEmpty()) {
                                trialMessage = msgNoOrigins
                                showTrialDialog = true
                            } else {
                                showOriginSelectDialog = true
                            }
                        }) {
                            Text(stringResource(id = R.string.action_select_origin), fontSize = 12.sp)
                        }

                        SpacerSmall()

                        Button(onClick = {
                            if (originPoints.size < 3) {
                                trialMessage = msgNeedThreeOrigins
                                showTrialDialog = true
                                return@Button
                            }
                            ui.showLifeCircleSelectDialog = true
                        }) {
                            Text(stringResource(id = R.string.action_life_circle_mode), fontSize = 12.sp)
                        }

                        SpacerSmall()

                        Button(onClick = {
                            if (selectedOriginPoint == null && originPoints.isEmpty()) {
                                trialMessage = msgSelectOriginFirst
                                showTrialDialog = true
                                return@Button
                            }
                            ui.showSectorConfigDialog = true
                        }) {
                            Text(stringResource(id = R.string.action_sector_search), fontSize = 11.sp)
                        }

                        SpacerSmall()

                        Button(onClick = {
                            addPointName = ""
                            addPointType = PointType.ORIGIN
                            addPointProjectId = currentProject?.id
                            addPointUseNewProject = false
                            addPointNewProjectName = ""
                            showAddPointDialog = true
                        }) {
                            Text(stringResource(id = R.string.action_add_point), fontSize = 12.sp)
                        }

                        if (originPoint != null && destPoint != null) {
                            SpacerSmall()
                            Button(onClick = {
                                // compute and show line info
                                val bearing = RhumbLineUtils.calculateRhumbBearing(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                                val shan = RhumbLineUtils.getShanName(bearing)
                                val bagua = RhumbLineUtils.getBaGua(bearing)
                                val wuxing = RhumbLineUtils.getWuXing(bearing)
                                val dist = RhumbLineUtils.haversineDistanceMeters(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                                lineInfoText = context.getString(
                                    R.string.line_info_text,
                                    originPoint!!.name,
                                    destPoint!!.name,
                                    originPoint!!.latitude,
                                    originPoint!!.longitude,
                                    destPoint!!.latitude,
                                    destPoint!!.longitude,
                                    bearing,
                                    shan,
                                    bagua,
                                    wuxing,
                                    dist
                                )
                                showLineInfo = true
                            }) { Text(stringResource(id = R.string.action_show_line_info)) }
                        }
                    }
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
                        .zIndex(1.2f)) {
                        CompassOverlay(
                            azimuthDegrees = azimuth,
                            latitude = realGpsLat!!,
                            longitude = realGpsLng!!,
                            sizeDp = 220.dp,
                            showInfo = false
                        )
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
                                text = stringResource(id = R.string.gps_locating),
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
                        .zIndex(1.2f)) {
                        Box(modifier = Modifier
                            .offset { 
                                IntOffset(
                                    (compassScreenPos.x - compassRadiusPx).toInt(),
                                    (compassScreenPos.y - compassRadiusPx).toInt()
                                )
                            }) {
                            CompassOverlay(
                                azimuthDegrees = azimuth,
                                latitude = lockedLat!!,
                                longitude = lockedLng!!,
                                sizeDp = 220.dp,
                                showInfo = false
                            )
                        }
                    }
                }
            }

            if (ui.lifeCircleMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(4f)
                ) {
                    LifeCircleBanner(
                        onShowInfo = { ui.showLifeCircleInfoDialog = true },
                        onExit = { exitLifeCircleMode() }
                    )
                }

                val data = ui.lifeCircleData
                if (data != null) {
                    val homeLabels = buildLifeCircleLabels(data.homePoint.id)
                    val workLabels = buildLifeCircleLabels(data.workPoint.id)
                    val entertainmentLabels = buildLifeCircleLabels(data.entertainmentPoint.id)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 56.dp)
                            .zIndex(4f)
                    ) {
                        LifeCircleLabelPanel(
                            homeLabels = homeLabels,
                            workLabels = workLabels,
                            entertainmentLabels = entertainmentLabels
                        )
                    }
                }
            }

            if (showLineInfo) {
                AlertDialog(
                    onDismissRequest = { showLineInfo = false },
                    confirmButton = {
                        TextButton(onClick = { showLineInfo = false }) { Text(stringResource(id = R.string.action_confirm)) }
                    },
                    text = { Text(lineInfoText) }
                )
            }

            if (showTrialDialog) {
                AlertDialog(
                    onDismissRequest = { showTrialDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showTrialDialog = false }) { Text(stringResource(id = R.string.action_cancel)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRegistrationDialog = true
                            showTrialDialog = false
                        }) { Text(stringResource(id = R.string.action_register)) }
                    },
                    text = { Text(trialMessage) }
                )
            }

            if (showRegistrationDialog) {
                RegistrationDialog(onDismissRequest = { showRegistrationDialog = false }) { code ->
                    scope.launch {
                        val ok = com.fengshui.app.TrialManager.registerWithCode(context, code)
                        if (ok) {
                            trialMessage = msgRegisterSuccess
                            showRegistrationDialog = false
                            showTrialDialog = true
                        } else {
                            trialMessage = msgRegisterInvalid
                            showRegistrationDialog = false
                            showTrialDialog = true
                        }
                    }
                }
            }

            if (ui.showLifeCircleSelectDialog) {
                LifeCircleOriginSelectDialog(
                    origins = originPoints,
                    onConfirm = { selected ->
                        ui.showLifeCircleSelectDialog = false
                        viewModel.prepareLifeCircleSelection(selected)
                        ui.showRoleAssignmentDialog = true
                    },
                    onDismiss = { ui.showLifeCircleSelectDialog = false }
                )
            }

            if (ui.showRoleAssignmentDialog && ui.pendingLifeCircleOrigins.isNotEmpty()) {
                RoleAssignmentDialog(
                    origins = ui.pendingLifeCircleOrigins,
                    initialAssignments = ui.pendingRoleAssignments,
                    onConfirm = { assignments ->
                        viewModel.cacheLifeCircleAssignments(ui.pendingLifeCircleOrigins, assignments)
                        activateLifeCircleMode(ui.pendingLifeCircleOrigins, assignments)
                        ui.showRoleAssignmentDialog = false
                    },
                    onDismiss = { ui.showRoleAssignmentDialog = false }
                )
            }

            if (ui.showLifeCircleInfoDialog) {
                val infoText = if (ui.lifeCircleConnections.isEmpty()) {
                    stringResource(id = R.string.life_circle_no_data)
                } else {
                    ui.lifeCircleConnections.joinToString("\n") { conn ->
                        "→${conn.fromPoint.name}→ | ${"%.1f".format(conn.bearing)}° | ${conn.shanName} | ${"%.1f".format(conn.distance / 1000f)}km"
                    }
                }
                AlertDialog(
                    onDismissRequest = { ui.showLifeCircleInfoDialog = false },
                    confirmButton = {
                        TextButton(onClick = { ui.showLifeCircleInfoDialog = false }) { Text(stringResource(id = R.string.action_close)) }
                    },
                    text = { Text(infoText) }
                )
            }

            if (ui.showSectorConfigDialog) {
                SectorConfigDialog(
                    onConfirm = { config ->
                        ui.showSectorConfigDialog = false
                        val origin = selectedOriginPoint ?: originPoints.firstOrNull()
                        if (origin == null) {
                            trialMessage = msgSelectOriginFirst
                            showTrialDialog = true
                            return@SectorConfigDialog
                        }
                        if (config.keyword.isBlank()) {
                            trialMessage = msgEnterKeyword
                            showTrialDialog = true
                            return@SectorConfigDialog
                        }

                        ui.sectorOrigin = UniversalLatLng(origin.latitude, origin.longitude)
                        ui.sectorConfigLabel = config.label
                        ui.sectorLoading = true
                        viewModel.runSectorSearch(
                            provider = poiProvider,
                            origin = ui.sectorOrigin!!,
                            config = config,
                            onResult = { results ->
                                showPoiMarkers(results)
                            }
                        )
                    },
                    onDismiss = { ui.showSectorConfigDialog = false }
                )
            }

            if (ui.showSectorResultDialog) {
                AlertDialog(
                    onDismissRequest = { ui.showSectorResultDialog = false },
                    title = {
                        Text(stringResource(id = R.string.sector_result_title, ui.sectorConfigLabel))
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (ui.sectorLoading) {
                                Text(stringResource(id = R.string.sector_search_loading))
                            } else if (ui.sectorResults.isEmpty()) {
                                Text(stringResource(id = R.string.sector_no_results))
                            } else {
                                ui.sectorNoticeCount?.let { count ->
                                    Text(
                                        stringResource(id = R.string.sector_notice, count),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                ui.sectorResults.forEach { poi ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(poi.name)
                                            Text(poi.address ?: "", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Button(onClick = {
                                            val target = UniversalLatLng(poi.lat, poi.lng)
                                            requestCameraMove(target, 16f, CameraMoveSource.USER_POINT_SELECT)
                                        }) {
                                            Text(stringResource(id = R.string.action_locate_short))
                                        }
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Button(onClick = {
                                            if (currentProject == null) {
                                                trialMessage = msgSelectCase
                                                showTrialDialog = true
                                                return@Button
                                            }
                                            scope.launch {
                                                try {
                                                    val p = repo.createPoint(
                                                        poi.name,
                                                        poi.lat,
                                                        poi.lng,
                                                        PointType.DESTINATION,
                                                        currentProject!!.id,
                                                        address = poi.address,
                                                        groupName = currentProject!!.name
                                                    )
                                                    destPoints.add(p)
                                                    for (origin in originPoints) {
                                                        linesList.add(LineData(origin, p))
                                                    }
                                                } catch (e: Exception) {
                                                    trialMessage = e.message ?: msgAddDestinationFailed
                                                    showTrialDialog = true
                                                }
                                            }
                                        }) {
                                            Text(stringResource(id = R.string.action_save))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { ui.showSectorResultDialog = false }) { Text(stringResource(id = R.string.action_close)) }
                    }
                )
            }

            if (showAddPointDialog) {
                AlertDialog(
                    onDismissRequest = { showAddPointDialog = false },
                    title = { Text(stringResource(id = R.string.add_point_title)) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = addPointName,
                                onValueChange = { addPointName = it },
                                label = { Text(stringResource(id = R.string.add_point_name_label)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            SpacerSmall()

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { addPointType = PointType.ORIGIN }) {
                                    Text(
                                        if (addPointType == PointType.ORIGIN) {
                                            stringResource(id = R.string.point_type_origin_checked)
                                        } else {
                                            stringResource(id = R.string.point_type_origin)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Button(onClick = { addPointType = PointType.DESTINATION }) {
                                    Text(
                                        if (addPointType == PointType.DESTINATION) {
                                            stringResource(id = R.string.point_type_destination_checked)
                                        } else {
                                            stringResource(id = R.string.point_type_destination)
                                        }
                                    )
                                }
                            }

                            SpacerSmall()

                            Text(stringResource(id = R.string.select_case_title_short), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            projects.forEach { project ->
                                Text(
                                    text = project.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            addPointProjectId = project.id
                                            addPointUseNewProject = false
                                        }
                                        .padding(8.dp),
                                    color = if (project.id == addPointProjectId) Color.Blue else Color.Black
                                )
                            }

                            SpacerSmall()

                            Button(onClick = { addPointUseNewProject = !addPointUseNewProject }) {
                                Text(
                                    if (addPointUseNewProject) {
                                        stringResource(id = R.string.action_cancel_new_case)
                                    } else {
                                        stringResource(id = R.string.action_create_new_case)
                                    }
                                )
                            }

                            if (addPointUseNewProject) {
                                SpacerSmall()
                                OutlinedTextField(
                                    value = addPointNewProjectName,
                                    onValueChange = { addPointNewProjectName = it },
                                    label = { Text(stringResource(id = R.string.label_new_case_name)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val mapCenter = mapProvider.getCameraPosition()?.target
                            if (mapCenter == null) {
                                trialMessage = msgNoLocation
                                showTrialDialog = true
                                return@Button
                            }

                            scope.launch {
                                try {
                                    var project = currentProject
                                    if (addPointUseNewProject) {
                                        if (addPointNewProjectName.isBlank()) {
                                            trialMessage = msgEnterNewCaseName
                                            showTrialDialog = true
                                            return@launch
                                        }
                                        project = repo.createProject(addPointNewProjectName.trim())
                                        projects = repo.loadProjects()
                                        currentProject = project
                                        loadProjectData(project)
                                    } else {
                                        val targetId = addPointProjectId
                                        project = projects.firstOrNull { it.id == targetId } ?: currentProject
                                    }

                                    if (project == null) {
                                        trialMessage = msgSelectCase
                                        showTrialDialog = true
                                        return@launch
                                    }

                                    val defaultName = if (addPointType == PointType.ORIGIN) {
                                        context.getString(R.string.default_origin_name, originPoints.size + 1)
                                    } else {
                                        context.getString(R.string.default_destination_name, destPoints.size + 1)
                                    }
                                    val name = addPointName.trim().ifBlank { defaultName }

                                    val p = repo.createPoint(
                                        name,
                                        mapCenter.latitude,
                                        mapCenter.longitude,
                                        addPointType,
                                        project.id,
                                        groupName = project.name
                                    )

                                    if (addPointType == PointType.ORIGIN) {
                                        originPoints.add(p)
                                        for (dest in destPoints) {
                                            linesList.add(LineData(p, dest))
                                        }
                                        selectedOriginPoint = p
                                    } else {
                                        destPoints.add(p)
                                        for (origin in originPoints) {
                                            linesList.add(LineData(origin, p))
                                        }
                                    }

                                    compassLocked = false
                                    lockedLat = null
                                    lockedLng = null
                                    requestCameraMove(
                                        UniversalLatLng(p.latitude, p.longitude),
                                        15f,
                                        CameraMoveSource.USER_POINT_SELECT
                                    )
                                    sideBarExpanded = false

                                    showAddPointDialog = false
                                } catch (e: Exception) {
                                    trialMessage = e.message ?: msgAddPointFailed
                                    showTrialDialog = true
                                }
                            }
                        }) { Text(stringResource(id = R.string.action_save)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPointDialog = false }) { Text(stringResource(id = R.string.action_cancel)) }
                    }
                )
            }

            // 案例选择对话框
            if (showProjectSelectDialog && projects.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showProjectSelectDialog = false },
                    title = { Text(stringResource(id = R.string.select_case_title)) },
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
                            Text(stringResource(id = R.string.action_cancel))
                        }
                    }
                )
            }

            // 原点选择对话框
            if (showOriginSelectDialog && originPoints.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showOriginSelectDialog = false },
                    title = { Text(stringResource(id = R.string.select_origin_title)) },
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
                                            requestCameraMove(
                                                com.fengshui.app.map.abstraction.UniversalLatLng(point.latitude, point.longitude),
                                                15f,
                                                CameraMoveSource.USER_POINT_SELECT
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
                            Text(stringResource(id = R.string.action_close))
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


