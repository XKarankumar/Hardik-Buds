# Setup, APK Build & Release Instructions — Hardik Buds Pro

## Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 11+
- Android SDK 36 (`minSdk = 24`, `targetSdk = 36`)
- Physical Android device recommended for live Bluetooth A2DP/BLE testing

## Opening in Android Studio
1. Clone the `hardik-buds-pro` repository.
2. Open the root directory in Android Studio and let Gradle sync.
3. Connect an Android phone with USB Debugging enabled (or use an Android 14+ device).
4. Click **Run** (`app` configuration).

## Building Debug APK via CLI
```bash
gradle :app:assembleDebug
```
Output APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Building Release APK / AAB
Configure your signing environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) and run:
```bash
gradle :app:assembleRelease
gradle :app:bundleRelease
```

## Running Tests
```bash
gradle :app:testDebugUnitTest
```
