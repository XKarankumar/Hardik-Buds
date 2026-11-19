package com.example.compatibility

import android.os.ParcelUuid
import android.util.SparseArray
import com.example.model.CompatibilityTier
import com.example.model.DeviceCompatibilityReport
import com.example.model.EarbudBrand
import com.example.model.FeatureSupportItem
import com.example.model.FeatureSupportLevel
import java.util.UUID

object DeviceCompatibilityEngine {

    private val UUID_BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val UUID_FAST_PAIR: UUID = UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")
    private val UUID_SONY_MDR: UUID = UUID.fromString("96cc203e-5068-46ad-b32d-e316f5e069ba")
    private val UUID_APPLE_AAP: UUID = UUID.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

    // Common Bluetooth MAC OUIs for quick hardware identification when name is generic
    private val OUI_BRAND_MAP: Map<String, EarbudBrand> = mapOf(
        "04:52:C7" to EarbudBrand.GENERIC,
        "74:45:CE" to EarbudBrand.SONY,
        "94:DB:56" to EarbudBrand.SONY,
        "CC:98:8B" to EarbudBrand.SONY,
        "AC:80:0A" to EarbudBrand.SONY,
        "24:5E:BE" to EarbudBrand.SAMSUNG,
        "5C:FB:7C" to EarbudBrand.SAMSUNG,
        "30:C0:1B" to EarbudBrand.SAMSUNG,
        "40:B0:76" to EarbudBrand.SAMSUNG,
        "18:1D:EA" to EarbudBrand.APPLE_AIRPODS,
        "40:98:AD" to EarbudBrand.APPLE_AIRPODS,
        "DC:2B:2A" to EarbudBrand.APPLE_AIRPODS,
        "78:9F:70" to EarbudBrand.ONEPLUS,
        "C0:EE:FB" to EarbudBrand.ONEPLUS,
        "38:E6:0A" to EarbudBrand.NOTHING,
        "2C:BE:EB" to EarbudBrand.NOTHING,
        "F4:60:E2" to EarbudBrand.XIAOMI,
        "64:B4:73" to EarbudBrand.XIAOMI,
        "58:EA:FC" to EarbudBrand.HUAWEI,
        "E0:9D:FA" to EarbudBrand.GOOGLE_PIXEL_BUDS,
        "54:60:09" to EarbudBrand.GOOGLE_PIXEL_BUDS,
        "B8:F6:53" to EarbudBrand.JBL,
        "F8:DF:15" to EarbudBrand.JBL,
    )

    fun detectBrand(
        deviceName: String?,
        address: String?,
        manufacturerData: SparseArray<ByteArray>? = null,
        serviceUuids: List<ParcelUuid>? = null,
    ): EarbudBrand {
        val normalized = deviceName?.lowercase()?.trim().orEmpty()

        // 1. Exact keyword matching on advertised/bonded name (most accurate for sub-brands like Redmi/iQOO/Realme)
        if (normalized.isNotEmpty()) {
            // Order matters: check specific sub-brands before parent brands
            val orderedBrands = listOf(
                EarbudBrand.GOOGLE_PIXEL_BUDS,
                EarbudBrand.APPLE_AIRPODS,
                EarbudBrand.SAMSUNG,
                EarbudBrand.ONEPLUS,
                EarbudBrand.REALME,
                EarbudBrand.OPPO,
                EarbudBrand.REDMI,
                EarbudBrand.XIAOMI,
                EarbudBrand.IQOO,
                EarbudBrand.VIVO,
                EarbudBrand.NOTHING,
                EarbudBrand.JBL,
                EarbudBrand.SONY,
                EarbudBrand.BOAT,
                EarbudBrand.NOISE,
                EarbudBrand.HUAWEI,
            )
            for (brand in orderedBrands) {
                if (brand.keywords.any { kw -> normalized.contains(kw) }) {
                    return brand
                }
            }
        }

        // 2. BLE Manufacturer Specific Data ID
        if (manufacturerData != null) {
            for (i in 0 until manufacturerData.size()) {
                val id = manufacturerData.keyAt(i)
                val match = EarbudBrand.entries.firstOrNull { it != EarbudBrand.GENERIC && id in it.manufacturerIds }
                if (match != null) return match
            }
        }

        // 3. Service UUIDs
        if (!serviceUuids.isNullOrEmpty()) {
            val uuids = serviceUuids.map { it.uuid }
            if (UUID_SONY_MDR in uuids) return EarbudBrand.SONY
            if (UUID_APPLE_AAP in uuids) return EarbudBrand.APPLE_AIRPODS
        }

        // 4. MAC Address OUI prefix
        if (!address.isNullOrBlank() && address.length >= 8) {
            val oui = address.take(8).uppercase()
            OUI_BRAND_MAP[oui]?.let { return it }
        }

        return EarbudBrand.GENERIC
    }

