# 🚀 Mock Map 开发指南

> 现在可以不需要 Google Maps API Key 直接开发和编译！

## 📋 快速开始

### 当前状态
✅ **Mock Map 模式已启用** - 可以直接编译运行，无需任何外部配置

```kotlin
// MainActivity.kt 第19行
MapScreen(useMockMap = true)  // ← 现在在这里
```

### 开发工作流

1. **开发和测试阶段**
   - 保持 `useMockMap = true`
   - 编译运行，开发所有功能
   - 无需 Google Maps API Key

2. **准备上线时**
   - 配置 Google Maps API Key（参考 MAPS_INTEGRATION_GUIDE.md）
   - 改一行代码：`useMockMap = false`
   - 编译打包

---

## 🎮 Mock MapProvider 功能说明

### ✅ 已支持的操作

| 操作 | 实现 | 日志 |
|------|------|------|
| 添加标记 | ✅ 虚拟存储，可追踪 | "Added marker: ..." |
| 添加折线 | ✅ 虚拟存储，可追踪 | "Added polyline..." |
| 平移地图 | ✅ 更新中心位置 | "Animate camera to: ..." |
| 适应边界 | ✅ 计算虚拟缩放级别 | "Animate camera to bounds..." |
| 屏幕坐标转换 | ✅ 虚拟坐标映射 | "Screen location to LatLng..." |
| 地图缩放 | ✅ 虚拟缩放等级 | "Zoom in/out" |
| 地图类型切换 | ✅ 记录类型选择 | "Map type changed to..." |

### 调试日志

所有操作都会输出 LogCat 日志，使用过滤器查看：

```bash
adb logcat | grep MockMapProvider
```

示例输出：
```
D/MockMapProvider: Added marker: 原点 at (39.9042, 116.4074)
D/MockMapProvider: Added polyline from (39.9042, 116.4074) to (39.9132, 116.4127), width=8.0, color=-65536
D/MockMapProvider: Animate camera to: (39.9042, 116.4074), zoom=15.0
```

---

## 🔄 切换模式指南

### 视图结构

```
MapScreen(useMockMap = true/false)
    ├─ if (useMockMap = true)
    │   └─ Box(灰色占位框 + "Mock Map Mode" 提示)
    └─ else (useMockMap = false)
        └─ GoogleMapViewWrapper(真实 Google Maps)
```

### 代码位置

**文件：** `app/src/main/java/com/fengshui/app/MainActivity.kt`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                MapScreen(useMockMap = true)  // ← 改这一行
                //                      ↓
                //              true  = 开发模式（Mock）
                //              false = 生产模式（真实 Google Maps）
            }
        }
    }
}
```

---

## 📊 现在可以做的事

无需 Google Maps API Key：

### Phase 2 任务（现在可开始）
- ✅ 罗盘 UI 设计（Canvas 绘制 24 山）
- ✅ 点位管理（加原点、加终点按钮）
- ✅ 折线绘制和数据面板
- ✅ 试用限制和注册逻辑
- ✅ 风水算法层扩展

### Phase 3 任务（进行中）
- ✅ 多案例管理系统
- ✅ 点位列表的增删改查
- ✅ 罗盘锁定/解锁逻辑

### Phase 4 任务（准备中）
- ✅ 扇形（Sector）算法
- ✅ POI 搜索逻辑
- ✅ 生活圈模式交互

---

## 🎯 完整开发路径

### 第一步：开发功能 ✅ 现在可做
```kotlin
MapScreen(useMockMap = true)  // 使用 Mock，快速迭代
编译 → 测试 → 功能完成
```

### 第二步：配置 Google Maps ⏳ 需要时再做
1. Google Cloud Console 创建项目
2. 启用 Maps SDK for Android
3. 生成 API Key（限制为你的 app）
4. 添加到 AndroidManifest.xml

### 第三步：切换到真实地图 ⏳ 上线前做
```kotlin
MapScreen(useMockMap = false)  // 改一行代码
编译 → 真实地图工作
```

---

## ⚠️ Mock 模式的限制

| 功能 | Mock | 真实地图 |
|------|------|--------|
| 标记显示 | ❌ 虚拟结果 | ✅ 真实显示 |
| 折线显示 | ❌ 虚拟结果 | ✅ 真实显示 |
| 触摸交互 | ❌ 无地图响应 | ✅ 可拖拽缩放 |
| 坐标精度 | ⚠️ 虚拟映射 | ✅ 精确 GPS |
| 实时定位 | ⚠️ 模拟值 | ✅ 真实 GPS |

**注：** 业务逻辑、罗盘、数据面板等**完全独立于地图实现**，两种模式下都能正常工作！

---

## 检查清单

### 开发期间
- [ ] `useMockMap = true` 在 MainActivity
- [ ] 编译通过без错误
- [ ] 所有按钮响应（加原点、加终点、缩放等）
- [ ] LogCat 中能看到 Mock 操作日志
- [ ] 罗盘显示和 GPS 定位工作
- [ ] 风水计算结果正确

### 上线前
- [ ] 配置 Google Maps API Key ✅
- [ ] 在 AndroidManifest.xml 中添加 Key ✅
- [ ] 改 `useMockMap = false`
- [ ] 编译并在真机测试
- [ ] 验证地图显示和标记

---

## 常见问题

**Q: Mock 模式下能看到地图吗？**  
A: 不能。会显示灰色框 + "Mock Map Mode" 提示。这是正常的设计，目的是让你知道现在在用虚拟模式。

**Q: 点位数据保存了吗？**  
A: 保存了！数据存在 SharedPreferences 中，与地图类型无关。切换到真实地图时，所有数据都会显示。

**Q: 能同时运行两个版本测试吗？**  
A: 可以。创建不同的 Build Variant，一个用 Mock 一个用真实地图。

**Q: Mock 模式上线会怎样？**  
A: 别这样做 😅。上线前一定要改 `useMockMap = false`。

---

## 后续步骤

🚀 **现在可以开始开发 Phase 2 的罗盘 UI 了！**

需要我帮助实现什么功能？例如：
- 罗盘 Canvas 绘制（24 山、八卦等）
- 多点案例管理 UI
- 地址搜索输入框
- 数据面板样式美化

告诉我你的优先级！
