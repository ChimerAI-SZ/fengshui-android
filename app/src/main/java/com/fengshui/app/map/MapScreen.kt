package com.fengshui.app.map

import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import androidx.core.content.ContextCompat
import com.fengshui.app.R
import com.fengshui.app.map.ui.CompassOverlay
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType
import com.fengshui.app.data.LifeCircleData
import com.fengshui.app.data.LifeCirclePointType
import com.fengshui.app.data.LifeCircleConnection
import com.fengshui.app.utils.RhumbLineUtils
import com.fengshui.app.utils.ApiKeyConfig
import com.fengshui.app.utils.Prefs
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.os.SystemClock
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fengshui.app.map.abstraction.MapProvider
import com.fengshui.app.map.abstraction.MapProviderType
import com.fengshui.app.map.abstraction.MapType
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.abstraction.CameraPosition
import com.fengshui.app.map.abstraction.MapProviderSelector
import com.fengshui.app.map.ui.MapControlButtons
import com.fengshui.app.map.ui.CrosshairModeUI
import com.fengshui.app.map.ui.LifeCircleOriginSelectDialog
import com.fengshui.app.map.ui.RoleAssignmentDialog
import com.fengshui.app.map.ui.LifeCircleBanner
import com.fengshui.app.map.ui.LifeCircleLabelPanel
import com.fengshui.app.map.ui.SectorConfigDialog
import com.fengshui.app.map.ui.SectorConfig
import com.fengshui.app.map.ui.RegistrationDialog
import com.fengshui.app.map.ui.MultiSelectDestinationDialog
import com.fengshui.app.map.ui.AmapMapViewWrapper
import com.fengshui.app.map.ui.ArCompassOverlay
import com.fengshui.app.map.GoogleMapView
import com.fengshui.app.map.LifeCircleUtils
import com.fengshui.app.map.poi.MapPoiProvider
import com.fengshui.app.map.poi.PoiResult
import com.fengshui.app.map.poi.AmapPoiProvider
import com.fengshui.app.map.poi.GooglePlacesProvider
import com.fengshui.app.map.poi.MockPoiProvider
import com.fengshui.app.map.poi.NominatimPoiProvider
import com.fengshui.app.map.poi.PoiTypeMapper
import com.fengshui.app.map.abstraction.amap.AMapProvider
import java.util.Locale

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
    onMapProviderSwitch: (MapProviderType, CameraPosition?) -> Unit,
    restoreCameraPosition: CameraPosition? = null,
    onRestoreCameraConsumed: (() -> Unit)? = null,
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

    val googlePoiProvider: MapPoiProvider? = remember {
        val googleKey = ApiKeyConfig.getGooglePlacesApiKey(context)
        if (ApiKeyConfig.isValidKey(googleKey)) GooglePlacesProvider(googleKey!!) else null
    }
    val amapPoiProvider: MapPoiProvider? = remember {
        val amapKey = ApiKeyConfig.getAmapWebApiKey(context)
        if (ApiKeyConfig.isValidKey(amapKey)) AmapPoiProvider(amapKey!!) else null
    }
    val nominatimPoiProvider: MapPoiProvider = remember { NominatimPoiProvider() }
    val mockPoiProvider: MapPoiProvider = remember { MockPoiProvider() }
    val isChinaLocale = remember { Locale.getDefault().country.equals("CN", ignoreCase = true) }

    fun buildPoiProviderChain(keyword: String): List<MapPoiProvider> {
        val hasChineseChars = keyword.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
        val isTypedKeyword = PoiTypeMapper.isTypedCategoryKeyword(keyword)
        return buildList {
            if (isTypedKeyword) {
                // Strict type-search mode: use map typed endpoints only, no text-shape fallback providers.
                if (mapProviderType == MapProviderType.AMAP) {
                    amapPoiProvider?.let { add(it) }
                    googlePoiProvider?.let { add(it) }
                } else {
                    googlePoiProvider?.let { add(it) }
                    amapPoiProvider?.let { add(it) }
                }
                return@buildList
            }
            if (mapProviderType == MapProviderType.AMAP || isChinaLocale || hasChineseChars) {
                amapPoiProvider?.let { add(it) }
            }
            if (mapProviderType == MapProviderType.GOOGLE) {
                googlePoiProvider?.let { add(it) }
            } else {
                googlePoiProvider?.let { add(it) }
            }
            if (mapProviderType != MapProviderType.AMAP && !isChinaLocale && !hasChineseChars) {
                amapPoiProvider?.let { add(it) }
            }
            add(nominatimPoiProvider)
            add(mockPoiProvider)
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
    var lineInfoSummary by remember { mutableStateOf("") }
    var lineInfoDetail by remember { mutableStateOf("") }
    var lineInfoExpanded by remember { mutableStateOf(false) }
    val lineByPolylineId = remember { mutableStateMapOf<String, LineData>() }
    var showTrialDialog by remember { mutableStateOf(false) }
    var trialMessage by remember { mutableStateOf("") }
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var showOriginSelectDialog by remember { mutableStateOf(false) }  // 原点选择对话框
    var showDestinationSelectDialog by remember { mutableStateOf(false) }
    var showOriginAfterDestinationDialog by remember { mutableStateOf(false) }
    var showProjectSelectDialog by remember { mutableStateOf(false) }  // 案例选择对话框
    val selectedDestinationIds = remember { mutableStateListOf<String>() }
    val pendingDestinationIds = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var sideBarExpanded by remember { mutableStateOf(false) }
    val sidebarScrollState = rememberScrollState()
    var arCompassEnabled by remember { mutableStateOf(false) }
    val destinationColorIndexById = remember { mutableStateMapOf<String, Int>() }
    val poiByMarkerId = remember { mutableMapOf<String, PoiResult>() }
    var selectedPoiDetail by remember { mutableStateOf<PoiResult?>(null) }
    var showPoiDetailDialog by remember { mutableStateOf(false) }
    var pendingSectorLocatePoi by remember { mutableStateOf<PoiResult?>(null) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var statusBannerToken by remember { mutableStateOf(0) }
    var showSectorUnsavedOnly by remember { mutableStateOf(false) }
    var showFirstUseGuide by remember { mutableStateOf(false) }
    var deletedPointUndoCandidate by remember { mutableStateOf<FengShuiPoint?>(null) }

    var showAddPointDialog by remember { mutableStateOf(false) }
    var addPointName by remember { mutableStateOf("") }
    var addPointType by remember { mutableStateOf(PointType.ORIGIN) }
    var addPointProjectId by remember { mutableStateOf<String?>(null) }
    var addPointUseNewProject by remember { mutableStateOf(false) }
    var addPointNewProjectName by remember { mutableStateOf("") }
    var continuousAddMode by remember { mutableStateOf(false) }
    var continuousAddType by remember { mutableStateOf(PointType.ORIGIN) }
    var showContinuousNameDialog by remember { mutableStateOf(false) }
    var continuousPointName by remember { mutableStateOf("") }
    var continuousPendingType by remember { mutableStateOf(PointType.ORIGIN) }
    var continuousPendingTarget by remember { mutableStateOf<UniversalLatLng?>(null) }
    var showPostSaveQuickActions by remember { mutableStateOf(false) }
    var lastAddedPoint by remember { mutableStateOf<FengShuiPoint?>(null) }
    var lastAddedPointType by remember { mutableStateOf(PointType.ORIGIN) }
    var sectionMapToolsExpanded by remember { mutableStateOf(false) }
    var sectionCaseExpanded by remember { mutableStateOf(false) }
    var sectionAnalysisExpanded by remember { mutableStateOf(false) }
    var subMapCompassExpanded by remember { mutableStateOf(false) }
    var subCaseSelectionExpanded by remember { mutableStateOf(false) }
    var subCaseEditExpanded by remember { mutableStateOf(false) }
    var subAnalysisCoreExpanded by remember { mutableStateOf(false) }
    var lifeCircleWizardStep by remember { mutableStateOf(0) } // 0 none, 1 home, 2 work, 3 entertainment
    var lifeCircleHomeId by remember { mutableStateOf<String?>(null) }
    var lifeCircleWorkId by remember { mutableStateOf<String?>(null) }
    var sectorSortByDistance by remember { mutableStateOf(true) }
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
    val msgNoDestinations = stringResource(id = R.string.no_destination_tip)
    val msgNeedThreeOrigins = stringResource(id = R.string.err_need_three_origins)
    val msgAddPointFailed = stringResource(id = R.string.err_add_point_failed)
    val msgRegisterSuccess = stringResource(id = R.string.register_success)
    val msgRegisterInvalid = stringResource(id = R.string.register_invalid)
    val msgSelectThreeOrigins = stringResource(id = R.string.err_select_three_origins)
    val msgEnterNewCaseName = stringResource(id = R.string.err_enter_new_case_name)
    val msgGoogleSatelliteFallback = stringResource(id = R.string.google_satellite_fallback_to_amap)
    val msgSectorNoKeywordDrawOnly = stringResource(id = R.string.sector_draw_only_notice)
    val msgSectorRadiusLimited = stringResource(id = R.string.sector_poi_radius_limited_notice)
    val msgSectorFallbackNearby = stringResource(id = R.string.sector_fallback_nearby_notice)
    val msgSectorSearchFailed = stringResource(id = R.string.sector_search_failed)
    val msgSectorFromMapCenter = stringResource(id = R.string.sector_origin_map_center_notice)
    val msgSectorSortByDistance = stringResource(id = R.string.sector_sort_distance)
    val msgArPermissionDenied = stringResource(id = R.string.ar_compass_permission_denied)
    val msgArOpenFailed = stringResource(id = R.string.ar_compass_open_failed)
    val actionArCompass = stringResource(id = R.string.action_ar_compass)

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            arCompassEnabled = true
        } else {
            trialMessage = msgArPermissionDenied
            showTrialDialog = true
        }
    }
    val msgSectorSortByName = stringResource(id = R.string.sector_sort_name)
    val msgPostSavedPoint = stringResource(id = R.string.post_save_point_saved)
    val msgPostSavedOrigin = stringResource(id = R.string.point_type_origin)
    val msgPostSavedDestination = stringResource(id = R.string.point_type_destination)
    val msgUndo = stringResource(id = R.string.action_undo)
    val msgContinueAdd = stringResource(id = R.string.action_continue_add)
    val msgContinueAddHint = stringResource(id = R.string.crosshair_continue_add_subtitle)
    val sectionMapTools = stringResource(id = R.string.section_map_tools)
    val sectionCaseOps = stringResource(id = R.string.section_case_ops)
    val sectionAnalysis = stringResource(id = R.string.section_analysis)
    val actionContinuousAddModeOn = stringResource(id = R.string.action_continuous_add_on)
    val actionContinuousAddModeOff = stringResource(id = R.string.action_continuous_add_off)
    val actionContinuousCurrentType = stringResource(
        id = R.string.action_continuous_current_type,
        if (continuousAddType == PointType.ORIGIN) msgPostSavedOrigin else msgPostSavedDestination
    )
    val lifeCircleStepHome = stringResource(id = R.string.life_circle_step_home)
    val lifeCircleStepWork = stringResource(id = R.string.life_circle_step_work)
    val lifeCircleStepEntertainment = stringResource(id = R.string.life_circle_step_entertainment)
    val lifeCircleStepHint = stringResource(id = R.string.life_circle_step_hint)
    val actionSelectDestination = stringResource(id = R.string.action_select_destination)
    val continuousAddNameTitle = stringResource(id = R.string.continuous_add_name_title)
    
    // 地图是否已初始化
    val mapReady = remember { mutableStateOf(false) }
    var lastProviderType by remember { mutableStateOf<MapProviderType?>(null) }

    fun openSidebarCollapsed() {
        sectionMapToolsExpanded = false
        sectionCaseExpanded = false
        sectionAnalysisExpanded = false
        subMapCompassExpanded = false
        subCaseSelectionExpanded = false
        subCaseEditExpanded = false
        subAnalysisCoreExpanded = false
        sideBarExpanded = true
    }

    fun showStatus(message: String) {
        statusBannerMessage = message
        statusBannerToken += 1
    }

    LaunchedEffect(mapProviderType) {
        if (lastProviderType != null && lastProviderType != mapProviderType) {
            mapReady.value = false
        }
        lastProviderType = mapProviderType
        lineByPolylineId.clear()
    }

    LaunchedEffect(statusBannerToken) {
        if (statusBannerMessage != null) {
            delay(2000)
            statusBannerMessage = null
        }
    }

    LaunchedEffect(Unit) {
        val seenGuide = Prefs.getBoolean(context, "map_first_guide_seen", false)
        if (!seenGuide) {
            showFirstUseGuide = true
            Prefs.saveBoolean(context, "map_first_guide_seen", true)
        }
    }

    LaunchedEffect(deletedPointUndoCandidate?.id) {
        if (deletedPointUndoCandidate != null) {
            delay(5000)
            deletedPointUndoCandidate = null
        }
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
                
                selectedDestinationIds.clear()
                selectedOriginPoint = originPoints.firstOrNull()
                linesList.clear()
                val origin = selectedOriginPoint
                if (origin != null) {
                    destPoints.forEach { dest ->
                        linesList.add(LineData(origin, dest))
                    }
                    lockedLat = origin.latitude
                    lockedLng = origin.longitude
                    compassLocked = true
                    try {
                        val screenPos = mapProvider.latLngToScreenLocation(
                            UniversalLatLng(origin.latitude, origin.longitude)
                        )
                        compassScreenPos = Offset(screenPos.x, screenPos.y)
                    } catch (_: Exception) {
                        compassScreenPos = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
                    }
                    if (mapReady.value) {
                        mapProvider.animateCamera(
                            UniversalLatLng(origin.latitude, origin.longitude),
                            15f
                        )
                    }
                } else {
                    compassLocked = false
                    lockedLat = null
                    lockedLng = null
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
        poiByMarkerId.clear()
    }

    fun renderPointMarkers(clearExisting: Boolean = true) {
        if (ui.lifeCircleMode) return
        if (clearExisting) {
            (mapProvider as? com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider)?.clearMarkers()
            (mapProvider as? AMapProvider)?.clearMarkers()
        }

        originPoints.forEach { point ->
            try {
                val isActive = selectedOriginPoint?.id == point.id
                mapProvider.addMarker(
                    UniversalLatLng(point.latitude, point.longitude),
                    if (isActive) {
                        "[ORIGIN_ACTIVE] ${context.getString(R.string.marker_origin_prefix, point.name)}"
                    } else {
                        context.getString(R.string.marker_origin_prefix, point.name)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Failed to add origin marker: ${e.message}")
            }
        }

        destPoints.forEach { point ->
            try {
                val colorIndex = destinationColorIndexById[point.id]
                val title = if (colorIndex != null) {
                    "[DEST_C${colorIndex}] ${context.getString(R.string.marker_destination_prefix, point.name)}"
                } else {
                    context.getString(R.string.marker_destination_prefix, point.name)
                }
                mapProvider.addMarker(
                    UniversalLatLng(point.latitude, point.longitude),
                    title
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
                val marker = mapProvider.addMarker(
                    UniversalLatLng(poi.lat, poi.lng),
                    "[POI] ${poi.name}"
                )
                poiByMarkerId[marker.id] = poi
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Failed to add POI marker: ${e.message}")
            }
        }
        renderPointMarkers(clearExisting = false)
    }

    fun clearSectorArtifacts() {
        ui.sectorOverlayVisible = false
        ui.sectorResults.clear()
        ui.showSectorResultDialog = false
        pendingSectorLocatePoi = null
        showPoiMarkers(emptyList())
    }

    fun focusOnSectorResults(results: List<PoiResult>) {
        if (results.isEmpty()) return
        if (results.size == 1) {
            requestCameraMove(UniversalLatLng(results[0].lat, results[0].lng), 16f, CameraMoveSource.SEARCH_RESULT)
            return
        }
        val minLat = results.minOf { it.lat }
        val maxLat = results.maxOf { it.lat }
        val minLng = results.minOf { it.lng }
        val maxLng = results.maxOf { it.lng }
        val latPad = ((maxLat - minLat) * 0.15).coerceAtLeast(0.002)
        val lngPad = ((maxLng - minLng) * 0.15).coerceAtLeast(0.002)
        mapProvider.animateCameraToBounds(
            com.fengshui.app.map.abstraction.UniversalLatLngBounds(
                southwest = UniversalLatLng(minLat - latPad, minLng - lngPad),
                northeast = UniversalLatLng(maxLat + latPad, maxLng + lngPad)
            ),
            padding = 120
        )
    }

    fun isPoiAlreadySaved(poi: PoiResult): Boolean {
        return destPoints.any { p ->
            RhumbLineUtils.calculateRhumbDistance(
                UniversalLatLng(p.latitude, p.longitude),
                UniversalLatLng(poi.lat, poi.lng)
            ) <= 15.0
        }
    }

    fun buildLifeCircleLabels(targetId: String): List<String> {
        return viewModel.buildLifeCircleLabels(targetId)
    }

    fun refreshLinesForDisplay() {
        linesList.clear()
        destinationColorIndexById.clear()
        val origin = selectedOriginPoint ?: originPoints.firstOrNull()
        if (origin == null) {
            android.util.Log.d("MapScreen", "refreshLinesForDisplay skipped: no origin in current case")
            renderPointMarkers(clearExisting = true)
            return
        }
        var activeDestinations = if (selectedDestinationIds.isEmpty()) {
            destPoints
        } else {
            destPoints.filter { selectedDestinationIds.contains(it.id) }
        }
        if (activeDestinations.isEmpty() && destPoints.isNotEmpty()) {
            selectedDestinationIds.clear()
            activeDestinations = destPoints
        }
        activeDestinations.forEachIndexed { index, dest ->
            destinationColorIndexById[dest.id] = index % 5
            linesList.add(LineData(origin, dest))
        }
        android.util.Log.d(
            "MapScreen",
            "refreshLinesForDisplay case=${currentProject?.name} origin=${origin.name} destCount=${activeDestinations.size} lineCount=${linesList.size}"
        )
        renderPointMarkers(clearExisting = true)
    }

    fun onPointAdded(point: FengShuiPoint, type: PointType) {
        lastAddedPoint = point
        lastAddedPointType = type
        showStatus(
            if (type == PointType.ORIGIN) "已添加原点：${point.name}" else "已添加终点：${point.name}"
        )
        if (continuousAddMode) {
            showPostSaveQuickActions = false
            viewModel.openCrosshair(
                crosshairManualTitle,
                msgContinueAddHint,
                mapProvider.getCameraPosition()?.target
            )
        } else {
            showPostSaveQuickActions = true
        }
    }

    fun activeOriginPoint(): FengShuiPoint? {
        return selectedOriginPoint ?: originPoints.firstOrNull()
    }

    fun lockCompassToLatLng(lat: Double, lng: Double) {
        lockedLat = lat
        lockedLng = lng
        compassLocked = true
        try {
            val screenPos = mapProvider.latLngToScreenLocation(
                UniversalLatLng(lat, lng)
            )
            compassScreenPos = Offset(screenPos.x, screenPos.y)
        } catch (_: Exception) {
            compassScreenPos = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
        }
    }

    fun lockCompassToPoint(point: FengShuiPoint) {
        lockCompassToLatLng(point.latitude, point.longitude)
    }

    fun unlockCompass() {
        compassLocked = false
        lockedLat = null
        lockedLng = null
    }

    fun saveCrosshairPoint(pointType: PointType, target: UniversalLatLng, enteredName: String? = null) {
        val project = currentProject
        if (project == null) {
            trialMessage = msgSelectCase
            showTrialDialog = true
            return
        }
        scope.launch {
            try {
                val defaultName = if (pointType == PointType.ORIGIN) {
                    context.getString(R.string.default_origin_name, originPoints.size + 1)
                } else {
                    context.getString(R.string.default_destination_name, destPoints.size + 1)
                }
                val name = enteredName?.trim().orEmpty().ifBlank { defaultName }
                val p = repo.createPoint(
                    name,
                    target.latitude,
                    target.longitude,
                    pointType,
                    project.id,
                    groupName = project.name
                )
                if (pointType == PointType.ORIGIN) {
                    originPoints.add(p)
                    selectedOriginPoint = p
                    selectedDestinationIds.clear()
                } else {
                    destPoints.add(p)
                    selectedDestinationIds.clear()
                }
                refreshLinesForDisplay()
                if (pointType == PointType.ORIGIN) {
                    // 文档交互：新增原点后，罗盘锁定在原点并以原点为中心。
                    lockCompassToPoint(p)
                    requestCameraMove(
                        UniversalLatLng(p.latitude, p.longitude),
                        15f,
                        CameraMoveSource.USER_POINT_SELECT
                    )
                } else {
                    // 文档交互：新增终点后，若存在原点则回到原点并锁定罗盘；若无原点仅保存终点，不强制改视角。
                    val origin = activeOriginPoint()
                    if (origin != null) {
                        lockCompassToPoint(origin)
                        requestCameraMove(
                            UniversalLatLng(origin.latitude, origin.longitude),
                            15f,
                            CameraMoveSource.USER_POINT_SELECT
                        )
                    }
                }
                onPointAdded(p, pointType)
                if (!continuousAddMode) {
                    ui.crosshairMode = false
                }
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Error adding point: ${e.message}", e)
                trialMessage = e.message ?: if (pointType == PointType.ORIGIN) msgAddOriginFailed else msgAddDestinationFailed
                showTrialDialog = true
            }
        }
    }

    fun removePointAndRefresh(point: FengShuiPoint) {
        deletedPointUndoCandidate = point
        repo.deletePoint(point.id)
        if (point.type == PointType.ORIGIN) {
            originPoints.removeAll { it.id == point.id }
            linesList.removeAll { it.origin.id == point.id }
            if (selectedOriginPoint?.id == point.id) {
                selectedOriginPoint = originPoints.firstOrNull()
            }
        } else {
            destPoints.removeAll { it.id == point.id }
            linesList.removeAll { it.destination.id == point.id }
            selectedDestinationIds.removeAll { it == point.id }
        }
        refreshLinesForDisplay()
        showStatus("${point.name} 已删除，可撤销")
    }

    fun saveLifeCircleWizardState() {
        val payload = "${currentProject?.id.orEmpty()}|$lifeCircleWizardStep|${lifeCircleHomeId.orEmpty()}|${lifeCircleWorkId.orEmpty()}"
        Prefs.saveString(context, "life_circle_wizard_state", payload)
    }

    fun clearLifeCircleWizardState() {
        Prefs.saveString(context, "life_circle_wizard_state", "")
    }
    
    // 更新罗盘在屏幕上的位置（锁定模式下使用）
    fun updateCompassScreenPosition() {
        if (compassLocked && lockedLat != null && lockedLng != null) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastCompassUpdateMs < 16) {
                return
            }
            lastCompassUpdateMs = now
            try {
                val screenPos = mapProvider.latLngToScreenLocation(
                    com.fengshui.app.map.abstraction.UniversalLatLng(
                        lockedLat!!,
                        lockedLng!!
                    )
                )
                compassScreenPos = Offset(screenPos.x, screenPos.y)
            } catch (_: Exception) {
                compassScreenPos = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
            }
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
            Prefs.getString(context, "life_circle_wizard_state")
                ?.takeIf { it.isNotBlank() }
                ?.split("|")
                ?.let { parts ->
                    if (parts.size >= 4) {
                        val projectId = parts[0]
                        val step = parts[1].toIntOrNull() ?: 0
                        if (projectId.isBlank() || projectId == currentProject?.id) {
                            lifeCircleWizardStep = step.coerceIn(0, 3)
                            lifeCircleHomeId = parts[2].ifBlank { null }
                            lifeCircleWorkId = parts[3].ifBlank { null }
                        }
                    }
                }
        }
    }

    LaunchedEffect(lifeCircleWizardStep, lifeCircleHomeId, lifeCircleWorkId, currentProject?.id) {
        if (lifeCircleWizardStep == 0) {
            clearLifeCircleWizardState()
        } else {
            saveLifeCircleWizardState()
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
        openSidebarCollapsed()
        onQuickAddConsumed?.invoke()
    }
    
    // 当linesList改变时，重新绘制所有连线
    LaunchedEffect(linesList.size, lineRefreshToken, ui.lifeCircleMode, mapReady.value, mapProviderType) {
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
                    val lineColor = when (destinationColorIndexById[line.destination.id] ?: 0) {
                        0 -> 0xFFE53935.toInt() // red
                        1 -> 0xFF1E88E5.toInt() // blue
                        2 -> 0xFF43A047.toInt() // green
                        3 -> 0xFFFB8C00.toInt() // orange
                        else -> 0xFF8E24AA.toInt() // purple
                    }
                    val polyline = mapProvider.addPolyline(
                        com.fengshui.app.map.abstraction.UniversalLatLng(line.origin.latitude, line.origin.longitude),
                        com.fengshui.app.map.abstraction.UniversalLatLng(line.destination.latitude, line.destination.longitude),
                        width = 5f,
                        color = lineColor
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

    LaunchedEffect(currentProject?.id, originPoints.size, destPoints.size, mapReady.value, mapProviderType, ui.lifeCircleMode) {
        if (!ui.lifeCircleMode && mapReady.value) {
            refreshLinesForDisplay()
            refreshNormalLines()
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

    DisposableEffect(mapProviderType) {
        locationHelper.start()  // 启动GPS定位
        val initBearing = mapProvider.getCameraPosition()?.bearing ?: 0f
        azimuth = if (mapProviderType == MapProviderType.GOOGLE) -initBearing else initBearing
        
        // 注册地图相机移动监听，用于更新锁定模式下罗盘位置
        mapProvider.onCameraChange { cam ->
            val now = SystemClock.elapsedRealtime()
            if (now - ui.lastProgrammaticMoveTimestamp > 700) {
                markUserManualCamera()
            }
            // Compass follows map bearing only (not device sensors).
            azimuth = if (mapProviderType == MapProviderType.GOOGLE) -cam.bearing else cam.bearing
            if (compassLocked && lockedLat != null && lockedLng != null) {
                updateCompassScreenPosition()
            }
        }
        mapProvider.onCameraChangeFinish { cam ->
            azimuth = if (mapProviderType == MapProviderType.GOOGLE) -cam.bearing else cam.bearing
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
            if (ui.sectorOverlayVisible && ui.sectorUseMapCenterOrigin) {
                ui.sectorRenderTick += 1
            }
        }
        
        onDispose {
            locationHelper.stop()  // 停止GPS定位
        }
    }

    LaunchedEffect(mapProviderType, mapReady.value, restoreCameraPosition) {
        val snapshot = restoreCameraPosition
        if (mapReady.value && snapshot != null) {
            requestCameraMove(
                snapshot.target,
                snapshot.zoom,
                CameraMoveSource.USER_MANUAL
            )
            onRestoreCameraConsumed?.invoke()
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
        lineInfoSummary = "${line.origin.name} -> ${line.destination.name} | ${"%.1f".format(bearing)}° | $shan | ${"%.1f".format(dist)}m"
        lineInfoDetail = context.getString(
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
        lineInfoExpanded = false
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
                            mapProvider.setOnMarkerClickListener { marker ->
                                val poi = poiByMarkerId[marker.id]
                                if (poi != null) {
                                    selectedPoiDetail = poi
                                    showPoiDetailDialog = true
                                }
                            }
                            refreshLinesForDisplay()
                            refreshNormalLines()
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
                            mapProvider.setOnPolylineClickListener { polyline ->
                                val line = lineByPolylineId[polyline.id]
                                if (line != null) {
                                    showLineInfoFor(line)
                                }
                            }
                            mapProvider.setOnMarkerClickListener { marker ->
                                val poi = poiByMarkerId[marker.id]
                                if (poi != null) {
                                    selectedPoiDetail = poi
                                    showPoiDetailDialog = true
                                }
                            }
                            refreshLinesForDisplay()
                            refreshNormalLines()
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

            // Top-left north indicator: points to map north based on camera bearing.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    // Keep this below the top title/language bar so it is always visible on map area.
                    .padding(start = 12.dp, top = 114.dp)
                    .zIndex(20f)
                    .background(Color(0xF7FFFFFF), RoundedCornerShape(12.dp))
                    .border(width = 1.dp, color = Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(
                        modifier = Modifier
                            .size(30.dp)
                            .rotate(-azimuth)
                    ) {
                        val cx = size.width / 2f
                        val top = size.height * 0.1f
                        val half = size.width * 0.22f
                        val h = size.height * 0.55f
                        drawLine(
                            color = Color.Black,
                            start = Offset(cx, size.height * 0.9f),
                            end = Offset(cx, size.height * 0.22f),
                            strokeWidth = 2.5f
                        )
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx, top)
                                lineTo(cx - half, top + h)
                                lineTo(cx + half, top + h)
                                close()
                            },
                            color = Color.Black
                        )
                    }
                    Text("北", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("N", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            statusBannerMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .zIndex(25f)
                        .background(Color(0xE62B2B2B), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(msg, color = Color.White, fontSize = 12.sp)
                }
            }

            if (!sideBarExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 186.dp, end = 2.dp)
                        .zIndex(19f)
                ) {
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
                                    onMapProviderSwitch(MapProviderType.AMAP, mapProvider.getCameraPosition())
                                    trialMessage = msgGoogleSatelliteFallback
                                    showTrialDialog = true
                                    return@MapControlButtons
                                }
                            }
                            mapProvider.setMapType(type)
                        },
                        onSwitchProvider = { target ->
                            onMapProviderSwitch(target, mapProvider.getCameraPosition())
                        },
                        modifier = Modifier
                    )
                }
            }
            
            // 连线绘制层（使用Canvas）
            Canvas(modifier = Modifier
                .fillMaxSize()
                .zIndex(0.5f)) {
                if (ui.sectorOverlayVisible) {
                    val renderTick = ui.sectorRenderTick
                    if (renderTick >= 0) {
                        // no-op: just subscribe state for recomposition
                    }
                    val config = ui.lastSectorConfig
                    val origin = if (ui.sectorUseMapCenterOrigin) {
                        mapProvider.getCameraPosition()?.target
                    } else {
                        ui.sectorOrigin
                    }
                    if (config != null && origin != null) {
                        val startPoint = RhumbLineUtils.calculateRhumbDestination(
                            start = origin,
                            bearing = config.startAngle,
                            distanceMeters = config.maxDistanceMeters
                        )
                        val endPoint = RhumbLineUtils.calculateRhumbDestination(
                            start = origin,
                            bearing = config.endAngle,
                            distanceMeters = config.maxDistanceMeters
                        )
                        val centerScreen = mapProvider.latLngToScreenLocation(origin)
                        val startScreen = mapProvider.latLngToScreenLocation(startPoint)
                        val endScreen = mapProvider.latLngToScreenLocation(endPoint)

                        val dashed = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        drawLine(
                            color = Color(0xFF6A4FB5),
                            start = Offset(centerScreen.x, centerScreen.y),
                            end = Offset(startScreen.x, startScreen.y),
                            strokeWidth = 4f,
                            pathEffect = dashed
                        )
                        drawLine(
                            color = Color(0xFF6A4FB5),
                            start = Offset(centerScreen.x, centerScreen.y),
                            end = Offset(endScreen.x, endScreen.y),
                            strokeWidth = 4f,
                            pathEffect = dashed
                        )

                        val span = sectorSpanDegrees(config.startAngle, config.endAngle)
                        val stepCount = 24
                        var prev = startPoint
                        for (index in 1..stepCount) {
                            val angle = (config.startAngle + span * (index.toFloat() / stepCount.toFloat())) % 360f
                            val curr = RhumbLineUtils.calculateRhumbDestination(
                                start = origin,
                                bearing = angle,
                                distanceMeters = config.maxDistanceMeters
                            )
                            val prevScreen = mapProvider.latLngToScreenLocation(prev)
                            val currScreen = mapProvider.latLngToScreenLocation(curr)
                            drawLine(
                                color = Color(0x996A4FB5),
                                start = Offset(prevScreen.x, prevScreen.y),
                                end = Offset(currScreen.x, currScreen.y),
                                strokeWidth = 3f
                            )
                            prev = curr
                        }
                    }
                }
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
                    continuousAddMode = continuousAddMode,
                    continuousAddType = continuousAddType,
                    onSwitchContinuousAddType = {
                        continuousAddType = if (continuousAddType == PointType.ORIGIN) {
                            PointType.DESTINATION
                        } else {
                            PointType.ORIGIN
                        }
                    },
                    onStopContinuousAdd = {
                        continuousAddMode = false
                        viewModel.closeCrosshair()
                        showPostSaveQuickActions = false
                    },
                    onSelectOrigin = {
                        val target = ui.crosshairLocation ?: mapProvider.getCameraPosition()?.target
                        if (target == null) {
                            trialMessage = msgNoLocation
                            showTrialDialog = true
                            return@CrosshairModeUI
                        }
                        if (continuousAddMode) {
                            continuousPendingType = PointType.ORIGIN
                            continuousPendingTarget = target
                            continuousPointName = context.getString(R.string.default_origin_name, originPoints.size + 1)
                            showContinuousNameDialog = true
                        } else {
                            saveCrosshairPoint(PointType.ORIGIN, target)
                        }
                    },
                    onSelectDestination = {
                        val target = ui.crosshairLocation ?: mapProvider.getCameraPosition()?.target
                        if (target == null) {
                            trialMessage = msgNoLocation
                            showTrialDialog = true
                            return@CrosshairModeUI
                        }
                        if (continuousAddMode) {
                            continuousPendingType = PointType.DESTINATION
                            continuousPendingTarget = target
                            continuousPointName = context.getString(R.string.default_destination_name, destPoints.size + 1)
                            showContinuousNameDialog = true
                        } else {
                            saveCrosshairPoint(PointType.DESTINATION, target)
                        }
                    },
                    onCancel = {
                        viewModel.closeCrosshair()
                    }
                )
            } else {
                // Keep center tap target for quick add, but remove persistent red cross overlay.
                Box(modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clickable {
                        addPointName = ""
                        addPointType = continuousAddType
                        addPointProjectId = currentProject?.id
                        addPointUseNewProject = false
                        addPointNewProjectName = ""
                        showAddPointDialog = true
                    }
                    .zIndex(2f)  // 高于地图
                ) {}
            }

            if (continuousAddMode && ui.crosshairMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 164.dp, start = 16.dp, end = 16.dp)
                        .zIndex(4f)
                        .background(Color(0xE62B2B2B), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.continuous_add_map_move_hint),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // 右侧侧边栏展开时允许地图继续拖动，避免误导用户“地图不可操作”。

            if (!sideBarExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 56.dp)
                        .zIndex(2f)
                ) {
                    Button(
                        onClick = { openSidebarCollapsed() },
                        modifier = Modifier
                            .width(54.dp)
                            .heightIn(min = 34.dp)
                            .shadow(5.dp, RoundedCornerShape(11.dp)),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A4FB5))
                    ) {
                        Text("☰", fontSize = 11.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 52.dp)
                        .zIndex(2f)
                        .width(138.dp)
                        .background(Color(0xEFFFFFFF), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color(0x16000000), RoundedCornerShape(16.dp))
                        .padding(7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.menu_open),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { sideBarExpanded = false },
                            modifier = Modifier
                                .width(54.dp)
                                .heightIn(min = 36.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A4FB5))
                        ) {
                            Text("收", fontSize = 10.sp)
                        }
                    }

                    SpacerSmall()
                    Column(
                        modifier = Modifier
                            .heightIn(max = 500.dp)
                            .verticalScroll(sidebarScrollState)
                    ) {
                        val sectionButtonModifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                        val subHeaderModifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .heightIn(min = 40.dp)
                        val subButtonModifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .heightIn(min = 40.dp)
                        SidebarSectionHeader(
                            title = sectionMapTools,
                            expanded = sectionMapToolsExpanded,
                            onToggle = { sectionMapToolsExpanded = !sectionMapToolsExpanded },
                            modifier = sectionButtonModifier
                        )
                        if (sectionMapToolsExpanded) {
                            SidebarSubSectionHeader(
                                title = stringResource(id = R.string.subsection_compass_tools),
                                expanded = subMapCompassExpanded,
                                onToggle = { subMapCompassExpanded = !subMapCompassExpanded },
                                modifier = subHeaderModifier
                            )
                            if (subMapCompassExpanded) {
                                Button(onClick = {
                                    if (!compassLocked) {
                                        val currentPos = mapProvider.getCameraPosition()?.target
                                        if (currentPos != null) {
                                            lockCompassToLatLng(currentPos.latitude, currentPos.longitude)
                                            showStatus("罗盘已锁定")
                                        } else {
                                            trialMessage = msgNoLocation
                                            showTrialDialog = true
                                        }
                                    } else {
                                        unlockCompass()
                                        showStatus("罗盘已解锁")
                                    }
                                }, modifier = subButtonModifier) {
                                    Text(
                                        if (compassLocked) stringResource(id = R.string.action_unlock) else stringResource(id = R.string.action_lock),
                                        fontSize = 11.sp
                                    )
                                }
                                SpacerSmall()
                                Button(onClick = {
                                    if (realGpsLat != null && realGpsLng != null) {
                                        requestCameraMove(
                                            UniversalLatLng(realGpsLat!!, realGpsLng!!),
                                            15f,
                                            CameraMoveSource.USER_MANUAL
                                        )
                                        unlockCompass()
                                        showStatus("已定位到当前位置")
                                    } else {
                                        trialMessage = msgGpsGetting
                                        showTrialDialog = true
                                    }
                                }, modifier = subButtonModifier) {
                                    Text(stringResource(id = R.string.action_locate), fontSize = 11.sp)
                                }
                                SpacerSmall()
                                Button(onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        arCompassEnabled = true
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }, modifier = subButtonModifier) {
                                    Text(actionArCompass, fontSize = 11.sp)
                                }
                                SpacerSmall()
                            }

                            SpacerSmall()
                        }

                        SidebarSectionHeader(
                            title = sectionCaseOps,
                            expanded = sectionCaseExpanded,
                            onToggle = { sectionCaseExpanded = !sectionCaseExpanded },
                            modifier = sectionButtonModifier
                        )
                        if (sectionCaseExpanded) {
                            SidebarSubSectionHeader(
                                title = stringResource(id = R.string.subsection_case_select),
                                expanded = subCaseSelectionExpanded,
                                onToggle = { subCaseSelectionExpanded = !subCaseSelectionExpanded },
                                modifier = subHeaderModifier
                            )
                            if (subCaseSelectionExpanded) {
                                Button(onClick = { showProjectSelectDialog = true }, modifier = subButtonModifier) {
                                    Text(
                                        stringResource(
                                            id = R.string.label_case_with_name,
                                            currentProject?.name ?: caseNone
                                        ),
                                        fontSize = 10.sp
                                    )
                                }
                                SpacerSmall()
                                Button(onClick = {
                                    if (originPoints.isEmpty()) {
                                        trialMessage = msgNoOrigins
                                        showTrialDialog = true
                                    } else {
                                        showOriginSelectDialog = true
                                    }
                                }, modifier = subButtonModifier) {
                                    Text(stringResource(id = R.string.action_select_origin), fontSize = 11.sp)
                                }
                                SpacerSmall()
                                Button(onClick = {
                                    if (currentProject == null) {
                                        trialMessage = msgSelectCase
                                        showTrialDialog = true
                                    } else if (destPoints.isEmpty()) {
                                        trialMessage = msgNoDestinations
                                        showTrialDialog = true
                                    } else {
                                        showDestinationSelectDialog = true
                                    }
                                }, modifier = subButtonModifier) {
                                    Text(actionSelectDestination, fontSize = 11.sp)
                                }
                                SpacerSmall()
                            }

                            SidebarSubSectionHeader(
                                title = stringResource(id = R.string.subsection_case_edit),
                                expanded = subCaseEditExpanded,
                                onToggle = { subCaseEditExpanded = !subCaseEditExpanded },
                                modifier = subHeaderModifier
                            )
                            if (subCaseEditExpanded) {
                                Button(onClick = {
                                    addPointName = ""
                                    addPointType = continuousAddType
                                    addPointProjectId = currentProject?.id
                                    addPointUseNewProject = false
                                    addPointNewProjectName = ""
                                    showAddPointDialog = true
                                }, modifier = subButtonModifier) {
                                    Text(stringResource(id = R.string.action_add_point), fontSize = 11.sp)
                                }
                                SpacerSmall()
                                Button(onClick = { continuousAddMode = !continuousAddMode }, modifier = subButtonModifier) {
                                    Text(
                                        if (continuousAddMode) actionContinuousAddModeOn else actionContinuousAddModeOff,
                                        fontSize = 11.sp
                                    )
                                }
                                SpacerSmall()
                                Button(
                                    onClick = {
                                        continuousAddType = if (continuousAddType == PointType.ORIGIN) PointType.DESTINATION else PointType.ORIGIN
                                    },
                                    modifier = subButtonModifier
                                ) {
                                    Text(actionContinuousCurrentType, fontSize = 10.sp)
                                }
                                SpacerSmall()
                            }
                            SpacerSmall()
                        }

                        SidebarSectionHeader(
                            title = sectionAnalysis,
                            expanded = sectionAnalysisExpanded,
                            onToggle = { sectionAnalysisExpanded = !sectionAnalysisExpanded },
                            modifier = sectionButtonModifier
                        )
                        if (sectionAnalysisExpanded) {
                            SidebarSubSectionHeader(
                                title = stringResource(id = R.string.subsection_analysis_core),
                                expanded = subAnalysisCoreExpanded,
                                onToggle = { subAnalysisCoreExpanded = !subAnalysisCoreExpanded },
                                modifier = subHeaderModifier
                            )
                            if (subAnalysisCoreExpanded) {
                                Button(onClick = {
                                    if (originPoints.size < 3) {
                                        trialMessage = msgNeedThreeOrigins
                                        showTrialDialog = true
                                        return@Button
                                    }
                                    lifeCircleHomeId = null
                                    lifeCircleWorkId = null
                                    lifeCircleWizardStep = 1
                                }, modifier = subButtonModifier) {
                                    Text(stringResource(id = R.string.action_life_circle_mode), fontSize = 10.sp)
                                }
                                SpacerSmall()
                                Button(onClick = { ui.showSectorConfigDialog = true }, modifier = subButtonModifier) {
                                    Text(stringResource(id = R.string.action_sector_search), fontSize = 10.sp)
                                }
                                if (originPoint != null && destPoint != null) {
                                    SpacerSmall()
                                    Button(onClick = {
                                        val bearing = RhumbLineUtils.calculateRhumbBearing(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                                        val shan = RhumbLineUtils.getShanName(bearing)
                                        val bagua = RhumbLineUtils.getBaGua(bearing)
                                        val wuxing = RhumbLineUtils.getWuXing(bearing)
                                        val dist = RhumbLineUtils.haversineDistanceMeters(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                                        lineInfoSummary = "${originPoint!!.name} -> ${destPoint!!.name} | ${"%.1f".format(bearing)}° | $shan | ${"%.1f".format(dist)}m"
                                        lineInfoDetail = context.getString(
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
                                        lineInfoExpanded = false
                                        showLineInfo = true
                                    }, modifier = subButtonModifier) { Text(stringResource(id = R.string.action_show_line_info), fontSize = 10.sp) }
                                }
                                SpacerSmall()
                            }
                        }
                    }
                }
            }

            if (showPostSaveQuickActions && !ui.crosshairMode && lastAddedPoint != null) {
                val latest = lastAddedPoint!!
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 84.dp, start = 12.dp, end = 12.dp)
                        .zIndex(4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xEEFFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "$msgPostSavedPoint: ${latest.name} (${if (lastAddedPointType == PointType.ORIGIN) msgPostSavedOrigin else msgPostSavedDestination})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SpacerSmall()
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        removePointAndRefresh(latest)
                                        showPostSaveQuickActions = false
                                        lastAddedPoint = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(msgUndo, fontSize = 12.sp) }
                            Spacer(modifier = Modifier.size(8.dp))
                            Button(
                                onClick = {
                                    showPostSaveQuickActions = false
                                    continuousAddMode = true
                                    continuousAddType = lastAddedPointType
                                    viewModel.openCrosshair(
                                        crosshairManualTitle,
                                        msgContinueAddHint,
                                        mapProvider.getCameraPosition()?.target
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(msgContinueAdd, fontSize = 12.sp) }
                        }
                        SpacerSmall()
                        Button(
                            onClick = {
                                showPostSaveQuickActions = false
                                continuousAddMode = false
                                lastAddedPoint = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.action_stop_add), fontSize = 12.sp)
                        }
                    }
                }
            }

            if (deletedPointUndoCandidate != null && !ui.crosshairMode) {
                val deleted = deletedPointUndoCandidate!!
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 84.dp, start = 12.dp, end = 12.dp)
                        .zIndex(4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xEEFFFFFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${deleted.name} 已删除",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val project = currentProject
                                        if (project != null) {
                                            val restored = repo.createPoint(
                                                deleted.name,
                                                deleted.latitude,
                                                deleted.longitude,
                                                deleted.type,
                                                project.id,
                                                address = deleted.address,
                                                groupName = project.name
                                            )
                                            if (restored.type == PointType.ORIGIN) {
                                                originPoints.add(restored)
                                            } else {
                                                destPoints.add(restored)
                                            }
                                            refreshLinesForDisplay()
                                            showStatus("已撤销删除")
                                        }
                                    } catch (_: Exception) {
                                    } finally {
                                        deletedPointUndoCandidate = null
                                    }
                                }
                            },
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text(stringResource(id = R.string.action_undo), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Compass overlay (hidden in life-circle mode; life-circle uses three point compasses).
            if (!ui.lifeCircleMode && !compassLocked) {
                // 解锁模式：罗盘在屏幕中央，显示当前GPS位置
                if (realGpsLat != null && realGpsLng != null) {
                    Box(modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(1.2f)) {
                        CompassOverlay(
                            azimuthDegrees = azimuth,
                            latitude = realGpsLat!!,
                            longitude = realGpsLng!!,
                            sizeDp = 260.dp,
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
            } else if (!ui.lifeCircleMode) {
                // 锁定模式：罗盘锁定在指定位置，随地图移动
                if (lockedLat != null && lockedLng != null) {
                    // 初始化屏幕位置
                    LaunchedEffect(lockedLat, lockedLng, compassLocked) {
                        updateCompassScreenPosition()
                    }
                    
                    val compassRadiusPx = with(density) { 130.dp.toPx() }  // 罗盘半径
                    
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
                                sizeDp = 260.dp,
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1.3f)
                    ) {
                        val lifePoints = listOf(data.homePoint, data.workPoint, data.entertainmentPoint)
                        lifePoints.forEach { point ->
                            val screenPos = mapProvider.latLngToScreenLocation(
                                UniversalLatLng(point.latitude, point.longitude)
                            )
                            val radiusPx = with(density) { 64.dp.toPx() }
                            Box(
                                modifier = Modifier.offset {
                                    IntOffset(
                                        (screenPos.x - radiusPx).toInt(),
                                        (screenPos.y - radiusPx).toInt()
                                    )
                                }
                            ) {
                                CompassOverlay(
                                    azimuthDegrees = azimuth,
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    sizeDp = 128.dp,
                                    showInfo = false
                                )
                            }
                        }
                    }

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

            if (arCompassEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(30f)
                ) {
                    ArCompassOverlay(
                        modifier = Modifier.fillMaxSize(),
                        onClose = { arCompassEnabled = false },
                        onCameraOpenError = {
                            arCompassEnabled = false
                            trialMessage = msgArOpenFailed
                            showTrialDialog = true
                        }
                    )
                }
            }

            if (showLineInfo) {
                AlertDialog(
                    onDismissRequest = { showLineInfo = false },
                    confirmButton = {
                        TextButton(onClick = { showLineInfo = false }) { Text(stringResource(id = R.string.action_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { lineInfoExpanded = !lineInfoExpanded }) {
                            Text(if (lineInfoExpanded) "收起详情" else "展开详情")
                        }
                    },
                    text = {
                        Column {
                            Text(lineInfoSummary)
                            if (lineInfoExpanded) {
                                SpacerSmall()
                                Text(lineInfoDetail)
                            }
                        }
                    }
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

            if (lifeCircleWizardStep == 1) {
                val selectedHomeName = originPoints.firstOrNull { it.id == lifeCircleHomeId }?.name
                AlertDialog(
                    onDismissRequest = { lifeCircleWizardStep = 0 },
                    title = { Text(lifeCircleStepHome) },
                    text = {
                        Column {
                            Text(lifeCircleStepHint, fontSize = 12.sp, color = Color.Gray)
                            selectedHomeName?.let {
                                SpacerSmall()
                                Text("${stringResource(id = R.string.life_circle_role_home)}: $it", fontSize = 12.sp)
                            }
                            SpacerSmall()
                            originPoints.forEach { point ->
                                Text(
                                    text = point.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            lifeCircleHomeId = point.id
                                            lifeCircleWizardStep = 2
                                        }
                                        .padding(10.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { lifeCircleWizardStep = 0 }) { Text(stringResource(id = R.string.action_cancel)) }
                    }
                )
            }

            if (showFirstUseGuide) {
                AlertDialog(
                    onDismissRequest = { showFirstUseGuide = false },
                    title = { Text("地图快速引导") },
                    text = {
                        Column {
                            Text("1. 右侧“菜单”可展开地图工具、案例操作、分析功能。")
                            SpacerSmall()
                            Text("2. 点击加点后，原点会自动锁定罗盘；终点会回到当前原点查看连线。")
                            SpacerSmall()
                            Text("3. 扇形搜索可先定位，再在地图底部保存并返回结果列表。")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFirstUseGuide = false }) {
                            Text(stringResource(id = R.string.action_confirm))
                        }
                    }
                )
            }

            if (lifeCircleWizardStep == 2) {
                val candidates = originPoints.filter { it.id != lifeCircleHomeId }
                val selectedHomeName = originPoints.firstOrNull { it.id == lifeCircleHomeId }?.name
                val selectedWorkName = originPoints.firstOrNull { it.id == lifeCircleWorkId }?.name
                AlertDialog(
                    onDismissRequest = { lifeCircleWizardStep = 0 },
                    title = { Text(lifeCircleStepWork) },
                    text = {
                        Column {
                            Text(lifeCircleStepHint, fontSize = 12.sp, color = Color.Gray)
                            selectedHomeName?.let {
                                SpacerSmall()
                                Text("${stringResource(id = R.string.life_circle_role_home)}: $it", fontSize = 12.sp)
                            }
                            selectedWorkName?.let {
                                SpacerSmall()
                                Text("${stringResource(id = R.string.life_circle_role_work)}: $it", fontSize = 12.sp)
                            }
                            SpacerSmall()
                            candidates.forEach { point ->
                                Text(
                                    text = point.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            lifeCircleWorkId = point.id
                                            lifeCircleWizardStep = 3
                                        }
                                        .padding(10.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { lifeCircleWizardStep = 0 }) { Text(stringResource(id = R.string.action_cancel)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { lifeCircleWizardStep = 1 }) { Text(stringResource(id = R.string.action_previous_step)) }
                    }
                )
            }

            if (lifeCircleWizardStep == 3) {
                val candidates = originPoints.filter { it.id != lifeCircleHomeId && it.id != lifeCircleWorkId }
                val selectedHomeName = originPoints.firstOrNull { it.id == lifeCircleHomeId }?.name
                val selectedWorkName = originPoints.firstOrNull { it.id == lifeCircleWorkId }?.name
                AlertDialog(
                    onDismissRequest = { lifeCircleWizardStep = 0 },
                    title = { Text(lifeCircleStepEntertainment) },
                    text = {
                        Column {
                            Text(lifeCircleStepHint, fontSize = 12.sp, color = Color.Gray)
                            selectedHomeName?.let {
                                SpacerSmall()
                                Text("${stringResource(id = R.string.life_circle_role_home)}: $it", fontSize = 12.sp)
                            }
                            selectedWorkName?.let {
                                SpacerSmall()
                                Text("${stringResource(id = R.string.life_circle_role_work)}: $it", fontSize = 12.sp)
                            }
                            SpacerSmall()
                            candidates.forEach { point ->
                                Text(
                                    text = point.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val home = originPoints.firstOrNull { it.id == lifeCircleHomeId }
                                            val work = originPoints.firstOrNull { it.id == lifeCircleWorkId }
                                            val entertainment = point
                                            if (home != null && work != null) {
                                                val selected = listOf(home, work, entertainment)
                                                val assignments = mapOf(
                                                    home.id to LifeCirclePointType.HOME,
                                                    work.id to LifeCirclePointType.WORK,
                                                    entertainment.id to LifeCirclePointType.ENTERTAINMENT
                                                )
                                                activateLifeCircleMode(selected, assignments)
                                            }
                                            lifeCircleWizardStep = 0
                                        }
                                        .padding(10.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { lifeCircleWizardStep = 0 }) { Text(stringResource(id = R.string.action_cancel)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { lifeCircleWizardStep = 2 }) { Text(stringResource(id = R.string.action_previous_step)) }
                    }
                )
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
                    initialConfig = ui.lastSectorConfig,
                    hasExistingSector = ui.sectorOverlayVisible,
                    onConfirm = { config, clearBeforeDraw ->
                        ui.showSectorConfigDialog = false
                        // Always clear previous sector and POI markers before a new sector search.
                        clearSectorArtifacts()

                        val originFromCase = selectedOriginPoint ?: originPoints.firstOrNull()
                        val usingMapCenter = originFromCase == null
                        val originLatLng = if (usingMapCenter) {
                            mapProvider.getCameraPosition()?.target
                        } else {
                            UniversalLatLng(originFromCase!!.latitude, originFromCase.longitude)
                        }
                        if (originLatLng == null) {
                            trialMessage = msgNoLocation
                            showTrialDialog = true
                            return@SectorConfigDialog
                        }

                        ui.sectorOrigin = originLatLng
                        ui.sectorUseMapCenterOrigin = usingMapCenter
                        ui.lastSectorConfig = config
                        ui.sectorConfigLabel = config.label
                        ui.sectorOverlayVisible = true
                        ui.sectorRenderTick += 1

                        if (usingMapCenter) {
                            trialMessage = msgSectorFromMapCenter
                            showTrialDialog = true
                        }

                        if (config.keyword.isBlank()) {
                            ui.sectorLoading = false
                            ui.sectorResults.clear()
                            ui.sectorNoticeCount = null
                            showPoiMarkers(emptyList())
                            trialMessage = msgSectorNoKeywordDrawOnly
                            showTrialDialog = true
                        } else {
                            ui.sectorLoading = true
                            viewModel.runSectorSearch(
                                providers = buildPoiProviderChain(config.keyword),
                                origin = ui.sectorOrigin!!,
                                config = config,
                                onResult = { results ->
                                    showPoiMarkers(results)
                                    focusOnSectorResults(results)
                                    if (ui.sectorRadiusLimited) {
                                        trialMessage = msgSectorRadiusLimited
                                        showTrialDialog = true
                                    }
                                },
                                onError = {
                                    trialMessage = "${msgSectorSearchFailed}: ${it.message ?: "unknown"}"
                                    showTrialDialog = true
                                }
                            )
                        }
                    },
                    onClearSector = {
                        ui.showSectorConfigDialog = false
                        clearSectorArtifacts()
                    },
                    onDismiss = { ui.showSectorConfigDialog = false }
                )
            }

            if (ui.showSectorResultDialog) {
                AlertDialog(
                    onDismissRequest = {
                        clearSectorArtifacts()
                    },
                    title = {
                        Text(stringResource(id = R.string.sector_result_title, ui.sectorConfigLabel))
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (ui.sectorLoading) {
                                Text(stringResource(id = R.string.sector_search_loading))
                            } else if (ui.sectorResults.isEmpty()) {
                                Text(stringResource(id = R.string.sector_no_results))
                                SpacerSmall()
                                Text(
                                    text = "建议：扩大距离 / 切换关键词（如住宅=小区/公寓）/ 切换地图源后重试",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            } else {
                                val effectiveKm = ui.sectorEffectiveRadiusMeters / 1000f
                                Text(
                                    stringResource(id = R.string.sector_effective_radius, effectiveKm),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                if (ui.sectorRadiusLimited) {
                                    Text(
                                        stringResource(id = R.string.sector_poi_radius_limited_notice),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (ui.sectorFallbackUsed) {
                                    Text(
                                        msgSectorFallbackNearby,
                                        fontSize = 11.sp,
                                        color = Color(0xFF7A5A00)
                                    )
                                }
                                Spacer(modifier = Modifier.size(6.dp))
                                Button(
                                    onClick = { sectorSortByDistance = !sectorSortByDistance },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (sectorSortByDistance) msgSectorSortByDistance else msgSectorSortByName,
                                        fontSize = 12.sp
                                    )
                                }
                                SpacerSmall()
                                Button(
                                    onClick = { showSectorUnsavedOnly = !showSectorUnsavedOnly },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (showSectorUnsavedOnly) "显示全部结果" else "仅显示未保存",
                                        fontSize = 12.sp
                                    )
                                }
                                ui.sectorNoticeCount?.let { count ->
                                    Text(
                                        stringResource(id = R.string.sector_notice, count),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                val origin = ui.sectorOrigin
                                val shownResults = if (sectorSortByDistance && origin != null) {
                                    ui.sectorResults.sortedBy {
                                        RhumbLineUtils.calculateRhumbDistance(
                                            origin,
                                            UniversalLatLng(it.lat, it.lng)
                                        )
                                    }
                                } else {
                                    ui.sectorResults.sortedBy { it.name }
                                }
                                val visibleResults = if (showSectorUnsavedOnly) {
                                    shownResults.filter { !isPoiAlreadySaved(it) }
                                } else {
                                    shownResults
                                }
                                visibleResults.forEach { poi ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(poi.name)
                                            Text(poi.address ?: "", fontSize = 10.sp, color = Color.Gray)
                                            if (origin != null) {
                                                val dKm = RhumbLineUtils.calculateRhumbDistance(
                                                    origin,
                                                    UniversalLatLng(poi.lat, poi.lng)
                                                ) / 1000f
                                                Text(
                                                    stringResource(id = R.string.sector_item_distance_km, dKm),
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        Button(onClick = {
                                            val target = UniversalLatLng(poi.lat, poi.lng)
                                            pendingSectorLocatePoi = poi
                                            ui.showSectorResultDialog = false
                                            requestCameraMove(target, 16f, CameraMoveSource.USER_POINT_SELECT)
                                        }, modifier = Modifier.heightIn(min = 44.dp)) {
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
                                                    selectedDestinationIds.clear()
                                                    refreshLinesForDisplay()
                                                    showStatus("已保存到当前案例")
                                                } catch (e: Exception) {
                                                    trialMessage = e.message ?: msgAddDestinationFailed
                                                    showTrialDialog = true
                                                }
                                            }
                                        }, modifier = Modifier.heightIn(min = 44.dp)) {
                                            Text(stringResource(id = R.string.action_save))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            clearSectorArtifacts()
                        }) { Text(stringResource(id = R.string.action_close)) }
                    }
                )
            }

            if (pendingSectorLocatePoi != null && !ui.showSectorResultDialog) {
                val poi = pendingSectorLocatePoi!!
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 84.dp, start = 12.dp, end = 12.dp)
                        .zIndex(5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xEEFFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(poi.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(poi.address ?: "", fontSize = 11.sp, color = Color.Gray)
                        SpacerSmall()
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
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
                                            selectedDestinationIds.clear()
                                            refreshLinesForDisplay()
                                            pendingSectorLocatePoi = null
                                            ui.showSectorResultDialog = true
                                        } catch (e: Exception) {
                                            trialMessage = e.message ?: msgAddDestinationFailed
                                            showTrialDialog = true
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(id = R.string.action_save))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Button(
                                onClick = {
                                    pendingSectorLocatePoi = null
                                    ui.showSectorResultDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(id = R.string.action_close))
                            }
                        }
                    }
                }
            }

            if (showPoiDetailDialog && selectedPoiDetail != null) {
                val poi = selectedPoiDetail!!
                AlertDialog(
                    onDismissRequest = { showPoiDetailDialog = false },
                    title = { Text(poi.name) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(poi.address ?: "")
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Lat: ${"%.6f".format(poi.lat)}")
                            Text("Lng: ${"%.6f".format(poi.lng)}")
                            Text("Provider: ${poi.provider}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            requestCameraMove(
                                UniversalLatLng(poi.lat, poi.lng),
                                16f,
                                CameraMoveSource.USER_POINT_SELECT
                            )
                        }) { Text(stringResource(id = R.string.action_locate_short)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPoiDetailDialog = false }) {
                            Text(stringResource(id = R.string.action_close))
                        }
                    }
                )
            }

            if (showContinuousNameDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showContinuousNameDialog = false
                        continuousPendingTarget = null
                    },
                    title = { Text(continuousAddNameTitle) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = continuousPointName,
                                onValueChange = { continuousPointName = it },
                                label = { Text(stringResource(id = R.string.add_point_name_label)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val target = continuousPendingTarget
                            if (target == null) {
                                trialMessage = msgNoLocation
                                showTrialDialog = true
                            } else {
                                saveCrosshairPoint(
                                    pointType = continuousPendingType,
                                    target = target,
                                    enteredName = continuousPointName
                                )
                            }
                            showContinuousNameDialog = false
                            continuousPendingTarget = null
                        }) {
                            Text(stringResource(id = R.string.action_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showContinuousNameDialog = false
                            continuousPendingTarget = null
                        }) {
                            Text(stringResource(id = R.string.action_cancel))
                        }
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

                                    if (project.id != currentProject?.id) {
                                        currentProject = project
                                        selectedDestinationIds.clear()
                                        loadProjectData(project)
                                    } else {
                                        if (addPointType == PointType.ORIGIN) {
                                            originPoints.add(p)
                                            selectedOriginPoint = p
                                            selectedDestinationIds.clear()
                                            refreshLinesForDisplay()
                                        } else {
                                            destPoints.add(p)
                                            selectedDestinationIds.clear()
                                            refreshLinesForDisplay()
                                        }
                                    }

                                    if (addPointType == PointType.ORIGIN) {
                                        lockCompassToPoint(p)
                                        requestCameraMove(
                                            UniversalLatLng(p.latitude, p.longitude),
                                            15f,
                                            CameraMoveSource.USER_POINT_SELECT
                                        )
                                    } else {
                                        val origin = activeOriginPoint()
                                        if (origin != null) {
                                            lockCompassToPoint(origin)
                                            requestCameraMove(
                                                UniversalLatLng(origin.latitude, origin.longitude),
                                                15f,
                                                CameraMoveSource.USER_POINT_SELECT
                                            )
                                        }
                                    }
                                    onPointAdded(p, addPointType)
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

            if (showDestinationSelectDialog) {
                MultiSelectDestinationDialog(
                    destinations = destPoints,
                    selectedIds = selectedDestinationIds.toSet(),
                    onConfirm = { selectedIds ->
                        pendingDestinationIds.clear()
                        pendingDestinationIds.addAll(selectedIds)
                        showDestinationSelectDialog = false
                        showOriginAfterDestinationDialog = true
                    },
                    onDismiss = { showDestinationSelectDialog = false }
                )
            }

            if (showOriginAfterDestinationDialog && originPoints.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showOriginAfterDestinationDialog = false },
                    title = { Text(stringResource(id = R.string.select_origin_title)) },
                    text = {
                        Column {
                            originPoints.forEach { point ->
                                Text(
                                    text = "${point.name} (${point.latitude.format(4)}, ${point.longitude.format(4)})",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedOriginPoint = point
                                            selectedDestinationIds.clear()
                                            selectedDestinationIds.addAll(pendingDestinationIds)
                                            refreshLinesForDisplay()
                                            lockCompassToPoint(point)
                                            showStatus("已切换原点并锁定罗盘")
                                            requestCameraMove(
                                                UniversalLatLng(point.latitude, point.longitude),
                                                15f,
                                                CameraMoveSource.USER_POINT_SELECT
                                            )
                                            showOriginAfterDestinationDialog = false
                                        }
                                        .padding(12.dp),
                                    color = if (point.id == selectedOriginPoint?.id) Color.Blue else Color.Black
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showOriginAfterDestinationDialog = false }) {
                            Text(stringResource(id = R.string.action_close))
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
                                            selectedDestinationIds.clear()
                                            refreshLinesForDisplay()
                                            // 锁定罗盘到原点位置
                                            lockCompassToPoint(point)
                                            showStatus("已切换原点并锁定罗盘")
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

private fun sectorSpanDegrees(start: Float, end: Float): Float {
    return if (end >= start) {
        end - start
    } else {
        (360f - start) + end
    }
}

@Composable
private fun SidebarSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onToggle,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A4FB5))
    ) {
        Text(
            text = if (expanded) "$title ▾" else "$title ▸",
            fontSize = 11.sp,
            maxLines = 1
        )
    }
    SpacerSmall()
}

@Composable
private fun SidebarSubSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onToggle,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1DAF2))
    ) {
        Text(
            text = if (expanded) "$title ▾" else "$title ▸",
            fontSize = 10.sp,
            color = Color(0xFF3F2E7A),
            maxLines = 1
        )
    }
    SpacerSmall()
}

@Composable
private fun SpacerSmall() {
    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.size(8.dp))
}


