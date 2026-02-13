package com.fengshui.app.map.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.fengshui.app.map.abstraction.amap.AMapProvider

@Composable
fun AMapViewWrapper(
    modifier: Modifier = Modifier,
    mapProvider: AMapProvider,
    onMapReady: (AMap) -> Unit = {}
) {
    val context: Context = LocalContext.current

    // ✅ Compose 里记住 MapView
    val mapView = remember { MapView(context) }

    // ✅ 生命周期绑定
    DisposableEffect(Unit) {
        mapView.onCreate(null)

        // ✅ AMap SDK v10：直接取 map（不要 getMap { } 回调）
        val aMap: AMap = mapView.map
        mapProvider.setAMap(aMap)
        onMapReady(aMap)

        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    // ✅ 把原生 MapView 放进 Compose
    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
