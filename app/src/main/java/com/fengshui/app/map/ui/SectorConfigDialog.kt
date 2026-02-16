package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fengshui.app.data.BaGua
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R
import com.fengshui.app.utils.ShanTextResolver
import kotlin.math.roundToInt

enum class SectorMode {
    SHAN_24,
    BAGUA_8
}

enum class DistanceUnit {
    METERS,
    KILOMETERS
}

data class SectorConfig(
    val keyword: String,
    val startAngle: Float,
    val endAngle: Float,
    val maxDistanceMeters: Float,
    val label: String,
    val mode: SectorMode
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectorConfigDialog(
    initialConfig: SectorConfig? = null,
    hasExistingSector: Boolean = false,
    onConfirm: (SectorConfig, Boolean) -> Unit,
    onClearSector: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var keyword by remember { mutableStateOf(initialConfig?.keyword ?: "") }
    var distanceInput by remember {
        mutableStateOf(
            when {
                initialConfig == null -> "20"
                initialConfig.maxDistanceMeters >= 1000f -> ((((initialConfig.maxDistanceMeters / 1000f) * 10f).roundToInt()) / 10f).toString()
                else -> initialConfig.maxDistanceMeters.roundToInt().toString()
            }
        )
    }
    var distanceUnit by remember {
        mutableStateOf(
            if (initialConfig != null && initialConfig.maxDistanceMeters < 1000f) {
                DistanceUnit.METERS
            } else {
                DistanceUnit.KILOMETERS
            }
        )
    }
    var mode by remember { mutableStateOf(initialConfig?.mode ?: SectorMode.SHAN_24) }
    var shanIndex by remember {
        mutableStateOf(
            if (initialConfig?.mode == SectorMode.SHAN_24) {
                val center = sectorCenterAngle(initialConfig.startAngle, initialConfig.endAngle)
                (((center + 7.5f) / 15f).toInt() % 24 + 24) % 24
            } else {
                0
            }
        )
    }
    var baguaIndex by remember {
        mutableStateOf(
            if (initialConfig?.mode == SectorMode.BAGUA_8) {
                BaGua.values().indexOfFirst {
                    kotlin.math.abs(normalizeAngle(it.startAngle - initialConfig.startAngle)) < 0.5f
                }.let { if (it >= 0) it else 0 }
            } else {
                0
            }
        )
    }

    val shanName = ShanTextResolver.shanName(context, shanIndex)
    val bagua = BaGua.values()[baguaIndex]
    val baguaName = ShanTextResolver.baguaName(context, bagua)
    val distanceNumber = distanceInput.toFloatOrNull()
    val distanceMeters = when (distanceUnit) {
        DistanceUnit.METERS -> distanceNumber
        DistanceUnit.KILOMETERS -> distanceNumber?.times(1000f)
    }
    val distanceError = when {
        distanceNumber == null -> stringResource(id = R.string.sector_invalid_distance)
        distanceMeters == null || distanceMeters < 100f -> stringResource(id = R.string.sector_min_distance_error)
        distanceMeters > 5_000_000f -> stringResource(id = R.string.sector_max_distance_error)
        else -> null
    }
    val canSubmit = distanceError == null
    val hasKeyword = keyword.trim().isNotEmpty()
    val submitLabelRes = when {
        hasExistingSector && hasKeyword -> R.string.action_clear_draw_and_search
        hasExistingSector -> R.string.action_clear_and_draw
        hasKeyword -> R.string.action_draw_and_search
        else -> R.string.action_draw_region
    }
    val keywordPresets = listOf(
        stringResource(id = R.string.sector_keyword_residence),
        stringResource(id = R.string.sector_keyword_hospital),
        stringResource(id = R.string.sector_keyword_tower)
    )
    val distancePresetsKm = listOf(20, 50, 200, 1000, 3000, 5000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.sector_search_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(id = R.string.label_keyword)) },
                    placeholder = { Text(stringResource(id = R.string.sector_keyword_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keywordPresets.forEach { preset ->
                        AssistChip(
                            onClick = { keyword = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = { distanceInput = it },
                    label = { Text(stringResource(id = R.string.label_distance_range)) },
                    supportingText = {
                        val text = distanceError ?: stringResource(id = R.string.sector_distance_hint)
                        Text(
                            text = text,
                            color = if (distanceError == null) Color.Unspecified else Color(0xFFB00020)
                        )
                    },
                    isError = distanceError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { distanceUnit = DistanceUnit.METERS },
                        colors = if (distanceUnit == DistanceUnit.METERS) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) { Text(stringResource(id = R.string.unit_meters)) }
                    Button(
                        onClick = { distanceUnit = DistanceUnit.KILOMETERS },
                        colors = if (distanceUnit == DistanceUnit.KILOMETERS) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) { Text(stringResource(id = R.string.unit_kilometers)) }
                }

                Spacer(modifier = Modifier.size(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    distancePresetsKm.forEach { preset ->
                        AssistChip(
                            onClick = {
                                distanceInput = preset.toString()
                                distanceUnit = DistanceUnit.KILOMETERS
                            },
                            label = { Text(stringResource(id = R.string.sector_distance_preset_km, preset)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Row {
                    Button(onClick = { mode = SectorMode.SHAN_24 }) {
                        Text(stringResource(id = R.string.sector_mode_shan))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(onClick = { mode = SectorMode.BAGUA_8 }) {
                        Text(stringResource(id = R.string.sector_mode_bagua))
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                if (mode == SectorMode.SHAN_24) {
                    Row {
                        Button(onClick = { shanIndex = (shanIndex + 23) % 24 }) {
                            Text(stringResource(id = R.string.sector_prev_shan))
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            stringResource(id = R.string.sector_current_label, shanName),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(onClick = { shanIndex = (shanIndex + 1) % 24 }) {
                            Text(stringResource(id = R.string.sector_next_shan))
                        }
                    }
                } else {
                    Row {
                        Button(onClick = { baguaIndex = (baguaIndex + 7) % 8 }) {
                            Text(stringResource(id = R.string.sector_prev_bagua))
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            stringResource(id = R.string.sector_current_label, baguaName),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(onClick = { baguaIndex = (baguaIndex + 1) % 8 }) {
                            Text(stringResource(id = R.string.sector_next_bagua))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    val finalDistanceMeters = distanceMeters ?: return@Button

                    val (startAngle, endAngle, label) = if (mode == SectorMode.SHAN_24) {
                        val center = shanIndex * 15f
                        val start = (center - 7.5f + 360f) % 360f
                        val end = (center + 7.5f) % 360f
                        Triple(start, end, shanName)
                    } else {
                        val start = bagua.startAngle
                        val end = (bagua.startAngle + 45f) % 360f
                        Triple(start, end, baguaName)
                    }

                    onConfirm(
                        SectorConfig(
                            keyword = keyword.trim(),
                            startAngle = startAngle,
                            endAngle = endAngle,
                            maxDistanceMeters = finalDistanceMeters,
                            label = label,
                            mode = mode
                        ),
                        hasExistingSector
                    )
                }
            ) { Text(stringResource(id = submitLabelRes)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasExistingSector) {
                    TextButton(onClick = onClearSector) {
                        Text(stringResource(id = R.string.action_clear_region))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
            }
        }
    )
}

private fun normalizeAngle(angle: Float): Float {
    var value = angle % 360f
    if (value < 0f) value += 360f
    return value
}

private fun sectorCenterAngle(start: Float, end: Float): Float {
    val span = if (end >= start) end - start else (360f - start) + end
    return normalizeAngle(start + span / 2f)
}
