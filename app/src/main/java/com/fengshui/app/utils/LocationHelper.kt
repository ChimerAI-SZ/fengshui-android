package com.fengshui.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

/**
 * LocationHelper - GPS位置获取工具
 * 获取设备的GPS位置并持续监听位置变化
 */
class LocationHelper(
    private val context: Context,
    private val onLocationUpdate: (Double, Double) -> Unit
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationListener: LocationListener? = null
    
    /**
     * 开始监听位置变化
     */
    @SuppressLint("MissingPermission")
    fun start() {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return
        }
        
        // 先尝试获取最后已知位置
        try {
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            lastKnownLocation?.let {
                onLocationUpdate(it.latitude, it.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 创建位置监听器
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocationUpdate(location.latitude, location.longitude)
            }
            
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        // 注册位置更新监听
        try {
            // 优先使用GPS，如果不可用则使用网络定位
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // 最小时间间隔1秒
                    5f,    // 最小距离变化5米
                    locationListener!!
                )
            }
            
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    5f,
                    locationListener!!
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    /**
     * 停止监听位置变化
     */
    fun stop() {
        locationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        locationListener = null
    }
}
