package com.fengshui.app.data

// 24-shan helpers and mappings.
object ShanUtils {
    val SHAN_NAMES = arrayOf(
        "zi", "gui", "chou", "gen", "yin", "jia",
        "mao", "yi", "chen", "xun", "si", "bing",
        "wu", "ding", "wei", "kun", "shen", "geng",
        "you", "xin", "xu", "qian", "hai", "ren"
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

enum class BaGua(val startAngle: Float) {
    KAN(337.5f),
    GEN(22.5f),
    ZHEN(67.5f),
    XUN(112.5f),
    LI(157.5f),
    KUN(202.5f),
    DUI(247.5f),
    QIAN(292.5f)
}

enum class WuXing(val color: Int) {
    JIN(0xFFFFD700.toInt()),
    MU(0xFF228B22.toInt()),
    SHUI(0xFF1E90FF.toInt()),
    HUO(0xFFFF4500.toInt()),
    TU(0xFFDEB887.toInt())
}

data class ShanInfo(
    val name: String,
    val wuXing: WuXing,
    val baGua: BaGua,
    val degree: Float,
    val index: Int
)
