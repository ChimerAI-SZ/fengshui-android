@echo off
REM ============================================================
REM 风水测量工具 - 自动编译和部署脚本 (Windows)
REM Phase 4: 真实地图集成
REM ============================================================
REM
REM 使用方法：
REM   build_and_deploy.bat           - 编译并部署到已连接的设备
REM   build_and_deploy.bat clean     - 清除构建缓存然后构建
REM   build_and_deploy.bat build     - 仅编译，不部署
REM

setlocal enabledelayedexpansion

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

REM 颜色输出（Windows 10+）
for /F %%A in ('echo prompt $H ^| cmd') do set "BS=%%A"

echo.
echo ================================
echo 风水测量工具 - 编译和部署脚本
echo ================================
echo.

REM 检查 local.properties
if not exist "local.properties" (
    echo [ERROR] local.properties 未找到!
    echo.
    echo 请按以下步骤操作：
    echo 1. 复制 local.properties.template 为 local.properties
    echo 2. 在 local.properties 中填入你的 API Key
    echo 3. 重新运行此脚本
    echo.
    pause
    exit /b 1
)

echo [OK] 找到 local.properties
echo.

REM 获取命令参数
set "BUILD_MODE=%1"
if "%BUILD_MODE%"=="" set "BUILD_MODE=deploy"

REM 清除构建记录（如果指定 clean）
if "%BUILD_MODE%"=="clean" (
    echo [*] 清除旧的构建文件...
    call .\gradlew.bat clean
    set "BUILD_MODE=deploy"
)

REM ============================================================
REM 编译应用
REM ============================================================
echo [*] 开始编译应用...
echo.

if "%BUILD_MODE%"=="build" (
    REM 仅编译
    call .\gradlew.bat assembleDebug
) else (
    REM 编译并部署
    echo [*] 编译中...
    call .\gradlew.bat assembleDebug
    
    if %ERRORLEVEL% EQU 0 (
        echo [OK] 编译成功!
        echo.
        
        REM ========================================================
        REM 部署到设备
        REM ========================================================
        echo [*] 检查已连接的设备...
        for /f "tokens=*" %%i in ('adb devices ^| find "device$"') do (
            set "DEVICE_LINE=%%i"
            for /f "tokens=1" %%d in ('echo !DEVICE_LINE!') do set "DEVICE=%%d"
        )
        
        if "!DEVICE!"=="" (
            echo [ERROR] 没有找到已连接的 Android 设备
            echo.
            echo 请检查：
            echo 1. 设备已通过 USB 连接
            echo 2. 设备已启用 USB 调试 (Developer Mode)
            echo 3. 已在设备上授权此电脑的连接
            echo.
            echo 可以手动安装 APK：
            echo app\build\outputs\apk\debug\app-debug.apk
            echo.
            pause
            exit /b 1
        )
        
        echo [OK] 找到设备: !DEVICE!
        echo.
        echo [*] 卸载旧版本应用...
        call adb uninstall com.fengshui.app
        
        echo [*] 安装新版本应用...
        call adb install -r "app\build\outputs\apk\debug\app-debug.apk"
        
        if %ERRORLEVEL% EQU 0 (
            echo [OK] 应用安装成功!
            echo.
            echo [*] 启动应用...
            call adb shell am start -n com.fengshui.app/.MainActivity
            
            echo.
            echo [OK] 部署完成!
            echo [*] 显示日志... (Ctrl+C 停止)
            echo.
            call adb logcat -s fengshui
        ) else (
            echo [ERROR] 应用安装失败!
            pause
            exit /b 1
        )
    ) else (
        echo [ERROR] 编译失败!
        echo.
        echo 请检查错误信息并修复代码。
        pause
        exit /b 1
    )
)

echo.
echo ================================
echo 操作完成
echo ================================
echo.

pause
