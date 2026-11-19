package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AcousticTestType
import com.example.audio.DspAudioProcessor
import com.example.model.AudioEngineCapabilities
import com.example.model.DspConfig
import com.example.model.DspLiveTelemetry
import com.example.model.DspVerificationStatus
import com.example.model.FilterSlope
import com.example.model.HeadphoneSoundProfile
import com.example.model.ParametricEqNode
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.RedSignalDot
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import kotlin.math.log10
import kotlin.math.roundToInt

@Composable
fun SoundEngineStatusCard(
    capabilities: AudioEngineCapabilities,
    telemetry: DspLiveTelemetry,
    soundTestRunning: String?,
    onToggleAuditionLoop: () -> Unit,
    onResetAllDsp: () -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    var showLayers by remember { mutableStateOf(false) }
    val isAnyActive = telemetry.verificationStatus != DspVerificationStatus.STANDBY

    LuxGlassCard(
        modifier = Modifier.fillMaxWidth().testTag("sound_engine_status_card"),
        highlightRedBorder = isAnyActive,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StatusBadgePill(
                    label = telemetry.verificationStatus.badgeLabel,
                    isRedActive = isAnyActive,
                    modifier = Modifier.testTag("dsp_verification_badge"),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = capabilities.engineStatusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onResetAllDsp,
                modifier = Modifier.testTag("dsp_reset_all_btn"),
            ) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset DSP", tint = extras.signalRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("RESET DSP", color = extras.signalRed)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadgePill(label = "${telemetry.activeSampleRateHz} Hz")
            StatusBadgePill(
                label = if (telemetry.dynamicsProcessingActive) "HAL 15-BAND" else "PCM BIQUAD",
                isRedActive = telemetry.dynamicsProcessingActive || telemetry.softwarePcmActive,
            )
            StatusBadgePill(
                label = if (telemetry.limiterGainReductionDb > 0.1f) {
                    "LIMIT -%.1fdB".format(telemetry.limiterGainReductionDb)
                } else {
                    "SAFE PEAK"
                },
                isRedActive = telemetry.limiterGainReductionDb > 0.1f,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val auditionActive = soundTestRunning == AcousticTestType.LIVE_DSP_AUDITION.label
            LuxActionButton(
                text = if (auditionActive) "Stop Audition Loop" else "Audition Live DSP",
                icon = if (auditionActive) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                useRedAccent = auditionActive,
                filled = auditionActive,
                onClick = onToggleAuditionLoop,
                modifier = Modifier.weight(1f).testTag("live_dsp_audition_btn"),
            )
            LuxActionButton(
                text = if (showLayers) "Hide Layers" else "4-Layer Scope",
                icon = if (showLayers) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                filled = false,
                onClick = { showLayers = !showLayers },
                modifier = Modifier.weight(1f).testTag("toggle_dsp_layers_btn"),
            )
        }

        AnimatedVisibility(visible = showLayers) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                telemetry.layers.forEach { layer ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(layer.layerTitle.uppercase(), style = MaterialTheme.typography.labelLarge)
                            Text(layer.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        StatusBadgePill(label = layer.statusBadge, isRedActive = layer.isActive)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedEqResponseGraphCard(
    eqCurveDb: List<Float>,
    spectrumBands: List<Float>,
    dspConfig: DspConfig,
    telemetry: DspLiveTelemetry,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val hasLiveSpectrum = spectrumBands.any { it > 0.015f }

    LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("animated_eq_graph_card")) {
        SectionHeader(
            title = "Real-Time DSP Frequency Curve",
            subtitle = when {
                telemetry.softwarePcmActive -> "LIVE PCM SPECTRUM + BIQUAD RESPONSE"
                hasLiveSpectrum -> "LIVE FFT SPECTRUM + DSP RESPONSE"
                else -> "EXACT TRANSFER RESPONSE (20Hz – 20kHz)"
            },
            trailing = { RedSignalDot(active = hasLiveSpectrum || telemetry.softwarePcmActive, size = 8.dp) },
        )
        Spacer(Modifier.height(10.dp))

        // Animate curve control points smoothly at 60-120Hz
        val animatedCurve = eqCurveDb.mapIndexed { idx, db ->
            val v by animateFloatAsState(
                targetValue = db.coerceIn(-24f, 18f),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "curvePt$idx",
            )
            v
        }

        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        val accent = extras.signalRed

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .testTag("eq_frequency_graph_canvas"),
        ) {
            val w = size.width
            val h = size.height
            val minDb = -18f
            val maxDb = 16f
            fun dbToY(db: Float): Float {
                val frac = ((db - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
                return h - (frac * h)
            }

            // Horizontal dB reference lines (+12, 0, -12)
            listOf(12f, 0f, -12f).forEach { refDb ->
                val y = dbToY(refDb)
                drawLine(
                    color = if (refDb == 0f) zeroLineColor else gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (refDb == 0f) 1.5.dp.toPx() else 0.8.dp.toPx(),
                )
            }

            // Logarithmic frequency verticals: 31, 125, 500, 2k, 8k
            val minLog = log10(20f)
            val maxLog = log10(20000f)
            listOf(31f, 125f, 500f, 2000f, 8000f).forEach { hz ->
                val xFrac = ((log10(hz) - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
                val x = xFrac * w
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 0.8.dp.toPx(),
                )
            }

            // Real-time FFT / Goertzel Spectrum Bars (drawn strictly when real audio data exists)
            if (hasLiveSpectrum) {
                val barCount = spectrumBands.size
                val slotW = w / barCount
                spectrumBands.forEachIndexed { i, mag ->
                    val barH = (mag.coerceIn(0f, 1f) * h * 0.72f)
                    val barX = i * slotW + slotW * 0.22f
                    drawRect(
                        color = accent.copy(alpha = 0.24f),
                        topLeft = Offset(barX, h - barH),
                        size = androidx.compose.ui.geometry.Size(slotW * 0.56f, barH),
                    )
                }
            }

            // Exact Frequency Response Curve Path
            val freqs = DspAudioProcessor.GRAPH_FREQUENCIES_HZ
            if (animatedCurve.size == freqs.size && freqs.size >= 2) {
                val strokePath = Path()
                val fillPath = Path()
                val zeroY = dbToY(0f)

                freqs.forEachIndexed { idx, hz ->
                    val x = (((log10(hz) - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)) * w
                    val y = dbToY(animatedCurve[idx])
                    if (idx == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, zeroY)
                        fillPath.lineTo(x, y)
                    } else {
                        strokePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    if (idx == freqs.lastIndex) {
                        fillPath.lineTo(x, zeroY)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(
                    path = strokePath,
                    color = accent,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("20Hz", "63Hz", "250Hz", "1kHz", "4kHz", "16kHz").forEach { lbl ->
                Text(lbl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (dspConfig.highPassEnabled || dspConfig.lowPassEnabled || telemetry.headroomReductionDb > 0.2f) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (dspConfig.highPassEnabled) {
                    StatusBadgePill(label = "HPF ${dspConfig.highPassCutoffHz.roundToInt()}Hz (${dspConfig.highPassSlope.dbPerOctave}dB)", isRedActive = true)
                }
                if (dspConfig.lowPassEnabled) {
                    StatusBadgePill(label = "LPF ${dspConfig.lowPassCutoffHz.roundToInt()}Hz (${dspConfig.lowPassSlope.dbPerOctave}dB)", isRedActive = true)
                }
                if (telemetry.headroomReductionDb > 0.2f) {
                    StatusBadgePill(label = "HEADROOM -%.1fdB".format(telemetry.headroomReductionDb))
                }
            }
        }
        if (!micPermissionGranted && !telemetry.softwarePcmActive) {
            Spacer(Modifier.height(8.dp))
            LuxActionButton(
                text = "Enable Live FFT Audio Visualizer",
                icon = Icons.Outlined.GraphicEq,
                filled = false,
                onClick = onRequestMicPermission,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ParametricAndHeadphoneDspCard(
    dspConfig: DspConfig,
    onUpdateParametricNode: (Int, (ParametricEqNode) -> ParametricEqNode) -> Unit,
    onUpdateDspConfig: ((DspConfig) -> DspConfig) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val haptic = LocalHapticFeedback.current
    var showParametric by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Headphone Sound Profile & Auto-Switching
        LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("headphone_profile_card")) {
            SectionHeader(
                title = "Headphone Sound Profile",
                subtitle = dspConfig.headphoneProfile.subtitle.uppercase(),
                trailing = { Icon(Icons.Outlined.Headphones, contentDescription = null, tint = extras.signalRed) },
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HeadphoneSoundProfile.entries) { prof ->
                    FilterChip(
                        selected = dspConfig.headphoneProfile == prof,
                        onClick = {
                            if (extras.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdateDspConfig { it.copy(headphoneProfile = prof) }
                        },
                        label = { Text(prof.title.uppercase(), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                            selectedLabelColor = extras.signalRed,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Automatic Profile Switching", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Auto-tunes headphone & EQ preset when a recognized Bluetooth headset connects",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = dspConfig.autoProfileSwitching,
                    onCheckedChange = { v -> onUpdateDspConfig { it.copy(autoProfileSwitching = v) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                    modifier = Modifier.testTag("auto_profile_switch_toggle"),
                )
            }
        }

        // Parametric EQ Card
        LuxGlassCard(
            modifier = Modifier.fillMaxWidth().testTag("parametric_eq_card"),
            highlightRedBorder = dspConfig.parametricNodes.any { it.enabled },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PARAMETRIC EQ NODES", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${dspConfig.parametricNodes.count { it.enabled }} / ${dspConfig.parametricNodes.size} ACTIVE BIQUAD FILTERS (FREQ · Q · GAIN)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { showParametric = !showParametric },
                    modifier = Modifier.testTag("expand_parametric_btn"),
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = extras.signalRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showParametric) "HIDE" else "EDIT", color = extras.signalRed)
                }
            }

            AnimatedVisibility(visible = showParametric) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dspConfig.parametricNodes.forEach { node ->
                        LuxGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            highlightRedBorder = node.enabled,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${node.label.uppercase()} · ${node.frequencyHz.roundToInt()}Hz · Q=${"%.1f".format(node.qFactor)}",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Switch(
                                    checked = node.enabled,
                                    onCheckedChange = { en -> onUpdateParametricNode(node.id) { it.copy(enabled = en) } },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                                    modifier = Modifier.testTag("peq_switch_${node.id}"),
                                )
                            }
                            Text(
                                "FREQUENCY (${node.frequencyHz.roundToInt()} Hz)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = log10(node.frequencyHz.coerceIn(20f, 20000f)),
                                onValueChange = { logF ->
                                    val hz = Math.pow(10.0, logF.toDouble()).toFloat().coerceIn(20f, 20000f)
                                    onUpdateParametricNode(node.id) { it.copy(frequencyHz = hz) }
                                },
                                valueRange = log10(20f)..log10(20000f),
                                enabled = node.enabled,
                                colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Q-FACTOR (${"%.2f".format(node.qFactor)})", style = MaterialTheme.typography.labelSmall)
                                    Slider(
                                        value = node.qFactor,
                                        onValueChange = { q -> onUpdateParametricNode(node.id) { it.copy(qFactor = q) } },
                                        valueRange = 0.3f..10f,
                                        enabled = node.enabled,
                                        colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("GAIN (%+.1f dB)".format(node.gainDb), style = MaterialTheme.typography.labelSmall)
                                    Slider(
                                        value = node.gainDb,
                                        onValueChange = { g -> onUpdateParametricNode(node.id) { it.copy(gainDb = g) } },
                                        valueRange = -12f..12f,
                                        enabled = node.enabled,
                                        colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersAndStereoMixSection(
    dspConfig: DspConfig,
    telemetry: DspLiveTelemetry,
    onUpdateDspConfig: ((DspConfig) -> DspConfig) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. High-Pass & Low-Pass Crossover Filters
        LuxGlassCard(
            modifier = Modifier.fillMaxWidth().testTag("filters_control_card"),
            highlightRedBorder = dspConfig.highPassEnabled || dspConfig.lowPassEnabled,
        ) {
            SectionHeader(
                title = "High-Pass & Low-Pass Filters",
                subtitle = "20Hz – 20,000Hz · 6 / 12 / 24 dB/OCT SLOPES",
                trailing = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = extras.signalRed) },
            )
            Spacer(Modifier.height(10.dp))

            // High-Pass
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("HIGH-PASS FILTER (${dspConfig.highPassCutoffHz.roundToInt()} Hz)", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = dspConfig.highPassEnabled,
                    onCheckedChange = { en -> onUpdateDspConfig { it.copy(highPassEnabled = en) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                    modifier = Modifier.testTag("hpf_enable_switch"),
                )
            }
            Slider(
                value = log10(dspConfig.highPassCutoffHz.coerceIn(20f, 20000f)),
                onValueChange = { logV ->
                    val hz = Math.pow(10.0, logV.toDouble()).toFloat().coerceIn(20f, 20000f)
                    onUpdateDspConfig { it.copy(highPassCutoffHz = hz) }
                },
                valueRange = log10(20f)..log10(20000f),
                enabled = dspConfig.highPassEnabled,
                colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                modifier = Modifier.testTag("hpf_cutoff_slider"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterSlope.entries.forEach { slope ->
                    FilterChip(
                        selected = dspConfig.highPassSlope == slope,
                        onClick = { onUpdateDspConfig { it.copy(highPassSlope = slope) } },
                        label = { Text(slope.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                            selectedLabelColor = extras.signalRed,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Low-Pass
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("LOW-PASS FILTER (${dspConfig.lowPassCutoffHz.roundToInt()} Hz)", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = dspConfig.lowPassEnabled,
                    onCheckedChange = { en -> onUpdateDspConfig { it.copy(lowPassEnabled = en) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                    modifier = Modifier.testTag("lpf_enable_switch"),
                )
            }
            Slider(
                value = log10(dspConfig.lowPassCutoffHz.coerceIn(20f, 20000f)),
                onValueChange = { logV ->
                    val hz = Math.pow(10.0, logV.toDouble()).toFloat().coerceIn(20f, 20000f)
                    onUpdateDspConfig { it.copy(lowPassCutoffHz = hz) }
                },
                valueRange = log10(20f)..log10(20000f),
                enabled = dspConfig.lowPassEnabled,
                colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                modifier = Modifier.testTag("lpf_cutoff_slider"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterSlope.entries.forEach { slope ->
                    FilterChip(
                        selected = dspConfig.lowPassSlope == slope,
                        onClick = { onUpdateDspConfig { it.copy(lowPassSlope = slope) } },
                        label = { Text(slope.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                            selectedLabelColor = extras.signalRed,
                        ),
                    )
                }
            }
        }

        // 2. Stereo Balance (L100% .. Center .. R100%), Channel Trim & Stereo Width
        LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("stereo_balance_card")) {
            SectionHeader(
                title = "Stereo Balance & Width",
                subtitle = "L100% – CENTER – R100% · M/S WIDTH STAGE",
                trailing = {
                    TextButton(
                        onClick = {
                            if (extras.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUpdateDspConfig {
                                it.copy(
                                    stereoBalance = 0f,
                                    leftChannelGainDb = 0f,
                                    rightChannelGainDb = 0f,
                                    stereoWidthPercent = 100,
                                )
                            }
                        },
                        modifier = Modifier.testTag("balance_center_reset_btn"),
                    ) {
                        Icon(Icons.Outlined.CenterFocusStrong, contentDescription = null, tint = extras.signalRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CENTER", color = extras.signalRed)
                    }
                },
            )
            Spacer(Modifier.height(8.dp))

            val balLabel = when {
                dspConfig.stereoBalance < -0.02f -> "L ${(-dspConfig.stereoBalance * 100).roundToInt()}%"
                dspConfig.stereoBalance > 0.02f -> "R ${(dspConfig.stereoBalance * 100).roundToInt()}%"
                else -> "CENTER (0%)"
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("L / R STEREO BALANCE", style = MaterialTheme.typography.labelMedium)
                Text(balLabel, style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
            }
            Slider(
                value = dspConfig.stereoBalance,
                onValueChange = { b -> onUpdateDspConfig { it.copy(stereoBalance = b) } },
                valueRange = -1f..1f,
                colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                modifier = Modifier.testTag("stereo_balance_slider"),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LEFT TRIM (%+.1f dB)".format(dspConfig.leftChannelGainDb), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = dspConfig.leftChannelGainDb,
                        onValueChange = { v -> onUpdateDspConfig { it.copy(leftChannelGainDb = v) } },
                        valueRange = -12f..6f,
                        colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("RIGHT TRIM (%+.1f dB)".format(dspConfig.rightChannelGainDb), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = dspConfig.rightChannelGainDb,
                        onValueChange = { v -> onUpdateDspConfig { it.copy(rightChannelGainDb = v) } },
                        valueRange = -12f..6f,
                        colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("STEREO WIDTH (M/S MATRIX)", style = MaterialTheme.typography.labelMedium)
                Text("${dspConfig.stereoWidthPercent}%", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
            }
            Slider(
                value = dspConfig.stereoWidthPercent.toFloat(),
                onValueChange = { w -> onUpdateDspConfig { it.copy(stereoWidthPercent = w.roundToInt()) } },
                valueRange = 0f..200f,
                colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                modifier = Modifier.testTag("stereo_width_slider"),
            )
        }

        // 3. Preamp, Gain Control, Limiter & Volume Normalization
        LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("gain_limiter_card")) {
            SectionHeader(
                title = "Preamp, Limiter & Gain",
                subtitle = if (telemetry.clippedSamplesPrevented > 0L) {
                    "PROTECTED ${telemetry.clippedSamplesPrevented} PEAK SAMPLES"
                } else {
                    "ANTI-CLIPPING SOFT-KNEE PROTECTION"
                },
                trailing = { Icon(Icons.Outlined.Shield, contentDescription = null, tint = extras.signalRed) },
            )
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PREAMP", style = MaterialTheme.typography.labelMedium)
                Text("%+.1f dB".format(dspConfig.preampDb), style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
            }
            Slider(
                value = dspConfig.preampDb,
                onValueChange = { v -> onUpdateDspConfig { it.copy(preampDb = v) } },
                valueRange = -12f..12f,
                colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                modifier = Modifier.testTag("preamp_slider"),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MASTER AUDIO GAIN", style = MaterialTheme.typography.labelMedium)
                Text("%+.1f dB".format(dspConfig.masterGainDb), style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
            }
            Slider(
                value = dspConfig.masterGainDb,
                onValueChange = { v -> onUpdateDspConfig { it.copy(masterGainDb = v) } },
                valueRange = -12f..12f,
                colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                modifier = Modifier.testTag("master_gain_slider"),
            )

            DspToggleRow("Lookahead Peak Limiter", dspConfig.limiterEnabled) { en ->
                onUpdateDspConfig { it.copy(limiterEnabled = en) }
            }
            DspToggleRow("Clipping Prevention & Auto-Headroom", dspConfig.clippingPreventionEnabled) { en ->
                onUpdateDspConfig { it.copy(clippingPreventionEnabled = en) }
            }
            DspToggleRow("Volume Normalization (-14 dBFS RMS)", dspConfig.volumeNormalizationEnabled) { en ->
                onUpdateDspConfig { it.copy(volumeNormalizationEnabled = en) }
            }
        }
    }
}

@Composable
private fun DspToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
        )
    }
}
