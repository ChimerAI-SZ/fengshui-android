package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.fengshui.app.map.abstraction.MapProviderType
import com.fengshui.app.map.abstraction.MapType
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

/**
 * 简易右侧控制按钮列：放大/缩小/图层切换（矢量/卫星）
 * 回调采用 lambda，MapScreen 负责将行为转发到 MapProvider。
 */
@Composable
fun MapControlButtons(
    currentMapType: MapType,
    currentProviderType: MapProviderType,
    hasGoogleMap: Boolean,
    hasAmapMap: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleMapType: (MapType) -> Unit,
    onSwitchProvider: (MapProviderType) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetProvider = if (currentProviderType == MapProviderType.AMAP) {
        MapProviderType.GOOGLE
    } else {
        MapProviderType.AMAP
    }
    val targetEnabled = when (targetProvider) {
        MapProviderType.GOOGLE -> hasGoogleMap
        MapProviderType.AMAP -> hasAmapMap
    }
    val switchLabel = if (targetProvider == MapProviderType.GOOGLE) {
        stringResource(id = R.string.action_switch_provider_to_google)
    } else {
        stringResource(id = R.string.action_switch_provider_to_amap)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        val label = if (currentMapType == MapType.VECTOR) {
            stringResource(id = R.string.map_type_satellite_short)
        } else {
            stringResource(id = R.string.map_type_vector_short)
        }
        MapChipButton(
            text = label,
            selected = true,
            onClick = { onToggleMapType(if (currentMapType == MapType.VECTOR) MapType.SATELLITE else MapType.VECTOR) }
        )
        MapChipButton(
            text = switchLabel,
            selected = false,
            enabled = targetEnabled,
            onClick = { onSwitchProvider(targetProvider) }
        )

        Box(
            modifier = Modifier
                .width(88.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Button(
                    onClick = onZoomIn,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(4.dp, RoundedCornerShape(19.dp)),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2B2B2B)
                    )
                ) {
                    Text("+", color = Color(0xFF2B2B2B), fontSize = 14.sp)
                }

                Button(
                    onClick = onZoomOut,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(4.dp, RoundedCornerShape(19.dp)),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2B2B2B)
                    )
                ) {
                    Text("-", color = Color(0xFF2B2B2B), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun MapChipButton(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .width(88.dp)
            .heightIn(min = 34.dp)
            .shadow(4.dp, RoundedCornerShape(19.dp)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        shape = RoundedCornerShape(18.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(containerColor = Color(0xFF6A4FB5))
        } else {
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFFFFF),
                contentColor = Color(0xFF2B2B2B)
            )
        }
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            fontSize = 10.sp
        )
    }
}
