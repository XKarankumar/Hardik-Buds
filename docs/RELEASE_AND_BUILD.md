# Setup, APK Build & CI/CD Instructions — Hardik Buds Pro

## Automatic GitHub Actions APK Builds (`XKarankumar/Hardik-Buds`)

This repository includes an automated CI/CD workflow at `.github/workflows/build-apk.yml` that builds **HARDIK BUDS PRO (ULTRA PREMIUM)** automatically on every push to `main` or manual `workflow_dispatch`.

### How to Download & Install the Built APK on Android
1. Open your GitHub repository: `https://github.com/XKarankumar/Hardik-Buds`.
2. Click the **Actions** tab at the top of the repository.
3. Click the latest **Build Hardik Buds Pro Debug APK** workflow run (with a green checkmark).
4. Scroll down to the **Artifacts** section at the bottom of the summary page.
5. Click **`Hardik-Buds-Pro-Ultra-Premium-Debug-APK`** to download the `.zip` archive.
6. Extract the `.zip` file on your Android phone (or PC and transfer via USB) to find `Hardik-Buds-Pro-Ultra-Premium-debug.apk`.
7. Tap the `.apk` file on your Android phone. If prompted, enable **Install unknown apps** for your file manager/browser and tap **Install**.

---

## Local Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 21 (Temurin recommended)
- Android SDK 36 (`minSdk = 24`, `targetSdk = 36`)
- Physical Android device recommended for live Bluetooth A2DP/BLE testing

## Opening in Android Studio
1. Clone `https://github.com/XKarankumar/Hardik-Buds.git`.
2. Open the root directory in Android Studio and let Gradle sync.
3. Connect an Android phone with USB Debugging enabled.
4. Click **Run** (`app` configuration).

## Building Debug APK via CLI
```bash
# Ensure debug.keystore exists (or decode from debug.keystore.base64)
base64 -d debug.keystore.base64 > debug.keystore

# Build Debug APK
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
