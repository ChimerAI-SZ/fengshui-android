package com.fengshui.app.utils

import android.content.Context
import com.fengshui.app.R
import com.fengshui.app.data.BaGua
import com.fengshui.app.data.WuXing

object ShanTextResolver {
    fun shanName(context: Context, shanIndex: Int): String {
        val labels = context.resources.getStringArray(R.array.compass_shan_names)
        if (labels.isEmpty()) return ""
        val normalized = ((shanIndex % labels.size) + labels.size) % labels.size
        return labels[normalized]
    }

    fun baguaName(context: Context, bagua: BaGua): String {
        return context.getString(
            when (bagua) {
                BaGua.KAN -> R.string.bagua_kan
                BaGua.GEN -> R.string.bagua_gen
                BaGua.ZHEN -> R.string.bagua_zhen
                BaGua.XUN -> R.string.bagua_xun
                BaGua.LI -> R.string.bagua_li
                BaGua.KUN -> R.string.bagua_kun
                BaGua.DUI -> R.string.bagua_dui
                BaGua.QIAN -> R.string.bagua_qian
            }
        )
    }

    fun wuxingName(context: Context, wuxing: WuXing): String {
        return context.getString(
            when (wuxing) {
                WuXing.JIN -> R.string.wuxing_jin
                WuXing.MU -> R.string.wuxing_mu
                WuXing.SHUI -> R.string.wuxing_shui
                WuXing.HUO -> R.string.wuxing_huo
                WuXing.TU -> R.string.wuxing_tu
            }
        )
    }
}
