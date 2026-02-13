package com.fengshui.app.data

// 24-shan helpers and mappings.
object ShanUtils {
    val SHAN_NAMES = arrayOf(
        "子", "癸", "丑", "艮", "寅", "甲",
        "卯", "乙", "辰", "巽", "巳", "丙",
        "午", "丁", "未", "坤", "申", "庚",
        "酉", "辛", "戌", "乾", "亥", "壬"
    )

    const val SHAN_ANGLE = 15f

    fun getShanIndex(angle: Float): Int {
        val normalizedAngle = ((angle % 360) + 360) % 360
        return ((normalizedAngle + 7.5f) / SHAN_ANGLE).toInt() % 24
    }

    fun getShanInfo(angle: Float): ShanInfo {
        val index = getShanIndex(angle)
        val name = SHAN_NAMES[index]
        val baGua = getBaGuaByIndex(index)
        val wuXing = getWuXingByIndex(index)
        val degree = (index * SHAN_ANGLE) % 360f
        return ShanInfo(
            name = name,
            wuXing = wuXing,
            baGua = baGua,
            degree = degree,
            index = index
        )
    }

    fun getBaGuaByIndex(index: Int): BaGua {
        return when (index / 3) {
            0 -> BaGua.KAN
            1 -> BaGua.GEN
            2 -> BaGua.ZHEN
            3 -> BaGua.XUN
            4 -> BaGua.LI
            5 -> BaGua.KUN
            6 -> BaGua.DUI
            else -> BaGua.QIAN
        }
    }

    fun getWuXingByIndex(index: Int): WuXing {
        return when (index) {
            in 0..2, in 18..20 -> WuXing.SHUI
            in 3..5, in 15..17 -> WuXing.MU
            in 6..8, in 12..14 -> WuXing.HUO
            in 9..11 -> WuXing.TU
            else -> WuXing.JIN
        }
    }

    fun getOppositeShanIndex(shanIndex: Int): Int = (shanIndex + 12) % 24

    fun getOppositeShanIndex(angle: Float): Int = getOppositeShanIndex(getShanIndex(angle))
}

enum class BaGua(val label: String, val startAngle: Float) {
    KAN("坎", 337.5f),
    GEN("艮", 22.5f),
    ZHEN("震", 67.5f),
    XUN("巽", 112.5f),
    LI("离", 157.5f),
    KUN("坤", 202.5f),
    DUI("兑", 247.5f),
    QIAN("乾", 292.5f)
}

enum class WuXing(val label: String, val color: Int) {
    JIN("金", 0xFFFFD700.toInt()),
    MU("木", 0xFF228B22.toInt()),
    SHUI("水", 0xFF1E90FF.toInt()),
    HUO("火", 0xFFFF4500.toInt()),
    TU("土", 0xFFDEB887.toInt())
}

data class ShanInfo(
    val name: String,
    val wuXing: WuXing,
    val baGua: BaGua,
    val degree: Float,
    val index: Int
)
