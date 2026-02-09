# 代码架构与组织指南

## 📐 项目结构

```
fengshui-tool/
├── App.js                              ← 主应用文件 (1209 行)
├── 文档文件/
│   ├── ENHANCED_FEATURES_GUIDE.md      ← V2.0 完整功能说明
│   ├── QUICK_START.md                  ← 5分钟快速开始
│   ├── POINT_FEATURE_GUIDE.md          ← V1.0 功能说明
│   ├── IMPLEMENTATION_SUMMARY.md       ← 实现总结 (本文件)
│   ├── 交互细节.txt                    ← 需求规范
│   └── 其他文档...
└── app.json                            ← 应用配置

```

---

## 🏗️ App.js 代码组织

### 1. 导入和常量定义 (第1-114行)

```javascript
// React 导入
import React, { useState, useEffect } from 'react';
import { ... } from 'react-native';

// 常量定义
const COLORS = [...]        // 5种颜色 (预留)
const MOUNTAINS = [...]     // 24山数据
const BAGUA = [...]         // 8卦数据

// 工具函数
const getBagua = (name) => {...}
const calculateBearing = (lat1, lon1, lat2, lon2) => {...}
const calculateDistance = (lat1, lon1, lat2, lon2) => {...}
```

### 2. App 组件 (第116-300行)

#### 2.1 状态定义 (第121-150行)

```javascript
export default function App() {
  // 传感器状态
  const [angle, setAngle] = useState(0)
  const [mountain, setMountain] = useState(null)
  const [fenjin, setFenjin] = useState(null)
  
  // 案例状态
  const [casesList, setCasesList] = useState([...])
  const [selectedCaseId, setSelectedCaseId] = useState('1')
  
  // 点位和连线状态
  const [pointsList, setPointsList] = useState([])
  const [linesList, setLinesList] = useState([])
  
  // UI 状态
  const [showCaseModal, setShowCaseModal] = useState(false)
  const [showAddPointModal, setShowAddPointModal] = useState(false)
  const [showPointsList, setShowPointsList] = useState(false)
  const [showLinesList, setShowLinesList] = useState(false)
  
  // 表单状态
  const [pointType, setPointType] = useState('origin')
  const [pointName, setPointName] = useState('')
  const [selectedCaseForPoint, setSelectedCaseForPoint] = useState('1')
}
```

#### 2.2 副作用钩子 (第151-180行)

```javascript
useEffect(() => {
  // 初始化传感器 (磁力计和加速度计)
})

useEffect(() => {
  // 基于传感器数据计算罗盘方向
})

useEffect(() => {
  // 基于罗盘方向计算24山和分金
})
```

#### 2.3 事件处理函数 (第181-260行)

```javascript
const handleAddCase = () => {...}       // 新建案例
const handleDeleteCase = () => {...}    // 删除案例
const handleSavePoint = () => {...}     // 保存点位
const createLine = () => {...}          // 创建连线
const handleDeletePoint = () => {...}   // 删除点位
const handleDeleteLine = () => {...}    // 删除连线
```

#### 2.4 计算变量 (第261-270行)

```javascript
const currentCasePoints = pointsList.filter(...)
const currentCaseLines = linesList.filter(...)
const selectedCase = casesList.find(...)
const currentCaseOrigins = currentCasePoints.filter(...)
const currentCaseEndpoints = currentCasePoints.filter(...)
```

### 3. 返回大结构 (JSX) (第271-950行)

```javascript
return (
  <SafeAreaView style={styles.container}>
    <ScrollView style={styles.scrollView}>
      <View style={styles.content}>
        {/* 标题 */}
        {/* 案例选择区 */}
        {/* 罗盘显示区 */}
        {/* 统计信息区 */}
        {/* 详细信息区 */}
        {/* 按钮组 */}
      </View>
    </ScrollView>
    
    {/* 模态框1：新建案例 */}
    {/* 模态框2：加点 */}
    {/* 模态框3：点位列表 */}
    {/* 模态框4：连线列表 */}
  </SafeAreaView>
)
```

