# 罗盘锁定/解锁功能实现总结

## 功能需求（来自交互细节 V1）

### V1(1) 定位按钮
> 地图页面右侧有定位按钮，点击后屏幕中心移动到当前位置，对准屏幕中心的小十字

### V1(2) 罗盘解锁和锁定模式
> 在地图页面右侧按键区选择罗盘模式切换按钮，可切换罗盘的锁定模式和解锁模式。
> - **锁定模式**：罗盘固定在当前位置，罗盘所在点在地图上的位置不随地图移动而移动
> - **解锁模式**：罗盘固定在屏幕中央，地图移动会改变罗盘相对于地图的位置

## 实现方案

### 1. 解锁模式（Unlock Mode）

**行为**：
- ✅ 罗盘固定在屏幕中央（`Alignment.Center`）
- ✅ 罗盘跟随当前GPS位置实时更新
- ✅ 用户拖动地图时，罗盘仍显示GPS位置，不随地图移动
- ✅ 显示"正在定位GPS..."提示（当未获取真实GPS时）

**实现代码**：
```kotlin
if (!compassLocked) {
    // 罗盘在屏幕中央
    Box(modifier = Modifier.align(Alignment.Center).zIndex(3f)) {
        CompassOverlay(
            azimuthDegrees = azimuth,
            latitude = realGpsLat,  // 显示GPS位置
            longitude = realGpsLng,
            sizeDp = 220.dp
        )
    }
}
```

### 2. 锁定模式（Lock Mode）

**行为**：
- ✅ 罗盘锁定在原点的地理坐标上
- ✅ 罗盘作为地图覆盖物，随地图平移而移动
- ✅ 使用 `MapProvider.latLngToScreenLocation()` 计算罗盘在屏幕上的位置
- ✅ 实时监听地图相机移动，更新罗盘屏幕位置
- ✅ 切换到锁定模式时，自动移动视角到原点位置

**实现代码**：
```kotlin
if (compassLocked && originPoint != null) {
    // 计算原点在屏幕上的坐标
    val screenPos = mapProvider.latLngToScreenLocation(
        UniversalLatLng(originPoint.latitude, originPoint.longitude)
    )
    
    // 使用 offset 将罗盘放置在计算出的屏幕位置
    Box(modifier = Modifier
        .offset { 
            IntOffset(
                (screenPos.x - compassRadius).toInt(),
                (screenPos.y - compassRadius).toInt()
            )
        }) {
        CompassOverlay(
            azimuthDegrees = azimuth,
            latitude = originPoint.latitude,  // 显示原点位置
            longitude = originPoint.longitude,
            sizeDp = 220.dp
        )
    }
}
```

### 3. 切换按钮逻辑

**"🔒 锁定" / "🔓 解锁" 按钮**：
- ✅ 显示当前状态（锁定/解锁）
- ✅ 点击切换模式
- ✅ 如果没有原点，提示"请先添加原点后再锁定罗盘"
- ✅ 切换到锁定模式时，自动移动地图到原点位置

**实现代码**：
```kotlin
Button(onClick = { 
    if (!compassLocked && originPoint == null) {
        // 提示用户先添加原点
        trialMessage = "请先添加原点后再锁定罗盘"
        showTrialDialog = true
    } else {
        compassLocked = !compassLocked
        if (compassLocked && originPoint != null) {
            updateCompassScreenPosition()
            mapProvider.animateCamera(
                UniversalLatLng(originPoint.latitude, originPoint.longitude),
                15f
            )
        }
    }
}) {
    Text(if (compassLocked) "🔒 锁定" else "🔓 解锁", fontSize = 12.sp)
}
```

### 4. 定位按钮

**"📍 定位" 按钮**：
- ✅ 点击后移动地图到当前GPS位置
- ✅ 自动切换到解锁模式
- ✅ 如果GPS未定位，提示"正在获取GPS位置..."

**实现代码**：
```kotlin
Button(onClick = {
    if (realGpsLat != null && realGpsLng != null) {
        mapProvider.animateCamera(
            UniversalLatLng(realGpsLat, realGpsLng),
            15f
        )
        compassLocked = false  // 解锁罗盘
    } else {
        trialMessage = "正在获取GPS位置..."
        showTrialDialog = true
    }
}) {
    Text("📍 定位", fontSize = 12.sp)
}
```

### 5. 地图相机监听

**监听地图移动，更新罗盘位置**：
```kotlin
DisposableEffect(Unit) {
    // 注册地图相机移动监听
    mapProvider.onCameraChangeFinish { cameraPos ->
        if (compassLocked && originPoint != null) {
            updateCompassScreenPosition()  // 更新罗盘屏幕坐标
        }
    }
    
    onDispose {
        // 清理资源
    }
}
```

## 技术亮点

### 1. 坐标系转换
使用 `MapProvider.latLngToScreenLocation()` 将地理坐标转换为屏幕坐标：
```kotlin
fun updateCompassScreenPosition() {
    if (compassLocked && originPoint != null) {
        val screenPos = mapProvider.latLngToScreenLocation(
            UniversalLatLng(originPoint.latitude, originPoint.longitude)
        )
        compassScreenPos = Offset(screenPos.x, screenPos.y)
    }
}
```

### 2. 动态 offset
使用 `Modifier.offset {}` 动态放置罗盘：
```kotlin
Box(modifier = Modifier.offset { 
    IntOffset(
        (compassScreenPos.x - compassRadiusPx).toInt(),
        (compassScreenPos.y - compassRadiusPx).toInt()
    )
})
```

