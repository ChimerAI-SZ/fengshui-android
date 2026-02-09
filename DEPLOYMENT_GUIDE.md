# Phase 4 真实地图与部署指南

## 1. 配置 API Key

### 1.1 Google Maps API Key

**步骤:**

1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 创建新项目或选择现有项目
3. 启用以下 API：
   - Maps SDK for Android
   - Places API
   - Geocoding API
4. 创建 Android API Key
   - Key type: API Key
   - In "Application restrictions" → Select "Android apps"
   - Add your app's SHA-1 fingerprint and package name
5. 复制 API Key

**SHA-1 Fingerprint 获取方法：**

```bash
# 查看 debug keystore 的 SHA-1：
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### 1.2 Amap (高德地图) API Key

**步骤:**

1. 访问 [高德开放平台](https://lbs.amap.com/)
2. 注册并登录账户
3. 进入"我的应用" → "创建新应用"
4. 填写应用信息（包括包名和签名）：
   - 包名：`com.fengshui.app`
   - SHA1 签名：使用上面获取的 SHA-1（去掉冒号）
5. 创建 Key（选择"Android"平台）
6. 复制生成的 Key

## 2. 配置 local.properties

**创建或编辑 `local.properties` 文件（项目根目录下）：**

```properties
# local.properties
GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE
AMAP_API_KEY=YOUR_AMAP_API_KEY_HERE
```

**重要：**
- 此文件不在 Git 中（已在 .gitignore）
- 仅在本地开发机器上使用
- 不要提交到版本控制系统

## 3. 代码中的 API Key 使用

### 3.1 自动选择逻辑

[SearchScreen](../../main/java/com/fengshui/app/screens/SearchAndInfoScreens.kt) 会根据 local.properties 中的 Key 自动选择提供者：

```kotlin
val provider: MapPoiProvider = remember {
    val googleKey = ApiKeyConfig.getGoogleMapsApiKey(context)
    val amapKey = ApiKeyConfig.getAmapApiKey(context)
    
    when {
        ApiKeyConfig.isValidKey(googleKey) -> GooglePlacesProvider(googleKey!!)
        ApiKeyConfig.isValidKey(amapKey) -> AmapPoiProvider(amapKey!!)
        else -> MockPoiProvider() // 开发模式
    }
}
```

**优先级：**
1. Google Maps API（若配置）
2. Amap API（若配置）
3. MockPoiProvider（如果没有配置任何真实 Key，用于完全离线开发）

### 3.2 API Key 读取方式

[ApiKeyConfig.kt](../../main/java/com/fengshui/app/utils/ApiKeyConfig.kt) 从 AndroidManifest.xml 的 meta-data 中读取 Key：

```kotlin
fun getGoogleMapsApiKey(context: Context): String? {
    val ai: ApplicationInfo = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    )
    return ai.metaData?.getString("com.google.android.geo.API_KEY")
}
```

Key 值由 Gradle 的 manifestPlaceholders 在构建时注入。

## 4. 构建配置

[build.gradle](../../build.gradle) 中的关键配置：

```gradle
// Phase 4 依赖
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.11.0'
implementation 'com.google.code.gson:gson:2.10.1'

// API Key 占位符
manifestPlaceholders = [
    GOOGLE_MAPS_API_KEY: project.findProperty("GOOGLE_MAPS_API_KEY") ?: "PLACEHOLDER",
    AMAP_API_KEY: project.findProperty("AMAP_API_KEY") ?: "PLACEHOLDER"
]
```

这使得 Gradle 会读取 local.properties 中的值并在编译时替换 AndroidManifest.xml 中的占位符。

## 5. Android 清单配置

[AndroidManifest.xml](../../main/AndroidManifest.xml) 中的 meta-data：

```xml
<!-- Google Maps API Key -->
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${GOOGLE_MAPS_API_KEY}" />

<!-- Amap API Key -->
<meta-data
    android:name="com.amap.api.v2.apikey"
    android:value="${AMAP_API_KEY}" />
```

占位符（${...}）由 Gradle 在编译时替换。

## 6. 编译与部署

### 6.1 编译 APK

在项目根目录运行：

```bash
# 生成 Debug APK（用于快速测试）
./gradlew build

# 或直接生成 APK
./gradlew assembleDebug

# 输出文件：app/build/outputs/apk/debug/app-debug.apk
```

### 6.2 部署到 Android 设备

**前置条件：**
- Android 设备已连接过 USB
- 设备已启用 USB 调试（Developer Mode）
- 已安装 Android SDK Platform Tools（包含 adb）

**部署方式 1：使用 Gradle（推荐）**

```bash
# 编译并直接安装到连接的设备
./gradlew installDebug
```

**部署方式 2：使用 adb**

```bash
# 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 卸载应用（如果需要重新安装）
adb uninstall com.fengshui.app

# 重新安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

**部署方式 3：手动安装**

1. 将 `app/build/outputs/apk/debug/app-debug.apk` 复制到设备
2. 在设备上打开文件管理器，找到 APK 文件
3. 点击安装

### 6.3 验证部署

```bash
# 检查应用是否已安装
adb shell pm list packages | grep fengshui

# 运行应用
adb shell am start -n com.fengshui.app/.MainActivity

# 查看日志
adb logcat | grep fengshui
```

## 7. 故障排查

### 问题：API Key is invalid

**原因：** 
- local.properties 未配置
- build.gradle 未成功读取 local.properties
- API Key 没有为该应用/签名启用

**解决方案：**
```bash
# 清除 Gradle 缓存
./gradlew clean

# 重新构建
./gradlew build

# 验证 local.properties 是否配置
cat local.properties | grep API_KEY
```

### 问题：adb 找不到设备

**原因：**
- 设备未连接或未启用 USB 调试
- USB 驱动程序未安装

**解决方案：**
```bash
# 列出已连接设备
adb devices

# 重新启动 adb 服务
adb kill-server
adb start-server

# 重新连接 USB 线
```

### 问题：Retrofit 请求失败

**检查清单：**
- [ ] 网络权限已在 AndroidManifest.xml 中声明（已有）
- [ ] API Key 有效且未过期
- [ ] 设备有网络连接（WiFi 或移动数据）
- [ ] API 端点在该地区可访问

**调试日志：**
```kotlin
// 在 AmapPoiProvider 或 GooglePlacesProvider 中查看 Logcat
adb logcat | grep "Retrofit\|POI\|Amap\|Google"
```

## 8. 发布到 Google Play / 应用宝

### 使用发布密钥生成 Release APK

```bash
# 生成发布 APK
./gradlew assembleRelease

# 输出文件：app/build/outputs/apk/release/app-release.apk
```

**需要签名密钥：**
- 创建 keystore 文件（仅一次）
- 在 build.gradle 中配置 signingConfigs
- Gradle 会使用该密钥签名 Release APK

详见 [Android 官方发布指南](https://developer.android.com/studio/publish)

## 9. 下一步

✅ Phase 4.0：POI 搜索（当前）
- [ ] 使用真实 API Key 测试 Google 和 Amap 搜索
- [ ] 在设备上验证搜索结果和添加 POI 功能

🟡 Phase 4.1：风水扇形绘制
- [ ] 实现扇形（Sector）算法
- [ ] 绘制指南针相对的扇形区域
- [ ] 支持角度/距离输入

🟡 Phase 4.2：扇形内 POI 过滤
- [ ] 在搜索结果中标注是否在扇形内
- [ ] 支持快速筛选扇形内的 POI

---

**Questions?** 查看相关代码文件或咨询开发团队。
