# 连线显示故障排查指南

## 问题诊断步骤

### 1. 启用日志查看
编译应用后，在Android Studio的logcat中查看以下日志：

```bash
# 查看所有MapScreen相关日志
adb logcat | grep "MapScreen"

# 或在Android Studio Terminal运行
./gradlew installDebug && adb logcat MapScreen
```

### 2. 验证数据加载

启动应用后在logcat中寻找：
```
MapScreen: Loading project: xxx, found X points
MapScreen: Origins: X, Destinations: X
MapScreen: Total lines: X
```

**预期结果：**
- 应该看到至少1个案例被加载
- Origins和Destinations的数字应该 > 0
- Total lines应该等于 Origins数 × Destinations数

**若出现问题：**
- 如果看不到"Loading project"，说明`loadProjectData()`没有被调用
- 如果Origins或Destinations为0，说明案例中没有点位数据

### 3. 验证连线生成

添加新原点时，应该在logcat中看到：
```
MapScreen: Created origin: 原点1 at (lat, lng)
MapScreen: Adding line from origin (...) to dest (...)
```

⚠️ **若没有看到这些日志**：
- 点击"➕原点"按钮没有工作
- 检查是否有案例被选中（应该在右上方看到"📋 案例: 案例名称"）

**若看到异常日志**：
```
MapScreen: Error adding origin point: xxx
```
- 记下错误消息，这可能是TrialLimitException或其他异常

### 4. 验证GoogleMap初始化

查看logcat，应该看到：
```
GoogleMapView: Map is ready
GoogleMapProvider: GoogleMap initialized
```

**若没有看到这些：**
- Google Maps可能没有正确初始化
- 检查GooglePlayServices是否正确安装
- 检查是否有API密钥问题

### 5. 验证Polyline添加

当linesList改变时，应该看到：
```
MapScreen: Adding polyline from (lat1, lng1) to (lat2, lng2)
```

**若没有看到：**
- 检查linesList是否真的改变了（logcat中应该看到加入操作）
- 检查gMapInstance是否为null（若为null，则GoogleMap未就绪）

## 常见问题及解决方案

### 问题1：启动应用后看不到任何连线

**检查清单：**
1. ✅ 点击"📋 案例"确认有案例存在
2. ✅ 案例中是否有原点和终点（查看logcat：Origins和Destinations数字）
3. ✅ 查看logcat是否有"GoogleMap not ready"警告
4. ✅ 地图本身是否能显示（应该能看到谷歌地图）

**解决方案：**
```
如果logcat显示 "Origins: 0, Destinations: 0"
→ 说明案例中没有点位，需要添加
→ 点击"➕原点"和"➕终点"添加点位

如果logcat显示 "GoogleMap not ready"
→ Google Maps初始化失败
→ 确保GooglePlayServices安装
→ 尝试重启应用
```

### 问题2：添加原点后还是看不到连线

**检查清单：**
1. ✅ 是否已有终点？（logcat应该显示Destinations > 0）
2. ✅ 是否看到"Adding line from origin ... to dest ..."日志
3. ✅ 是否有"Error adding polyline"错误信息

**解决方案：**
```
如果没有终点，添加原点不会产生连线
→ 先三击"➕终点"添加终点
→ 再点击"➕原点"添加原点
→ 现在应该能看到连线

如果看到"Error adding polyline"
→ 这表示mapProvider可能有问题
→ 检查GoogleMapProvider.setGoogleMap()是否调用
→ 查看完整错误信息
```

### 问题3：mapProvider出错

示例错误日志：
```
GoogleMapProvider: AndroidMap not initialized
```

**解决方案：**
1. 确保GoogleMapView的onMapReady被正确调用
2. 检查GoogleMapProvider的setGoogleMap()是否被调用
3. 确保没有其他线程问题（addPolyline应该在主线程调用）

## 详细的调试步骤

### 步骤1：确认案例和点位已存在

