package com.fengshui.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.fengshui.app.map.MapScreen
import com.fengshui.app.map.abstraction.amap.AMapProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    // 使用 AMap 作为默认地图提供者（占位实现）
                    MapScreen(mapProvider = AMapProvider())
                }
            }
        }
    }
}
