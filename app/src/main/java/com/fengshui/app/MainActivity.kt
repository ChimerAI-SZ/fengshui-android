package com.fengshui.app

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.fengshui.app.screens.MainAppScreen
import com.fengshui.app.utils.PermissionHelper

class MainActivity : AppCompatActivity() {
    
    // 权限请求启动器
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            // 权限已授予，无需额外操作，LocationHelper会自动开始工作
        } else {
            // 权限被拒绝，可以在这里显示提示
            // 注意：罗盘会显示默认位置（北京），不会完全不显示
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 请求GPS定位权限
        if (!PermissionHelper.hasLocationPermission(this)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Phase 3: 集成底部导航栏
                    // MainAppScreen 包含四个 Tab：
                    // - [地图] MapScreen (使用真实 Google Maps)
                    // - [堪舆管理] CaseListScreen
                    // - [搜索] SearchScreen
                    // - [说明] InfoScreen
                    MainAppScreen()
                }
            }
        }
    }
}
