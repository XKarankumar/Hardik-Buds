package com.example.model

import androidx.compose.runtime.Immutable

enum class EarbudBrand(
    val displayName: String,
    val manufacturerIds: Set<Int> = emptySet(),
    val companionPackageHint: String? = null,
    val keywords: List<String> = emptyList(),
) {
    ONEPLUS(
        displayName = "OnePlus",
        manufacturerIds = setOf(0x072F, 0x0667),
        companionPackageHint = "com.oplus.melody",
        keywords = listOf("oneplus", "nord buds", "bullets"),
    ),
    SAMSUNG(
        displayName = "Samsung",
        manufacturerIds = setOf(0x0075),
        companionPackageHint = "com.samsung.android.app.watchmanager",
        keywords = listOf("galaxy buds", "samsung", "sm-r"),
    ),
    APPLE_AIRPODS(
        displayName = "Apple AirPods",
        manufacturerIds = setOf(0x004C),
        companionPackageHint = null,
        keywords = listOf("airpods", "powerbeats", "beats studio buds", "beats fit"),
    ),
    REALME(
        displayName = "Realme",
        manufacturerIds = setOf(0x072F),
        companionPackageHint = "com.realme.link",
        keywords = listOf("realme"),
    ),
    OPPO(
        displayName = "Oppo",
        manufacturerIds = setOf(0x072F, 0x0667),
        companionPackageHint = "com.heytap.headset",
        keywords = listOf("oppo", "enco"),
    ),
    XIAOMI(
        displayName = "Xiaomi",
        manufacturerIds = setOf(0x038F),
        companionPackageHint = "com.xiaomi.wearable",
        keywords = listOf("xiaomi", "mi buds", "mi true", "flipbuds"),
    ),
    REDMI(
        displayName = "Redmi",
        manufacturerIds = setOf(0x038F),
        companionPackageHint = "com.xiaomi.wearable",
        keywords = listOf("redmi", "airdots"),
    ),
    VIVO(
        displayName = "Vivo",
        manufacturerIds = setOf(0x08E1),
        companionPackageHint = "com.vivo.tws",
        keywords = listOf("vivo"),
    ),
    IQOO(
        displayName = "iQOO",
        manufacturerIds = setOf(0x08E1),
        companionPackageHint = "com.vivo.tws",
        keywords = listOf("iqoo"),
    ),
    NOTHING(
        displayName = "Nothing",
        manufacturerIds = setOf(0x0C3A),
        companionPackageHint = "com.nothing.smartcenter",
        keywords = listOf("nothing", "ear (1)", "ear (2)", "ear (a)", "ear (", "cmf buds"),
    ),
    JBL(
        displayName = "JBL",
        manufacturerIds = setOf(0x0057),
        companionPackageHint = "jbl.stc.com",
        keywords = listOf("jbl"),
    ),
    SONY(
        displayName = "Sony",
        manufacturerIds = setOf(0x012D),
        companionPackageHint = "com.sony.songpal.mdr",
        keywords = listOf("sony", "wf-", "wh-", "linkbuds"),
    ),
    BOAT(
        displayName = "boAt",
        manufacturerIds = emptySet(),
        companionPackageHint = "com.boat.hearables",
        keywords = listOf("boat", "airdopes", "immortal", "nirvana"),
    ),
    NOISE(
        displayName = "Noise",
        manufacturerIds = emptySet(),
        companionPackageHint = "com.noisefit",
        keywords = listOf("noise", "intellibuds", "aura buds"),
    ),
    HUAWEI(
        displayName = "Huawei",
        manufacturerIds = setOf(0x027D),
        companionPackageHint = "com.huawei.associate",
        keywords = listOf("huawei", "freebuds", "freelace"),
    ),
    GOOGLE_PIXEL_BUDS(
        displayName = "Google Pixel Buds",
        manufacturerIds = setOf(0x00E0),
        companionPackageHint = "com.google.android.apps.wearables.maestro.companion",
        keywords = listOf("pixel buds"),
    ),
    GENERIC(
        displayName = "Generic Bluetooth",
        manufacturerIds = emptySet(),
        companionPackageHint = null,
        keywords = emptyList(),
    ),
}

