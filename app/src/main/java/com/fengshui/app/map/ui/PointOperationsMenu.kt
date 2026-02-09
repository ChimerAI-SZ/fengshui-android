package com.fengshui.app.map.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType

/**
 * PointOperationsMenu - 点位操作菜单
 *
 * 显示所有点位（原点和终点）并支持长按操作
 * - 原点可以重命名或删除
 * - 终点可以重命名或删除
 *
 * @param originPoint 原点
 * @param destPoints 终点列表
 * @param onPointLongPress 长按点位时的回调，返回点位ID
 * @param onDeletePoint 删除点位的回调
 * @param onRenamePoint 重命名点位的回调
 */
@Composable
fun PointOperationsMenu(
    originPoint: FengShuiPoint?,
    destPoints: List<FengShuiPoint>,
    onPointLongPress: (FengShuiPoint) -> Unit,
    onDeletePoint: (String) -> Unit,
    onRenamePoint: (FengShuiPoint) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var longPressedPointId by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(0.9f),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "点位列表 (${(if (originPoint != null) 1 else 0) + destPoints.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "展开/收起"
                )
            }

            if (expanded) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    // 原点
                    if (originPoint != null) {
                        item {
                            PointItem(
                                point = originPoint,
                                isLongPressed = longPressedPointId == originPoint.id,
                                onLongPress = {
                                    longPressedPointId = originPoint.id
                                    onPointLongPress(originPoint)
                                },
                                onDelete = { onDeletePoint(originPoint.id) },
                                onRename = { onRenamePoint(originPoint) }
                            )
                        }
                    }

                    // 终点
                    items(destPoints) { point ->
                        PointItem(
                            point = point,
                            isLongPressed = longPressedPointId == point.id,
                            onLongPress = {
                                longPressedPointId = point.id
                                onPointLongPress(point)
                            },
                            onDelete = { onDeletePoint(point.id) },
                            onRename = { onRenamePoint(point) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PointItem(
    point: FengShuiPoint,
    isLongPressed: Boolean,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isLongPressed) Color(0xFFFFEBEE) else Color.Transparent
            )
            .clickable(enabled = !isLongPressed) { }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 点位类型标签
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (point.type == PointType.ORIGIN) Color(0xFF4CAF50) else Color(0xFF2196F3),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (point.type == PointType.ORIGIN) "原" else "终",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = point.name,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "纬: %.4f, 经: %.4f".format(point.latitude, point.longitude),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 长按时显示操作按钮
        if (isLongPressed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PointActionButton(
                    text = "重命名",
                    backgroundColor = Color(0xFFFFC107),
                    onClick = onRename
                )
                PointActionButton(
                    text = "删除",
                    backgroundColor = Color(0xFFFF5252),
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun PointActionButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
