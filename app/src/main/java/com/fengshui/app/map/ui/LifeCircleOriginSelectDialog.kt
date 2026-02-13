package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fengshui.app.data.FengShuiPoint
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

@Composable
fun LifeCircleOriginSelectDialog(
    origins: List<FengShuiPoint>,
    onConfirm: (List<FengShuiPoint>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.life_circle_select_origins_title)) },
        text = {
            Column {
                origins.forEach { origin ->
                    val checked = selectedIds.contains(origin.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked && selectedIds.size < 3) {
                                    selectedIds.add(origin.id)
                                } else if (!isChecked) {
                                    selectedIds.remove(origin.id)
                                }
                            }
                        )
                        Text(origin.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = origins.filter { selectedIds.contains(it.id) }
                    onConfirm(selected)
                },
                enabled = selectedIds.size == 3
            ) {
                Text(stringResource(id = R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        }
    )
}
