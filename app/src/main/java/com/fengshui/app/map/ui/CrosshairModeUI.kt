package com.fengshui.app.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R
import com.fengshui.app.data.PointType

@Composable
fun CrosshairModeUI(
    title: String,
    subtitle: String,
    projectName: String,
    isLifeCircleSelection: Boolean,
    tempViewMode: Boolean,
    continuousAddMode: Boolean,
    continuousAddType: PointType,
    onSwitchContinuousAddType: () -> Unit,
    onStopContinuousAdd: () -> Unit,
    onSelectOrigin: () -> Unit,
    onSelectDestination: () -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
        // Semi-transparent overlay (does not intercept gestures)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x14000000))
        )

        // Center crosshair
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Red,
                        start = Offset(w / 2, 0f),
                        end = Offset(w / 2, h),
                        strokeWidth = 2f
                    )
                }
        )

        if (continuousAddMode) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 12.dp, end = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE6FFFFFF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.continuous_add_map_move_hint),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 11.sp,
                    color = Color(0xFF4A3A8C),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom panel
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.size(4.dp))
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF555555))
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    stringResource(id = R.string.crosshair_current_case, projectName),
                    fontSize = 11.sp,
                    color = Color(0xFF777777)
                )

                Spacer(modifier = Modifier.size(12.dp))

                when {
                    isLifeCircleSelection -> {
                        Button(onClick = onSelectOrigin, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.action_select_location))
                        }
                    }
                    tempViewMode -> {
                        Button(onClick = onSelectOrigin, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.action_locate_here))
                        }
                    }
                    else -> {
                        if (continuousAddMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        if (continuousAddType == PointType.ORIGIN) onSelectOrigin() else onSelectDestination()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (continuousAddType == PointType.ORIGIN) {
                                            stringResource(id = R.string.action_save_origin)
                                        } else {
                                            stringResource(id = R.string.action_save_destination)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Button(onClick = onSwitchContinuousAddType, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(id = R.string.action_switch_point_type))
                                }
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Button(onClick = onStopContinuousAdd, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(id = R.string.action_stop_add))
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(onClick = onSelectOrigin, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(id = R.string.action_save_origin))
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Button(onClick = onSelectDestination, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(id = R.string.action_save_destination))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))
                Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        }
    }
}
