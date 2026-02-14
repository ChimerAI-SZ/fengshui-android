package com.fengshui.app.map.poi

object PoiTypeMapper {
    private fun norm(keyword: String): String = keyword.trim().lowercase()

    private fun category(keyword: String): String? {
        val k = norm(keyword)
        return when {
            k.contains("医院") || k.contains("hospital") -> "hospital"
            k.contains("住宅") || k.contains("小区") || k.contains("residence") || k.contains("apartment") -> "residence"
            k.contains("大厦") || k.contains("写字楼") || k.contains("building") || k.contains("office") -> "building"
            else -> null
        }
    }

    fun toGoogleType(keyword: String): String? {
        return when (category(keyword)) {
            "hospital" -> "hospital"
            "residence" -> "apartment"
            "building" -> "premise"
            else -> null
        }
    }

    fun toAmapTypeCode(keyword: String): String? {
        return when (category(keyword)) {
            "hospital" -> "090100"
            // 商务住宅相关 + 住宅小区 + 楼宇相关（提高命中率）
            "residence" -> "120000|120300|120301|120302|120303"
            "building" -> "120200|120201|120202"
            else -> null
        }
    }

    fun isTypedCategoryKeyword(keyword: String): Boolean {
        return toGoogleType(keyword) != null || toAmapTypeCode(keyword) != null
    }

    fun fallbackQueries(keyword: String): List<String> {
        val k = keyword.trim()
        return when (category(keyword)) {
            "residence" -> listOf(
                k, "住宅", "小区", "社区", "公寓", "residential", "apartment", "housing", "condominium"
            )
            "hospital" -> listOf(
                k, "医院", "诊所", "medical center", "hospital", "clinic"
            )
            "building" -> listOf(
                k, "大厦", "写字楼", "楼宇", "office", "office building", "tower", "building"
            )
            else -> listOf(k)
        }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    fun matchesAmapTypeLabel(keyword: String, amapTypeLabel: String?): Boolean {
        val label = amapTypeLabel.orEmpty().lowercase()
        val k = norm(keyword)
        return when {
            k.contains("医院") || k.contains("hospital") ->
                label.contains("医院") || label.contains("医疗") || label.contains("诊所") || label.contains("health")
            k.contains("住宅") || k.contains("小区") || k.contains("residence") || k.contains("apartment") ->
                label.contains("住宅") || label.contains("小区") || label.contains("商务住宅") || label.contains("公寓") || label.contains("社区")
            k.contains("大厦") || k.contains("写字楼") || k.contains("building") || k.contains("office") ->
                label.contains("楼宇") || label.contains("写字楼") || label.contains("商务") || label.contains("大厦") || label.contains("办公")
            else -> false
        }
    }

    fun matchesGoogleTypes(keyword: String, googleTypes: List<String>?): Boolean {
        val types = googleTypes?.map { it.lowercase() } ?: emptyList()
        val k = norm(keyword)
        return when {
            k.contains("医院") || k.contains("hospital") ->
                types.any { it == "hospital" || it == "health" || it == "doctor" }
            k.contains("住宅") || k.contains("小区") || k.contains("residence") || k.contains("apartment") ->
                types.any {
                    it == "apartment" || it == "premise" || it == "neighborhood" || it == "sublocality" ||
                        it == "lodging" || it == "real_estate_agency" || it == "point_of_interest"
                }
            k.contains("大厦") || k.contains("写字楼") || k.contains("building") || k.contains("office") ->
                types.any { it == "premise" || it == "point_of_interest" || it == "establishment" || it == "office" }
            else -> false
        }
    }

    fun matchesTextByCategory(keyword: String, name: String?, address: String?): Boolean {
        val text = "${name.orEmpty()} ${address.orEmpty()}".lowercase()
        val k = norm(keyword)
        return when {
            k.contains("医院") || k.contains("hospital") ->
                listOf("医院", "医疗", "诊所", "hospital", "clinic", "medical").any { text.contains(it) }
            k.contains("住宅") || k.contains("小区") || k.contains("residence") || k.contains("apartment") ->
                listOf("住宅", "小区", "社区", "公寓", "residential", "residence", "apartment", "housing", "estate", "condo").any { text.contains(it) }
            k.contains("大厦") || k.contains("写字楼") || k.contains("building") || k.contains("office") ->
                listOf("大厦", "写字楼", "办公", "楼", "building", "office", "tower").any { text.contains(it) }
            else -> false
        }
    }
}
