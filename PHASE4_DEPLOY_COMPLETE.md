# Phase 4 真实地图集成 - 完整部署手册

## 📋 概述

风水测量工具已完成 **Phase 4 核心基础设施**的实现，支持：
- ✅ Google Maps POI 搜索
- ✅ 高德地图（Amap）POI 搜索  
- ✅ 动态提供者选择（基于 API Key 配置）
- ✅ 完整的编译和部署流程

---

## 🚀 快速开始（5分钟）

### 第1步：获取 API Keys

**Google Maps:**
- 访问 [Google Cloud Console](https://console.cloud.google.com/)
- 创建/选择项目 → 启用 Maps SDK for Android + Places API
- 创建 Android API Key（需要支持你的 SHA-1 签名）

**高德地图:**
- 访问 [高德开放平台](https://lbs.amap.com/)
- 创建应用 → 生成 Android Key（需要支持你的 SHA-1 签名）

**获取 SHA-1:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey
```

### 第2步：配置 API Keys

1. **项目根目录创建 `local.properties`：**

```properties
GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_KEY_HERE
AMAP_API_KEY=YOUR_AMAP_KEY_HERE
```

2. **验证配置：**
```bash
cat local.properties
```

### 第3步：编译和部署

**Windows:**
```bash
build_and_deploy.bat
```

**macOS/Linux:**
```bash
chmod +x build_and_deploy.sh
./build_and_deploy.sh
```

### 第4步：测试

应用启动后：
1. 点击底部导航的"搜索"标签页
2. 输入地址（如"西安钟楼"）
3. 点击搜索按钮
4. 应该看到 POI 搜索结果
5. 点击"添加到案例"可以保存点位到正在编辑的案例

---

## 📁 新增文件清单

### 配置文件

| 文件 | 说明 |
|------|------|
| `local.properties` | API Keys 本地存储（需手动创建，已 .gitignore）|
| `local.properties.template` | 创建 `local.properties` 的模版文件 |

### 部署脚本

| 文件 | 说明 | 平台 |
|------|------|------|
| `build_and_deploy.bat` | 自动编译和部署脚本 | Windows |
| `build_and_deploy.sh` | 自动编译和部署脚本 | macOS/Linux |

### 文档

| 文件 | 说明 |
|------|------|
| `DEPLOYMENT_GUIDE.md` | 完整的部署配置文档 |
| `PHASE4_SPEC.md` | Phase 4 功能规范 |
| `API_KEYS_SETUP.md` | API Key 获取和配置指南 |

### 新增代码文件

| 文件 | 说明 |
|------|------|
| `app/src/main/java/.../utils/ApiKeyConfig.kt` | API Key 读取工具类 |

### 修改的代码文件

| 文件 | 修改内容 |
|------|--------|
| `app/build.gradle` | 添加 Retrofit/Gson/OkHttp 依赖 + manifestPlaceholders |
| `app/src/main/AndroidManifest.xml` | 使用 gradle 占位符替换硬编码的 Google Maps Key |
| `app/src/main/java/.../screens/SearchAndInfoScreens.kt` | 添加动态提供者选择逻辑 |

---

## 🔧 技术架构

### POI 提供者优先级

SearchScreen 会自动选择可用的提供者：

```
1. Google Maps API (若 GOOGLE_MAPS_API_KEY 有效)
        ↓ (如果 Key 无效或未配置)
2. Amap API (若 AMAP_API_KEY 有效)
        ↓ (如果 Key 无效或未配置)
3. MockPoiProvider (开发模式，不需要网络)
```

### API Key 注入流程

```
local.properties
     ↓
build.gradle (manifestPlaceholders)
     ↓
AndroidManifest.xml (${GOOGLE_MAPS_API_KEY}, ${AMAP_API_KEY})
     ↓
运行时 (ApiKeyConfig 从 meta-data 读取)
     ↓
SearchScreen (自动选择提供者)
```

### 网络请求流程

```
User Input (地址搜索)
     ↓
SearchScreen (LazyColumn + search button)
     ↓
选中的 Provider (GooglePlacesProvider 或 AmapPoiProvider)
     ↓
Retrofit HTTP Request
     ↓
JSON Response
     ↓
解析为 PoiResult List
     ↓
显示结果 + Add to Case 按钮
     ↓
repo.createPoint() 保存到数据库
```

---

## 📦 依赖项

新增网络相关依赖（build.gradle）：

```gradle
// Retrofit 2 - REST client
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// OkHttp - HTTP client
implementation 'com.squareup.okhttp3:okhttp:4.11.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

// Gson - JSON 序列化
implementation 'com.google.code.gson:gson:2.10.1'
```

**现有依赖仍然保留：**
- Compose UI framework
- Google Maps SDK
- Amap SDK
- Location Services
- coroutines

---

## 🔐 安全注意事项

### ✅ 已实施的安全措施

1. **API Keys 不在版本控制中**
   - `local.properties` 在 `.gitignore`
   - 永远不会提交到 Git

2. **构建时注入**
   - 使用 Gradle `manifestPlaceholders`
   - APK 构建时才会写入真实 Key
   - 源代码中没有硬编码 Key

3. **运行时验证**
   - 检查 Key 有效性
   - 无效 Key 时自动降级到 Mock

### ⚠️ 建议措施

1. **API Key 限制** (在 Cloud Console/高德平台)
   - 限制 Key 只能在 Android 应用中使用
   - 限制特定 API（Places、Geocoding 等）
   - 限制 SHA-1 签名

2. **监控配额**
   - 定期检查 API 用量
   - 设置配额告警

3. **发布前操作**
   - 使用不同的发布密钥（非 debug.keystore）
   - 在 Google Cloud 中配置发布版本的 SHA-1
   - 使用不同的 API Keys（可选但推荐）

---

## 🧪 测试清单

部署后请验证以下功能：

- [ ] 应用安装成功且能启动
- [ ] 搜索界面可访问（底部导航 → 搜索 Tab）
- [ ] 搜索框可输入文本
- [ ] 搜索按钮可点击触发搜索
- [ ] 搜索结果能够显示（如果配置了有效 API Key）
- [ ] 结果中能看到 POI 名称、地址、坐标
- [ ] "添加到案例" 按钮可点击
- [ ] 选择案例后 POI 能保存到该案例
- [ ] 地图视图能对应的显示新增的 POI

---

## 🐛 常见问题

### Q: 搜索无结果？

**A:** 检查以下几点：
1. 设备有网络连接（WiFi 或移动数据）
2. API Keys 在 `local.properties` 中正确配置
3. Key 未过期且有权限访问相应 API
4. 在 Google Cloud / 高德平台中验证 Key 和 SHA-1 配置

### Q: 如何从 GooglePlaces 切换到 Amap？

**A:** 删除或注释掉 `local.properties` 中的 `GOOGLE_MAPS_API_KEY` 即可。应用会自动切换到 Amap。

### Q: 我只想用 Mock 数据开发？

**A:** 在 `local.properties` 中使用无效的 Key（如 `KEY=PLACEHOLDER`），或删除 `local.properties` 文件。应用会自动使用 MockPoiProvider。

### Q: 如何在 Logcat 中查看网络请求？

**A:** 运行应用后，在终端执行：
```bash
adb logcat | grep -E "Retrofit|OkHttp|POI|Amap|Google"
```

---

## 📈 Phase 4 进度

### ✅ 已完成

- [x] POI 抽象层设计 (MapPoiProvider 接口)
- [x] MockPoiProvider 实现（开发模式）
- [x] GooglePlacesProvider 实现（140 行）
- [x] AmapPoiProvider 实现（140 行）
- [x] SearchScreen 功能性搜索 UI
- [x] 动态提供者选择
- [x] API Key 配置系统
- [x] 编译和部署脚本
- [x] 完整文档

### 🟡 进行中

- [ ] 用户真实 API Key 配置
- [ ] 设备上测试和验证

### 🔜 待做

- [ ] **Phase 4.1** POI 搜索优化（距离排序、分类筛选）
- [ ] **Phase 4.2** 风水扇形（Sector）绘制算法
- [ ] **Phase 4.3** 扇形内 POI 过滤
- [ ] **Phase 4.4** 综合风水分析界面

---

## 📞 技术支持

遇到问题？请检查以下资源：

1. **本地文档**
   - `DEPLOYMENT_GUIDE.md` - 详细配置指南
   - `PHASE4_SPEC.md` - 功能规范
   - `API_KEYS_SETUP.md` - API Key 获取步骤

2. **源代码**
   - `SearchAndInfoScreens.kt` - 搜索界面实现
   - `ApiKeyConfig.kt` - Key 配置工具
   - `AmapPoiProvider.kt` - 高德集成
   - `GooglePlacesProvider.kt` - Google 集成

3. **官方文档**
   - [Google Maps API 文档](https://developers.google.com/maps)
   - [Google Places API 文档](https://developers.google.com/maps/documentation/places)
   - [高德地图 API 文档](https://lbs.amap.com/api)

---

## ✨ 下一步

1. **配置 API Keys**（见快速开始）
2. **运行部署脚本**开始编译
3. **在设备上测试**搜索功能
4. **反馈和改进**
5. **为 Phase 4.1 做准备**（扇形绘制）

祝好运！ 🎉
