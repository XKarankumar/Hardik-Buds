package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AcousticTestType
import com.example.model.AncMode
import com.example.model.AudioEngineCapabilities
import com.example.model.DiscoveredAudioDevice
import com.example.model.DspConfig
import com.example.model.DspLiveTelemetry
import com.example.model.EqBandMode
import com.example.model.EqualizerProfile
import com.example.model.ParametricEqNode
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.model.TouchAction
import com.example.model.TouchControlConfig
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.MusicReactiveBassVisualizer
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import kotlin.math.roundToInt

@Composable
fun SoundScreen(
    activeDevice: DiscoveredAudioDevice?,
    dspConfig: DspConfig,
    dspTelemetry: DspLiveTelemetry,
    eqCurveDb: List<Float>,
    spectrumBands: List<Float>,
    subwoofer: SubwooferConfig,
    eqProfiles: List<EqualizerProfile>,
    activeEqProfile: EqualizerProfile,
    activeSoundMode: SoundMode,
    preferredAncMode: AncMode,
    touchConfig: TouchControlConfig,
    capabilities: AudioEngineCapabilities,
    smartSettings: SmartSettings,
    bassEnergy: Float,
    soundTestRunning: String?,
    micPermissionGranted: Boolean,
    initialTab: Int = 0,
    onRequestMicPermission: () -> Unit,
    onUpdateDspConfig: ((DspConfig) -> DspConfig) -> Unit,
    onResetAllDsp: () -> Unit,
    onUpdateSubwoofer: ((SubwooferConfig) -> SubwooferConfig) -> Unit,
    onResetSubwoofer: () -> Unit,
    onSelectEqPreset: (EqualizerProfile) -> Unit,
    onUpdateEqBand15: (Int, Float) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onUpdateParametricNode: (Int, (ParametricEqNode) -> ParametricEqNode) -> Unit,
    onSaveCustomEq: (String) -> Unit,
    onRenameCustomEq: (String, String) -> Unit,
    onDeleteCustomEq: (String) -> Unit,
    onExportEqJson: () -> String,
    onImportEqJson: (String) -> Boolean,
    onSelectSoundMode: (SoundMode) -> Unit,
    onSelectAncMode: (AncMode) -> Unit,
    onToggleGamingMode: (Boolean) -> Unit,
    onUpdateTouchConfig: ((TouchControlConfig) -> TouchControlConfig) -> Unit,
    onRunSoundTest: (AcousticTestType) -> Unit,
    onStopSoundTest: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = listOf("Equalizer", "Deep Bass", "Filters & Mix", "Hardware & Test")

    var showSaveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sound_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = "Sound Studio",
                subtitle = "REAL SOFTWARE PCM & ANDROID AUDIOEFFECT DSP",
                trailing = {
                    StatusBadgePill(
                        label = "${dspConfig.eqBandMode.bandCount}-BAND",
                        isRedActive = true,
                    )
                },
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testTag("sound_tabs_row")) {
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = {
                            if (extras.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = idx
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = extras.signalRed.copy(alpha = 0.16f),
                            activeContentColor = extras.signalRed,
                        ),
                        modifier = Modifier.testTag("sound_tab_$idx"),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        // Verified Real-Time Engine Status Banner (Always accurate: never fakes active processing)
        item {
            SoundEngineStatusCard(
                capabilities = capabilities,
                telemetry = dspTelemetry,
                soundTestRunning = soundTestRunning,
                onToggleAuditionLoop = {
                    if (soundTestRunning == AcousticTestType.LIVE_DSP_AUDITION.label) {
                        onStopSoundTest()
                    } else {
                        onRunSoundTest(AcousticTestType.LIVE_DSP_AUDITION)
                    }
                },
                onResetAllDsp = onResetAllDsp,
            )
        }

        when (selectedTab) {
            0 -> {
                // TAB 0: 15-BAND / 10-BAND EQUALIZER + ANIMATED CURVE + PRESETS + PARAMETRIC EQ + HEADPHONE PROFILE
                item {
                    AnimatedEqResponseGraphCard(
                        eqCurveDb = eqCurveDb,
                        spectrumBands = spectrumBands,
                        dspConfig = dspConfig,
                        telemetry = dspTelemetry,
                        micPermissionGranted = micPermissionGranted,
                        onRequestMicPermission = onRequestMicPermission,
                    )
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("eq_presets_card")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${dspConfig.eqBandMode.label.uppercase()} EQUALIZER",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "PRESET: ${activeEqProfile.name.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extras.signalRed,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EqBandMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = dspConfig.eqBandMode == mode,
                                        onClick = { onUpdateDspConfig { it.copy(eqBandMode = mode) } },
                                        label = { Text(mode.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                            selectedLabelColor = extras.signalRed,
                                        ),
                                        modifier = Modifier.testTag("eq_mode_${mode.name}"),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(eqProfiles, key = { it.id }) { prof ->
                                FilterChip(
                                    selected = prof.id == activeEqProfile.id,
                                    onClick = {
                                        if (extras.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectEqPreset(prof)
                                    },
                                    label = { Text(prof.name.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                        selectedLabelColor = extras.signalRed,
                                    ),
                                    modifier = Modifier.testTag("eq_preset_${prof.id}"),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LuxActionButton(
                                text = "Save",
                                icon = Icons.Outlined.Add,
                                filled = false,
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.weight(1f).testTag("eq_save_button"),
                            )
                            LuxActionButton(
                                text = "Export",
                                icon = Icons.Outlined.Upload,
                                filled = false,
                                onClick = { showExportDialog = onExportEqJson() },
                                modifier = Modifier.weight(1f).testTag("eq_export_button"),
                            )
                            LuxActionButton(
                                text = "Import",
                                icon = Icons.Outlined.Download,
                                filled = false,
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f).testTag("eq_import_button"),
                            )
                        }
                        if (!activeEqProfile.isBuiltIn) {
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showRenameDialog = true }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("RENAME")
                                }
                                TextButton(onClick = { onDeleteCustomEq(activeEqProfile.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("DELETE", color = extras.signalRed)
                                }
                            }
                        }
                    }
                }

                // 15-Band or 10-Band Frequency Sliders (-12dB to +12dB)
                item {
                    val bandsToShow = if (dspConfig.eqBandMode == EqBandMode.BANDS_15) {
                        DspConfig.BANDS_15_SPECS
                    } else {
                        DspConfig.BANDS_15_SPECS.filter { it.mapped10BandIndex != null }
                    }
                    val cur15 = activeEqProfile.resolved15Bands()

                    LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("eq_sliders_card")) {
                        SectionHeader(
                            title = if (dspConfig.eqBandMode == EqBandMode.BANDS_15) {
                                "15-Band Precision EQ (20Hz – 16kHz)"
                            } else {
                                "10-Band EQ (31Hz – 16kHz)"
                            },
                            subtitle = "-12 dB TO +12 dB · DIRECT BIQUAD FILTER CONTROL",
                            trailing = {
                                TextButton(
                                    onClick = {
                                        val flat = eqProfiles.firstOrNull { it.id == "preset_balanced" }
                                        if (flat != null) onSelectEqPreset(flat) else onResetAllDsp()
                                    },
                                    modifier = Modifier.testTag("eq_flat_reset_btn"),
                                ) {
                                    Text("FLAT 0dB", color = extras.signalRed)
                                }
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        bandsToShow.forEach { spec ->
                            val db = cur15.getOrElse(spec.index) { 0f }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.width(86.dp)) {
                                    Text(
                                        text = spec.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (db != 0f) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = spec.roleLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Slider(
                                    value = db,
                                    onValueChange = { newVal ->
                                        onUpdateEqBand15(spec.index, newVal)
                                    },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = extras.signalRed,
                                        activeTrackColor = extras.signalRed,
                                    ),
                                    modifier = Modifier.weight(1f).testTag("eq_slider_${spec.shortLabel}"),
                                )
                                Text(
                                    text = "%+.1f dB".format(db),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (db != 0f) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(56.dp),
                                )
                            }
                        }
                    }
                }

                // Parametric EQ + Headphone Profile + Auto Switching
                item {
                    ParametricAndHeadphoneDspCard(
                        dspConfig = dspConfig,
                        onUpdateParametricNode = onUpdateParametricNode,
                        onUpdateDspConfig = onUpdateDspConfig,
                    )
                }
            }

            1 -> {
                // TAB 1: REAL DEEP BASS CONTROL (5 OPTIONS + ANTI-CLIPPING + CROSSOVER + SUB SHAPING)
                item {
                    LuxGlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("deep_bass_hero_card"),
                        highlightRedBorder = dspConfig.bassEngineEnabled,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DEEP BASS & SUBWOOFER DSP",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = if (dspConfig.bassEngineEnabled) {
                                        "BIQUAD SUB-SHELF + PUNCH PEAK + HARMONIC TEXTURE"
                                    } else {
                                        "BASS ENHANCEMENT BYPASSED"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dspConfig.bassEngineEnabled) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = dspConfig.bassEngineEnabled,
                                onCheckedChange = { en ->
                                    onUpdateDspConfig { it.copy(bassEngineEnabled = en) }
                                    onUpdateSubwoofer { it.copy(enabled = en) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                                modifier = Modifier.testTag("subwoofer_master_switch"),
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        MusicReactiveBassVisualizer(
                            energy = bassEnergy,
                            bassIntensity = dspConfig.deepBassPercent,
                            enabled = dspConfig.bassEngineEnabled,
                            useRedFrequencyBars = true,
                        )
                    }
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("deep_bass_sliders_card")) {
                        SectionHeader(
                            title = "Deep Bass Processing (0–100%)",
                            subtitle = "HEADROOM GUARD & ANTI-CLIPPING ACTIVE",
                            trailing = {
                                TextButton(onClick = onResetSubwoofer) {
                                    Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset Deep Bass", tint = extras.signalRed, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("RESET", color = extras.signalRed)
                                }
                            },
                        )
                        Spacer(Modifier.height(10.dp))

                        BassPercentSlider("Deep Bass (45–80Hz Shelf)", dspConfig.deepBassPercent, dspConfig.bassEngineEnabled, "slider_deep_bass") { v ->
                            onUpdateDspConfig { it.copy(deepBassPercent = v) }
                            onUpdateSubwoofer { it.copy(deepBass = v, bassIntensity = v, presetName = "Custom") }
                        }
                        BassPercentSlider("Sub Bass (20–55Hz Rumble)", dspConfig.subBassPercent, dspConfig.bassEngineEnabled, "slider_sub_bass") { v ->
                            onUpdateDspConfig { it.copy(subBassPercent = v) }
                            onUpdateSubwoofer { it.copy(subwooferSimulation = v, presetName = "Custom") }
                        }
                        BassPercentSlider("Bass Punch (80–130Hz Kick)", dspConfig.bassPunchPercent, dspConfig.bassEngineEnabled, "slider_bass_punch") { v ->
                            onUpdateDspConfig { it.copy(bassPunchPercent = v) }
                            onUpdateSubwoofer { it.copy(punchBass = v, presetName = "Custom") }
                        }
                        BassPercentSlider("Bass Warmth (150–250Hz Body)", dspConfig.bassWarmthPercent, dspConfig.bassEngineEnabled, "slider_bass_warmth") { v ->
                            onUpdateDspConfig { it.copy(bassWarmthPercent = v) }
                        }
                        BassPercentSlider("Bass Texture (Soft-Knee Harmonics)", dspConfig.bassTexturePercent, dspConfig.bassEngineEnabled, "slider_bass_texture") { v ->
                            onUpdateDspConfig { it.copy(bassTexturePercent = v) }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("LOW-FREQ CROSSOVER TARGET", style = MaterialTheme.typography.labelMedium)
                            Text("${dspConfig.subCrossoverHz.roundToInt()} Hz", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                        }
                        Slider(
                            value = dspConfig.subCrossoverHz,
                            onValueChange = { hz ->
                                onUpdateDspConfig { it.copy(subCrossoverHz = hz) }
                                onUpdateSubwoofer { it.copy(lowFreqCutoffHz = hz.roundToInt(), presetName = "Custom") }
                            },
                            valueRange = 20f..200f,
                            enabled = dspConfig.bassEngineEnabled,
                            colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                        )

                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = extras.signalRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Clip-safe DSP automatically compensates ${"%.1f".format(dspTelemetry.headroomReductionDb)} dB headroom to prevent bass distortion & volume spikes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            2 -> {
                // TAB 2: FILTERS (HPF/LPF 20-20000Hz 6/12/24 dB/oct), STEREO BALANCE & WIDTH, PREAMP, GAIN, LIMITER, NORMALIZATION, SOUND MODES
                item {
                    FiltersAndStereoMixSection(
                        dspConfig = dspConfig,
                        telemetry = dspTelemetry,
                        onUpdateDspConfig = onUpdateDspConfig,
                    )
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Sound Modes & Spatial Stage",
                            subtitle = if (capabilities.virtualizerSupported) {
                                "MID/SIDE MATRIX + ANDROID VIRTUALIZER"
                            } else {
                                "SOFTWARE MID/SIDE SPATIAL + BIQUAD STAGE"
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        SoundMode.entries.forEach { mode ->
                            val selected = activeSoundMode == mode
                            LuxGlassCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                contentPadding = PaddingValues(12.dp),
                                highlightRedBorder = selected,
                                onClick = { onSelectSoundMode(mode) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(mode.title.uppercase(), style = MaterialTheme.typography.titleSmall)
                                            if (mode.isSpatial) StatusBadgePill(label = "3D SPATIAL", isRedActive = selected)
                                            if (mode.isLowLatency) StatusBadgePill(label = "LOW LATENCY", isRedActive = selected)
                                        }
                                        Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (selected) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = extras.signalRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // TAB 3: ACOUSTIC TESTS + GAMING LOW-LATENCY + HARDWARE ANC PROTOCOL + TOUCH MAP
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Real-Time DSP Acoustic Tests",
                            subtitle = soundTestRunning?.let { "STREAMING THROUGH DSP: $it" } ?: "LIVE PCM AUDIO ROUTED THROUGH ALL ACTIVE FILTERS",
                            trailing = { Icon(Icons.Outlined.Speaker, contentDescription = null, tint = extras.signalRed) },
                        )
                        Spacer(Modifier.height(10.dp))
                        listOf(
                            AcousticTestType.LIVE_DSP_AUDITION,
                            AcousticTestType.LEFT_CHANNEL,
                            AcousticTestType.RIGHT_CHANNEL,
                            AcousticTestType.STEREO_CHIME,
                            AcousticTestType.SUBWOOFER_SWEEP,
                        ).forEach { test ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(test.label.uppercase(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                LuxActionButton(
                                    text = if (soundTestRunning == test.label) "Playing" else "Play",
                                    icon = Icons.Outlined.PlayArrow,
                                    filled = false,
                                    useRedAccent = soundTestRunning == test.label,
                                    enabled = soundTestRunning == null,
                                    onClick = { onRunSoundTest(test) },
                                )
                            }
                        }
                        if (soundTestRunning != null) {
                            Spacer(Modifier.height(8.dp))
                            LuxActionButton(
                                text = "Stop Sound Test",
                                icon = Icons.Outlined.Stop,
                                useRedAccent = true,
                                onClick = onStopSoundTest,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item {
                    val gamingActive = smartSettings.gamingLowLatencyHint || activeSoundMode == SoundMode.GAMING
                    val pulseAnim = if (gamingActive && extras.animationSpeed.factor > 0f) {
                        val inf = rememberInfiniteTransition(label = "gamingPulse")
                        val v by inf.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(950, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "pulse",
                        )
                        v
                    } else 0.35f

                    LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = gamingActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(
                                            color = if (gamingActive) extras.signalRed.copy(alpha = pulseAnim) else Color.Gray.copy(alpha = 0.35f),
                                            radius = size.minDimension * 0.44f,
                                            style = Stroke(width = 1.8.dp.toPx()),
                                        )
                                    }
                                    Icon(
                                        Icons.Outlined.SportsEsports,
                                        contentDescription = null,
                                        tint = if (gamingActive) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("GAMING MODE", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = if (gamingActive) "LOW-LATENCY DSP (~${dspTelemetry.latencyEstimateMs.roundToInt()}ms)" else "STANDARD AUDIO BUFFERING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (gamingActive) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = gamingActive,
                                onCheckedChange = onToggleGamingMode,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                                modifier = Modifier.testTag("gaming_mode_switch"),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Output Route: ${capabilities.outputRouteLabel} (${capabilities.outputSampleRateHz} Hz). Gaming Mode engages 2–6kHz transient clarity biquads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Hardware ANC Control (Honest non-faked hardware gate)
                item {
                    val hwAncSupported = activeDevice?.compatibilityReport?.supportsHardwareAncControl == true
                    LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = hwAncSupported) {
                        SectionHeader(
                            title = "Earbud Hardware ANC",
                            subtitle = if (hwAncSupported) "HARDWARE ANC ACTIVE" else "REQUIRES EARBUD MICROPHONE HARDWARE",
                            trailing = {
                                Icon(
                                    imageVector = if (hwAncSupported) Icons.Outlined.GraphicEq else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (hwAncSupported) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Active Noise Cancellation & Transparency run inside earbud microphone hardware chips and are never faked in software when Bluetooth GATT ANC APIs are absent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        AncMode.entries.take(6).forEach { mode ->
                            val selected = preferredAncMode == mode
                            LuxGlassCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                contentPadding = PaddingValues(12.dp),
                                highlightRedBorder = selected,
                                onClick = { onSelectAncMode(mode) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mode.title.uppercase(), style = MaterialTheme.typography.titleSmall)
                                        Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusBadgePill(
                                        label = if (hwAncSupported && selected) "ACTIVE" else if (selected) "SAVED" else "HW ONLY",
                                        isRedActive = selected,
                                    )
                                }
                            }
                        }
                    }
                }

                // Touch Controls
                item {
                    val touchSupported = activeDevice?.compatibilityReport?.supportsHardwareTouchRemap == true
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Touch Preferences",
                            subtitle = if (touchSupported) "SYNCED WITH EARBUDS" else "SAVED LOCALLY PER PROFILE",
                            trailing = { Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("LEFT EARBUD", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                        TouchRow("Single Tap", touchConfig.leftSingle) { next -> onUpdateTouchConfig { it.copy(leftSingle = next) } }
                        TouchRow("Double Tap", touchConfig.leftDouble) { next -> onUpdateTouchConfig { it.copy(leftDouble = next) } }
                        TouchRow("Long Press", touchConfig.leftLong) { next -> onUpdateTouchConfig { it.copy(leftLong = next) } }
                        Spacer(Modifier.height(8.dp))
                        Text("RIGHT EARBUD", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                        TouchRow("Single Tap", touchConfig.rightSingle) { next -> onUpdateTouchConfig { it.copy(rightSingle = next) } }
                        TouchRow("Double Tap", touchConfig.rightDouble) { next -> onUpdateTouchConfig { it.copy(rightDouble = next) } }
                        TouchRow("Long Press", touchConfig.rightLong) { next -> onUpdateTouchConfig { it.copy(rightLong = next) } }
                    }
                }
            }
        }
    }

    // Dialogs (Save, Rename, Export, Import)
    if (showSaveDialog) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("SAVE EQ PROFILE") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveCustomEq(nameInput)
                    showSaveDialog = false
                }) { Text("SAVE") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("CANCEL") } },
        )
    }

    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(activeEqProfile.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("RENAME EQ PROFILE") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("New Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameCustomEq(activeEqProfile.id, nameInput)
                    showRenameDialog = false
                }) { Text("RENAME") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("CANCEL") } },
        )
    }

    showExportDialog?.let { json ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text("EXPORT EQ JSON") },
            text = { Text(json, style = MaterialTheme.typography.labelSmall) },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(json))
                    onShowToast("15-Band EQ profile JSON copied to clipboard")
                    showExportDialog = null
                }) { Text("COPY JSON") }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = null }) { Text("CLOSE") } },
        )
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("IMPORT EQ JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste EQ JSON") },
                    minLines = 4,
                    maxLines = 8,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onImportEqJson(jsonInput)) showImportDialog = false
                }) { Text("IMPORT") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun BassPercentSlider(
    title: String,
    value: Int,
    enabled: Boolean,
    tag: String,
    onValueChange: (Int) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium)
            Text("$value%", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun TouchRow(
    gesture: String,
    currentAction: TouchAction,
    onSelect: (TouchAction) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(gesture, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(92.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(TouchAction.entries) { action ->
                FilterChip(
                    selected = currentAction == action,
                    onClick = { onSelect(action) },
                    label = { Text(action.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                        selectedLabelColor = extras.signalRed,
                    ),
                )
            }
        }
    }
}
