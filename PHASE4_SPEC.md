# Phase 4 - 高级搜索与扇形分析 规范 (PHASE4_SPEC)

版本：2026-02-06
目标：实现 POI 搜索、扇形（sector/fan）绘制与基于扇形的 POI 过滤功能。

范围与优先级
- 优先实现：POI 抽象层、Search UI、API 客户端骨架（Google/Amap）、将搜索结果展示为列表和地图标记。
- 次要实现：扇形绘制与客户端过滤（Phase 4.1，可在此基础上并行开发）。

设计要点
1. POI 抽象层
- 接口：`MapPoiProvider`（方法：searchByKeyword(), searchInBounds(), reverseGeocode()）
- 实现：`AmapPoiProvider`, `GooglePlacesProvider`, `MockPoiProvider`
- 配置：通过构造函数或依赖注入注入 API key（不在源码库内硬编码）

2. Search UI
- 文件：`SearchAndInfoScreens.kt` 里的 `SearchScreen`
- 功能：关键字输入、城市/范围选择、结果列表、地图预览、`Add to Case` 操作
- 数据流：SearchScreen -> MapPoiProvider -> results -> UI

3. 扇形（sector）算法
- 函数：`computeSectorPolygon(center: LatLng, bearingDeg: Double, radiusMeters: Double, spreadDeg: Double): List<LatLng>`
- 方法：采样边界角度并计算大圆点（geodesic）或使用 RhumbLineUtils 提供的方法
- UI：允许用户在地图上设置中心、朝向、展开角度和距离

4. 服务降级与本地 mock
- 在没有 API Key 时，使用 `MockPoiProvider` 返回静态测试数据
- SearchScreen 在开发模式下自动使用 MockProvider

5. 权限与配额
- 对于 Google/Amap，需在运行时提示 API key 配置与配额注意事项（文档中说明）

6. 测试计划
- 单元：`computeSectorPolygon` 边界测试、`MapPoiProvider` 的参数传递
- 集成：SearchScreen -> MockPoiProvider 展示、AddToCase 流程

文件清单要创建或修改
- 新增：`app/src/main/java/com/fengshui/app/map/poi/MapPoiProvider.kt`
- 新增：`app/src/main/java/com/fengshui/app/map/poi/MockPoiProvider.kt`
- 新增：`app/src/main/java/com/fengshui/app/map/poi/AmapPoiProvider.kt`（骨架）
- 修改：`app/src/main/java/com/fengshui/app/screens/SearchAndInfoScreens.kt`（完善 `SearchScreen`）
- 更新文档：`PHASE4_SPEC.md`（本文件）

时间估算
- POI 抽象层 + Mock 实现 + Search UI 基本功能：2 天
- 第三方 API（Google/Amap）集成：1-2 天（取决于键与 SDK）
- 扇形绘制与过滤：2 天

安全与隐私
- 不要在源码中提交 API keys
- 在 README 中写明如何在本地配置 keys（gradle properties 或 local.properties）

---

接下来的操作：
1. 添加 POI 接口与 Mock 实现骨架到代码库
2. 更新 `SearchAndInfoScreens.kt` 以使用抽象层并提供 mock 搜索体验
3. 提交更改并进行本地编译检查（注意：我会先做静态编辑并报告，运行需你在本地执行）

