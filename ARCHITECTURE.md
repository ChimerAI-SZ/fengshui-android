# 🗺️ 地图集成架构总结

## 已实现的架构设计

### 分层架构图

```
┌─────────────────────────────────────────────────────┐
│              UI Layer (Compose)                      │
│  ┌───────────────────────────────────────────────┐  │
│  │  MapScreen  ← 主界面（十字准心、罗盘、按钮）  │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│        AndroidView Composition Layer                 │
│  ┌──────────────────┐    ┌──────────────────┐       │
│  │GoogleMapViewWrapper│  │AMapViewWrapper   │       │
│  │ (MapView 包装)    │  │(MapView 包装)    │       │
│  └──────────────────┘    └──────────────────┘       │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│      Map Provider Abstraction Layer                  │
│  ┌──────────────────┐    ┌──────────────────┐       │
│  │GoogleMapProvider │    │AMapProvider      │       │
│  │ (实现MapProvider)│    │(实现MapProvider) │       │
│  └──────────────────┘    └──────────────────┘       │
│            ↓                      ↓                   │
│  ┌──────────────────┐    ┌──────────────────┐       │
│  │MapProvider接口   │    │通用接口定义      │       │
│  │ (统一API)       │    │(addMarker等)     │       │
│  └──────────────────┘    └──────────────────┘       │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│     Native SDK Layer                                 │
│ ┌────────────────────┐  ┌────────────────────┐      │
│ │Google Maps API     │  │高德地图 API        │      │
│ │(原生GoogleMap)     │  │(原生AMap)         │      │
│ └────────────────────┘  └────────────────────┘      │
└─────────────────────────────────────────────────────┘
```

---

## 关键设计决策

### 1. **Adapter 模式实现**
每个地图提供商都实现统一的 `MapProvider` 接口：

```kotlin
interface MapProvider {
    fun addMarker(position: UniversalLatLng, title: String?): UniversalMarker
    fun addPolyline(start: UniversalLatLng, end: UniversalLatLng, width: Float, color: Int): UniversalPolyline
    fun animateCamera(target: UniversalLatLng, zoom: Float)
    fun animateCameraToBounds(bounds: UniversalLatLngBounds, padding: Int)
    fun screenLocationToLatLng(x: Float, y: Float): UniversalLatLng
    fun onCameraChangeFinish(callback: (CameraPosition) -> Unit)
    fun setMapType(type: MapType)
    fun zoomIn()
    fun zoomOut()
}
```

**优点：**
- ✅ UI 代码不依赖特定地图 SDK
- ✅ 可以随时切换地图源（Google ↔ 高德）
- ✅ 未来支持 Mapbox/BaiduMap 仅需新增 Provider

### 2. **AndroidView 包装策略**
使用 `AndroidView` Composable 包装原生 MapView：

```kotlin
AndroidView(
    factory = { mapView },       // 创建原生 MapView
    modifier = modifier           // Compose 修饰符
)
```

**优点：**
- ✅ 保留原生地图性能（Web/Compose 渲染无法比拟）
- ✅ 支持所有原生 SDK 特性
- ✅ 生命周期自动管理
- ✅ 与 Compose UI 无缝集成

### 3. **生命周期管理**
```kotlin
DisposableEffect(Unit) {
    mapView.onCreate(null)
    mapView.getMapAsync { ... }  // 地图加载完成回调
    mapView.onResume()
    
    onDispose {
        mapView.onPause()
        mapView.onDestroy()
    }
}
```

**关键细节：**
- MapView 自动同步 Activity 生命周期
- 确保地图资源正确释放，防止内存泄漏

---

## 实现对比表

| 功能 | Google Maps | 高德地图 | 统一接口 |
|------|-----------|---------|--------|
| 标记添加 | `addMarker()` | `addMarker()` | ✅ `addMarker()` |
| 折线绘制 | `addPolyline()` | `addPolyline()` | ✅ `addPolyline()` |
| 相机动画 | `animateCamera()` | `animateCamera()` | ✅ `animateCamera()` |
| 边界适应 | `newLatLngBounds()` | `newLatLngBounds()` | ✅ `animateCameraToBounds()` |
| 屏幕坐标转换 | `getProjection()` | `getProjection()` | ✅ `screenLocationToLatLng()` |
| 地图类型 | `MAP_TYPE_NORMAL/SATELLITE` | `MAP_TYPE_NORMAL/SATELLITE` | ✅ `setMapType()` |
| 缩放 | `zoomIn()/zoomOut()` | `zoomIn()/zoomOut()` | ✅ `zoomIn()/zoomOut()` |

