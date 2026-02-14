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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset as UiOffset
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
    fun inwardReadableRotation(angleDeg: Float): Float {
        // Inward radial orientation.
        var rotation = angleDeg - 90f
        // Keep lower-half labels upright/readable.
        if (angleDeg > 90f && angleDeg < 270f) {
            rotation += 180f
        }
        return rotation
    }

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

                    drawCircle(color = Color.White, radius = radius, style = Stroke(width = 2.8f))
                    drawCircle(color = Color.Black, radius = radius, style = Stroke(width = 1.4f))
                    // subtle readable ring behind degree labels
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = radius * 0.90f
                    )

                    // High-precision degree scale:
                    // - 1deg minor ticks
                    // - 5deg medium ticks
                    // - 10deg major ticks
                    for (deg in 0 until 360) {
                        val angle = Math.toRadians((deg - 90).toDouble())
                        val outerRadius = radius * 0.97f
                        val innerRadius = when {
                            deg % 10 == 0 -> radius * 0.845f
                            deg % 5 == 0 -> radius * 0.885f
                            else -> radius * 0.93f
                        }
                        val x1 = centerX + (outerRadius * cos(angle)).toFloat()
                        val y1 = centerY + (outerRadius * sin(angle)).toFloat()
                        val x2 = centerX + (innerRadius * cos(angle)).toFloat()
                        val y2 = centerY + (innerRadius * sin(angle)).toFloat()
                        val blackWidth = when {
                            deg % 10 == 0 -> 1.5f
                            deg % 5 == 0 -> 1.0f
                            else -> 0.55f
                        }
                        val whiteBoost = when {
                            deg % 10 == 0 -> 1.2f
                            deg % 5 == 0 -> 0.9f
                            else -> 0.55f
                        }
                        val blackAlpha = when {
                            deg % 10 == 0 -> 1.0f
                            deg % 5 == 0 -> 0.88f
                            else -> 0.62f
                        }
                        drawLine(
                            color = Color.White.copy(alpha = if (deg % 10 == 0) 0.98f else 0.82f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = blackWidth + whiteBoost
                        )
                        drawLine(
                            color = Color.Black.copy(alpha = blackAlpha),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = blackWidth
                        )
                    }

                    val degreeStrokePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = with(density) { 1.8.dp.toPx() }
                    }
                    val degreeFillPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    val labelRadius = radius * 0.80f
                    for (deg in 0 until 360 step 10) {
                        val angle = Math.toRadians((deg - 90).toDouble())
                        val tx = centerX + (labelRadius * cos(angle)).toFloat()
                        val ty = centerY + (labelRadius * sin(angle)).toFloat()
                        drawContext.canvas.nativeCanvas.drawText(
                            deg.toString(),
                            tx,
                            ty + degreeFillPaint.textSize / 3f,
                            degreeStrokePaint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            deg.toString(),
                            tx,
                            ty + degreeFillPaint.textSize / 3f,
                            degreeFillPaint
                        )
                    }

                    val shanRadius = radius * 0.70f
                    drawCircle(color = Color.White.copy(alpha = 0.97f), radius = shanRadius, style = Stroke(width = 2.2f))
                    drawCircle(color = Color.Black.copy(alpha = 0.9f), radius = shanRadius, style = Stroke(width = 1f))

                    // Draw 24-shan axis lines as full diameters so they stay continuous through center.
                    for (i in 0 until 12) {
                        val angle = Math.toRadians(((i * 15) - 90).toDouble())
                        val r = radius * 0.965f
                        val x1 = centerX - (r * cos(angle)).toFloat()
                        val y1 = centerY - (r * sin(angle)).toFloat()
                        val x2 = centerX + (r * cos(angle)).toFloat()
                        val y2 = centerY + (r * sin(angle)).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 1.2f
                        )
                        drawLine(
                            color = Color.Black.copy(alpha = 0.72f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 1.0f
                        )
                    }

                    val baguaRadius = radius * 0.52f
                    val dirRadius = radius * 0.40f
                    drawCircle(color = Color.White.copy(alpha = 0.95f), radius = baguaRadius, style = Stroke(width = 2.0f))
                    drawCircle(color = Color.White.copy(alpha = 0.95f), radius = dirRadius, style = Stroke(width = 2.0f))
                    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = holeRadius, style = Stroke(width = 1.8f))
                    drawCircle(color = Color.Black.copy(alpha = 0.82f), radius = baguaRadius, style = Stroke(width = 1f))
                    drawCircle(color = Color.Black.copy(alpha = 0.82f), radius = dirRadius, style = Stroke(width = 1f))
                    drawCircle(color = Color.Black.copy(alpha = 0.72f), radius = holeRadius, style = Stroke(width = 1f))

                    // Repaint axis lines on top of rings to guarantee fully continuous center/edge visibility.
                    for (i in 0 until 12) {
                        val angle = Math.toRadians(((i * 15) - 90).toDouble())
                        val r = radius * 0.965f
                        val x1 = centerX - (r * cos(angle)).toFloat()
                        val y1 = centerY - (r * sin(angle)).toFloat()
                        val x2 = centerX + (r * cos(angle)).toFloat()
                        val y2 = centerY + (r * sin(angle)).toFloat()
                        drawLine(
                            color = Color.Black.copy(alpha = 0.8f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 1.0f
                        )
                    }

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
                    val textRotation = inwardReadableRotation((angleDeg + 360f) % 360f)

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
                            fontSize = if (i % 3 == 0) 14.sp else 11.sp,
                            fontWeight = if (i % 3 == 0) FontWeight.Bold else FontWeight.Normal,
                            style = TextStyle(
                                color = if (i % 3 == 0) Color(0xFFB31212) else Color.Black,
                                shadow = Shadow(
                                    color = Color.White,
                                    offset = UiOffset(0f, 0f),
                                    blurRadius = 2.8f
                                )
                            ),
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
                    val textRotation = inwardReadableRotation((angleDeg + 360f) % 360f)

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
                            style = TextStyle(
                                color = Color.Black,
                                shadow = Shadow(
                                    color = Color.White,
                                    offset = UiOffset(0f, 0f),
                                    blurRadius = 2.8f
                                )
                            ),
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
