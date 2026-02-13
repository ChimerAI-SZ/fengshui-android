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
import com.fengshui.app.map.poi.NominatimPoiProvider
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.PointType
import com.fengshui.app.utils.ApiKeyConfig
import com.fengshui.app.map.ui.RegistrationDialog
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R
import java.util.Locale

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
    var savePointType by remember { mutableStateOf(PointType.DESTINATION) }
    var createNewCaseName by remember { mutableStateOf("") }
    var showTrialDialog by remember { mutableStateOf(false) }
    var showRegistrationDialog by remember { mutableStateOf(false) }
    var trialMessage by remember { mutableStateOf("") }
    val trialLimitMessage = stringResource(id = R.string.trial_limit_reached)
    val registerSuccessMessage = stringResource(id = R.string.register_success)
    val registerInvalidMessage = stringResource(id = R.string.register_invalid)

    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { PointRepository(context) }
    
    val providerGoogle: MapPoiProvider? = remember {
        val googleKey = ApiKeyConfig.getGoogleMapsApiKey(context)
        if (ApiKeyConfig.isValidKey(googleKey)) GooglePlacesProvider(googleKey!!) else null
    }
    val providerAmap: MapPoiProvider? = remember {
        val amapKey = ApiKeyConfig.getAmapApiKey(context)
        if (ApiKeyConfig.isValidKey(amapKey)) AmapPoiProvider(amapKey!!) else null
    }
    val providerFallback: MapPoiProvider = remember { NominatimPoiProvider() }
    val providerMock: MapPoiProvider = remember { MockPoiProvider() }
    val isChinaLocale = remember { Locale.getDefault().country.equals("CN", ignoreCase = true) }
    var providerName by remember {
        mutableStateOf(
            if (isChinaLocale && providerAmap != null) {
                context.getString(R.string.provider_amap)
            } else if (providerGoogle != null) {
                context.getString(R.string.provider_google_places)
            } else {
                context.getString(R.string.provider_openstreetmap)
            }
        )
    }
    val scope = rememberCoroutineScope()

    suspend fun runSearch(query: String): List<PoiResult> {
        val hasChineseChars = query.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
        val providers = buildList<Pair<String, MapPoiProvider>> {
            if ((isChinaLocale || hasChineseChars) && providerAmap != null) {
                add(context.getString(R.string.provider_amap) to providerAmap)
            }
            if (providerGoogle != null) {
                add(context.getString(R.string.provider_google_places) to providerGoogle)
            }
            if (providerAmap != null && !(isChinaLocale || hasChineseChars)) {
                add(context.getString(R.string.provider_amap) to providerAmap)
            }
            add(context.getString(R.string.provider_openstreetmap) to providerFallback)
            add(context.getString(R.string.provider_mock) to providerMock)
        }

        providers.forEach { (name, provider) ->
            val list = provider.searchByKeyword(query.trim())
            if (list.isNotEmpty()) {
                providerName = name
                return list
            }
        }
        return emptyList()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(350)
        results = runSearch(searchQuery)
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
                stringResource(id = R.string.search_title),
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
                    label = { Text(stringResource(id = R.string.search_input_label)) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.action_search)
                        )
                    }
                )
                Button(onClick = {
                    if (searchQuery.isNotBlank()) {
                        loading = true
                        scope.launch {
                            results = runSearch(searchQuery)
                            loading = false
                        }
                    }
                }) {
                    Text(stringResource(id = R.string.action_search))
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
                    stringResource(id = R.string.search_tips_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    stringResource(id = R.string.search_tip_provider, providerName),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    stringResource(id = R.string.search_tip_auto),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // 结果列表
            if (loading) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text(
                        stringResource(id = R.string.search_loading),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else if (results.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(id = R.string.search_empty), fontSize = 14.sp, color = Color.Gray)
                    Text(
                        stringResource(id = R.string.search_provider_label, providerName),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                                Text(
                                    stringResource(
                                        id = R.string.search_result_coordinates,
                                        poi.lat,
                                        poi.lng
                                    ),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Button(onClick = { onNavigateToMap(poi) }) {
                                    Text(stringResource(id = R.string.action_locate_to_map))
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                                Button(onClick = {
                                    selectedPoi = poi
                                    savePointType = PointType.DESTINATION
                                    createNewCaseName = ""
                                    showCaseSelectDialog = true
                                }) {
                                    Text(stringResource(id = R.string.action_add_to_case))
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
                    title = { Text(stringResource(id = R.string.select_case_title)) },
                    text = {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { savePointType = PointType.ORIGIN }) {
                                    Text(
                                        if (savePointType == PointType.ORIGIN) {
                                            stringResource(id = R.string.point_type_origin_checked)
                                        } else {
                                            stringResource(id = R.string.point_type_origin)
                                        }
                                    )
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                                Button(onClick = { savePointType = PointType.DESTINATION }) {
                                    Text(
                                        if (savePointType == PointType.DESTINATION) {
                                            stringResource(id = R.string.point_type_destination_checked)
                                        } else {
                                            stringResource(id = R.string.point_type_destination)
                                        }
                                    )
                                }
                            }

                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                            TextField(
                                value = createNewCaseName,
                                onValueChange = { createNewCaseName = it },
                                label = { Text(stringResource(id = R.string.label_new_case_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                            Button(
                                onClick = {
                                    if (createNewCaseName.isBlank()) return@Button
                                    scope.launch {
                                        try {
                                            val newProject = repo.createProject(createNewCaseName.trim())
                                            repo.createPoint(
                                                selectedPoi!!.name,
                                                selectedPoi!!.lat,
                                                selectedPoi!!.lng,
                                                savePointType,
                                                groupId = newProject.id,
                                                groupName = newProject.name,
                                                address = selectedPoi!!.address
                                            )
                                            showCaseSelectDialog = false
                                            selectedPoi = null
                                        } catch (e: com.fengshui.app.TrialLimitException) {
                                            trialMessage = e.message ?: trialLimitMessage
                                            showCaseSelectDialog = false
                                            selectedPoi = null
                                            showTrialDialog = true
                                        }
                                    }
                                },
                                enabled = createNewCaseName.isNotBlank()
                            ) {
                                Text(stringResource(id = R.string.action_create_case_and_save))
                            }

                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))
                            if (projects.isEmpty()) {
                                Text(stringResource(id = R.string.case_select_empty))
                            } else {
                                projects.forEach { p ->
                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)) {
                                        Text(p.name, modifier = Modifier.weight(1f))
                                        Button(onClick = {
                                            // 保存为终点（默认）
                                            scope.launch {
                                                try {
                                                    repo.createPoint(
                                                        selectedPoi!!.name,
                                                        selectedPoi!!.lat,
                                                        selectedPoi!!.lng,
                                                        savePointType,
                                                        groupId = p.id,
                                                        groupName = p.name,
                                                        address = selectedPoi!!.address
                                                    )
                                                    showCaseSelectDialog = false
                                                    selectedPoi = null
                                                } catch (e: com.fengshui.app.TrialLimitException) {
                                                    trialMessage = e.message ?: trialLimitMessage
                                                    showCaseSelectDialog = false
                                                    selectedPoi = null
                                                    showTrialDialog = true
                                                }
                                            }
                                        }) {
                                            Text(stringResource(id = R.string.save_to_case, p.name))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCaseSelectDialog = false; selectedPoi = null }) {
                            Text(stringResource(id = R.string.action_cancel))
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
                        trialMessage = if (ok) {
                            registerSuccessMessage
                        } else {
                            registerInvalidMessage
                        }
                        showRegistrationDialog = false
                        showTrialDialog = true
                    }
                }
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
                stringResource(id = R.string.info_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 版本信息
            InfoSection(
                title = stringResource(id = R.string.info_section_version_title),
                content = stringResource(id = R.string.info_section_version_content)
            )

            // 功能说明
            InfoSection(
                title = stringResource(id = R.string.info_section_features_title),
                content = stringResource(id = R.string.info_section_features_content)
            )

            // 使用技巧
            InfoSection(
                title = stringResource(id = R.string.info_section_tips_title),
                content = stringResource(id = R.string.info_section_tips_content)
            )

            // 技术信息
            InfoSection(
                title = stringResource(id = R.string.info_section_tech_title),
                content = stringResource(id = R.string.info_section_tech_content)
            )

            Text(
                stringResource(id = R.string.info_footer),
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