---

## 实际代码示例

### MapScreen 中的使用（Google Maps）
```kotlin
GoogleMapViewWrapper(
    context = context,
    modifier = Modifier.fillMaxSize(),
    onMapReady = { provider ->
        mapProvider = provider
        // 统一的 MapProvider 接口
        provider.addMarker(UniversalLatLng(lat, lng), "我的位置")
        provider.animateCamera(UniversalLatLng(lat, lng), 15f)
    }
)
```

### 业务逻辑层（完全独立于地图类型）
```kotlin
// 添加点位到地图
mapProvider?.addMarker(
    UniversalLatLng(point.latitude, point.longitude),
    point.name
)

// 绘制连线
mapProvider?.addPolyline(
    UniversalLatLng(originPoint.latitude, originPoint.longitude),
    UniversalLatLng(destPoint.latitude, destPoint.longitude)
)

// 控制按钮不再关心地图类型
mapProvider?.zoomIn()
mapProvider?.setMapType(MapType.SATELLITE)
```

---

## 扩展性设计

### 未来支持其他地图源的步骤

1. **创建新 Provider**
   ```kotlin
   class MapboxProvider(context: Context) : MapProvider {
       override fun addMarker(position: UniversalLatLng, title: String?): UniversalMarker {
           // Mapbox API 调用
       }
       // ... 实现其他方法
   }
   ```

2. **创建新 Wrapper**
   ```kotlin
   @Composable
   fun MapboxViewWrapper(context: Context, onMapReady: (MapboxProvider) -> Unit) {
       // Mapbox MapView 包装
   }
   ```

3. **在 MapScreen 中切换**
   ```kotlin
   // 仅一行改动，无需修改业务代码
   MapboxViewWrapper(context, onMapReady = { ... })
   ```

---

## 坐标系统处理

### WGS-84 vs GCJ-02

| 系统 | 使用者 | 存储 | 显示 |
|------|--------|------|------|
| **WGS-84** | GPS、Google Maps | ✅ 统一存储 | Google Maps |
| **GCJ-02** | 高德、百度（国内） | 显示时转换 | 高德地图 |

**当前方案：**
- 数据库统一存储 WGS-84 坐标
- 显示时通过 Provider 自动处理坐标系转换
- 未来可在 `UniversalLatLng` 中标记坐标系类型

---

## 性能考量

### 地图渲染优化
- ✅ 原生 MapView 性能（60fps 地图交互）
- ✅ Compose UI 与地图分离，互不影响
- ✅ AndroidView 使用 Hardware Accelerated 渲染

### 内存管理
- ✅ 自动生命周期管理（DisposableEffect）
- ✅ 标记/折线对象池化（通过 mutableMapOf）
- ✅ MapView 销毁时自动清理资源

---

## 测试策略

### 单元测试
```kotlin
// 可单独测试 Provider 的坐标转换逻辑
@Test
fun testScreenLocationToLatLng() {
    val provider = GoogleMapProvider(context)
    val latLng = provider.screenLocationToLatLng(100f, 200f)
    assertEquals(expectedLat, latLng.latitude, 0.001)
}
```

### 集成测试
```kotlin
// UI 层可独立测试，不依赖真实地图
@Composable
fun MapScreenPreview() {
    MapScreen(modifier = Modifier.fillMaxSize())
}
```

---

## 总结

✅ **已完成：**
- 双提供商架构（Google + 高德）
- 统一接口层
- Compose 集成
- 生命周期管理

🚀 **立即可做：**
- 配置 Google Maps API 密钥
- 编译运行测试
- 手机真机验证地图显示

📋 **后续优化：**
- 离线地图支持
- 更复杂的图形覆盖物（Polygon、Circle）
- 高级地图交互（手势、路径规划）
- 多语言地名显示