enum class CompatibilityTier(
    val level: Int,
    val title: String,
    val subtitle: String,
) {
    TIER_1_STANDARD(
        level = 1,
        title = "Tier 1 · Standard Bluetooth",
        subtitle = "A2DP Stereo Audio, HFP Calls, AVRCP Media & Android EQ",
    ),
    TIER_2_ENHANCED(
        level = 2,
        title = "Tier 2 · Enhanced Telemetry",
        subtitle = "Standard Audio + Battery Reporting & Multi-Effect DSP",
    ),
    TIER_3_PROPRIETARY(
        level = 3,
        title = "Tier 3 · Extended Protocol",
        subtitle = "Manufacturer GATT / Fast Pair / Companion Protocol Support",
    ),
}

enum class FeatureSupportLevel {
    SUPPORTED,
    ANDROID_DSP_ONLY,
    OS_OR_HARDWARE_RESTRICTED,
    PROPRIETARY_UNAVAILABLE,
}

@Immutable
data class FeatureSupportItem(
    val featureName: String,
    val supportLevel: FeatureSupportLevel,
    val detail: String,
)

@Immutable
data class DeviceCompatibilityReport(
    val brand: EarbudBrand,
    val tier: CompatibilityTier,
    val supportsStandardAudio: Boolean,
    val supportsBatteryReporting: Boolean,
    val supportsSeparateEarbudBatteries: Boolean,
    val supportsAndroidDspEq: Boolean,
    val supportsHardwareAncControl: Boolean,
    val supportsHardwareTouchRemap: Boolean,
    val supportsFirmwareRevisionRead: Boolean,
    val supportsLowLatencyHint: Boolean,
    val features: List<FeatureSupportItem>,
    val summaryNotice: String,
)

enum class BatteryDataSource {
    UNAVAILABLE,
    ANDROID_HFP_METADATA,
    BLE_GATT_BATTERY_SERVICE,
    ANDROID_METADATA_UNTETHERED,
}

@Immutable
data class DiscoveredAudioDevice(
    val address: String,
    val name: String,
    val alias: String? = null,
    val isBonded: Boolean = false,
    val isConnected: Boolean = false,
    val isA2dpConnected: Boolean = false,
    val isHeadsetConnected: Boolean = false,
    val isBleDevice: Boolean = false,
    val isFastPairSupported: Boolean = false,
    val fastPairModelId: String? = null,
    val isHomeTheatreSpeaker: Boolean = false,
    val rssiDbm: Int? = null,
    val deviceClassMajor: Int? = null,
    val brand: EarbudBrand = EarbudBrand.GENERIC,
    val compatibilityReport: DeviceCompatibilityReport,
    val batteryOverallPercent: Int? = null,
    val leftBatteryPercent: Int? = null,
    val rightBatteryPercent: Int? = null,
    val caseBatteryPercent: Int? = null,
    val batterySource: BatteryDataSource = BatteryDataSource.UNAVAILABLE,
    val firmwareRevision: String? = null,
    val modelNumber: String? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val lastKnownLocationLabel: String? = null,
) {
    val displayTitle: String
        get() = alias?.takeIf { it.isNotBlank() } ?: name.ifBlank { "Bluetooth Audio ($address)" }
}

@Immutable
data class AudioOutputRoute(
    val id: Int,
    val productName: String,
    val typeLabel: String,
    val address: String? = null,
    val isBluetooth: Boolean = false,
    val isHomeTheatreCapable: Boolean = false,
    val maxChannels: Int = 2,
    val sampleRateLabel: String = "48 kHz",
)

@Immutable
data class HomeTheatreSettings(
    val smoothFadeHandoff: Boolean = true,
    val autoCinemaSubwoofer: Boolean = true,
    val lfeCrossoverHz: Int = 80,
    val smoothTransitionMs: Int = 1200,
    val preferredTheatreAddress: String? = null,
    val preferredTheatreName: String? = null,
    val theatreConnectedActive: Boolean = false,
    val lastSmoothTransitionStatus: String = "Ready for smooth cinema audio handoff",
)