### 4. 样式定义 (第952-1209行)

```javascript
const styles = StyleSheet.create({
  container: {...}
  scrollView: {...}
  content: {...}
  
  // 案例相关
  caseSection: {...}
  caseTag: {...}
  
  // 罗盘相关
  compassContainer: {...}
  crosshair: {...}
  
  // 统计信息
  statsContainer: {...}
  
  // 信息区
  infoContainer: {...}
  
  // 按钮
  button: {...}
  primaryButton: {...}
  
  // 模态框
  modalBackground: {...}
  modalContent: {...}
  
  // 列表项
  pointItem: {...}
  lineItem: {...}
  
  // ... 共40+ 个样式定义
})
```

---

## 🔄 数据流向

### 加点流程

```
点击"+ 在十字准星处加点"
            ↓
setShowAddPointModal(true)
            ↓
showAddPointModal Modal 出现
    - TypeSelector: 选择 origin/endpoint
    - CaseSelector: 选择关联案例
    - TextInput: 输入点的名称
    - 显示当前位置信息
            ↓
用户点击"保存"按钮
            ↓
handleSavePoint() 执行
    1. 验证 pointName 不为空
    2. 生成模拟 GPS 坐标 (演示用)
    3. 创建点位对象
    4. 添加到 pointsList
    5. **触发自动连线逻辑**
            ↓
检查 pointType:
    
    如果 pointType === 'endpoint':
        1. 查找同案例的所有原点
        2. 遍历每个原点
        3. 调用 createLine(origin, endpoint)
    
    如果 pointType === 'origin':
        1. 查找同案例的所有终点
        2. 遍历每个终点
        3. 调用 createLine(origin, endpoint)
            ↓
createLine(originPoint, endpointPoint) 执行
    1. 计算 bearing = calculateBearing(...)
    2. 计算 distance = calculateDistance(...)
    3. 推导 bearingMountain = calculateMountain(bearing)
    4. 创建 newLine 对象
    5. 添加到 linesList
            ↓
UI 自动更新 (useState 触发 re-render)
    - 原点数/终点数/连线数 刷新
    - 点位列表更新
    - 连线列表更新
            ↓
显示成功提示: Alert.alert()
关闭对话框: setShowAddPointModal(false)
```

---

## 🎯 核心函数详解

### calculateBearing(lat1, lon1, lat2, lon2)

```javascript
目的：计算从点1指向点2的方位角
输入：两个点的纬度和经度
输出：0-360 之间的角度
      0° = 北
      90° = 东
      180° = 南
      270° = 西

使用的三角学算法：
    y = sin(dLon) * cos(lat2)
    x = cos(lat1) * sin(lat2) - 
        sin(lat1) * cos(lat2) * cos(dLon)
    bearing = atan2(y, x) * 180 / π
```

### calculateDistance(lat1, lon1, lat2, lon2)

```javascript
目的：计算两点间的地面距离
输入：两个点的纬度和经度  
输出：距离（km），保留2位小数

使用的球面距离公式（Haversine）：
    a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
    c = 2 * atan2(√a, √(1-a))
    distance = 6371 * c

特点：考虑地球曲率，精度高
```

### handleSavePoint()

```javascript
目的：保存点位并自动生成连线
流程：
    1. 验证输入
    2. 创建点位对象（包含GPS、方位、五行、八卦等信息）
    3. 更新 pointsList
    4. 根据点的类型触发连线生成
    5. 显示成功提示
    6. 关闭对话框

关键：
    - 通过检查点的类型决定查找哪类点
    - 遍历所有可配对的点并生成连线
    - 利用 createLine() 进行实际生成
```

### createLine(originPoint, endpointPoint)