    fun evaluateCompatibility(
        brand: EarbudBrand,
        hasBatteryData: Boolean = false,
        hasUntetheredBattery: Boolean = false,
        hasGattBatteryService: Boolean = false,
        hasGattDeviceInfo: Boolean = false,
        serviceUuids: List<ParcelUuid>? = null,
    ): DeviceCompatibilityReport {
        val uuids = serviceUuids?.map { it.uuid }.orEmpty()
        val hasFastPair = UUID_FAST_PAIR in uuids

        val tier = when {
            brand in setOf(
                EarbudBrand.SAMSUNG,
                EarbudBrand.SONY,
                EarbudBrand.ONEPLUS,
                EarbudBrand.OPPO,
                EarbudBrand.REALME,
                EarbudBrand.NOTHING,
                EarbudBrand.GOOGLE_PIXEL_BUDS,
                EarbudBrand.XIAOMI,
                EarbudBrand.REDMI,
                EarbudBrand.HUAWEI,
                EarbudBrand.JBL,
            ) && (hasUntetheredBattery || hasFastPair || hasGattDeviceInfo) -> CompatibilityTier.TIER_3_PROPRIETARY

            hasBatteryData || hasGattBatteryService || brand != EarbudBrand.GENERIC -> CompatibilityTier.TIER_2_ENHANCED
            else -> CompatibilityTier.TIER_1_STANDARD
        }

        val batteryReportingSupported = hasBatteryData || hasGattBatteryService || tier.level >= 2
        val separateBatterySupported = hasUntetheredBattery || brand in setOf(
            EarbudBrand.GOOGLE_PIXEL_BUDS,
            EarbudBrand.SAMSUNG,
            EarbudBrand.ONEPLUS,
            EarbudBrand.OPPO,
            EarbudBrand.REALME,
            EarbudBrand.NOTHING,
            EarbudBrand.SONY,
            EarbudBrand.APPLE_AIRPODS,
        )

        val features = listOf(
            FeatureSupportItem(
                featureName = "Standard Bluetooth A2DP & HFP Audio",
                supportLevel = FeatureSupportLevel.SUPPORTED,
                detail = "Full stereo playback, call routing, AVRCP media commands, and RSSI monitoring.",
            ),
            FeatureSupportItem(
                featureName = "15-Band & Parametric Software DSP Engine",
                supportLevel = FeatureSupportLevel.SUPPORTED,
                detail = "Real-time biquad EQ, Deep Bass shaping, HPF/LPF filters, L/R balance & Limiter via Android AudioEffect & internal PCM DSP.",
            ),
            FeatureSupportItem(
                featureName = "Battery Telemetry (Main / L / R / Case)",
                supportLevel = if (hasBatteryData || hasGattBatteryService) {
                    FeatureSupportLevel.SUPPORTED
                } else {
                    FeatureSupportLevel.OS_OR_HARDWARE_RESTRICTED
                },
                detail = when {
                    hasUntetheredBattery -> "Untethered Left, Right & Case battery exposed via Android Metadata / Fast Pair."
                    hasGattBatteryService -> "BLE GATT Battery Service (0x180F) detected."
                    hasBatteryData -> "Overall Bluetooth HFP battery level reported by OS."
                    else -> "Displays live battery when exposed by hardware via HFP, GATT, or Android Metadata."
                },
            ),
            FeatureSupportItem(
                featureName = "Earbud Hardware ANC & Transparency",
                supportLevel = FeatureSupportLevel.PROPRIETARY_UNAVAILABLE,
                detail = "Active Noise Cancellation switching runs exclusively inside earbud microphone DSP firmware when exposed over standard Bluetooth protocols.",
            ),
            FeatureSupportItem(
                featureName = "Hardware Touch Gesture Storage",
                supportLevel = FeatureSupportLevel.PROPRIETARY_UNAVAILABLE,
                detail = "Touch preferences are stored locally in app profile; hardware button EEPROM remapping requires manufacturer firmware support.",
            ),
            FeatureSupportItem(
                featureName = "BLE Firmware Revision Readout (0x2A26)",
                supportLevel = if (hasGattDeviceInfo) {
                    FeatureSupportLevel.SUPPORTED
                } else {
                    FeatureSupportLevel.OS_OR_HARDWARE_RESTRICTED
                },
                detail = if (hasGattDeviceInfo) {
                    "Device Information Service (0x180A) readable over BLE GATT."
                } else {
                    "Readable when earbuds advertise BLE GATT Device Information Service (0x180A)."
                },
            ),
        )

        val notice = when (tier) {
            CompatibilityTier.TIER_1_STANDARD ->
                "Tier 1 Standard: Full Software & AudioEffect DSP (15-band EQ, Deep Bass, HPF/LPF, Balance) and Bluetooth audio controls active."
            CompatibilityTier.TIER_2_ENHANCED ->
                "Tier 2 Enhanced (${brand.displayName}): Supports real Android AudioEffect DSP, Software PCM Engine, and OS battery telemetry."
            CompatibilityTier.TIER_3_PROPRIETARY ->
                "Tier 3 Extended (${brand.displayName}): Full Software DSP + enhanced Bluetooth/Fast Pair battery & GATT Device Info telemetry."
        }

        return DeviceCompatibilityReport(
            brand = brand,
            tier = tier,
            supportsStandardAudio = true,
            supportsBatteryReporting = batteryReportingSupported,
            supportsSeparateEarbudBatteries = separateBatterySupported,
            supportsAndroidDspEq = true,
            supportsHardwareAncControl = false,
            supportsHardwareTouchRemap = false,
            supportsFirmwareRevisionRead = hasGattDeviceInfo,
            supportsLowLatencyHint = true,
            features = features,
            summaryNotice = notice,
        )
    }

