package com.example.data

import com.example.model.AnimationSpeedOption
import com.example.model.AppThemeId
import com.example.model.BatteryRingStyle
import com.example.model.ButtonStyleOption
import com.example.model.CardAppearance
import com.example.model.ConnectionAnimStyle
import com.example.model.CustomizationSettings
import com.example.model.DarkModePreference
import com.example.model.EqualizerProfile
import com.example.model.FontScaleOption
import com.example.model.IconStyleOption
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

object SecurityHashUtil {
    private const val SALT = "HBP_2026_TITANIUM_SALT_"

    fun hashSecret(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SALT + raw.trim().lowercase()).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifySecret(raw: String, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return false
        return hashSecret(raw) == expectedHash
    }
}

object BackupManager {

    fun exportEqProfileJson(profile: EqualizerProfile): String {
        val obj = JSONObject()
        obj.put("format", "hardik_buds_pro_eq_v1")
        obj.put("name", profile.name)
        obj.put("subwooferBoost", profile.subwooferBoost)
        val arr = JSONArray()
        profile.bandsDb.forEach { arr.put(it.toDouble()) }
        obj.put("bandsDb", arr)
        return obj.toString(2)
    }

    fun importEqProfileJson(jsonText: String): Result<EqualizerProfile> = runCatching {
        val obj = JSONObject(jsonText.trim())
        val name = obj.optString("name", "Imported Profile").ifBlank { "Imported Profile" }
        val boost = obj.optInt("subwooferBoost", 50).coerceIn(0, 100)
        val arr = obj.getJSONArray("bandsDb")
        val bands = (0 until 10).map { idx ->
            if (idx < arr.length()) arr.getDouble(idx).toFloat().coerceIn(-12f, 12f) else 0f
        }
        EqualizerProfile(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = name,
            bandsDb = bands,
            subwooferBoost = boost,
            isBuiltIn = false,
        )
    }

    fun exportFullBackupJson(
        customization: CustomizationSettings,
        subwoofer: SubwooferConfig,
        soundMode: SoundMode,
        smartSettings: SmartSettings,
        customEqProfiles: List<EqualizerProfile>,
    ): String {
        val root = JSONObject()
        root.put("format", "hardik_buds_pro_backup_v1")
        root.put("exportedAt", System.currentTimeMillis())

        val cObj = JSONObject().apply {
            put("themeId", customization.themeId.name)
            put("darkMode", customization.darkMode.name)
            put("customAccentEnabled", customization.customAccentEnabled)
            put("customAccentRed", customization.customAccentRed)
            put("customAccentGreen", customization.customAccentGreen)
            put("customAccentBlue", customization.customAccentBlue)
            put("rgbCycleEnabled", customization.rgbCycleEnabled)
            put("cardAppearance", customization.cardAppearance.name)
            put("buttonStyle", customization.buttonStyle.name)
            put("fontScale", customization.fontScale.name)
            put("iconStyle", customization.iconStyle.name)
            put("animationSpeed", customization.animationSpeed.name)
            put("batteryRingStyle", customization.batteryRingStyle.name)
            put("connectionAnimStyle", customization.connectionAnimStyle.name)
            put("showQuickAncCard", customization.showQuickAncCard)
            put("showSubwooferCard", customization.showSubwooferCard)
            put("showCompatibilityCard", customization.showCompatibilityCard)
            put("showFirmwareCard", customization.showFirmwareCard)
            put("showMiniVisualizerWidget", customization.showMiniVisualizerWidget)
            put("showBottomNavLabels", customization.showBottomNavLabels)
        }
        root.put("customization", cObj)

        val sObj = JSONObject().apply {
            put("enabled", subwoofer.enabled)
            put("bassIntensity", subwoofer.bassIntensity)
            put("subwooferSimulation", subwoofer.subwooferSimulation)
            put("punchBass", subwoofer.punchBass)
            put("deepBass", subwoofer.deepBass)
            put("lowFreqCutoffHz", subwoofer.lowFreqCutoffHz)
            put("balance", subwoofer.balance.toDouble())
            put("presetName", subwoofer.presetName)
            put("band20HzDb", subwoofer.band20HzDb.toDouble())
            put("band31HzDb", subwoofer.band31HzDb.toDouble())
            put("band40HzDb", subwoofer.band40HzDb.toDouble())
            put("band63HzDb", subwoofer.band63HzDb.toDouble())
            put("band80HzDb", subwoofer.band80HzDb.toDouble())
            put("band125HzDb", subwoofer.band125HzDb.toDouble())
            put("band200HzDb", subwoofer.band200HzDb.toDouble())
        }
        root.put("subwoofer", sObj)
        root.put("soundMode", soundMode.name)

        val smObj = JSONObject().apply {
            put("autoReconnect", smartSettings.autoReconnect)
            put("autoConnectLastDevice", smartSettings.autoConnectLastDevice)
            put("connectionNotifications", smartSettings.connectionNotifications)
            put("batteryAlertThreshold", smartSettings.batteryAlertThreshold)
            put("hearingProtectionEnabled", smartSettings.hearingProtectionEnabled)
            put("hearingWarningPercent", smartSettings.hearingWarningPercent)
            put("gamingLowLatencyHint", smartSettings.gamingLowLatencyHint)
        }
        root.put("smartSettings", smObj)

        val eqArr = JSONArray()
        customEqProfiles.filter { !it.isBuiltIn }.forEach { eq ->
            eqArr.put(JSONObject(exportEqProfileJson(eq)))
        }
        root.put("customEqProfiles", eqArr)

        return root.toString(2)
    }

