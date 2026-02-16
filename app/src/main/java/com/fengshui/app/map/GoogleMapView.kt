package com.fengshui.app.map

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng

/**
 * Google Maps Composable 集成组件
 * 使用 AndroidView 包装 MapView 以支持 Compose
 */
@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    initialZoom: Float = 15f,
    initialCenter: LatLng? = null,
    onMapReady: (GoogleMap) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                onCreate(Bundle())
                getMapAsync { gMap ->
                    gMap.uiSettings.isRotateGesturesEnabled = true
                    gMap.uiSettings.isTiltGesturesEnabled = true
                    initialCenter?.let { center ->
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, initialZoom))
                    }
                    onMapReady(gMap)
                }
                onResume()
            }
        },
        modifier = modifier,
        update = { _ ->
            // 更新时的处理
        },
        onRelease = { view ->
            // 清理资源
            view.onPause()
            view.onDestroy()
        }
    )
}
