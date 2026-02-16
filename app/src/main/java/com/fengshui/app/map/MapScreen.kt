package com.fengshui.app.map

import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import com.fengshui.app.R
import com.fengshui.app.map.ui.CompassOverlay
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType
import com.fengshui.app.data.LifeCircleData
import com.fengshui.app.data.LifeCirclePointType
import com.fengshui.app.data.LifeCircleConnection
import com.fengshui.app.data.ShanUtils
import com.fengshui.app.utils.RhumbLineUtils
import com.fengshui.app.utils.ApiKeyConfig
import com.fengshui.app.utils.AppLanguageManager
import com.fengshui.app.utils.PermissionHelper
import com.fengshui.app.utils.Prefs
import com.fengshui.app.utils.ShanTextResolver
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
import com.fengshui.app.utils.SensorHelper

private enum class QuickMenuTarget {
    NONE,
    CASE_OPS,
    ANALYSIS
}

/**
 * 简易 MapScreen 示例：
 * - 占位地图区域（后续替换为真正的 MapView/MapCompose）
 * - 屏幕中心十字准心
 * - 右侧放大/缩小/图层切换控件（使用 `MapControlButtons`）
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    quickAddCaseId: String? = null,
    onQuickAddConsumed: (() -> Unit)? = null,
    focusLocation: UniversalLatLng? = null,
    onFocusConsumed: (() -> Unit)? = null,
    openCaseOpsSignal: Int = 0,
    openAnalysisSignal: Int = 0,
    closeQuickMenuSignal: Int = 0,
    forceRelocateSignal: Int = 0,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentLanguageTag = AppLanguageManager.getCurrentLanguageTag(context)
    var currentMapType by remember {
        mutableStateOf(MapSessionStore.loadMapType(context) ?: MapType.VECTOR)
    }
    var compassLocked by remember { mutableStateOf(false) }  // 罗盘锁定状态
    var compassScreenPos by remember { mutableStateOf(Offset(0f, 0f)) }  // 锁定时罗盘在屏幕上的位置
    var lockedLat by remember { mutableStateOf<Double?>(null) }  // 锁定位置的纬度
    var lockedLng by remember { mutableStateOf<Double?>(null) }  // 锁定位置的经度
    var lastCompassUpdateMs by remember { mutableStateOf(0L) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // GPS location state - 启动后必须等待真实GPS定位，不再使用北京默认坐标兜底
    var realGpsLat by remember { mutableStateOf<Double?>(null) }
    var realGpsLng by remember { mutableStateOf<Double?>(null) }
    var hasRealGps by remember { mutableStateOf(false) }  // 是否已获取真实GPS
    var azimuth by remember { mutableStateOf(0f) }
    val startupLastKnownLocation = remember {
        val permissionGranted = PermissionHelper.hasLocationPermission(context)
        if (!permissionGranted) {
            null
        } else {
            runCatching {
                val locationManager = context.getSystemService(LocationManager::class.java)
                val candidates = listOfNotNull(
                    locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                    locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
                    locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                )
                candidates.maxByOrNull { it.time }?.let {
                    UniversalLatLng(it.latitude, it.longitude)
                }
            }.getOrNull()
        }
    }
    val sessionSavedCameraSnapshot = remember {
        MapSessionStore.loadCameraPosition(context)
    }

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
    fun isChinaLocale(): Boolean =
        (context.resources.configuration.locales[0] ?: java.util.Locale.getDefault())
            .country
            .equals("CN", ignoreCase = true)

    fun isChineseLanguage(): Boolean = AppLanguageManager.isChineseLanguage(context)

    fun buildPoiProviderChain(keyword: String): List<MapPoiProvider> {
        val hasChineseChars = keyword.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
        val isTypedKeyword = PoiTypeMapper.isTypedCategoryKeyword(keyword)
        val appChinese = isChineseLanguage()
        return buildList {
            if (isTypedKeyword) {
                // Strict type-search mode: use map typed endpoints only, no text-shape fallback providers.
                if (!appChinese) {
                    googlePoiProvider?.let { add(it) }
                    amapPoiProvider?.let { add(it) }
                } else if (mapProviderType == MapProviderType.AMAP) {
                    amapPoiProvider?.let { add(it) }
                    googlePoiProvider?.let { add(it) }
                } else {
                    googlePoiProvider?.let { add(it) }
                    amapPoiProvider?.let { add(it) }
                }
                return@buildList
            }
            if (!appChinese) {
                googlePoiProvider?.let { add(it) }
                amapPoiProvider?.let { add(it) }
                add(nominatimPoiProvider)
                add(mockPoiProvider)
                return@buildList
            }
            if (mapProviderType == MapProviderType.AMAP || isChinaLocale() || hasChineseChars) {
                amapPoiProvider?.let { add(it) }
            }
            if (mapProviderType == MapProviderType.GOOGLE) {
                googlePoiProvider?.let { add(it) }
            } else {
                googlePoiProvider?.let { add(it) }
            }
            if (mapProviderType != MapProviderType.AMAP && !isChinaLocale() && !hasChineseChars) {
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
    var quickMenuTarget by remember { mutableStateOf(QuickMenuTarget.NONE) }
    val quickMenuScrollState = rememberScrollState()
    val quickSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val layerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var arCompassEnabled by remember { mutableStateOf(false) }
    val destinationColorIndexById = remember { mutableStateMapOf<String, Int>() }
    val poiByMarkerId = remember { mutableMapOf<String, PoiResult>() }
    var selectedPoiDetail by remember { mutableStateOf<PoiResult?>(null) }
    var showPoiDetailDialog by remember { mutableStateOf(false) }
    var pendingSectorLocatePoi by remember { mutableStateOf<PoiResult?>(null) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var statusBannerToken by remember { mutableStateOf(0) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var layerDialogProvider by remember { mutableStateOf(mapProviderType) }
    var deviceDirectionMode by remember { mutableStateOf(false) }
    var deviceHeading by remember { mutableStateOf(0f) }
    var lastDeviceDirectionUpdateMs by remember { mutableStateOf(0L) }
    var topSearchInput by remember { mutableStateOf(TextFieldValue("")) }
    var topSearchLoading by remember { mutableStateOf(false) }
    var topSearchResults by remember { mutableStateOf(listOf<PoiResult>()) }
    var topSearchResultsVisible by remember { mutableStateOf(false) }
    var lifeCircleTopPanelVisible by remember { mutableStateOf(true) }
    var lifeCircleOverlayHeightPx by remember { mutableStateOf(0) }
    var showSectorUnsavedOnly by remember { mutableStateOf(false) }
    var showFirstUseGuide by remember { mutableStateOf(false) }
    var deletedPointUndoCandidate by remember { mutableStateOf<FengShuiPoint?>(null) }
    var suppressAutoLocateOnce by remember { mutableStateOf(false) }
    var lastKnownCameraPosition by remember {
        mutableStateOf(restoreCameraPosition ?: sessionSavedCameraSnapshot)
    }
    var pendingAutoLocateToGps by remember { mutableStateOf(true) }
    var googleSatelliteRestrictionNotified by remember { mutableStateOf(false) }
    var gpsInitializationStartMs by remember { mutableStateOf<Long?>(null) }
    var suppressPersistUntilMs by remember { mutableStateOf(0L) }
    var mapCameraTick by remember { mutableStateOf(0L) }
    var shouldAutoLocateOnFirstFix by remember { mutableStateOf(true) }
    val hasLocationPermission = PermissionHelper.hasLocationPermission(context)
    val gpsInitializationBlocking = pendingAutoLocateToGps && !hasRealGps && hasLocationPermission
    val gpsInitializationTimeoutMs = 12_000L

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
    var lifeCircleWizardStep by remember { mutableStateOf(0) } // 0 none, 1 home, 2 work, 3 entertainment
    var lifeCircleHomeId by remember { mutableStateOf<String?>(null) }
    var lifeCircleWorkId by remember { mutableStateOf<String?>(null) }
    var sectorSortByDistance by remember { mutableStateOf(true) }
    val viewModel: MapUiStateViewModel = viewModel()
    val ui = viewModel.ui
    val crosshairSearchTitle = stringResource(id = R.string.crosshair_search_title)
    val crosshairSearchSubtitle = stringResource(id = R.string.crosshair_search_subtitle)
    val crosshairManualTitle = stringResource(id = R.string.crosshair_manual_title)
    val crosshairNotLocated = stringResource(id = R.string.crosshair_not_located)
    val caseNotSelected = stringResource(id = R.string.case_not_selected)
    val caseNone = stringResource(id = R.string.case_none)
    val msgNoLocation = stringResource(id = R.string.err_no_location)
    val msgSelectCase = stringResource(id = R.string.err_select_or_create_case)
    val msgAddOriginFailed = stringResource(id = R.string.err_add_origin_failed)
    val msgAddDestinationFailed = stringResource(id = R.string.err_add_destination_failed)
    val msgGpsGetting = stringResource(id = R.string.gps_getting)
    val msgGpsPermissionMissingContinue = stringResource(id = R.string.status_gps_permission_missing_continue)
    val msgGpsTimeoutContinue = stringResource(id = R.string.status_gps_timeout_continue)
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
    val msgSectorReadyOpenDetails = stringResource(id = R.string.sector_ready_open_details)
    val msgArPermissionDenied = stringResource(id = R.string.ar_compass_permission_denied)
    val msgArOpenFailed = stringResource(id = R.string.ar_compass_open_failed)
    val msgSearchNoResult = stringResource(id = R.string.search_hint_no_result_provider)
    val msgDeviceDirectionOn = stringResource(id = R.string.status_device_direction_on)
    val msgDeviceDirectionOff = stringResource(id = R.string.status_device_direction_off)
    val msgMapNorthUp = stringResource(id = R.string.status_map_north_up)

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
    val msgUnknown = stringResource(id = R.string.generic_unknown)
    val msgUndo = stringResource(id = R.string.action_undo)
    val msgContinueAdd = stringResource(id = R.string.action_continue_add)
    val msgContinueAddHint = stringResource(id = R.string.crosshair_continue_add_subtitle)
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
    val msgLocatedCurrentPosition = stringResource(id = R.string.status_located_current_position)
    val msgCompassLocked = stringResource(id = R.string.status_compass_locked)
    val msgCompassUnlocked = stringResource(id = R.string.status_compass_unlocked)
    val msgLineInfoCollapse = stringResource(id = R.string.action_collapse_details)
    val msgLineInfoExpand = stringResource(id = R.string.action_expand_details)
    val msgFirstUseGuideTitle = stringResource(id = R.string.first_use_guide_title)
    val msgFirstUseGuideLine1 = stringResource(id = R.string.first_use_guide_line_1)
    val msgFirstUseGuideLine2 = stringResource(id = R.string.first_use_guide_line_2)
    val msgFirstUseGuideLine3 = stringResource(id = R.string.first_use_guide_line_3)
    val msgFirstUseGuideLine4 = stringResource(id = R.string.first_use_guide_line_4)
    val msgFirstUseGuideLine5 = stringResource(id = R.string.first_use_guide_line_5)
    val msgSectorSuggestion = stringResource(id = R.string.sector_no_results_suggestion)
    val msgShowAllSectorResults = stringResource(id = R.string.sector_show_all_results)
    val msgShowUnsavedSectorResults = stringResource(id = R.string.sector_show_unsaved_only)
    val actionSectorDetail = stringResource(id = R.string.action_sector_view_details)
    val actionBackToMap = stringResource(id = R.string.action_back_to_map)
    val actionCloseSectorSearch = stringResource(id = R.string.action_close_sector_search)
    val msgSavedToCurrentCase = stringResource(id = R.string.status_saved_to_current_case)
    val msgSwitchOriginLocked = stringResource(id = R.string.status_switched_origin_and_locked)
    val msgUndoDelete = stringResource(id = R.string.status_undo_delete)
    
    // 地图是否已初始化
    val mapReady = remember { mutableStateOf(false) }
    var lastProviderType by remember { mutableStateOf<MapProviderType?>(null) }
    val sensorHelper = remember {
        SensorHelper(context) { heading ->
            deviceHeading = heading
        }
    }

    fun openCaseOpsMenu() {
        quickMenuTarget = QuickMenuTarget.CASE_OPS
    }

    fun openAnalysisMenu() {
        quickMenuTarget = QuickMenuTarget.ANALYSIS
    }

    fun closeQuickMenu() {
        quickMenuTarget = QuickMenuTarget.NONE
    }

    fun showStatus(message: String) {
        statusBannerMessage = message
        statusBannerToken += 1
    }

    fun persistCameraSnapshot(position: CameraPosition?) {
        val snapshot = position ?: return
        if (SystemClock.elapsedRealtime() < suppressPersistUntilMs) return
        if (restoreCameraPosition != null) return
        if (pendingAutoLocateToGps && !hasRealGps) return
        val looksUninitialized =
            kotlin.math.abs(snapshot.target.latitude) < 0.000001 &&
                kotlin.math.abs(snapshot.target.longitude) < 0.000001
        if (!looksUninitialized) {
            MapSessionStore.saveCameraPosition(context, snapshot)
        }
    }

    LaunchedEffect(mapProviderType) {
        if (lastProviderType != null && lastProviderType != mapProviderType) {
            mapReady.value = false
        }
        lastProviderType = mapProviderType
        lineByPolylineId.clear()
        val hasRestoreSnapshot = restoreCameraPosition != null
        pendingAutoLocateToGps = !hasRestoreSnapshot
        shouldAutoLocateOnFirstFix = !hasRestoreSnapshot
    }

    LaunchedEffect(startupLastKnownLocation?.latitude, startupLastKnownLocation?.longitude) {
        val cached = startupLastKnownLocation ?: return@LaunchedEffect
        if (!hasRealGps && realGpsLat == null && realGpsLng == null) {
            realGpsLat = cached.latitude
            realGpsLng = cached.longitude
            hasRealGps = true
        }
    }

    LaunchedEffect(mapReady.value, mapProviderType, currentLanguageTag) {
        if (mapReady.value) {
            val cameraSnapshotBeforeLanguageChange = mapProvider.getCameraPosition()
                ?: lastKnownCameraPosition
                ?: (if (realGpsLat != null && realGpsLng != null) {
                    CameraPosition(
                        target = UniversalLatLng(realGpsLat!!, realGpsLng!!),
                        zoom = (mapProvider.getCameraPosition()?.zoom ?: lastKnownCameraPosition?.zoom ?: 15f),
                        bearing = (mapProvider.getCameraPosition()?.bearing ?: lastKnownCameraPosition?.bearing ?: 0f)
                    )
                } else {
                    null
                })
                ?: MapSessionStore.loadCameraPosition(context)
            suppressPersistUntilMs = SystemClock.elapsedRealtime() + 1200L
            mapProvider.setLanguageTag(currentLanguageTag)
            cameraSnapshotBeforeLanguageChange?.let { snapshot ->
                scope.launch {
                    delay(220)
                    mapProvider.animateCamera(snapshot)
                    lastKnownCameraPosition = snapshot
                    if (hasRealGps) {
                        pendingAutoLocateToGps = false
                        shouldAutoLocateOnFirstFix = false
                    }
                    persistCameraSnapshot(snapshot)
                }
            }
        }
    }

    LaunchedEffect(statusBannerToken) {
        if (statusBannerMessage != null) {
            delay(2000)
            statusBannerMessage = null
        }
    }

    LaunchedEffect(openCaseOpsSignal) {
        if (openCaseOpsSignal > 0) {
            openCaseOpsMenu()
        }
    }

    LaunchedEffect(openAnalysisSignal) {
        if (openAnalysisSignal > 0) {
            openAnalysisMenu()
        }
    }

    LaunchedEffect(closeQuickMenuSignal) {
        if (closeQuickMenuSignal > 0) {
            closeQuickMenu()
        }
    }

    LaunchedEffect(ui.lifeCircleMode) {
        if (ui.lifeCircleMode) {
            closeQuickMenu()
            topSearchLoading = false
            topSearchResultsVisible = false
            lifeCircleTopPanelVisible = true
        } else {
            lifeCircleTopPanelVisible = true
        }
    }

    LaunchedEffect(ui.lifeCircleMode, lifeCircleTopPanelVisible) {
        if (!ui.lifeCircleMode || !lifeCircleTopPanelVisible) {
            lifeCircleOverlayHeightPx = 0
        }
    }

    LaunchedEffect(mapReady.value, pendingAutoLocateToGps, hasLocationPermission, hasRealGps) {
        if (!mapReady.value) return@LaunchedEffect
        if (!pendingAutoLocateToGps) {
            gpsInitializationStartMs = null
            return@LaunchedEffect
        }
        if (hasRealGps) {
            gpsInitializationStartMs = null
            return@LaunchedEffect
        }
        if (!hasLocationPermission) {
            gpsInitializationStartMs = null
            showStatus(msgGpsPermissionMissingContinue)
            return@LaunchedEffect
        }
        if (gpsInitializationStartMs == null) {
            gpsInitializationStartMs = SystemClock.elapsedRealtime()
        }
    }

    LaunchedEffect(gpsInitializationStartMs, pendingAutoLocateToGps, hasRealGps, hasLocationPermission) {
        val start = gpsInitializationStartMs ?: return@LaunchedEffect
        if (!pendingAutoLocateToGps || hasRealGps || !hasLocationPermission) return@LaunchedEffect
        val elapsed = SystemClock.elapsedRealtime() - start
        val remaining = (gpsInitializationTimeoutMs - elapsed).coerceAtLeast(0L)
        if (remaining > 0L) {
            delay(remaining)
        }
        if (pendingAutoLocateToGps && !hasRealGps && hasLocationPermission) {
            pendingAutoLocateToGps = false
            gpsInitializationStartMs = null
            showStatus(msgGpsTimeoutContinue)
        }
    }

    LaunchedEffect(showLayerDialog, mapProviderType) {
        if (showLayerDialog) {
            layerDialogProvider = mapProviderType
        }
    }

    DisposableEffect(deviceDirectionMode) {
        if (deviceDirectionMode) {
            sensorHelper.start()
        } else {
            sensorHelper.stop()
        }
        onDispose {
            sensorHelper.stop()
        }
    }

    LaunchedEffect(deviceDirectionMode, deviceHeading, realGpsLat, realGpsLng) {
        if (!deviceDirectionMode) return@LaunchedEffect
        val lat = realGpsLat ?: return@LaunchedEffect
        val lng = realGpsLng ?: return@LaunchedEffect
        val now = SystemClock.elapsedRealtime()
        if (now - lastDeviceDirectionUpdateMs < 110L) return@LaunchedEffect
        lastDeviceDirectionUpdateMs = now
        val current = mapProvider.getCameraPosition() ?: lastKnownCameraPosition
        val followPosition = CameraPosition(
            target = UniversalLatLng(lat, lng),
            zoom = current?.zoom ?: 16f,
            bearing = deviceHeading
        )
        if (viewModel.applyCameraMove(CameraMoveSource.USER_MANUAL)) {
            mapProvider.animateCamera(followPosition)
            lastKnownCameraPosition = followPosition
            pendingAutoLocateToGps = false
            shouldAutoLocateOnFirstFix = false
        }
    }

    LaunchedEffect(Unit) {
        val guideAcknowledged = Prefs.getBoolean(context, "map_first_guide_seen", false)
        if (!guideAcknowledged) {
            showFirstUseGuide = true
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
            val currentBearing = mapProvider.getCameraPosition()?.bearing
                ?: lastKnownCameraPosition?.bearing
                ?: 0f
            val position = CameraPosition(target = target, zoom = zoom, bearing = currentBearing)
            mapProvider.animateCamera(position)
            lastKnownCameraPosition = position
        }
    }

    fun buildCurrentGpsCameraSnapshot(defaultZoom: Float = 15f): CameraPosition? {
        val lat = realGpsLat ?: return null
        val lng = realGpsLng ?: return null
        val current = mapProvider.getCameraPosition() ?: lastKnownCameraPosition
        return CameraPosition(
            target = UniversalLatLng(lat, lng),
            zoom = current?.zoom ?: defaultZoom,
            bearing = current?.bearing ?: 0f
        )
    }

    fun buildStartupCameraSnapshot(defaultZoom: Float = 15f): CameraPosition? {
        return restoreCameraPosition
            ?: lastKnownCameraPosition
            ?: sessionSavedCameraSnapshot
            ?: buildCurrentGpsCameraSnapshot(defaultZoom)
            ?: startupLastKnownLocation?.let { cached ->
                CameraPosition(
                    target = cached,
                    zoom = defaultZoom,
                    bearing = 0f
                )
            }
    }

    fun buildPreferredSwitchCameraSnapshot(defaultZoom: Float = 15f): CameraPosition? {
        return mapProvider.getCameraPosition()
            ?: lastKnownCameraPosition
            ?: buildCurrentGpsCameraSnapshot(defaultZoom)
            ?: MapSessionStore.loadCameraPosition(context)
    }

    fun fallbackGoogleSatelliteToAmap(cameraSnapshot: CameraPosition?) {
        onMapProviderSwitch(
            MapProviderType.AMAP,
            cameraSnapshot ?: buildCurrentGpsCameraSnapshot() ?: MapSessionStore.loadCameraPosition(context)
        )
        if (!googleSatelliteRestrictionNotified) {
            showStatus(msgGoogleSatelliteFallback)
            googleSatelliteRestrictionNotified = true
        }
    }

    fun locateToCurrentPosition(showBanner: Boolean = true, source: CameraMoveSource = CameraMoveSource.USER_MANUAL) {
        val snapshot = buildCurrentGpsCameraSnapshot()
        if (snapshot != null) {
            requestCameraMove(snapshot.target, snapshot.zoom, source)
            compassLocked = false
            lockedLat = null
            lockedLng = null
            deviceDirectionMode = false
            pendingAutoLocateToGps = false
            shouldAutoLocateOnFirstFix = false
            if (showBanner) {
                showStatus(msgLocatedCurrentPosition)
            }
        } else {
            trialMessage = msgGpsGetting
            showTrialDialog = true
        }
    }

    fun applyMapTypeSelection(type: MapType) {
        val cameraSnapshot = buildPreferredSwitchCameraSnapshot()
        currentMapType = type
        MapSessionStore.saveMapType(context, type)
        if (
            type == MapType.SATELLITE &&
            mapProviderType == MapProviderType.GOOGLE &&
            hasAmapMap
        ) {
            val center = cameraSnapshot?.target
            val inChina = center?.let {
                MapProviderSelector.isInChina(it.latitude, it.longitude)
            } == true
            if (inChina) {
                fallbackGoogleSatelliteToAmap(cameraSnapshot)
                return
            }
        }
        mapProvider.setMapType(type)
        cameraSnapshot?.let { snapshot ->
            scope.launch {
                delay(160)
                mapProvider.animateCamera(snapshot)
                lastKnownCameraPosition = snapshot
                pendingAutoLocateToGps = false
                shouldAutoLocateOnFirstFix = false
            }
        }
    }

    fun isLayerProviderEnabled(provider: MapProviderType): Boolean {
        return when (provider) {
            MapProviderType.AMAP -> hasAmapMap || mapProviderType == MapProviderType.AMAP
            MapProviderType.GOOGLE -> hasGoogleMap || mapProviderType == MapProviderType.GOOGLE
        }
    }

    fun applyLayerSelection(provider: MapProviderType, type: MapType) {
        if (!isLayerProviderEnabled(provider)) return

        currentMapType = type
        MapSessionStore.saveMapType(context, type)

        val cameraSnapshot = buildPreferredSwitchCameraSnapshot()
        if (
            type == MapType.SATELLITE &&
            provider == MapProviderType.GOOGLE &&
            hasAmapMap
        ) {
            val center = cameraSnapshot?.target
            val inChina = center?.let {
                MapProviderSelector.isInChina(it.latitude, it.longitude)
            } == true
            if (inChina) {
                fallbackGoogleSatelliteToAmap(cameraSnapshot)
                return
            }
        }

        if (provider == mapProviderType) {
            applyMapTypeSelection(type)
        } else {
            onMapProviderSwitch(provider, cameraSnapshot)
        }
    }

    fun switchProviderFromLayer(provider: MapProviderType) {
        if (!isLayerProviderEnabled(provider)) return
        val cameraSnapshot = buildPreferredSwitchCameraSnapshot()
        var targetType = currentMapType
        if (
            provider == MapProviderType.GOOGLE &&
            targetType == MapType.SATELLITE &&
            hasAmapMap
        ) {
            val center = cameraSnapshot?.target
            val inChina = center?.let {
                MapProviderSelector.isInChina(it.latitude, it.longitude)
            } == true
            if (inChina) {
                targetType = MapType.VECTOR
            }
        }
        if (targetType != currentMapType) {
            currentMapType = targetType
            MapSessionStore.saveMapType(context, targetType)
        }
        if (provider == mapProviderType) {
            applyMapTypeSelection(targetType)
        } else {
            onMapProviderSwitch(provider, cameraSnapshot)
        }
    }

    fun resetMapBearingToNorth() {
        val current = mapProvider.getCameraPosition() ?: lastKnownCameraPosition
        if (current != null && viewModel.applyCameraMove(CameraMoveSource.USER_MANUAL)) {
            val northPosition = current.copy(bearing = 0f)
            mapProvider.animateCamera(northPosition)
            lastKnownCameraPosition = northPosition
            showStatus(msgMapNorthUp)
        }
    }

    fun isCameraNearCurrentLocation(thresholdMeters: Double = 35.0): Boolean {
        val gpsLat = realGpsLat ?: return false
        val gpsLng = realGpsLng ?: return false
        val currentCenter = mapProvider.getCameraPosition()?.target ?: lastKnownCameraPosition?.target ?: return false
        return RhumbLineUtils.calculateRhumbDistance(
            UniversalLatLng(gpsLat, gpsLng),
            currentCenter
        ) <= thresholdMeters
    }

    fun locateToCurrentAndNorth() {
        val snapshot = buildCurrentGpsCameraSnapshot()
        if (snapshot == null) {
            trialMessage = msgGpsGetting
            showTrialDialog = true
            return
        }
        if (viewModel.applyCameraMove(CameraMoveSource.USER_MANUAL)) {
            val northPosition = snapshot.copy(bearing = 0f)
            mapProvider.animateCamera(northPosition)
            lastKnownCameraPosition = northPosition
            pendingAutoLocateToGps = false
            shouldAutoLocateOnFirstFix = false
            deviceDirectionMode = false
            compassLocked = false
            lockedLat = null
            lockedLng = null
            showStatus(msgLocatedCurrentPosition)
        }
    }

    fun onMyLocationClicked() {
        if (realGpsLat == null || realGpsLng == null) {
            trialMessage = msgGpsGetting
            showTrialDialog = true
            return
        }
        if (!isCameraNearCurrentLocation()) {
            locateToCurrentAndNorth()
        } else {
            deviceDirectionMode = !deviceDirectionMode
            showStatus(if (deviceDirectionMode) msgDeviceDirectionOn else msgDeviceDirectionOff)
        }
    }

    LaunchedEffect(forceRelocateSignal) {
        if (forceRelocateSignal > 0) {
            pendingAutoLocateToGps = true
            shouldAutoLocateOnFirstFix = true
            suppressAutoLocateOnce = false
            if (realGpsLat != null && realGpsLng != null) {
                locateToCurrentAndNorth()
            }
        }
    }

    fun runTopSearch() {
        val keyword = topSearchInput.text.trim()
        if (keyword.isBlank()) {
            trialMessage = context.getString(R.string.err_enter_keyword)
            showTrialDialog = true
            return
        }
        val anchor = mapProvider.getCameraPosition()?.target
            ?: buildCurrentGpsCameraSnapshot()?.target
            ?: lastKnownCameraPosition?.target
        topSearchLoading = true
        scope.launch {
            val providers = buildPoiProviderChain(keyword)
            var found = emptyList<PoiResult>()
            providers.forEach { provider ->
                if (found.isNotEmpty()) return@forEach
                val result = runCatching {
                    provider.searchByKeyword(
                        keyword = keyword,
                        location = anchor,
                        radiusMeters = 50_000
                    )
                }.getOrDefault(emptyList())
                if (result.isNotEmpty()) {
                    found = if (anchor != null) {
                        result.sortedBy {
                            RhumbLineUtils.calculateRhumbDistance(
                                anchor,
                                UniversalLatLng(it.lat, it.lng)
                            )
                        }
                    } else {
                        result
                    }
                }
            }
            topSearchLoading = false
            topSearchResults = found
            topSearchResultsVisible = true
            if (found.isEmpty()) {
                showStatus(msgSearchNoResult)
            }
        }
    }

    fun markUserManualCamera() {
        viewModel.markUserManualCamera()
    }

    fun refreshNormalLines() {
        lineRefreshToken += 1
    }

    fun drawLifeCircleLines(data: LifeCircleData) {
        mapProvider.clearPolylines()
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
        closeQuickMenu()
        ui.lifeCircleData?.homePoint?.let { home ->
            lockedLat = home.latitude
            lockedLng = home.longitude
            compassLocked = true
            try {
                val screenPos = mapProvider.latLngToScreenLocation(
                    UniversalLatLng(home.latitude, home.longitude)
                )
                compassScreenPos = Offset(screenPos.x, screenPos.y)
            } catch (_: Exception) {
                compassScreenPos = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
            }
        }
        ui.lifeCircleData?.let { drawLifeCircleLines(it) }
    }

    fun exitLifeCircleMode() {
        compassLocked = false
        lockedLat = null
        lockedLng = null
        viewModel.exitLifeCircleMode()
        refreshNormalLines()
    }

    fun clearPoiMarkers() {
        mapProvider.clearMarkers()
        poiByMarkerId.clear()
    }

    fun renderPointMarkers(clearExisting: Boolean = true) {
        if (ui.lifeCircleMode) return
        if (clearExisting) {
            mapProvider.clearMarkers()
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
        val sectorOrigin = ui.sectorOrigin
        if (ui.sectorOverlayVisible && sectorOrigin != null) {
            try {
                mapProvider.addMarker(
                    sectorOrigin,
                    "[ORIGIN_ACTIVE] ${context.getString(R.string.marker_sector_origin)}"
                )
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "Failed to add sector origin marker: ${e.message}")
            }
        }
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
        ui.sectorUseMapCenterOrigin = false
        ui.sectorResults.clear()
        ui.showSectorResultDialog = false
        pendingSectorLocatePoi = null
        showPoiMarkers(emptyList())
        val anchor = ui.sectorOrigin
            ?: selectedOriginPoint?.let { UniversalLatLng(it.latitude, it.longitude) }
            ?: mapProvider.getCameraPosition()?.target
        if (anchor != null) {
            lockedLat = anchor.latitude
            lockedLng = anchor.longitude
            compassLocked = true
            try {
                val screenPos = mapProvider.latLngToScreenLocation(anchor)
                compassScreenPos = Offset(screenPos.x, screenPos.y)
            } catch (_: Exception) {
                compassScreenPos = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
            }
        }
    }

    fun focusOnSectorResults(
        results: List<PoiResult>,
        origin: UniversalLatLng?,
        config: SectorConfig?
    ) {
        val boundsPoints = mutableListOf<UniversalLatLng>()
        results.forEach { poi ->
            boundsPoints.add(UniversalLatLng(poi.lat, poi.lng))
        }
        if (origin != null) {
            boundsPoints.add(origin)
            if (config != null) {
                boundsPoints.add(
                    RhumbLineUtils.calculateRhumbDestination(
                        start = origin,
                        bearing = config.startAngle,
                        distanceMeters = config.maxDistanceMeters
                    )
                )
                boundsPoints.add(
                    RhumbLineUtils.calculateRhumbDestination(
                        start = origin,
                        bearing = config.endAngle,
                        distanceMeters = config.maxDistanceMeters
                    )
                )
                val span = sectorSpanDegrees(config.startAngle, config.endAngle)
                val stepCount = 24
                for (index in 1 until stepCount) {
                    val angle = (config.startAngle + span * (index.toFloat() / stepCount.toFloat())) % 360f
                    boundsPoints.add(
                        RhumbLineUtils.calculateRhumbDestination(
                            start = origin,
                            bearing = angle,
                            distanceMeters = config.maxDistanceMeters
                        )
                    )
                }
            }
        }
        if (boundsPoints.isEmpty()) return
        val minLat = boundsPoints.minOf { it.latitude }
        val maxLat = boundsPoints.maxOf { it.latitude }
        val minLng = boundsPoints.minOf { it.longitude }
        val maxLng = boundsPoints.maxOf { it.longitude }
        val latPad = ((maxLat - minLat) * 0.15).coerceAtLeast(0.002)
        val lngPad = ((maxLng - minLng) * 0.15).coerceAtLeast(0.002)
        val fitBounds = com.fengshui.app.map.abstraction.UniversalLatLngBounds(
            southwest = UniversalLatLng(minLat - latPad, minLng - lngPad),
            northeast = UniversalLatLng(maxLat + latPad, maxLng + lngPad)
        )
        val minScreenEdge = screenWidthPx.coerceAtMost(screenHeightPx)
        val basePadding = with(density) { 208.dp.toPx() }.toInt()
        val fitPadding = basePadding.coerceAtMost((minScreenEdge * 0.42f).toInt()).coerceAtLeast(140)

        mapProvider.animateCameraToBounds(
            fitBounds,
            padding = fitPadding
        )

        // Re-fit after first animation and bias center upward to avoid bottom overlays covering POI markers.
        scope.launch {
            delay(320)
            mapProvider.animateCameraToBounds(
                fitBounds,
                padding = (fitPadding * 1.12f).toInt()
            )
            delay(180)
            val current = mapProvider.getCameraPosition() ?: lastKnownCameraPosition
            if (current != null) {
                val topInsetPx = with(density) { 96.dp.toPx() }
                val bottomInsetPx = with(density) { 182.dp.toPx() }
                val shiftYPx = ((bottomInsetPx - topInsetPx) / 2f)
                    .coerceIn(-screenHeightPx * 0.2f, screenHeightPx * 0.2f)
                if (kotlin.math.abs(shiftYPx) > 2f) {
                    val target = mapProvider.screenLocationToLatLng(
                        screenWidthPx / 2f,
                        (screenHeightPx / 2f) + shiftYPx
                    )
                    val shifted = current.copy(target = target)
                    mapProvider.animateCamera(shifted)
                    lastKnownCameraPosition = shifted
                }
            }
        }
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
        return ui.lifeCircleConnections
            .filter { it.toPoint.id == targetId }
            .map { conn ->
                val shan = ShanTextResolver.shanName(context, RhumbLineUtils.getShanIndex(conn.bearing))
                context.getString(
                    R.string.life_circle_connection_item,
                    conn.fromPoint.name,
                    conn.bearing,
                    shan,
                    conn.distance / 1000f
                )
            }
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

    fun currentCompassAnchor(): UniversalLatLng? {
        if (compassLocked && lockedLat != null && lockedLng != null) {
            return UniversalLatLng(lockedLat!!, lockedLng!!)
        }
        if (realGpsLat != null && realGpsLng != null) {
            return UniversalLatLng(realGpsLat!!, realGpsLng!!)
        }
        return null
    }

    fun resolveCrosshairCenterAnchor(): UniversalLatLng? {
        return runCatching {
            mapProvider.screenLocationToLatLng(screenWidthPx / 2f, screenHeightPx / 2f)
        }.getOrNull()
            ?: ui.crosshairLocation
            ?: mapProvider.getCameraPosition()?.target
            ?: lastKnownCameraPosition?.target
    }

    fun continuousAddAnchor(): UniversalLatLng? {
        return resolveCrosshairCenterAnchor()
            ?: currentCompassAnchor()
    }

    fun onPointAdded(point: FengShuiPoint, type: PointType) {
        lastAddedPoint = point
        lastAddedPointType = type
        showStatus(
            if (type == PointType.ORIGIN) {
                context.getString(R.string.status_added_origin, point.name)
            } else {
                context.getString(R.string.status_added_destination, point.name)
            }
        )
        if (continuousAddMode) {
            showPostSaveQuickActions = false
            viewModel.openCrosshair(
                crosshairManualTitle,
                msgContinueAddHint,
                continuousAddAnchor()
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
        showStatus(context.getString(R.string.status_deleted_with_undo, point.name))
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
            lastCompassUpdateMs = android.os.SystemClock.elapsedRealtime()
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
        onQuickAddConsumed?.invoke()
    }
    
    // 当linesList改变时，重新绘制所有连线
    LaunchedEffect(linesList.size, lineRefreshToken, ui.lifeCircleMode, mapReady.value, mapProviderType) {
        if (ui.lifeCircleMode) {
            return@LaunchedEffect
        }
        if (mapReady.value) {
            // 清除旧的polylines
            mapProvider.clearPolylines()
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
            if (mapReady.value && shouldAutoLocateOnFirstFix && !suppressAutoLocateOnce) {
                requestCameraMove(
                    UniversalLatLng(lat, lng),
                    15f,
                    CameraMoveSource.GPS_AUTO_LOCATE
                )
                pendingAutoLocateToGps = false
                shouldAutoLocateOnFirstFix = false
            }
        }
    }

    // 罗盘显示的坐标（根据锁定状态决定）
    // 已删除旧的 compassLat/compassLng 逻辑，改用 lockedLat/lockedLng

    DisposableEffect(mapProviderType) {
        locationHelper.start()  // 启动GPS定位
        val initCamera = mapProvider.getCameraPosition()
        if (initCamera != null) {
            lastKnownCameraPosition = initCamera
        }
        val initBearing = initCamera?.bearing ?: 0f
        azimuth = if (mapProviderType == MapProviderType.GOOGLE) -initBearing else initBearing
        
        // 注册地图相机移动监听，用于更新锁定模式下罗盘位置
        mapProvider.onCameraChange { cam ->
            val now = SystemClock.elapsedRealtime()
            if (now - ui.lastProgrammaticMoveTimestamp > 700) {
                markUserManualCamera()
            }
            // Compass follows map bearing only (not device sensors).
            azimuth = if (mapProviderType == MapProviderType.GOOGLE) -cam.bearing else cam.bearing
            lastKnownCameraPosition = cam
            if (compassLocked && lockedLat != null && lockedLng != null) {
                updateCompassScreenPosition()
            }
            mapCameraTick += 1
        }
        mapProvider.onCameraChangeFinish { cam ->
            azimuth = if (mapProviderType == MapProviderType.GOOGLE) -cam.bearing else cam.bearing
            lastKnownCameraPosition = cam
            persistCameraSnapshot(cam)
            if (compassLocked && lockedLat != null && lockedLng != null) {
                updateCompassScreenPosition()
            }
            mapCameraTick += 1
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
        }
    }

    LaunchedEffect(mapReady.value, realGpsLat, realGpsLng, shouldAutoLocateOnFirstFix, suppressAutoLocateOnce) {
        if (
            mapReady.value &&
            shouldAutoLocateOnFirstFix &&
            hasRealGps &&
            realGpsLat != null &&
            realGpsLng != null &&
            !suppressAutoLocateOnce
        ) {
            requestCameraMove(
                UniversalLatLng(realGpsLat!!, realGpsLng!!),
                15f,
                CameraMoveSource.GPS_AUTO_LOCATE
            )
            pendingAutoLocateToGps = false
            shouldAutoLocateOnFirstFix = false
        }
    }

    LaunchedEffect(restoreCameraPosition) {
        if (restoreCameraPosition != null) {
            pendingAutoLocateToGps = false
            shouldAutoLocateOnFirstFix = false
        }
    }

    LaunchedEffect(mapProviderType, mapReady.value, restoreCameraPosition) {
        val snapshot = restoreCameraPosition
        if (mapReady.value && snapshot != null) {
            mapProvider.animateCamera(snapshot)
            lastKnownCameraPosition = snapshot
            pendingAutoLocateToGps = false
            shouldAutoLocateOnFirstFix = false
            persistCameraSnapshot(snapshot)
            onRestoreCameraConsumed?.invoke()
        }
    }

    fun showLineInfoFor(line: LineData) {
        val bearing = RhumbLineUtils.calculateRhumbBearing(
            line.origin.latitude, line.origin.longitude,
            line.destination.latitude, line.destination.longitude
        )
        val shanIndex = RhumbLineUtils.getShanIndex(bearing)
        val shan = ShanTextResolver.shanName(context, shanIndex)
        val bagua = ShanTextResolver.baguaName(context, ShanUtils.getBaGuaByIndex(shanIndex))
        val wuxing = ShanTextResolver.wuxingName(context, ShanUtils.getWuXingByIndex(shanIndex))
        val dist = RhumbLineUtils.haversineDistanceMeters(
            line.origin.latitude, line.origin.longitude,
            line.destination.latitude, line.destination.longitude
        )
        lineInfoSummary = context.getString(
            R.string.line_info_summary,
            line.origin.name,
            line.destination.name,
            bearing,
            shan,
            dist
        )
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
                    val startupSnapshot = buildStartupCameraSnapshot(defaultZoom = 15f)
                    GoogleMapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f),
                        initialZoom = startupSnapshot?.zoom ?: 15f,
                        initialCenter = startupSnapshot?.target?.let { center ->
                            com.google.android.gms.maps.model.LatLng(center.latitude, center.longitude)
                        },
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
                            if (startupSnapshot != null) {
                                mapProvider.animateCamera(startupSnapshot)
                                lastKnownCameraPosition = startupSnapshot
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
                            val startupSnapshot = buildStartupCameraSnapshot(defaultZoom = 15f)
                            if (startupSnapshot != null) {
                                mapProvider.animateCamera(startupSnapshot)
                                lastKnownCameraPosition = startupSnapshot
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
                        Text(stringResource(id = R.string.map_provider_unsupported))
                    }
                }
            }

            if (!ui.lifeCircleMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .zIndex(25f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = topSearchInput,
                            onValueChange = { topSearchInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { runTopSearch() }),
                            placeholder = { Text(stringResource(id = R.string.map_top_search_hint), fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(id = R.string.action_search)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { runTopSearch() }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(id = R.string.action_search)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(22.dp)
                        )
                        IconButton(
                            onClick = {
                                if (onOpenSettings != null) {
                                    onOpenSettings.invoke()
                                }
                            },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .background(Color(0xEFFFFFFF), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.nav_settings),
                                tint = Color(0xFF2A2A2A)
                            )
                        }
                    }

                    if (topSearchLoading) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(Color(0xECFFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.search_loading),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (topSearchResultsVisible) {
                        Column(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .background(Color(0xF4FFFFFF), RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color(0x22000000), RoundedCornerShape(12.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            if (topSearchResults.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.search_empty),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else {
                                topSearchResults.take(10).forEach { poi ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                topSearchResultsVisible = false
                                                topSearchInput = TextFieldValue(poi.name)
                                                val target = UniversalLatLng(poi.lat, poi.lng)
                                                requestCameraMove(target, 16f, CameraMoveSource.SEARCH_RESULT)
                                                showPoiMarkers(listOf(poi))
                                            }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(text = poi.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        poi.address?.takeIf { it.isNotBlank() }?.let { address ->
                                            Text(text = address, fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Text(
                                            text = stringResource(id = R.string.search_result_coordinates, poi.lat, poi.lng),
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val rightControlsTopPadding = if (ui.lifeCircleMode && lifeCircleTopPanelVisible) {
                if (lifeCircleOverlayHeightPx > 0) {
                    with(density) { lifeCircleOverlayHeightPx.toDp() } + 12.dp
                } else {
                    116.dp
                }
            } else {
                88.dp
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = rightControlsTopPadding)
                    .zIndex(24f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { showLayerDialog = true },
                    modifier = Modifier
                        .background(Color(0xEFFFFFFF), RoundedCornerShape(12.dp))
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = stringResource(id = R.string.action_layer_switch)
                    )
                }
                IconButton(
                    onClick = { resetMapBearingToNorth() },
                    modifier = Modifier
                        .background(Color(0xEFFFFFFF), RoundedCornerShape(12.dp))
                        .size(42.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(-azimuth)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(
                                color = Color(0xFF2F3136),
                                radius = radius * 0.96f,
                                center = center
                            )
                            drawCircle(
                                color = Color(0x22000000),
                                radius = radius * 0.96f,
                                center = center,
                                style = Stroke(width = radius * 0.08f)
                            )

                            val northNeedle = Path().apply {
                                moveTo(center.x, center.y - radius * 0.86f)
                                lineTo(center.x - radius * 0.24f, center.y + radius * 0.08f)
                                lineTo(center.x + radius * 0.24f, center.y + radius * 0.08f)
                                close()
                            }
                            drawPath(
                                path = northNeedle,
                                color = Color(0xFFD93025)
                            )

                            val southNeedle = Path().apply {
                                moveTo(center.x, center.y + radius * 0.86f)
                                lineTo(center.x - radius * 0.22f, center.y - radius * 0.04f)
                                lineTo(center.x + radius * 0.22f, center.y - radius * 0.04f)
                                close()
                            }
                            drawPath(
                                path = southNeedle,
                                color = Color(0xFFEFF2F6)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = radius * 0.13f,
                                center = center
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        if (ui.lifeCircleMode) {
                            showStatus(msgCompassLocked)
                            return@IconButton
                        }
                        if (!compassLocked) {
                            val currentPos = mapProvider.getCameraPosition()?.target
                            if (currentPos != null) {
                                lockCompassToLatLng(currentPos.latitude, currentPos.longitude)
                                showStatus(msgCompassLocked)
                            } else {
                                trialMessage = msgNoLocation
                                showTrialDialog = true
                            }
                        } else {
                            unlockCompass()
                            showStatus(msgCompassUnlocked)
                        }
                    },
                    modifier = Modifier
                        .background(
                            color = if (ui.lifeCircleMode || compassLocked) Color(0xFF6A4FB5) else Color(0xEFFFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = if (ui.lifeCircleMode || compassLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = stringResource(id = R.string.action_lock_compass_toggle),
                        tint = if (ui.lifeCircleMode || compassLocked) Color.White else Color(0xFF2A2A2A)
                    )
                }
                IconButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            arCompassEnabled = true
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .background(
                            color = if (arCompassEnabled) Color(0xFF6A4FB5) else Color(0xEFFFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = stringResource(id = R.string.action_ar_compass),
                        tint = if (arCompassEnabled) Color.White else Color(0xFF2A2A2A)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 84.dp)
                    .zIndex(24f)
            ) {
                IconButton(
                    onClick = { onMyLocationClicked() },
                    modifier = Modifier
                        .background(
                            color = if (deviceDirectionMode) Color(0xFF1A73E8) else Color(0xEFFFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(id = R.string.action_locate_short),
                        tint = if (deviceDirectionMode) Color.White else Color(0xFF1A73E8)
                    )
                }
            }

            statusBannerMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 82.dp)
                        .zIndex(25f)
                        .background(Color(0xE62B2B2B), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(msg, color = Color.White, fontSize = 12.sp)
                }
            }

            if (showLayerDialog) {
                ModalBottomSheet(
                    onDismissRequest = { showLayerDialog = false },
                    sheetState = layerSheetState,
                    containerColor = Color(0xFFFAFAFA)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.layer_dialog_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showLayerDialog = false }) {
                                Text(
                                    text = stringResource(id = R.string.symbol_close),
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.layer_provider_section),
                            fontSize = 13.sp,
                            color = Color(0xFF70757A),
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val amapEnabled = isLayerProviderEnabled(MapProviderType.AMAP)
                            val amapSelected = layerDialogProvider == MapProviderType.AMAP
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = if (amapEnabled) 1f else 0.45f }
                                    .clickable(enabled = amapEnabled) {
                                        layerDialogProvider = MapProviderType.AMAP
                                        switchProviderFromLayer(MapProviderType.AMAP)
                                        showLayerDialog = false
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (amapSelected) Color(0xFFE7F2FF) else Color(0xFFF2F3F5),
                                tonalElevation = if (amapSelected) 2.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = stringResource(id = R.string.provider_amap),
                                        tint = if (amapSelected) Color(0xFF1A73E8) else Color(0xFF5F6368),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.provider_amap),
                                        fontSize = 13.sp,
                                        fontWeight = if (amapSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            val googleEnabled = isLayerProviderEnabled(MapProviderType.GOOGLE)
                            val googleSelected = layerDialogProvider == MapProviderType.GOOGLE
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = if (googleEnabled) 1f else 0.45f }
                                    .clickable(enabled = googleEnabled) {
                                        layerDialogProvider = MapProviderType.GOOGLE
                                        switchProviderFromLayer(MapProviderType.GOOGLE)
                                        showLayerDialog = false
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (googleSelected) Color(0xFFE7F2FF) else Color(0xFFF2F3F5),
                                tonalElevation = if (googleSelected) 2.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = stringResource(id = R.string.provider_google_map_full),
                                        tint = if (googleSelected) Color(0xFF1A73E8) else Color(0xFF5F6368),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.provider_google_map_full),
                                        fontSize = 13.sp,
                                        fontWeight = if (googleSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = Color(0x22000000)
                        )

                        Text(
                            text = stringResource(id = R.string.layer_detail_section),
                            fontSize = 13.sp,
                            color = Color(0xFF70757A),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val vectorSelected = currentMapType == MapType.VECTOR
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        applyLayerSelection(layerDialogProvider, MapType.VECTOR)
                                        showLayerDialog = false
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (vectorSelected) Color(0xFFE7F2FF) else Color(0xFFF2F3F5),
                                tonalElevation = if (vectorSelected) 2.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = stringResource(id = R.string.map_type_vector),
                                        tint = if (vectorSelected) Color(0xFF1A73E8) else Color(0xFF5F6368),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.map_type_vector),
                                        fontSize = 13.sp,
                                        fontWeight = if (vectorSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            val satelliteSelected = currentMapType == MapType.SATELLITE
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        applyLayerSelection(layerDialogProvider, MapType.SATELLITE)
                                        showLayerDialog = false
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (satelliteSelected) Color(0xFFE7F2FF) else Color(0xFFF2F3F5),
                                tonalElevation = if (satelliteSelected) 2.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SatelliteAlt,
                                        contentDescription = stringResource(id = R.string.map_type_satellite),
                                        tint = if (satelliteSelected) Color(0xFF1A73E8) else Color(0xFF5F6368),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.map_type_satellite),
                                        fontSize = 13.sp,
                                        fontWeight = if (satelliteSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.size(12.dp))
                    }
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
                    val origin = ui.sectorOrigin
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
                        val target = if (continuousAddMode) {
                            continuousAddAnchor()
                        } else {
                            resolveCrosshairCenterAnchor() ?: currentCompassAnchor()
                        }
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
                        val target = if (continuousAddMode) {
                            continuousAddAnchor()
                        } else {
                            resolveCrosshairCenterAnchor() ?: currentCompassAnchor()
                        }
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

            if (quickMenuTarget != QuickMenuTarget.NONE) {
                val panelTitle = when (quickMenuTarget) {
                    QuickMenuTarget.CASE_OPS -> stringResource(id = R.string.nav_case_ops)
                    QuickMenuTarget.ANALYSIS -> stringResource(id = R.string.nav_analysis)
                    QuickMenuTarget.NONE -> ""
                }

                ModalBottomSheet(
                    onDismissRequest = { closeQuickMenu() },
                    sheetState = quickSheetState,
                    containerColor = Color(0xFFFDFDFD)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(quickMenuScrollState)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = panelTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { closeQuickMenu() }) {
                                Text(stringResource(id = R.string.action_close), fontSize = 12.sp)
                            }
                        }

                        SpacerSmall()

                        val panelButtonModifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)

                        when (quickMenuTarget) {
                            QuickMenuTarget.CASE_OPS -> {
                                Text(
                                    text = stringResource(id = R.string.subsection_case_select),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF666666)
                                )
                                SpacerSmall()
                                Button(onClick = { showProjectSelectDialog = true }, modifier = panelButtonModifier) {
                                    Text(
                                        stringResource(
                                            id = R.string.label_case_with_name,
                                            currentProject?.name ?: caseNone
                                        ),
                                        fontSize = 11.sp
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
                                }, modifier = panelButtonModifier) {
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
                                }, modifier = panelButtonModifier) {
                                    Text(actionSelectDestination, fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.size(10.dp))
                                Text(
                                    text = stringResource(id = R.string.subsection_case_edit),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF666666)
                                )
                                SpacerSmall()
                                Button(onClick = {
                                    addPointName = ""
                                    addPointType = continuousAddType
                                    addPointProjectId = currentProject?.id
                                    addPointUseNewProject = false
                                    addPointNewProjectName = ""
                                    showAddPointDialog = true
                                }, modifier = panelButtonModifier) {
                                    Text(stringResource(id = R.string.action_add_point), fontSize = 11.sp)
                                }
                                SpacerSmall()
                                Button(onClick = { continuousAddMode = !continuousAddMode }, modifier = panelButtonModifier) {
                                    Text(
                                        if (continuousAddMode) actionContinuousAddModeOn else actionContinuousAddModeOff,
                                        fontSize = 11.sp
                                    )
                                }
                                SpacerSmall()
                                Button(
                                    onClick = {
                                        continuousAddType = if (continuousAddType == PointType.ORIGIN) {
                                            PointType.DESTINATION
                                        } else {
                                            PointType.ORIGIN
                                        }
                                    },
                                    modifier = panelButtonModifier
                                ) {
                                    Text(actionContinuousCurrentType, fontSize = 10.sp)
                                }
                            }

                            QuickMenuTarget.ANALYSIS -> {
                                Text(
                                    text = stringResource(id = R.string.subsection_analysis_core),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF666666)
                                )
                                SpacerSmall()
                                Button(onClick = {
                                    if (originPoints.size < 3) {
                                        trialMessage = msgNeedThreeOrigins
                                        showTrialDialog = true
                                        return@Button
                                    }
                                    lifeCircleHomeId = null
                                    lifeCircleWorkId = null
                                    lifeCircleWizardStep = 1
                                }, modifier = panelButtonModifier) {
                                    Text(stringResource(id = R.string.action_life_circle_mode), fontSize = 11.sp)
                                }
                                SpacerSmall()
                                Button(onClick = {
                                    closeQuickMenu()
                                    ui.showSectorConfigDialog = true
                                }, modifier = panelButtonModifier) {
                                    Text(stringResource(id = R.string.action_sector_search), fontSize = 11.sp)
                                }
                                if (originPoint != null && destPoint != null) {
                                    SpacerSmall()
                                    Button(onClick = {
                                        val bearing = RhumbLineUtils.calculateRhumbBearing(
                                            originPoint!!.latitude,
                                            originPoint!!.longitude,
                                            destPoint!!.latitude,
                                            destPoint!!.longitude
                                        )
                                        val shanIndex = RhumbLineUtils.getShanIndex(bearing)
                                        val shan = ShanTextResolver.shanName(context, shanIndex)
                                        val bagua = ShanTextResolver.baguaName(context, ShanUtils.getBaGuaByIndex(shanIndex))
                                        val wuxing = ShanTextResolver.wuxingName(context, ShanUtils.getWuXingByIndex(shanIndex))
                                        val dist = RhumbLineUtils.haversineDistanceMeters(
                                            originPoint!!.latitude,
                                            originPoint!!.longitude,
                                            destPoint!!.latitude,
                                            destPoint!!.longitude
                                        )
                                        lineInfoSummary = context.getString(
                                            R.string.line_info_summary,
                                            originPoint!!.name,
                                            destPoint!!.name,
                                            bearing,
                                            shan,
                                            dist
                                        )
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
                                    }, modifier = panelButtonModifier) {
                                        Text(stringResource(id = R.string.action_show_line_info), fontSize = 11.sp)
                                    }
                                }
                            }

                            QuickMenuTarget.NONE -> Unit
                        }

                        Spacer(modifier = Modifier.size(8.dp))
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
                                        continuousAddAnchor()
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
                            text = stringResource(id = R.string.status_deleted_simple, deleted.name),
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
                                            showStatus(msgUndoDelete)
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
                            .graphicsLayer {
                                translationX = compassScreenPos.x - compassRadiusPx
                                translationY = compassScreenPos.y - compassRadiusPx
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
                val data = ui.lifeCircleData

                if (lifeCircleTopPanelVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(4f)
                            .padding(end = 78.dp)
                            .onGloballyPositioned { coordinates ->
                                lifeCircleOverlayHeightPx = coordinates.size.height
                            }
                    ) {
                        Column {
                            LifeCircleBanner(
                                onShowInfo = { ui.showLifeCircleInfoDialog = true },
                                topPanelVisible = lifeCircleTopPanelVisible,
                                onToggleTopPanel = {
                                    lifeCircleTopPanelVisible = false
                                },
                                onExit = {
                                    lifeCircleTopPanelVisible = true
                                    exitLifeCircleMode()
                                }
                            )

                            if (data != null) {
                                val homeLabels = buildLifeCircleLabels(data.homePoint.id)
                                val workLabels = buildLifeCircleLabels(data.workPoint.id)
                                val entertainmentLabels = buildLifeCircleLabels(data.entertainmentPoint.id)
                                LifeCircleLabelPanel(
                                    homeLabels = homeLabels,
                                    workLabels = workLabels,
                                    entertainmentLabels = entertainmentLabels
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                            .zIndex(5f)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { lifeCircleTopPanelVisible = true }) {
                                Text(text = stringResource(id = R.string.action_show_top_panel), fontSize = 11.sp)
                            }
                            Button(onClick = { exitLifeCircleMode() }) {
                                Text(text = stringResource(id = R.string.action_exit), fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (data != null) {
                    val lifePoints = remember(data, mapCameraTick) {
                        listOf(data.homePoint, data.workPoint, data.entertainmentPoint)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1.3f)
                    ) {
                        val lifeCompassSize = 156.dp
                        val lifeCompassRadiusPx = with(density) { lifeCompassSize.toPx() / 2f }
                        lifePoints.forEach { point ->
                            val screenPos = runCatching {
                                mapProvider.latLngToScreenLocation(
                                    UniversalLatLng(point.latitude, point.longitude)
                                )
                            }.getOrNull() ?: return@forEach
                            Box(
                                modifier = Modifier.offset {
                                    IntOffset(
                                        (screenPos.x - lifeCompassRadiusPx).toInt(),
                                        (screenPos.y - lifeCompassRadiusPx).toInt()
                                    )
                                }
                            ) {
                                CompassOverlay(
                                    azimuthDegrees = azimuth,
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    sizeDp = lifeCompassSize,
                                    showInfo = false,
                                    labelScale = 0.78f
                                )
                            }
                        }
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

            if (gpsInitializationBlocking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(35f)
                        .background(Color(0xFF121212))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = stringResource(id = R.string.gps_locating),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = msgGpsGetting,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Button(
                            onClick = {
                                pendingAutoLocateToGps = false
                                gpsInitializationStartMs = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = stringResource(id = R.string.action_continue_without_gps),
                                color = Color(0xFF222222),
                                fontSize = 12.sp
                            )
                        }
                    }
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
                            Text(if (lineInfoExpanded) msgLineInfoCollapse else msgLineInfoExpand)
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
                                Text(
                                    stringResource(
                                        id = R.string.label_role_value,
                                        stringResource(id = R.string.life_circle_role_home),
                                        it
                                    ),
                                    fontSize = 12.sp
                                )
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
                    onDismissRequest = { },
                    title = { Text(msgFirstUseGuideTitle) },
                    text = {
                        Column {
                            Text(msgFirstUseGuideLine1)
                            SpacerSmall()
                            Text(msgFirstUseGuideLine2)
                            SpacerSmall()
                            Text(msgFirstUseGuideLine3)
                            SpacerSmall()
                            Text(msgFirstUseGuideLine4)
                            SpacerSmall()
                            Text(msgFirstUseGuideLine5)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showFirstUseGuide = false
                            Prefs.saveBoolean(context, "map_first_guide_seen", true)
                        }) {
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
                                Text(
                                    stringResource(
                                        id = R.string.label_role_value,
                                        stringResource(id = R.string.life_circle_role_home),
                                        it
                                    ),
                                    fontSize = 12.sp
                                )
                            }
                            selectedWorkName?.let {
                                SpacerSmall()
                                Text(
                                    stringResource(
                                        id = R.string.label_role_value,
                                        stringResource(id = R.string.life_circle_role_work),
                                        it
                                    ),
                                    fontSize = 12.sp
                                )
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
                                Text(
                                    stringResource(
                                        id = R.string.label_role_value,
                                        stringResource(id = R.string.life_circle_role_home),
                                        it
                                    ),
                                    fontSize = 12.sp
                                )
                            }
                            selectedWorkName?.let {
                                SpacerSmall()
                                Text(
                                    stringResource(
                                        id = R.string.label_role_value,
                                        stringResource(id = R.string.life_circle_role_work),
                                        it
                                    ),
                                    fontSize = 12.sp
                                )
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
                        val shan = ShanTextResolver.shanName(context, RhumbLineUtils.getShanIndex(conn.bearing))
                        context.getString(
                            R.string.life_circle_connection_item,
                            conn.fromPoint.name,
                            conn.bearing,
                            shan,
                            conn.distance / 1000f
                        )
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
                    onConfirm = { config, _ ->
                        closeQuickMenu()
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
                        ui.sectorUseMapCenterOrigin = false
                        ui.lastSectorConfig = config
                        ui.sectorConfigLabel = config.label
                        ui.sectorOverlayVisible = true
                        ui.sectorRenderTick += 1
                        // 扇形搜索进行中及结束后保持罗盘锁定，锚定到搜索原点。
                        lockCompassToLatLng(originLatLng.latitude, originLatLng.longitude)

                        if (usingMapCenter) {
                            showStatus(msgSectorFromMapCenter)
                        }

                        if (config.keyword.isBlank()) {
                            ui.sectorLoading = false
                            ui.sectorResults.clear()
                            ui.sectorNoticeCount = null
                            showPoiMarkers(emptyList())
                            showStatus(msgSectorNoKeywordDrawOnly)
                        } else {
                            ui.sectorLoading = true
                            viewModel.runSectorSearch(
                                providers = buildPoiProviderChain(config.keyword),
                                origin = ui.sectorOrigin!!,
                                config = config,
                                onResult = { results ->
                                    showPoiMarkers(results)
                                    focusOnSectorResults(results, ui.sectorOrigin, ui.lastSectorConfig)
                                    if (results.isNotEmpty()) {
                                        showStatus(msgSectorReadyOpenDetails)
                                    }
                                    if (ui.sectorRadiusLimited) {
                                        showStatus(msgSectorRadiusLimited)
                                    }
                                },
                                onError = {
                                    trialMessage = context.getString(
                                        R.string.error_with_reason,
                                        msgSectorSearchFailed,
                                        it.message ?: msgUnknown
                                    )
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

            if (
                ui.sectorOverlayVisible &&
                !ui.showSectorResultDialog &&
                pendingSectorLocatePoi == null
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 92.dp, start = 12.dp, end = 12.dp)
                        .zIndex(5f)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xEEFFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (ui.sectorLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = stringResource(id = R.string.sector_search_loading),
                                fontSize = 12.sp
                            )
                        } else {
                            Button(
                                onClick = { ui.showSectorResultDialog = true },
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Text(actionSectorDetail, fontSize = 12.sp)
                            }
                            TextButton(onClick = { clearSectorArtifacts() }) {
                                Text(actionCloseSectorSearch, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (ui.showSectorResultDialog) {
                AlertDialog(
                    onDismissRequest = {
                        ui.showSectorResultDialog = false
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
                                    text = msgSectorSuggestion,
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
                                        if (showSectorUnsavedOnly) msgShowAllSectorResults else msgShowUnsavedSectorResults,
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
                                                    showStatus(msgSavedToCurrentCase)
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
                            ui.showSectorResultDialog = false
                        }) { Text(actionBackToMap) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            clearSectorArtifacts()
                        }) { Text(actionCloseSectorSearch) }
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
                                Text(actionSectorDetail)
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
                            Text(stringResource(id = R.string.poi_detail_latitude, poi.lat))
                            Text(stringResource(id = R.string.poi_detail_longitude, poi.lng))
                            Text(stringResource(id = R.string.poi_detail_provider, poi.provider))
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
                                    var project: com.fengshui.app.data.Project?
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
                                    closeQuickMenu()

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
                                            showStatus(msgSwitchOriginLocked)
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
                                            showStatus(msgSwitchOriginLocked)
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