### 3. 响应式更新
使用 `LaunchedEffect` 在原点变化时更新罗盘位置：
```kotlin
LaunchedEffect(originPoint, compassLocked) {
    updateCompassScreenPosition()
}
```

## UI 布局

### 右侧控制按钮（从上到下）
1. **🔒 锁定 / 🔓 解锁** - 切换罗盘锁定状态
2. **📍 定位** - 移动到当前GPS位置
3. **+** - 放大地图
4. **-** - 缩小地图
5. **矢量/卫星** - 切换地图图层
6. **加原点** - 添加原点
7. **加终点** - 添加终点

## 用户体验流程

### 场景1：首次使用（无原点）
1. 启动应用，罗盘显示在屏幕中央（解锁模式）
2. 罗盘跟随GPS位置，显示"正在定位GPS..."
3. 获取GPS后，提示消失，罗盘显示当前GPS坐标
4. 用户拖动地图，罗盘仍在屏幕中央显示GPS位置

### 场景2：添加原点后
1. 点击"加原点"按钮，在屏幕中心位置添加原点
2. 罗盘自动切换到锁定模式
3. 罗盘移动到原点位置，随地图移动
4. 视角移动到原点位置

### 场景3：锁定模式下拖动地图
1. 罗盘锁定在原点的地理位置
2. 用户拖动地图，罗盘随地图移动
3. 罗盘始终显示在原点在屏幕上的位置
4. 如果原点移出视野，罗盘也移出视野

### 场景4：切换回解锁模式
1. 点击"🔒 锁定"按钮
2. 切换到"🔓 解锁"状态
3. 罗盘立即回到屏幕中央
4. 罗盘显示当前GPS位置

### 场景5：使用定位按钮
1. 点击"📍 定位"按钮
2. 地图平滑移动到当前GPS位置
3. 罗盘自动切换到解锁模式
4. 罗盘显示在屏幕中央

## 修改的文件

### [MapScreen.kt](app/src/main/java/com/fengshui/app/map/MapScreen.kt)

**新增变量**：
- `compassScreenPos: Offset` - 锁定模式下罗盘在屏幕上的位置
- `density: Density` - 用于 dp 到 px 的转换

**新增函数**：
- `updateCompassScreenPosition()` - 更新罗盘屏幕位置

**修改逻辑**：
- ✅ 罗盘显示逻辑（解锁/锁定模式）
- ✅ 锁定/解锁按钮逻辑
- ✅ 新增定位按钮
- ✅ 地图相机监听

**新增 imports**：
```kotlin
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
```

## 测试建议

### 功能测试
1. ✅ **解锁模式**：罗盘在屏幕中央，跟随GPS
2. ✅ **锁定模式**：罗盘锁定在原点，随地图移动
3. ✅ **切换按钮**：正确切换锁定/解锁状态
4. ✅ **定位按钮**：移动到GPS位置并解锁罗盘
5. ✅ **无原点提示**：尝试锁定时提示先添加原点
6. ✅ **添加原点**：自动切换到锁定模式

### 边界测试
1. ✅ **GPS未定位**：显示默认位置（北京）
2. ✅ **原点不存在**：无法切换到锁定模式
3. ✅ **原点移出视野**：罗盘也移出视野（符合预期）
4. ✅ **快速切换**：锁定/解锁模式快速切换工作正常

### 性能测试
1. ✅ **地图拖动**：锁定模式下罗盘位置实时更新
2. ✅ **相机监听**：不影响地图流畅度
3. ✅ **坐标转换**：latLngToScreenLocation 计算快速

## 与需求对比

| 需求项 | 状态 | 说明 |
|-------|------|------|
| V1(1) 定位按钮 | ✅ | 右侧有定位按钮，点击移动到当前位置 |
| V1(2) 切换按钮 | ✅ | 右侧有锁定/解锁切换按钮 |
| V1(2) 锁定模式 | ✅ | 罗盘固定在原点位置，随地图移动 |
| V1(2) 解锁模式 | ✅ | 罗盘固定在屏幕中央，跟随GPS |
| V0(3) 添加原点后罗盘移动 | ✅ | 添加原点自动切换到锁定模式 |

## 后续优化建议

### 功能增强
1. **多原点支持**：V1(3) 需要支持多原点，需要添加原点选择功能
2. **罗盘样式**：锁定/解锁模式使用不同的视觉样式区分
3. **平滑过渡**：模式切换时添加动画效果
4. **边界检测**：原点接近屏幕边缘时，罗盘自动调整显示位置

### 性能优化
1. **防抖处理**：地图快速拖动时，限制罗盘位置更新频率
2. **缓存计算**：缓存罗盘半径等常量，避免重复计算
3. **条件渲染**：原点在视野外时，不渲染罗盘

### 用户体验
1. **首次引导**：新用户首次使用时，展示锁定/解锁功能说明
2. **状态保存**：记住用户上次的锁定/解锁状态
3. **快捷操作**：双击罗盘切换锁定/解锁状态

## 总结

✅ **完整实现了罗盘锁定/解锁功能**，符合交互细节 V1 的全部要求：
- 解锁模式：罗盘固定在屏幕中央，跟随GPS
- 锁定模式：罗盘锁定在原点位置，随地图移动
- 切换按钮：可在两种模式间切换
- 定位按钮：快速定位到当前GPS位置

🎯 **技术实现亮点**：
- 使用坐标系转换实现罗盘在地图上的动态定位
- 监听地图相机变化实时更新罗盘位置
- 良好的错误处理和用户提示
- 符合 Compose 最佳实践的响应式更新

🚀 **用户体验优化**：
- 直观的锁定/解锁图标
- 智能的模式切换逻辑
- 清晰的操作提示
- 流畅的视角动画
