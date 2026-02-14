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
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.abstraction.amap.AMapProvider
import com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
import com.fengshui.app.screens.CaseListScreen
import com.fengshui.app.screens.SearchScreen
import com.fengshui.app.screens.InfoScreen
import com.fengshui.app.R
import com.fengshui.app.utils.ApiKeyConfig
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

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
    var currentTab by remember { mutableStateOf(NavigationItem.MAP) }
    var quickAddCaseId by remember { mutableStateOf<String?>(null) }
    var searchFocus by remember { mutableStateOf<UniversalLatLng?>(null) }
    var showUnsupportedMapDialog by remember { mutableStateOf(false) }
    var showLanguageOnlyConfirmDialog by remember { mutableStateOf(false) }
    var pendingIsChinese by remember { mutableStateOf(false) }
    var pendingLanguageTag by remember { mutableStateOf("en") }
    var pendingTargetProvider by remember { mutableStateOf(MapProviderType.AMAP) }
    var pendingUnsupportedReason by remember { mutableStateOf("") }
    val context = LocalContext.current
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
    var isChinese by remember {
        mutableStateOf(
            if (initialLocales.isEmpty) {
                Locale.getDefault().language.startsWith("zh")
            } else {
                initialLocales[0]?.language?.startsWith("zh") == true
            }
        )
    }

    fun applyLanguageOnly() {
        isChinese = pendingIsChinese
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(pendingLanguageTag)
        )
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
        pendingIsChinese = nextChinese
        pendingLanguageTag = languageTag
        pendingTargetProvider = if (nextChinese) MapProviderType.AMAP else MapProviderType.GOOGLE

        if (mapProviderType == pendingTargetProvider) {
            applyLanguageOnly()
            return
        }

        if (isProviderAvailable(pendingTargetProvider)) {
            mapProviderTypeName = pendingTargetProvider.name
            applyLanguageOnly()
        } else {
            pendingUnsupportedReason = providerUnsupportedReason(pendingTargetProvider)
            showUnsupportedMapDialog = true
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
                        onMapProviderSwitch = { targetType ->
                            val targetAvailable = when (targetType) {
                                MapProviderType.GOOGLE -> hasGoogleMapKey
                                MapProviderType.AMAP -> hasAmapKey
                            }
                            if (targetAvailable && mapProviderType != targetType) {
                                mapProviderTypeName = targetType.name
                            }
                        },
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

    if (showUnsupportedMapDialog) {
        val providerLabel = if (pendingTargetProvider == MapProviderType.AMAP) "高德地图" else "Google 地图"
        AlertDialog(
            onDismissRequest = { showUnsupportedMapDialog = false },
            title = { Text("地图切换确认") },
            text = {
                Text(
                    "当前环境可能不支持 $providerLabel（$pendingUnsupportedReason），继续切换可能闪退。\n\n" +
                        "你可以选择：\n" +
                        "1) 仍然切换地图并执行语言切换\n" +
                        "2) 只切换语言，不切换地图"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mapProviderTypeName = pendingTargetProvider.name
                        applyLanguageOnly()
                        showUnsupportedMapDialog = false
                    }
                ) {
                    Text("确认切换地图")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnsupportedMapDialog = false
                        showLanguageOnlyConfirmDialog = true
                    }
                ) {
                    Text("仅切换语言")
                }
            }
        )
    }

    if (showLanguageOnlyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageOnlyConfirmDialog = false },
            title = { Text("执行确认") },
            text = { Text("将仅切换语言，不切换地图。是否继续？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        applyLanguageOnly()
                        showLanguageOnlyConfirmDialog = false
                    }
                ) {
                    Text("继续")
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
