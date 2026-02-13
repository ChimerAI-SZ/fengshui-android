package com.fengshui.app.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.LifeCircleData
import com.fengshui.app.data.LifeCirclePointType
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.poi.MapPoiProvider
import com.fengshui.app.map.poi.PoiResult
import com.fengshui.app.map.sector.SectorUtils
import com.fengshui.app.map.ui.SectorConfig
import kotlinx.coroutines.launch
import android.os.SystemClock

class MapUiStateViewModel : ViewModel() {
    val ui = MapUiStateHolder()

    fun applyCameraMove(source: CameraMoveSource): Boolean {
        val now = SystemClock.elapsedRealtime()
        val hasHigherPriority = source.priority >= ui.cameraMoveSource.priority
        val isExpired = now - ui.cameraMoveTimestamp > ui.cameraMoveTimeoutMs

        return if (hasHigherPriority || isExpired) {
            ui.cameraMoveSource = source
            ui.cameraMoveTimestamp = now
            ui.lastProgrammaticMoveTimestamp = now
            true
        } else {
            false
        }
    }

    fun markUserManualCamera() {
        ui.cameraMoveSource = CameraMoveSource.USER_MANUAL
        ui.cameraMoveTimestamp = SystemClock.elapsedRealtime()
    }

    fun openCrosshair(title: String, subtitle: String, location: UniversalLatLng?) {
        ui.crosshairMode = true
        ui.crosshairTitle = title
        ui.crosshairSubtitle = subtitle
        ui.crosshairLocation = location
    }

    fun closeCrosshair() {
        ui.crosshairMode = false
    }

    fun updateCrosshairLocation(location: UniversalLatLng) {
        ui.crosshairLocation = location
    }

    fun prepareLifeCircleSelection(origins: List<FengShuiPoint>): Map<String, LifeCirclePointType> {
        val key = origins.map { it.id }.toSet()
        val cached = ui.roleAssignmentCache[key]
        val assignments = cached ?: LifeCircleUtils.recommendRoles(origins)
        ui.pendingLifeCircleOrigins = origins
        ui.pendingRoleAssignments = assignments
        return assignments
    }

    fun cacheLifeCircleAssignments(origins: List<FengShuiPoint>, assignments: Map<String, LifeCirclePointType>) {
        val key = origins.map { it.id }.toSet()
        ui.roleAssignmentCache[key] = assignments
    }

    fun activateLifeCircleMode(
        selectedOrigins: List<FengShuiPoint>,
        assignments: Map<String, LifeCirclePointType>,
        projectId: String
    ): Boolean {
        val distinctTypes = assignments.values.toSet()
        if (distinctTypes.size < 3) {
            return false
        }

        val byType = assignments.entries.associate { it.value to it.key }
        val home = selectedOrigins.first { it.id == byType[LifeCirclePointType.HOME] }
        val work = selectedOrigins.first { it.id == byType[LifeCirclePointType.WORK] }
        val entertainment = selectedOrigins.first { it.id == byType[LifeCirclePointType.ENTERTAINMENT] }

        val data = LifeCircleData(
            projectId = projectId,
            homePoint = home,
            workPoint = work,
            entertainmentPoint = entertainment
        )
        ui.lifeCircleData = data
        ui.lifeCircleConnections.clear()
        ui.lifeCircleConnections.addAll(LifeCircleUtils.buildConnections(data))
        ui.lifeCircleMode = true
        return true
    }

    fun exitLifeCircleMode() {
        ui.lifeCircleMode = false
        ui.lifeCircleData = null
        ui.lifeCircleConnections.clear()
    }

    fun buildLifeCircleLabels(targetId: String): List<String> {
        return ui.lifeCircleConnections
            .filter { it.toPoint.id == targetId }
            .map { conn ->
                "→${conn.fromPoint.name}→ | ${"%.1f".format(conn.bearing)}° | ${conn.shanName} | ${"%.1f".format(conn.distance / 1000f)}km"
            }
    }

    fun runSectorSearch(
        providers: List<MapPoiProvider>,
        origin: UniversalLatLng,
        config: SectorConfig,
        onResult: (List<PoiResult>) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        ui.sectorOrigin = origin
        ui.sectorConfigLabel = config.label
        ui.sectorLoading = true
        ui.sectorRadiusLimited = false
        ui.sectorEffectiveRadiusMeters = config.maxDistanceMeters.toInt()

        viewModelScope.launch {
            try {
                val searchRadius = config.maxDistanceMeters.toInt().coerceAtMost(250_000)
                ui.sectorEffectiveRadiusMeters = searchRadius
                if (config.maxDistanceMeters > 250_000f) {
                    ui.sectorRadiusLimited = true
                }

                var raw: List<PoiResult> = emptyList()
                for (provider in providers) {
                    raw = provider.searchByKeyword(
                        keyword = config.keyword,
                        location = origin,
                        radiusMeters = searchRadius
                    )
                    if (raw.isNotEmpty()) break
                }
                val filtered = SectorUtils.filterPOIsInSector(
                    origin = origin,
                    pois = raw,
                    startAngle = config.startAngle,
                    endAngle = config.endAngle,
                    maxDistanceMeters = searchRadius.toFloat()
                )
                val maxPoiCount = 50
                val trimmed = filtered.take(maxPoiCount)

                ui.sectorResults.clear()
                ui.sectorResults.addAll(trimmed)
                ui.sectorNoticeCount = if (filtered.size > maxPoiCount) maxPoiCount else null
                ui.sectorLoading = false
                ui.showSectorResultDialog = true
                onResult(trimmed)
            } catch (t: Throwable) {
                ui.sectorLoading = false
                onError(t)
            }
        }
    }
}
