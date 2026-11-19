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
import com.example.model.DspConfig
import com.example.model.EqBandMode
import com.example.model.FilterSlope
import com.example.model.HeadphoneSoundProfile
import com.example.model.ParametricEqNode
import com.example.ota.OtaChannel
import com.example.ota.OtaCheckFrequencyHours
import com.example.ota.OtaSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

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

        // Real Software DSP Engine Configuration
        val DSP_EQ_BAND_MODE = stringPreferencesKey("dsp_eq_band_mode")
        val DSP_BANDS_15_CSV = stringPreferencesKey("dsp_bands_15_csv")
        val DSP_PARAMETRIC_JSON = stringPreferencesKey("dsp_parametric_json")
        val DSP_BASS_ENABLED = booleanPreferencesKey("dsp_bass_enabled")
        val DSP_DEEP_BASS = intPreferencesKey("dsp_deep_bass")
        val DSP_SUB_BASS = intPreferencesKey("dsp_sub_bass")
        val DSP_BASS_PUNCH = intPreferencesKey("dsp_bass_punch")
        val DSP_BASS_WARMTH = intPreferencesKey("dsp_bass_warmth")
        val DSP_BASS_TEXTURE = intPreferencesKey("dsp_bass_texture")
        val DSP_SUB_CROSSOVER_HZ = floatPreferencesKey("dsp_sub_crossover_hz")
        val DSP_HPF_ENABLED = booleanPreferencesKey("dsp_hpf_enabled")
        val DSP_HPF_CUTOFF_HZ = floatPreferencesKey("dsp_hpf_cutoff_hz")
        val DSP_HPF_SLOPE = stringPreferencesKey("dsp_hpf_slope")
        val DSP_LPF_ENABLED = booleanPreferencesKey("dsp_lpf_enabled")
        val DSP_LPF_CUTOFF_HZ = floatPreferencesKey("dsp_lpf_cutoff_hz")
        val DSP_LPF_SLOPE = stringPreferencesKey("dsp_lpf_slope")
        val DSP_STEREO_BALANCE = floatPreferencesKey("dsp_stereo_balance")
        val DSP_LEFT_TRIM_DB = floatPreferencesKey("dsp_left_trim_db")
        val DSP_RIGHT_TRIM_DB = floatPreferencesKey("dsp_right_trim_db")
        val DSP_STEREO_WIDTH_PCT = intPreferencesKey("dsp_stereo_width_pct")
        val DSP_PREAMP_DB = floatPreferencesKey("dsp_preamp_db")
        val DSP_MASTER_GAIN_DB = floatPreferencesKey("dsp_master_gain_db")
        val DSP_LIMITER_ENABLED = booleanPreferencesKey("dsp_limiter_enabled")
        val DSP_CLIPPING_PREV = booleanPreferencesKey("dsp_clipping_prev")
        val DSP_VOL_NORM = booleanPreferencesKey("dsp_vol_norm")
        val DSP_HEADPHONE_PROFILE = stringPreferencesKey("dsp_headphone_profile")
        val DSP_AUTO_SWITCH = booleanPreferencesKey("dsp_auto_switch")
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

    val dspConfigFlow: Flow<DspConfig> = context.budsDataStore.data.map { p ->
        val raw15 = p[Keys.DSP_BANDS_15_CSV]
            ?.split(",")
            ?.mapNotNull { it.trim().toFloatOrNull() }
            ?.take(15)
        val bands15 = if (raw15 != null && raw15.size == 15) raw15 else List(15) { 0f }

        val peq = parseParametricNodes(p[Keys.DSP_PARAMETRIC_JSON])

        DspConfig(
            eqBandMode = enumOrDefault(p[Keys.DSP_EQ_BAND_MODE], EqBandMode.BANDS_15),
            bands15Db = bands15,
            parametricNodes = peq,
            bassEngineEnabled = p[Keys.DSP_BASS_ENABLED] ?: (p[Keys.SUB_ENABLED] ?: true),
            deepBassPercent = p[Keys.DSP_DEEP_BASS] ?: (p[Keys.SUB_DEEP] ?: 55),
            subBassPercent = p[Keys.DSP_SUB_BASS] ?: 50,
            bassPunchPercent = p[Keys.DSP_BASS_PUNCH] ?: (p[Keys.SUB_PUNCH] ?: 45),
            bassWarmthPercent = p[Keys.DSP_BASS_WARMTH] ?: 40,
            bassTexturePercent = p[Keys.DSP_BASS_TEXTURE] ?: 30,
            subCrossoverHz = p[Keys.DSP_SUB_CROSSOVER_HZ] ?: 95f,
            highPassEnabled = p[Keys.DSP_HPF_ENABLED] ?: false,
            highPassCutoffHz = p[Keys.DSP_HPF_CUTOFF_HZ] ?: 30f,
            highPassSlope = enumOrDefault(p[Keys.DSP_HPF_SLOPE], FilterSlope.SLOPE_12DB),
            lowPassEnabled = p[Keys.DSP_LPF_ENABLED] ?: false,
            lowPassCutoffHz = p[Keys.DSP_LPF_CUTOFF_HZ] ?: 18000f,
            lowPassSlope = enumOrDefault(p[Keys.DSP_LPF_SLOPE], FilterSlope.SLOPE_12DB),
            stereoBalance = p[Keys.DSP_STEREO_BALANCE] ?: (p[Keys.SUB_BALANCE] ?: 0f),
            leftChannelGainDb = p[Keys.DSP_LEFT_TRIM_DB] ?: 0f,
            rightChannelGainDb = p[Keys.DSP_RIGHT_TRIM_DB] ?: 0f,
            stereoWidthPercent = p[Keys.DSP_STEREO_WIDTH_PCT] ?: 100,
            preampDb = p[Keys.DSP_PREAMP_DB] ?: 0f,
            masterGainDb = p[Keys.DSP_MASTER_GAIN_DB] ?: 0f,
            limiterEnabled = p[Keys.DSP_LIMITER_ENABLED] ?: true,
            clippingPreventionEnabled = p[Keys.DSP_CLIPPING_PREV] ?: true,
            volumeNormalizationEnabled = p[Keys.DSP_VOL_NORM] ?: false,
            headphoneProfile = enumOrDefault(p[Keys.DSP_HEADPHONE_PROFILE], HeadphoneSoundProfile.REFERENCE_NEUTRAL),
            autoProfileSwitching = p[Keys.DSP_AUTO_SWITCH] ?: false,
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

    suspend fun updateDspConfig(d: DspConfig) {
        context.budsDataStore.edit { p ->
            p[Keys.DSP_EQ_BAND_MODE] = d.eqBandMode.name
            p[Keys.DSP_BANDS_15_CSV] = d.bands15Db.joinToString(",")
            p[Keys.DSP_PARAMETRIC_JSON] = encodeParametricNodes(d.parametricNodes)
            p[Keys.DSP_BASS_ENABLED] = d.bassEngineEnabled
            p[Keys.DSP_DEEP_BASS] = d.deepBassPercent
            p[Keys.DSP_SUB_BASS] = d.subBassPercent
            p[Keys.DSP_BASS_PUNCH] = d.bassPunchPercent
            p[Keys.DSP_BASS_WARMTH] = d.bassWarmthPercent
            p[Keys.DSP_BASS_TEXTURE] = d.bassTexturePercent
            p[Keys.DSP_SUB_CROSSOVER_HZ] = d.subCrossoverHz
            p[Keys.DSP_HPF_ENABLED] = d.highPassEnabled
            p[Keys.DSP_HPF_CUTOFF_HZ] = d.highPassCutoffHz
            p[Keys.DSP_HPF_SLOPE] = d.highPassSlope.name
            p[Keys.DSP_LPF_ENABLED] = d.lowPassEnabled
            p[Keys.DSP_LPF_CUTOFF_HZ] = d.lowPassCutoffHz
            p[Keys.DSP_LPF_SLOPE] = d.lowPassSlope.name
            p[Keys.DSP_STEREO_BALANCE] = d.stereoBalance
            p[Keys.DSP_LEFT_TRIM_DB] = d.leftChannelGainDb
            p[Keys.DSP_RIGHT_TRIM_DB] = d.rightChannelGainDb
            p[Keys.DSP_STEREO_WIDTH_PCT] = d.stereoWidthPercent
            p[Keys.DSP_PREAMP_DB] = d.preampDb
            p[Keys.DSP_MASTER_GAIN_DB] = d.masterGainDb
            p[Keys.DSP_LIMITER_ENABLED] = d.limiterEnabled
            p[Keys.DSP_CLIPPING_PREV] = d.clippingPreventionEnabled
            p[Keys.DSP_VOL_NORM] = d.volumeNormalizationEnabled
            p[Keys.DSP_HEADPHONE_PROFILE] = d.headphoneProfile.name
            p[Keys.DSP_AUTO_SWITCH] = d.autoProfileSwitching
        }
    }

    private fun parseParametricNodes(raw: String?): List<ParametricEqNode> {
        if (raw.isNullOrBlank()) return DspConfig.defaultParametricNodes()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<ParametricEqNode>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ParametricEqNode(
                        id = obj.optInt("id", i + 1),
                        enabled = obj.optBoolean("enabled", false),
                        frequencyHz = obj.optDouble("freq", 1000.0).toFloat().coerceIn(20f, 20000f),
                        qFactor = obj.optDouble("q", 1.4).toFloat().coerceIn(0.3f, 10f),
                        gainDb = obj.optDouble("gain", 0.0).toFloat().coerceIn(-12f, 12f),
                        label = obj.optString("label", "PEQ ${i + 1}"),
                    ),
                )
            }
            if (list.isEmpty()) DspConfig.defaultParametricNodes() else list
        }.getOrDefault(DspConfig.defaultParametricNodes())
    }

    private fun encodeParametricNodes(nodes: List<ParametricEqNode>): String {
        val arr = JSONArray()
        nodes.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("enabled", n.enabled)
                    put("freq", n.frequencyHz.toDouble())
                    put("q", n.qFactor.toDouble())
                    put("gain", n.gainDb.toDouble())
                    put("label", n.label)
                },
            )
        }
        return arr.toString()
    }

    suspend fun resetAllPreferences() {
        context.budsDataStore.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