enum class AncMode(
    val title: String,
    val shortLabel: String,
    val description: String,
) {
    OFF("ANC Off", "Off", "Passive isolation only — saves maximum battery"),
    ANC_LOW("ANC Low", "Low", "Gentle attenuation for quiet offices & study"),
    ANC_MEDIUM("ANC Medium", "Medium", "Balanced cancellation for daily commutes"),
    ANC_HIGH("ANC High", "High", "Maximum low-frequency noise suppression"),
    ADAPTIVE_ANC("Adaptive ANC", "Adaptive", "Dynamic environment-aware noise control"),
    TRANSPARENCY("Transparency", "Transparent", "Natural external sound pass-through"),
    AMBIENT_SOUND("Ambient Sound", "Ambient", "Amplified surrounding awareness"),
    VOICE_AWARENESS("Voice Awareness", "Voice", "Prioritizes human speech frequencies"),
    WIND_REDUCTION("Wind Reduction", "Wind", "Minimizes wind turbulence microphone noise"),
    CUSTOM_ANC("Custom ANC", "Custom", "User-calibrated acoustic attenuation curve"),
}

enum class SoundMode(
    val title: String,
    val subtitle: String,
    val isSpatial: Boolean = false,
    val isLowLatency: Boolean = false,
) {
    MUSIC("Music Mode", "Studio-balanced clarity with crisp transients"),
    MOVIE("Movie Mode", "Wide cinema soundstage with deep sub-rumble", isSpatial = true),
    GAMING("Gaming Mode", "Low-latency routing & footstep detail", isLowLatency = true),
    BASS("Bass Mode", "High-impact sub-bass weight & warmth"),
    VOCAL("Vocal Mode", "Forward midrange presence & crisp dialogue"),
    SPATIAL_AUDIO("Spatial Audio", "3D psychoacoustic width & virtualizer", isSpatial = true),
    SURROUND_SOUND("Surround Sound", "7.1 virtual acoustic room immersion", isSpatial = true),
    NIGHT_MODE("Night Mode", "Gentle loudness compression & soft highs"),
    CUSTOM("Custom Sound", "User-sculpted EQ & Subwoofer Engine X"),
}

@Immutable
data class SubwooferConfig(
    val enabled: Boolean = true,
    val bassIntensity: Int = 55, // 0..100
    val subwooferSimulation: Int = 60, // 0..100
    val punchBass: Int = 50, // 0..100
    val deepBass: Int = 65, // 0..100
    val lowFreqCutoffHz: Int = 80, // 20..200
    val balance: Float = 0f, // -1.0 (L) .. +1.0 (R)
    val presetName: String = "Titanium Sub",
    val band20HzDb: Float = 4.5f,
    val band31HzDb: Float = 5.0f,
    val band40HzDb: Float = 4.0f,
    val band63HzDb: Float = 3.0f,
    val band80HzDb: Float = 2.0f,
    val band125HzDb: Float = 1.0f,
    val band200HzDb: Float = 0.0f,
) {
    fun subBandsList(): List<Pair<String, Float>> = listOf(
        "20Hz" to band20HzDb,
        "31Hz" to band31HzDb,
        "40Hz" to band40HzDb,
        "63Hz" to band63HzDb,
        "80Hz" to band80HzDb,
        "125Hz" to band125HzDb,
        "200Hz" to band200HzDb,
    )
}

