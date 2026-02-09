package com.fengshui.app.utils

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * ApiKeyConfig - 获取构建时注入的 API Key 配置
 *
 * Phase 4: 从 AndroidManifest.xml 中读取 API Key
 * 这些 Key 由 gradle 的 manifestPlaceholders 在构建时填充
 * Keys 来自 local.properties (不在 git 中)
 */
object ApiKeyConfig {
    
    /**
     * 从 AndroidManifest.xml 的 meta-data 中获取 Google Maps API Key
     */
    fun getGoogleMapsApiKey(context: Context): String? {
        return try {
            val ai: ApplicationInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            ai.metaData?.getString("com.google.android.geo.API_KEY") ?: "PLACEHOLDER"
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从 AndroidManifest.xml 的 meta-data 中获取 Amap API Key
     */
    fun getAmapApiKey(context: Context): String? {
        return try {
            val ai: ApplicationInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            ai.metaData?.getString("com.amap.api.v2.apikey") ?: "PLACEHOLDER"
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 返回有效的 API Key（非 PLACEHOLDER，非空）
     */
    fun isValidKey(key: String?): Boolean {
        return !key.isNullOrBlank() && key != "PLACEHOLDER"
    }
}
