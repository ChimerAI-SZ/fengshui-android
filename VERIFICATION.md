# V0 验收说明

## 功能清单

### ✅ 已完成

1. **地图抽象层与基础 APK**
   - 地图抽象接口 `MapProvider` 支持多 SDK（AMap/Google Maps）
   - 矢量/卫星图层切换（UI 占位）
   - 缩放按钮（+/- 控制缩放级别）

2. **罗盘显示与实时更新**
   - `CompassManager` 集成设备 GPS 和传感器（磁力计/陀螺仪）
   - 罗盘针固定在当前 GPS 位置，随磁力计朝向旋转
   - `CompassOverlay` Compose 组件显示方位标注（N/S/E/W、度数）

3. **屏幕中心十字准心**
   - 固定在屏幕中心，红色十字线
   - 可点击触发"新增原点/终点"流程

4. **单案例堪舆点位流程**
   - "加原点"：创建 `FengShuiPoint(ORIGIN)` 并保存
   - "加终点"：创建 `FengShuiPoint(DESTINATION)` 并保存
   - 原点→终点连线逻辑（绘制线条、相机动画）
   - 点击连线：弹出线信息对话框

5. **连线信息显示**
   - 原点/终点名称与经纬度
   - **方位角**（Rhumb Line 恒向线计算，0°～360°）
   - **24山方位**（`getShanName`，例如"正北""东偏北45度"等）
   - **八卦**（`getBaGua`，例如"离""坎"等）
   - **五行**（`getWuXing`，例如"火""水"等）
   - **直线距离**（`haversineDistanceMeters`，单位米）

6. **基本持久化**
   - SharedPreferences 存储项目和点位（JSON 格式）
   - `PointRepository` 提供 CRUD 接口

7. **试用限制与注册**
   - 试用限制：最多 2 个原点、5 个终点（GPS 原点不计入）
   - `TrialLimitException` 超限时抛出
   - `RegistrationDialog` 注册码输入界面
   - 注册码校验：`TRIAL-UNLOCK-2026`（示例码）
   - 注册成功后解锁所有功能

8. **权限与传感器**
   - 运行时权限申请（GPS）
   - 传感器整合：Rotation Vector（优先）→ Accel+Mag（回退）

### ⚠️ 占位实现（待完整化）

- **地图 SDK 集成**：`AMapProvider`、`GoogleMapProvider` 为占位（需接入真实 SDK）
- **标记与多边形绘制**：`addMarker()`、`addPolyline()` 占位
- **屏幕坐标<→经纬度转换**：`screenLocationToLatLng()` 占位
- **相机动画**：`animateCamera()` 占位

### ❌ 未实现（V1+ 功能）

- 多原点多终点选择器
- 堪舆管理界面（案例 CRUD）
- 定位按钮（Jump to GPS）
- 罗盘锁定/解锁模式切换
- 扇形搜索、POI 搜索
- 生活圈模式
- 新手指导

## 编译与运行

### 环境要求
- Android SDK 34+
- Gradle 8.0+
- Java 11+

### 编译步骤
```bash
cd fengshui-tool
./gradlew assembleDebug
```

输出 APK: `app/build/outputs/apk/debug/app-debug.apk`

### 安装与运行
```bash
# 安装到连接的设备或模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.fengshui.app/.MainActivity
```

或在 Android Studio 中按 `Run` 直接运行。

## 验收测试场景

### 场景 1：基础界面
1. 启动应用
2. 验证：
   - ✓ 地图占位区域显示
   - ✓ 屏幕中心出现红色十字准心
   - ✓ 右侧出现缩放/图层按钮
   - ✓ 底部出现罗盘（GPS 定位后）

### 场景 2：添加原点
1. 点击"加原点"按钮
2. 验证：
   - ✓ 创建 origin 点并保存
   - ✓ 罗盘移动到原点位置
   - ✓ 应用数据库保存该点

### 场景 3：添加终点与连线
1. 点击"加终点"按钮
2. 验证：
   - ✓ 创建 destination 点
   - ✓ 显示原→终点连线
   - ✓ 罗盘保持在原点
   - ✓ 相机动画（占位）到原点

### 场景 4：点击连线查看信息
1. 添加原点和终点后，点击连线
2. 验证信息对话框显示：
   - ✓ 两点名称与坐标
   - ✓ 方位角（例如 45.3°）
   - ✓ 24 山（例如"东偏北22.5度"）
   - ✓ 八卦、五行
   - ✓ 距离（米）

### 场景 5：试用限制
1. 创建 2 个原点，再试创建第 3 个
2. 验证：
   - ✓ 弹出"试用版最多创建 2 个原点"提示
   - ✓ 出现"注册"按钮

### 场景 6：注册解锁
1. 在试用提示中点击"注册"
2. 输入注册码 `TRIAL-UNLOCK-2026`
3. 验证：
   - ✓ 注册成功提示
   - ✓ 可继续添加第 3 个原点

### 场景 7：数据持久化
1. 创建原点和终点，关闭应用
2. 重新打开应用
3. 验证：
   - ✓ 之前的点位重新加载显示
   - ✓ 连线恢复

## 已知限制

1. **地图渲染**：使用占位区域；真实地图需集成 AMap/Google Maps SDK
2. **地图交互**：无法真实拖拽地图、缩放不生效
3. **点击检测**：基于几何计算（距离阈值 60m），不依赖地图库
4. **相机动画**：占位实现，不执行真实动画
5. **跨地区坐标**：使用 Rhumb Line（恒向线）计算方位，WGS-84 坐标系

## 技术架构

```
MainActivity
├── MapScreen (Compose)
│   ├── MapProvider (抽象)
│   │   ├── AMapProvider (占位)
│   │   └── GoogleMapProvider (占位)
│   ├── CompassManager (传感器 + GPS)
│   ├── CompassOverlay (Compose)
│   └── RegistrationDialog (注册)
├── PointRepository (持久化)
│   ├── Project 模型
│   └── FengShuiPoint 模型
├── TrialManager (试用限制)
└── RhumbLineUtils, GeometryUtils (计算)
```

## 后续改进

### 高优先级
- [ ] 接入 AMap SDK（中国用户）或 Google Maps（国际）
- [ ] 实现真实标记、多边形、点击回调
- [ ] 完成相机动画与定位
- [ ] 本地代码更安全的哈希验证或服务器认证

### 中优先级
- [ ] 多原点多终点 UI 选择器
- [ ] 堪舆管理界面（增删改查）
- [ ] 定位按钮与罗盘锁定/解锁模式

### 低优先级
- [ ] POI 搜索与扇形绘制（V2）
- [ ] 生活圈模式（V3）
- [ ] 新手指导（V4）
- [ ] iOS 适配

