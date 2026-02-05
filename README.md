# fengshui-tool

V0: Android FengShui compass prototype (Kotlin + Jetpack Compose).

Features:
- Map abstraction layer supporting vector/satellite toggle and zoom controls
- Real-time compass overlay (azimuth from device magnetometer/gyroscope)
- Single-case point-of-interest flows (add origin/destination, draw polylines)
- Persistent storage via SharedPreferences (JSON)
- Trial limits: 2 origins, 5 destinations (local registration to unlock)
- Click detection on polylines with line info display (bearing, 24-shan, bagua, wuxing, distance)

Quick Start

Prerequisites:
- Android SDK 34+
- Gradle 8.0+
- Kotlin 1.9+

Setup:
1. Install Android SDK: https://developer.android.com/studio
2. Set `ANDROID_HOME` environment variable or update `local.properties` with SDK path
3. Open project in Android Studio or use CLI

Build APK:
```bash
cd fengshui-tool
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Run on Emulator/Device:
```bash
./gradlew installDebug
adb shell am start -n com.fengshui.app/.MainActivity
```

Notes
- Map providers (`AMapProvider`, `GoogleMapProvider`) are placeholders; integrate real SDKs for full functionality
- Registration demo code: `TRIAL-UNLOCK-2026`
- Sensors: Rotation Vector preferred; falls back to Accelerometer + Magnetometer

Development
- Source: `app/src/main/java/com/fengshui/app/`
- Resources: `app/src/main/res/`
- Manifest: `app/src/main/AndroidManifest.xml`
