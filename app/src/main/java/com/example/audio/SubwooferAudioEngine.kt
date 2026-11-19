package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Visualizer
import android.os.Build
import com.example.model.AudioEngineCapabilities
import com.example.model.AudioOutputRoute
import com.example.model.DspConfig
import com.example.model.DspLayerCompatibility
import com.example.model.DspLiveTelemetry
import com.example.model.DspVerificationStatus
import com.example.model.EqualizerProfile
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class SubwooferAudioEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Default)

    val dspProcessor = DspAudioProcessor(48000)

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Any? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var visualizer: Visualizer? = null

    private var activeSessionId: Int = 0
    private var soundTestJob: Job? = null

    private val _capabilities = MutableStateFlow(AudioEngineCapabilities())
    val capabilities: StateFlow<AudioEngineCapabilities> = _capabilities.asStateFlow()

    private val _dspTelemetry = MutableStateFlow(DspLiveTelemetry())
    val dspTelemetry: StateFlow<DspLiveTelemetry> = _dspTelemetry.asStateFlow()

    private val _eqCurveDb = MutableStateFlow(
        dspProcessor.computeFrequencyResponseCurve(DspAudioProcessor.GRAPH_FREQUENCIES_HZ),
    )
    val eqCurveDb: StateFlow<List<Float>> = _eqCurveDb.asStateFlow()

    private val _spectrumBands = MutableStateFlow(List(16) { 0f })
    val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

    private val _bassEnergy = MutableStateFlow(0f)
    val bassEnergy: StateFlow<Float> = _bassEnergy.asStateFlow()

    private val _volumePercent = MutableStateFlow(readMusicVolumePercent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    private val _soundTestRunning = MutableStateFlow<String?>(null)
    val soundTestRunning: StateFlow<String?> = _soundTestRunning.asStateFlow()

    private val _audioOutputRoutes = MutableStateFlow<List<AudioOutputRoute>>(emptyList())
    val audioOutputRoutes: StateFlow<List<AudioOutputRoute>> = _audioOutputRoutes.asStateFlow()

    private val _smoothRampProgress = MutableStateFlow<Float?>(null)
    val smoothRampProgress: StateFlow<Float?> = _smoothRampProgress.asStateFlow()

    private var monitorJob: Job? = null
    private var smoothTransitionJob: Job? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var lastDpSupported = false
    private var lastEqSupported = false
    private var lastBassBoostSupported = false
    private var lastVirtualizerSupported = false
    private var lastLoudnessSupported = false

    fun initialize() {
        releaseEffects()

        val sampleRate = runCatching {
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        }.getOrNull()?.coerceIn(8000, 192000) ?: 48000
        dspProcessor.setSampleRate(sampleRate)

        val candidateSessions = listOf(0, audioManager.generateAudioSessionId())
        var attachedSession = 0
        var eqOk = false
        var dpOk = false

        for (session in candidateSessions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val dpAttempt = runCatching { createDynamicsProcessing(session) }.getOrNull()
                if (dpAttempt != null) {
                    dynamicsProcessing = dpAttempt
                    attachedSession = session
                    dpOk = true
                }
            }

            val eqAttempt = runCatching {
                Equalizer(0, session).apply { enabled = true }
            }.getOrNull()
            if (eqAttempt != null) {
                equalizer = eqAttempt
                attachedSession = session
                eqOk = true
            }

            if (dpOk || eqOk) break
        }
        activeSessionId = attachedSession

        bassBoost = runCatching {
            BassBoost(0, attachedSession).apply { enabled = true }
        }.getOrNull()

        virtualizer = runCatching {
            val clazz = Class.forName("android.media.audiofx.Virtualizer")
            val ctor = clazz.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            val inst = ctor.newInstance(0, attachedSession)
            clazz.getMethod("setEnabled", Boolean::class.javaPrimitiveType).invoke(inst, false)
            inst
        }.getOrNull()

        loudnessEnhancer = runCatching {
            LoudnessEnhancer(attachedSession).apply { enabled = false }
        }.getOrNull()

        val bandCount = when {
            dpOk -> 15
            eqOk -> runCatching { equalizer?.numberOfBands?.toInt() ?: 0 }.getOrDefault(0)
            else -> 15 // Software PCM DSP runs 15 bands cleanly
        }
        val range = runCatching { equalizer?.bandLevelRange }.getOrNull()
        val minDb = if (dpOk) -12f else (range?.getOrNull(0) ?: -1200) / 100f
        val maxDb = if (dpOk) 12f else (range?.getOrNull(1) ?: 1200) / 100f

        val virtSupported = virtualizer?.let { inst ->
            runCatching {
                inst.javaClass.getMethod("getStrengthSupported").invoke(inst) as? Boolean
            }.getOrNull()
        } == true

        val bassOk = bassBoost?.strengthSupported == true
        val loudOk = loudnessEnhancer != null

        lastDpSupported = dpOk
        lastEqSupported = eqOk
        lastBassBoostSupported = bassOk
        lastVirtualizerSupported = virtSupported
        lastLoudnessSupported = loudOk

        val statusSummary = when {
            attachedSession == 0 && dpOk ->
                "Global Session 0 · 15-Band DynamicsProcessing & Limiter Active"
            attachedSession == 0 && eqOk ->
                "Global Session 0 · Android AudioEffect + Software PCM DSP Active"
            dpOk || eqOk ->
                "App Session ($attachedSession) · Android AudioEffect + Software PCM DSP Active"
            else ->
                "App Software PCM DSP Engine Active (Device AudioEffect HAL Unavailable)"
        }

        _capabilities.value = AudioEngineCapabilities(
            equalizerSupported = eqOk || dpOk,
            dynamicsProcessingSupported = dpOk,
            hardwareBandCount = bandCount,
            hardwareMinDb = minDb,
            hardwareMaxDb = maxDb,
            bassBoostSupported = bassOk,
            virtualizerSupported = virtSupported,
            loudnessSupported = loudOk,
            visualizerActive = false,
            activeAudioSessionId = attachedSession,
            outputSampleRateHz = sampleRate,
            outputRouteLabel = resolvePrimaryRouteLabel(),
            engineStatusMessage = statusSummary,
            limitationNotice = "Software PCM DSP operates with zero sample loss on internal audio. Android AudioEffect processes supported system media streams.",
        )

        registerAudioDeviceMonitor()
        refreshAudioOutputRoutes()
        updateTelemetrySnapshot()
        startMonitorLoop()
    }

    private fun createDynamicsProcessing(sessionId: Int): DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2, // Stereo channels (0 = Left, 1 = Right)
            true, 15, // preEqInUse = true, 15 frequency bands
            true, 4,  // mbcInUse = true, 4 multiband compressor bands
            false, 0, // postEq not needed when 15-band preEq is active
            true,     // limiterInUse = true for anti-clipping
        )
            .setPreferredFrameDuration(10f)
            .build()

        return DynamicsProcessing(0, sessionId, config).apply {
            enabled = true
        }
    }

    private fun resolvePrimaryRouteLabel(): String = runCatching {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btOut = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        btOut?.productName?.toString()?.takeIf { it.isNotBlank() }?.let { "Bluetooth · $it" }
            ?: if (btOut != null) "Bluetooth Audio" else "Device / Wired Audio"
    }.getOrDefault("System Audio Out")

    private fun registerAudioDeviceMonitor() {
        runCatching {
            audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
            val cb = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    refreshAudioOutputRoutes()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    refreshAudioOutputRoutes()
                }
            }
            audioDeviceCallback = cb
            audioManager.registerAudioDeviceCallback(cb, null)
        }
    }

    fun refreshAudioOutputRoutes() {
        val routes = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { info ->
                val typeLabel = when (info.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                    AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST -> "LE Audio"
                    AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI / eARC"
                    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Audio / DAC"
                    AudioDeviceInfo.TYPE_LINE_DIGITAL, AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_AUX_LINE -> "Line Out / Optical"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Audio"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Device Speaker"
                    else -> null
                } ?: return@mapNotNull null

                val name = info.productName?.toString()?.takeIf { it.isNotBlank() } ?: typeLabel
                val maxCh = info.channelCounts.maxOrNull() ?: 2
                val maxRate = info.sampleRates.maxOrNull() ?: 48000
                val isTheatre = info.type == AudioDeviceInfo.TYPE_HDMI ||
                    info.type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                    info.type == AudioDeviceInfo.TYPE_HDMI_EARC ||
                    info.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                    info.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                    info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    (info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && maxCh >= 2) ||
                    info.type == AudioDeviceInfo.TYPE_USB_DEVICE

                val isBt = info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    info.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER

                val macAddr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    runCatching { info.address.takeIf { it.isNotBlank() } }.getOrNull()
                } else null

                AudioOutputRoute(
                    id = info.id,
                    productName = name,
                    typeLabel = typeLabel,
                    address = macAddr,
                    isBluetooth = isBt,
                    isHomeTheatreCapable = isTheatre,
                    maxChannels = maxCh,
                    sampleRateLabel = "${maxRate / 1000} kHz",
                )
            }
        }.getOrDefault(emptyList())
        _audioOutputRoutes.value = routes

        val primaryRoute = routes.firstOrNull { it.isBluetooth }
            ?: routes.firstOrNull { it.typeLabel.contains("HDMI") || it.typeLabel.contains("USB") || it.typeLabel.contains("Line") }
            ?: routes.firstOrNull()
        if (primaryRoute != null) {
            _capabilities.value = _capabilities.value.copy(
                outputRouteLabel = "${primaryRoute.typeLabel} · ${primaryRoute.productName}",
            )
        }
    }

    fun runSmoothHomeTheatreTransition(
        targetBassStrength: Int,
        durationMs: Int,
        enableRamp: Boolean,
        onComplete: () -> Unit = {},
    ) {
        smoothTransitionJob?.cancel()
        smoothTransitionJob = scope.launch {
            if (!enableRamp || durationMs <= 100) {
                _smoothRampProgress.value = 1f
                delay(150L)
                _smoothRampProgress.value = null
                onComplete()
                return@launch
            }
            val steps = 16
            val stepDelay = (durationMs / steps).toLong().coerceAtLeast(35L)
            val targetStr = (targetBassStrength.coerceIn(0, 100) * 10)
            for (step in 1..steps) {
                val frac = step.toFloat() / steps.toFloat()
                val eased = (0.5f * (1f - cos(PI * frac))).toFloat()
                _smoothRampProgress.value = eased
                runCatching {
                    val bb = bassBoost
                    if (bb != null && bb.strengthSupported) {
                        bb.enabled = true
                        bb.setStrength((targetStr * eased).roundToInt().toShort())
                    }
                    val loud = loudnessEnhancer
                    if (loud != null) {
                        loud.setTargetGain((350 * eased).roundToInt())
                        loud.enabled = true
                    }
                }
                delay(stepDelay)
            }
            _smoothRampProgress.value = null
            refreshAudioOutputRoutes()
            onComplete()
        }
    }

    fun refreshPlaybackAndVolume() {
        _volumePercent.value = readMusicVolumePercent()
        _isMusicPlaying.value = runCatching { audioManager.isMusicActive }.getOrDefault(false)
    }

    fun readMusicVolumePercent(): Int {
        val current = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        val max = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(15)
        if (max <= 0) return 0
        return ((current.toFloat() / max.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    }

    /**
     * Unified Audio Processing Pipeline:
     * 1. Updates pure Kotlin `DspAudioProcessor` (15-band / 10-band GEQ, Parametric EQ, Deep Bass 5 controls,
     *    HPF/LPF filters 6/12/24 dB/oct, L/R Balance & Channel Trim, Stereo Width, Preamp, Gain, Limiter, Normalization)
     * 2. Updates exact frequency response graph curve (`_eqCurveDb`)
     * 3. Synchronizes Android `DynamicsProcessing` (API 28+), `Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`
     */
    fun applyAudioPipeline(
        dspConfig: DspConfig,
        subwoofer: SubwooferConfig,
        eqProfile: EqualizerProfile,
        soundMode: SoundMode,
        lowLatencyHint: Boolean,
    ) {
        refreshPlaybackAndVolume()

        val merged15Bands = eqProfile.resolved15Bands()
        dspProcessor.updateConfiguration(
            dspConfig = dspConfig,
            soundMode = soundMode,
            profileBands15 = merged15Bands,
        )

        // Update animated EQ curve with exact DSP mathematical response (including HPF/LPF/Deep Bass/Parametric EQ)
        val curve = dspProcessor.computeFrequencyResponseCurve(DspAudioProcessor.GRAPH_FREQUENCIES_HZ)
        _eqCurveDb.value = curve

        // 1. Apply Android DynamicsProcessing (API 28+) if supported
        var dpActuallyEnabled = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing?.let { dp ->
                runCatching {
                    dp.enabled = true
                    val preEq = DynamicsProcessing.Eq(true, true, 15)
                    val curveAtBands = dspProcessor.computeFrequencyResponseCurve(
                        DspConfig.BANDS_15_SPECS.map { it.frequencyHz },
                    )
                    DspConfig.BANDS_15_SPECS.forEachIndexed { idx, spec ->
                        val cutoff = if (idx < 14) {
                            (spec.frequencyHz + DspConfig.BANDS_15_SPECS[idx + 1].frequencyHz) * 0.5f
                        } else {
                            20000f
                        }
                        val band = preEq.getBand(idx)
                        band.isEnabled = true
                        band.cutoffFrequency = cutoff
                        // Subtract global preamp+master so PreEq holds clean differential shape
                        val shapeDb = (curveAtBands.getOrElse(idx) { 0f } - dspConfig.preampDb - dspConfig.masterGainDb)
                            .coerceIn(-15f, 15f)
                        band.gain = shapeDb
                        preEq.setBand(idx, band)
                    }
                    dp.setPreEqAllChannelsTo(preEq)

                    // Stereo L/R balance + channel trim + preamp applied per channel cleanly
                    val bal = dspConfig.stereoBalance.coerceIn(-1f, 1f)
                    val leftBalAttenDb = if (bal > 0.01f) (-24f * bal) else 0f
                    val rightBalAttenDb = if (bal < -0.01f) (24f * bal) else 0f
                    val baseGainDb = (dspConfig.preampDb + dspConfig.masterGainDb - dspProcessor.lastHeadroomReductionDb)
                        .coerceIn(-18f, 12f)
                    dp.setInputGainbyChannel(0, (baseGainDb + leftBalAttenDb + dspConfig.leftChannelGainDb).coerceIn(-36f, 12f))
                    dp.setInputGainbyChannel(1, (baseGainDb + rightBalAttenDb + dspConfig.rightChannelGainDb).coerceIn(-36f, 12f))

                    // Multi-band compressor for Volume Normalization or Deep Bass Punch
                    val mbc = DynamicsProcessing.Mbc(true, dspConfig.volumeNormalizationEnabled || dspConfig.bassEngineEnabled, 4)
                    val mbcCutoffs = listOf(120f, 500f, 4000f, 20000f)
                    for (b in 0 until 4) {
                        val mbcBand = mbc.getBand(b)
                        mbcBand.isEnabled = dspConfig.volumeNormalizationEnabled || (b == 0 && dspConfig.bassEngineEnabled)
                        mbcBand.cutoffFrequency = mbcCutoffs[b]
                        mbcBand.ratio = if (dspConfig.volumeNormalizationEnabled) 2.5f else 1.5f
                        mbcBand.threshold = if (dspConfig.volumeNormalizationEnabled) -16f else -8f
                        mbcBand.attackTime = 8f
                        mbcBand.releaseTime = 90f
                        mbc.setBand(b, mbcBand)
                    }
                    dp.setMbcAllChannelsTo(mbc)

                    // Hardware HAL Limiter for anti-clipping
                    val lim = DynamicsProcessing.Limiter(
                        true,
                        dspConfig.limiterEnabled || dspConfig.clippingPreventionEnabled,
                        0,
                        0.5f,
                        50f,
                        4.0f,
                        -0.5f,
                        0f,
                    )
                    dp.setLimiterAllChannelsTo(lim)
                    dpActuallyEnabled = dp.enabled
                }
            }
        }

        // 2. Apply Standard Android Equalizer when active (if DP is not overriding or alongside it)
        var eqActuallyEnabled = false
        equalizer?.let { eq ->
            runCatching {
                if (dpActuallyEnabled) {
                    // Prevent double-EQ stacking when 15-band DynamicsProcessing handles EQ directly
                    eq.enabled = false
                } else {
                    eq.enabled = true
                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange
                    val minLevel = range[0].toInt()
                    val maxLevel = range[1].toInt()
                    val hwCenters = (0 until numBands).map { (eq.getCenterFreq(it.toShort()) / 1000f) }
                    val hwTargets = dspProcessor.computeFrequencyResponseCurve(hwCenters)
                    for (hwBand in 0 until numBands) {
                        val mB = (hwTargets[hwBand] * 100f).roundToInt().coerceIn(minLevel, maxLevel)
                        eq.setBandLevel(hwBand.toShort(), mB.toShort())
                    }
                    eqActuallyEnabled = eq.enabled
                }
            }
        }

        // 3. Apply Android BassBoost (0..1000) when DP is not handling all sub-bass exclusively
        val bb = bassBoost
        if (bb != null) {
            runCatching {
                if (dspConfig.bassEngineEnabled && bb.strengthSupported && !dpActuallyEnabled) {
                    bb.enabled = true
                    val combinedStrength = (
                        dspConfig.deepBassPercent * 0.40f +
                            dspConfig.subBassPercent * 0.35f +
                            dspConfig.bassPunchPercent * 0.25f
                        ).roundToInt().coerceIn(0, 100) * 10
                    bb.setStrength(combinedStrength.toShort())
                } else {
                    bb.enabled = false
                }
            }
        }

        // 4. Apply Android Virtualizer (Spatial / Surround / Wide Stereo)
        val virt = virtualizer
        if (virt != null) {
            runCatching {
                val clazz = virt.javaClass
                val supported = clazz.getMethod("getStrengthSupported").invoke(virt) as? Boolean == true
                val setEnabled = clazz.getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                val wideEnabled = soundMode.isSpatial || dspConfig.stereoWidthPercent > 110
                if (wideEnabled && supported) {
                    setEnabled.invoke(virt, true)
                    val baseStr = when (soundMode) {
                        SoundMode.SPATIAL_AUDIO -> 800
                        SoundMode.SURROUND_SOUND -> 900
                        SoundMode.MOVIE -> 700
                        else -> ((dspConfig.stereoWidthPercent - 100).coerceIn(0, 100) * 8)
                    }
                    clazz.getMethod("setStrength", Short::class.javaPrimitiveType).invoke(virt, baseStr.coerceIn(0, 1000).toShort())
                } else {
                    setEnabled.invoke(virt, false)
                }
            }
        }

        // 5. Apply LoudnessEnhancer only when gain normalization / preamp lift requested and DP is off
        val loud = loudnessEnhancer
        if (loud != null) {
            runCatching {
                if (dpActuallyEnabled) {
                    loud.enabled = false
                } else {
                    val netGainDb = (dspConfig.preampDb + dspConfig.masterGainDb - dspProcessor.lastHeadroomReductionDb)
                        .coerceIn(0f, 8f)
                    val gainMb = when {
                        soundMode == SoundMode.NIGHT_MODE -> 250
                        dspConfig.volumeNormalizationEnabled -> 350
                        netGainDb > 0.5f -> (netGainDb * 80f).roundToInt()
                        else -> 0
                    }
                    if (gainMb > 0) {
                        loud.setTargetGain(gainMb)
                        loud.enabled = true
                    } else {
                        loud.enabled = false
                    }
                }
            }
        }

        lastDpSupported = dpActuallyEnabled
        lastEqSupported = eqActuallyEnabled || dpActuallyEnabled
        updateTelemetrySnapshot()
    }

    private fun updateTelemetrySnapshot() {
        val pcmActive = _soundTestRunning.value != null
        val hwActive = lastDpSupported || lastEqSupported || lastBassBoostSupported
        val status = when {
            pcmActive && hwActive -> DspVerificationStatus.DUAL_ENGINE_ACTIVE
            pcmActive -> DspVerificationStatus.SOFTWARE_DSP_ACTIVE
            hwActive -> DspVerificationStatus.HARDWARE_EFFECT_ACTIVE
            else -> DspVerificationStatus.STANDBY
        }

        val sr = dspProcessor.getSampleRateHz()
        val layers = listOf(
            DspLayerCompatibility(
                layerTitle = "1. App-Internal PCM DSP",
                isActive = pcmActive,
                statusBadge = if (pcmActive) "PROCESSING" else "READY",
                detail = "15-band/Parametric biquads, 6/12/24 dB/oct HPF & LPF, Mid/Side width, L/R balance & soft limiter at $sr Hz.",
            ),
            DspLayerCompatibility(
                layerTitle = "2. Android AudioEffect Engine",
                isActive = hwActive,
                statusBadge = when {
                    lastDpSupported -> "DYNAMICS 15-BAND"
                    lastEqSupported -> "EQ + BASSBOOST"
                    else -> "HAL UNAVAILABLE"
                },
                detail = if (lastDpSupported) {
                    "Android DynamicsProcessing (15-band PreEQ + MBC + Limiter + L/R channel gain) attached on session $activeSessionId."
                } else if (lastEqSupported) {
                    "Android Equalizer (${_capabilities.value.hardwareBandCount} bands) & BassBoost active on session $activeSessionId."
                } else {
                    "Device audio HAL did not allocate AudioEffect; app-internal PCM DSP handles acoustic streams cleanly."
                },
            ),
            DspLayerCompatibility(
                layerTitle = "3. System-Wide Media Mix",
                isActive = hwActive && activeSessionId == 0,
                statusBadge = if (hwActive && activeSessionId == 0) "SESSION 0" else "APP SESSION",
                detail = if (hwActive && activeSessionId == 0) {
                    "Global Session 0 active across standard Android media players routing through Audio HAL."
                } else {
                    "Android restricts third-party apps from modifying external encrypted audio streams when Session 0 is blocked."
                },
            ),
            DspLayerCompatibility(
                layerTitle = "4. Earbud Hardware DSP",
                isActive = false,
                statusBadge = "HARDWARE LINK",
                detail = "Bluetooth earbuds receive processed PCM audio over A2DP; native chip ANC & Transparency modes stay hardware-controlled.",
            ),
        )

        _dspTelemetry.value = DspLiveTelemetry(
            verificationStatus = status,
            softwarePcmActive = pcmActive,
            hardwareEffectActive = hwActive,
            dynamicsProcessingActive = lastDpSupported,
            systemWideSession0Attached = hwActive && activeSessionId == 0,
            activeSampleRateHz = sr,
            headroomReductionDb = dspProcessor.lastHeadroomReductionDb,
            limiterGainReductionDb = dspProcessor.lastLimiterReductionDb,
            outputPeakDb = dspProcessor.lastPeakDb,
            clippedSamplesPrevented = dspProcessor.clippedSamplesPrevented,
            cpuLoadMicrosPerBlock = dspProcessor.lastBlockProcessMicros,
            latencyEstimateMs = ((512f / sr.coerceAtLeast(8000)) * 1000f),
            layers = layers,
        )
    }

    fun tryStartLiveVisualizer(): Boolean {
        if (!PermissionManager.hasPermission(context, android.Manifest.permission.RECORD_AUDIO)) {
            return false
        }
        if (visualizer != null) return true

        val created = runCatching {
            Visualizer(activeSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft != null && fft.size >= 32 && _soundTestRunning.value == null) {
                                val bands = MutableList(16) { 0f }
                                val binStep = max(1, (fft.size / 2 - 2) / 16)
                                for (b in 0 until 16) {
                                    var sum = 0f
                                    val startBin = 1 + b * binStep
                                    val endBin = minOf(fft.size / 2, startBin + binStep)
                                    for (k in startBin until endBin) {
                                        val r = fft[2 * k].toFloat()
                                        val im = fft[2 * k + 1].toFloat()
                                        sum += hypot(r, im)
                                    }
                                    val count = max(1, endBin - startBin)
                                    bands[b] = (sum / (count * 52f)).coerceIn(0f, 1f)
                                }
                                _spectrumBands.value = bands
                                _bassEnergy.value = ((bands[0] + bands[1] + bands[2]) / 3f).coerceIn(0f, 1f)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true,
                )
                enabled = true
            }
        }.getOrNull()

        visualizer = created
        _capabilities.value = _capabilities.value.copy(visualizerActive = created != null)
        return created != null
    }

    private fun startMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                refreshPlaybackAndVolume()
                if (visualizer == null && _soundTestRunning.value == null) {
                    // Honest non-fake energy meter: zero when idle/no live FFT capture
                    _bassEnergy.value = 0f
                    _spectrumBands.value = List(16) { 0f }
                }
                updateTelemetrySnapshot()
                delay(180L)
            }
        }
    }

    fun runAcousticSoundTest(testType: AcousticTestType) {
        soundTestJob?.cancel()
        soundTestJob = scope.launch {
            _soundTestRunning.value = testType.label
            val sampleRate = dspProcessor.getSampleRateHz().coerceIn(44100, 96000)
            dspProcessor.setSampleRate(sampleRate)
            dspProcessor.resetStates()
            updateTelemetrySnapshot()

            val frameSize = 512
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(frameSize * 4 * 2)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build(),
                    )
                    .setBufferSizeInBytes(minBufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            if (track == null) {
                _soundTestRunning.value = null
                updateTelemetrySnapshot()
                return@launch
            }

            runCatching { track.play() }

            val maxDurationSec = when (testType) {
                AcousticTestType.LIVE_DSP_AUDITION -> 18.0
                AcousticTestType.SUBWOOFER_SWEEP -> 4.0
                else -> 2.2
            }
            val totalFrames = (sampleRate * maxDurationSec).toInt()
            var writtenFrames = 0
            val floatStereo = FloatArray(frameSize * 2)
            val shortStereo = ShortArray(frameSize * 2)
            var noiseSeed = 0x12345678L

            try {
                while (isActive && writtenFrames < totalFrames) {
                    for (i in 0 until frameSize) {
                        val absIdx = writtenFrames + i
                        val t = absIdx.toDouble() / sampleRate
                        val env = when {
                            absIdx < 2000 -> absIdx / 2000.0
                            absIdx > totalFrames - 2000 -> max(0.0, (totalFrames - absIdx) / 2000.0)
                            else -> 1.0
                        }

                        var rawL = 0f
                        var rawR = 0f

                        when (testType) {
                            AcousticTestType.LEFT_CHANNEL -> {
                                val f = if (absIdx < totalFrames / 2) 523.25 else 659.25
                                rawL = (sin(2.0 * PI * f * t) * 0.45 * env).toFloat()
                                rawR = 0f
                            }
                            AcousticTestType.RIGHT_CHANNEL -> {
                                val f = if (absIdx < totalFrames / 2) 523.25 else 659.25
                                rawL = 0f
                                rawR = (sin(2.0 * PI * f * t) * 0.45 * env).toFloat()
                            }
                            AcousticTestType.STEREO_CHIME -> {
                                val fL = 440.0
                                val fR = 554.37
                                rawL = (sin(2.0 * PI * fL * t) * 0.35 * env).toFloat()
                                rawR = (sin(2.0 * PI * fR * t) * 0.35 * env).toFloat()
                            }
                            AcousticTestType.SUBWOOFER_SWEEP -> {
                                val frac = absIdx.toDouble() / totalFrames
                                val freq = 24.0 + 136.0 * frac
                                val v = (sin(2.0 * PI * freq * t) * 0.55 * env).toFloat()
                                rawL = v
                                rawR = v
                            }
                            AcousticTestType.FIND_EARBUDS_CHIME -> {
                                val pulse = if ((absIdx / (sampleRate / 6)) % 2 == 0) 1.0 else 0.15
                                val v = (sin(2.0 * PI * 2400.0 * t) * 0.65 * pulse * env).toFloat()
                                rawL = v
                                rawR = v
                            }
                            AcousticTestType.LIVE_DSP_AUDITION -> {
                                // Rich multi-band acoustic reference chord (sub-bass 45Hz + warm 110Hz + harmonic mid 330/660Hz + shimmer + gentle noise)
                                noiseSeed = (noiseSeed * 1664525L + 1013904223L) and 0xFFFFFFFFL
                                val white = ((noiseSeed ushr 16).toInt() / 32768f) - 1f
                                val bass = 0.28 * sin(2.0 * PI * 48.0 * t) + 0.18 * sin(2.0 * PI * 96.0 * t)
                                val chordL = 0.16 * sin(2.0 * PI * 220.0 * t) + 0.12 * sin(2.0 * PI * 659.25 * t) + 0.06 * sin(2.0 * PI * 2637.0 * t)
                                val chordR = 0.16 * sin(2.0 * PI * 277.18 * t) + 0.12 * sin(2.0 * PI * 880.0 * t) + 0.06 * sin(2.0 * PI * 3520.0 * t)
                                val groove = 0.72 + 0.28 * sin(2.0 * PI * 2.0 * t)
                                rawL = (((bass * groove) + chordL + white * 0.04) * env * 0.7).toFloat()
                                rawR = (((bass * groove) + chordR + white * 0.04) * env * 0.7).toFloat()
                            }
                        }

                        floatStereo[i * 2] = rawL
                        floatStereo[i * 2 + 1] = rawR
                    }

                    // Process block through the FULL real Software DSP Chain (15-band EQ, PEQ, Deep Bass, HPF/LPF, L/R Balance, Width, Limiter)
                    if (testType != AcousticTestType.FIND_EARBUDS_CHIME) {
                        dspProcessor.processStereoInterleaved(floatStereo, frameSize)
                    }

                    // Real-time Goertzel spectrum telemetry from genuine processed PCM samples
                    val realSpec = dspProcessor.analyzeRealPcmSpectrum(floatStereo)
                    _spectrumBands.value = realSpec
                    _bassEnergy.value = ((realSpec[0] + realSpec[1] + realSpec[2]) / 3f).coerceIn(0f, 1f)
                    updateTelemetrySnapshot()

                    for (k in floatStereo.indices) {
                        shortStereo[k] = (floatStereo[k] * 32767f).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    track.write(shortStereo, 0, shortStereo.size)
                    writtenFrames += frameSize
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
                _soundTestRunning.value = null
                _bassEnergy.value = 0f
                _spectrumBands.value = List(16) { 0f }
                updateTelemetrySnapshot()
            }
        }
    }

    fun stopSoundTest() {
        soundTestJob?.cancel()
        _soundTestRunning.value = null
        _bassEnergy.value = 0f
        _spectrumBands.value = List(16) { 0f }
        updateTelemetrySnapshot()
    }

    fun releaseEffects() {
        monitorJob?.cancel()
        soundTestJob?.cancel()
        smoothTransitionJob?.cancel()
        runCatching { audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) } }
        runCatching { visualizer?.release() }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.release()
            }
        }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching {
            virtualizer?.let { it.javaClass.getMethod("release").invoke(it) }
        }
        runCatching { loudnessEnhancer?.release() }
        audioDeviceCallback = null
        visualizer = null
        dynamicsProcessing = null
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}

enum class AcousticTestType(val label: String) {
    LIVE_DSP_AUDITION("Live DSP Audio Reference Loop"),
    LEFT_CHANNEL("Left Earbud Chime"),
    RIGHT_CHANNEL("Right Earbud Chime"),
    STEREO_CHIME("Stereo Balance Test"),
    SUBWOOFER_SWEEP("24–160Hz Sub Sweep"),
    FIND_EARBUDS_CHIME("Find Earbuds Acoustic Ping"),
}