    data class RestoredBackup(
        val customization: CustomizationSettings,
        val subwoofer: SubwooferConfig,
        val soundMode: SoundMode,
        val smartSettings: SmartSettings,
        val customEqProfiles: List<EqualizerProfile>,
    )

    fun importFullBackupJson(jsonText: String): Result<RestoredBackup> = runCatching {
        val root = JSONObject(jsonText.trim())
        val cObj = root.getJSONObject("customization")
        val customization = CustomizationSettings(
            themeId = enumSafe(cObj.optString("themeId"), AppThemeId.NOTHING_MONOCHROME),
            darkMode = enumSafe(cObj.optString("darkMode"), DarkModePreference.DARK),
            customAccentEnabled = cObj.optBoolean("customAccentEnabled", false),
            customAccentRed = cObj.optInt("customAccentRed", 255),
            customAccentGreen = cObj.optInt("customAccentGreen", 48),
            customAccentBlue = cObj.optInt("customAccentBlue", 56),
            rgbCycleEnabled = cObj.optBoolean("rgbCycleEnabled", false),
            cardAppearance = enumSafe(cObj.optString("cardAppearance"), CardAppearance.GLASS),
            buttonStyle = enumSafe(cObj.optString("buttonStyle"), ButtonStyleOption.PILL),
            fontScale = enumSafe(cObj.optString("fontScale"), FontScaleOption.STANDARD),
            iconStyle = enumSafe(cObj.optString("iconStyle"), IconStyleOption.OUTLINED),
            animationSpeed = enumSafe(cObj.optString("animationSpeed"), AnimationSpeedOption.SMOOTH_1X),
            batteryRingStyle = enumSafe(cObj.optString("batteryRingStyle"), BatteryRingStyle.ORBITAL),
            connectionAnimStyle = enumSafe(cObj.optString("connectionAnimStyle"), ConnectionAnimStyle.SONIC_PULSE),
            hapticEnabled = cObj.optBoolean("hapticEnabled", true),
            showQuickAncCard = cObj.optBoolean("showQuickAncCard", true),
            showSubwooferCard = cObj.optBoolean("showSubwooferCard", true),
            showGamingCard = cObj.optBoolean("showGamingCard", true),
            showCompatibilityCard = cObj.optBoolean("showCompatibilityCard", true),
            showFirmwareCard = cObj.optBoolean("showFirmwareCard", true),
            showMiniVisualizerWidget = cObj.optBoolean("showMiniVisualizerWidget", true),
            showBottomNavLabels = cObj.optBoolean("showBottomNavLabels", true),
        )

        val sObj = root.getJSONObject("subwoofer")
        val subwoofer = SubwooferConfig(
            enabled = sObj.optBoolean("enabled", true),
            bassIntensity = sObj.optInt("bassIntensity", 55),
            subwooferSimulation = sObj.optInt("subwooferSimulation", 60),
            punchBass = sObj.optInt("punchBass", 50),
            deepBass = sObj.optInt("deepBass", 65),
            lowFreqCutoffHz = sObj.optInt("lowFreqCutoffHz", 80),
            balance = sObj.optDouble("balance", 0.0).toFloat(),
            presetName = sObj.optString("presetName", "Titanium Sub"),
            band20HzDb = sObj.optDouble("band20HzDb", 4.5).toFloat(),
            band31HzDb = sObj.optDouble("band31HzDb", 5.0).toFloat(),
            band40HzDb = sObj.optDouble("band40HzDb", 4.0).toFloat(),
            band63HzDb = sObj.optDouble("band63HzDb", 3.0).toFloat(),
            band80HzDb = sObj.optDouble("band80HzDb", 2.0).toFloat(),
            band125HzDb = sObj.optDouble("band125HzDb", 1.0).toFloat(),
            band200HzDb = sObj.optDouble("band200HzDb", 0.0).toFloat(),
        )

        val soundMode = enumSafe(root.optString("soundMode"), SoundMode.MUSIC)
        val smObj = root.optJSONObject("smartSettings") ?: JSONObject()
        val smart = SmartSettings(
            autoReconnect = smObj.optBoolean("autoReconnect", true),
            autoConnectLastDevice = smObj.optBoolean("autoConnectLastDevice", false),
            connectionNotifications = smObj.optBoolean("connectionNotifications", true),
            batteryAlertThreshold = smObj.optInt("batteryAlertThreshold", 20),
            hearingProtectionEnabled = smObj.optBoolean("hearingProtectionEnabled", true),
            hearingWarningPercent = smObj.optInt("hearingWarningPercent", 80),
            gamingLowLatencyHint = smObj.optBoolean("gamingLowLatencyHint", false),
        )

        val profiles = mutableListOf<EqualizerProfile>()
        val eqArr = root.optJSONArray("customEqProfiles")
        if (eqArr != null) {
            for (i in 0 until eqArr.length()) {
                val eqJson = eqArr.getJSONObject(i).toString()
                importEqProfileJson(eqJson).getOrNull()?.let { profiles.add(it) }
            }
        }

        RestoredBackup(customization, subwoofer, soundMode, smart, profiles)
    }

    private inline fun <reified T : Enum<T>> enumSafe(raw: String, fallback: T): T {
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
