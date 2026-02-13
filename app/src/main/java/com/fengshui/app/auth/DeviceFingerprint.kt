package com.fengshui.app.auth

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceFingerprint {
    fun get(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
        return androidId.toMD5()
    }
}

private fun String.toMD5(): String {
    val md = MessageDigest.getInstance("MD5")
    val bytes = md.digest(toByteArray(Charsets.UTF_8))
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        sb.append(((b.toInt() shr 4) and 0x0F).toString(16))
        sb.append((b.toInt() and 0x0F).toString(16))
    }
    return sb.toString()
}
