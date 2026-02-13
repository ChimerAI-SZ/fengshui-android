package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Button(onClick = onZoomIn, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) {
            Text("+", color = Color.White)
        }

        Button(onClick = onZoomOut, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)) {
            Text("-", color = Color.White)
        }

        val label = if (currentMapType == MapType.VECTOR) {
            stringResource(id = R.string.map_type_satellite)
        } else {
            stringResource(id = R.string.map_type_vector)
        }
        Button(onClick = { onToggleMapType(if (currentMapType == MapType.VECTOR) MapType.SATELLITE else MapType.VECTOR) }, modifier = Modifier.size(64.dp)) {
            Text(label)
        }

        val googleLabel = stringResource(id = R.string.provider_google_map_short)
        Button(
            enabled = hasGoogleMap,
            onClick = { onSwitchProvider(MapProviderType.GOOGLE) },
            modifier = Modifier.size(72.dp)
        ) {
            Text(if (currentProviderType == MapProviderType.GOOGLE) "[$googleLabel]" else googleLabel)
        }

        val amapLabel = stringResource(id = R.string.provider_amap_short)
        Button(
            enabled = hasAmapMap,
            onClick = { onSwitchProvider(MapProviderType.AMAP) },
            modifier = Modifier.size(72.dp)
        ) {
            Text(if (currentProviderType == MapProviderType.AMAP) "[$amapLabel]" else amapLabel)
        }
    }
}
