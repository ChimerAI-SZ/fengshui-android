package com.fengshui.app.map

import android.content.Context
import com.fengshui.app.map.abstraction.CameraPosition
import com.fengshui.app.map.abstraction.MapProviderType
import com.fengshui.app.map.abstraction.MapType
import com.fengshui.app.map.abstraction.UniversalLatLng
import com.fengshui.app.utils.Prefs

object MapSessionStore {
    private const val KEY_RESTORE_LAST_POSITION_ENABLED = "map_restore_last_position_enabled"
    private const val KEY_LAST_CAMERA_STATE = "map_last_camera_state"
    private const val KEY_LAST_PROVIDER_TYPE = "map_last_provider_type"
    private const val KEY_LAST_MAP_TYPE = "map_last_map_type"

    fun isRestoreLastPositionEnabled(context: Context): Boolean =
        Prefs.getBoolean(context, KEY_RESTORE_LAST_POSITION_ENABLED, true)

    fun setRestoreLastPositionEnabled(context: Context, enabled: Boolean) {
        Prefs.saveBoolean(context, KEY_RESTORE_LAST_POSITION_ENABLED, enabled)
    }

    fun saveCameraPosition(context: Context, position: CameraPosition) {
        val payload = listOf(
            position.target.latitude.toString(),
            position.target.longitude.toString(),
            position.zoom.toString(),
            position.bearing.toString()
        ).joinToString(",")
        Prefs.saveString(context, KEY_LAST_CAMERA_STATE, payload)
    }

    fun loadCameraPosition(context: Context): CameraPosition? {
        val raw = Prefs.getString(context, KEY_LAST_CAMERA_STATE)?.trim().orEmpty()
        if (raw.isBlank()) return null
        val parts = raw.split(",")
        if (parts.size != 4) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        val zoom = parts[2].toFloatOrNull() ?: return null
        val bearing = parts[3].toFloatOrNull() ?: 0f
        return CameraPosition(
            target = UniversalLatLng(lat, lng),
            zoom = zoom,
            bearing = bearing
        )
    }

    fun clearCameraPosition(context: Context) {
        Prefs.saveString(context, KEY_LAST_CAMERA_STATE, "")
    }

    fun saveMapProviderType(context: Context, providerType: MapProviderType) {
        Prefs.saveString(context, KEY_LAST_PROVIDER_TYPE, providerType.name)
    }

    fun loadMapProviderType(context: Context): MapProviderType? {
        val raw = Prefs.getString(context, KEY_LAST_PROVIDER_TYPE)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { MapProviderType.valueOf(raw) }.getOrNull()
    }

    fun saveMapType(context: Context, mapType: MapType) {
        Prefs.saveString(context, KEY_LAST_MAP_TYPE, mapType.name)
    }

    fun loadMapType(context: Context): MapType? {
        val raw = Prefs.getString(context, KEY_LAST_MAP_TYPE)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { MapType.valueOf(raw) }.getOrNull()
    }
}
