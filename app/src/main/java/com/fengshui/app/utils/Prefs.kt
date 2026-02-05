package com.fengshui.app.utils

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREFS_NAME = "fengshui_prefs"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String): String? =
        prefs(context).getString(key, null)
}
