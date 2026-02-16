package com.fengshui.app.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.LifeCircleData
import com.fengshui.app.data.LifeCirclePointType
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.map.poi.MapPoiProvider
import com.fengshui.app.map.poi.PoiResult
import com.fengshui.app.map.poi.AmapPoiProvider
import com.fengshui.app.map.poi.GooglePlacesProvider
import com.fengshui.app.map.poi.NominatimPoiProvider
import com.fengshui.app.map.poi.PoiTypeMapper
import com.fengshui.app.map.sector.SectorUtils
import com.fengshui.app.map.ui.SectorConfig
import com.fengshui.app.utils.RhumbLineUtils
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
        ui.sectorFallbackUsed = false
        ui.sectorDebugAmapRaw = 0
        ui.sectorDebugGoogleRaw = 0
        ui.sectorDebugTypeFiltered = 0
        ui.sectorDebugSectorFiltered = 0
        ui.sectorDebugAmapStatus = "-"
        ui.sectorDebugGoogleStatus = "-"
        ui.sectorRadiusLimited = false
        ui.sectorEffectiveRadiusMeters = config.maxDistanceMeters.toInt()

        viewModelScope.launch {
            try {
                val searchRadius = config.maxDistanceMeters.toInt().coerceAtMost(250_000)
                ui.sectorEffectiveRadiusMeters = searchRadius
                if (config.maxDistanceMeters > 250_000f) {
                    ui.sectorRadiusLimited = true
                }

                val allRaw = mutableListOf<PoiResult>()
                val isTypedKeyword = PoiTypeMapper.isTypedCategoryKeyword(config.keyword)
                for (provider in providers) {
                    val raw = provider.searchByKeyword(
                        keyword = config.keyword,
                        location = origin,
                        radiusMeters = searchRadius
                    )
                    val stats = provider.lastSearchStats()
                    when (provider) {
                        is AmapPoiProvider -> {
                            ui.sectorDebugAmapRaw += stats?.rawCount ?: raw.size
                            ui.sectorDebugAmapStatus = stats?.debugStatus ?: "-"
                        }
                        is GooglePlacesProvider -> {
                            ui.sectorDebugGoogleRaw += stats?.rawCount ?: raw.size
                            ui.sectorDebugGoogleStatus = stats?.debugStatus ?: "-"
                        }
                    }
                    if (stats != null) {
                        ui.sectorDebugTypeFiltered += stats.typeFilteredCount
                    } else {
                        ui.sectorDebugTypeFiltered += raw.size
                    }
                    if (raw.isNotEmpty()) {
                        allRaw.addAll(raw)
                    }
                }
                // Typed search fallback when map providers are unavailable/denied:
                // use Nominatim with generalized category words to avoid hard zero.
                if (allRaw.isEmpty() && isTypedKeyword) {
                    val nominatim = NominatimPoiProvider()
                    val fallbackQueries = buildList {
                        add(config.keyword)
                        when {
                            config.keyword.contains("住宅") || config.keyword.contains("小区") || config.keyword.contains("residence", ignoreCase = true) -> {
                                add("residential")
                                add("apartment")
                                add("housing")
                            }
                            config.keyword.contains("医院") || config.keyword.contains("hospital", ignoreCase = true) -> {
                                add("hospital")
                                add("clinic")
                            }
                            config.keyword.contains("大厦") || config.keyword.contains("写字楼") || config.keyword.contains("office", ignoreCase = true) || config.keyword.contains("building", ignoreCase = true) -> {
                                add("office")
                                add("building")
                                add("tower")
                            }
                        }
                    }.distinct()
                    for (q in fallbackQueries) {
                        val raw = nominatim.searchByKeyword(
                            keyword = q,
                            location = origin,
                            radiusMeters = searchRadius
                        )
                        if (raw.isNotEmpty()) {
                            allRaw.addAll(raw)
                            break
                        }
                    }
                }
                // If radius-bounded lookup misses, retry once without radius limits.
                if (allRaw.isEmpty()) {
                    for (provider in providers) {
                        val raw = provider.searchByKeyword(
                            keyword = config.keyword,
                            location = origin,
                            radiusMeters = 0
                        )
                        val stats = provider.lastSearchStats()
                        when (provider) {
                            is AmapPoiProvider -> {
                                ui.sectorDebugAmapRaw += stats?.rawCount ?: raw.size
                            }
                            is GooglePlacesProvider -> {
                                ui.sectorDebugGoogleRaw += stats?.rawCount ?: raw.size
                            }
                        }
                        if (stats != null) {
                            ui.sectorDebugTypeFiltered += stats.typeFilteredCount
                        } else {
                            ui.sectorDebugTypeFiltered += raw.size
                        }
                        if (raw.isNotEmpty()) {
                            allRaw.addAll(raw)
                        }
                    }
                }
                val raw = allRaw
                    .distinctBy {
                        val lat = String.format("%.5f", it.lat)
                        val lng = String.format("%.5f", it.lng)
                        "${it.provider}|${it.name}|$lat|$lng"
                    }

                var filtered = SectorUtils.filterPOIsInSector(
                    origin = origin,
                    pois = raw,
                    startAngle = config.startAngle,
                    endAngle = config.endAngle,
                    maxDistanceMeters = searchRadius.toFloat()
                )
                // Robust fallback: tolerate coordinate-system mismatch and narrow-sector misses.
                if (filtered.isEmpty() && raw.isNotEmpty()) {
                    filtered = SectorUtils.filterPOIsInSector(
                        origin = origin,
                        pois = raw,
                        startAngle = config.startAngle,
                        endAngle = config.endAngle,
                        maxDistanceMeters = searchRadius.toFloat(),
                        bearingOffsetDegrees = 180f
                    )
                }
                if (filtered.isEmpty() && raw.isNotEmpty()) {
                    filtered = SectorUtils.filterPOIsInSector(
                        origin = origin,
                        pois = raw,
                        startAngle = config.startAngle,
                        endAngle = config.endAngle,
                        maxDistanceMeters = searchRadius.toFloat(),
                        angleToleranceDegrees = 12f
                    )
                }
                if (filtered.isEmpty() && raw.isNotEmpty()) {
                    filtered = SectorUtils.filterPOIsInSector(
                        origin = origin,
                        pois = raw,
                        startAngle = config.startAngle,
                        endAngle = config.endAngle,
                        maxDistanceMeters = searchRadius.toFloat(),
                        bearingOffsetDegrees = 180f,
                        angleToleranceDegrees = 12f
                    )
                }
                if (filtered.isEmpty() && raw.isNotEmpty()) {
                    // Last-resort fallback: show nearest usable results so user can still operate.
                    filtered = raw.sortedBy {
                        RhumbLineUtils.calculateRhumbDistance(
                            origin,
                            UniversalLatLng(it.lat, it.lng)
                        )
                    }.take(50)
                    ui.sectorFallbackUsed = true
                }
                val maxPoiCount = 50
                val trimmed = filtered.take(maxPoiCount)
                ui.sectorDebugSectorFiltered = filtered.size

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
