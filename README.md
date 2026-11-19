# HARDIK BUDS PRO — ULTRA PREMIUM EDITION

**Repository:** `hardik-buds-pro`  
**Platform:** Android (Kotlin · Jetpack Compose · Material 3)  
**Developer:** Karan Kumar  
**Copyright:** © 2026 Hardik Kumar. All Rights Reserved.

---

## Overview

**Hardik Buds Pro** is a native Android Bluetooth earbuds companion application crafted with a **Black Titanium & Liquid Silver** luxury design language. It pairs authentic Android Bluetooth & BLE hardware telemetry with a real Android `AudioEffect` DSP pipeline (**Subwoofer Engine X** + **10-Band Pro Equalizer**), a **17-Brand Compatibility Engine**, **10 Luxury Themes**, **Owner Security**, and a **Play-compliant Firmware & App Update Center**.

> **Hardware Integrity Principle:** Hardik Buds Pro never fakes battery levels, Bluetooth connection states, ANC toggles, or touch-control remapping. Hardware capabilities are inspected via real Android Bluetooth profiles, BLE GATT services (`0x180F` Battery, `0x180A` Device Information), and `AudioEffect` HAL queries, with unsupported OEM protocols labeled transparently.

---

## Key Features

1. **Luxury Adaptive UI & 60–120Hz Motion**
   - Black Titanium & Silver glassmorphic design with custom bundled typography (`Outfit`, `Plus Jakarta Sans`, `JetBrains Mono`)
   - 3D floating earbuds showcase, orbital battery rings, and music-reactive bass pulse visualizer
   - Adaptive layouts (`NavigationBar` on phones, `NavigationRail` on tablets/foldables)
2. **Real Android Bluetooth & BLE Management**
   - Runtime permission flow for Android 12+ (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`), Notifications (`POST_NOTIFICATIONS`), optional Location, and optional Microphone FFT
   - Bonded device inspection, A2DP/HFP active link detection, BLE/Classic nearby scanning with RSSI (dBm)
   - Real hardware battery readout via HFP, Android untethered metadata (`L / R / Case`), and BLE GATT `0x180F`
   - Room-backed connection history & optional `connectedDevice` foreground monitor service
3. **SUBWOOFER ENGINE X & Pro 10-Band Equalizer**
   - Powered by Android `Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`, and optional `Visualizer`
   - 7 sub-bass control bands (`20Hz`, `31Hz`, `40Hz`, `63Hz`, `80Hz`, `125Hz`, `200Hz`), cutoff `20–200Hz`, L/R balance, and `0–100` intensity
   - 10-band EQ (`31Hz–16kHz`) with 14 presets (`Deep Bass`, `Subwoofer`, `EDM`, `Punjabi`, `Bollywood`, `Pop`, `Rock`, `Hip-Hop`, `Classical`, `Gaming`, `Cinema`, `Vocal Boost`, `Balanced`, `Custom`)
   - Save, rename, delete, and JSON import/export custom EQ profiles
   - Real `AudioTrack` PCM acoustic tests (Left, Right, Stereo, 25–160Hz Sub Sweep, Find Earbuds ping)
4. **10 Luxury Themes & Studio Customizer**
   - `Black Titanium`, `Midnight Blue`, `Neon Purple`, `Luxury Gold`, `Emerald Green`, `Red Phantom`, `Silver Chrome`, `Cosmic Space`, `Sunset Amber`, `Arctic Ice`
   - Custom RGB accent sliders + dynamic RGB cycle, Dark/Light/System modes, Photo Picker custom wallpaper, card/button/font/icon/animation/battery ring customization
5. **Owner Security & Firmware/App Update System**
   - Local SHA-256 salted 4/6-digit PIN app lock, PIN recovery question, device identity binding, and unrecognized device alerts
   - BLE GATT `0x180A -> 0x2A26` firmware revision inspection, integrity checksum verification, and official OEM companion support links

---

## Architecture

- **UI Layer:** Jetpack Compose + Material 3 (`ui/screens`, `ui/components`, `ui/theme`, `ui/navigation`)
- **ViewModel:** `BudsViewModel` (`StateFlow` + `collectAsStateWithLifecycle`)
- **Bluetooth Service:** `BluetoothController` + `BudsMonitorService`
- **Audio Engine:** `SubwooferAudioEngine`
- **Compatibility Engine:** `DeviceCompatibilityEngine` (17 brands, Tiers 1–3)
- **Persistence:** Room (`AppDatabase` — history, EQ profiles, device profiles, security logs) & Jetpack DataStore (`SettingsRepository`)
- **Permissions:** `PermissionManager`

---

## Visual Preview

| Asset | Purpose |
|---|---|
| `app/src/main/res/drawable/img_earbuds_hero.jpg` | 3D Black Titanium & Silver Earbuds Hero Render |
| `app/src/main/res/drawable/img_subwoofer_banner.jpg` | Subwoofer Engine X Acoustic Wave Sculpture |
| `app/src/main/res/drawable/ic_buds_pro_fg.jpg` | Custom Adaptive Launcher Emblem |

---

## Quick Setup & Build Instructions

See [`docs/RELEASE_AND_BUILD.md`](docs/RELEASE_AND_BUILD.md) for Android Studio setup, Gradle debug APK build commands, and release signing instructions.

```bash
# Build Debug APK
gradle :app:assembleDebug

# Run Unit & Robolectric UI Tests
gradle :app:testDebugUnitTest
```

See [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md) for the complete 17-brand Bluetooth Compatibility Matrix.
