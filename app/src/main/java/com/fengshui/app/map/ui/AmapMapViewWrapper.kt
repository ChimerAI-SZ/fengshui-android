package com.fengshui.app.map.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fengshui.app.map.abstraction.amap.AMapProvider
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.TextureMapView

/**
 * AmapMapViewWrapper - 在 Compose 中集成高德地图
 * 
 * 使用 AndroidView 包装原生 TextureMapView，支持 Compose 生命周期管理
 */
@Composable
fun AmapMapViewWrapper(
    context: Context,
    modifier: Modifier = Modifier,
    onMapReady: (AMapProvider) -> Unit
) {
    val mapView = remember { TextureMapView(context) }
    val mapProvider = remember { AMapProvider(context) }
    
    DisposableEffect(Unit) {
        // AMap SDK privacy compliance (required for map display)
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        mapView.onCreate(null)
        
        // 高德地图在创建后立即可用
        val aMap = mapView.map
        mapProvider.setAMap(aMap)
        
        // UI 设置（可选）
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.uiSettings.isCompassEnabled = false
        
        onMapReady(mapProvider)
        
        mapView.onResume()
        
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
    
    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
