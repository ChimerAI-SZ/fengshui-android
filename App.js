import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, SafeAreaView, StatusBar, ScrollView, TouchableOpacity, Modal, TextInput, FlatList, Alert, Dimensions } from 'react-native';
import { Platform } from 'react-native';
import * as Sensors from 'expo-sensors';

const COLORS = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8'];
// 24山数据
const MOUNTAINS = [
  { name: '壬', start: 337.5, end: 7.5, element: '水' },
  { name: '子', start: 7.5, end: 22.5, element: '水' },
  { name: '癸', start: 22.5, end: 37.5, element: '水' },
  { name: '丑', start: 37.5, end: 52.5, element: '土' },
  { name: '艮', start: 52.5, end: 67.5, element: '土' },
  { name: '寅', start: 67.5, end: 82.5, element: '木' },
  { name: '甲', start: 82.5, end: 97.5, element: '木' },
  { name: '卯', start: 97.5, end: 112.5, element: '木' },
  { name: '乙', start: 112.5, end: 127.5, element: '木' },
  { name: '辰', start: 127.5, end: 142.5, element: '土' },
  { name: '巽', start: 142.5, end: 157.5, element: '木' },
  { name: '巳', start: 157.5, end: 172.5, element: '火' },
  { name: '丙', start: 172.5, end: 187.5, element: '火' },
  { name: '午', start: 187.5, end: 202.5, element: '火' },
  { name: '丁', start: 202.5, end: 217.5, element: '火' },
  { name: '未', start: 217.5, end: 232.5, element: '土' },
  { name: '坤', start: 232.5, end: 247.5, element: '土' },
  { name: '申', start: 247.5, end: 262.5, element: '金' },
  { name: '庚', start: 262.5, end: 277.5, element: '金' },
  { name: '酉', start: 277.5, end: 292.5, element: '金' },
  { name: '辛', start: 292.5, end: 307.5, element: '金' },
  { name: '戌', start: 307.5, end: 322.5, element: '土' },
  { name: '乾', start: 322.5, end: 337.5, element: '金' },
  { name: '亥', start: 337.5, end: 352.5, element: '水' }
];

// 八卦数据
const BAGUA = [
  { name: '乾', mountains: ['乾', '亥', '壬'], position: '西北' },
  { name: '坎', mountains: ['子', '癸'], position: '北' },
  { name: '艮', mountains: ['艮', '丑'], position: '东北' },
  { name: '震', mountains: ['甲', '卯'], position: '东' },
  { name: '巽', mountains: ['巽', '辰'], position: '东南' },
  { name: '离', mountains: ['丙', '午'], position: '南' },
  { name: '坤', mountains: ['坤', '未'], position: '西南' },
  { name: '兑', mountains: ['庚', '酉'], position: '西' }
];

// 每山15度，细分为三分金（每段5度）
const FENJIN = [
  { name: '上分金', start: 0, end: 5 },
  { name: '中分金', start: 5, end: 10 },
  { name: '下分金', start: 10, end: 15 }
];

// 获取八卦信息
const getBagua = (mountainName) => {
  return BAGUA.find(b => b.mountains.includes(mountainName));
};

// 计算两点间的方位角 (使用简化的平面坐标计算)
const calculateBearing = (lat1, lon1, lat2, lon2) => {
  const dLon = lon2 - lon1;
  const y = Math.sin(dLon * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180);
  const x = Math.cos(lat1 * Math.PI / 180) * Math.sin(lat2 * Math.PI / 180) -
            Math.sin(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.cos(dLon * Math.PI / 180);
  let bearing = Math.atan2(y, x) * 180 / Math.PI;
  bearing = (bearing + 360) % 360;
  return bearing;
};

// 计算两点间距离 (Haversine公式)
const calculateDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371; // 地球半径 km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return (R * c).toFixed(2); // 返回 km
};

// 计算24山方位
const calculateMountain = (angle) => {
  const normalizedAngle = angle < 0 ? angle + 360 : angle;
  return MOUNTAINS.find(mountain => {
    if (mountain.start <= mountain.end) {
      return normalizedAngle >= mountain.start && normalizedAngle < mountain.end;
    } else {
      return normalizedAngle >= mountain.start || normalizedAngle < mountain.end;
    }
  });
};

// 计算分金
const calculateFenjin = (angle, mountain) => {
  if (!mountain) return null;
  
  let relativeAngle;
  if (mountain.start <= mountain.end) {
    relativeAngle = angle - mountain.start;
  } else {
    relativeAngle = angle >= mountain.start ? angle - mountain.start : angle + (360 - mountain.start);
  }
  
  return FENJIN.find(fenjin => {
    return relativeAngle >= fenjin.start && relativeAngle < fenjin.end;
  });
};

// 计算罗盘方向
const calculateCompassHeading = (magnetometerData, accelerometerData) => {
  if (!magnetometerData || !accelerometerData) return 0;
  
  const { x: mx, y: my, z: mz } = magnetometerData;
  const { x: ax, y: ay, z: az } = accelerometerData;
  
  // 计算倾斜补偿的罗盘方向
  // 这里使用简化的算法，实际应用中可能需要更复杂的计算
  const normX = mx / Math.sqrt(mx * mx + my * my + mz * mz);
  const normY = my / Math.sqrt(mx * mx + my * my + mz * mz);
  
  let heading = Math.atan2(normY, normX) * 180 / Math.PI;
  heading = (heading + 360) % 360;
  
  return heading;
};

