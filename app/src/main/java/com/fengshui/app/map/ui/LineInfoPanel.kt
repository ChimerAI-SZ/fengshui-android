package com.fengshui.app.map.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

/**
 * LineInfoPanel - 连线信息面板
 * 
 * 显示原点到终点的以下信息：
 * - 点位名称和坐标
 * - 方位角（Azimuth）
 * - 24山（如：子山午向）
 * - 八卦（如：离卦）
 * - 五行（如：火）
 * - 直线距离
 * 
 * 特性：
 * - 可展开/收起
 * - 卡片式设计
 * - 清晰的字段排列
 */
@Composable
fun LineInfoPanel(
    originName: String,
    destName: String,
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
    bearing: Float,
    shan: String,
    bagua: String,
    wuxing: String,
    distance: Float,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.line_info_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Text("×", fontSize = 28.sp, color = Color.Gray)
                }
            }

            if (isExpanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // 点位信息区块
                InfoSection(title = stringResource(id = R.string.line_info_section_points)) {
                    InfoRow(label = stringResource(id = R.string.label_origin), value = originName)
                    InfoRow(label = stringResource(id = R.string.label_destination), value = destName)
                    InfoCoordinate(
                        label = stringResource(id = R.string.label_origin_coordinates),
                        lat = originLat,
                        lng = originLng
                    )
                    InfoCoordinate(
                        label = stringResource(id = R.string.label_destination_coordinates),
                        lat = destLat,
                        lng = destLng
                    )
                }

                // 方位信息区块
                InfoSection(title = stringResource(id = R.string.line_info_section_direction)) {
                    InfoRow(label = stringResource(id = R.string.label_bearing), value = "${"%.1f".format(bearing)}°")
                    InfoRow(label = stringResource(id = R.string.label_shan_24), value = shan, backgroundColor = Color(0xFFFFF9C4))
                    InfoRow(label = stringResource(id = R.string.label_bagua), value = bagua, backgroundColor = Color(0xFFE1F5FE))
                    InfoRow(label = stringResource(id = R.string.label_wuxing), value = wuxing, backgroundColor = Color(0xFFF3E5F5))
                }

                // 距离信息
                InfoSection(title = stringResource(id = R.string.line_info_section_distance)) {
                    InfoRow(
                        label = stringResource(id = R.string.label_distance),
                        value = String.format("%.1f m", distance),
                        backgroundColor = Color(0xFFE8F5E9)
                    )
                }

                // 提示信息
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFDE7), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.line_info_tip),
                        fontSize = 12.sp,
                        color = Color(0xFFF57F17)
                    )
                }
            }
        }
    }
}

/**
 * 信息部分标题
 */
@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/**
 * 单行信息展示
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    backgroundColor: Color = Color(0xFFF5F5F5)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, shape = RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.weight(0.65f)
        )
    }
}

/**
 * 坐标信息展示（纬度、经度）
 */
@Composable
private fun InfoCoordinate(
    label: String,
    lat: Double,
    lng: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEDE7F6), shape = RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "N ${"%.6f".format(lat)}",
            fontSize = 11.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "E ${"%.6f".format(lng)}",
            fontSize = 11.sp,
            color = Color.Black
        )
    }
}
