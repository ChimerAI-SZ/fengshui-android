package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val providerButtonWidth = 132.dp

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
            stringResource(id = R.string.map_type_satellite_short)
        } else {
            stringResource(id = R.string.map_type_vector_short)
        }
        Button(
            onClick = { onToggleMapType(if (currentMapType == MapType.VECTOR) MapType.SATELLITE else MapType.VECTOR) },
            modifier = Modifier.size(width = providerButtonWidth, height = 64.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                fontSize = 14.sp
            )
        }

        val googleLabel = stringResource(id = R.string.provider_google_map_short)
        val googleSelected = currentProviderType == MapProviderType.GOOGLE
        Button(
            enabled = hasGoogleMap,
            onClick = { onSwitchProvider(MapProviderType.GOOGLE) },
            modifier = Modifier.size(width = providerButtonWidth, height = 72.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = if (googleSelected) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        ) {
            Text(
                text = googleLabel,
                maxLines = 1,
                softWrap = false,
                fontSize = 14.sp
            )
        }

        val amapLabel = stringResource(id = R.string.provider_amap_short)
        val amapSelected = currentProviderType == MapProviderType.AMAP
        Button(
            enabled = hasAmapMap,
            onClick = { onSwitchProvider(MapProviderType.AMAP) },
            modifier = Modifier.size(width = providerButtonWidth, height = 72.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = if (amapSelected) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        ) {
            Text(
                text = amapLabel,
                maxLines = 1,
                softWrap = false,
                fontSize = 14.sp
            )
        }
    }
}