export default function App() {
  const [angle, setAngle] = useState(0);
  const [mountain, setMountain] = useState(null);
  const [fenjin, setFenjin] = useState(null);
  const [magnetometerData, setMagnetometerData] = useState(null);
  const [accelerometerData, setAccelerometerData] = useState(null);
  const [isCalibrating, setIsCalibrating] = useState(false);
  
  // 堪舆案例和点位相关状态
  const [casesList, setCasesList] = useState([
    { id: '1', name: '样本案例 1', createdAt: new Date().toISOString() }
  ]);
  const [selectedCaseId, setSelectedCaseId] = useState('1');
  const [pointsList, setPointsList] = useState([]);
  const [linesList, setLinesList] = useState([]);
  
  // 模态框和表单状态
  const [showCaseModal, setShowCaseModal] = useState(false);
  const [newCaseName, setNewCaseName] = useState('');
  const [showAddPointModal, setShowAddPointModal] = useState(false);
  const [pointType, setPointType] = useState('origin'); // 'origin' | 'endpoint'
  const [pointName, setPointName] = useState('');
  const [selectedCaseForPoint, setSelectedCaseForPoint] = useState('1');
  const [showPointsList, setShowPointsList] = useState(false);
  const [showLinesList, setShowLinesList] = useState(false);
  const [selectedLineInfo, setSelectedLineInfo] = useState(null);
  
  // 原点和终点选择相关状态
  const [selectedOriginId, setSelectedOriginId] = useState(null);
  const [selectedEndpointIds, setSelectedEndpointIds] = useState([]);
  const [showOriginSelector, setShowOriginSelector] = useState(false);
  const [showEndpointSelector, setShowEndpointSelector] = useState(false);

  useEffect(() => {
  // Web 不支持这些传感器，直接跳过避免白屏
  if (Platform.OS === 'web') return;

  // 兼容：有些环境 native module 没挂上，会导致 addListener 不是函数
  const Magnetometer = Sensors?.Magnetometer;
  const Accelerometer = Sensors?.Accelerometer;

  if (!Magnetometer?.addListener || !Accelerometer?.addListener) {
    console.warn('Sensors not available in this build.');
    return;
  }

  const magnetometerSubscription = Magnetometer.addListener((data) => {
    setMagnetometerData(data);
  });

  const accelerometerSubscription = Accelerometer.addListener((data) => {
    setAccelerometerData(data);
  });

  Magnetometer.setUpdateInterval?.(100);
  Accelerometer.setUpdateInterval?.(100);

  return () => {
    magnetometerSubscription?.remove?.();
    accelerometerSubscription?.remove?.();
  };
}, []);

  useEffect(() => {
    if (magnetometerData && accelerometerData) {
      const heading = calculateCompassHeading(magnetometerData, accelerometerData);
      setAngle(heading);
    }
  }, [magnetometerData, accelerometerData]);

  useEffect(() => {
    const currentMountain = calculateMountain(angle);
    setMountain(currentMountain);
    
    const currentFenjin = calculateFenjin(angle, currentMountain);
    setFenjin(currentFenjin);
  }, [angle]);

  const handleCalibrate = () => {
    setIsCalibrating(true);
    // 模拟校准过程
    setTimeout(() => {
      setIsCalibrating(false);
    }, 2000);
  };

  // 添加新堪舆案例
  const handleAddCase = () => {
    if (!newCaseName.trim()) {
      Alert.alert('错误', '请输入案例名称');
      return;
    }
    const newCase = {
      id: Date.now().toString(),
      name: newCaseName,
      createdAt: new Date().toISOString()
    };
    setCasesList([...casesList, newCase]);
    setSelectedCaseId(newCase.id);
    setNewCaseName('');
    setShowCaseModal(false);
  };

  // 删除案例
  const handleDeleteCase = (caseId) => {
    if (casesList.length === 1) {
      Alert.alert('错误', '至少需要保留一个案例');
      return;
    }
    const updatedCases = casesList.filter(c => c.id !== caseId);
    setCasesList(updatedCases);
    const updatedPoints = pointsList.filter(p => p.caseId !== caseId);
    setPointsList(updatedPoints);
    const updatedLines = linesList.filter(l => l.caseId !== caseId);
    setLinesList(updatedLines);
    if (selectedCaseId === caseId) {
      setSelectedCaseId(updatedCases[0].id);
    }
  };

  // 保存点位
  const handleSavePoint = () => {
    if (!pointName.trim()) {
      Alert.alert('错误', '请输入点的名称');
      return;
    }
    
    // 模拟GPS坐标（实际应用中应从真实GPS获取）
    const randomLat = 39.9 + (Math.random() - 0.5) * 0.1;
    const randomLon = 116.4 + (Math.random() - 0.5) * 0.1;
    
    const newPoint = {
      id: Date.now().toString(),
      caseId: selectedCaseForPoint,
      pointType: pointType, // 'origin' | 'endpoint'
      name: pointName,
      angle: angle,
      mountain: mountain?.name || '未知',
      fenjin: fenjin?.name || '未知',
      element: mountain?.element || '未知',
      bagua: getBagua(mountain?.name)?.name || '未知',
      latitude: randomLat,
      longitude: randomLon,
      addedAt: new Date().toISOString()
    };
    
    const updatedPoints = [...pointsList, newPoint];
    setPointsList(updatedPoints);
    
    // 检查是否需要自动生成连线
    if (newPoint.pointType === 'endpoint') {
      // 查找同案例的原点
      const origins = updatedPoints.filter(p => 
        p.caseId === selectedCaseForPoint && p.pointType === 'origin'
      );
      
      origins.forEach(origin => {
        // 为每个原点和新终点生成连线
        createLine(origin, newPoint);
      });
    } else if (newPoint.pointType === 'origin') {
      // 查找同案例的所有终点
      const endpoints = updatedPoints.filter(p => 
        p.caseId === selectedCaseForPoint && p.pointType === 'endpoint'
      );
      
      endpoints.forEach(endpoint => {
        // 为新原点和每个终点生成连线
        createLine(newPoint, endpoint);
      });
    }
    
    Alert.alert('成功', `${pointType === 'origin' ? '原点' : '终点'}已添加: ${pointName}`);
    setShowAddPointModal(false);
    setPointName('');
    setPointType('origin');
  };

  // 创建连线
  const createLine = (originPoint, endpointPoint) => {
    const bearing = calculateBearing(
      originPoint.latitude, originPoint.longitude,
      endpointPoint.latitude, endpointPoint.longitude
    );
    const distance = calculateDistance(
      originPoint.latitude, originPoint.longitude,
      endpointPoint.latitude, endpointPoint.longitude
    );
    const bearingMountain = calculateMountain(bearing);
    
    const newLine = {
      id: Date.now().toString() + Math.random(),
      caseId: originPoint.caseId,
      originId: originPoint.id,
      originName: originPoint.name,
      endpointId: endpointPoint.id,
      endpointName: endpointPoint.name,
      bearing: bearing.toFixed(1),
      distance: distance,
      mountain: bearingMountain?.name || '未知',
      element: bearingMountain?.element || '未知',
      bagua: getBagua(bearingMountain?.name)?.name || '未知',
      createdAt: new Date().toISOString()
    };
    
    setLinesList(prev => [...prev, newLine]);
  };

  // 删除点位
  const handleDeletePoint = (pointId) => {
    setPointsList(pointsList.filter(p => p.id !== pointId));
    // 删除相关的连线
    setLinesList(linesList.filter(l => !(l.originId === pointId || l.endpointId === pointId)));
  };

  // 删除连线
  const handleDeleteLine = (lineId) => {
    setLinesList(linesList.filter(l => l.id !== lineId));
  };

  // 处理原点选择
  const handleSelectOrigin = (originId) => {
    setSelectedOriginId(originId);
    setSelectedEndpointIds([]); // 重置终点选择
    const selectedOrigin = currentCasePoints.find(p => p.id === originId);
    if (selectedOrigin) {
      Alert.alert('成功', `已选择原点: ${selectedOrigin.name}\n该原点对应的所有终点和连线已显示`);
    }
    setShowOriginSelector(false);
  };

  // 处理终点选择
  const handleSelectEndpoints = () => {
    if (selectedEndpointIds.length === 0) {
      Alert.alert('提示', '请至少选择一个终点');
      return;
    }
    setShowEndpointSelector(false);
  };

  // 切换终点选择状态
  const toggleEndpointSelection = (endpointId) => {
    setSelectedEndpointIds(prevIds => 
      prevIds.includes(endpointId) 
        ? prevIds.filter(id => id !== endpointId)
        : [...prevIds, endpointId]
    );
  };

  // 全选所有终点
  const handleSelectAllEndpoints = () => {
    const allEndpointIds = currentCaseEndpoints.map(p => p.id);
    setSelectedEndpointIds(allEndpointIds);
  };

  // 清空所有终点选择
  const handleClearEndpointSelection = () => {
    setSelectedEndpointIds([]);
  };

  // 获取要显示的连线 (基于选择的原点和终点)
  const getDisplayedLines = () => {
    if (!selectedOriginId && selectedEndpointIds.length === 0) {
      return currentCaseLines; // 显示所有
    }
    
    return currentCaseLines.filter(line => {
      const originMatches = !selectedOriginId || line.originId === selectedOriginId;
      const endpointMatches = selectedEndpointIds.length === 0 || selectedEndpointIds.includes(line.endpointId);
      return originMatches && endpointMatches;
    });
  };

  // 获取当前案例的数据
  const currentCasePoints = pointsList.filter(p => p.caseId === selectedCaseId);
  const currentCaseLines = linesList.filter(l => l.caseId === selectedCaseId);
  const selectedCase = casesList.find(c => c.id === selectedCaseId);
  const currentCaseOrigins = currentCasePoints.filter(p => p.pointType === 'origin');
  const currentCaseEndpoints = currentCasePoints.filter(p => p.pointType === 'endpoint');

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView style={styles.scrollView}>
        <View style={styles.content}>
          <Text style={styles.title}>24山风水测量工具 v2.0</Text>
          
          {/* 案例选择区域 */}
          <View style={styles.caseSection}>
            <Text style={styles.sectionTitle}>堪舆案例: {selectedCase?.name}</Text>
            <View style={styles.caseButtonsRow}>
              <TouchableOpacity 
                style={[styles.smallButton, styles.primaryButton]}
                onPress={() => setShowCaseModal(true)}
              >
                <Text style={styles.buttonText}>+ 新建案例</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={[styles.smallButton, styles.secondaryButton]}
                onPress={() => setShowPointsList(true)}
              >
                <Text style={styles.buttonText}>📍点位({currentCasePoints.length})</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={[styles.smallButton, styles.warningButton]}
                onPress={() => setShowLinesList(true)}
              >
                <Text style={styles.buttonText}>📈连线({currentCaseLines.length})</Text>
              </TouchableOpacity>
            </View>
            
            {/* 案例列表 */}
            <ScrollView horizontal style={styles.caseListScroll} showsHorizontalScrollIndicator={false}>
              {casesList.map(caseItem => (
                <TouchableOpacity
                  key={caseItem.id}
                  style={[
                    styles.caseTag,
                    selectedCaseId === caseItem.id && styles.caseTagSelected
                  ]}
                  onPress={() => setSelectedCaseId(caseItem.id)}
                  onLongPress={() => handleDeleteCase(caseItem.id)}
                >
                  <Text style={[
                    styles.caseTagText,
                    selectedCaseId === caseItem.id && styles.caseTagTextSelected
                  ]}>
                    {caseItem.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
          
          {/* 罗盘区域 */}
          <View style={styles.compassContainer}>
            {/* 十字准星 */}
            <View style={styles.crosshair}>
              <View style={styles.crosshairVertical} />
              <View style={styles.crosshairHorizontal} />
              <View style={styles.crosshairCenter} />
            </View>
            
            <Text style={styles.angleText}>{angle.toFixed(1)}°</Text>
            <Text style={styles.mountainText}>{mountain?.name || '计算中...'}</Text>
            <Text style={styles.fenjinText}>{fenjin?.name || ''}</Text>
          </View>
          
          {/* 案例统计信息 */}
          <View style={styles.statsContainer}>
            <View style={styles.statItem}>
              <Text style={styles.statLabel}>原点数</Text>
              <Text style={styles.statValue}>{currentCaseOrigins.length}</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={styles.statLabel}>终点数</Text>
              <Text style={styles.statValue}>{currentCaseEndpoints.length}</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={styles.statLabel}>连线数</Text>
              <Text style={styles.statValue}>{currentCaseLines.length}</Text>
            </View>
          </View>
          
          <View style={styles.infoContainer}>
            <Text style={styles.infoTitle}>详细信息</Text>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>方位:</Text>
              <Text style={styles.infoValue}>{mountain?.name || '未知'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>五行:</Text>
              <Text style={styles.infoValue}>{mountain?.element || '未知'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>八卦:</Text>
              <Text style={styles.infoValue}>{getBagua(mountain?.name)?.name || '未知'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>分金:</Text>
              <Text style={styles.infoValue}>{fenjin?.name || '未知'}</Text>
            </View>
          </View>
          
          <View style={styles.buttonGroup}>
            <TouchableOpacity 
              style={[styles.button, styles.primaryButton]}
              onPress={() => {
                setShowAddPointModal(true);
                setSelectedCaseForPoint(selectedCaseId);
              }}
            >
              <Text style={styles.buttonText}>+ 在十字准星处加点</Text>
            </TouchableOpacity>
            
            {/* 原点选择按钮 */}
            <TouchableOpacity 
              style={[styles.button, styles.originButton]}
              onPress={() => {
                if (currentCaseOrigins.length === 0) {
                  Alert.alert('提示', '暂无原点，请在堪舆管理中添加');
                } else {
                  setShowOriginSelector(true);
                }
              }}
            >
              <Text style={styles.buttonText}>
                🔴 选择原点 {selectedOriginId ? '✓' : ''}
              </Text>
            </TouchableOpacity>
            
            {/* 终点选择按钮 */}
            <TouchableOpacity 
              style={[styles.button, styles.endpointButton]}
              onPress={() => {
                if (currentCaseEndpoints.length === 0) {
                  Alert.alert('提示', '暂无终点，请在堪舆管理中添加');
                } else {
                  setShowEndpointSelector(true);
                }
              }}
            >
              <Text style={styles.buttonText}>
                🔵 选择终点 {selectedEndpointIds.length > 0 ? `(${selectedEndpointIds.length})` : ''}
              </Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={[styles.button, isCalibrating && styles.buttonDisabled]} 
              onPress={() => {
                setIsCalibrating(true);
                setTimeout(() => setIsCalibrating(false), 2000);
              }}
              disabled={isCalibrating}
            >
              <Text style={styles.buttonText}>
                {isCalibrating ? '校准中...' : '🧭 校准罗盘'}
              </Text>
            </TouchableOpacity>
          </View>
          
          {/* 连线可视化显示区域 */}
          {getDisplayedLines().length > 0 && (
            <View style={styles.linesDisplayContainer}>
              <Text style={styles.linesDisplayTitle}>显示中的连线:</Text>
              <View style={styles.linesDisplayGrid}>
                {getDisplayedLines().map((line, index) => (
                  <View key={line.id} style={styles.lineDisplayCard}>
                    <View style={[styles.lineColorDot, { backgroundColor: COLORS[index % COLORS.length] }]} />
                    <View style={styles.lineDisplayInfo}>
                      <Text style={styles.lineDisplayName}>
                        {line.originName} → {line.endpointName}
                      </Text>
                      <Text style={styles.lineDisplayDetail}>
                        {line.bearing}° | {line.mountain} | {line.distance}km
                      </Text>
                    </View>
                  </View>
                ))}
              </View>
            </View>
          )}

        </View>
      </ScrollView>

      {/* 新建案例弹窗 */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showCaseModal}
        onRequestClose={() => setShowCaseModal(false)}
      >
        <View style={styles.modalBackground}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>新建堪舆案例</Text>
            <TextInput
              style={styles.textInput}
              placeholder="输入案例名称"
              value={newCaseName}
              onChangeText={setNewCaseName}
              placeholderTextColor="#999"
            />
            <View style={styles.modalButtonsRow}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={() => {
                  setShowCaseModal(false);
                  setNewCaseName('');
                }}
              >
                <Text style={styles.modalButtonText}>取消</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalButton, styles.confirmButton]}
                onPress={handleAddCase}
              >
                <Text style={styles.modalButtonText}>创建</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* 加点弹窗 */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showAddPointModal}
        onRequestClose={() => setShowAddPointModal(false)}
      >
        <View style={styles.modalBackground}>
          <View style={[styles.modalContent, styles.addPointModal]}>
            <Text style={styles.modalTitle}>在十字准星处加点</Text>
            
            {/* 选择点的类型 */}
            <Text style={styles.formLabel}>点的类型:</Text>
            <View style={styles.typeSelector}>
              <TouchableOpacity
                style={[
                  styles.typeOption,
                  pointType === 'origin' && styles.typeOptionSelected
                ]}
                onPress={() => setPointType('origin')}
              >
                <Text style={[
                  styles.typeOptionText,
                  pointType === 'origin' && styles.typeOptionTextSelected
                ]}>原点</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.typeOption,
                  pointType === 'endpoint' && styles.typeOptionSelected
                ]}
                onPress={() => setPointType('endpoint')}
              >
                <Text style={[
                  styles.typeOptionText,
                  pointType === 'endpoint' && styles.typeOptionTextSelected
                ]}>终点</Text>
              </TouchableOpacity>
            </View>
            
            {/* 选择案例 */}
            <Text style={styles.formLabel}>选择案例:</Text>
            <View style={styles.caseSelector}>
              {casesList.map(c => (
                <TouchableOpacity
                  key={c.id}
                  style={[
                    styles.caseSelectOption,
                    selectedCaseForPoint === c.id && styles.caseSelectOptionSelected
                  ]}
                  onPress={() => setSelectedCaseForPoint(c.id)}
                >
                  <Text style={[
                    styles.caseSelectOptionText,
                    selectedCaseForPoint === c.id && styles.caseSelectOptionTextSelected
                  ]}>{c.name}</Text>
                </TouchableOpacity>
              ))}
            </View>
            
            {/* 输入点的名称 */}
            <Text style={styles.formLabel}>点的名称:</Text>
            <TextInput
              style={styles.textInput}
              placeholder="输入点的名称"
              value={pointName}
              onChangeText={setPointName}
              placeholderTextColor="#999"
            />
            
            {/* 显示当前方向信息 */}
            <View style={styles.currentPositionInfo}>
              <Text style={styles.infoText}>当前位置: {mountain?.name || '计算中...'} ({angle.toFixed(1)}°)</Text>
            </View>
            
            <View style={styles.modalButtonsRow}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={() => {
                  setShowAddPointModal(false);
                  setPointName('');
                  setPointType('origin');
                }}
              >
                <Text style={styles.modalButtonText}>取消</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalButton, styles.confirmButton]}
                onPress={handleSavePoint}
              >
                <Text style={styles.modalButtonText}>保存</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* 点位列表弹窗 */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showPointsList}
        onRequestClose={() => setShowPointsList(false)}
      >
        <View style={styles.modalBackground}>
          <View style={[styles.modalContent, styles.listModal]}>
            <Text style={styles.modalTitle}>{selectedCase?.name} - 点位记录</Text>
            
            {currentCasePoints.length === 0 ? (
              <Text style={styles.emptyText}>暂无点位记录</Text>
            ) : (
              <FlatList
                data={currentCasePoints}
                keyExtractor={item => item.id}
                scrollEnabled={true}
                renderItem={({ item }) => (
                  <View style={[
                    styles.pointItem,
                    item.pointType === 'origin' ? styles.originItem : styles.endpointItem
                  ]}>
                    <View style={styles.pointInfo}>
                      <Text style={styles.pointTitle}>
                        {item.pointType === 'origin' ? '🔴' : '🔵'} {item.name}
                      </Text>
                      <Text style={styles.pointDetail}>
                        {item.mountain} ({item.angle.toFixed(1)}°) | {item.element}
                      </Text>
                      <Text style={styles.pointDetail}>
                        分金: {item.fenjin} | 八卦: {item.bagua}
                      </Text>
                      <Text style={styles.pointTime}>
                        {new Date(item.addedAt).toLocaleString()}
                      </Text>
                    </View>
                    <TouchableOpacity
                      style={styles.deletePointButton}
                      onPress={() => handleDeletePoint(item.id)}
                    >
                      <Text style={styles.deleteButtonText}>删除</Text>
                    </TouchableOpacity>
                  </View>
                )}
              />
            )}
            
            <TouchableOpacity
              style={[styles.modalButton, styles.confirmButton]}
              onPress={() => setShowPointsList(false)}
            >
              <Text style={styles.modalButtonText}>关闭</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* 连线列表弹窗 */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showLinesList}
        onRequestClose={() => setShowLinesList(false)}
      >
        <View style={styles.modalBackground}>
          <View style={[styles.modalContent, styles.listModal]}>
            <Text style={styles.modalTitle}>{selectedCase?.name} - 连线信息</Text>
            
            {getDisplayedLines().length === 0 ? (
              <Text style={styles.emptyText}>暂无连线记录</Text>
            ) : (
              <FlatList
                data={getDisplayedLines()}
                keyExtractor={item => item.id}
                scrollEnabled={true}
                renderItem={({ item }) => (
                  <TouchableOpacity 
                    style={styles.lineItem}
                    onPress={() => setSelectedLineInfo(item)}
                  >
                    <View style={styles.lineInfo}>
                      <Text style={styles.lineTitle}>
                        🔴 {item.originName} → 🔵 {item.endpointName}
                      </Text>
                      <Text style={styles.lineDetail}>
                        方位角: {item.bearing}° | {item.mountain}
                      </Text>
                      <Text style={styles.lineDetail}>
                        直线距离: {item.distance} km | 五行: {item.element}
                      </Text>
                      <Text style={styles.lineDetail}>
                        八卦: {item.bagua}
                      </Text>
                    </View>
                    <View style={styles.lineActions}>
                      <TouchableOpacity
                        style={styles.deleteButton}
                        onPress={() => handleDeleteLine(item.id)}
                      >
                        <Text style={styles.deleteButtonText}>删除</Text>
                      </TouchableOpacity>
                    </View>
                  </TouchableOpacity>
                )}
              />
            )}
            
            <TouchableOpacity
              style={[styles.modalButton, styles.confirmButton]}
              onPress={() => {
                setShowLinesList(false);
                setSelectedLineInfo(null);
              }}
            >
              <Text style={styles.modalButtonText}>关闭</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* 原点选择弹窗 */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showOriginSelector}
        onRequestClose={() => setShowOriginSelector(false)}
      >
        <View style={styles.modalBackground}>
          <View style={[styles.modalContent, styles.selectorModal]}>
            <Text style={styles.modalTitle}>选择原点</Text>
            
            {currentCaseOrigins.length === 0 ? (
              <Text style={styles.emptyText}>暂无原点</Text>
            ) : (
              <FlatList
                data={currentCaseOrigins}
                keyExtractor={item => item.id}
                scrollEnabled={true}
                renderItem={({ item }) => (
                  <TouchableOpacity 
                    style={[
                      styles.selectorItem,
                      selectedOriginId === item.id && styles.selectorItemSelected
                    ]}
                    onPress={() => handleSelectOrigin(item.id)}
                  >
                    <View style={styles.selectorItemContent}>
                      <Text style={[
                        styles.selectorItemText,
                        selectedOriginId === item.id && styles.selectorItemTextSelected
                      ]}>
                        {selectedOriginId === item.id ? '✓ ' : '  '}🔴 {item.name}
                      </Text>
                      <Text style={styles.selectorItemDetail}>
                        {item.mountain} ({item.angle .toFixed(1)}°)
                      </Text>
                    </View>
                  </TouchableOpacity>
                )}
              />
            )}
            
            <TouchableOpacity
              style={[styles.modalButton, styles.cancelButton]}
              onPress={() => setShowOriginSelector(false)}
            >
              <Text style={styles.modalButtonText}>关闭</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* 终点选择弹窗 */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showEndpointSelector}
        onRequestClose={() => setShowEndpointSelector(false)}
      >
        <View style={styles.modalBackground}>
          <View style={[styles.modalContent, styles.selectorModal]}>
            <Text style={styles.modalTitle}>选择终点</Text>
            
            {currentCaseEndpoints.length === 0 ? (
              <Text style={styles.emptyText}>暂无终点</Text>
            ) : (
              <>
                <View style={styles.selectorControlsRow}>
                  <TouchableOpacity 
                    style={[styles.smallButton, styles.secondaryButton]}
                    onPress={handleSelectAllEndpoints}
                  >
                    <Text style={styles.buttonText}>全选</Text>
                  </TouchableOpacity>
                  <TouchableOpacity 
                    style={[styles.smallButton, styles.warningButton]}
                    onPress={handleClearEndpointSelection}
                  >
                    <Text style={styles.buttonText}>清空</Text>
                  </TouchableOpacity>
                </View>
                
                <FlatList
                  data={currentCaseEndpoints}
                  keyExtractor={item => item.id}
                  scrollEnabled={true}
                  renderItem={({ item }) => (
                    <TouchableOpacity 
                      style={[
                        styles.selectorItem,
                        selectedEndpointIds.includes(item.id) && styles.selectorItemSelected
                      ]}
                      onPress={() => toggleEndpointSelection(item.id)}
                    >
                      <View style={styles.selectorItemContent}>
                        <Text style={[
                          styles.selectorItemText,
                          selectedEndpointIds.includes(item.id) && styles.selectorItemTextSelected
                        ]}>
                          {selectedEndpointIds.includes(item.id) ? '✓ ' : '  '}🔵 {item.name}
                        </Text>
                        <Text style={styles.selectorItemDetail}>
                          {item.mountain} ({item.angle.toFixed(1)}°)
                        </Text>
                      </View>
                    </TouchableOpacity>
                  )}
                />
              </>
            )}
            
            <View style={styles.modalButtonsRow}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={() => {
                  setShowEndpointSelector(false);
                }}
              >
                <Text style={styles.modalButtonText}>取消</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalButton, styles.confirmButton]}
                onPress={handleSelectEndpoints}
              >
                <Text style={styles.modalButtonText}>确定</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  scrollView: {
    flex: 1,
  },
  content: {
    alignItems: 'center',
    padding: 15,
    paddingBottom: 40,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginTop: 15,
    marginBottom: 20,
    color: '#1d3557',
  },
  
  // 案例相关样式
  caseSection: {
    width: '100%',
    marginBottom: 25,
    backgroundColor: '#f0f8ff',
    borderRadius: 10,
    padding: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#1d3557',
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#1d3557',
    marginBottom: 10,
  },
  caseButtonsRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 10,
  },
  smallButton: {
    flex: 1,
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  caseListScroll: {
    marginTop: 10,
  },
  caseTag: {
    backgroundColor: '#e8e8e8',
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 12,
    marginRight: 6,
    minWidth: 90,
    alignItems: 'center',
  },
  caseTagSelected: {
    backgroundColor: '#1d3557',
  },
  caseTagText: {
    fontSize: 13,
    color: '#333',
    fontWeight: '500',
  },
  caseTagTextSelected: {
    color: '#fff',
    fontWeight: 'bold',
  },
  
  // 统计信息
  statsContainer: {
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: '#fff8e1',
    borderRadius: 8,
    paddingVertical: 12,
    marginBottom: 15,
    borderWidth: 1,
    borderColor: '#ffe082',
  },
  statItem: {
    alignItems: 'center',
  },
  statLabel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  statValue: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#f57c00',
  },
  
  // 罗盘样式
  compassContainer: {
    width: 200,
    height: 200,
    borderRadius: 100,
    borderWidth: 2,
    borderColor: '#333',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 25,
    backgroundColor: '#f5f5f5',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  
  // 十字准星
  crosshair: {
    position: 'absolute',
    width: 50,
    height: 50,
    alignItems: 'center',
    justifyContent: 'center',
  },
  crosshairVertical: {
    position: 'absolute',
    width: 1,
    height: 45,
    backgroundColor: '#ff4444',
    opacity: 0.8,
  },
  crosshairHorizontal: {
    position: 'absolute',
    width: 45,
    height: 1,
    backgroundColor: '#ff4444',
    opacity: 0.8,
  },
  crosshairCenter: {
    width: 5,
    height: 5,
    borderRadius: 2.5,
    backgroundColor: '#ff4444',
    zIndex: 1,
  },
  
  angleText: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    marginTop: 35,
  },
  mountainText: {
    fontSize: 40,
    fontWeight: 'bold',
    color: '#e63946',
    marginTop: 8,
  },
  fenjinText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#457b9d',
    marginTop: 4,
  },
  
  infoContainer: {
    width: '100%',
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
    padding: 15,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.18,
    shadowRadius: 1.0,
    elevation: 1,
  },
  infoTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  infoLabel: {
    fontSize: 14,
    color: '#666',
  },
  infoValue: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
  },
  
  buttonGroup: {
    width: '100%',
    gap: 10,
    marginBottom: 20,
  },
  button: {
    paddingVertical: 12,
    paddingHorizontal: 15,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  primaryButton: {
    backgroundColor: '#e63946',
  },
  secondaryButton: {
    backgroundColor: '#457b9d',
  },
  warningButton: {
    backgroundColor: '#f77f00',
  },
  originButton: {
    backgroundColor: '#ff6b6b',
  },
  endpointButton: {
    backgroundColor: '#4ecdc4',
  },
  buttonDisabled: {
    backgroundColor: '#a8dadc',
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  
  // 模态框
  modalBackground: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    minHeight: 250,
    maxHeight: '90%',
  },
  addPointModal: {
    maxHeight: '85%',
  },
  listModal: {
    maxHeight: '80%',
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
    textAlign: 'center',
  },
  formLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginTop: 12,
    marginBottom: 8,
  },
  
  // 类型选择器
  typeSelector: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 15,
  },
  typeOption: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 6,
    backgroundColor: '#f0f0f0',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#ddd',
  },
  typeOptionSelected: {
    backgroundColor: '#e63946',
    borderColor: '#e63946',
  },
  typeOptionText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
  },
  typeOptionTextSelected: {
    color: '#fff',
  },
  
  // 案例选择器
  caseSelector: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 15,
    flexWrap: 'wrap',
  },
  caseSelectOption: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 6,
    backgroundColor: '#e8e8e8',
    borderWidth: 2,
    borderColor: '#ddd',
  },
  caseSelectOptionSelected: {
    backgroundColor: '#457b9d',
    borderColor: '#457b9d',
  },
  caseSelectOptionText: {
    fontSize: 12,
    fontWeight: '500',
    color: '#333',
  },
  caseSelectOptionTextSelected: {
    color: '#fff',
  },
  
  // 表单输入
  textInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 6,
    paddingVertical: 10,
    paddingHorizontal: 12,
    marginBottom: 12,
    fontSize: 14,
    color: '#333',
    backgroundColor: '#f9f9f9',
  },
  
  // 当前位置信息
  currentPositionInfo: {
    backgroundColor: '#e8f5e9',
    borderLeftWidth: 4,
    borderLeftColor: '#4caf50',
    paddingVertical: 10,
    paddingHorizontal: 12,
    marginBottom: 15,
    borderRadius: 4,
  },
  infoText: {
    fontSize: 13,
    color: '#2e7d32',
    fontWeight: '500',
  },
  
  // 模态框按钮
  modalButtonsRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 15,
  },
  modalButton: {
    flex: 1,
    paddingVertical: 11,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cancelButton: {
    backgroundColor: '#ddd',
  },
  confirmButton: {
    backgroundColor: '#1d3557',
  },
  modalButtonText: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#fff',
  },
  
  // 点位列表
  emptyText: {
    fontSize: 16,
    color: '#999',
    textAlign: 'center',
    marginVertical: 30,
  },
  pointItem: {
    flexDirection: 'row',
    backgroundColor: '#f5f5f5',
    borderRadius: 6,
    padding: 12,
    marginBottom: 10,
    alignItems: 'center',
    justifyContent: 'space-between',
    borderLeftWidth: 4,
    borderLeftColor: '#e63946',
  },
  originItem: {
    borderLeftColor: '#ff6b6b',
    backgroundColor: '#fff5f5',
  },
  endpointItem: {
    borderLeftColor: '#4ecdc4',
    backgroundColor: '#f5fff9',
  },
  pointInfo: {
    flex: 1,
  },
  pointTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  pointDetail: {
    fontSize: 12,
    color: '#666',
    marginBottom: 3,
  },
  pointTime: {
    fontSize: 11,
    color: '#999',
  },
  deletePointButton: {
    backgroundColor: '#ff6b6b',
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 4,
    marginLeft: 8,
  },
  
  // 连线列表
  lineItem: {
    flexDirection: 'row',
    backgroundColor: '#f5f5f5',
    borderRadius: 6,
    padding: 12,
    marginBottom: 10,
    alignItems: 'center',
    justifyContent: 'space-between',
    borderLeftWidth: 4,
    borderLeftColor: '#f77f00',
    borderTopWidth: 1,
    borderTopColor: '#ffe082',
  },
  lineInfo: {
    flex: 1,
  },
  lineTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  lineDetail: {
    fontSize: 12,
    color: '#666',
    marginBottom: 2,
  },
  lineActions: {
    marginLeft: 10,
  },
  deleteButton: {
    backgroundColor: '#ff6b6b',
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 4,
  },
  deleteButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  
  // 连线显示区域
  linesDisplayContainer: {
    width: '100%',
    backgroundColor: '#f0f8ff',
    borderRadius: 8,
    padding: 12,
    marginTop: 15,
    marginBottom: 10,
    borderLeftWidth: 4,
    borderLeftColor: '#f77f00',
  },
  linesDisplayTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  linesDisplayGrid: {
    gap: 8,
  },
  lineDisplayCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 6,
    padding: 10,
    borderLeftWidth: 3,
    borderLeftColor: '#f77f00',
  },
  lineColorDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 10,
  },
  lineDisplayInfo: {
    flex: 1,
  },
  lineDisplayName: {
    fontSize: 13,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 2,
  },
  lineDisplayDetail: {
    fontSize: 11,
    color: '#666',
  },
  
  // 选择器相关样式
  selectorModal: {
    maxHeight: '75%',
  },
  selectorControlsRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 15,
  },
  selectorItem: {
    backgroundColor: '#f5f5f5',
    borderRadius: 6,
    padding: 12,
    marginBottom: 10,
    borderLeftWidth: 3,
    borderLeftColor: '#ddd',
  },
  selectorItemSelected: {
    backgroundColor: '#e8f5e9',
    borderLeftColor: '#4caf50',
  },
  selectorItemContent: {
    flex: 1,
  },
  selectorItemText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#333',
    marginBottom: 3,
  },
  selectorItemTextSelected: {
    color: '#2e7d32',
    fontWeight: 'bold',
  },
  selectorItemDetail: {
    fontSize: 11,
    color: '#999',
  },
});
