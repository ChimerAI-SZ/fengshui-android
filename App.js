import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, SafeAreaView, StatusBar, ScrollView, TouchableOpacity } from 'react-native';
import { Magnetometer, Accelerometer } from 'expo-sensors';

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

// 分金数据
const FENJIN = [
  { name: '甲子', start: 0, end: 3 },
  { name: '丙子', start: 3, end: 6 },
  { name: '戊子', start: 6, end: 9 },
  { name: '庚子', start: 9, end: 12 },
  { name: '壬子', start: 12, end: 15 }
];

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

  useEffect(() => {
    const magnetometerSubscription = Magnetometer.addListener(data => {
      setMagnetometerData(data);
    });

    const accelerometerSubscription = Accelerometer.addListener(data => {
      setAccelerometerData(data);
    });

    Magnetometer.setUpdateInterval(100);
    Accelerometer.setUpdateInterval(100);

    return () => {
      magnetometerSubscription.remove();
      accelerometerSubscription.remove();
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

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView style={styles.scrollView}>
        <View style={styles.content}>
          <Text style={styles.title}>24山风水测量工具</Text>
          
          <View style={styles.compassContainer}>
            <Text style={styles.angleText}>{angle.toFixed(1)}°</Text>
            <Text style={styles.mountainText}>{mountain?.name || '计算中...'}</Text>
            <Text style={styles.fenjinText}>{fenjin?.name || ''}</Text>
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
              <Text style={styles.infoLabel}>分金:</Text>
              <Text style={styles.infoValue}>{fenjin?.name || '未知'}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>度数范围:</Text>
              <Text style={styles.infoValue}>
                {mountain ? `${mountain.start.toFixed(1)}° - ${mountain.end.toFixed(1)}°` : '未知'}
              </Text>
            </View>
          </View>
          
          <TouchableOpacity 
            style={[styles.button, isCalibrating && styles.buttonDisabled]} 
            onPress={handleCalibrate}
            disabled={isCalibrating}
          >
            <Text style={styles.buttonText}>
              {isCalibrating ? '校准中...' : '校准罗盘'}
            </Text>
          </TouchableOpacity>
          
          <Text style={styles.instructions}>
            将手机水平放置，缓慢旋转以测量方位。
            首次使用时请校准罗盘以获得最佳精度。
          </Text>
        </View>
      </ScrollView>
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
    flex: 1,
    alignItems: 'center',
    padding: 20,
    paddingBottom: 40,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginTop: 20,
    marginBottom: 40,
    color: '#333',
  },
  compassContainer: {
    width: 220,
    height: 220,
    borderRadius: 110,
    borderWidth: 2,
    borderColor: '#333',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 40,
    backgroundColor: '#f5f5f5',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  angleText: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#333',
  },
  mountainText: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#e63946',
    marginTop: 10,
  },
  fenjinText: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#457b9d',
    marginTop: 5,
  },
  infoContainer: {
    width: '100%',
    backgroundColor: '#f9f9f9',
    borderRadius: 10,
    padding: 20,
    marginBottom: 30,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.18,
    shadowRadius: 1.0,
    elevation: 1,
  },
  infoTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 15,
    color: '#333',
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  infoLabel: {
    fontSize: 16,
    color: '#666',
  },
  infoValue: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  button: {
    backgroundColor: '#1d3557',
    paddingVertical: 12,
    paddingHorizontal: 30,
    borderRadius: 25,
    marginBottom: 30,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  buttonDisabled: {
    backgroundColor: '#a8dadc',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  instructions: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    lineHeight: 22,
  },
});
