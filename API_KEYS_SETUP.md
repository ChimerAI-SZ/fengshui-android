# 配置 API Keys 指南

本指南说明如何在本地配置 Google Maps 和高德地图 API keys，而**不将 keys 提交到代码库**。

## 1. Google Maps API Key

### 1.1 获取 Key
1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 创建或选择一个项目
3. 启用以下 APIs:
   - Maps SDK for Android
   - Places API
   - Geocoding API
4. 创建 API Key（限制为 Android，包含你的应用签名）

### 1.2 本地配置
在项目根目录创建 `local.properties` 文件（**此文件在 `.gitignore` 中，不会被提交**）：

```properties
# Google Maps API Key
GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE

# Amap API Key
AMAP_API_KEY=YOUR_AMAP_API_KEY_HERE
```

### 1.3 在 AndroidManifest.xml 中引用
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${GOOGLE_MAPS_API_KEY}" />
```

## 2. 高德地图 API Key

### 2.1 获取 Key
1. 访问 [高德开放平台](https://lbs.amap.com/)
2. 登录开发者账号，创建应用
3. 获取 Android Key（需提供应用签名指纹）

### 2.2 本地配置
同上，在 `local.properties` 中：
```properties
AMAP_API_KEY=YOUR_AMAP_API_KEY_HERE
```

## 3. 在 build.gradle 中使用

### AndroidManifest.xml
```xml
<meta-data
    android:name="com.amap.api.v2.apikey"
    android:value="${AMAP_API_KEY}" />
```

### build.gradle (app)
```gradle
android {
    ...
    defaultConfig {
        ...
        manifestPlaceholders = [
            GOOGLE_MAPS_API_KEY: getKeysProperties("GOOGLE_MAPS_API_KEY"),
            AMAP_API_KEY: getKeysProperties("AMAP_API_KEY")
        ]
    }
}

def getKeysProperties(key) {
    def keysFile = rootProject.file("local.properties")
    if (!keysFile.exists()) {
        return "PLACEHOLDER"
    }
    def keysProperties = new Properties()
    keysProperties.load(new FileInputStream(keysFile))
    return keysProperties.getProperty(key, "PLACEHOLDER")
}
```

## 4. 依赖声明

在 `build.gradle (app)` 中添加：

```gradle
dependencies {
    // Google Maps SDK for Android
    implementation 'com.google.android.gms:play-services-maps:18.2.0'
    
    // Amap SDK
    implementation 'com.amap.platform:3dmap:latest.integration'
    implementation 'com.amap.platform:common:latest.integration'
    
    // Network: Retrofit + OkHttp for API calls
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
}
```

## 5. 安全最佳实践

✅ **必做**：
- `local.properties` 必须在 `.gitignore` 中
- 不要在源码中硬编码 API key
- 在 Google Cloud 控制台中限制 key 使用范围（仅 Android app）
- 在高德开放平台中限制 key 使用范围（仅你的应用包名）

✅ **建议**：
- 为开发环境、测试环境、生产环境分别创建 key
- 定期轮换 key
- 监控异常使用情况

---

**下一步**：
1. 创建 `local.properties` 并填入你的 keys
2. 运行编译
3. 部署到设备

