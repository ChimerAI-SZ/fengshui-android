package com.fengshui.app.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.LifeCircleConnection
import com.fengshui.app.data.LifeCircleData
import com.fengshui.app.data.LifeCirclePointType
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.poi.PoiResult

class MapUiStateHolder {
    var crosshairMode by mutableStateOf(false)
    var crosshairTitle by mutableStateOf("")
    var crosshairSubtitle by mutableStateOf("")
    var crosshairLocation by mutableStateOf<UniversalLatLng?>(null)
    var isLifeCircleSelection by mutableStateOf(false)
    var tempViewMode by mutableStateOf(false)

    var cameraMoveSource by mutableStateOf(CameraMoveSource.MAP_INIT)
    var cameraMoveTimestamp by mutableStateOf(0L)
    var lastProgrammaticMoveTimestamp by mutableStateOf(0L)
    val cameraMoveTimeoutMs = 3000L

    var lifeCircleMode by mutableStateOf(false)
    var showLifeCircleSelectDialog by mutableStateOf(false)
    var showRoleAssignmentDialog by mutableStateOf(false)
    var showLifeCircleInfoDialog by mutableStateOf(false)
    var lifeCircleData by mutableStateOf<LifeCircleData?>(null)
    val lifeCircleConnections = mutableStateListOf<LifeCircleConnection>()
    val roleAssignmentCache = mutableStateMapOf<Set<String>, Map<String, LifeCirclePointType>>()
    var pendingLifeCircleOrigins by mutableStateOf(listOf<FengShuiPoint>())
    var pendingRoleAssignments by mutableStateOf(mapOf<String, LifeCirclePointType>())

    var showSectorConfigDialog by mutableStateOf(false)
    var showSectorResultDialog by mutableStateOf(false)
    var sectorLoading by mutableStateOf(false)
    val sectorResults = mutableStateListOf<PoiResult>()
    var sectorOrigin by mutableStateOf<UniversalLatLng?>(null)
    var sectorConfigLabel by mutableStateOf("")
    var sectorNoticeCount by mutableStateOf<Int?>(null)
}
