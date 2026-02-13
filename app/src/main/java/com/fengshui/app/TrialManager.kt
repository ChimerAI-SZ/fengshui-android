package com.fengshui.app

import android.content.Context
import com.fengshui.app.auth.DeviceFingerprint
import com.fengshui.app.utils.Prefs
import org.json.JSONObject
import java.security.MessageDigest

object TrialManager {
    const val TRIAL_MAX_GROUPS = 2
    const val TRIAL_MAX_ORIGINS = 2
    const val TRIAL_MAX_DESTINATIONS = 5

    private const val PREF_REGISTERED_CODE = "fengshui_registered_code"
    private const val PREF_REGISTERED_DEVICE = "fengshui_registered_device"
    private const val PREF_REGISTRATION_CODES = "registration_codes"

    // Keep only SHA-256 hashes in code to avoid exposing plain unlock codes.
    private val VALID_LOCAL_CODE_HASHES = setOf(
        "702373b221ea8d34f040dc966e20e57b9506164056f396a31a1327e1871bd4d0",
        "1670e8f37ea84331ccd62a50394549c3399dd5903520f5f4306212b6e7f9c134",
        "29781dbb13650a4045a4aaf1dcf8d83203510d31fe5d540bd55884c8f3e06fb5",
        "22f6eab1851118cd19af8b0a1059dde7facce05a318d7ca6896963226ceda7c5",
        "812f65cb2543d39e37940efa43932112cbadf69da3f5ee5fd2f9d9a9f3c60ccc",
        "6cb07f7b52dfc517062bb857b33adf81be3a1e10a48c9fc28097d857bbd3b030",
        "cb9857fcb838663852420dbb8d49f756ef108311df88201f901c0edfe4b27aa3",
        "0475be07e741e0fdfb1299942a2f0fcef3ef3436d99b135c10198aa1d6539bf5",
        "ff4a7805abc706219a59c83c351878c9fc7ec95054e518521bfefa2047266429",
        "ada127ff8003e7b9e1f651e1e667acb6dcf397a82c330fabb06391487e437cf0"
    )

    @Deprecated("Use isRegistered(context) when context is available")
    fun isRegistered(): Boolean = false

    fun isRegistered(context: Context): Boolean {
        val deviceId = DeviceFingerprint.get(context)
        val savedCode = Prefs.getString(context, PREF_REGISTERED_CODE) ?: return false
        val savedDevice = Prefs.getString(context, PREF_REGISTERED_DEVICE) ?: return false
        if (savedDevice != deviceId) {
            return false
        }
        val map = loadRegistrationMap(context)
        return map.optString(savedCode, "") == deviceId
    }

    /**
     * 简单的本地注册码校验接口。
     * 当前实现使用固定示例码 `TRIAL-UNLOCK-2026`。
     * 成功时保存本地注册标记并返回 true。
     */
    fun registerWithCode(context: Context, code: String): Boolean {
        val normalized = code.trim().uppercase()
        if (!VALID_LOCAL_CODE_HASHES.contains(normalized.sha256())) {
            return false
        }

        val deviceId = DeviceFingerprint.get(context)
        val map = loadRegistrationMap(context)
        val boundDevice = map.optString(normalized, null)

        if (boundDevice != null && boundDevice != deviceId) {
            return false
        }

        map.put(normalized, deviceId)
        Prefs.saveString(context, PREF_REGISTRATION_CODES, map.toString())
        Prefs.saveString(context, PREF_REGISTERED_CODE, normalized)
        Prefs.saveString(context, PREF_REGISTERED_DEVICE, deviceId)
        return true
    }

    private fun loadRegistrationMap(context: Context): JSONObject {
        val raw = Prefs.getString(context, PREF_REGISTRATION_CODES) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            for (b in digest) {
                append(((b.toInt() shr 4) and 0x0F).toString(16))
                append((b.toInt() and 0x0F).toString(16))
            }
        }
    }
}
