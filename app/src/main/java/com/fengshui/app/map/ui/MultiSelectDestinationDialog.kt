package com.fengshui.app.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType

/**
 * MultiSelectDestinationDialog - 多选终点对话框
 *
 * 用于从可用的终点列表中选择一个或多个要显示连线的终点
 *
 * @param destinations 所有可用的终点列表
 * @param selectedIds 已选中的终点ID集合
 * @param onConfirm 确认选择时的回调，返回选中的终点ID列表
 * @param onDismiss 取消/关闭时的回调
 */
@Composable
fun MultiSelectDestinationDialog(
    destinations: List<FengShuiPoint>,
    selectedIds: Set<String>,
    onConfirm: (selectedIds: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var localSelected by remember { mutableStateOf(selectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择终点")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (destinations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无终点，请先在地图上添加终点",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(destinations) { point ->
                            DestinationListItem(
                                point = point,
                                isSelected = localSelected.contains(point.id),
                                onSelectionChange = { selected ->
                                    localSelected = if (selected) {
                                        localSelected + point.id
                                    } else {
                                        localSelected - point.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(localSelected.toList())
                    onDismiss()
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DestinationListItem(
    point: FengShuiPoint,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onSelectionChange(!isSelected) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = point.name,
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                text = "纬: %.4f, 经: %.4f".format(point.latitude, point.longitude),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
