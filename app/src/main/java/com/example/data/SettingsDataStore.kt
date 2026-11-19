package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AncMode
import com.example.model.AnimationSpeedOption
import com.example.model.AppThemeId
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.CardAppearance
import com.example.model.ConnectionAnimStyle
import com.example.model.CustomizationSettings
import com.example.model.DarkModePreference
import com.example.model.FontScaleOption
import com.example.model.IconStyleOption
import com.example.model.OwnerSecuritySettings
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.budsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hardik_buds_pro_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        // Customization & Theme
        val THEME_ID = stringPreferencesKey("theme_id")
        val DARK_MODE = stringPreferencesKey("dark_mode")
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
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val SHOW_QUICK_ANC = booleanPreferencesKey("show_quick_anc")
        val SHOW_SUBWOOFER = booleanPreferencesKey("show_subwoofer")
        val SHOW_COMPATIBILITY = booleanPreferencesKey("show_compatibility")
        val SHOW_FIRMWARE = booleanPreferencesKey("show_firmware")
        val SHOW_MINI_VIZ = booleanPreferencesKey("show_mini_viz")
        val SHOW_NAV_LABELS = booleanPreferencesKey("show_nav_labels")

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
    }

    val customizationFlow: Flow<CustomizationSettings> = context.budsDataStore.data.map { p ->
        CustomizationSettings(
            themeId = enumOrDefault(p[Keys.THEME_ID], AppThemeId.BLACK_TITANIUM),
            darkMode = enumOrDefault(p[Keys.DARK_MODE], DarkModePreference.DARK),
            customAccentEnabled = p[Keys.CUSTOM_ACCENT_ENABLED] ?: false,
            customAccentRed = p[Keys.CUSTOM_ACCENT_R] ?: 216,
            customAccentGreen = p[Keys.CUSTOM_ACCENT_G] ?: 223,
            customAccentBlue = p[Keys.CUSTOM_ACCENT_B] ?: 233,
            rgbCycleEnabled = p[Keys.RGB_CYCLE] ?: false,
            cardAppearance = enumOrDefault(p[Keys.CARD_APPEARANCE], CardAppearance.GLASS),
            buttonStyle = enumOrDefault(p[Keys.BUTTON_STYLE], ButtonStyleOption.PILL),
            fontScale = enumOrDefault(p[Keys.FONT_SCALE], FontScaleOption.STANDARD),
            iconStyle = enumOrDefault(p[Keys.ICON_STYLE], IconStyleOption.FILLED),
            animationSpeed = enumOrDefault(p[Keys.ANIM_SPEED], AnimationSpeedOption.SMOOTH_1X),
            batteryRingStyle = enumOrDefault(p[Keys.BATTERY_RING_STYLE], BatteryRingStyle.ORBITAL),
            connectionAnimStyle = enumOrDefault(p[Keys.CONNECTION_ANIM_STYLE], ConnectionAnimStyle.SONIC_PULSE),
            customWallpaperUri = p[Keys.CUSTOM_WALLPAPER_URI],
            showQuickAncCard = p[Keys.SHOW_QUICK_ANC] ?: true,
            showSubwooferCard = p[Keys.SHOW_SUBWOOFER] ?: true,
            showCompatibilityCard = p[Keys.SHOW_COMPATIBILITY] ?: true,
            showFirmwareCard = p[Keys.SHOW_FIRMWARE] ?: true,
            showMiniVisualizerWidget = p[Keys.SHOW_MINI_VIZ] ?: true,
            showBottomNavLabels = p[Keys.SHOW_NAV_LABELS] ?: true,
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

    suspend fun updateCustomization(c: CustomizationSettings) {
        context.budsDataStore.edit { p ->
            p[Keys.THEME_ID] = c.themeId.name
            p[Keys.DARK_MODE] = c.darkMode.name
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
            if (c.customWallpaperUri != null) {
                p[Keys.CUSTOM_WALLPAPER_URI] = c.customWallpaperUri
            } else {
                p.remove(Keys.CUSTOM_WALLPAPER_URI)
            }
            p[Keys.SHOW_QUICK_ANC] = c.showQuickAncCard
            p[Keys.SHOW_SUBWOOFER] = c.showSubwooferCard
            p[Keys.SHOW_COMPATIBILITY] = c.showCompatibilityCard
            p[Keys.SHOW_FIRMWARE] = c.showFirmwareCard
            p[Keys.SHOW_MINI_VIZ] = c.showMiniVisualizerWidget
            p[Keys.SHOW_NAV_LABELS] = c.showBottomNavLabels
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

    suspend fun resetAllPreferences() {
        context.budsDataStore.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
