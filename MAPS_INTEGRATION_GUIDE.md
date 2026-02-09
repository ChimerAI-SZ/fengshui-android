## 📱 Google Maps 和高德地图集成完成指南

> 最后更新: 2026-02-06

### ✅ 已完成的工作

#### 1. **依赖配置** (`app/build.gradle`)
- ✓ Google Maps SDK: `com.google.android.gms:play-services-maps:18.2.0`
- ✓ 高德地图SDK: `com.amap.api:maps:latest.integration`
- ✓ Compose AndroidView 支持: `androidx.compose.ui:ui-viewbinding:1.5.0`

#### 2. **地图适配层实现**
- ✓ **GoogleMapProvider** (`map/abstraction/googlemaps/GoogleMapProvider.kt`)
  - 完整封装 Google Maps API
  - 支持标记、折线、相机动画、坐标转换
  - 支持地图类型切换 (矢量/卫星)
  - 支持缩放、平移、FitBounds等操作

- ✓ **AMapProvider** (`map/abstraction/amap/AMapProvider.kt`)  
  - 完整封装高德地图 API
  - API 功能与 GoogleMapProvider 对齐
  - 支持高德专有特性（GCJ-02坐标系等）

#### 3. **Compose 集成组件**
- ✓ **GoogleMapViewWrapper** (`map/ui/GoogleMapViewWrapper.kt`)
  - 使用 `AndroidView` 包装 MapView
  - 自动生命周期管理（onCreate/onResume/onPause/onDestroy）
  - 支持 `getMapAsync` 回调

- ✓ **AMapViewWrapper** (`map/ui/AMapViewWrapper.kt`)
  - 同上，针对高德地图

#### 4. **主界面更新**
- ✓ **MapScreen** (`map/MapScreen.kt`)
  - 替换灰色占位框为真实 Google Maps
  - 保留所有现有功能：十字准心、罗盘、控制按钮
  - 集成点位管理、连线绘制、数据面板

- ✓ **MainActivity** (`MainActivity.kt`)
  - 简化初始化流程
  - 直接调用 `MapScreen()`

---

### 🔧 后续配置步骤

#### **需要配置 Google Maps API 密钥**

Google Maps 需要 API 密钥才能正确工作。按以下步骤配置：

1. **获取 SHA-1 密钥指纹**
   ```bash
   # Windows
   cd d:\Win_Data\Desktop\fengshui-tool
   
   # 使用 Google Play Console 提供的密钥库，或自签密钥
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

2. **在 Google Cloud Console 创建项目并生成 API 密钥**
   - 访问 [Google Cloud Console](https://console.cloud.google.com)
   - 创建新项目或选择现有项目
   - 启用 `Maps SDK for Android`
   - 创建 API 密钥（限制为 Android 应用）
   - 添加上述 SHA-1 指纹和应用包名 `com.fengshui.app`

3. **在 AndroidManifest.xml 添加 API 密钥**
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY_HERE" />
   ```
   
   将其添加到 `<application>` 标签内，示例位置：
   ```xml
   <application
       ...
       android:theme="@style/Theme.Material3.Light"
       tools:targetApi="31">
       
       <meta-data
           android:name="com.google.android.geo.API_KEY"
           android:value="AIzaSyDxxxxxxxxxxxxxxxxxxxxxxxxxxx" />
   
       <activity ...>
   ```

#### **高德地图配置（可选）**

如果需要在国内使用高德地图版本：

