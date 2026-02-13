# 风水罗盘App 需求与架构文档

**目标读者**：两位即将重建iOS+Android双平台应用的初级全栈开发者
**文档性质**：仅说明当前版本的设计决策和技术实现，不做建议和决策
**版本日期**：2026-02-04

---

## 目录

1. [核心算法模块](#模块1核心算法)
2. [24山方位系统](#模块224山方位系统)
3. [坐标系统与地图抽象层](#模块3坐标系统与地图抽象层)
4. [交互细节与性能优化](#模块4交互细节与性能优化)
5. [搜索与十字准星模式](#模块5搜索与十字准星模式)
6. [扇形区域POI搜索](#模块6扇形区域poi搜索)
7. [项目/分组管理](#模块7项目分组管理)
8. [数据层架构](#模块8数据层架构)
9. [生活圈模式](#模块9生活圈模式)
10. [认证系统](#模块10认证系统)
11. [云端同步机制](#模块11云端同步机制)
12. [性能优化与边界处理](#模块12性能优化与边界处理)
13. [UI组件与对话框系统](#模块13ui组件与对话框系统)
14. [功能开关与试用限制](#模块14功能开关与试用限制)
15. [文件结构概览](#模块15文件结构概览)
16. [重建注意事项清单](#模块16重建注意事项清单)

---

## 模块1：核心算法

### 1.1 Rhumb Line（恒向线）算法

**为什么必须使用Rhumb Line而非Geodesic？**

传统风水罗盘基于平面几何和Mercator投影原理，Rhumb Line保证了关键的角度对称性：

```
✅ 角度完美对称：bearing_AB + bearing_BA = 360°
✅ 24山方位正对：原点终点互换后，山位索引差正好12
✅ 四正方向正交：子午卯酉（正北、正南、正东、正西）成90度直角
✅ 连线是直线：在Mercator地图上与罗盘刻度线完美对齐
```

**核心算法公式**：

```kotlin
// Mercator投影的纬度变换
val dPhi = ln(tan(lat2 / 2 + PI / 4) / tan(lat1 / 2 + PI / 4))

// 处理跨180度经线
val adjustedDLon = when {
    dLon > PI -> dLon - 2 * PI
    dLon < -PI -> dLon + 2 * PI
    else -> dLon
}

// 方位角计算
val bearing = atan2(adjustedDLon, dPhi)
return ((Math.toDegrees(bearing) + 360) % 360).toFloat()
```

**代码位置**：`utils/RhumbLineUtils.kt`（340行）

**关键方法**：
- `calculateRhumbBearing(origin, destination): Float` - 计算方位角
- `calculateRhumbDestination(start, bearing, distance): UniversalLatLng` - 根据方位角计算终点
- `calculateRhumbDistance(origin, destination): Float` - 计算恒向线距离
- `verifySymmetry(pointA, pointB): Boolean` - 验证对称性
- `getReverseBearing(bearing): Float` - 获取反向方位角（+180°）
- `getOppositeShanIndex(shanIndex): Int` - 获取对面山位索引（+12）

### 1.2 与Geodesic的区别

| 特性 | Rhumb Line | Geodesic |
|------|------------|----------|
| 方位角 | 恒定不变 | 沿途变化 |
| 路径 | 在Mercator上是直线 | 最短路径（大圆弧） |
| 距离 | 稍长 | 最短 |
| 对称性 | 完美对称 | 不对称 |
| 适用场景 | 风水罗盘、传统航海 | 航空导航 |

**距离差异参考**：
- 1-10km：差异<0.01%（可忽略）
- 100km：差异约0.1-0.5%
- 1000km：差异约0.5-2%

---

## 模块2：24山方位系统

### 2.1 24山基础数据

```kotlin
// 24山名称数组（从正北0°开始，顺时针排列）
val SHAN_NAMES = arrayOf(
    "子", "癸", "丑", "艮", "寅", "甲",  // 北→东北
    "卯", "乙", "辰", "巽", "巳", "丙",  // 东→东南
    "午", "丁", "未", "坤", "申", "庚",  // 南→西南
    "酉", "辛", "戌", "乾", "亥", "壬"   // 西→西北
)

// 每山覆盖角度
val SHAN_ANGLE = 15f  // 360° / 24 = 15°

// 24山索引计算公式（关键！）
fun getShanIndex(angle: Float): Int {
    val normalizedAngle = ((angle % 360) + 360) % 360
    return ((normalizedAngle + 7.5f) / 15f).toInt() % 24
}
```

**公式解释**：
- `+ 7.5f`：偏移半个山位，使[352.5°, 7.5°)映射到子山(index=0)
- `/ 15f`：每山15度
- `% 24`：循环索引

### 2.2 八卦与五行映射

```kotlin
// 八卦方位（每卦覆盖3山=45°）
enum class BaGua(val label: String, val startAngle: Float) {
    KAN("坎", 337.5f),   // 子癸丑
    GEN("艮", 22.5f),    // 艮寅甲
    ZHEN("震", 67.5f),   // 卯乙辰
    XUN("巽", 112.5f),   // 巽巳丙
    LI("离", 157.5f),    // 午丁未
    KUN("坤", 202.5f),   // 坤申庚
    DUI("兑", 247.5f),   // 酉辛戌
    QIAN("乾", 292.5f)   // 乾亥壬
}

// 五行属性
enum class WuXing(val label: String, val color: Int) {
    JIN("金", 0xFFFFD700),   // 金色
    MU("木", 0xFF228B22),    // 绿色
    SHUI("水", 0xFF1E90FF),  // 蓝色
    HUO("火", 0xFFFF4500),   // 红色
    TU("土", 0xFFDEB887)     // 棕色
}
```

### 2.3 ShanInfo数据结构

```kotlin
data class ShanInfo(
    val name: String,      // 山名（如"子"）
    val wuXing: WuXing,    // 五行属性
    val baGua: BaGua,      // 所属八卦
    val degree: Float,     // 山的中心角度
    val index: Int         // 山的索引(0-23)
)
```

**代码位置**：`data/ShanUtils.kt`

---

## 模块3：坐标系统与地图抽象层

### 3.1 坐标系统

**当前使用的坐标系**：
- **中国区**：GCJ-02（火星坐标，高德地图）
- **海外区**：WGS-84（GPS标准，谷歌地图）

**重要警告**：
- GCJ-02与WGS-84存在50-500米偏移
- 当前代码中`toGoogleLatLng()`未做转换，直接使用会导致偏移
- 未来双平台需要实现`CoordinateConverter`

```kotlin
// 坐标转换接口（当前未完全实现）
object CoordinateConverter {
    fun gcj02ToWgs84(lat: Double, lng: Double): Pair<Double, Double>
    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double>
}
```

### 3.2 地图抽象层架构

```
┌─────────────────────────────────────────────────┐
│                  MapProvider                     │
│              (统一接口，399行)                    │
├─────────────────────────────────────────────────┤
│  核心方法:                                       │
│  - addMarker(position): UniversalMarker         │
│  - addPolyline(start, end): UniversalPolyline   │
│  - animateCameraWithPriority(target, priority)  │
│  - animateCameraToBounds(bounds, padding)       │
│  - screenLocationToLatLng(x, y): UniversalLatLng│
│  - onCameraChangeFinish(callback)               │
└─────────────────────────────────────────────────┘
           ↙                      ↘
┌──────────────────┐      ┌──────────────────┐
│  AMapProvider    │      │ GoogleMapProvider│
│  (高德实现)       │      │  (谷歌实现)       │
└──────────────────┘      └──────────────────┘
```

**统一数据类型**：
- `UniversalLatLng` - 统一经纬度
- `UniversalMarker` - 统一标记
- `UniversalPolyline` - 统一折线
- `UniversalLatLngBounds` - 统一边界

### 3.3 SDK自动切换逻辑

```kotlin
// 区域检测规则
fun detectRecommendedSDK(latitude: Double, longitude: Double): MapProviderType {
    return if (isInChina(latitude, longitude)) {
        MapProviderType.AMAP
    } else {
        MapProviderType.GOOGLE
    }
}

// 中国区域判断（简化版）
fun isInChina(lat: Double, lng: Double): Boolean {
    return lat in 3.86..53.55 && lng in 73.66..135.05
}
```

**代码位置**：
- `map/abstraction/MapProvider.kt`
- `map/abstraction/amap/AMapProvider.kt`
- `map/abstraction/googlemaps/GoogleMapProvider.kt`

---

## 模块4：交互细节与性能优化

### 4.1 相机优先级系统

```kotlin
enum class CameraMoveSource(val priority: Int) {
    GPS_AUTO_LOCATE(1),      // 最低：自动GPS定位
    MAP_INIT(2),             // 地图初始化
    USER_POINT_SELECT(3),    // 用户选择点位
    SEARCH_RESULT(4),        // 搜索结果（高优先级）
    USER_MANUAL(5)           // 用户手动拖动（最高）
}
```

**工作原理**：
1. 每次相机移动记录`cameraMoveSource`和`cameraMoveTimestamp`
2. 低优先级操作（如GPS）检查当前优先级
3. 如果当前优先级更高且未超时（3秒），则忽略低优先级操作
4. 超时后允许低优先级操作执行

**解决的问题**：搜索结果跳转后，GPS定位返回不会覆盖搜索位置

### 4.2 连线点击检测

```kotlin
// 常量定义
const val POLYLINE_CLICK_THRESHOLD = 60f  // 像素，约为线宽的5倍
const val POLYLINE_WIDTH = 12f            // 线宽

// 点到线段距离算法（GeometryUtils.kt）
fun pointToLineSegmentDistance(
    point: PointF,
    lineStart: PointF,
    lineEnd: PointF
): Float {
    // 计算投影点，判断是否在线段内
    // 返回点到线段的最短距离
}
```

**为什么需要自定义点击检测？**
- 高德/谷歌SDK的Polyline点击回调不稳定
- 线太细难以点击，需要扩大热区
- 需要支持多条重叠线的优先级判断

### 4.3 文字标签碰撞检测

```kotlin
// 8个锚点位置（按优先级排序）
val ANCHOR_POSITIONS = listOf(
    0.5f to 1.0f,   // 底部中心（默认）
    0.5f to 0.0f,   // 顶部中心
    1.0f to 0.5f,   // 右侧中心
    0.0f to 0.5f,   // 左侧中心
    1.0f to 1.0f,   // 右下角
    0.0f to 1.0f,   // 左下角
    1.0f to 0.0f,   // 右上角
    0.0f to 0.0f    // 左上角
)

// 碰撞检测逻辑
fun findNonCollidingAnchor(
    position: UniversalLatLng,
    existingMarkers: List<Rect>
): Pair<Float, Float> {
    for (anchor in ANCHOR_POSITIONS) {
        val rect = calculateMarkerRect(position, anchor)
        if (!existingMarkers.any { it.intersects(rect) }) {
            return anchor
        }
    }
    return ANCHOR_POSITIONS.first()  // 全部冲突则使用默认
}
```

**代码位置**：`TextMarkerManager.kt`（639行）

### 4.4 重复终点检测

```kotlin
const val DUPLICATE_THRESHOLD_METERS = 300f  // 300米内视为重复

// 检测逻辑
fun isDuplicateDestination(newPoint: UniversalLatLng, existingPoints: List<UniversalLatLng>): Boolean {
    return existingPoints.any { existing ->
        newPoint.distanceTo(existing) < DUPLICATE_THRESHOLD_METERS
    }
}
```

### 4.5 数量限制常量

| 限制项 | 数值 | 原因 |
|--------|------|------|
| MAX_VISIBLE_POLYLINES | 50条 | 防止华为设备内存泄漏 |
| MAX_VISIBLE_TEXT_MARKERS | 50个 | 终点标签性能限制 |
| MAX_POI_COUNT | 50个 | 扇形搜索POI数量限制 |
| MAX_LATITUDE | 85.05° | Web Mercator极地限制 |

---

## 模块5：搜索与十字准星模式

### 5.1 完整流程图

```
┌──────────────────────────────────────────────────────────────┐
│                  搜索 → 十字准星 → 确认 完整流程              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐                                        │
│  │   SearchScreen   │  POI搜索页面                            │
│  │──────────────────│                                        │
│  │ 1. 输入关键词    │                                        │
│  │ 2. 实时建议(300ms防抖)                                     │
│  │ 3. 点击执行POI搜索                                         │
│  │ 4. 显示搜索结果列表                                        │
│  └────────┬─────────┘                                        │
│           │ 用户点击结果                                      │
│           ▼                                                  │
│  ┌──────────────────┐                                        │
│  │SearchResultManager│  单例对象，跨页面传递数据               │
│  │──────────────────│                                        │
│  │ setPendingResult() │                                      │
│  │ - latitude       │                                        │
│  │ - longitude      │                                        │
│  │ - name           │                                        │
│  │ - address        │                                        │
│  └────────┬─────────┘                                        │
│           │ navigate("map")                                  │
│           ▼                                                  │
│  ┌──────────────────────────────────────────────────────┐    │
│  │                    MapScreen                          │    │
│  │──────────────────────────────────────────────────────│    │
│  │ SearchResultProcessingEffect 检测到 pendingResult     │    │
│  │      │                                                │    │
│  │      ▼                                                │    │
│  │ ┌────────────────────────────────────────────────┐   │    │
│  │ │  进入十字准星模式 (crosshairMode = true)        │   │    │
│  │ │  - 相机移动到搜索位置（优先级=SEARCH_RESULT）    │   │    │
│  │ │  - 显示半透明覆盖层                             │   │    │
│  │ │  - 中心显示红色十字准星                         │   │    │
│  │ │  - 地址/名称预览卡片                            │   │    │
│  │ └───────────────────────┬────────────────────────┘   │    │
│  │                         │                             │    │
│  │                         ▼ 用户可拖拽地图微调位置       │    │
│  │                                                       │    │
│  │ ┌────────────────────────────────────────────────┐   │    │
│  │ │  CrosshairModeUI 操作面板                       │   │    │
│  │ │  ┌─────────────────────────────────────────┐   │   │    │
│  │ │  │ [选择客户▼]  项目/分组选择               │   │   │    │
│  │ │  │  └─ 新建客户（内嵌创建项目对话框）        │   │   │    │
│  │ │  │  └─ 已有项目列表                         │   │   │    │
│  │ │  ├─────────────────────────────────────────┤   │   │    │
│  │ │  │ [🏠 原点]  保存为原点                    │   │   │    │
│  │ │  │ [⭐ 终点]  保存为终点                    │   │   │    │
│  │ │  │ [✕ 取消]  退出十字准星模式               │   │   │    │
│  │ │  └─────────────────────────────────────────┘   │   │    │
│  │ └────────────────────────────────────────────────┘   │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 CrosshairModeUI的4种显示模式

```kotlin
// 根据不同场景显示不同按钮
when {
    isLifeCircleSelection -> {
        // 生活圈选择模式：只显示"选择此位置"按钮
        showLifeCircleConfirmButton()
    }
    tempViewMode -> {
        // 临时查看模式：显示定位按钮
        showLocateButton()
    }
    else -> {
        // 正常模式：显示原点/终点/取消按钮
        showOriginDestinationButtons()
    }
}
```

### 5.3 十字准星位置同步

```kotlin
// 地图拖拽时实时更新十字准星位置
onCameraChangeFinish { cameraPosition ->
    if (crosshairMode) {
        val centerLatLng = mapProvider.screenLocationToLatLng(
            screenWidth / 2f,
            screenHeight / 2f
        )
        updateCrosshairState { copy(crosshairLocation = centerLatLng) }
    }
}
```

**代码位置**：
- `SearchScreen.kt`（557行）
- `SearchResultManager.kt`
- `map/ui/CrosshairModeUI.kt`（426行）
- `map/effects/SearchResultEffects.kt`

---

## 模块6：扇形区域POI搜索

### 6.1 扇形区域配置

```
触发条件：原点存在 + 用户开启扇形搜索

配置参数：
- 关键词（如"酒店"、"餐厅"）
- 24山选择 或 八方位选择
- 搜索距离（100m ~ 250km）
- 当前角度范围：15°（24山）或 45°（八方位）
```

### 6.2 扇形过滤算法

```kotlin
fun filterPOIsInSector(
    origin: UniversalLatLng,
    pois: List<POIPoint>,
    startAngle: Float,    // 扇形起始角度
    endAngle: Float,      // 扇形结束角度
    maxDistance: Float    // 最大距离（米）
): List<POIPoint> {
    return pois.filter { poi ->
        // 1. 计算原点到POI的方位角（使用Rhumb Line）
        val bearing = RhumbLineUtils.calculateRhumbBearing(origin, poi.position)

        // 2. 判断方位角是否在扇形范围内
        val inAngleRange = isAngleInRange(bearing, startAngle, endAngle)

        // 3. 判断距离是否在范围内
        val distance = origin.distanceTo(poi.position)
        val inDistanceRange = distance <= maxDistance

        inAngleRange && inDistanceRange
    }
}

// 处理跨0度的角度范围判断
fun isAngleInRange(angle: Float, start: Float, end: Float): Boolean {
    return if (start <= end) {
        angle in start..end
    } else {
        // 跨越0度，如 [350°, 10°]
        angle >= start || angle <= end
    }
}
```

### 6.3 八方位到24山映射

```kotlin
// 八方位选择时，自动扩展为对应的3个24山
val BA_GUA_TO_SHAN_MAP = mapOf(
    "坎" to listOf("子", "癸", "丑"),   // 北方
    "艮" to listOf("艮", "寅", "甲"),   // 东北
    "震" to listOf("卯", "乙", "辰"),   // 东方
    "巽" to listOf("巽", "巳", "丙"),   // 东南
    "离" to listOf("午", "丁", "未"),   // 南方
    "坤" to listOf("坤", "申", "庚"),   // 西南
    "兑" to listOf("酉", "辛", "戌"),   // 西方
    "乾" to listOf("乾", "亥", "壬")    // 西北
)
```

**代码位置**：
- `map/dialog/SectorConfigDialog.kt`
- `SectorAreaManager.kt`
- `SectorPOISearchManager.kt`

---

## 模块7：项目/分组管理

### 7.1 数据组织层级

```
Project（项目/客户）
   │
   ├── FengShuiPoint (ORIGIN) 原点1
   │      ├── isActive = true（当前活动原点）
   │      └── isGPSOrigin = false
   │
   ├── FengShuiPoint (ORIGIN) 原点2
   │      └── isActive = false
   │
   ├── FengShuiPoint (DESTINATION) 终点1
   │      └── isVisible = true（显示在罗盘上）
   │
   ├── FengShuiPoint (DESTINATION) 终点2
   │      └── isVisible = false（隐藏）
   │
   └── ...更多点位
```

### 7.2 特殊点位处理

**GPS原点**：
```kotlin
const val GPS_ORIGIN_ID = "gps_location_origin"

// 特性：
// - 系统自动创建，不可删除/重命名
// - 坐标随GPS定位更新
// - 不占用用户原点配额（试用限制不计算）
// - 每个项目可以有一个GPS原点
```

### 7.3 数据模型

```kotlin
data class Project(
    val id: String,
    val name: String,
    val description: String? = null,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)

data class FengShuiPoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: PointType,              // ORIGIN / DESTINATION
    val groupId: String? = null,      // 所属项目ID
    val groupName: String? = null,    // 冗余：项目名称
    val address: String? = null,      // 地址
    val isActive: Boolean = false,    // 是否为当前活动原点
    val isVisible: Boolean = true,    // 是否显示在地图上
    val isGPSOrigin: Boolean = false, // 是否为GPS原点
    val createTime: Long = System.currentTimeMillis()
)

enum class PointType {
    ORIGIN,      // 原点
    DESTINATION  // 终点
}
```

---

## 模块8：数据层架构

### 8.1 存储架构（三层）

```
┌─────────────────────────────────────────────────────────────┐
│                      数据存储架构                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  层1: SharedPreferences (主存储，必需)               │   │
│  │  - JSON格式存储                                      │   │
│  │  - 同步写入，立即生效                                │   │
│  │  - Keys: fengshui_projects, fengshui_points, etc.   │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                 │
│                           ▼ 双写（异步，可选）              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  层2: Room Database (可选，FeatureFlag控制)          │   │
│  │  - 结构化存储                                        │   │
│  │  - 支持复杂查询                                      │   │
│  │  - 表: fengshui_points, point_groups, user_profiles │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                 │
│                           ▼ 同步（后台，可选）              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  层3: Supabase Cloud (可选，FeatureFlag控制)         │   │
│  │  - PostgreSQL数据库                                  │   │
│  │  - Row Level Security (RLS)                         │   │
│  │  - 每15分钟后台同步                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 双写模式实现

```kotlin
// PointRepository.kt 中的双写逻辑
fun createPoint(...): FengShuiPoint {
    // 1. 主写入：SharedPreferences（同步，必须成功）
    saveToSharedPreferences(point)

    // 2. 副写入：Room（异步，失败静默）
    if (FeatureFlags.ENABLE_ROOM_STORAGE) {
        repositoryScope.launch {
            try {
                val entity = point.toEntity(userId, isDirty = true)
                pointDao.insert(entity)
            } catch (e: Exception) {
                e.printStackTrace()  // 静默失败，不影响主流程
            }
        }
    }

    return point
}
```

### 8.3 SharedPreferences存储格式

```kotlin
// Keys
"fengshui_projects"     → JSON Array of Project
"fengshui_points"       → JSON Array of FengShuiPoint
"gps_origin_latitude"   → Double
"gps_origin_longitude"  → Double
"registration_codes"    → JSON Object（设备绑定信息）
"trial_prefs"           → Boolean（是否已注册）
"life_circle_data_${projectId}" → JSON Object
"life_circle_temp_progress"     → JSON Object
```

**代码位置**：`data/PointRepository.kt`（553行）

---

## 模块9：生活圈模式

### 9.1 数据模型

```kotlin
data class LifeCircleData(
    val projectId: String,
    val homePoint: FengShuiPoint,           // 家（原点1，罗盘1000px）
    val workPoint: FengShuiPoint,           // 公司（原点2，罗盘750px）
    val entertainmentPoint: FengShuiPoint,  // 日常场所（原点3，罗盘500px）
    val createTime: Long = System.currentTimeMillis()
)

data class LifeCircleConnection(
    val fromPoint: FengShuiPoint,
    val toPoint: FengShuiPoint,
    val distance: Float,      // 距离（米）
    val bearing: Float,       // 方位角（0-360度）
    val shanName: String      // 24山方位名称
)

enum class LifeCirclePointType(
    val label: String,
    val icon: String,
    val compassSize: Int  // 罗盘像素尺寸
) {
    HOME("家", "🏠", 1000),              // 最大罗盘
    WORK("公司", "💼", 750),              // 中等罗盘
    ENTERTAINMENT("日常场所", "🍽️", 500)  // 最小罗盘
}
```

### 9.2 激活流程

```
1. 用户点击「更多」→「生活圈模式」
   ↓
2. 原点选择器变为多选模式（需选择3个原点）
   ↓
3. 用户选择3个原点并点击「确定」
   ↓
4. 显示角色分配对话框（RoleAssignmentDialog）
   ├─ 读取缓存：roleAssignmentCache[Set(id1,id2,id3)]
   ├─ 有缓存 → 显示历史分配
   └─ 无缓存 → 智能推荐（基于名称关键词）
   ↓
5. 用户确认角色分配
   ├─ 保存到缓存（会话级别）
   └─ 调用 recalculateBearings()
   ↓
6. activateLifeCircleModeFromOrigins() 执行
   ├─ 构建 LifeCircleData
   ├─ 隐藏主功能罗盘和连线
   ├─ 创建3个不同尺寸罗盘（1000, 750, 500px）
   ├─ 绘制三角连线（三种颜色）
   ├─ 计算"指入"连线信息
   └─ 更新 TextMarker 标签
   ↓
7. 地图显示生活圈
   ├─ 三个罗盘在三个位置
   ├─ 三条彩色连线构成三角形
   ├─ 每个罗盘显示2个"指入"标签
   └─ 顶部显示 LifeCircleBanner
```

### 9.3 "指入"逻辑

```
核心设计：每个罗盘上显示"指向它"的连线，而非"它指向"的连线

家的罗盘上显示：
  - 餐厅→家（方位角、距离、24山）
  - 公司→家（方位角、距离、24山）

公司的罗盘上显示：
  - 家→公司
  - 餐厅→公司

日常场所的罗盘上显示：
  - 公司→餐厅
  - 家→餐厅

标签格式：「→来源名→ | 45.3° | 艮山 | 2.5km」
```

### 9.4 三角连线颜色编码

```
家 ↔ 公司：      绿色 #00C853
公司 ↔ 餐厅：    蓝色 #2196F3
餐厅 ↔ 家：      橙色 #FF9800
```

### 9.5 智能角色推荐算法

```kotlin
// 基于名称关键词自动推荐角色
val homeKeywords = setOf("家", "住宅", "小区", "公寓", "楼盘", "房", "宅", "居")
val workKeywords = setOf("公司", "办公", "工作", "单位", "企业", "写字楼", "厂", "店")
val entertainmentKeywords = setOf("餐厅", "商场", "健身", "娱乐", "咖啡", "超市", "饭店")

// 两轮匹配：
// 第一轮：明确匹配（某个类别得分显著高于其他）
// 第二轮：为剩余原点分配剩余角色（按列表顺序）
```

### 9.6 角色分配缓存机制

```kotlin
// 缓存结构
val roleAssignmentCache: Map<Set<String>, Map<String, LifeCirclePointType>>
// Key: 原点ID集合 Set("id1","id2","id3")
// Value: 角色映射 {"id1": HOME, "id2": WORK, "id3": ENTERTAINMENT}

// 生命周期：会话级别（MapScreen销毁时清空，防止跨项目污染）

// 作用：用户再次选择相同3个原点时，自动恢复之前的角色分配
```

**代码位置**：
- `data/LifeCircleData.kt`
- `map/viewmodel/LifeCircleActions.kt`
- `map/dialog/RoleAssignmentDialog.kt`
- `map/ui/LifeCircleBanner.kt`

---

## 模块10：认证系统

### 10.1 双认证系统设计

```
ENABLE_CLOUD_AUTH = false（默认）
┌─────────────────────────────────────┐
│        本地注册码系统                │
│  (TrialManager)                     │
│  - 10个预设注册码                    │
│  - 设备指纹绑定（一码一机）          │
│  - 存储在 SharedPreferences         │
└─────────────────────────────────────┘

ENABLE_CLOUD_AUTH = true（可选）
┌─────────────────────────────────────┐
│        云端认证系统                  │
│  (AuthManager + Supabase)           │
│  - 手机号 + 短信验证码               │
│  - Supabase GoTrue 模块             │
│  - JWT Token 自动管理               │
└─────────────────────────────────────┘
```

### 10.2 本地注册码验证流程

```
用户输入注册码
    ↓
验证码是否在预定义列表中
    ↓
获取设备指纹（Android ID 的 MD5）
    ↓
检查注册码是否已被其他设备使用
    ├─ 已使用 → 返回错误
    └─ 未使用 → 绑定设备
    ↓
保存注册状态到 SharedPreferences
    ↓
解除试用限制
```

### 10.3 10个预设注册码

```kotlin
private val VALID_LOCAL_CODES = listOf(
    "FENGSHUI2024", "COMPASS888", "LUOPAN666", "BAGUA8899",
    "WUXING5588", "YIJING9999", "TIANPAN2025", "DIPAN2025",
    "RENPAN2025", "SANYUAN3333"
)
```

### 10.4 设备指纹生成

```kotlin
object DeviceFingerprint {
    fun get(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
        return androidId.toMD5()  // 生成32位MD5哈希
    }
}
```

**代码位置**：
- `TrialManager.kt`
- `auth/AuthManager.kt`
- `auth/DeviceFingerprint.kt`

---

## 模块11：云端同步机制

### 11.1 Supabase表结构

```sql
-- 用户档案表
user_profiles (
    id TEXT PRIMARY KEY,              -- Supabase用户ID
    phone_number TEXT UNIQUE,         -- 手机号
    device_fingerprint TEXT,          -- 设备指纹
    registration_code TEXT,           -- 注册码
    registration_status TEXT,         -- 'trial' | 'premium'
    last_sync_at BIGINT
)

-- 点位分组表
point_groups (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT,
    is_deleted BOOLEAN DEFAULT false, -- 软删除
    is_dirty BOOLEAN DEFAULT false,   -- 脏数据标记
    cloud_updated_at BIGINT           -- 云端更新时间
)

-- 风水点位表
fengshui_points (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    group_id TEXT,
    name TEXT,
    latitude DOUBLE,
    longitude DOUBLE,
    point_type TEXT,                  -- 'ORIGIN' | 'DESTINATION'
    is_active BOOLEAN,
    is_visible BOOLEAN,
    is_gps_origin BOOLEAN,
    is_deleted BOOLEAN DEFAULT false,
    is_dirty BOOLEAN DEFAULT false,
    cloud_updated_at BIGINT
)
```

### 11.2 同步策略

```
SyncWorker（每15分钟后台触发）
    │
    ├─ 阶段1: 上传脏数据
    │   ├─ SELECT * FROM fengshui_points WHERE is_dirty = true
    │   ├─ 逐条 Upsert 到 Supabase
    │   ├─ 成功后 markAsSynced (is_dirty = false)
    │   └─ 单条失败隔离，不影响其他
    │
    └─ 阶段2: 下载云端数据
        ├─ SELECT * FROM fengshui_points WHERE user_id = ?
        ├─ 与本地数据对比
        ├─ ConflictResolver 解决冲突
        └─ 更新本地 Room 数据库
```

### 11.3 冲突解决策略（Last-Write-Wins）

```
冲突解决优先级：

1. 删除状态优先
   - 本地已删除 → 保留删除
   - 云端已删除 → 标记为删除

2. 时间戳比较（cloudUpdatedAt 或 updatedAt）
   - 本地时间 >= 云端时间 → 保留本地
   - 本地时间 < 云端时间 → 使用云端版本

3. 新数据插入
   - 云端有、本地无 → 插入本地
```

**代码位置**：
- `sync/SyncWorker.kt`
- `data/remote/PointSyncRepository.kt`
- `sync/ConflictResolver.kt`

---

## 模块12：性能优化与边界处理

### 12.1 华为设备黑屏修复

```kotlin
// 问题：华为Mali GPU在应用从后台恢复时，OpenGL纹理丢失导致黑屏

// 解决方案：PageLifecycleEffect中显式管理生命周期
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                mapView?.onPause()
                compassMarkerManager?.temporaryReleaseBitmap()
            }
            Lifecycle.Event.ON_RESUME -> {
                mapView?.onResume()
            }
            Lifecycle.Event.ON_DESTROY -> {
                compassMarkerManager?.destroy()
            }
            else -> {}
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

### 12.2 罗盘Bitmap内存管理

```kotlin
// CompassMarkerManager.kt

// 罗盘Bitmap：1000x1000 ARGB_8888 = 约3.8MB
private var compassBitmap: Bitmap? = null
private var isDestroyed = false

// 应用后台时临时释放
fun temporaryReleaseBitmap() {
    compassBitmap?.recycle()
    compassBitmap = null
}

// 页面销毁时永久释放
fun destroy() {
    isDestroyed = true
    compassBitmap?.recycle()
    compassBitmap = null
    compassMarker?.remove()
}

// 防止销毁后创建
fun getOrCreateBitmap(): Bitmap? {
    if (isDestroyed) return null
    if (compassBitmap == null) {
        compassBitmap = createCompassBitmap()
    }
    return compassBitmap
}
```

### 12.3 覆盖物数量动态调整

```kotlin
// 根据内存压力动态调整最大数量
fun getMaxPolylineCount(): Int {
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val usagePercent = 1.0 - (memoryInfo.availMem.toDouble() / memoryInfo.totalMem)

    return when {
        usagePercent > 0.8 -> 25   // 内存紧张：最多25条
        usagePercent > 0.6 -> 35   // 内存中等：最多35条
        else -> 50                  // 正常情况：最多50条
    }
}
```

### 12.4 竞态条件防护

```kotlin
// DataLoadEffects.kt 中的防护机制
fun loadProjectData(project: Project) {
    // 1. 生成唯一操作ID
    val operationId = UUID.randomUUID().toString()
    updateUIHelperState { copy(loadingOperationId = operationId) }

    // 2. 禁用选择器交互
    updateSelectorUIState { copy(selectorInteractionEnabled = false) }

    // 3. 执行数据加载...

    // 4. 检查operationId是否仍然有效
    if (uiHelperState.value.loadingOperationId == operationId) {
        // 恢复交互
        updateSelectorUIState { copy(selectorInteractionEnabled = true) }
    }
}
```

### 12.5 高德SDK隐私合规配置

```kotlin
// MainActivity.kt - 必须在SDK使用前调用
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 地图SDK隐私合规
    MapsInitializer.updatePrivacyShow(this, true, true)
    MapsInitializer.updatePrivacyAgree(this, true)

    // 定位SDK隐私合规
    AMapLocationClient.updatePrivacyShow(this, true, true)
    AMapLocationClient.updatePrivacyAgree(this, true)

    // 搜索SDK隐私合规
    ServiceSettings.updatePrivacyShow(this, true, true)
    ServiceSettings.updatePrivacyAgree(this, true)
}
```

---

## 模块13：UI组件与对话框系统

### 13.1 主要UI组件

| 组件 | 文件 | 功能 |
|------|------|------|
| SelectorButton | SelectorButton.kt | 原点/终点选择按钮，支持徽章 |
| CrosshairModeUI | CrosshairModeUI.kt | 十字准星覆盖层（4种模式） |
| MoreMenuButton | MoreMenuButton.kt | 更多菜单（隐藏连线、生活圈、AR） |
| LifeCircleBanner | LifeCircleBanner.kt | 生活圈状态横幅（可折叠） |
| MapControlButtons | MapControlButtons.kt | 扇形区域+定位按钮 |
| CrosshairHintCard | CrosshairHintCard.kt | 首次使用提示卡片 |

### 13.2 对话框系统

| 对话框 | 文件 | 复杂度 | 功能 |
|--------|------|--------|------|
| SelectorDialog | SelectorDialog.kt | 极高 | 统一原点/终点选择器，单/多选切换 |
| AddPointDialog | AddPointDialog.kt | 极高 | 点位添加，内嵌创建项目，试用限制 |
| SectorConfigDialog | SectorConfigDialog.kt | 极高 | 扇形配置，24山/8方位，距离输入 |
| RoleAssignmentDialog | RoleAssignmentDialog.kt | 高 | 生活圈角色分配，智能推荐 |
| POIDetailDialog | POIDetailDialog.kt | 高 | POI详情，支持生活圈选择模式 |
| LineInfoDialog | LineInfoDialog.kt | 中 | 连线详情（方位、距离、山名） |
| RegistrationDialog | RegistrationDialog.kt | 中 | 注册码验证 |
| CreateProjectDialog | CreateProjectDialog.kt | 低 | 创建项目 |
| PointListDialog | PointListDialog.kt | 中 | 点位列表管理 |
| RegionChangeDialog | RegionChangeDialog.kt | 中 | 区域切换提示 |

### 13.3 ToastManager优先级队列

```kotlin
// ToastManager.kt - 防重复、优先级队列

enum class ToastPriority(val value: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    URGENT(4),
    CRITICAL(5)
}

class ToastManager {
    // 优先级队列
    private val queue = PriorityQueue<ToastItem>(compareByDescending { it.priority.value })

    // 防重复：5秒时间窗口内相同消息不重复显示
    private val recentMessages = mutableMapOf<String, Long>()
    private const val DUPLICATE_WINDOW_MS = 5000L

    fun show(message: String, priority: ToastPriority = ToastPriority.NORMAL) {
        val now = System.currentTimeMillis()
        val lastShown = recentMessages[message] ?: 0L

        if (now - lastShown < DUPLICATE_WINDOW_MS) {
            return  // 跳过重复消息
        }

        recentMessages[message] = now
        queue.offer(ToastItem(message, priority))
        processQueue()
    }
}
```

---

## 模块14：功能开关与试用限制

### 14.1 FeatureFlags

```kotlin
// FeatureFlags.kt - 所有新功能默认关闭
object FeatureFlags {
    // 云端认证（false=使用本地注册码）
    const val ENABLE_CLOUD_AUTH = false

    // Room数据库双写（false=仅SharedPreferences）
    const val ENABLE_ROOM_STORAGE = false

    // 云端Supabase同步（false=纯本地）
    const val ENABLE_CLOUD_SYNC = false
}

// 设计原则：
// - 所有新功能默认关闭，确保生产稳定
// - 支持灰度发布和快速回滚
// - 双写模式保证向后兼容
```

### 14.2 试用限制

```kotlin
// TrialManager.kt
object TrialManager {
    const val TRIAL_MAX_GROUPS = 2         // 最多2个项目
    const val TRIAL_MAX_ORIGINS = 2        // 最多2个原点
    const val TRIAL_MAX_DESTINATIONS = 5   // 最多5个终点
}

// TrialLimitException
class TrialLimitException(
    message: String,
    val limitType: LimitType
) : Exception(message) {
    enum class LimitType {
        GROUP,
        ORIGIN,
        DESTINATION
    }
}
```

### 14.3 限制检查逻辑

```kotlin
// PointRepository.createPoint() 中的限制检查
if (!trialManager.isRegistered()) {
    when (type) {
        PointType.ORIGIN -> {
            val origins = getPointsByType(PointType.ORIGIN)
                .filter { it.id != GPS_ORIGIN_ID }  // 排除GPS原点
            if (origins.size >= TRIAL_MAX_ORIGINS) {
                throw TrialLimitException(
                    "试用版最多创建2个原点...",
                    TrialLimitException.LimitType.ORIGIN
                )
            }
        }
        PointType.DESTINATION -> {
            if (destinations.size >= TRIAL_MAX_DESTINATIONS) {
                throw TrialLimitException(
                    "试用版最多创建5个终点...",
                    TrialLimitException.LimitType.DESTINATION
                )
            }
        }
    }
}
```

---

## 模块15：文件结构概览

```
app/src/main/java/com/fengshui/app/
├── MainActivity.kt              # 入口+隐私合规配置
├── Navigation.kt                # Compose导航配置
├── FeatureFlags.kt              # 功能开关
├── TrialManager.kt              # 试用限制
├── SearchResultManager.kt       # 跨页面搜索结果传递
│
├── MapScreen.kt                 # 主地图页面（1183行）
├── SearchScreen.kt              # POI搜索页面（557行）
├── SettingsScreen.kt            # 设置页面
├── ProjectManagementScreen.kt   # 项目管理页面
│
├── CompassMarkerManager.kt      # 罗盘Bitmap管理（607行）
├── GeodesicCompassManager.kt    # 测地线罗盘（Polyline实现）
├── PolylineManager.kt           # 连线管理（381行）
├── TextMarkerManager.kt         # 终点标签管理（639行）
├── SectorAreaManager.kt         # 扇形区域绘制
├── SectorPOISearchManager.kt    # 扇形POI搜索
│
├── data/                        # 数据层
│   ├── PointData.kt             # 点位数据模型
│   ├── Project.kt               # 项目数据模型
│   ├── PointRepository.kt       # 点位仓库（SP+Room双写，553行）
│   ├── ProjectRepository.kt     # 项目仓库
│   ├── ShanUtils.kt             # 24山计算工具
│   ├── LifeCircleData.kt        # 生活圈数据模型
│   └── ...
│
├── map/                         # 地图相关
│   ├── abstraction/             # 地图抽象层（双SDK支持）
│   │   ├── MapProvider.kt       # 统一接口（399行）
│   │   ├── amap/                # 高德实现
│   │   └── googlemaps/          # 谷歌实现
│   ├── viewmodel/               # MVVM视图模型
│   │   ├── MapViewModel.kt      # 主ViewModel
│   │   ├── MapState.kt          # 状态类（173行）
│   │   ├── MapActions.kt        # 业务逻辑
│   │   └── LifeCircleActions.kt # 生活圈逻辑
│   ├── dialog/                  # 弹窗组件（10个）
│   ├── ui/                      # UI组件（8个）
│   └── effects/                 # Compose副作用（4个文件，12个Effect）
│
├── utils/                       # 工具类
│   ├── RhumbLineUtils.kt        # Rhumb Line算法（340行）
│   ├── GeometryUtils.kt         # 几何计算（点到线段距离）
│   └── ScreenAdaptive.kt        # 响应式设计
│
├── auth/                        # 认证相关
│   ├── AuthManager.kt           # 云端认证
│   └── DeviceFingerprint.kt     # 设备指纹
│
├── sync/                        # 同步相关
│   ├── SyncWorker.kt            # 后台同步Worker
│   └── ConflictResolver.kt      # 冲突解决
│
└── ui/                          # 通用UI
    ├── ToastManager.kt          # Toast优先级队列
    └── auth/                    # 认证UI
        ├── PhoneAuthScreen.kt
        └── OtpVerificationScreen.kt
```

---

## 模块16：重建注意事项清单

### 算法层面

1. **必须使用Rhumb Line**计算方位角，不是Geodesic
   - 保证角度对称性：bearing_AB + bearing_BA = 360°
   - 代码位置：`utils/RhumbLineUtils.kt`

2. **24山索引公式**：
   ```kotlin
   shanIndex = ((angle + 7.5) / 15).toInt() % 24
   ```
   - 7.5°偏移使[352.5°, 7.5°)映射到子山

3. **坐标系统**：
   - 中国区用GCJ-02（高德）
   - 海外用WGS-84（谷歌）
   - 需要实现坐标转换

### 交互层面

4. **相机优先级系统**：
   - 搜索结果(4) > 用户选点(3) > 地图初始化(2) > GPS定位(1)
   - 防止低优先级覆盖高优先级

5. **点击连线**：
   - 60px热区（线宽的5倍）
   - 自定义点到线段距离算法
   - 点击后自动缩放 + 罗盘半径调整

6. **十字准星模式**：
   - 搜索→跳转→拖拽→确认分组→保存
   - 4种显示模式（正常/临时/生活圈/项目选择）

7. **生活圈模式**：
   - 3原点选择→角色分配→激活
   - "指入"逻辑：每个罗盘显示指向它的连线
   - 会话级角色分配缓存

### 性能层面

8. **50条限制**：
   - Polyline、TextMarker、POI都有上限
   - 华为设备可能需要动态降低限制

9. **华为设备黑屏修复**：
   - Bitmap需要生命周期管理
   - `temporaryReleaseBitmap()` + `destroy()`
   - `isDestroyed`标记防止销毁后创建

10. **8锚点碰撞检测**：
    - 防止标签重叠
    - 按优先级尝试8个位置

### 数据层面

11. **SharedPreferences为主**：
    - Room和Supabase都是可选（FeatureFlag控制）
    - 双写模式：异步写入Room，失败静默

12. **GPS原点特殊处理**：
    - 固定ID：`gps_location_origin`
    - 不占配额
    - 坐标动态更新

13. **试用限制**：
    - 2个项目、2个原点、5个终点
    - GPS原点不计入限制
    - 通过`TrialLimitException`触发注册对话框

### 功能开关

14. **所有新功能默认关闭**：
    - `ENABLE_CLOUD_AUTH = false`
    - `ENABLE_ROOM_STORAGE = false`
    - `ENABLE_CLOUD_SYNC = false`
    - 保证向后兼容

15. **AR罗盘**：
    - 入口存在但未完全实现
    - 可移除

---

## 附录：关键代码位置索引

| 功能 | 文件 | 行数 |
|------|------|------|
| Rhumb Line算法 | utils/RhumbLineUtils.kt | 340 |
| 24山计算 | data/ShanUtils.kt | ~200 |
| 地图抽象层 | map/abstraction/MapProvider.kt | 399 |
| 罗盘Bitmap | CompassMarkerManager.kt | 607 |
| 连线管理 | PolylineManager.kt | 381 |
| 标签碰撞检测 | TextMarkerManager.kt | 639 |
| 数据仓库 | data/PointRepository.kt | 553 |
| 主地图页面 | MapScreen.kt | 1183 |
| 状态管理 | map/viewmodel/MapState.kt | 173 |
| 十字准星UI | map/ui/CrosshairModeUI.kt | 426 |
| 生活圈逻辑 | map/viewmodel/LifeCircleActions.kt | ~300 |
| 同步Worker | sync/SyncWorker.kt | ~60 |
| 冲突解决 | sync/ConflictResolver.kt | ~211 |

---

**文档结束**

此文档涵盖了风水罗盘App的所有核心实现细节，旨在帮助新开发者理解项目并避免常见陷阱。如有疑问，请参考对应的代码文件。
