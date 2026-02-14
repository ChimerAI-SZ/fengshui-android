package com.fengshui.app.map.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

/**
 * CompassOverlay - 风水罗盘视图，带有24山和8卦标注
 * 
 * 功能：
 * - 24山标注（子、丑、寅、卯...）
 * - 8卦符号（乾、坤、离、坎...）
 * - 度数标注
 * - 旋转盘面（无指针）
 * - 实时显示方位角和坐标
 */
@Composable
fun CompassOverlay(
    azimuthDegrees: Float,
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 240.dp,
    centerHoleRadiusDp: androidx.compose.ui.unit.Dp = 20.dp,
    showInfo: Boolean = true
) {
    val compassSize = sizeDp
    val density = LocalDensity.current
    // 完整的24山名称，按罗盘顺逆序排列（北方为起点）
    val shanNames = arrayOf(
        "子", "癸", "丑", "艮", "寅", "甲",
        "卯", "乙", "辰", "巽", "巳", "丙",
        "午", "丁", "未", "坤", "申", "庚",
        "酉", "辛", "戌", "乾", "亥", "壬"
    )
    
    val baGuaNames = arrayOf("坎", "艮", "震", "巽", "离", "坤", "兑", "乾")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // 主罗盘
        Box(
            modifier = Modifier
                .size(compassSize)
                .background(Color.Transparent, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Rotate dial itself (no rotating needle).
            Box(
                modifier = Modifier
                    .size(compassSize)
                    .rotate(-azimuthDegrees),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(compassSize)) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = size.minDimension / 2f
                    val holeRadius = with(density) { centerHoleRadiusDp.toPx() }

                    drawCircle(color = Color.Black, radius = radius, style = Stroke(width = 1.4f))

                    for (deg in 0 until 360 step 5) {
                        val angle = Math.toRadians((deg - 90).toDouble())
                        val outerRadius = radius * 0.97f
                        val innerRadius = if (deg % 10 == 0) radius * 0.87f else radius * 0.91f
                        val x1 = centerX + (outerRadius * cos(angle)).toFloat()
                        val y1 = centerY + (outerRadius * sin(angle)).toFloat()
                        val x2 = centerX + (innerRadius * cos(angle)).toFloat()
                        val y2 = centerY + (innerRadius * sin(angle)).toFloat()
                        drawLine(
                            color = Color.Black.copy(alpha = if (deg % 10 == 0) 1.0f else 0.78f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = if (deg % 10 == 0) 1.2f else 0.85f
                        )
                    }

                    val degreePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = with(density) { 9.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    val labelRadius = radius * 0.90f
                    for (deg in 0 until 360 step 10) {
                        val angle = Math.toRadians((deg - 90).toDouble())
                        val tx = centerX + (labelRadius * cos(angle)).toFloat()
                        val ty = centerY + (labelRadius * sin(angle)).toFloat()
                        drawContext.canvas.nativeCanvas.drawText(
                            deg.toString(),
                            tx,
                            ty + degreePaint.textSize / 3f,
                            degreePaint
                        )
                    }

                    val shanRadius = radius * 0.70f
                    drawCircle(color = Color.Black.copy(alpha = 0.9f), radius = shanRadius, style = Stroke(width = 1f))

                    for (i in 0 until 24) {
                        val angle = Math.toRadians(((i * 15) - 90).toDouble())
                        val x1 = centerX + (radius * 0.57f * cos(angle)).toFloat()
                        val y1 = centerY + (radius * 0.57f * sin(angle)).toFloat()
                        val x2 = centerX + (radius * 0.79f * cos(angle)).toFloat()
                        val y2 = centerY + (radius * 0.79f * sin(angle)).toFloat()
                        drawLine(
                            color = Color.Black.copy(alpha = 0.55f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 0.9f
                        )
                    }

                    val baguaRadius = radius * 0.52f
                    val dirRadius = radius * 0.40f
                    drawCircle(color = Color.Black.copy(alpha = 0.82f), radius = baguaRadius, style = Stroke(width = 1f))
                    drawCircle(color = Color.Black.copy(alpha = 0.82f), radius = dirRadius, style = Stroke(width = 1f))
                    drawCircle(color = Color.Black.copy(alpha = 0.72f), radius = holeRadius, style = Stroke(width = 1f))

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.9f),
                        radius = holeRadius * 0.12f,
                        center = Offset(centerX, centerY)
                    )
                }

                val compassSizePx = with(density) { compassSize.toPx() }
                val radiusBase = compassSizePx / 2f

                for (i in 0 until 24) {
                    val angleDeg = (i * 15f) + 7.5f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val radiusPx = if (i % 3 == 0) radiusBase * 0.64f else radiusBase * 0.60f
                    val offsetX = radiusPx * cos(angleRad).toFloat()
                    val offsetY = radiusPx * sin(angleRad).toFloat()
                    // Radially inward orientation.
                    val textRotation = angleDeg - 90f

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.Center)
                            .offset(x = (offsetX / density.density).dp, y = (offsetY / density.density).dp)
                            .rotate(textRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shanNames[i],
                            fontSize = if (i % 3 == 0) 13.sp else 10.sp,
                            fontWeight = if (i % 3 == 0) FontWeight.Bold else FontWeight.Normal,
                            style = TextStyle(color = if (i % 3 == 0) Color(0xFFB31212) else Color.Black),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                for (i in 0 until 8) {
                    val angleDeg = (i * 45f) + 22.5f - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val radiusPx = radiusBase * 0.48f
                    val offsetX = radiusPx * cos(angleRad).toFloat()
                    val offsetY = radiusPx * sin(angleRad).toFloat()
                    // Radially inward orientation.
                    val textRotation = angleDeg - 90f

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                            .offset(x = (offsetX / density.density).dp, y = (offsetY / density.density).dp)
                            .rotate(textRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = baGuaNames[i],
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(color = Color.Black),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Fixed top reference marker (non-rotating).
            Canvas(
                modifier = Modifier
                    .size(compassSize)
                    .align(Alignment.Center)
            ) {
                val cx = size.width / 2f
                val topY = size.height * 0.02f
                val triWidth = size.width * 0.05f
                val triHeight = size.height * 0.05f
                val marker = Path().apply {
                    moveTo(cx, topY)
                    lineTo(cx - triWidth / 2f, topY + triHeight)
                    lineTo(cx + triWidth / 2f, topY + triHeight)
                    close()
                }
                drawPath(marker, color = Color.Black)
            }
        }

        if (showInfo) {
            // 信息显示区（方位角和坐标）
            Box(
                modifier = Modifier
                    .background(Color.Transparent, shape = CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.compass_bearing_label, azimuthDegrees),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(color = Color.Black)
                    )
                    if (latitude != null && longitude != null) {
                        Text(
                            text = "${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}",
                            fontSize = 9.sp,
                            style = TextStyle(color = Color.Black.copy(alpha = 0.86f))
                        )
                    }
                }
            }
        }
    }
}
