package com.fengshui.app.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.fengshui.app.R
import java.util.Locale

object AppLanguageManager {
    private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"

    data class LanguageOption(
        val tag: String,
        @StringRes val labelRes: Int
    )

    val languageOptions: List<LanguageOption> = listOf(
        LanguageOption(tag = "zh-CN", labelRes = R.string.language_option_zh_cn),
        LanguageOption(tag = "zh-TW", labelRes = R.string.language_option_zh_tw),
        LanguageOption(tag = "en", labelRes = R.string.language_option_en)
    )

    fun applySavedLanguageOrSystem(context: Context) {
        val savedTag = getSavedLanguageTag(context).orEmpty()
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (savedTag.isBlank()) {
            if (currentTags.isNotBlank()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
            return
        }
        if (!savedTag.equals(currentTags, ignoreCase = true)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedTag))
        }
    }

    fun updateLanguage(context: Context, tag: String) {
        Prefs.saveString(context, KEY_APP_LANGUAGE_TAG, tag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun getSavedLanguageTag(context: Context): String? =
        Prefs.getString(context, KEY_APP_LANGUAGE_TAG)?.takeIf { it.isNotBlank() }

    fun getCurrentLanguageTag(context: Context): String {
        val locale = currentLocale(context)
        if (locale.language.equals("zh", ignoreCase = true)) {
            return if (isTraditionalChineseRegion(locale)) "zh-TW" else "zh-CN"
        }
        return "en"
    }

    fun isChineseLanguage(context: Context): Boolean =
        currentLocale(context).language.startsWith("zh", ignoreCase = true)

    fun isChineseLanguage(): Boolean =
        currentLocale(null).language.startsWith("zh", ignoreCase = true)

    fun amapLanguageCode(): String = if (isChineseLanguage()) "zh_cn" else "en"

    fun googleLanguageCode(): String =
        when (getCurrentLanguageTag()) {
            "zh-TW" -> "zh-TW"
            "zh-CN" -> "zh-CN"
            else -> "en"
        }

    fun googleLanguageCode(context: Context): String =
        when (getCurrentLanguageTag(context)) {
            "zh-TW" -> "zh-TW"
            "zh-CN" -> "zh-CN"
            else -> "en"
        }

    fun nominatimAcceptLanguage(): String =
        when (getCurrentLanguageTag()) {
            "zh-TW" -> "zh-TW,zh-CN,en"
            "zh-CN" -> "zh-CN,zh-TW,en"
            else -> "en,zh-CN,zh-TW"
        }

    fun nominatimAcceptLanguage(context: Context): String =
        when (getCurrentLanguageTag(context)) {
            "zh-TW" -> "zh-TW,zh-CN,en"
            "zh-CN" -> "zh-CN,zh-TW,en"
            else -> "en,zh-CN,zh-TW"
        }

    fun getCurrentLanguageTag(): String {
        val locale = currentLocale(null)
        if (locale.language.equals("zh", ignoreCase = true)) {
            return if (isTraditionalChineseRegion(locale)) "zh-TW" else "zh-CN"
        }
        return "en"
    }

    private fun currentLocale(context: Context?): Locale {
        val appLocale = AppCompatDelegate.getApplicationLocales()[0]
        if (appLocale != null) return appLocale
        if (context != null) {
            return context.resources.configuration.locales[0] ?: Locale.getDefault()
        }
        return Locale.getDefault()
    }

    private fun isTraditionalChineseRegion(locale: Locale): Boolean {
        val region = locale.country.uppercase(Locale.ROOT)
        return region == "TW" || region == "HK" || region == "MO"
    }
}
