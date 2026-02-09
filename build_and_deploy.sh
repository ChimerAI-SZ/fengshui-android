#!/bin/bash

# ============================================================
# 风水测量工具 - 自动编译和部署脚本 (macOS/Linux)
# Phase 4: 真实地图集成
# ============================================================
#
# 使用方法：
#   chmod +x build_and_deploy.sh     # 首次使用，添加执行权限
#   ./build_and_deploy.sh            # 编译并部署到已连接的设备
#   ./build_and_deploy.sh clean      # 清除构建缓存然后构建
#   ./build_and_deploy.sh build      # 仅编译，不部署
#

set -e  # 任何命令失败就退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo ""
echo "================================"
echo "风水测量工具 - 编译和部署脚本"
echo "================================"
echo ""

# 检查 local.properties
if [ ! -f "local.properties" ]; then
    echo -e "${RED}[ERROR]${NC} local.properties 未找到!"
    echo ""
    echo "请按以下步骤操作："
    echo "1. 复制 local.properties.template 为 local.properties"
    echo "2. 在 local.properties 中填入你的 API Key"
    echo "3. 重新运行此脚本"
    echo ""
    exit 1
fi

echo -e "${GREEN}[OK]${NC} 找到 local.properties"
echo ""

# 获取命令参数
BUILD_MODE="${1:-deploy}"

# 清除构建记录（如果指定 clean）
if [ "$BUILD_MODE" = "clean" ]; then
    echo -e "${YELLOW}[*]${NC} 清除旧的构建文件..."
    ./gradlew clean
    BUILD_MODE="deploy"
fi

# ============================================================
# 编译应用
# ============================================================
echo -e "${YELLOW}[*]${NC} 开始编译应用..."
echo ""

if [ "$BUILD_MODE" = "build" ]; then
    # 仅编译
    ./gradlew assembleDebug
else
    # 编译并部署
    echo -e "${YELLOW}[*]${NC} 编译中..."
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[OK]${NC} 编译成功!"
        echo ""
        
        # ========================================================
        # 部署到设备
        # ========================================================
        echo -e "${YELLOW}[*]${NC} 检查已连接的设备..."
        
        DEVICE=$(adb devices | grep -w "device" | head -1 | awk '{print $1}')
        
        if [ -z "$DEVICE" ]; then
            echo -e "${RED}[ERROR]${NC} 没有找到已连接的 Android 设备"
            echo ""
            echo "请检查："
            echo "1. 设备已通过 USB 连接"
            echo "2. 设备已启用 USB 调试 (Developer Mode)"
            echo "3. 已在设备上授权此电脑的连接"
            echo ""
            echo "可以手动安装 APK："
            echo "app/build/outputs/apk/debug/app-debug.apk"
            echo ""
            exit 1
        fi
        
        echo -e "${GREEN}[OK]${NC} 找到设备: $DEVICE"
        echo ""
        
        echo -e "${YELLOW}[*]${NC} 卸载旧版本应用..."
        adb uninstall com.fengshui.app || true
        
        echo -e "${YELLOW}[*]${NC} 安装新版本应用..."
        adb install -r "app/build/outputs/apk/debug/app-debug.apk"
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}[OK]${NC} 应用安装成功!"
            echo ""
            
            echo -e "${YELLOW}[*]${NC} 启动应用..."
            adb shell am start -n com.fengshui.app/.MainActivity
            
            echo ""
            echo -e "${GREEN}[OK]${NC} 部署完成!"
            echo -e "${YELLOW}[*]${NC} 显示日志... (Ctrl+C 停止)"
            echo ""
            
            adb logcat -s fengshui
        else
            echo -e "${RED}[ERROR]${NC} 应用安装失败!"
            exit 1
        fi
    else
        echo -e "${RED}[ERROR]${NC} 编译失败!"
        echo ""
        echo "请检查错误信息并修复代码。"
        exit 1
    fi
fi

echo ""
echo "================================"
echo "操作完成"
echo "================================"
echo ""
