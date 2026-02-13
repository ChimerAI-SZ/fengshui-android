package com.fengshui.app.map.abstraction

object MapProviderSelector {
    fun detectRecommendedSDK(latitude: Double, longitude: Double): MapProviderType {
        return if (isInChina(latitude, longitude)) {
            MapProviderType.AMAP
        } else {
            MapProviderType.GOOGLE
        }
    }

    fun isInChina(lat: Double, lng: Double): Boolean {
        return lat in 3.86..53.55 && lng in 73.66..135.05
    }
}