1. **注册高德开发账号** 
   - 访问 [高德开放平台](https://lbs.amap.com)
   - 创建应用并获取 API Key

2. **在 AndroidManifest.xml 添加高德 API 密钥**
   ```xml
   <meta-data
       android:name="com.amap.api.v2.apikey"
       android:value="YOUR_AMAP_KEY_HERE" />
   ```

---

### 📋 地图选择策略

#### **推荐：Google Maps（当前默认）**
- 优点：国际化、跨境支持好、API 功能丰富
- UI 直观、集成成熟
- 适合：全球用户、境外業務

#### **高德地图（备选）**
- 优点：国内定位准度高、图源优秀
- 支持離線地圖
- 適合：國內市場、對地理精准度要求高

#### **切换地图**
无需修改业务代码，只需修改 `MapScreen.kt` 中的导入和调用：

**Google Maps 版本：**
```kotlin
GoogleMapViewWrapper(
    context = context,
    modifier = Modifier.fillMaxSize(),
    onMapReady = { provider -> mapProvider = provider }
)
```

**高德地图版本：**
```kotlin
AMapViewWrapper(
    context = context,
    modifier = Modifier.fillMaxSize(),
    onMapReady = { provider -> mapProvider = provider }
)
```

---

### 🧪 测试清单

- [ ] 编译项目（需要 Google Maps API 密钥）
- [ ] 在真机上测试 Google Maps 显示
- [ ] 测试标记添加（加原点/加终点按钮）
- [ ] 测试折线绘制（原点+终点同时存在）
- [ ] 测试地图控制（缩放、拖拽、图层切换）
- [ ] 测试罗盘显示和 GPS 定位
- [ ] 在真机上验证坐标转换（屏幕点击 → 经纬度）
- [ ] 测试试用限制和注册逻辑
- [ ] 高德地图版本集成测试（可选）

---

### 📱 权限要求

确保 `AndroidManifest.xml` 包含以下权限（已配置）：
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

在 Android 6.0 (API 23) 及以上，需要在运行时请求权限（通过 PermissionHelper）。

---

### 🎯 后续开发计划

**Phase 2 优先任务：**
1. ✓ 地图基础集成 (已完成)
2. 网络定位和权限请求增强
3. 罗盘覆盖层（Canvas绘制24山）
4. 地图点击事件优化（从 MapProvider 层接收触摸事件）
5. 多点案例的连线批量绘制

**Phase 3 任务：**
1. 案例管理列表 UI
2. 多原点文件功能
3. 点位搜索和地址反向解析

---

### ⚠️ 已知限制

1. **高德地图 API 限制**
   - `getMap()` 只支持单次回调，后续调用需通过 AMapView 引用获取
   - 当前实现没有支持多次异步回调，需要改进

2. **坐标系统**
   - Google Maps 使用 WGS-84 (GPS 坐标系)
   - 高德地图使用 GCJ-02（国内坐标系）
   - 需要在显示城表层和存储展示中保持一致

3. **组件生命周期**
   - MapView 生命周期与 Compose 生命周期可能不同步
   - 长时间后台可能导致地图状态异常（需测试）

---

### 📞 常见问题

**Q: 网络连接失败导致地图不显示？**  
A: 确保设备有网络连接，且 Google Maps API 密钥正确。検查 Logcat 中的错误信息。

**Q: 如何动态切换 Google Maps 和高德地图？**  
A: 创建一个状态变量控制 MapViewWrapper 的类型，在 onMapReady 回调中根据选择初始化不同的 Provider。

**Q: 为什么标记不显示或显示位置不对？**  
A: 检查：1) 经纬度输入是否正确 2) 地图是否已加载（onMapReady） 3) 坐标系统是否匹配

---

## 文件修改汇总

| 文件 | 改动 | 状态 |
|------|------|------|
| `app/build.gradle` | 添加 Google Maps & 高德地图依赖 | ✅ |
| `map/abstraction/googlemaps/GoogleMapProvider.kt` | 完整实现 GoogleMapProvider | ✅ |
| `map/abstraction/amap/AMapProvider.kt` | 完整实现 AMapProvider | ✅ |
| `map/ui/GoogleMapViewWrapper.kt` | 新建 Compose GoogleMap 包装器 | ✅ |
| `map/ui/AMapViewWrapper.kt` | 新建 Compose 高德地图包装器 | ✅ |
| `map/MapScreen.kt` | 集成真实地图，替换占位框 | ✅ |
| `MainActivity.kt` | 简化初始化 | ✅ |

---

**下一步：配置 Google Maps API 密钥后，就可以编译运行了！🚀**
