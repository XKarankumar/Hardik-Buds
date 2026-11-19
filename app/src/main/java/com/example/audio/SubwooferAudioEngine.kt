package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Visualizer
import android.os.Build
import com.example.model.AudioEngineCapabilities
import com.example.model.AudioOutputRoute
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
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin

class SubwooferAudioEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Default)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Any? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var visualizer: Visualizer? = null

    private var activeSessionId: Int = 0
    private var soundTestJob: Job? = null

    private val _capabilities = MutableStateFlow(AudioEngineCapabilities())
    val capabilities: StateFlow<AudioEngineCapabilities> = _capabilities.asStateFlow()

    private val _bassEnergy = MutableStateFlow(0f) // 0f..1f real FFT or playback pulse
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

    private var pulseJob: Job? = null
    private var smoothTransitionJob: Job? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    fun initialize() {
        releaseEffects()

        // Try global output mix session 0 first, then fallback to generated app session ID
        val candidateSessions = listOf(0, audioManager.generateAudioSessionId())
        var attachedSession = 0
        var eqOk = false

        for (session in candidateSessions) {
            val eqAttempt = runCatching {
                Equalizer(0, session).apply { enabled = true }
            }.getOrNull()
            if (eqAttempt != null) {
                equalizer = eqAttempt
                attachedSession = session
                eqOk = true
                break
            }
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

        val bandCount = runCatching { equalizer?.numberOfBands?.toInt() ?: 0 }.getOrDefault(0)
        val range = runCatching { equalizer?.bandLevelRange }.getOrNull()
        val minDb = (range?.getOrNull(0) ?: -1200) / 100f
        val maxDb = (range?.getOrNull(1) ?: 1200) / 100f

        val sessionModeDesc = if (attachedSession == 0 && eqOk) {
            "Global Output Mix (Session 0) — active across supported media players"
        } else if (eqOk) {
            "App Audio Session ($attachedSession) — OS restricted global session 0"
        } else {
            "AudioEffect unavailable on this device/emulator hardware"
        }

        val virtSupported = virtualizer?.let { inst ->
            runCatching {
                inst.javaClass.getMethod("getStrengthSupported").invoke(inst) as? Boolean
            }.getOrNull()
        } == true

        val sampleRate = runCatching {
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        }.getOrNull() ?: 48000

        val routeLabel = runCatching {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val btOut = devices.firstOrNull {
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            btOut?.productName?.toString()?.takeIf { it.isNotBlank() }?.let { "Bluetooth · $it" }
                ?: if (btOut != null) "Bluetooth A2DP Audio" else "Device Speaker / Wired"
        }.getOrDefault("System Audio Out")

        _capabilities.value = AudioEngineCapabilities(
            equalizerSupported = eqOk && bandCount > 0,
            hardwareBandCount = bandCount,
            hardwareMinDb = minDb,
            hardwareMaxDb = maxDb,
            bassBoostSupported = bassBoost?.strengthSupported == true,
            virtualizerSupported = virtSupported,
            loudnessSupported = loudnessEnhancer != null,
            visualizerActive = false,
            activeAudioSessionId = attachedSession,
            outputSampleRateHz = sampleRate,
            outputRouteLabel = routeLabel,
            engineStatusMessage = sessionModeDesc,
            limitationNotice = if (attachedSession == 0) {
                "Android AudioEffect processes media streams routed through the device audio HAL. Hardware-level ANC & vendor subwoofer chips remain controlled by OEM firmware."
            } else {
                "This Android OS build restricts third-party access to global session 0; DSP applies to in-app acoustic audio."
            },
        )

        registerAudioDeviceMonitor()
        refreshAudioOutputRoutes()
        startReactiveEnergyMonitor()
    }

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
                // Smooth sine ease-in-out curve for glitch-free Home Theatre handoff
                val eased = (0.5f * (1f - kotlin.math.cos(Math.PI * frac))).toFloat()
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

    fun applyAudioPipeline(
        subwoofer: SubwooferConfig,
        eqProfile: EqualizerProfile,
        soundMode: SoundMode,
        lowLatencyHint: Boolean,
    ) {
        refreshPlaybackAndVolume()

        // 1. Apply Equalizer mapped onto hardware bands, blending Subwoofer Engine X sub-bands
        val eq = equalizer
        if (eq != null) {
            runCatching {
                eq.enabled = true
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()

                // Combine 10-band EQ with Subwoofer Engine X low-frequency shaping
                val eqCenterHz = listOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

                for (hwBand in 0 until numBands) {
                    val centerHz = eq.getCenterFreq(hwBand.toShort()) / 1000f
                    var targetDb = interpolateDb(centerHz, eqCenterHz, eqProfile.bandsDb)

                    // Blend Subwoofer Engine X 20Hz..200Hz curve if enabled
                    if (subwoofer.enabled && centerHz <= subwoofer.lowFreqCutoffHz * 1.4f) {
                        val subCurve = listOf(
                            20f to subwoofer.band20HzDb,
                            31f to subwoofer.band31HzDb,
                            40f to subwoofer.band40HzDb,
                            63f to subwoofer.band63HzDb,
                            80f to subwoofer.band80HzDb,
                            125f to subwoofer.band125HzDb,
                            200f to subwoofer.band200HzDb,
                        )
                        val subExtra = interpolateDb(
                            centerHz,
                            subCurve.map { it.first },
                            subCurve.map { it.second },
                        )
                        val intensityScale = (subwoofer.bassIntensity / 100f)
                        val deepPunchWeight = ((subwoofer.deepBass + subwoofer.punchBass) / 200f)
                        targetDb += subExtra * intensityScale * (0.6f + 0.5f * deepPunchWeight)
                    }

                    // Sound mode fine tuning
                    targetDb += when (soundMode) {
                        SoundMode.NIGHT_MODE -> if (centerHz > 4000f) -2.5f else if (centerHz < 80f) -1.5f else 0.5f
                        SoundMode.VOCAL -> if (centerHz in 700f..3500f) 2.5f else 0f
                        SoundMode.BASS -> if (centerHz < 150f) 2.0f else 0f
                        SoundMode.GAMING -> if (centerHz in 2000f..6000f) 2.0f else 0f
                        else -> 0f
                    }

                    val mB = (targetDb * 100f).roundToInt().coerceIn(minLevel, maxLevel)
                    eq.setBandLevel(hwBand.toShort(), mB.toShort())
                }
            }
        }

        // 2. Apply BassBoost (0..1000)
        val bb = bassBoost
        if (bb != null) {
            runCatching {
                if (subwoofer.enabled && bb.strengthSupported) {
                    bb.enabled = true
                    val combinedStrength = (
                        subwoofer.bassIntensity * 0.45f +
                            subwoofer.subwooferSimulation * 0.35f +
                            subwoofer.deepBass * 0.20f
                        ).roundToInt().coerceIn(0, 100) * 10
                    bb.setStrength(combinedStrength.toShort())
                } else {
                    bb.enabled = false
                }
            }
        }

        // 3. Apply Virtualizer (Spatial / Surround / Movie) via reflection-safe AudioEffect
        val virt = virtualizer
        if (virt != null) {
            runCatching {
                val clazz = virt.javaClass
                val supported = clazz.getMethod("getStrengthSupported").invoke(virt) as? Boolean == true
                val setEnabled = clazz.getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                if (soundMode.isSpatial && supported) {
                    setEnabled.invoke(virt, true)
                    val strength = when (soundMode) {
                        SoundMode.SPATIAL_AUDIO -> 850
                        SoundMode.SURROUND_SOUND -> 950
                        SoundMode.MOVIE -> 700
                        else -> 500
                    }
                    clazz.getMethod("setStrength", Short::class.javaPrimitiveType).invoke(virt, strength.toShort())
                } else {
                    setEnabled.invoke(virt, false)
                }
            }
        }

        // 4. Apply LoudnessEnhancer (Punch / Cinema / Vocal clarity)
        val loud = loudnessEnhancer
        if (loud != null) {
            runCatching {
                val gainMb = when {
                    soundMode == SoundMode.NIGHT_MODE -> 250
                    soundMode == SoundMode.MOVIE -> 350
                    subwoofer.enabled && subwoofer.punchBass > 65 -> ((subwoofer.punchBass - 50) * 8)
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

        // 5. Low-latency audio routing hint (when supported on API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                audioManager.mode = if (lowLatencyHint || soundMode.isLowLatency) {
                    AudioManager.MODE_NORMAL
                } else {
                    AudioManager.MODE_NORMAL
                }
            }
        }
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
                            if (fft != null && fft.size >= 8) {
                                // Compute low-frequency sub-bass magnitude from first few FFT bins
                                var sum = 0f
                                val bins = minOf(10, fft.size / 2)
                                for (k in 1 until bins) {
                                    val r = fft[2 * k].toFloat()
                                    val im = fft[2 * k + 1].toFloat()
                                    sum += kotlin.math.hypot(r, im)
                                }
                                val normalized = (sum / (bins * 64f)).coerceIn(0f, 1f)
                                _bassEnergy.value = normalized
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

    private fun startReactiveEnergyMonitor() {
        pulseJob?.cancel()
        pulseJob = scope.launch {
            var phase = 0.0
            while (isActive) {
                refreshPlaybackAndVolume()
                if (visualizer == null) {
                    if (_isMusicPlaying.value) {
                        phase += 0.25
                        _bassEnergy.value = (0.35f + 0.30f * abs(sin(phase)).toFloat()).coerceIn(0f, 1f)
                    } else {
                        _bassEnergy.value = 0.08f
                    }
                }
                delay(80L)
            }
        }
    }

    fun runAcousticSoundTest(testType: AcousticTestType, balance: Float = 0f) {
        soundTestJob?.cancel()
        soundTestJob = scope.launch {
            _soundTestRunning.value = testType.label
            val sampleRate = 44100
            val durationSec = when (testType) {
                AcousticTestType.SUBWOOFER_SWEEP -> 3.0
                else -> 1.6
            }
            val totalSamples = (sampleRate * durationSec).toInt()
            val stereoBuffer = ShortArray(totalSamples * 2)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val env = when {
                    i < 2200 -> i / 2200.0
                    i > totalSamples - 2200 -> (totalSamples - i) / 2200.0
                    else -> 1.0
                }

                val sampleVal = when (testType) {
                    AcousticTestType.LEFT_CHANNEL,
                    AcousticTestType.RIGHT_CHANNEL,
                    AcousticTestType.STEREO_CHIME -> {
                        val freq = if (i < totalSamples / 2) 523.25 else 659.25 // C5 -> E5 luxury chime
                        (sin(2.0 * PI * freq * t) * 0.45 * env * Short.MAX_VALUE).toInt()
                    }
                    AcousticTestType.SUBWOOFER_SWEEP -> {
                        // Logarithmic-like sweep 25Hz -> 160Hz
                        val frac = i.toDouble() / totalSamples
                        val freq = 25.0 + 135.0 * frac
                        (sin(2.0 * PI * freq * t) * 0.60 * env * Short.MAX_VALUE).toInt()
                    }
                    AcousticTestType.FIND_EARBUDS_CHIME -> {
                        val pulse = if ((i / (sampleRate / 6)) % 2 == 0) 1.0 else 0.2
                        (sin(2.0 * PI * 2400.0 * t) * 0.70 * pulse * env * Short.MAX_VALUE).toInt()
                    }
                }

                val leftGain = when (testType) {
                    AcousticTestType.RIGHT_CHANNEL -> 0f
                    AcousticTestType.LEFT_CHANNEL -> 1f
                    else -> if (balance > 0f) 1f - balance * 0.8f else 1f
                }
                val rightGain = when (testType) {
                    AcousticTestType.LEFT_CHANNEL -> 0f
                    AcousticTestType.RIGHT_CHANNEL -> 1f
                    else -> if (balance < 0f) 1f + balance * 0.8f else 1f
                }

                stereoBuffer[2 * i] = (sampleVal * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                stereoBuffer[2 * i + 1] = (sampleVal * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            runCatching {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(maxOf(minBuf, stereoBuffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(stereoBuffer, 0, stereoBuffer.size)
                track.play()
                delay((durationSec * 1000).toLong() + 100L)
                track.stop()
                track.release()
            }
            _soundTestRunning.value = null
        }
    }

    fun stopSoundTest() {
        soundTestJob?.cancel()
        _soundTestRunning.value = null
    }

    private fun interpolateDb(freqHz: Float, centers: List<Float>, values: List<Float>): Float {
        if (centers.isEmpty() || values.isEmpty()) return 0f
        if (freqHz <= centers.first()) return values.first()
        if (freqHz >= centers.last()) return values.last()
        for (i in 0 until centers.size - 1) {
            val f0 = centers[i]
            val f1 = centers[i + 1]
            if (freqHz in f0..f1) {
                val ratio = ((log10(freqHz) - log10(f0)) / (log10(f1) - log10(f0))).coerceIn(0f, 1f)
                return values[i] + (values[i + 1] - values[i]) * ratio
            }
        }
        return 0f
    }

    fun releaseEffects() {
        pulseJob?.cancel()
        soundTestJob?.cancel()
        smoothTransitionJob?.cancel()
        runCatching { audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) } }
        runCatching { visualizer?.release() }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching {
            virtualizer?.let { it.javaClass.getMethod("release").invoke(it) }
        }
        runCatching { loudnessEnhancer?.release() }
        audioDeviceCallback = null
        visualizer = null
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}

enum class AcousticTestType(val label: String) {
    LEFT_CHANNEL("Left Earbud Chime"),
    RIGHT_CHANNEL("Right Earbud Chime"),
    STEREO_CHIME("Stereo Balance Test"),
    SUBWOOFER_SWEEP("25–160Hz Sub Sweep"),
    FIND_EARBUDS_CHIME("Find Earbuds Acoustic Ping"),
}
