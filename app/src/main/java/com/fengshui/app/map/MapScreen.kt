package com.fengshui.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fengshui.app.map.ui.CompassOverlay
import com.fengshui.app.map.CompassManager
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType
import com.fengshui.app.utils.RhumbLineUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import com.fengshui.app.map.abstraction.MapProvider
import com.fengshui.app.map.abstraction.MapType
import com.fengshui.app.map.ui.MapControlButtons
import com.fengshui.app.map.ui.RegistrationDialog

/**
 * 简易 MapScreen 示例：
 * - 占位地图区域（后续替换为真正的 MapView/MapCompose）
 * - 屏幕中心十字准心
 * - 右侧放大/缩小/图层切换控件（使用 `MapControlButtons`）
 */
@Composable
fun MapScreen(
    mapProvider: MapProvider,
    modifier: Modifier = Modifier,
    onCenterCrossClicked: (() -> Unit)? = null
) {
    var currentMapType by remember { mutableStateOf(MapType.VECTOR) }
    val context = LocalContext.current

    // GPS location state (nullable until set)
    var gpsLat by remember { mutableStateOf<Double?>(null) }
    var gpsLng by remember { mutableStateOf<Double?>(null) }
    var azimuth by remember { mutableStateOf(0f) }

    val compassManager = remember {
        CompassManager(context) { lat, lng, deg ->
            gpsLat = lat
            gpsLng = lng
            azimuth = deg
        }
    }

    DisposableEffect(Unit) {
        compassManager.start()
        onDispose {
            compassManager.stop()
        }
    }

    // repository
    val repo = remember { PointRepository(context) }
    var originPoint by remember { mutableStateOf<FengShuiPoint?>(null) }
    var destPoint by remember { mutableStateOf<FengShuiPoint?>(null) }
    val lines = remember { mutableStateListOf<Pair<FengShuiPoint, FengShuiPoint>>() }
    var showLineInfo by remember { mutableStateOf(false) }
    var lineInfoText by remember { mutableStateOf("") }
    var showTrialDialog by remember { mutableStateOf(false) }
    var trialMessage by remember { mutableStateOf("") }
    var showRegistrationDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 占位地图区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEEEEEE))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // convert screen point to lat/lng via MapProvider (implementation-specific)
                            val latlng = mapProvider.screenLocationToLatLng(offset.x, offset.y)
                            // check all polylines (origin-dest pairs)
                            val THRESHOLD_METERS = 60.0
                            for (pair in lines) {
                                val a = pair.first
                                val b = pair.second
                                val d = com.fengshui.app.utils.GeometryUtils.pointToLineDistanceMeters(
                                    latlng.latitude, latlng.longitude,
                                    a.latitude, a.longitude,
                                    b.latitude, b.longitude
                                )
                                if (d <= THRESHOLD_METERS) {
                                    // show info
                                    val bearing = RhumbLineUtils.calculateRhumbBearing(a.latitude, a.longitude, b.latitude, b.longitude)
                                    val shan = RhumbLineUtils.getShanName(bearing)
                                    val bagua = RhumbLineUtils.getBaGua(bearing)
                                    val wuxing = RhumbLineUtils.getWuXing(bearing)
                                    val dist = RhumbLineUtils.haversineDistanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                                    lineInfoText = "原点: ${a.name}\n终点: ${b.name}\n经纬: ${a.latitude}, ${a.longitude} → ${b.latitude}, ${b.longitude}\n方位角: ${"%.1f".format(bearing)}°\n24山: $shan\n八卦: $bagua\n五行: $wuxing\n直线距离: ${"%.1f".format(dist)} m"
                                    showLineInfo = true
                                    break
                                }
                            }
                        }
                    }
            ) {
                Text("Map Area (placeholder)", modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
            }

            // 屏幕中心十字准心
            Box(modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .clickable { onCenterCrossClicked?.invoke() }
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

            // 右侧控制按钮
            Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                MapControlButtons(
                    currentMapType = currentMapType,
                    onZoomIn = { mapProvider.zoomIn() },
                    onZoomOut = { mapProvider.zoomOut() },
                    onToggleMapType = { type ->
                        currentMapType = type
                        mapProvider.setMapType(type)
                    }
                )

                // Add origin / destination buttons for V0 single-case flow
                SpacerSmall()
                Button(onClick = {
                    // add origin at current GPS
                    if (gpsLat != null && gpsLng != null) {
                        scope.launch {
                            try {
                                val proj = repo.loadProjects().firstOrNull() ?: repo.createProject("默认案例")
                                val p = repo.createPoint("原点", gpsLat!!, gpsLng!!, PointType.ORIGIN, proj.id)
                                originPoint = p
                                mapProvider.addMarker(com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude), p.name)
                                // if destination exists, draw polyline and animate camera
                                if (destPoint != null) {
                                    mapProvider.addPolyline(
                                        com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude),
                                        com.fengshui.app.map.abstraction.UniversalLatLng(destPoint!!.latitude, destPoint!!.longitude)
                                    )
                                    lines.add(Pair(p, destPoint!!))
                                    mapProvider.animateCamera(com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude), 15f)
                                }
                            } catch (e: com.fengshui.app.TrialLimitException) {
                                trialMessage = e.message ?: "达到试用限制"
                                showTrialDialog = true
                            }
                        }
                    }
                }) { Text("加原点") }

                Button(onClick = {
                    if (gpsLat != null && gpsLng != null) {
                        scope.launch {
                            try {
                                val proj = repo.loadProjects().firstOrNull() ?: repo.createProject("默认案例")
                                val p = repo.createPoint("终点", gpsLat!!, gpsLng!!, PointType.DESTINATION, proj.id)
                                destPoint = p
                                mapProvider.addMarker(com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude), p.name)
                                if (originPoint != null) {
                                    mapProvider.addPolyline(
                                        com.fengshui.app.map.abstraction.UniversalLatLng(originPoint!!.latitude, originPoint!!.longitude),
                                        com.fengshui.app.map.abstraction.UniversalLatLng(p.latitude, p.longitude)
                                    )
                                    lines.add(Pair(originPoint!!, p))
                                    mapProvider.animateCamera(com.fengshui.app.map.abstraction.UniversalLatLng(originPoint!!.latitude, originPoint!!.longitude), 15f)
                                }
                            } catch (e: com.fengshui.app.TrialLimitException) {
                                trialMessage = e.message ?: "达到试用限制"
                                showTrialDialog = true
                            }
                        }
                    }
                }) { Text("加终点") }

                if (originPoint != null && destPoint != null) {
                    SpacerSmall()
                    Button(onClick = {
                        // compute and show line info
                        val bearing = RhumbLineUtils.calculateRhumbBearing(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                        val shan = RhumbLineUtils.getShanName(bearing)
                        val bagua = RhumbLineUtils.getBaGua(bearing)
                        val wuxing = RhumbLineUtils.getWuXing(bearing)
                        val dist = RhumbLineUtils.haversineDistanceMeters(originPoint!!.latitude, originPoint!!.longitude, destPoint!!.latitude, destPoint!!.longitude)
                        lineInfoText = "原点: ${originPoint!!.name}\n终点: ${destPoint!!.name}\n经纬: ${originPoint!!.latitude}, ${originPoint!!.longitude} → ${destPoint!!.latitude}, ${destPoint!!.longitude}\n方位角: ${"%.1f".format(bearing)}°\n24山: $shan\n八卦: $bagua\n五行: $wuxing\n直线距离: ${"%.1f".format(dist)} m"
                        showLineInfo = true
                    }) { Text("显示连线信息") }
                }
            }

            // Compass overlay (anchored bottom-start for placeholder map)
            if (gpsLat != null && gpsLng != null) {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    CompassOverlay(azimuthDegrees = azimuth, latitude = gpsLat, longitude = gpsLng)
                }
            }

            if (showLineInfo) {
                AlertDialog(
                    onDismissRequest = { showLineInfo = false },
                    confirmButton = {
                        TextButton(onClick = { showLineInfo = false }) { Text("确定") }
                    },
                    text = { Text(lineInfoText) }
                )
            }

            if (showTrialDialog) {
                AlertDialog(
                    onDismissRequest = { showTrialDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showTrialDialog = false }) { Text("取消") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRegistrationDialog = true
                            showTrialDialog = false
                        }) { Text("注册") }
                    },
                    text = { Text(trialMessage) }
                )
            }

            if (showRegistrationDialog) {
                RegistrationDialog(onDismissRequest = { showRegistrationDialog = false }) { code ->
                    scope.launch {
                        val ok = com.fengshui.app.TrialManager.registerWithCode(context, code)
                        if (ok) {
                            trialMessage = "注册成功，已解锁完整功能"
                            showRegistrationDialog = false
                            showTrialDialog = true
                        } else {
                            trialMessage = "注册码无效"
                            showRegistrationDialog = false
                            showTrialDialog = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacerSmall() {
    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.size(8.dp))
}
