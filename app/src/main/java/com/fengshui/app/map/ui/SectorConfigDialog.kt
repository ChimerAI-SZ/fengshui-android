package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fengshui.app.data.BaGua
import com.fengshui.app.data.ShanUtils
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

enum class SectorMode {
    SHAN_24,
    BAGUA_8
}

data class SectorConfig(
    val keyword: String,
    val startAngle: Float,
    val endAngle: Float,
    val maxDistanceMeters: Float,
    val label: String
)

@Composable
fun SectorConfigDialog(
    onConfirm: (SectorConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val keywordState = remember { mutableStateOf("") }
    val distanceState = remember { mutableStateOf("1000") }
    val modeState = remember { mutableStateOf(SectorMode.SHAN_24) }
    val shanIndexState = remember { mutableStateOf(0) }
    val baguaIndexState = remember { mutableStateOf(0) }

    val shanName = ShanUtils.SHAN_NAMES[shanIndexState.value]
    val bagua = BaGua.values()[baguaIndexState.value]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.sector_search_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = keywordState.value,
                    onValueChange = { keywordState.value = it },
                    label = { Text(stringResource(id = R.string.label_keyword)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(8.dp))

                OutlinedTextField(
                    value = distanceState.value,
                    onValueChange = { distanceState.value = it },
                    label = { Text(stringResource(id = R.string.label_distance_range)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(12.dp))

                Row {
                    Button(onClick = { modeState.value = SectorMode.SHAN_24 }) {
                        Text(stringResource(id = R.string.sector_mode_shan))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(onClick = { modeState.value = SectorMode.BAGUA_8 }) {
                        Text(stringResource(id = R.string.sector_mode_bagua))
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                if (modeState.value == SectorMode.SHAN_24) {
                    Row {
                        Button(onClick = { shanIndexState.value = (shanIndexState.value + 23) % 24 }) {
                            Text(stringResource(id = R.string.sector_prev_shan))
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            stringResource(id = R.string.sector_current_label, shanName),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(onClick = { shanIndexState.value = (shanIndexState.value + 1) % 24 }) {
                            Text(stringResource(id = R.string.sector_next_shan))
                        }
                    }
                } else {
                    Row {
                        Button(onClick = { baguaIndexState.value = (baguaIndexState.value + 7) % 8 }) {
                            Text(stringResource(id = R.string.sector_prev_bagua))
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            stringResource(id = R.string.sector_current_label, bagua.label),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(onClick = { baguaIndexState.value = (baguaIndexState.value + 1) % 8 }) {
                            Text(stringResource(id = R.string.sector_next_bagua))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val distance = distanceState.value.toFloatOrNull()?.coerceIn(100f, 250000f) ?: 1000f

                val (startAngle, endAngle, label) = if (modeState.value == SectorMode.SHAN_24) {
                    val center = shanIndexState.value * 15f
                    val start = (center - 7.5f + 360f) % 360f
                    val end = (center + 7.5f) % 360f
                    Triple(start, end, shanName)
                } else {
                    val start = bagua.startAngle
                    val end = (bagua.startAngle + 45f) % 360f
                    Triple(start, end, bagua.label)
                }

                onConfirm(
                    SectorConfig(
                        keyword = keywordState.value.trim(),
                        startAngle = startAngle,
                        endAngle = endAngle,
                        maxDistanceMeters = distance,
                        label = label
                    )
                )
            }) { Text(stringResource(id = R.string.action_start_search)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        }
    )
}
