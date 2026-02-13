package com.fengshui.app.map.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.fengshui.app.map.abstraction.amap.AMapProvider
import com.amap.api.maps.AMap
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.TextureMapView
import com.fengshui.app.utils.ApiKeyConfig
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * AmapMapViewWrapper - 在 Compose 中集成高德地图
 * 
 * 使用 AndroidView 包装原生 TextureMapView，支持 Compose 生命周期管理
 */
@Composable
fun AmapMapViewWrapper(
    context: Context,
    modifier: Modifier = Modifier,
    mapProvider: AMapProvider,
    onMapReady: (AMap) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(context, mapProvider) {
        val appContext = context.applicationContext
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(appContext, true)
        ApiKeyConfig.getAmapApiKey(appContext)
            ?.takeIf(ApiKeyConfig::isValidKey)
            ?.let { MapsInitializer.setApiKey(it) }

        TextureMapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(mapView, lifecycleOwner, mapProvider) {
        val aMap = mapView.map
        mapProvider.setAMap(aMap)
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.uiSettings.isCompassEnabled = false
        onMapReady(aMap)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }
    
    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
