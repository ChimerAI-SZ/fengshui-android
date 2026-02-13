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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

/**
 * CompassOverlay - 风水罗盘视图，带有24山和8卦标注
 * 
 * 功能：
 * - 24山标注（子、丑、寅、卯...）
 * - 8卦符号（乾、坤、离、坎...）
 * - 度数标注
 * - 旋转指针
 * - 实时显示方位角和坐标
 */
@Composable
fun CompassOverlay(
    azimuthDegrees: Float,
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 200.dp,
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
    val directions = arrayOf("N", "E", "S", "W")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // 主罗盘
        Box(
            modifier = Modifier
                .size(compassSize)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .background(Color(0xFFFBF7F0).copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // 罗盘主体 Canvas
            Canvas(modifier = Modifier.size(compassSize)) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = size.minDimension / 2f
                val holeRadius = with(density) { centerHoleRadiusDp.toPx() }

                // ========== 外圈：360度标注 ==========
                // 外圆边框
                drawCircle(
                    color = Color(0xFF999999).copy(alpha = 0.2f),
                    radius = radius,
                    style = Stroke(width = 2f)
                )

                // 度数刻度（每10度一个）
                for (deg in 0 until 360 step 10) {
                    val angle = Math.toRadians((deg - 90).toDouble())
                    val outerRadius = radius * 0.95f
                    val innerRadius = radius * 0.88f
                    
                    val x1 = centerX + (outerRadius * cos(angle)).toFloat()
                    val y1 = centerY + (outerRadius * sin(angle)).toFloat()
                    val x2 = centerX + (innerRadius * cos(angle)).toFloat()
                    val y2 = centerY + (innerRadius * sin(angle)).toFloat()
                    
                    drawLine(
                        color = Color(0xFF666666).copy(alpha = 0.25f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.5f
                    )
                }

                // 外圈角度数字（每30度一个）
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val labelRadius = radius * 0.86f
                for (deg in 0 until 360 step 30) {
                    if (deg == 0 || deg == 90 || deg == 180 || deg == 270) {
                        continue
                    }
                    val angle = Math.toRadians((deg - 90).toDouble())
                    val tx = centerX + (labelRadius * cos(angle)).toFloat()
                    val ty = centerY + (labelRadius * sin(angle)).toFloat()
                    val label = deg.toString()
                    drawContext.canvas.nativeCanvas.drawText(label, tx, ty + textPaint.textSize / 3f, textPaint)
                }

                // ========== 24山圈 ==========
                val shanRadius = radius * 0.70f
                drawCircle(
                    color = Color(0xFFE8E8E8).copy(alpha = 0.15f),
                    radius = shanRadius,
                    style = Stroke(width = 1f)
                )

                // 24个扇形圈（每15度）
                for (i in 0 until 24) {
                    val angle1 = Math.toRadians(((i * 15) - 90).toDouble())
                    val angle2 = Math.toRadians((((i + 1) * 15) - 90).toDouble())
                    
                    val x1 = centerX + (radius * 0.65f * cos(angle1)).toFloat()
                    val y1 = centerY + (radius * 0.65f * sin(angle1)).toFloat()
                    val x2 = centerX + (radius * 0.75f * cos(angle1)).toFloat()
                    val y2 = centerY + (radius * 0.75f * sin(angle1)).toFloat()
                    
                    drawLine(
                        color = Color(0xFFCCCCCC).copy(alpha = 0.2f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1f
                    )
                }

                // ========== 8卦圈 ==========
                val baguaRadius = radius * 0.52f
                drawCircle(
                    color = Color(0xFFD0D0D0).copy(alpha = 0.2f),
                    radius = baguaRadius,
                    style = Stroke(width = 1f)
                )

                // ========== 方向圈 ==========
                val dirRadius = radius * 0.40f
                drawCircle(
                    color = Color(0xFFDCDCDC).copy(alpha = 0.2f),
                    radius = dirRadius,
                    style = Stroke(width = 1f)
                )

                // ========== 中心部分 ==========
                // 中心圆：保持全透明，仅保留红色十字
                drawCircle(
                    color = Color.Transparent,
                    radius = dirRadius * 0.75f
                )

                // 中心十字准心（红色）
                val crossSize = dirRadius * 0.35f
                drawLine(
                    color = Color.Red,
                    start = Offset(centerX - crossSize, centerY),
                    end = Offset(centerX + crossSize, centerY),
                    strokeWidth = 2.5f
                )
                drawLine(
                    color = Color.Red,
                    start = Offset(centerX, centerY - crossSize),
                    end = Offset(centerX, centerY + crossSize),
                    strokeWidth = 2.5f
                )

                // 蓝色圆圈（当前位置指示）
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = dirRadius * 0.6f,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color.Blue.copy(alpha = 0.35f),
                    radius = dirRadius * 0.55f,
                    style = Stroke(width = 2f)
                )
            }

            // 24山标注（外圈）
            // 计算罗盘的有效半径（以 px 为单位）
            val compassSizePx = with(density) { compassSize.toPx() }
            val radiusBase = compassSizePx / 2f
            
            for (i in 0 until 24) {
                val angleDeg = (i * 15f) - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val radiusPx = radiusBase * 0.68f
                
                val offsetX = radiusPx * cos(angleRad).toFloat()
                val offsetY = radiusPx * sin(angleRad).toFloat()
                
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center)
                        .offset(x = (offsetX / density.density).dp, y = (offsetY / density.density).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shanNames[i],
                        fontSize = if (i % 3 == 0) 11.sp else 10.sp,
                        fontWeight = if (i % 3 == 0) FontWeight.Bold else FontWeight.Normal,
                        style = TextStyle(color = if (i % 3 == 0) Color.Red else Color.Black),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 8卦标注（中间圆）
            for (i in 0 until 8) {
                val angleDeg = (i * 45f) + 22.5f - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val radiusPx = radiusBase * 0.48f
                
                val offsetX = radiusPx * cos(angleRad).toFloat()
                val offsetY = radiusPx * sin(angleRad).toFloat()
                
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                        .offset(x = (offsetX / density.density).dp, y = (offsetY / density.density).dp)
                        .background(
                            color = getBaGuaColor(i).copy(alpha = 0.45f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = baGuaNames[i],
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(color = Color.White),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 方向标注（N、E、S、W）
            Box(modifier = Modifier.size(compassSize), contentAlignment = Alignment.TopCenter) {
                Text("N", fontSize = 14.sp, fontWeight = FontWeight.Bold, style = TextStyle(color = Color.Red))
            }
            Box(modifier = Modifier.size(compassSize), contentAlignment = Alignment.BottomCenter) {
                Text("S", fontSize = 14.sp, fontWeight = FontWeight.Bold, style = TextStyle(color = Color.Red))
            }
            Box(modifier = Modifier.size(compassSize), contentAlignment = Alignment.CenterStart) {
                Text("W", fontSize = 13.sp, style = TextStyle(color = Color.Black))
            }
            Box(modifier = Modifier.size(compassSize), contentAlignment = Alignment.CenterEnd) {
                Text("E", fontSize = 13.sp, style = TextStyle(color = Color.Black))
            }

            // 旋转指针（红上黑下）
            Canvas(
                modifier = Modifier
                    .size(compassSize * 0.55f)
                    .align(Alignment.Center)
                    .rotate(azimuthDegrees)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val needleLength = size.height / 2.8f
                val arrowSize = needleLength * 0.18f
                
                // 上指针（红色）
                drawLine(
                    color = Color.Red,
                    start = Offset(cx, cy),
                    end = Offset(cx, cy - needleLength),
                    strokeWidth = 5.5f,
                    cap = StrokeCap.Round
                )

                val arrowPath = Path().apply {
                    moveTo(cx, cy - needleLength)
                    lineTo(cx - arrowSize, cy - needleLength + arrowSize * 1.4f)
                    lineTo(cx + arrowSize, cy - needleLength + arrowSize * 1.4f)
                    close()
                }
                drawPath(color = Color(0xFF8E1B1B), path = arrowPath, style = Stroke(width = 2f))
                drawPath(color = Color.Red, path = arrowPath)
                
                // 下指针（黑色）
                drawLine(
                    color = Color.Black,
                    start = Offset(cx, cy),
                    end = Offset(cx, cy + needleLength * 0.25f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                
                // 中心点
                drawCircle(color = Color.White, radius = 5.5f, center = Offset(cx, cy))
                drawCircle(color = Color.Black, radius = 3.5f, center = Offset(cx, cy))
            }
        }

        if (showInfo) {
            // 信息显示区（方位角和坐标）
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFF9C4), shape = CircleShape)
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
                        style = TextStyle(color = Color(0xFF333333))
                    )
                    if (latitude != null && longitude != null) {
                        Text(
                            text = "${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}",
                            fontSize = 9.sp,
                            style = TextStyle(color = Color(0xFF666666))
                        )
                    }
                }
            }
        }
    }
}

/**
 * 获取八卦对应的颜色
 */
private fun getBaGuaColor(index: Int): Color {
    return when (index % 8) {
        0 -> Color(0xFF1976D2)  // 坎 - 蓝色（水）
        1 -> Color(0xFF8B4513)  // 艮 - 棕色（土）
        2 -> Color(0xFF4CAF50)  // 震 - 绿色（木）
        3 -> Color(0xFF8BC34A)  // 巽 - 浅绿（木）
        4 -> Color(0xFFFF5722)  // 离 - 红色（火）
        5 -> Color(0xFFDCAB6F)  // 坤 - 土色（土）
        6 -> Color(0xFFFFC107)  // 兑 - 金色（金）
        else -> Color(0xFF9C27B0) // 乾 - 紫色（金）
    }
}
