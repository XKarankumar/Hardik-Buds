package com.example.model

import androidx.compose.runtime.Immutable

enum class EqBandMode(val label: String, val bandCount: Int) {
    BANDS_10("10-Band", 10),
    BANDS_15("15-Band", 15),
}

enum class FilterSlope(
    val label: String,
    val dbPerOctave: Int,
    val stages: Int,
) {
    SLOPE_6DB("6 dB/oct", 6, 1),
    SLOPE_12DB("12 dB/oct", 12, 1),
    SLOPE_24DB("24 dB/oct", 24, 2),
}

enum class HeadphoneSoundProfile(
    val title: String,
    val subtitle: String,
) {
    REFERENCE_NEUTRAL("Reference Neutral", "Flat studio target curve"),
    IN_EAR_DYNAMIC("In-Ear TWS Dynamic", "Balanced upper-mid presence & controlled sub-bass"),
    BASS_DRIVER_TUNED("Deep Bass Driver", "Sub-harmonic warmth with vocal separation"),
    PLANAR_STUDIO("Hi-Fi Analytical", "Extended 12–16kHz air & crisp transient detail"),
    OPEN_EAR_COMPENSATED("Open-Ear / Semi-In-Ear", "Low-end seal loss compensation"),
}

@Immutable
data class EqBandSpec(
    val index: Int,
    val frequencyHz: Float,
    val shortLabel: String,
    val roleLabel: String,
    val mapped10BandIndex: Int?,
)

@Immutable
data class ParametricEqNode(
    val id: Int,
    val enabled: Boolean = false,
    val frequencyHz: Float = 1000f,
    val qFactor: Float = 1.4f,
    val gainDb: Float = 0f,
    val label: String = "PEQ",
)

