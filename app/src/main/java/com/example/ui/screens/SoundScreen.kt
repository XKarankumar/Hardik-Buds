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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AcousticTestType
import com.example.model.AncMode
import com.example.model.AudioEngineCapabilities
import com.example.model.DiscoveredAudioDevice
import com.example.model.EqualizerProfile
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.model.TouchAction
import com.example.model.TouchControlConfig
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.MusicReactiveBassVisualizer
import com.example.ui.components.RedSignalDot
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import kotlin.math.roundToInt

@Composable
fun SoundScreen(
    activeDevice: DiscoveredAudioDevice?,
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
    onUpdateSubwoofer: ((SubwooferConfig) -> SubwooferConfig) -> Unit,
    onResetSubwoofer: () -> Unit,
    onSelectEqPreset: (EqualizerProfile) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
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
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = listOf("Equalizer", "Subwoofer X", "ANC Studio", "Gaming & Test")

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
                subtitle = capabilities.engineStatusMessage,
                trailing = {
                    StatusBadgePill(
                        label = if (capabilities.equalizerSupported) "${capabilities.hardwareBandCount}-BAND DSP" else "SAFE DSP",
                        isRedActive = capabilities.equalizerSupported,
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = extras.signalRed.copy(alpha = 0.16f),
                            activeContentColor = extras.signalRed,
                        ),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // SOUND STUDIO: 10-BAND EQUALIZER + SPATIAL / SURROUND + VISUALIZER
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Music Reactive Telemetry",
                            subtitle = if (micPermissionGranted) "LIVE FFT ANALYZER ACTIVE" else "PLAYBACK REACTIVE PULSE",
                            trailing = { RedSignalDot(active = subwoofer.enabled, size = 8.dp) },
                        )
                        Spacer(Modifier.height(10.dp))
                        MusicReactiveBassVisualizer(
                            energy = bassEnergy,
                            bassIntensity = subwoofer.bassIntensity,
                            enabled = true,
                            useRedFrequencyBars = true,
                        )
                        if (!micPermissionGranted) {
                            Spacer(Modifier.height(8.dp))
                            LuxActionButton(
                                text = "Enable Live FFT Mic Visualizer",
                                icon = Icons.Outlined.Mic,
                                filled = false,
                                onClick = onRequestMicPermission,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "10-Band Equalizer",
                            subtitle = "PROFILE: ${activeEqProfile.name.uppercase()}",
                            trailing = { Icon(Icons.Outlined.Equalizer, contentDescription = null, tint = extras.signalRed) },
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(eqProfiles, key = { it.id }) { prof ->
                                FilterChip(
                                    selected = prof.id == activeEqProfile.id,
                                    onClick = { onSelectEqPreset(prof) },
                                    label = { Text(prof.name.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                        selectedLabelColor = extras.signalRed,
                                    ),
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

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Frequency Sliders (31Hz – 16kHz)",
                            subtitle = "MAPPED TO ${capabilities.hardwareBandCount.coerceAtLeast(5)} ANDROID DSP BANDS",
                        )
                        Spacer(Modifier.height(10.dp))
                        EqualizerProfile.BAND_LABELS.forEachIndexed { idx, freqLabel ->
                            val db = activeEqProfile.bandsDb.getOrElse(idx) { 0f }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(freqLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(54.dp))
                                Slider(
                                    value = db,
                                    onValueChange = { newVal -> onUpdateEqBand(idx, newVal) },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = extras.signalRed,
                                        activeTrackColor = extras.signalRed,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "%+.1f".format(db),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (db != 0f) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(46.dp),
                                )
                            }
                        }
                    }
                }

                // Sound Modes + Spatial / Surround Sound
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Sound Modes & Spatial Stage",
                            subtitle = if (capabilities.virtualizerSupported) {
                                "ANDROID VIRTUALIZER 3D ENGINE READY"
                            } else {
                                "EQ STAGE SHAPING (VIRTUALIZER HAL UNSUPPORTED)"
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
                                            if (mode.isSpatial) StatusBadgePill(label = if (capabilities.virtualizerSupported) "3D SPATIAL" else "EQ STAGE", isRedActive = selected)
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

            1 -> {
                // SUBWOOFER ENGINE X (PURE BLACK + SIGNAL RED FREQUENCY VISUALIZATION)
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = subwoofer.enabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SUBWOOFER ENGINE X",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = if (capabilities.bassBoostSupported) {
                                        "ACTIVE: BASSBOOST + EQUALIZER + LOUDNESS"
                                    } else {
                                        "ACTIVE: EQUALIZER LOW-FREQ DSP"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (subwoofer.enabled) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = subwoofer.enabled,
                                onCheckedChange = { checked -> onUpdateSubwoofer { it.copy(enabled = checked) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                                modifier = Modifier.testTag("subwoofer_master_switch"),
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        MusicReactiveBassVisualizer(
                            energy = bassEnergy,
                            bassIntensity = subwoofer.bassIntensity,
                            enabled = subwoofer.enabled,
                            useRedFrequencyBars = true,
                        )
                    }
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Subwoofer Profiles",
                            subtitle = "ACTIVE: ${subwoofer.presetName.uppercase()}",
                            trailing = {
                                TextButton(onClick = onResetSubwoofer) {
                                    Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset Subwoofer", tint = extras.signalRed, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("RESET", color = extras.signalRed)
                                }
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        val subPresets = listOf(
                            SubwooferConfig(true, 55, 60, 50, 65, 80, 0f, "Titanium Sub", 4.5f, 5f, 4f, 3f, 2f, 1f, 0f),
                            SubwooferConfig(true, 88, 92, 82, 90, 95, 0f, "808 Seismic", 9f, 9.5f, 8f, 6f, 4f, 2f, 0.5f),
                            SubwooferConfig(true, 76, 70, 88, 65, 110, 0f, "Club Punch", 5.5f, 6.5f, 7.5f, 7f, 5.5f, 3f, 1.5f),
                            SubwooferConfig(true, 35, 30, 35, 30, 63, 0f, "Studio Warm", 2f, 2.5f, 2f, 1.5f, 1f, 0.5f, 0f),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subPresets) { p ->
                                FilterChip(
                                    selected = subwoofer.presetName == p.presetName,
                                    onClick = { onUpdateSubwoofer { p } },
                                    label = { Text(p.presetName.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                        selectedLabelColor = extras.signalRed,
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Low-Frequency Dynamics", subtitle = "0–100 REAL ANDROID AUDIOEFFECT SCALING")
                        Spacer(Modifier.height(10.dp))
                        LabeledIntSlider("Bass Level", subwoofer.bassIntensity, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(bassIntensity = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Deep Bass", subwoofer.deepBass, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(deepBass = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Punch Bass", subwoofer.punchBass, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(punchBass = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Subwoofer Simulation", subwoofer.subwooferSimulation, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(subwooferSimulation = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Low-Frequency Cutoff", subwoofer.lowFreqCutoffHz, 20..200, " Hz", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(lowFreqCutoffHz = v, presetName = "Custom") }
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("L/R BALANCE", style = MaterialTheme.typography.labelSmall)
                            val balText = when {
                                subwoofer.balance < -0.05f -> "L ${(-subwoofer.balance * 100).roundToInt()}%"
                                subwoofer.balance > 0.05f -> "R ${(subwoofer.balance * 100).roundToInt()}%"
                                else -> "CENTER"
                            }
                            Text(balText, style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                        }
                        Slider(
                            value = subwoofer.balance,
                            onValueChange = { v -> onUpdateSubwoofer { it.copy(balance = v) } },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                        )
                    }
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Sub-Bass Band Tuning", subtitle = "20Hz · 31Hz · 40Hz · 63Hz · 80Hz · 125Hz · 200Hz")
                        Spacer(Modifier.height(8.dp))
                        SubBandSlider("20Hz INFRASONIC", subwoofer.band20HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band20HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("31Hz SUB RUMBLE", subwoofer.band31HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band31HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("40Hz SUB CORE", subwoofer.band40HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band40HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("63Hz DEEP KICK", subwoofer.band63HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band63HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("80Hz CHEST PUNCH", subwoofer.band80HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band80HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("125Hz UPPER BASS", subwoofer.band125HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band125HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("200Hz WARMTH", subwoofer.band200HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band200HzDb = v, presetName = "Custom") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = extras.signalRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = capabilities.limitationNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            2 -> {
                // ANC STUDIO
                item {
                    val hwAncSupported = activeDevice?.compatibilityReport?.supportsHardwareAncControl == true
                    LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = hwAncSupported) {
                        SectionHeader(
                            title = "ANC Studio",
                            subtitle = if (hwAncSupported) "HARDWARE ANC PROTOCOL LINKED" else "HARDWARE PROTOCOL GATE ACTIVE",
                            trailing = {
                                Icon(
                                    imageVector = if (hwAncSupported) Icons.Outlined.GraphicEq else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (hwAncSupported) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (activeDevice == null) {
                                "No Bluetooth earbuds connected. Select a staged preference below; hardware support will be verified upon connection."
                            } else {
                                "${activeDevice.displayTitle} (${activeDevice.brand.displayName}) does not expose public Android ANC GATT/RFCOMM APIs. Unsupported controls are clearly labeled and never faked."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        AncMode.entries.forEach { mode ->
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
                                        label = if (hwAncSupported && selected) "ACTIVE" else if (selected) "STAGED" else "OEM REQ",
                                        isRedActive = selected,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // GAMING MODE + TOUCH CONTROLS + ACOUSTIC TEST
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
                                        text = if (gamingActive) "LOW-LATENCY AUDIO PROFILE ACTIVE" else "STANDARD AUDIO BUFFERING",
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

                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadgePill(label = "${capabilities.outputSampleRateHz} Hz", isRedActive = gamingActive)
                            StatusBadgePill(label = "SESSION ${capabilities.activeAudioSessionId}", isRedActive = false)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Output Route: ${capabilities.outputRouteLabel}. Gaming Mode activates low-latency EQ transient boost (2–6kHz footstep clarity) and requests minimal AudioManager routing latency. Bluetooth codec-level LL mode depends on earbuds & OS Bluetooth stack.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Touch Controls
                item {
                    val touchSupported = activeDevice?.compatibilityReport?.supportsHardwareTouchRemap == true
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Touch Controls",
                            subtitle = if (touchSupported) "SYNCED WITH HARDWARE" else "LOCAL PREFERENCE · OEM PROTOCOL REQUIRED",
                            trailing = { Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("LEFT EARBUD", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                        Spacer(Modifier.height(4.dp))
                        TouchRow("Single Tap", touchConfig.leftSingle) { next -> onUpdateTouchConfig { it.copy(leftSingle = next) } }
                        TouchRow("Double Tap", touchConfig.leftDouble) { next -> onUpdateTouchConfig { it.copy(leftDouble = next) } }
                        TouchRow("Triple Tap", touchConfig.leftTriple) { next -> onUpdateTouchConfig { it.copy(leftTriple = next) } }
                        TouchRow("Long Press", touchConfig.leftLong) { next -> onUpdateTouchConfig { it.copy(leftLong = next) } }
                        Spacer(Modifier.height(10.dp))
                        Text("RIGHT EARBUD", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                        Spacer(Modifier.height(4.dp))
                        TouchRow("Single Tap", touchConfig.rightSingle) { next -> onUpdateTouchConfig { it.copy(rightSingle = next) } }
                        TouchRow("Double Tap", touchConfig.rightDouble) { next -> onUpdateTouchConfig { it.copy(rightDouble = next) } }
                        TouchRow("Triple Tap", touchConfig.rightTriple) { next -> onUpdateTouchConfig { it.copy(rightTriple = next) } }
                        TouchRow("Long Press", touchConfig.rightLong) { next -> onUpdateTouchConfig { it.copy(rightLong = next) } }
                    }
                }

                // Acoustic Sound Test
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Acoustic Sound Test",
                            subtitle = soundTestRunning?.let { "PLAYING: $it" } ?: "REAL PCM STEREO & SUB-BASS TONES",
                            trailing = { Icon(Icons.Outlined.Speaker, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                        )
                        Spacer(Modifier.height(10.dp))
                        listOf(
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
            }
        }
    }

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
                    onShowToast("EQ profile JSON copied to clipboard")
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
private fun LabeledIntSlider(
    title: String,
    value: Int,
    range: IntRange,
    unit: String,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium)
            Text("$value$unit", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
        )
    }
}

@Composable
private fun SubBandSlider(
    label: String,
    db: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(122.dp))
        Slider(
            value = db,
            onValueChange = onChange,
            valueRange = -6f..12f,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%+.1fdB".format(db),
            style = MaterialTheme.typography.labelSmall,
            color = extras.signalRed,
            modifier = Modifier.width(56.dp),
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
