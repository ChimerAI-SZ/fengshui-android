package com.fengshui.app

import android.content.Context
import com.fengshui.app.utils.Prefs

object TrialManager {
    const val TRIAL_MAX_GROUPS = 2
    const val TRIAL_MAX_ORIGINS = 2
    const val TRIAL_MAX_DESTINATIONS = 5

    private const val PREF_REGISTERED_CODE = "fengshui_registered_code"

    @Deprecated("Use isRegistered(context) when context is available")
    fun isRegistered(): Boolean = false

    fun isRegistered(context: Context): Boolean {
        return Prefs.getString(context, PREF_REGISTERED_CODE) != null
    }

    /**
     * 简单的本地注册码校验接口。
     * 当前实现使用固定示例码 `TRIAL-UNLOCK-2026`。
     * 成功时保存本地注册标记并返回 true。
     */
    fun registerWithCode(context: Context, code: String): Boolean {
        val valid = code.trim() == "TRIAL-UNLOCK-2026"
        if (valid) {
            Prefs.saveString(context, PREF_REGISTERED_CODE, code.trim())
        }
        return valid
    }
}