```javascript
目的：从原点到终点创建单条连线
步骤：
    1. 基于两点坐标计算方位角 (bearing)
    2. 计算两点间距离 (distance)  
    3. 查找方位角对应的24山 (mountain)
    4. 获取山的五行属性 (element)
    5. 从山位推导八卦 (bagua)
    6. 创建连线数据对象
    7. 添加到 linesList

返回值：无（直接修改状态）
副作用：linesList 状态更新
```

---

## 🧲 状态管理架构

### 传感器相关状态

```javascript
const [magnetometerData, setMagnetometerData] = useState(null)
const [accelerometerData, setAccelerometerData] = useState(null)
        ↓ (useEffect 监听)
const [angle, setAngle] = useState(0)           // 0-360°
const [mountain, setMountain] = useState(null)  // 24山
const [fenjin, setFenjin] = useState(null)      // 分金
```

### 案例相关状态

```javascript
const [casesList, setCasesList] = useState([
    { id: '1', name: '样本案例 1', createdAt: '...' },
    ...
])
const [selectedCaseId, setSelectedCaseId] = useState('1')
```

### 业务数据状态

```javascript
const [pointsList, setPointsList] = useState([
    { id, caseId, pointType, name, angle, mountain, ... },
    ...
])
const [linesList, setLinesList] = useState([
    { id, caseId, originId, endpointId, bearing, distance, ... },
    ...
])
```

### UI 状态

```javascript
const [showCaseModal, setShowCaseModal] = useState(false)
const [showAddPointModal, setShowAddPointModal] = useState(false)
const [showPointsList, setShowPointsList] = useState(false)
const [showLinesList, setShowLinesList] = useState(false)
```

### 表单状态

```javascript
const [pointType, setPointType] = useState('origin')
const [pointName, setPointName] = useState('')
const [newCaseName, setNewCaseName] = useState('')
const [selectedCaseForPoint, setSelectedCaseForPoint] = useState('1')
```

---

## 🎨 样式架构

### 样式组织方式

```css
顶级容器：
  - container (SafeAreaView)
    - scrollView
    - content

区域样式：
  - caseSection (案例选择)
  - compassContainer (罗盘)
  - statsContainer (统计)
  - infoContainer (信息)
  - buttonGroup (按钮)

组件样式：
  - caseTag (案例标签)
  - crosshair* (十字准星)
  - button* (按钮变体)
  
模态框样式：
  - modalBackground
  - modalContent
  - typeSelector
  - caseSelector

列表项样式：
  - pointItem (单个点位)
  - lineItem (单条连线)

工具样式：
  - textInput
  - infoRow
  - statItem
```

### 色彩系统

```
主色：
  - primaryButton: #e63946 (红色 - 重要操作)
  - secondaryButton: #457b9d (蓝色 - 次要操作)
  - warningButton: #f77f00 (橙色 - 警示)

背景色：
  - caseSection: #f0f8ff (浅蓝)
  - infoContainer: #f9f9f9 (浅灰)
  - statsContainer: #fff8e1 (浅黄)
  - originItem: #fff5f5 (浅红)
  - endpointItem: #f5fff9 (浅青)

文字色：
  - title: #1d3557 (深蓝)
  - mountainText: #e63946 (红色)
  - label: #666 (中灰)
```

---

## 📦 模态框组件

| 模态框 | 位置 | 功能 | 触发 |
|--------|------|------|------|
| CaseModal | 第560行 | 新建案例 | 点击"+ 新建案例" |
| AddPointModal | 第590行 | 加点 | 点击"+ 在十字准星处加点" |
| PointsListModal | 第690行 | 查看点位 | 点击"📍点位" |
| LinesListModal | 第760行 | 查看连线 | 点击"📈连线" |

---

## 🧪 测试要点

### 单元测试

```javascript
// 应测试的函数
- calculateBearing() - 方位角计算准确性
- calculateDistance() - 距离计算准确性
- getBagua() - 八卦推导正确性
- handleSavePoint() - 点位保存逻辑
- createLine() - 连线生成逻辑
```

