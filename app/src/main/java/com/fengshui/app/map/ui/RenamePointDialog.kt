package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

/**
 * RenamePointDialog - 点位重命名对话框
 *
 * 用于编辑已有点位的名称
 *
 * @param pointName 当前点位名称
 * @param onConfirm 确认时的回调，返回新名称
 * @param onDismiss 取消/关闭时的回调
 */
@Composable
fun RenamePointDialog(
    pointName: String,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(pointName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(id = R.string.rename_point_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(id = R.string.rename_point_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    singleLine = true,
                    maxLines = 1
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName.trim())
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(id = R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}