@Immutable
data class EqualizerProfile(
    val id: String,
    val name: String,
    val bandsDb: List<Float>, // 10 bands: 31, 63, 125, 250, 500, 1k, 2k, 4k, 8k, 16k (-12f..+12f)
    val bands15Db: List<Float> = DspConfig.expand10To15(bandsDb),
    val subwooferBoost: Int = 50,
    val isBuiltIn: Boolean = false,
) {
    fun resolved15Bands(): List<Float> =
        if (bands15Db.size == 15) bands15Db else DspConfig.expand10To15(bandsDb)

    fun resolved10Bands(): List<Float> =
        if (bandsDb.size == 10) bandsDb else DspConfig.compress15To10(bands15Db)

    companion object {
        val BAND_LABELS = listOf("31Hz", "63Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    }
}

enum class TouchGesture(val label: String) {
    SINGLE_TAP("Single Tap"),
    DOUBLE_TAP("Double Tap"),
    TRIPLE_TAP("Triple Tap"),
    LONG_PRESS("Long Press"),
}

enum class TouchAction(val label: String) {
    PLAY_PAUSE("Play / Pause"),
    NEXT_TRACK("Next Track"),
    PREVIOUS_TRACK("Previous Track"),
    ANC_SWITCH("Cycle ANC / Transparency"),
    VOICE_ASSISTANT("Voice Assistant"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    NONE("Disabled"),
}

@Immutable
data class TouchControlConfig(
    val leftSingle: TouchAction = TouchAction.PLAY_PAUSE,
    val leftDouble: TouchAction = TouchAction.PREVIOUS_TRACK,
    val leftTriple: TouchAction = TouchAction.VOLUME_DOWN,
    val leftLong: TouchAction = TouchAction.ANC_SWITCH,
    val rightSingle: TouchAction = TouchAction.PLAY_PAUSE,
    val rightDouble: TouchAction = TouchAction.NEXT_TRACK,
    val rightTriple: TouchAction = TouchAction.VOLUME_UP,
    val rightLong: TouchAction = TouchAction.VOICE_ASSISTANT,
)

enum class AppThemeId(
    val displayName: String,
    val tagline: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val darkBgHex: Long,
    val darkSurfaceHex: Long,
    val lightBgHex: Long,
    val lightSurfaceHex: Long,
    val prefersLightMode: Boolean = false,
) {
    NOTHING_MONOCHROME(
        displayName = "Nothing Monochrome",
        tagline = "AMOLED Black · White · Signal Red",
        primaryHex = 0xFFFFFFFF,
        secondaryHex = 0xFFA1A1AA,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF000000,
        darkSurfaceHex = 0xFF0C0C0E,
        lightBgHex = 0xFFF7F7F8,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    BLACK(
        displayName = "Black",
        tagline = "Pure Obsidian · Crisp White",
        primaryHex = 0xFFF4F4F5,
        secondaryHex = 0xFF71717A,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF000000,
        darkSurfaceHex = 0xFF09090B,
        lightBgHex = 0xFFF4F4F5,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    WHITE(
        displayName = "White",
        tagline = "Minimal Paper · Ink · Signal Red",
        primaryHex = 0xFF111113,
        secondaryHex = 0xFF6B7280,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF0A0A0C,
        darkSurfaceHex = 0xFF141418,
        lightBgHex = 0xFFF6F6F7,
        lightSurfaceHex = 0xFFFFFFFF,
        prefersLightMode = true,
    ),
    RED(
        displayName = "Red",
        tagline = "Signal Red #FF3038 · Carbon Black",
        primaryHex = 0xFFFF3038,
        secondaryHex = 0xFFD4D4D8,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF050203,
        darkSurfaceHex = 0xFF12090B,
        lightBgHex = 0xFFFFF5F5,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    TITANIUM(
        displayName = "Titanium",
        tagline = "Industrial Titanium · Chrome · Red Dot",
        primaryHex = 0xFFD8DFE9,
        secondaryHex = 0xFF8A94A6,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF07080A,
        darkSurfaceHex = 0xFF12151B,
        lightBgHex = 0xFFF1F3F6,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    MIDNIGHT(
        displayName = "Midnight",
        tagline = "Deep Midnight Slate · Ice White",
        primaryHex = 0xFFE2E8F0,
        secondaryHex = 0xFF64748B,
        accentHex = 0xFF7EB6FF,
        darkBgHex = 0xFF04070F,
        darkSurfaceHex = 0xFF0D1322,
        lightBgHex = 0xFFEEF2F8,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    SILVER(
        displayName = "Silver",
        tagline = "Brushed Silver · Graphite · Red Accent",
        primaryHex = 0xFFE4E4E7,
        secondaryHex = 0xFF9CA3AF,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF0B0C0E,
        darkSurfaceHex = 0xFF17191E,
        lightBgHex = 0xFFF3F4F6,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    NEON(
        displayName = "Neon",
        tagline = "Cyber Matrix · High-Contrast Pulse",
        primaryHex = 0xFF00F0FF,
        secondaryHex = 0xFFA1A1AA,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF020305,
        darkSurfaceHex = 0xFF0B0E14,
        lightBgHex = 0xFFEDF8FA,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    AMOLED(
        displayName = "AMOLED",
        tagline = "Zero-Pixel True Black · Ultra Battery",
        primaryHex = 0xFFFFFFFF,
        secondaryHex = 0xFF8E8E93,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF000000,
        darkSurfaceHex = 0xFF000000,
        lightBgHex = 0xFFFAFAFA,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
    CUSTOM(
        displayName = "Custom",
        tagline = "User Accent RGB & Custom Matrix",
        primaryHex = 0xFFFFFFFF,
        secondaryHex = 0xFFA1A1AA,
        accentHex = 0xFFFF3038,
        darkBgHex = 0xFF000000,
        darkSurfaceHex = 0xFF0D0D10,
        lightBgHex = 0xFFF5F5F7,
        lightSurfaceHex = 0xFFFFFFFF,
    ),
}

enum class DotMatrixStyle(val label: String) {
    DOTO("Doto Matrix"),
    DOT_GOTHIC("DotGothic 16"),
    CLEAN_MONO("Clean Tech"),
}

enum class DarkModePreference(val label: String) {
    DARK("AMOLED Dark"),
    LIGHT("Minimal White"),
    SYSTEM("System"),
}

enum class CardAppearance(val label: String) {
    GLASS("Translucent Glass"),
    TITANIUM("AMOLED Hairline"),
    MINIMAL("Flat Matrix"),
}

enum class ButtonStyleOption(val label: String) {
    PILL("Pill"),
    SHARP("Industrial"),
    METALLIC("Outlined"),
}

enum class FontScaleOption(val label: String, val multiplier: Float) {
    COMPACT("Compact", 0.92f),
    STANDARD("Standard", 1.0f),
    LUXURY_LARGE("Large", 1.12f),
}

enum class IconStyleOption(val label: String) {
    FILLED("Geometric Filled"),
    OUTLINED("Minimal Line"),
    DUOTONE("Monochrome"),
}

enum class AnimationSpeedOption(val label: String, val factor: Float) {
    SMOOTH_1X("60–120Hz Smooth", 1.0f),
    ULTRA_1_5X("Fast Snappy", 0.65f),
    REDUCED("Battery Saver", 0.0f),
}

enum class BatteryRingStyle(val label: String) {
    ORBITAL("Circular Ring"),
    ARC("Dot Arc"),
    BAR("Matrix Bar"),
}

enum class ConnectionAnimStyle(val label: String) {
    SONIC_PULSE("Red Pulse"),
    ORBIT_GLOW("Radar Orbit"),
    MINIMAL("Minimal Dot"),
}

enum class AppLanguageOption(val label: String, val code: String) {
    ENGLISH("English (Global)", "en"),
    SYSTEM("Follow Device OS", "sys"),
    HINDI("हिन्दी (Hindi Label)", "hi"),
}

enum class UiModeOption(
    val label: String,
    val badge: String,
    val description: String,
) {
    STUDIO(
        label = "Studio OS",
        badge = "STUDIO",
        description = "Full Nothing OS command dashboard with all DSP, Fast Pair & telemetry cards",
    ),
    MINIMAL_ZEN(
        label = "Minimal Matrix",
        badge = "MINIMAL",
        description = "Clean AMOLED focus view with 3D earbuds visual, battery rings & quick ANC",
    ),
    PRO_TELEMETRY(
        label = "Pro Telemetry HUD",
        badge = "HUD",
        description = "Live engineering readout with BT RSSI, audio session, sample rate & DSP status",
    ),
    THEATRE_AMBIENT(
        label = "Theatre Cinema",
        badge = "CINEMA",
        description = "Spotlights Home Theatre Smooth Connect, LFE crossover & subwoofer wave",
    ),
    DRIVE_DOCK(
        label = "Drive / Dock",
        badge = "DOCK",
        description = "Oversized high-contrast tactile controls for desk dock or travel",
    ),
}

@Immutable
data class CustomizationSettings(
    val themeId: AppThemeId = AppThemeId.NOTHING_MONOCHROME,
    val darkMode: DarkModePreference = DarkModePreference.DARK,
    val dotMatrixStyle: DotMatrixStyle = DotMatrixStyle.DOTO,
    val uiMode: UiModeOption = UiModeOption.STUDIO,
    val customAccentEnabled: Boolean = false,
    val customAccentRed: Int = 255,
    val customAccentGreen: Int = 48,
    val customAccentBlue: Int = 56,
    val rgbCycleEnabled: Boolean = false,
    val cardAppearance: CardAppearance = CardAppearance.GLASS,
    val buttonStyle: ButtonStyleOption = ButtonStyleOption.PILL,
    val fontScale: FontScaleOption = FontScaleOption.STANDARD,
    val iconStyle: IconStyleOption = IconStyleOption.OUTLINED,
    val animationSpeed: AnimationSpeedOption = AnimationSpeedOption.SMOOTH_1X,
    val batteryRingStyle: BatteryRingStyle = BatteryRingStyle.ORBITAL,
    val connectionAnimStyle: ConnectionAnimStyle = ConnectionAnimStyle.SONIC_PULSE,
    val hapticEnabled: Boolean = true,
    val customWallpaperUri: String? = null,
    val showQuickAncCard: Boolean = true,
    val showSubwooferCard: Boolean = true,
    val showGamingCard: Boolean = true,
    val showCompatibilityCard: Boolean = true,
    val showFirmwareCard: Boolean = true,
    val showMiniVisualizerWidget: Boolean = true,
    val showBottomNavLabels: Boolean = true,
    val language: AppLanguageOption = AppLanguageOption.ENGLISH,
)

@Immutable
data class SmartSettings(
    val autoReconnect: Boolean = true,
    val autoConnectLastDevice: Boolean = false,
    val connectionNotifications: Boolean = true,
    val disconnectAlertNotifications: Boolean = true,
    val batteryAlertThreshold: Int = 20,
    val hearingProtectionEnabled: Boolean = true,
    val hearingWarningPercent: Int = 80,
    val gamingLowLatencyHint: Boolean = false,
    val backgroundMonitoringEnabled: Boolean = false,
    val recordLastLocation: Boolean = false,
)

@Immutable
data class OwnerSecuritySettings(
    val isOwnerRegistered: Boolean = false,
    val ownerName: String = "",
    val ownerContact: String = "",
    val registeredDeviceAddress: String? = null,
    val registeredDeviceName: String? = null,
    val pinEnabled: Boolean = false,
    val pinLength: Int = 4, // 4 or 6
    val pinHash: String = "",
    val recoveryQuestion: String = "",
    val recoveryAnswerHash: String = "",
    val lockAppOnLaunch: Boolean = false,
    val lockSensitiveSettings: Boolean = true,
    val alertOnUnauthorizedDevice: Boolean = true,
)

@Immutable
data class AudioEngineCapabilities(
    val equalizerSupported: Boolean = false,
    val dynamicsProcessingSupported: Boolean = false,
    val hardwareBandCount: Int = 0,
    val hardwareMinDb: Float = -12f,
    val hardwareMaxDb: Float = 12f,
    val bassBoostSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
    val loudnessSupported: Boolean = false,
    val visualizerActive: Boolean = false,
    val activeAudioSessionId: Int = 0,
    val outputSampleRateHz: Int = 48000,
    val outputRouteLabel: String = "System Audio Out",
    val engineStatusMessage: String = "Initializing Software PCM & Android AudioEffect DSP...",
    val limitationNotice: String = "Software PCM DSP processes internal streams; Android AudioEffect applies to supported system media streams.",
)

@Immutable
data class FirmwareCatalogEntry(
    val targetBrand: EarbudBrand,
    val latestVersion: String,
    val releaseDate: String,
    val changelog: List<String>,
    val sha256Checksum: String,
    val officialGuideUrl: String,
)

@Immutable
data class FirmwareUpdateState(
    val currentAppVersion: String = "1.0.0",
    val latestAppVersion: String = "1.0.0",
    val appUpdateNotes: List<String> = listOf(
        "Initial Ultra Premium Edition release",
        "Subwoofer Engine X 7-band sub-bass DSP pipeline",
        "17-brand Bluetooth & BLE compatibility detection",
        "10 luxury themes with RGB accent studio",
    ),
    val connectedDeviceFirmware: String? = null,
    val catalogEntry: FirmwareCatalogEntry? = null,
    val isChecking: Boolean = false,
    val lastCheckedTimestamp: Long? = null,
    val verificationStatus: String = "Ready to inspect connected BLE GATT Device Information (0x180A) & update channel.",
)
