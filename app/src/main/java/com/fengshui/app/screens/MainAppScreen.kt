package com.fengshui.app.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R
import com.fengshui.app.map.MapScreen
import com.fengshui.app.map.MapSessionStore
import com.fengshui.app.map.abstraction.CameraPosition
import com.fengshui.app.map.abstraction.MapProviderType
import com.fengshui.app.map.abstraction.amap.AMapProvider
import com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
import com.fengshui.app.navigation.NavigationItem
import com.fengshui.app.utils.ApiKeyConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(NavigationItem.MAP) }
    var openCaseOpsSignal by remember { mutableStateOf(0) }
    var openAnalysisSignal by remember { mutableStateOf(0) }
    var closeQuickMenuSignal by remember { mutableStateOf(0) }
    var forceRelocateSignal by remember { mutableStateOf(0) }
    var quickAddCaseId by remember { mutableStateOf<String?>(null) }

    var showMapSwitchConfirmDialog by remember { mutableStateOf(false) }
    var pendingTargetProvider by remember { mutableStateOf(MapProviderType.AMAP) }
    var pendingUnsupportedReason by remember { mutableStateOf("") }
    var switchingBusy by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleKey = ApiKeyConfig.getGoogleMapsApiKey(context)
    val amapKey = ApiKeyConfig.getAmapApiKey(context)
    val hasGoogleMapKey = ApiKeyConfig.isValidKey(googleKey)
    val hasAmapKey = ApiKeyConfig.isValidKey(amapKey)

    val initialRestoreEnabled = remember {
        MapSessionStore.isRestoreLastPositionEnabled(context)
    }
    var restoreLastPositionEnabled by rememberSaveable { mutableStateOf(initialRestoreEnabled) }
    var pendingCameraToRestore by remember {
        mutableStateOf(
            if (initialRestoreEnabled) {
                MapSessionStore.loadCameraPosition(context)
            } else {
                null
            }
        )
    }

    val initialProviderType = remember {
        MapSessionStore.loadMapProviderType(context) ?: MapProviderType.AMAP
    }
    var mapProviderTypeName by rememberSaveable { mutableStateOf(initialProviderType.name) }
    val mapProviderType = runCatching { MapProviderType.valueOf(mapProviderTypeName) }
        .getOrDefault(MapProviderType.AMAP)
    val googleMapProvider = remember { GoogleMapProvider(context) }
    val amapProvider = remember { AMapProvider(context) }
    val mapProvider = when (mapProviderType) {
        MapProviderType.AMAP -> amapProvider
        MapProviderType.GOOGLE -> googleMapProvider
    }

    fun isProviderAvailable(providerType: MapProviderType): Boolean {
        return when (providerType) {
            MapProviderType.GOOGLE -> hasGoogleMapKey

            MapProviderType.AMAP -> hasAmapKey
        }
    }

    fun providerUnsupportedReason(providerType: MapProviderType): String {
        return when (providerType) {
            MapProviderType.GOOGLE -> {
                if (!hasGoogleMapKey) {
                    context.getString(R.string.map_switch_reason_missing_google_key)
                } else {
                    context.getString(R.string.map_switch_reason_google_env_unsupported)
                }
            }

            MapProviderType.AMAP -> {
                if (!hasAmapKey) {
                    context.getString(R.string.map_switch_reason_missing_amap_key)
                } else {
                    context.getString(R.string.map_switch_reason_amap_env_unsupported)
                }
            }
        }
    }

    fun requestMapSwitchOnly(targetProvider: MapProviderType, cameraSnapshot: CameraPosition? = null) {
        if (switchingBusy || mapProviderType == targetProvider) return
        pendingTargetProvider = targetProvider
        pendingCameraToRestore = cameraSnapshot
            ?: mapProvider.getCameraPosition()
            ?: MapSessionStore.loadCameraPosition(context)
        val available = isProviderAvailable(targetProvider)
        pendingUnsupportedReason = if (available) "" else providerUnsupportedReason(targetProvider)
        if (available) {
            switchingBusy = true
            scope.launch {
                runCatching {
                    delay(120)
                    if (pendingCameraToRestore == null) {
                        pendingCameraToRestore = mapProvider.getCameraPosition()
                            ?: MapSessionStore.loadCameraPosition(context)
                    }
                    mapProviderTypeName = targetProvider.name
                    pendingUnsupportedReason = ""
                }.onFailure {
                    mapProviderTypeName = MapProviderType.AMAP.name
                }
                switchingBusy = false
            }
            return
        }
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
                        ?: MapSessionStore.loadCameraPosition(context)
                }
                mapProviderTypeName = targetProvider.name
                pendingUnsupportedReason = ""
            }.onFailure {
                mapProviderTypeName = MapProviderType.AMAP.name
            }
            switchingBusy = false
        }
    }

    fun updateRestorePreference(enabled: Boolean) {
        restoreLastPositionEnabled = enabled
        MapSessionStore.setRestoreLastPositionEnabled(context, enabled)
        if (enabled) {
            pendingCameraToRestore = MapSessionStore.loadCameraPosition(context)
        } else {
            pendingCameraToRestore = null
            MapSessionStore.clearCameraPosition(context)
        }
    }

    fun relocateNowFromSettings() {
        MapSessionStore.clearCameraPosition(context)
        pendingCameraToRestore = null
        currentTab = NavigationItem.MAP
        forceRelocateSignal += 1
    }

    LaunchedEffect(mapProviderType) {
        MapSessionStore.saveMapProviderType(context, mapProviderType)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            val bottomItems = listOf(
                NavigationItem.MAP,
                NavigationItem.CASE_MANAGEMENT,
                NavigationItem.CASE_OPS,
                NavigationItem.ANALYSIS
            )
            NavigationBar {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentTab == item,
                        onClick = {
                            currentTab = item
                            when (item) {
                                NavigationItem.MAP -> {
                                    openCaseOpsSignal = 0
                                    openAnalysisSignal = 0
                                    closeQuickMenuSignal += 1
                                }
                                NavigationItem.CASE_MANAGEMENT -> {
                                    openCaseOpsSignal = 0
                                    openAnalysisSignal = 0
                                    closeQuickMenuSignal += 1
                                }
                                NavigationItem.CASE_OPS -> {
                                    openAnalysisSignal = 0
                                    openCaseOpsSignal += 1
                                }
                                NavigationItem.ANALYSIS -> {
                                    openCaseOpsSignal = 0
                                    openAnalysisSignal += 1
                                }
                                else -> Unit
                            }
                        },
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
                NavigationItem.MAP,
                NavigationItem.CASE_OPS,
                NavigationItem.ANALYSIS -> {
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
                        openCaseOpsSignal = openCaseOpsSignal,
                        openAnalysisSignal = openAnalysisSignal,
                        closeQuickMenuSignal = closeQuickMenuSignal,
                        forceRelocateSignal = forceRelocateSignal,
                        quickAddCaseId = quickAddCaseId,
                        onQuickAddConsumed = { quickAddCaseId = null },
                        onOpenSettings = {
                            openCaseOpsSignal = 0
                            openAnalysisSignal = 0
                            closeQuickMenuSignal += 1
                            currentTab = NavigationItem.SETTINGS
                        }
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

                NavigationItem.SETTINGS -> {
                    SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        restoreLastMapPositionEnabled = restoreLastPositionEnabled,
                        onRestoreLastMapPositionEnabledChange = { enabled ->
                            updateRestorePreference(enabled)
                        },
                        onRelocateNow = {
                            relocateNowFromSettings()
                        }
                    )
                }
            }
        }
    }

    if (showMapSwitchConfirmDialog) {
        val unsupportedOnly = pendingUnsupportedReason.isNotBlank()
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
                if (unsupportedOnly) {
                    Text(riskText)
                } else {
                    Text("$riskText\n\n${stringResource(id = R.string.map_switch_continue_question)}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!unsupportedOnly) {
                            if (switchingBusy) return@TextButton
                            applyMapSwitchSafely(pendingTargetProvider)
                        }
                        showMapSwitchConfirmDialog = false
                    }
                ) {
                    Text(
                        if (unsupportedOnly) {
                            stringResource(id = R.string.action_close)
                        } else {
                            stringResource(id = R.string.map_switch_button_confirm)
                        }
                    )
                }
            },
            dismissButton = if (unsupportedOnly) null else {
                {
                    TextButton(onClick = { showMapSwitchConfirmDialog = false }) {
                        Text(stringResource(id = R.string.action_cancel))
                    }
                }
            }
        )
    }
}
