package com.fengshui.app

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.fengshui.app.screens.MainAppScreen
import com.fengshui.app.utils.AppLanguageManager
import com.fengshui.app.utils.PermissionHelper

class MainActivity : AppCompatActivity() {
    
    // 权限请求启动器
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            // 首次授权后重建，确保 Compose 与地图层读取到最新权限并立即触发自动定位。
            recreate()
        } else {
            // 权限被拒绝，可以在这里显示提示
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguageManager.applySavedLanguageOrSystem(this)
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
