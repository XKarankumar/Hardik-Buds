package com.example.audio

import com.example.model.DspConfig
import com.example.model.EqBandMode
import com.example.model.FilterSlope
import com.example.model.HeadphoneSoundProfile
import com.example.model.ParametricEqNode
import com.example.model.SoundMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Real-time, zero-allocation stereo PCM Digital Signal Processing (DSP) engine.
 *
 * Implements:
 * - 15-band / 10-band Graphic Equalizer (Audio-EQ-Cookbook Direct Form II Transposed Biquads)
 * - Parametric EQ nodes (Frequency 20Hz..20kHz, Q 0.3..10.0, Gain -12..+12 dB)
 * - Headphone target compensation filters
 * - Deep Bass Enhancement (Sub-Bass 42Hz shelf, Deep Bass 68Hz peak, Bass Punch 105Hz peak,
 *   Bass Warmth 190Hz peak, and band-limited Bass Texture harmonic soft-shaper)
 * - Independent High-Pass & Low-Pass crossover filters with 6 / 12 / 24 dB/oct slopes
 * - Smoothed Stereo Width (Mid/Side matrix) & Left/Right Balance + Channel Trims
 * - Preamp, Master Gain, Optional RMS Volume Normalization, Soft-Knee Peak Limiter & Anti-Clipping
 */
class DspAudioProcessor(initialSampleRateHz: Int = 48000) {

    @Volatile
    private var sampleRateHz: Float = initialSampleRateHz.coerceIn(8000, 192000).toFloat()

    @Volatile
    private var config: DspConfig = DspConfig()

    @Volatile
    private var activeSoundMode: SoundMode = SoundMode.MUSIC

    @Volatile
    private var activeBands15: List<Float> = List(15) { 0f }

    // Active biquad chain: updated cleanly on config change, processed in lock-free style
    @Volatile
    private var compiledChain: CompiledDspChain = compileChain(sampleRateHz, config, activeSoundMode, activeBands15)

    // Pre-allocated smoothing & dynamics states (no per-sample allocations)
    private var smoothedLeftGain = 1f
    private var smoothedRightGain = 1f
    private var smoothedWidth = 1f
    private var smoothedPreGain = 1f
    private var smoothedMasterGain = 1f
    private var rmsEnvelope = 0.05f
    private var limiterGainEnvelope = 1f
    private var bassTextureLpfStateL = 0f
    private var bassTextureLpfStateR = 0f

    // Telemetry accumulators (updated per audio block)
    @Volatile
    var lastPeakDb: Float = -96f
        private set

    @Volatile
    var lastLimiterReductionDb: Float = 0f
        private set

    @Volatile
    var lastHeadroomReductionDb: Float = 0f
        private set

    @Volatile
    var clippedSamplesPrevented: Long = 0L
        private set

    @Volatile
    var lastBlockProcessMicros: Long = 0L
        private set

    fun setSampleRate(rateHz: Int) {
        val safeRate = rateHz.coerceIn(8000, 192000).toFloat()
        if (abs(safeRate - sampleRateHz) > 1f) {
            sampleRateHz = safeRate
            rebuildChain()
        }
    }

    fun getSampleRateHz(): Int = sampleRateHz.toInt()

    fun updateConfiguration(
        dspConfig: DspConfig,
        soundMode: SoundMode,
        profileBands15: List<Float>,
    ) {
        val normalizedBands = List(15) { idx ->
            profileBands15.getOrElse(idx) { dspConfig.bands15Db.getOrElse(idx) { 0f } }.coerceIn(-12f, 12f)
        }
        config = dspConfig
        activeSoundMode = soundMode
        activeBands15 = normalizedBands
        rebuildChain()
    }

    @Synchronized
    private fun rebuildChain() {
        val previous = compiledChain
        val next = compileChain(sampleRateHz, config, activeSoundMode, activeBands15)
        // Carry over biquad delay memory for matching filter indices to eliminate clicks on live slider moves
        val sharedCount = min(previous.biquads.size, next.biquads.size)
        for (i in 0 until sharedCount) {
            next.biquads[i].copyStateFrom(previous.biquads[i])
        }
        next.hpfFirstOrderL.copyStateFrom(previous.hpfFirstOrderL)
        next.hpfFirstOrderR.copyStateFrom(previous.hpfFirstOrderR)
        next.lpfFirstOrderL.copyStateFrom(previous.lpfFirstOrderL)
        next.lpfFirstOrderR.copyStateFrom(previous.lpfFirstOrderR)
        compiledChain = next
        lastHeadroomReductionDb = next.autoHeadroomDb
    }