```
1. 启动应用
2. 打开logcat，过滤"MapScreen"
3. 寻找类似的输出：
   MapScreen: Loading project: 默认案例, found 3 points
   MapScreen: Origins: 1, Destinations: 2
   MapScreen: Total lines: 2
```

如果看不到这些日志，说明`loadProjectData()`未被调用或案例为空。

### 步骤2：添加新点位并观察日志

```
1. 打开logcat
2. 点击"➕原点"按钮
3. 应该看到：
   MapScreen: Created origin: 原点1 at (39.9, 116.4)
   MapScreen: Adding line from origin ... to dest ...
```

### 步骤3：查看Polyline添加日志

在LaunchedEffect的日志中查看：
```
MapScreen: Adding polyline from (lat1, lng1) to (lat2, lng2)
```

### 步骤4：验证GoogleMap是否完全初始化

```
1. 打开logcat
2. 搜索"GoogleMap"或"setGoogleMap"
3. 确保onMapReady被调用
```

## 完整的日志跟踪示例

### 正常工作的日志序列：
```
应用启动
  ↓
MapScreen: Loading project: 默认案例, found 2 points
MapScreen: Origins: 1, Destinations: 1
MapScreen: Total lines: 1
  ↓
用户点击"➕原点"
  ↓
MapScreen: Created origin: 原点1 at (39.90, 116.40)
MapScreen: Adding line from origin (39.90, 116.40) to dest (39.91, 116.41)
MapScreen: Adding polyline from (39.90, 116.40) to (39.91, 116.41)
  ↓
连线应该出现在地图上 ✓
```

### 异常的日志序列：
```
应用启动
  ↓
MapScreen: Loading project: 默认案例, found 0 points
MapScreen: Origins: 0, Destinations: 0
  ↓
用户点击"➕原点"
  ↓
MapScreen: Created origin: 原点1 at (39.90, 116.40)
  ↓
（没有"Adding line"日志，因为没有终点）
  ↓
连线不会出现 ✗
```

## 关键调试技巧

### 1. 查看linesList的内容
在logcat中搜索"Total lines"，如果显示0，说明没有生成连线数据。

### 2. 查看polyline添加是否成功
搜索"Adding polyline"字样，如果没有看到这个日志，说明polylines没有被添加。

### 3. 检查GoogleMap对象
搜索"GoogleMap not ready"警告，如果有这个警告，说明GoogleMap初始化失败。

### 4. 添加自定义日志
如果仍然找不到问题，可以在代码中添加额外的日志：

```kotlin
// 在MapScreen.kt中添加
android.util.Log.d("MapScreen", "gMapInstance is ${if (gMapInstance != null) "ready" else "null"}")
android.util.Log.d("MapScreen", "linesList size: ${linesList.size}")
android.util.Log.d("MapScreen", "mapProvider type: ${mapProvider.javaClass.simpleName}")
```

## 若仍无法解决

请提供以下信息：
1. **完整的logcat输出**（应用启动到添加点位的整个过程）
2. **是否看到地图显示**（确认GoogleMapView工作）
3. **原点和终点数据是否存在**（通过logcat中的Origins/Destinations数字）
4. **具体是哪一步失败**（根据日志序列判断）

## 验证步骤总结

| 步骤 | 期望结果 | 日志关键词 | 问题时检查 |
|-----|--------|---------|---------|
| 启动应用 | 看到案例加载 | "Loading project" | 案例是否存在 |
| 加载数据 | Origins/Destinations > 0 | "Total lines" | 案例是否有点位 |
| GoogleMap就绪 | gMapInstance不为null | "GoogleMap not ready" | GooglePlayServices是否安装 |
| 添加原点 | 看到创建日志 | "Created origin" | 点击是否有响应 |
| 生成连线 | 看到linesList更新 | "Adding line" | 是否有终点 |
| 显示连线 | 地图上有蓝色线条 | "Adding polyline" | mapProvider是否就绪 |