### 集成测试

```javascript
// 应测试的流程
- 加点完整流程
- 自动连线生成
- 点位删除和清理
- 案例切换和隔离
- 模态框打开/关闭
```

### 用户交互测试

```
- 触摸按钮响应
- 输入框可编辑性
- 列表滚动平滑性
- 对话框动画流畅
```

---

## 🚀 优化建议

### 性能优化

```javascript
// 1. 大列表优化 (目前无虚拟化)
使用 FlatList 的 maxToRenderPerBatch
考虑实现 windowSize

// 2. 计算优化
缓存 calculateBearing 结果
使用 useMemo 避免重复计算

// 3. 渲染优化
memo() 包装列表项
使用 useCallback 稳定函数引用
```

### 代码优化

```javascript
// 1. 抽离 useEffect
分离传感器、罗盘、UI 更新的 effect

// 2. 自定义 Hook
创建 useCompass() - 罗盘逻辑
创建 usePoints() - 点位管理逻辑

// 3. 常量提取
定义 POINT_TYPES = { ORIGIN, ENDPOINT }
定义 MODAL_TYPES = { CASE, POINT, ... }
```

### 架构改进

```javascript
// 1. Context API
使用 PointsContext 管理业务数据
使用 CompassContext 管理传感器

// 2. Reducer
使用 useReducer 管理复杂的状态更新

// 3. 分离
创建独立的 Modal 组件
创建独立的 List 组件
```

---

## 📚 代码阅读导航

### 快速查找

| 功能 | 位置 | 行号 |
|------|------|------|
| 导入和常量 | App.js 顶部 | 1-114 |
| 状态定义 | App 函数内 | 121-150 |
| Effect 钩子 | App 函数内 | 151-180 |
| 事件处理 | App 函数内 | 181-260 |
| JSX 返回 | App 函数内 | 271-950 |
| 样式定义 | App.js 底部 | 952-1209 |

### 推荐阅读顺序

1. **快速了解**：阅读 QUICK_START.md
2. **功能详解**：阅读 ENHANCED_FEATURES_GUIDE.md
3. **源码分析**：按上表顺序阅读 App.js
4. **深入研究**：研究算法函数（calculateBearing、calculateDistance）

---

## 🔐 安全考虑

### 数据验证

```javascript
// 应添加：
- 输入长度限制 (pointName 最大长度)
- 输入类型检查 (coordinates 必须是数字)
- 范围验证 (bearing 在 0-360)
```

### 错误处理

```javascript
// 目前缺少：
try-catch 块
错误日志记录
用户友好的错误提示
```

---

## 📝 注释说明

### 注释覆盖率

- 常数定义：✅ 有注释
- 函数：⚠️ 部分有注释
- 复杂逻辑：✅ 有注释
- 计算公式：⚠️ 需要更详细

### 改进建议

```javascript
// 当前风格
const calculateBearing = (lat1, lon1, lat2, lon2) => {
  // ... 代码 ...
}

// 建议风格
/**
 * 计算从点1指向点2的地理方位角
 * @param {number} lat1 - 点1纬度
 * @param {number} lon1 - 点1经度
 * @param {number} lat2 - 点2纬度
 * @param {number} lon2 - 点2经度
 * @returns {number} 方位角 (0-360°)
 */
const calculateBearing = (lat1, lon1, lat2, lon2) => {
  // ...
}
```

---

## 🎓 学习资源

### 相关技术

- React Native 官方教程
- useState/useEffect 文档
- StyleSheet 最佳实践
- Modal 组件深入学习

### 风水学知识

- 24山方位体系
- 八卦原理
- 五行属性
- 分金定义

### 地理计算

- Bearing 公式推导
- Haversine 公式详解
- GPS 坐标系统
- 地球曲率修正

---

**本文档持续更新中...**

版本：2.0  
最后更新：2026年2月9日