    fun allSupportedBrandsOverview(): List<DeviceCompatibilityReport> =
        EarbudBrand.entries.map { brand ->
            evaluateCompatibility(
                brand = brand,
                hasBatteryData = brand != EarbudBrand.GENERIC,
                hasUntetheredBattery = brand in setOf(
                    EarbudBrand.ONEPLUS,
                    EarbudBrand.SAMSUNG,
                    EarbudBrand.GOOGLE_PIXEL_BUDS,
                    EarbudBrand.NOTHING,
                    EarbudBrand.SONY,
                    EarbudBrand.OPPO,
                    EarbudBrand.REALME,
                ),
            )
        }

    fun isHomeTheatreDevice(deviceName: String?): Boolean {
        val norm = deviceName?.lowercase()?.trim().orEmpty()
        if (norm.isEmpty()) return false
        val keywords = listOf(
            "soundbar", "home theater", "home theatre", "cinema", "avr",
            "receiver", "subwoofer", "bar ", "jbl bar", "ht-", "hw-",
            "sonos", "bose smart", "aavante", "marshal", "jbl boombox",
            "partybox", "tv", "theatre", "theater", "surround", "dolby",
            "denon", "yamaha", "onkyo", "klipsch", "polk", "harman",
        )
        return keywords.any { norm.contains(it) }
    }
}
