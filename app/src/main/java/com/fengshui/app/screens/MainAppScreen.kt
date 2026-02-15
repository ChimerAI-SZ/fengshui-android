package com.fengshui.app.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.fengshui.app.navigation.NavigationItem
import com.fengshui.app.map.MapScreen
import com.fengshui.app.map.abstraction.MapProviderType
import com.fengshui.app.map.abstraction.CameraPosition
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.abstraction.amap.AMapProvider
import com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
import com.fengshui.app.screens.CaseListScreen
import com.fengshui.app.screens.SearchScreen
import com.fengshui.app.screens.InfoScreen
import com.fengshui.app.R
import com.fengshui.app.utils.ApiKeyConfig
import com.fengshui.app.utils.Prefs
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(modifier: Modifier = Modifier) {
    val PREF_PENDING_MAP_SWITCH = "pending_map_switch_after_locale"
    val PREF_PENDING_MAP_CAMERA = "pending_map_camera_after_locale"
    var currentTab by remember { mutableStateOf(NavigationItem.MAP) }
    var quickAddCaseId by remember { mutableStateOf<String?>(null) }
    var searchFocus by remember { mutableStateOf<UniversalLatLng?>(null) }
    var showLanguageMapConfirmDialog by remember { mutableStateOf(false) }
    var showMapSwitchConfirmDialog by remember { mutableStateOf(false) }
    var showLanguageOnlyConfirmDialog by remember { mutableStateOf(false) }
    var pendingIsChinese by remember { mutableStateOf(false) }
    var pendingLanguageTag by remember { mutableStateOf("en") }
    var pendingTargetProvider by remember { mutableStateOf(MapProviderType.AMAP) }
    var pendingCameraToRestore by remember { mutableStateOf<CameraPosition?>(null) }
    var pendingUnsupportedReason by remember { mutableStateOf("") }
    var switchingBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleKey = ApiKeyConfig.getGoogleMapsApiKey(context)
    val amapKey = ApiKeyConfig.getAmapApiKey(context)
    val hasGoogleMapKey = ApiKeyConfig.isValidKey(googleKey)
    val hasAmapKey = ApiKeyConfig.isValidKey(amapKey)
    // Always default to AMap. Keep provider across locale/activity recreation.
    var mapProviderTypeName by rememberSaveable { mutableStateOf(MapProviderType.AMAP.name) }
    val mapProviderType = runCatching { MapProviderType.valueOf(mapProviderTypeName) }
        .getOrDefault(MapProviderType.AMAP)
    val googleMapProvider = remember { GoogleMapProvider(context) }
    val amapProvider = remember { AMapProvider(context) }
    val mapProvider = when (mapProviderType) {
        MapProviderType.AMAP -> amapProvider
        MapProviderType.GOOGLE -> googleMapProvider
    }
    val initialLocales = AppCompatDelegate.getApplicationLocales()
    var isChinese by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Default language on first app open only: Chinese.
        // If user has explicitly switched language before, keep their choice.
        if (initialLocales.isEmpty) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("zh-CN")
            )
            isChinese = true
        } else {
            isChinese = initialLocales[0]?.language?.startsWith("zh") == true
        }

        val pending = Prefs.getString(context, PREF_PENDING_MAP_SWITCH).orEmpty()
        val pendingCamera = Prefs.getString(context, PREF_PENDING_MAP_CAMERA).orEmpty()
        if (pendingCamera.isNotBlank()) {
            pendingCameraToRestore = pendingCamera
                .split(",")
                .takeIf { it.size == 4 }
                ?.let { parts ->
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    val zoom = parts[2].toFloatOrNull()
                    val bearing = parts[3].toFloatOrNull()
                    if (lat != null && lng != null && zoom != null && bearing != null) {
                        CameraPosition(
                            target = UniversalLatLng(lat, lng),
                            zoom = zoom,
                            bearing = bearing
                        )
                    } else {
                        null
                    }
                }
            Prefs.saveString(context, PREF_PENDING_MAP_CAMERA, "")
        }
        if (pending.isNotBlank()) {
            runCatching { MapProviderType.valueOf(pending) }
                .getOrNull()
                ?.let { target ->
                    mapProviderTypeName = target.name
                }
            Prefs.saveString(context, PREF_PENDING_MAP_SWITCH, "")
        }
    }

    fun applyLanguageOnly() {
        if (switchingBusy) return
        switchingBusy = true
        scope.launch {
            runCatching {
                delay(120)
                isChinese = pendingIsChinese
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(pendingLanguageTag)
                )
            }
            switchingBusy = false
        }
    }

    fun isProviderAvailable(providerType: MapProviderType): Boolean {
        return when (providerType) {
            MapProviderType.GOOGLE -> {
                val googlePlayOk = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
                hasGoogleMapKey && googlePlayOk
            }
            MapProviderType.AMAP -> hasAmapKey
        }
    }

    fun providerUnsupportedReason(providerType: MapProviderType): String {
        return when (providerType) {
            MapProviderType.GOOGLE -> {
                val googlePlayOk = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
                when {
                    !hasGoogleMapKey -> "未配置 Google Maps API Key"
                    !googlePlayOk -> "当前设备缺少 Google Play Services"
                    else -> "当前环境可能不支持 Google 地图"
                }
            }
            MapProviderType.AMAP -> {
                if (!hasAmapKey) "未配置高德 API Key" else "当前环境可能不支持高德地图"
            }
        }
    }

    fun requestLanguageSwitch(nextChinese: Boolean, languageTag: String) {
        if (switchingBusy) return
        val currentLanguageTag = if (isChinese) "zh-CN" else "en"
        if (currentLanguageTag.equals(languageTag, ignoreCase = true)) {
            return
        }
        pendingCameraToRestore = mapProvider.getCameraPosition() ?: pendingCameraToRestore
        pendingIsChinese = nextChinese
        pendingLanguageTag = languageTag
        pendingTargetProvider = if (nextChinese) MapProviderType.AMAP else MapProviderType.GOOGLE
        pendingUnsupportedReason = when {
            mapProviderType == pendingTargetProvider -> "当前地图已是目标地图"
            isProviderAvailable(pendingTargetProvider) -> ""
            else -> providerUnsupportedReason(pendingTargetProvider)
        }
        showLanguageMapConfirmDialog = true
    }

    fun requestMapSwitchOnly(targetProvider: MapProviderType, cameraSnapshot: CameraPosition? = null) {
        if (switchingBusy || mapProviderType == targetProvider) return
        pendingTargetProvider = targetProvider
        pendingCameraToRestore = cameraSnapshot ?: mapProvider.getCameraPosition()
        pendingUnsupportedReason = if (isProviderAvailable(targetProvider)) "" else providerUnsupportedReason(targetProvider)
        showMapSwitchConfirmDialog = true
    }

    fun applyMapSwitchSafely(targetProvider: MapProviderType) {
        if (switchingBusy) return
        switchingBusy = true
        val availableNow = isProviderAvailable(targetProvider)
        if (!availableNow) {
            pendingUnsupportedReason = providerUnsupportedReason(targetProvider)
            switchingBusy = false
            showMapSwitchConfirmDialog = true
            return
        }
        scope.launch {
            runCatching {
                delay(120)
                if (pendingCameraToRestore == null) {
                    pendingCameraToRestore = mapProvider.getCameraPosition()
                }
                mapProviderTypeName = targetProvider.name
            }.onFailure {
                // Fallback to AMap to reduce crash risk on provider initialization errors.
                mapProviderTypeName = MapProviderType.AMAP.name
            }
            switchingBusy = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    LanguageTogglePill(
                        isChinese = isChinese,
                        onSelectChinese = {
                            requestLanguageSwitch(nextChinese = true, languageTag = "zh-CN")
                        },
                        onSelectEnglish = {
                            requestLanguageSwitch(nextChinese = false, languageTag = "en")
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = currentTab == item,
                        onClick = { currentTab = item },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(id = item.labelRes)
                            )
                        },
                        label = { Text(stringResource(id = item.labelRes)) }
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
                        mapProviderType = mapProviderType,
                        hasGoogleMap = hasGoogleMapKey,
                        hasAmapMap = hasAmapKey,
                        onMapProviderSwitch = { targetType, cameraSnapshot ->
                            requestMapSwitchOnly(targetType, cameraSnapshot)
                        },
                        restoreCameraPosition = pendingCameraToRestore,
                        onRestoreCameraConsumed = { pendingCameraToRestore = null },
                        modifier = Modifier.fillMaxSize(),
                        quickAddCaseId = quickAddCaseId,
                        onQuickAddConsumed = { quickAddCaseId = null },
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

    if (showLanguageMapConfirmDialog) {
        val providerLabel = if (pendingTargetProvider == MapProviderType.AMAP) {
            stringResource(id = R.string.provider_amap)
        } else {
            stringResource(id = R.string.provider_google_map_full)
        }
        val mapAlreadyMatched = pendingUnsupportedReason == "当前地图已是目标地图"
        val mapSwitchSupported = pendingUnsupportedReason.isBlank()
        val riskText = if (mapSwitchSupported) {
            stringResource(id = R.string.map_switch_risk_supported, providerLabel)
        } else if (mapAlreadyMatched) {
            stringResource(id = R.string.map_switch_risk_already_matched, providerLabel)
        } else {
            stringResource(
                id = R.string.map_switch_risk_unsupported,
                providerLabel,
                pendingUnsupportedReason
            )
        }
        AlertDialog(
            onDismissRequest = { showLanguageMapConfirmDialog = false },
            title = { Text(stringResource(id = R.string.map_switch_title)) },
            text = {
                Text(
                    "$riskText\n\n${stringResource(id = R.string.map_switch_language_dialog_options)}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (switchingBusy) return@TextButton
                        applyLanguageOnly()
                        showLanguageMapConfirmDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.map_switch_button_lang_only_recommended))
                }
            },
            dismissButton = {
                if (mapSwitchSupported) {
                    TextButton(
                        onClick = {
                            if (switchingBusy) return@TextButton
                            // Defer provider switch until after locale recreation to prevent crash.
                            Prefs.saveString(context, PREF_PENDING_MAP_SWITCH, pendingTargetProvider.name)
                            pendingCameraToRestore?.let { cam ->
                                val payload = "${cam.target.latitude},${cam.target.longitude},${cam.zoom},${cam.bearing}"
                                Prefs.saveString(context, PREF_PENDING_MAP_CAMERA, payload)
                            }
                            applyLanguageOnly()
                            showLanguageMapConfirmDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.map_switch_button_lang_plus_map))
                    }
                } else if (mapAlreadyMatched) {
                    TextButton(
                        onClick = {
                            applyLanguageOnly()
                            showLanguageMapConfirmDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.map_switch_button_lang_only))
                    }
                } else {
                    TextButton(
                        onClick = {
                            showLanguageMapConfirmDialog = false
                        }
                    ) {
                        Text("关闭")
                    }
                }
            }
        )
    }

    if (showMapSwitchConfirmDialog) {
        val providerLabel = if (pendingTargetProvider == MapProviderType.AMAP) {
            stringResource(id = R.string.provider_amap)
        } else {
            stringResource(id = R.string.provider_google_map_full)
        }
        val riskText = if (pendingUnsupportedReason.isBlank()) {
            stringResource(id = R.string.map_switch_risk_go, providerLabel)
        } else {
            stringResource(
                id = R.string.map_switch_risk_go_with_reason,
                providerLabel,
                pendingUnsupportedReason
            )
        }
        AlertDialog(
            onDismissRequest = { showMapSwitchConfirmDialog = false },
            title = { Text(stringResource(id = R.string.map_switch_risk_title)) },
            text = {
                Text("$riskText\n\n${stringResource(id = R.string.map_switch_continue_question)}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (switchingBusy) return@TextButton
                        applyMapSwitchSafely(pendingTargetProvider)
                        showMapSwitchConfirmDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.map_switch_button_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMapSwitchConfirmDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    if (showLanguageOnlyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageOnlyConfirmDialog = false },
            title = { Text(stringResource(id = R.string.map_switch_execute_title)) },
            text = { Text(stringResource(id = R.string.map_switch_language_only_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        applyLanguageOnly()
                        showLanguageOnlyConfirmDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.map_switch_button_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLanguageOnlyConfirmDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun LanguageTogglePill(
    isChinese: Boolean,
    onSelectChinese: () -> Unit,
    onSelectEnglish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zhLabel = stringResource(id = R.string.lang_short_zh)
    val enLabel = stringResource(id = R.string.lang_short_en)

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isChinese) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onSelectChinese)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = zhLabel,
                color = if (isChinese) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Box(
            modifier = Modifier
                .background(
                    color = if (isChinese) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onSelectEnglish)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = enLabel,
                color = if (isChinese) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