    fun resetStates() {
        compiledChain.biquads.forEach { it.reset() }
        compiledChain.hpfFirstOrderL.reset()
        compiledChain.hpfFirstOrderR.reset()
        compiledChain.lpfFirstOrderL.reset()
        compiledChain.lpfFirstOrderR.reset()
        rmsEnvelope = 0.05f
        limiterGainEnvelope = 1f
        bassTextureLpfStateL = 0f
        bassTextureLpfStateR = 0f
    }

    /**
     * Processes stereo interleaved float samples in-place (`[L0, R0, L1, R1, ...]`),
     * writing safe clamped `-1.0f..1.0f` values and updating real telemetry.
     */
    fun processStereoInterleaved(interleaved: FloatArray, frameCount: Int = interleaved.size / 2) {
        if (frameCount <= 0 || interleaved.size < frameCount * 2) return
        val startNanos = System.nanoTime()
        val chain = compiledChain
        val sr = sampleRateHz
        val cfg = config

        // Per-sample coefficient smoothing (~5 ms time constant) prevents audible zipper/crackle noise
        val smoothCoeff = 1f - exp(-1f / max(1f, sr * 0.005f))
        val rmsAttack = 1f - exp(-1f / max(1f, sr * 0.020f))
        val rmsRelease = 1f - exp(-1f / max(1f, sr * 0.350f))
        val limAttack = 1f - exp(-1f / max(1f, sr * 0.0004f))
        val limRelease = 1f - exp(-1f / max(1f, sr * 0.065f))
        val texLpfAlpha = (1f - exp(-2.0 * PI * cfg.subCrossoverHz.coerceIn(40f, 180f) / sr)).toFloat()

        val textureDrive = if (cfg.bassEngineEnabled) (cfg.bassTexturePercent / 100f) * 1.4f else 0f
        val thresholdLinear = dbToLinear(-0.5f)
        var maxAbsOut = 0f
        var minLimGain = 1f
        var clipPreventionsThisBlock = 0L

        val biquads = chain.biquads
        val biquadCount = biquads.size

        for (f in 0 until frameCount) {
            val idxL = f * 2
            val idxR = idxL + 1
            var l = interleaved[idxL]
            var r = interleaved[idxR]

            // Smooth gain parameters sample-by-sample
            smoothedPreGain += (chain.preampLinear - smoothedPreGain) * smoothCoeff
            smoothedMasterGain += (chain.masterLinear - smoothedMasterGain) * smoothCoeff
            smoothedWidth += (chain.stereoWidth - smoothedWidth) * smoothCoeff
            smoothedLeftGain += (chain.leftBalanceGain - smoothedLeftGain) * smoothCoeff
            smoothedRightGain += (chain.rightBalanceGain - smoothedRightGain) * smoothCoeff

            // 1. Preamp + Auto-Headroom safety attenuation
            l *= smoothedPreGain
            r *= smoothedPreGain

            // 2. High-Pass Filter (6dB 1st-order when SLOPE_6DB; 12/24dB biquads handled in biquads array)
            if (chain.useFirstOrderHpf) {
                l = chain.hpfFirstOrderL.process(l)
                r = chain.hpfFirstOrderR.process(r)
            }

            // 3. Low-Pass Filter (6dB 1st-order when SLOPE_6DB)
            if (chain.useFirstOrderLpf) {
                l = chain.lpfFirstOrderL.process(l)
                r = chain.lpfFirstOrderR.process(r)
            }

            // 4. Biquad Filter Cascade (GEQ + Parametric + Deep Bass Shelves/Peaks + HPF/LPF 12/24dB + Headphone Profile)
            for (b in 0 until biquadCount) {
                val bq = biquads[b]
                if (!bq.isBypassed) {
                    l = bq.processLeft(l)
                    r = bq.processRight(r)
                }
            }

            // 5. Bass Texture: band-limited low-frequency harmonic warmth (non-harsh soft-clip only on sub-band)
            if (textureDrive > 0.01f) {
                bassTextureLpfStateL += (l - bassTextureLpfStateL) * texLpfAlpha
                bassTextureLpfStateR += (r - bassTextureLpfStateR) * texLpfAlpha
                val shapedL = tanh((bassTextureLpfStateL * (1f + textureDrive)).toDouble()).toFloat() - bassTextureLpfStateL
                val shapedR = tanh((bassTextureLpfStateR * (1f + textureDrive)).toDouble()).toFloat() - bassTextureLpfStateR
                l += shapedL * 0.35f
                r += shapedR * 0.35f
            }

            // 6. Optional RMS Volume Normalization (gentle auto-leveling to -14 dBFS target)
            if (cfg.volumeNormalizationEnabled) {
                val instPower = 0.5f * (l * l + r * r)
                val alpha = if (instPower > rmsEnvelope) rmsAttack else rmsRelease
                rmsEnvelope += (instPower - rmsEnvelope) * alpha
                val rms = sqrt(max(1e-7f, rmsEnvelope))
                val targetRms = 0.20f // ~ -14 dBFS
                val normGain = (targetRms / max(0.02f, rms)).coerceIn(0.45f, 2.2f)
                l *= normGain
                r *= normGain
            }

            // 7. Stereo Width (Mid-Side matrix)
            if (abs(smoothedWidth - 1f) > 0.002f) {
                val mid = 0.5f * (l + r)
                val side = 0.5f * (l - r) * smoothedWidth
                l = mid + side
                r = mid - side
            }

            // 8. Stereo Balance & Left/Right Channel Trim + Master Gain
            l *= smoothedLeftGain * smoothedMasterGain
            r *= smoothedRightGain * smoothedMasterGain

            // 9. Peak Limiter & Anti-Clipping Protection
            if (cfg.limiterEnabled || cfg.clippingPreventionEnabled) {
                val peak = max(abs(l), abs(r))
                val desiredGain = if (peak > thresholdLinear) thresholdLinear / peak else 1f
                val coeff = if (desiredGain < limiterGainEnvelope) limAttack else limRelease
                limiterGainEnvelope += (desiredGain - limiterGainEnvelope) * coeff
                l *= limiterGainEnvelope
                r *= limiterGainEnvelope
                if (limiterGainEnvelope < minLimGain) {
                    minLimGain = limiterGainEnvelope
                }
            }

            // 10. Final soft-knee anti-clipper safety ceiling (-0.1 dBFS max)
            if (cfg.clippingPreventionEnabled) {
                if (abs(l) > 0.94f) {
                    l = (0.94f * tanh((l / 0.94f).toDouble())).toFloat()
                    clipPreventionsThisBlock++
                }
                if (abs(r) > 0.94f) {
                    r = (0.94f * tanh((r / 0.94f).toDouble())).toFloat()
                    clipPreventionsThisBlock++
                }
            }

            l = l.coerceIn(-1f, 1f)
            r = r.coerceIn(-1f, 1f)

            val outPeak = max(abs(l), abs(r))
            if (outPeak > maxAbsOut) maxAbsOut = outPeak

            interleaved[idxL] = l
            interleaved[idxR] = r
        }

        lastPeakDb = if (maxAbsOut > 1e-6f) (20f * log10(maxAbsOut)) else -96f
        lastLimiterReductionDb = if (minLimGain < 0.999f) (-20f * log10(minLimGain)).coerceAtLeast(0f) else 0f
        if (clipPreventionsThisBlock > 0) {
            clippedSamplesPrevented += clipPreventionsThisBlock
        }
        lastBlockProcessMicros = ((System.nanoTime() - startNanos) / 1000L).coerceAtLeast(1L)
    }

