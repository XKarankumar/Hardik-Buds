package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AncMode
import com.example.model.AnimationSpeedOption
import com.example.model.AppLanguageOption
import com.example.model.AppThemeId
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.CardAppearance
import com.example.model.ConnectionAnimStyle
import com.example.model.CustomizationSettings
import com.example.model.DarkModePreference
import com.example.model.DotMatrixStyle
import com.example.model.FontScaleOption
import com.example.model.HomeTheatreSettings
import com.example.model.IconStyleOption
import com.example.model.OwnerSecuritySettings
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.model.UiModeOption
import com.example.ota.OtaChannel
import com.example.ota.OtaCheckFrequencyHours
import com.example.ota.OtaSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.budsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hardik_buds_pro_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        // Customization & Theme
        val THEME_ID = stringPreferencesKey("theme_id")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DOT_MATRIX_STYLE = stringPreferencesKey("dot_matrix_style")
        val UI_MODE = stringPreferencesKey("ui_mode")
        val CUSTOM_ACCENT_ENABLED = booleanPreferencesKey("custom_accent_enabled")
        val CUSTOM_ACCENT_R = intPreferencesKey("custom_accent_r")
        val CUSTOM_ACCENT_G = intPreferencesKey("custom_accent_g")
        val CUSTOM_ACCENT_B = intPreferencesKey("custom_accent_b")
        val RGB_CYCLE = booleanPreferencesKey("rgb_cycle")
        val CARD_APPEARANCE = stringPreferencesKey("card_appearance")
        val BUTTON_STYLE = stringPreferencesKey("button_style")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val ANIM_SPEED = stringPreferencesKey("anim_speed")
        val BATTERY_RING_STYLE = stringPreferencesKey("battery_ring_style")
        val CONNECTION_ANIM_STYLE = stringPreferencesKey("connection_anim_style")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val SHOW_QUICK_ANC = booleanPreferencesKey("show_quick_anc")
        val SHOW_SUBWOOFER = booleanPreferencesKey("show_subwoofer")
        val SHOW_GAMING = booleanPreferencesKey("show_gaming")
        val SHOW_COMPATIBILITY = booleanPreferencesKey("show_compatibility")
        val SHOW_FIRMWARE = booleanPreferencesKey("show_firmware")
        val SHOW_MINI_VIZ = booleanPreferencesKey("show_mini_viz")
        val SHOW_NAV_LABELS = booleanPreferencesKey("show_nav_labels")
        val APP_LANGUAGE = stringPreferencesKey("app_language")

        // Audio / Subwoofer / Modes
        val SUB_ENABLED = booleanPreferencesKey("sub_enabled")
        val SUB_INTENSITY = intPreferencesKey("sub_intensity")
        val SUB_SIMULATION = intPreferencesKey("sub_simulation")
        val SUB_PUNCH = intPreferencesKey("sub_punch")
        val SUB_DEEP = intPreferencesKey("sub_deep")
        val SUB_CUTOFF_HZ = intPreferencesKey("sub_cutoff_hz")
        val SUB_BALANCE = floatPreferencesKey("sub_balance")
        val SUB_PRESET_NAME = stringPreferencesKey("sub_preset_name")
        val SUB_B20 = floatPreferencesKey("sub_b20")
        val SUB_B31 = floatPreferencesKey("sub_b31")
        val SUB_B40 = floatPreferencesKey("sub_b40")
        val SUB_B63 = floatPreferencesKey("sub_b63")
        val SUB_B80 = floatPreferencesKey("sub_b80")
        val SUB_B125 = floatPreferencesKey("sub_b125")
        val SUB_B200 = floatPreferencesKey("sub_b200")
        val ACTIVE_EQ_ID = stringPreferencesKey("active_eq_id")
        val ACTIVE_SOUND_MODE = stringPreferencesKey("active_sound_mode")
        val PREFERRED_ANC_MODE = stringPreferencesKey("preferred_anc_mode")

        // Smart Features
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val AUTO_CONNECT_LAST = booleanPreferencesKey("auto_connect_last")
        val CONN_NOTIFICATIONS = booleanPreferencesKey("conn_notifications")
        val DISCONNECT_NOTIFICATIONS = booleanPreferencesKey("disconnect_notifications")
        val BATTERY_ALERT_THRESHOLD = intPreferencesKey("battery_alert_threshold")
        val HEARING_PROTECTION = booleanPreferencesKey("hearing_protection")
        val HEARING_WARNING_PCT = intPreferencesKey("hearing_warning_pct")
        val GAMING_LOW_LATENCY = booleanPreferencesKey("gaming_low_latency")
        val BACKGROUND_MONITOR = booleanPreferencesKey("background_monitor")
        val RECORD_LAST_LOCATION = booleanPreferencesKey("record_last_location")
        val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")

        // Owner Security
        val OWNER_REGISTERED = booleanPreferencesKey("owner_registered")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val OWNER_CONTACT = stringPreferencesKey("owner_contact")
        val OWNER_DEVICE_ADDR = stringPreferencesKey("owner_device_addr")
        val OWNER_DEVICE_NAME = stringPreferencesKey("owner_device_name")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val RECOVERY_QUESTION = stringPreferencesKey("recovery_question")
        val RECOVERY_ANSWER_HASH = stringPreferencesKey("recovery_answer_hash")
        val LOCK_ON_LAUNCH = booleanPreferencesKey("lock_on_launch")
        val LOCK_SENSITIVE = booleanPreferencesKey("lock_sensitive")
        val ALERT_UNAUTHORIZED = booleanPreferencesKey("alert_unauthorized")

        // Home Theatre Smooth Connection
        val HT_SMOOTH_FADE = booleanPreferencesKey("ht_smooth_fade")
        val HT_AUTO_CINEMA = booleanPreferencesKey("ht_auto_cinema")
        val HT_LFE_HZ = intPreferencesKey("ht_lfe_hz")
        val HT_TRANSITION_MS = intPreferencesKey("ht_transition_ms")
        val HT_PREFERRED_ADDR = stringPreferencesKey("ht_preferred_addr")
        val HT_PREFERRED_NAME = stringPreferencesKey("ht_preferred_name")

        // OTA Auto Update System
        val OTA_GITHUB_OWNER = stringPreferencesKey("ota_github_owner")
        val OTA_GITHUB_REPO = stringPreferencesKey("ota_github_repo")
        val OTA_AUTO_LAUNCH = booleanPreferencesKey("ota_auto_launch")
        val OTA_BG_CHECK = booleanPreferencesKey("ota_bg_check")
        val OTA_FREQ = stringPreferencesKey("ota_freq")
        val OTA_CHANNEL = stringPreferencesKey("ota_channel")
        val OTA_NOTIFICATIONS = booleanPreferencesKey("ota_notifications")
        val OTA_LAST_CHECKED = longPreferencesKey("ota_last_checked")
        val OTA_LAST_VERSION = stringPreferencesKey("ota_last_version")
        val OTA_SHA256_OVERRIDE = stringPreferencesKey("ota_sha256_override")
    }

    val customizationFlow: Flow<CustomizationSettings> = context.budsDataStore.data.map { p ->
        CustomizationSettings(
            themeId = enumOrDefault(p[Keys.THEME_ID], AppThemeId.NOTHING_MONOCHROME),
            darkMode = enumOrDefault(p[Keys.DARK_MODE], DarkModePreference.DARK),
            dotMatrixStyle = enumOrDefault(p[Keys.DOT_MATRIX_STYLE], DotMatrixStyle.DOTO),
            uiMode = enumOrDefault(p[Keys.UI_MODE], UiModeOption.STUDIO),
            customAccentEnabled = p[Keys.CUSTOM_ACCENT_ENABLED] ?: false,
            customAccentRed = p[Keys.CUSTOM_ACCENT_R] ?: 255,
            customAccentGreen = p[Keys.CUSTOM_ACCENT_G] ?: 48,
            customAccentBlue = p[Keys.CUSTOM_ACCENT_B] ?: 56,
            rgbCycleEnabled = p[Keys.RGB_CYCLE] ?: false,
            cardAppearance = enumOrDefault(p[Keys.CARD_APPEARANCE], CardAppearance.GLASS),
            buttonStyle = enumOrDefault(p[Keys.BUTTON_STYLE], ButtonStyleOption.PILL),
            fontScale = enumOrDefault(p[Keys.FONT_SCALE], FontScaleOption.STANDARD),
            iconStyle = enumOrDefault(p[Keys.ICON_STYLE], IconStyleOption.OUTLINED),
            animationSpeed = enumOrDefault(p[Keys.ANIM_SPEED], AnimationSpeedOption.SMOOTH_1X),
            batteryRingStyle = enumOrDefault(p[Keys.BATTERY_RING_STYLE], BatteryRingStyle.ORBITAL),
            connectionAnimStyle = enumOrDefault(p[Keys.CONNECTION_ANIM_STYLE], ConnectionAnimStyle.SONIC_PULSE),
            hapticEnabled = p[Keys.HAPTIC_ENABLED] ?: true,
            customWallpaperUri = p[Keys.CUSTOM_WALLPAPER_URI],
            showQuickAncCard = p[Keys.SHOW_QUICK_ANC] ?: true,
            showSubwooferCard = p[Keys.SHOW_SUBWOOFER] ?: true,
            showGamingCard = p[Keys.SHOW_GAMING] ?: true,
            showCompatibilityCard = p[Keys.SHOW_COMPATIBILITY] ?: true,
            showFirmwareCard = p[Keys.SHOW_FIRMWARE] ?: true,
            showMiniVisualizerWidget = p[Keys.SHOW_MINI_VIZ] ?: true,
            showBottomNavLabels = p[Keys.SHOW_NAV_LABELS] ?: true,
            language = enumOrDefault(p[Keys.APP_LANGUAGE], AppLanguageOption.ENGLISH),
        )
    }

    val subwooferConfigFlow: Flow<SubwooferConfig> = context.budsDataStore.data.map { p ->
        SubwooferConfig(
            enabled = p[Keys.SUB_ENABLED] ?: true,
            bassIntensity = p[Keys.SUB_INTENSITY] ?: 55,
            subwooferSimulation = p[Keys.SUB_SIMULATION] ?: 60,
            punchBass = p[Keys.SUB_PUNCH] ?: 50,
            deepBass = p[Keys.SUB_DEEP] ?: 65,
            lowFreqCutoffHz = p[Keys.SUB_CUTOFF_HZ] ?: 80,
            balance = p[Keys.SUB_BALANCE] ?: 0f,
            presetName = p[Keys.SUB_PRESET_NAME] ?: "Titanium Sub",
            band20HzDb = p[Keys.SUB_B20] ?: 4.5f,
            band31HzDb = p[Keys.SUB_B31] ?: 5.0f,
            band40HzDb = p[Keys.SUB_B40] ?: 4.0f,
            band63HzDb = p[Keys.SUB_B63] ?: 3.0f,
            band80HzDb = p[Keys.SUB_B80] ?: 2.0f,
            band125HzDb = p[Keys.SUB_B125] ?: 1.0f,
            band200HzDb = p[Keys.SUB_B200] ?: 0.0f,
        )
    }

    val activeEqIdFlow: Flow<String> = context.budsDataStore.data.map { p ->
        p[Keys.ACTIVE_EQ_ID] ?: "preset_balanced"
    }

    val activeSoundModeFlow: Flow<SoundMode> = context.budsDataStore.data.map { p ->
        enumOrDefault(p[Keys.ACTIVE_SOUND_MODE], SoundMode.MUSIC)
    }

    val preferredAncModeFlow: Flow<AncMode> = context.budsDataStore.data.map { p ->
        enumOrDefault(p[Keys.PREFERRED_ANC_MODE], AncMode.OFF)
    }

    val smartSettingsFlow: Flow<SmartSettings> = context.budsDataStore.data.map { p ->
        SmartSettings(
            autoReconnect = p[Keys.AUTO_RECONNECT] ?: true,
            autoConnectLastDevice = p[Keys.AUTO_CONNECT_LAST] ?: false,
            connectionNotifications = p[Keys.CONN_NOTIFICATIONS] ?: true,
            disconnectAlertNotifications = p[Keys.DISCONNECT_NOTIFICATIONS] ?: true,
            batteryAlertThreshold = p[Keys.BATTERY_ALERT_THRESHOLD] ?: 20,
            hearingProtectionEnabled = p[Keys.HEARING_PROTECTION] ?: true,
            hearingWarningPercent = p[Keys.HEARING_WARNING_PCT] ?: 80,
            gamingLowLatencyHint = p[Keys.GAMING_LOW_LATENCY] ?: false,
            backgroundMonitoringEnabled = p[Keys.BACKGROUND_MONITOR] ?: false,
            recordLastLocation = p[Keys.RECORD_LAST_LOCATION] ?: false,
        )
    }

    val ownerSecurityFlow: Flow<OwnerSecuritySettings> = context.budsDataStore.data.map { p ->
        OwnerSecuritySettings(
            isOwnerRegistered = p[Keys.OWNER_REGISTERED] ?: false,
            ownerName = p[Keys.OWNER_NAME] ?: "",
            ownerContact = p[Keys.OWNER_CONTACT] ?: "",
            registeredDeviceAddress = p[Keys.OWNER_DEVICE_ADDR],
            registeredDeviceName = p[Keys.OWNER_DEVICE_NAME],
            pinEnabled = p[Keys.PIN_ENABLED] ?: false,
            pinLength = p[Keys.PIN_LENGTH] ?: 4,
            pinHash = p[Keys.PIN_HASH] ?: "",
            recoveryQuestion = p[Keys.RECOVERY_QUESTION] ?: "",
            recoveryAnswerHash = p[Keys.RECOVERY_ANSWER_HASH] ?: "",
            lockAppOnLaunch = p[Keys.LOCK_ON_LAUNCH] ?: false,
            lockSensitiveSettings = p[Keys.LOCK_SENSITIVE] ?: true,
            alertOnUnauthorizedDevice = p[Keys.ALERT_UNAUTHORIZED] ?: true,
        )
    }

    val lastDeviceAddressFlow: Flow<String?> = context.budsDataStore.data.map { p ->
        p[Keys.LAST_DEVICE_ADDRESS]
    }

    val homeTheatreSettingsFlow: Flow<HomeTheatreSettings> = context.budsDataStore.data.map { p ->
        HomeTheatreSettings(
            smoothFadeHandoff = p[Keys.HT_SMOOTH_FADE] ?: true,
            autoCinemaSubwoofer = p[Keys.HT_AUTO_CINEMA] ?: true,
            lfeCrossoverHz = p[Keys.HT_LFE_HZ] ?: 80,
            smoothTransitionMs = p[Keys.HT_TRANSITION_MS] ?: 1200,
            preferredTheatreAddress = p[Keys.HT_PREFERRED_ADDR],
            preferredTheatreName = p[Keys.HT_PREFERRED_NAME],
        )
    }

    val otaSettingsFlow: Flow<OtaSettings> = context.budsDataStore.data.map { p ->
        OtaSettings(
            githubOwner = p[Keys.OTA_GITHUB_OWNER] ?: "XKarankumar",
            githubRepo = p[Keys.OTA_GITHUB_REPO] ?: "Hardik-Buds",
            autoCheckOnLaunch = p[Keys.OTA_AUTO_LAUNCH] ?: true,
            backgroundCheckEnabled = p[Keys.OTA_BG_CHECK] ?: true,
            checkFrequency = enumOrDefault(p[Keys.OTA_FREQ], OtaCheckFrequencyHours.HOURS_24),
            channel = enumOrDefault(p[Keys.OTA_CHANNEL], OtaChannel.STABLE),
            notificationsEnabled = p[Keys.OTA_NOTIFICATIONS] ?: true,
            lastCheckedTimestamp = p[Keys.OTA_LAST_CHECKED],
            lastKnownLatestVersion = p[Keys.OTA_LAST_VERSION],
            expectedSha256Override = p[Keys.OTA_SHA256_OVERRIDE] ?: "",
        )
    }

    suspend fun updateCustomization(c: CustomizationSettings) {
        context.budsDataStore.edit { p ->
            p[Keys.THEME_ID] = c.themeId.name
            p[Keys.DARK_MODE] = c.darkMode.name
            p[Keys.DOT_MATRIX_STYLE] = c.dotMatrixStyle.name
            p[Keys.UI_MODE] = c.uiMode.name
            p[Keys.CUSTOM_ACCENT_ENABLED] = c.customAccentEnabled
            p[Keys.CUSTOM_ACCENT_R] = c.customAccentRed
            p[Keys.CUSTOM_ACCENT_G] = c.customAccentGreen
            p[Keys.CUSTOM_ACCENT_B] = c.customAccentBlue
            p[Keys.RGB_CYCLE] = c.rgbCycleEnabled
            p[Keys.CARD_APPEARANCE] = c.cardAppearance.name
            p[Keys.BUTTON_STYLE] = c.buttonStyle.name
            p[Keys.FONT_SCALE] = c.fontScale.name
            p[Keys.ICON_STYLE] = c.iconStyle.name
            p[Keys.ANIM_SPEED] = c.animationSpeed.name
            p[Keys.BATTERY_RING_STYLE] = c.batteryRingStyle.name
            p[Keys.CONNECTION_ANIM_STYLE] = c.connectionAnimStyle.name
            p[Keys.HAPTIC_ENABLED] = c.hapticEnabled
            if (c.customWallpaperUri != null) {
                p[Keys.CUSTOM_WALLPAPER_URI] = c.customWallpaperUri
            } else {
                p.remove(Keys.CUSTOM_WALLPAPER_URI)
            }
            p[Keys.SHOW_QUICK_ANC] = c.showQuickAncCard
            p[Keys.SHOW_SUBWOOFER] = c.showSubwooferCard
            p[Keys.SHOW_GAMING] = c.showGamingCard
            p[Keys.SHOW_COMPATIBILITY] = c.showCompatibilityCard
            p[Keys.SHOW_FIRMWARE] = c.showFirmwareCard
            p[Keys.SHOW_MINI_VIZ] = c.showMiniVisualizerWidget
            p[Keys.SHOW_NAV_LABELS] = c.showBottomNavLabels
            p[Keys.APP_LANGUAGE] = c.language.name
        }
    }

    suspend fun updateSubwooferConfig(s: SubwooferConfig) {
        context.budsDataStore.edit { p ->
            p[Keys.SUB_ENABLED] = s.enabled
            p[Keys.SUB_INTENSITY] = s.bassIntensity
            p[Keys.SUB_SIMULATION] = s.subwooferSimulation
            p[Keys.SUB_PUNCH] = s.punchBass
            p[Keys.SUB_DEEP] = s.deepBass
            p[Keys.SUB_CUTOFF_HZ] = s.lowFreqCutoffHz
            p[Keys.SUB_BALANCE] = s.balance
            p[Keys.SUB_PRESET_NAME] = s.presetName
            p[Keys.SUB_B20] = s.band20HzDb
            p[Keys.SUB_B31] = s.band31HzDb
            p[Keys.SUB_B40] = s.band40HzDb
            p[Keys.SUB_B63] = s.band63HzDb
            p[Keys.SUB_B80] = s.band80HzDb
            p[Keys.SUB_B125] = s.band125HzDb
            p[Keys.SUB_B200] = s.band200HzDb
        }
    }

    suspend fun setActiveEqId(id: String) {
        context.budsDataStore.edit { it[Keys.ACTIVE_EQ_ID] = id }
    }

    suspend fun setActiveSoundMode(mode: SoundMode) {
        context.budsDataStore.edit { it[Keys.ACTIVE_SOUND_MODE] = mode.name }
    }

    suspend fun setPreferredAncMode(mode: AncMode) {
        context.budsDataStore.edit { it[Keys.PREFERRED_ANC_MODE] = mode.name }
    }

    suspend fun updateSmartSettings(s: SmartSettings) {
        context.budsDataStore.edit { p ->
            p[Keys.AUTO_RECONNECT] = s.autoReconnect
            p[Keys.AUTO_CONNECT_LAST] = s.autoConnectLastDevice
            p[Keys.CONN_NOTIFICATIONS] = s.connectionNotifications
            p[Keys.DISCONNECT_NOTIFICATIONS] = s.disconnectAlertNotifications
            p[Keys.BATTERY_ALERT_THRESHOLD] = s.batteryAlertThreshold
            p[Keys.HEARING_PROTECTION] = s.hearingProtectionEnabled
            p[Keys.HEARING_WARNING_PCT] = s.hearingWarningPercent
            p[Keys.GAMING_LOW_LATENCY] = s.gamingLowLatencyHint
            p[Keys.BACKGROUND_MONITOR] = s.backgroundMonitoringEnabled
            p[Keys.RECORD_LAST_LOCATION] = s.recordLastLocation
        }
    }

    suspend fun setLastDeviceAddress(address: String) {
        context.budsDataStore.edit { it[Keys.LAST_DEVICE_ADDRESS] = address }
    }

    suspend fun updateOwnerSecurity(o: OwnerSecuritySettings) {
        context.budsDataStore.edit { p ->
            p[Keys.OWNER_REGISTERED] = o.isOwnerRegistered
            p[Keys.OWNER_NAME] = o.ownerName
            p[Keys.OWNER_CONTACT] = o.ownerContact
            if (o.registeredDeviceAddress != null) {
                p[Keys.OWNER_DEVICE_ADDR] = o.registeredDeviceAddress
            } else {
                p.remove(Keys.OWNER_DEVICE_ADDR)
            }
            if (o.registeredDeviceName != null) {
                p[Keys.OWNER_DEVICE_NAME] = o.registeredDeviceName
            } else {
                p.remove(Keys.OWNER_DEVICE_NAME)
            }
            p[Keys.PIN_ENABLED] = o.pinEnabled
            p[Keys.PIN_LENGTH] = o.pinLength
            p[Keys.PIN_HASH] = o.pinHash
            p[Keys.RECOVERY_QUESTION] = o.recoveryQuestion
            p[Keys.RECOVERY_ANSWER_HASH] = o.recoveryAnswerHash
            p[Keys.LOCK_ON_LAUNCH] = o.lockAppOnLaunch
            p[Keys.LOCK_SENSITIVE] = o.lockSensitiveSettings
            p[Keys.ALERT_UNAUTHORIZED] = o.alertOnUnauthorizedDevice
        }
    }

    suspend fun updateHomeTheatreSettings(h: HomeTheatreSettings) {
        context.budsDataStore.edit { p ->
            p[Keys.HT_SMOOTH_FADE] = h.smoothFadeHandoff
            p[Keys.HT_AUTO_CINEMA] = h.autoCinemaSubwoofer
            p[Keys.HT_LFE_HZ] = h.lfeCrossoverHz
            p[Keys.HT_TRANSITION_MS] = h.smoothTransitionMs
            if (h.preferredTheatreAddress != null) {
                p[Keys.HT_PREFERRED_ADDR] = h.preferredTheatreAddress
            } else {
                p.remove(Keys.HT_PREFERRED_ADDR)
            }
            if (h.preferredTheatreName != null) {
                p[Keys.HT_PREFERRED_NAME] = h.preferredTheatreName
            } else {
                p.remove(Keys.HT_PREFERRED_NAME)
            }
        }
    }

    suspend fun updateOtaSettings(o: OtaSettings) {
        context.budsDataStore.edit { p ->
            p[Keys.OTA_GITHUB_OWNER] = o.githubOwner.trim().ifBlank { "XKarankumar" }
            p[Keys.OTA_GITHUB_REPO] = o.githubRepo.trim().ifBlank { "Hardik-Buds" }
            p[Keys.OTA_AUTO_LAUNCH] = o.autoCheckOnLaunch
            p[Keys.OTA_BG_CHECK] = o.backgroundCheckEnabled
            p[Keys.OTA_FREQ] = o.checkFrequency.name
            p[Keys.OTA_CHANNEL] = o.channel.name
            p[Keys.OTA_NOTIFICATIONS] = o.notificationsEnabled
            if (o.lastCheckedTimestamp != null) {
                p[Keys.OTA_LAST_CHECKED] = o.lastCheckedTimestamp
            }
            if (o.lastKnownLatestVersion != null) {
                p[Keys.OTA_LAST_VERSION] = o.lastKnownLatestVersion
            } else {
                p.remove(Keys.OTA_LAST_VERSION)
            }
            p[Keys.OTA_SHA256_OVERRIDE] = o.expectedSha256Override.trim().lowercase()
        }
    }

    suspend fun resetAllPreferences() {
        context.budsDataStore.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
