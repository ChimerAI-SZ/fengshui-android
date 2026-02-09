package com.fengshui.app.data

import android.content.Context
import com.fengshui.app.utils.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PointRepository(private val context: Context) {
    companion object {
        private const val KEY_PROJECTS = "fengshui_projects"
        private const val KEY_POINTS = "fengshui_points"
    }

    fun saveProjects(projects: List<Project>) {
        val arr = JSONArray()
        for (p in projects) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("description", p.description)
            o.put("createTime", p.createTime)
            o.put("updateTime", p.updateTime)
            arr.put(o)
        }
        Prefs.saveString(context, KEY_PROJECTS, arr.toString())
    }

    fun loadProjects(): List<Project> {
        val s = Prefs.getString(context, KEY_PROJECTS) ?: return emptyList()
        val arr = JSONArray(s)
        val res = mutableListOf<Project>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            res.add(
                Project(
                    o.getString("id"),
                    o.getString("name"),
                    if (o.has("description")) o.optString("description", null) else null,
                    o.optLong("createTime"),
                    o.optLong("updateTime")
                )
            )
        }
        return res
    }

    fun savePoints(points: List<FengShuiPoint>) {
        val arr = JSONArray()
        for (p in points) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("latitude", p.latitude)
            o.put("longitude", p.longitude)
            o.put("type", p.type.name)
            o.put("groupId", p.groupId)
            o.put("address", p.address)
            o.put("isGPSOrigin", p.isGPSOrigin)
            o.put("isVisible", p.isVisible)
            o.put("createTime", p.createTime)
            arr.put(o)
        }
        Prefs.saveString(context, KEY_POINTS, arr.toString())
    }

    fun loadPoints(): List<FengShuiPoint> {
        val s = Prefs.getString(context, KEY_POINTS) ?: return emptyList()
        val arr = JSONArray(s)
        val res = mutableListOf<FengShuiPoint>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val type = PointType.valueOf(o.getString("type"))
            res.add(
                FengShuiPoint(
                    o.getString("id"),
                    o.getString("name"),
                    o.getDouble("latitude"),
                    o.getDouble("longitude"),
                    type,
                    if (o.has("groupId")) o.optString("groupId", null) else null,
                    if (o.has("address")) o.optString("address", null) else null,
                    o.optBoolean("isGPSOrigin", false),
                    o.optBoolean("isVisible", true),
                    o.optLong("createTime")
                )
            )
        }
        return res
    }

    fun createProject(name: String, description: String? = null): Project {
        val p = Project(UUID.randomUUID().toString(), name, description)
        val list = loadProjects().toMutableList()
        list.add(p)
        saveProjects(list)
        return p
    }

    fun createPoint(
        name: String,
        lat: Double,
        lon: Double,
        type: PointType,
        groupId: String? = null,
        address: String? = null,
        isGPSOrigin: Boolean = false
    ): FengShuiPoint {
        // Trial limit checks
        if (!com.fengshui.app.TrialManager.isRegistered(context)) {
            val existing = loadPoints()
            when (type) {
                PointType.ORIGIN -> {
                    val origins = existing.filter { it.type == PointType.ORIGIN && !it.isGPSOrigin }
                    if (origins.size >= com.fengshui.app.TrialManager.TRIAL_MAX_ORIGINS) {
                        throw com.fengshui.app.TrialLimitException("试用版最多创建 ${com.fengshui.app.TrialManager.TRIAL_MAX_ORIGINS} 个原点。", com.fengshui.app.TrialLimitException.LimitType.ORIGIN)
                    }
                }
                PointType.DESTINATION -> {
                    val dests = existing.filter { it.type == PointType.DESTINATION }
                    if (dests.size >= com.fengshui.app.TrialManager.TRIAL_MAX_DESTINATIONS) {
                        throw com.fengshui.app.TrialLimitException("试用版最多创建 ${com.fengshui.app.TrialManager.TRIAL_MAX_DESTINATIONS} 个终点。", com.fengshui.app.TrialLimitException.LimitType.DESTINATION)
                    }
                }
            }
        }

        val p = FengShuiPoint(
            UUID.randomUUID().toString(),
            name,
            lat,
            lon,
            type,
            groupId,
            address,
            isGPSOrigin
        )
        val list = loadPoints().toMutableList()
        list.add(p)
        savePoints(list)
        return p
    }

    fun updatePoint(point: FengShuiPoint) {
        val list = loadPoints().toMutableList()
        val index = list.indexOfFirst { it.id == point.id }
        if (index >= 0) {
            list[index] = point
            savePoints(list)
        }
    }

    fun deletePoint(pointId: String) {
        val list = loadPoints().toMutableList()
        list.removeAll { it.id == pointId }
        savePoints(list)
    }

    fun getPointById(pointId: String): FengShuiPoint? {
        return loadPoints().find { it.id == pointId }
    }

    // Phase 3: 按案例获取点位
    fun getPointsByCase(caseId: String): List<FengShuiPoint> {
        return loadPoints().filter { it.groupId == caseId }
    }

    fun getPointsByCaseAndType(caseId: String, type: PointType): List<FengShuiPoint> {
        return loadPoints().filter { it.groupId == caseId && it.type == type }
    }

    fun deletePointsByCase(caseId: String) {
        val list = loadPoints().toMutableList()
        list.removeAll { it.groupId == caseId }
        savePoints(list)
    }

    // Phase 3: 案例管理方法
    fun updateProject(project: Project) {
        val list = loadProjects().toMutableList()
        val index = list.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            list[index] = project
            saveProjects(list)
        }
    }

    fun deleteProject(projectId: String) {
        val list = loadProjects().toMutableList()
        list.removeAll { it.id == projectId }
        saveProjects(list)
        // 级联删除该案例下的所有点位
        deletePointsByCase(projectId)
    }
}
