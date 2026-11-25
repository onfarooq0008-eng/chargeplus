# ChargePulse — Live Watt & Ampere Charging Meter

ChargePulse is an Android battery telemetry and charging diagnostics app built with **Kotlin + Jetpack Compose (Material 3)**, featuring a **Gitflow GitHub Actions workflow** that runs unit tests and generates **`app-debug.apk`** automatically.

## Features

- **Live Watt & Ampere Meter**
  - Real-time **Watts (`W`)**, **Amperes (`A` / `mA`)**, and **Voltage (`V`)** sampled from Android `BatteryManager` (`BATTERY_PROPERTY_CURRENT_NOW`, `BATTERY_PROPERTY_CURRENT_AVERAGE`, `EXTRA_VOLTAGE`).
  - Live dual-trace waveform chart plotting Watts and Amperes over time, plus session peak Watts and peak mA.
- **Charging Speed & Fast Charging Detection**
  - Detects plugged power source (`AC Wall Adapter`, `USB Port`, `Wireless Charger`, `Charging Dock`) and classifies live charging speed into **Slow / Trickle**, **Standard**, **Fast Charging**, and **Super Fast Charging** with configurable Watt thresholds.
  - Displays **Estimated Time to Full** (`BatteryManager.computeChargeTimeRemaining()` on Android 9+), **%/hr** charge rate, and negotiated max charger limit when exposed by the kernel.
- **Battery Health & Thermal Diagnostics**
  - Displays system battery health status (`Good`, `Overheat`, `Over Voltage`, `Cold`, etc.), real-time temperature (`°C` / `°F`), thermal tier (`Cool`, `Optimal`, `Warm`, `Hot`), cell chemistry, **Cycle Count** & **Hardware State of Health** (Android 14+ when exposed by hardware HAL), plus **Estimated Capacity Health %** calibrated against configurable design capacity (`mAh`).
- **Shows on Android Home Page When Charger Connects**
  - **Live Android Home Screen Widget (`4×2`)**: Automatically updates on `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED` and ticks live Watts, Amperes, Charging Speed / Fast Charging badge, Battery %, ETA, and Battery Health directly on your Android home screen (with one-tap **Add Widget** pinning from the app).
  - **Optional Home Page Floating HUD Overlay**: When **Display Over Other Apps** (`SYSTEM_ALERT_WINDOW`) is granted, plugging in your charger automatically pops up a draggable live Watt & Ampere HUD card over your Android home page.
  - **WorkManager Charging Trigger**: Uses Android 14+ compliant `WorkManager` (`setRequiresCharging(true)`) alongside `ChargingMonitorService` (`specialUse` foreground service) for reliable charger detection.

## Project Structure

```text
ChargePulse/
├── .github/workflows/build-debug-apk.yml   # Gitflow CI pipeline -> builds app-debug.apk
├── docs/GITFLOW.md                         # Gitflow branching & APK download guide
├── gradle/libs.versions.toml               # Version catalog (AGP 8.7.3, Kotlin 2.1.0, Compose BOM)
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/chargepulse/meter/
        │   │   ├── data/                   # BatteryMetricsCalculator, Reader, Repository, SettingsStore
        │   │   ├── model/                  # Telemetry, Settings, SessionStats models
        │   │   ├── receiver/               # BootCompletedReceiver
        │   │   ├── service/                # ChargingMonitorService, ChargingOverlayManager
        │   │   ├── ui/                     # Compose theme, ViewModel, Gauge, Waveform, 3 Screens
        │   │   ├── widget/                 # ChargeMeterWidgetProvider + RemoteViews helper
        │   │   └── worker/                 # PowerConnectionWorker (WorkManager)
        │   └── res/                        # Widget XML, Overlay HUD XML, adaptive icons, drawables
        └── test/java/com/chargepulse/meter/
            └── BatteryMetricsCalculatorTest.kt
```

## Building the Debug APK

### Option 1: Automated GitHub Actions (Gitflow CI)

Push to `main`, `develop`, `feature/**`, `release/**`, or `hotfix/**` (or trigger **Run workflow** manually under **Actions**). See [docs/GITFLOW.md](docs/GITFLOW.md) for branch conventions and downloading **`app-debug.apk`**.

### Option 2: Local Build (Android Studio or Gradle CLI)

1. Open `ChargePulse` in **Android Studio** (uses Gradle `8.10.2`, JDK `17`, `compileSdk = 35`), or ensure JDK 17 + Android SDK 35 + Gradle `8.10.2` are installed locally.
2. Run unit tests and assemble the Debug APK:
   ```bash
   gradle testDebugUnitTest assembleDebug
   ```
   *(If you prefer `./gradlew`, run `gradle wrapper` once first to generate `gradle-wrapper.jar` locally.)*
3. Find the output APK at:
   `app/build/outputs/apk/debug/app-debug.apk`