@Immutable
data class DspConfig(
    val eqBandMode: EqBandMode = EqBandMode.BANDS_15,
    val bands15Db: List<Float> = List(15) { 0f },
    val parametricNodes: List<ParametricEqNode> = defaultParametricNodes(),
    // Deep Bass Control (0..100%)
    val bassEngineEnabled: Boolean = true,
    val deepBassPercent: Int = 55,
    val subBassPercent: Int = 50,
    val bassPunchPercent: Int = 45,
    val bassWarmthPercent: Int = 40,
    val bassTexturePercent: Int = 30,
    val subCrossoverHz: Float = 95f,
    // Independent High-Pass & Low-Pass Filters
    val highPassEnabled: Boolean = false,
    val highPassCutoffHz: Float = 30f,
    val highPassSlope: FilterSlope = FilterSlope.SLOPE_12DB,
    val lowPassEnabled: Boolean = false,
    val lowPassCutoffHz: Float = 18000f,
    val lowPassSlope: FilterSlope = FilterSlope.SLOPE_12DB,
    // Stereo Balance (-1f = L100%, 0f = Center, +1f = R100%) & Channel Trims (-12..0 dB)
    val stereoBalance: Float = 0f,
    val leftChannelGainDb: Float = 0f,
    val rightChannelGainDb: Float = 0f,
    val stereoWidthPercent: Int = 100, // 0..200%
    // Gain staging, Limiter & Normalization
    val preampDb: Float = 0f, // -12..+12 dB
    val masterGainDb: Float = 0f, // -12..+12 dB
    val limiterEnabled: Boolean = true,
    val clippingPreventionEnabled: Boolean = true,
    val volumeNormalizationEnabled: Boolean = false,
    // Smart Headphone & Profile Automation
    val headphoneProfile: HeadphoneSoundProfile = HeadphoneSoundProfile.REFERENCE_NEUTRAL,
    val autoProfileSwitching: Boolean = false,
) {
    companion object {
        val BANDS_15_SPECS: List<EqBandSpec> = listOf(
            EqBandSpec(0, 20f, "20Hz", "Infrasonic", null),
            EqBandSpec(1, 31f, "31Hz", "Sub Rumble", 0),
            EqBandSpec(2, 40f, "40Hz", "Subwoofer Core", null),
            EqBandSpec(3, 63f, "63Hz", "Deep Kick", 1),
            EqBandSpec(4, 80f, "80Hz", "Chest Punch", null),
            EqBandSpec(5, 125f, "125Hz", "Upper Bass", 2),
            EqBandSpec(6, 200f, "200Hz", "Warmth", null),
            EqBandSpec(7, 250f, "250Hz", "Body", 3),
            EqBandSpec(8, 500f, "500Hz", "Low Midrange", 4),
            EqBandSpec(9, 1000f, "1kHz", "Midrange", 5),
            EqBandSpec(10, 2000f, "2kHz", "Presence", 6),
            EqBandSpec(11, 4000f, "4kHz", "Clarity", 7),
            EqBandSpec(12, 8000f, "8kHz", "Detail", 8),
            EqBandSpec(13, 12000f, "12kHz", "Air", null),
            EqBandSpec(14, 16000f, "16kHz", "Ultra High", 9),
        )

        val TEN_TO_FIFTEEN_INDICES: List<Int> = listOf(1, 3, 5, 7, 8, 9, 10, 11, 12, 14)

        fun defaultParametricNodes(): List<ParametricEqNode> = listOf(
            ParametricEqNode(id = 1, enabled = false, frequencyHz = 55f, qFactor = 1.6f, gainDb = 2.0f, label = "Sub Peak"),
            ParametricEqNode(id = 2, enabled = false, frequencyHz = 420f, qFactor = 2.0f, gainDb = -1.5f, label = "Mud Cut"),
            ParametricEqNode(id = 3, enabled = false, frequencyHz = 2800f, qFactor = 1.8f, gainDb = 2.0f, label = "Vocal Presence"),
            ParametricEqNode(id = 4, enabled = false, frequencyHz = 10500f, qFactor = 1.2f, gainDb = 1.5f, label = "Treble Air"),
        )

        fun expand10To15(bands10: List<Float>): List<Float> {
            val b = List(10) { idx -> bands10.getOrElse(idx) { 0f }.coerceIn(-12f, 12f) }
            return listOf(
                (b[0] * 0.85f).coerceIn(-12f, 12f),        // 20Hz
                b[0],                                       // 31Hz
                ((b[0] + b[1]) * 0.5f),                     // 40Hz
                b[1],                                       // 63Hz
                ((b[1] + b[2]) * 0.5f),                     // 80Hz
                b[2],                                       // 125Hz
                ((b[2] * 0.35f) + (b[3] * 0.65f)),          // 200Hz
                b[3],                                       // 250Hz
                b[4],                                       // 500Hz
                b[5],                                       // 1kHz
                b[6],                                       // 2kHz
                b[7],                                       // 4kHz
                b[8],                                       // 8kHz
                ((b[8] + b[9]) * 0.5f),                     // 12kHz
                b[9],                                       // 16kHz
            )
        }

        fun compress15To10(bands15: List<Float>): List<Float> =
            TEN_TO_FIFTEEN_INDICES.map { idx -> bands15.getOrElse(idx) { 0f }.coerceIn(-12f, 12f) }
    }
}

enum class DspVerificationStatus(val badgeLabel: String) {
    STANDBY("DSP STANDBY"),
    HARDWARE_EFFECT_ACTIVE("HARDWARE EFFECT ACTIVE"),
    SOFTWARE_DSP_ACTIVE("SOFTWARE DSP ACTIVE"),
    DUAL_ENGINE_ACTIVE("SOFTWARE DSP + HARDWARE EFFECT ACTIVE"),
}

@Immutable
data class DspLayerCompatibility(
    val layerTitle: String,
    val isActive: Boolean,
    val statusBadge: String,
    val detail: String,
)

@Immutable
data class DspLiveTelemetry(
    val verificationStatus: DspVerificationStatus = DspVerificationStatus.STANDBY,
    val softwarePcmActive: Boolean = false,
    val hardwareEffectActive: Boolean = false,
    val dynamicsProcessingActive: Boolean = false,
    val systemWideSession0Attached: Boolean = false,
    val activeSampleRateHz: Int = 48000,
    val headroomReductionDb: Float = 0f,
    val limiterGainReductionDb: Float = 0f,
    val outputPeakDb: Float = -96f,
    val clippedSamplesPrevented: Long = 0L,
    val cpuLoadMicrosPerBlock: Long = 0L,
    val latencyEstimateMs: Float = 0f,
    val layers: List<DspLayerCompatibility> = emptyList(),
)
