package com.fengshui.app.map.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fengshui.app.map.abstraction.MapProvider
import com.fengshui.app.map.abstraction.googlemaps.GoogleMapProvider
import com.google.android.gms.maps.MapView

/**
 * GoogleMapViewWrapper - 在 Compose 中集成 GoogleMap
 * 
 * 使用 AndroidView 包装原生 MapView，支持 Compose 生命周期管理
 */
@Composable
fun GoogleMapViewWrapper(
    context: Context,
    modifier: Modifier = Modifier,
    onMapReady: (GoogleMapProvider) -> Unit
) {
    val mapView = remember { MapView(context) }
    val mapProvider = remember { GoogleMapProvider(context) }
    
    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.getMapAsync { googleMap ->
            mapProvider.setGoogleMap(googleMap)
            onMapReady(mapProvider)
        }
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