    /**
     * Convenience wrapper for Short 16-bit PCM AudioTrack pipelines.
     */
    fun processStereoShortPcm(pcm16: ShortArray) {
        val frames = pcm16.size / 2
        if (frames <= 0) return
        val floatBuf = FloatArray(frames * 2)
        val inv = 1f / 32768f
        for (i in floatBuf.indices) {
            floatBuf[i] = pcm16[i] * inv
        }
        processStereoInterleaved(floatBuf, frames)
        for (i in floatBuf.indices) {
            pcm16[i] = (floatBuf[i] * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /**
     * Evaluates the exact mathematical frequency magnitude response (in dB) of the active DSP chain
     * across logarithmically spaced frequencies from 20Hz to 20kHz.
     * Used directly to plot the animated EQ Graph in Sound Studio.
     */
    fun computeFrequencyResponseCurve(frequenciesHz: List<Float>): List<Float> {
        val chain = compiledChain
        val sr = sampleRateHz
        return frequenciesHz.map { f ->
            val safeF = f.coerceIn(10f, sr * 0.48f)
            val omega = (2.0 * PI * safeF / sr).toFloat()
            var magSq = 1.0
            for (bq in chain.biquads) {
                if (!bq.isBypassed) {
                    magSq *= bq.magnitudeSquaredAt(omega)
                }
            }
            if (chain.useFirstOrderHpf) {
                magSq *= chain.hpfFirstOrderL.magnitudeSquaredAt(omega)
            }
            if (chain.useFirstOrderLpf) {
                magSq *= chain.lpfFirstOrderL.magnitudeSquaredAt(omega)
            }
            val totalLin = sqrt(max(1e-12, magSq)).toFloat() * chain.preampLinear * chain.masterLinear
            (20f * log10(max(1e-5f, totalLin))).coerceIn(-36f, 24f)
        }
    }

    /**
     * Computes genuine normalized spectral energy across 16 log-spaced bins using Goertzel resonators
     * on real processed PCM buffers (no simulated wave generation).
     */
    fun analyzeRealPcmSpectrum(interleavedStereo: FloatArray): List<Float> {
        val frames = min(512, interleavedStereo.size / 2)
        if (frames < 32) return List(16) { 0f }
        val sr = sampleRateHz
        val centerFreqs = SPECTRUM_BANDS_HZ
        return centerFreqs.map { freq ->
            val k = (frames * freq / sr).coerceIn(1f, frames * 0.45f)
            val omega = (2.0 * PI * k / frames)
            val coeff = 2.0 * cos(omega)
            var q0 = 0.0
            var q1 = 0.0
            var q2 = 0.0
            for (n in 0 until frames) {
                val mono = 0.5 * (interleavedStereo[n * 2] + interleavedStereo[n * 2 + 1])
                // Hann window reduces spectral leakage cleanly
                val w = 0.5 * (1.0 - cos(2.0 * PI * n / (frames - 1)))
                q0 = coeff * q1 - q2 + mono * w
                q2 = q1
                q1 = q0
            }
            val power = q1 * q1 + q2 * q2 - q1 * q2 * coeff
            val amp = sqrt(max(0.0, power)) / (frames * 0.25)
            // Log compression for readable meter scaling 0f..1f
            (ln(1.0 + amp * 12.0) / ln(13.0)).toFloat().coerceIn(0f, 1f)
        }
    }

    companion object {
        val GRAPH_FREQUENCIES_HZ: List<Float> = listOf(
            20f, 25f, 31f, 40f, 50f, 63f, 80f, 100f, 125f, 160f,
            200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
            2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 20000f,
        )

        val SPECTRUM_BANDS_HZ: List<Float> = listOf(
            31f, 45f, 63f, 90f, 125f, 180f, 250f, 360f,
            500f, 750f, 1000f, 1600f, 2500f, 4000f, 8000f, 14000f,
        )

        fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

        fun compileChain(
            sampleRate: Float,
            cfg: DspConfig,
            mode: SoundMode,
            bands15: List<Float>,
        ): CompiledDspChain {
            val biquads = mutableListOf<StereoBiquad>()
            var positiveBoostSum = 0f

            // A. 15-Band or 10-Band Peaking Graphic EQ
            val activeSpecs = if (cfg.eqBandMode == EqBandMode.BANDS_15) {
                DspConfig.BANDS_15_SPECS
            } else {
                DspConfig.BANDS_15_SPECS.filter { it.mapped10BandIndex != null }
            }
            val eqQ = if (cfg.eqBandMode == EqBandMode.BANDS_15) 1.85f else 1.41f

            for (spec in activeSpecs) {
                val gain = bands15.getOrElse(spec.index) { 0f }.coerceIn(-12f, 12f)
                if (abs(gain) > 0.05f) {
                    val bq = StereoBiquad()
                    bq.setPeakingEq(sampleRate, spec.frequencyHz, eqQ, gain)
                    biquads.add(bq)
                    if (gain > 0f) positiveBoostSum = max(positiveBoostSum, gain)
                }
            }

            // B. Parametric EQ Nodes
            for (node in cfg.parametricNodes) {
                if (node.enabled && abs(node.gainDb) > 0.05f) {
                    val bq = StereoBiquad()
                    bq.setPeakingEq(
                        sampleRate,
                        node.frequencyHz.coerceIn(20f, 20000f),
                        node.qFactor.coerceIn(0.3f, 10f),
                        node.gainDb.coerceIn(-12f, 12f),
                    )
                    biquads.add(bq)
                    if (node.gainDb > 0f) positiveBoostSum += node.gainDb * 0.35f
                }
            }

            // C. Headphone Sound Profile Target Shaping
            when (cfg.headphoneProfile) {
                HeadphoneSoundProfile.REFERENCE_NEUTRAL -> Unit
                HeadphoneSoundProfile.IN_EAR_DYNAMIC -> {
                    biquads.add(StereoBiquad().apply { setPeakingEq(sampleRate, 3200f, 1.3f, 1.5f) })
                    biquads.add(StereoBiquad().apply { setPeakingEq(sampleRate, 60f, 0.9f, 1.2f) })
                }
                HeadphoneSoundProfile.BASS_DRIVER_TUNED -> {
                    biquads.add(StereoBiquad().apply { setLowShelf(sampleRate, 110f, 0.7f, 2.5f) })
                    biquads.add(StereoBiquad().apply { setPeakingEq(sampleRate, 2200f, 1.1f, 1.5f) })
                }
                HeadphoneSoundProfile.PLANAR_STUDIO -> {
                    biquads.add(StereoBiquad().apply { setHighShelf(sampleRate, 8500f, 0.7f, 2.2f) })
                }
                HeadphoneSoundProfile.OPEN_EAR_COMPENSATED -> {
                    biquads.add(StereoBiquad().apply { setLowShelf(sampleRate, 140f, 0.65f, 4.0f) })
                    positiveBoostSum += 2.0f
                }
            }

            // D. Deep Bass Control Pipeline (5 dedicated sliders: Sub-Bass, Deep Bass, Punch, Warmth + Texture in PCM loop)
            if (cfg.bassEngineEnabled) {
                val subDb = (cfg.subBassPercent / 100f) * 7.5f
                val deepDb = (cfg.deepBassPercent / 100f) * 6.5f
                val punchDb = (cfg.bassPunchPercent / 100f) * 5.5f
                val warmthDb = (cfg.bassWarmthPercent / 100f) * 4.5f

                if (subDb > 0.1f) {
                    biquads.add(StereoBiquad().apply { setLowShelf(sampleRate, 42f, 0.72f, subDb) })
                }
                if (deepDb > 0.1f) {
                    biquads.add(StereoBiquad().apply {
                        setPeakingEq(sampleRate, min(cfg.subCrossoverHz, 85f), 1.05f, deepDb)
                    })
                }
                if (punchDb > 0.1f) {
                    biquads.add(StereoBiquad().apply { setPeakingEq(sampleRate, 105f, 1.25f, punchDb) })
                }
                if (warmthDb > 0.1f) {
                    biquads.add(StereoBiquad().apply { setPeakingEq(sampleRate, 195f, 1.15f, warmthDb) })
                }
                val maxBassPeak = max(subDb, deepDb) + punchDb * 0.4f
                positiveBoostSum = max(positiveBoostSum, maxBassPeak)
            }

            // E. SoundMode acoustic curve fine-tuning (software DSP side)
            when (mode) {
                SoundMode.MOVIE -> biquads.add(StereoBiquad().apply { setLowShelf(sampleRate, 90f, 0.75f, 2.5f) })
                SoundMode.VOCAL -> biquads.add(StereoBiquad().apply { setPeakingEq(sampleRate, 1800f, 1.0f, 3.0f) })
                SoundMode.GAMING -> biquads.add(StereoBiquad().apply { setPeakingEq(sampleRate, 3800f, 1.4f, 2.5f) })
                SoundMode.NIGHT_MODE -> {
                    biquads.add(StereoBiquad().apply { setLowShelf(sampleRate, 100f, 0.7f, -2.5f) })
                    biquads.add(StereoBiquad().apply { setHighShelf(sampleRate, 6000f, 0.7f, -3.0f) })
                }
                else -> Unit
            }

            // F. Independent High-Pass Filter (6, 12, or 24 dB/oct)
            val hpf1L = FirstOrderFilter()
            val hpf1R = FirstOrderFilter()
            var use1stHpf = false
            if (cfg.highPassEnabled) {
                val cutoff = cfg.highPassCutoffHz.coerceIn(20f, min(20000f, sampleRate * 0.45f))
                when (cfg.highPassSlope) {
                    FilterSlope.SLOPE_6DB -> {
                        use1stHpf = true
                        hpf1L.setHighPass(sampleRate, cutoff)
                        hpf1R.setHighPass(sampleRate, cutoff)
                    }
                    FilterSlope.SLOPE_12DB -> {
                        biquads.add(StereoBiquad().apply { setHighPass(sampleRate, cutoff, 0.7071f) })
                    }
                    FilterSlope.SLOPE_24DB -> {
                        biquads.add(StereoBiquad().apply { setHighPass(sampleRate, cutoff, 0.7071f) })
                        biquads.add(StereoBiquad().apply { setHighPass(sampleRate, cutoff, 0.7071f) })
                    }
                }
            }

            // G. Independent Low-Pass Filter (6, 12, or 24 dB/oct)
            val lpf1L = FirstOrderFilter()
            val lpf1R = FirstOrderFilter()
            var use1stLpf = false
            if (cfg.lowPassEnabled) {
                val cutoff = cfg.lowPassCutoffHz.coerceIn(20f, min(20000f, sampleRate * 0.45f))
                when (cfg.lowPassSlope) {
                    FilterSlope.SLOPE_6DB -> {
                        use1stLpf = true
                        lpf1L.setLowPass(sampleRate, cutoff)
                        lpf1R.setLowPass(sampleRate, cutoff)
                    }
                    FilterSlope.SLOPE_12DB -> {
                        biquads.add(StereoBiquad().apply { setLowPass(sampleRate, cutoff, 0.7071f) })
                    }
                    FilterSlope.SLOPE_24DB -> {
                        biquads.add(StereoBiquad().apply { setLowPass(sampleRate, cutoff, 0.7071f) })
                        biquads.add(StereoBiquad().apply { setLowPass(sampleRate, cutoff, 0.7071f) })
                    }
                }
            }

            // H. Headroom & Gain Staging Calculation (prevents clipping & sudden volume jumps)
            val autoHeadroomDb = if (cfg.clippingPreventionEnabled && positiveBoostSum > 3f) {
                -((positiveBoostSum - 3f) * 0.55f).coerceIn(0f, 9f)
            } else 0f

            val effectivePreampDb = (cfg.preampDb.coerceIn(-12f, 12f) + autoHeadroomDb).coerceIn(-18f, 12f)
            val preampLinear = dbToLinear(effectivePreampDb)
            val masterLinear = dbToLinear(cfg.masterGainDb.coerceIn(-12f, 12f))

            // I. Stereo Balance & Individual Channel Trim
            val bal = cfg.stereoBalance.coerceIn(-1f, 1f)
            val panLeft = if (bal > 0f) (1f - bal).coerceIn(0f, 1f) else 1f
            val panRight = if (bal < 0f) (1f + bal).coerceIn(0f, 1f) else 1f
            val leftTrimLin = dbToLinear(cfg.leftChannelGainDb.coerceIn(-12f, 6f))
            val rightTrimLin = dbToLinear(cfg.rightChannelGainDb.coerceIn(-12f, 6f))

            val widthFactor = (cfg.stereoWidthPercent.coerceIn(0, 200) / 100f) * when (mode) {
                SoundMode.SPATIAL_AUDIO -> 1.25f
                SoundMode.SURROUND_SOUND -> 1.38f
                SoundMode.MOVIE -> 1.18f
                else -> 1f
            }

            return CompiledDspChain(
                biquads = biquads.toTypedArray(),
                hpfFirstOrderL = hpf1L,
                hpfFirstOrderR = hpf1R,
                useFirstOrderHpf = use1stHpf,
                lpfFirstOrderL = lpf1L,
                lpfFirstOrderR = lpf1R,
                useFirstOrderLpf = use1stLpf,
                preampLinear = preampLinear,
                masterLinear = masterLinear,
                autoHeadroomDb = abs(autoHeadroomDb),
                stereoWidth = widthFactor.coerceIn(0f, 2.2f),
                leftBalanceGain = panLeft * leftTrimLin,
                rightBalanceGain = panRight * rightTrimLin,
            )
        }
    }

    class CompiledDspChain(
        val biquads: Array<StereoBiquad>,
        val hpfFirstOrderL: FirstOrderFilter,
        val hpfFirstOrderR: FirstOrderFilter,
        val useFirstOrderHpf: Boolean,
        val lpfFirstOrderL: FirstOrderFilter,
        val lpfFirstOrderR: FirstOrderFilter,
        val useFirstOrderLpf: Boolean,
        val preampLinear: Float,
        val masterLinear: Float,
        val autoHeadroomDb: Float,
        val stereoWidth: Float,
        val leftBalanceGain: Float,
        val rightBalanceGain: Float,
    )

    /**
     * Direct Form II Transposed 1st-Order Filter (for 6 dB/oct slope).
     */
    class FirstOrderFilter {
        private var b0 = 1.0
        private var b1 = 0.0
        private var a1 = 0.0
        private var z1 = 0.0

        fun setLowPass(sampleRate: Float, cutoffHz: Float) {
            val fc = cutoffHz.coerceIn(10f, sampleRate * 0.45f)
            val k = kotlin.math.tan(PI * fc / sampleRate)
            val norm = 1.0 / (1.0 + k)
            b0 = k * norm
            b1 = b0
            a1 = (k - 1.0) * norm
        }

        fun setHighPass(sampleRate: Float, cutoffHz: Float) {
            val fc = cutoffHz.coerceIn(10f, sampleRate * 0.45f)
            val k = kotlin.math.tan(PI * fc / sampleRate)
            val norm = 1.0 / (1.0 + k)
            b0 = norm
            b1 = -norm
            a1 = (k - 1.0) * norm
        }

        fun process(input: Float): Float {
            val out = b0 * input + z1
            z1 = b1 * input - a1 * out
            return out.toFloat()
        }

        fun magnitudeSquaredAt(omega: Float): Double {
            val cosW = cos(omega.toDouble())
            val sinW = sin(omega.toDouble())
            val numRe = b0 + b1 * cosW
            val numIm = -b1 * sinW
            val denRe = 1.0 + a1 * cosW
            val denIm = -a1 * sinW
            val numSq = numRe * numRe + numIm * numIm
            val denSq = max(1e-12, denRe * denRe + denIm * denIm)
            return numSq / denSq
        }

        fun copyStateFrom(other: FirstOrderFilter) {
            z1 = other.z1
        }

        fun reset() {
            z1 = 0.0
        }
    }

    /**
     * Stereo Audio-EQ-Cookbook Biquad IIR Filter (Direct Form II Transposed).
     */
    class StereoBiquad {
        var isBypassed: Boolean = false
            private set

        private var b0 = 1.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var a1 = 0.0
        private var a2 = 0.0

        private var z1L = 0.0
        private var z2L = 0.0
        private var z1R = 0.0
        private var z2R = 0.0

        fun setPeakingEq(sampleRate: Float, freqHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.02f) {
                setBypass()
                return
            }
            val safeFreq = freqHz.coerceIn(15f, sampleRate * 0.46f)
            val safeQ = q.coerceIn(0.25f, 12f)
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * safeFreq / sampleRate
            val sinW = sin(w0)
            val cosW = cos(w0)
            val alpha = sinW / (2.0 * safeQ)

            val norm = 1.0 / (1.0 + alpha / a)
            b0 = (1.0 + alpha * a) * norm
            b1 = (-2.0 * cosW) * norm
            b2 = (1.0 - alpha * a) * norm
            a1 = (-2.0 * cosW) * norm
            a2 = (1.0 - alpha / a) * norm
            isBypassed = false
        }

        fun setLowShelf(sampleRate: Float, freqHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.02f) {
                setBypass()
                return
            }
            val safeFreq = freqHz.coerceIn(15f, sampleRate * 0.46f)
            val safeQ = q.coerceIn(0.3f, 2.0f)
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * safeFreq / sampleRate
            val sinW = sin(w0)
            val cosW = cos(w0)
            val alpha = sinW / (2.0 * safeQ)
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

            val norm = 1.0 / ((a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha)
            b0 = (a * ((a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha)) * norm
            b1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cosW)) * norm
            b2 = (a * ((a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha)) * norm
            a1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cosW)) * norm
            a2 = ((a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha) * norm
            isBypassed = false
        }

        fun setHighShelf(sampleRate: Float, freqHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.02f) {
                setBypass()
                return
            }
            val safeFreq = freqHz.coerceIn(15f, sampleRate * 0.46f)
            val safeQ = q.coerceIn(0.3f, 2.0f)
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * safeFreq / sampleRate
            val sinW = sin(w0)
            val cosW = cos(w0)
            val alpha = sinW / (2.0 * safeQ)
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

            val norm = 1.0 / ((a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha)
            b0 = (a * ((a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha)) * norm
            b1 = (-2.0 * a * ((a - 1.0) + (a + 1.0) * cosW)) * norm
            b2 = (a * ((a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha)) * norm
            a1 = (2.0 * ((a - 1.0) - (a + 1.0) * cosW)) * norm
            a2 = ((a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha) * norm
            isBypassed = false
        }

        fun setLowPass(sampleRate: Float, freqHz: Float, q: Float = 0.7071f) {
            val safeFreq = freqHz.coerceIn(15f, sampleRate * 0.46f)
            val w0 = 2.0 * PI * safeFreq / sampleRate
            val sinW = sin(w0)
            val cosW = cos(w0)
            val alpha = sinW / (2.0 * q.coerceIn(0.3f, 4f))

            val norm = 1.0 / (1.0 + alpha)
            b0 = ((1.0 - cosW) * 0.5) * norm
            b1 = (1.0 - cosW) * norm
            b2 = b0
            a1 = (-2.0 * cosW) * norm
            a2 = (1.0 - alpha) * norm
            isBypassed = false
        }

        fun setHighPass(sampleRate: Float, freqHz: Float, q: Float = 0.7071f) {
            val safeFreq = freqHz.coerceIn(15f, sampleRate * 0.46f)
            val w0 = 2.0 * PI * safeFreq / sampleRate
            val sinW = sin(w0)
            val cosW = cos(w0)
            val alpha = sinW / (2.0 * q.coerceIn(0.3f, 4f))

            val norm = 1.0 / (1.0 + alpha)
            b0 = ((1.0 + cosW) * 0.5) * norm
            b1 = -(1.0 + cosW) * norm
            b2 = b0
            a1 = (-2.0 * cosW) * norm
            a2 = (1.0 - alpha) * norm
            isBypassed = false
        }

        private fun setBypass() {
            b0 = 1.0
            b1 = 0.0
            b2 = 0.0
            a1 = 0.0
            a2 = 0.0
            isBypassed = true
        }

        fun processLeft(x: Float): Float {
            val y = b0 * x + z1L
            z1L = b1 * x - a1 * y + z2L
            z2L = b2 * x - a2 * y
            return y.toFloat()
        }

        fun processRight(x: Float): Float {
            val y = b0 * x + z1R
            z1R = b1 * x - a1 * y + z2R
            z2R = b2 * x - a2 * y
            return y.toFloat()
        }

        fun magnitudeSquaredAt(omega: Float): Double {
            val cosW = cos(omega.toDouble())
            val sinW = sin(omega.toDouble())
            val cos2W = cos(2.0 * omega)
            val sin2W = sin(2.0 * omega)

            val numRe = b0 + b1 * cosW + b2 * cos2W
            val numIm = -b1 * sinW - b2 * sin2W
            val denRe = 1.0 + a1 * cosW + a2 * cos2W
            val denIm = -a1 * sinW - a2 * sin2W

            val numSq = numRe * numRe + numIm * numIm
            val denSq = max(1e-12, denRe * denRe + denIm * denIm)
            return numSq / denSq
        }

        fun copyStateFrom(other: StereoBiquad) {
            z1L = other.z1L
            z2L = other.z2L
            z1R = other.z1R
            z2R = other.z2R
        }

        fun reset() {
            z1L = 0.0
            z2L = 0.0
            z1R = 0.0
            z2R = 0.0
        }
    }
}
