package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AcousticTestType
import com.example.model.AncMode
import com.example.model.AudioEngineCapabilities
import com.example.model.DiscoveredAudioDevice
import com.example.model.EqualizerProfile
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
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning
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
    bassEnergy: Float,
    soundTestRunning: String?,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    onUpdateSubwoofer: ((SubwooferConfig) -> SubwooferConfig) -> Unit,
    onSelectEqPreset: (EqualizerProfile) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onSaveCustomEq: (String) -> Unit,
    onRenameCustomEq: (String, String) -> Unit,
    onDeleteCustomEq: (String) -> Unit,
    onExportEqJson: () -> String,
    onImportEqJson: (String) -> Boolean,
    onSelectSoundMode: (SoundMode) -> Unit,
    onSelectAncMode: (AncMode) -> Unit,
    onUpdateTouchConfig: ((TouchControlConfig) -> TouchControlConfig) -> Unit,
    onRunSoundTest: (AcousticTestType) -> Unit,
    onStopSoundTest: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Subwoofer X", "Pro EQ", "Modes & ANC", "Touch & Test")

    var showSaveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sound_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "Acoustic & DSP Studio",
                subtitle = capabilities.engineStatusMessage,
                trailing = {
                    StatusBadgePill(
                        label = if (capabilities.equalizerSupported) "${capabilities.hardwareBandCount}-Band DSP" else "DSP Safe Mode",
                        color = if (capabilities.equalizerSupported) StatusConnected else StatusWarning,
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
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // SUBWOOFER ENGINE X
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_subwoofer_banner),
                                contentDescription = stringResource(R.string.cd_subwoofer_art),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SUBWOOFER ENGINE X",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Low-frequency psychoacoustic sub-bass shaping",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = subwoofer.enabled,
                                onCheckedChange = { checked -> onUpdateSubwoofer { it.copy(enabled = checked) } },
                                modifier = Modifier.testTag("subwoofer_master_switch"),
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        MusicReactiveBassVisualizer(
                            energy = bassEnergy,
                            bassIntensity = subwoofer.bassIntensity,
                            enabled = subwoofer.enabled,
                        )

                        if (!micPermissionGranted) {
                            Spacer(Modifier.height(8.dp))
                            LuxActionButton(
                                text = "Enable Live FFT Audio Visualizer",
                                icon = Icons.Rounded.Mic,
                                filled = false,
                                onClick = onRequestMicPermission,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // Bass Presets
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Subwoofer Presets", subtitle = "Active: ${subwoofer.presetName}")
                        Spacer(Modifier.height(10.dp))
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
                                    label = { Text(p.presetName) },
                                )
                            }
                        }
                    }
                }

                // Core Subwoofer Controls
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Subwoofer Dynamics", subtitle = "Real BassBoost + Equalizer + LoudnessEnhancer")
                        Spacer(Modifier.height(12.dp))
                        LabeledIntSlider("Bass Intensity", subwoofer.bassIntensity, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(bassIntensity = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Subwoofer Simulation", subwoofer.subwooferSimulation, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(subwooferSimulation = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Punch Bass", subwoofer.punchBass, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(punchBass = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Deep Bass", subwoofer.deepBass, 0..100, "%", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(deepBass = v, presetName = "Custom") }
                        }
                        LabeledIntSlider("Low-Pass Cutoff", subwoofer.lowFreqCutoffHz, 20..200, " Hz", subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(lowFreqCutoffHz = v, presetName = "Custom") }
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("L/R Audio Balance", style = MaterialTheme.typography.labelMedium)
                            val balText = when {
                                subwoofer.balance < -0.05f -> "L ${(-subwoofer.balance * 100).roundToInt()}%"
                                subwoofer.balance > 0.05f -> "R ${(subwoofer.balance * 100).roundToInt()}%"
                                else -> "Center"
                            }
                            Text(balText, style = MaterialTheme.typography.labelMedium, color = extras.accent)
                        }
                        Slider(
                            value = subwoofer.balance,
                            onValueChange = { v -> onUpdateSubwoofer { it.copy(balance = v) } },
                            valueRange = -1f..1f,
                        )
                    }
                }

                // 7 Sub-Bass Frequency Bands (20Hz .. 200Hz)
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Sub-Frequency Bands",
                            subtitle = "20Hz · 31Hz · 40Hz · 63Hz · 80Hz · 125Hz · 200Hz",
                        )
                        Spacer(Modifier.height(10.dp))
                        SubBandSlider("20Hz (Infrasonic)", subwoofer.band20HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band20HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("31Hz (Sub Rumble)", subwoofer.band31HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band31HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("40Hz (Subwoofer Core)", subwoofer.band40HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band40HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("63Hz (Deep Kick)", subwoofer.band63HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band63HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("80Hz (Chest Punch)", subwoofer.band80HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band80HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("125Hz (Upper Bass)", subwoofer.band125HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band125HzDb = v, presetName = "Custom") }
                        }
                        SubBandSlider("200Hz (Warmth)", subwoofer.band200HzDb, subwoofer.enabled) { v ->
                            onUpdateSubwoofer { it.copy(band200HzDb = v, presetName = "Custom") }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(16.dp))
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

            1 -> {
                // PRO 10-BAND EQUALIZER
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Pro 10-Band Equalizer",
                            subtitle = "Active Profile: ${activeEqProfile.name}",
                            trailing = {
                                Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = extras.accent)
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(eqProfiles, key = { it.id }) { prof ->
                                FilterChip(
                                    selected = prof.id == activeEqProfile.id,
                                    onClick = { onSelectEqPreset(prof) },
                                    label = { Text(prof.name) },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LuxActionButton(
                                text = "Save As",
                                icon = Icons.Rounded.Add,
                                filled = false,
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.weight(1f).testTag("eq_save_button"),
                            )
                            LuxActionButton(
                                text = "Export",
                                icon = Icons.Rounded.Upload,
                                filled = false,
                                onClick = { showExportDialog = onExportEqJson() },
                                modifier = Modifier.weight(1f).testTag("eq_export_button"),
                            )
                            LuxActionButton(
                                text = "Import",
                                icon = Icons.Rounded.Download,
                                filled = false,
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f).testTag("eq_import_button"),
                            )
                        }

                        if (!activeEqProfile.isBuiltIn) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { showRenameDialog = true }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Rename")
                                }
                                TextButton(onClick = { onDeleteCustomEq(activeEqProfile.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Frequency Sliders (31Hz – 16kHz)",
                            subtitle = "Mapped in real time to ${capabilities.hardwareBandCount.coerceAtLeast(5)} hardware DSP bands",
                        )
                        Spacer(Modifier.height(12.dp))
                        EqualizerProfile.BAND_LABELS.forEachIndexed { idx, freqLabel ->
                            val db = activeEqProfile.bandsDb.getOrElse(idx) { 0f }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = freqLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(56.dp),
                                )
                                Slider(
                                    value = db,
                                    onValueChange = { newVal -> onUpdateEqBand(idx, newVal) },
                                    valueRange = -12f..12f,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "%+.1f dB".format(db),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extras.accent,
                                    modifier = Modifier.width(62.dp),
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // SOUND MODES & ANC DASHBOARD
                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Premium Sound Modes",
                            subtitle = if (capabilities.virtualizerSupported) {
                                "Spatial & Virtualizer DSP supported on this device"
                            } else {
                                "Uses Equalizer & Loudness shaping (Virtualizer unsupported by hardware HAL)"
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        SoundMode.entries.forEach { mode ->
                            val selected = activeSoundMode == mode
                            LuxGlassCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                contentPadding = PaddingValues(14.dp),
                                onClick = { onSelectSoundMode(mode) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(mode.title, style = MaterialTheme.typography.titleMedium)
                                            if (mode.isSpatial) StatusBadgePill(label = if (capabilities.virtualizerSupported) "3D DSP" else "EQ Stage")
                                            if (mode.isLowLatency) StatusBadgePill(label = "Low Latency")
                                        }
                                        Text(
                                            text = mode.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (selected) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = StatusConnected)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val hwAncSupported = activeDevice?.compatibilityReport?.supportsHardwareAncControl == true
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "ANC & Transparency Matrix",
                            subtitle = if (hwAncSupported) {
                                "Connected hardware supports ANC protocol"
                            } else {
                                "Protocol Gate Active — Hardware ANC not exposed"
                            },
                            trailing = {
                                Icon(
                                    imageVector = if (hwAncSupported) Icons.Rounded.GraphicEq else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = if (hwAncSupported) StatusConnected else StatusWarning,
                                )
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (activeDevice == null) {
                                "No earbuds connected. You can stage a preferred ANC mode below, and Hardik Buds Pro will verify manufacturer protocol support upon connection."
                            } else {
                                "${activeDevice.displayTitle} (${activeDevice.brand.displayName}) does not expose public Android Bluetooth ANC control APIs. Hardware ANC is never faked; use your earbuds' touch stem or OEM app for hardware ANC switching."
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
                                onClick = { onSelectAncMode(mode) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mode.title, style = MaterialTheme.typography.titleSmall)
                                        Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusBadgePill(
                                        label = if (hwAncSupported && selected) "ACTIVE" else if (selected) "STAGED" else "OEM REQ",
                                        color = if (hwAncSupported && selected) StatusConnected else if (selected) StatusInfo else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // TOUCH CONTROLS & ACOUSTIC SOUND TEST
                item {
                    val touchSupported = activeDevice?.compatibilityReport?.supportsHardwareTouchRemap == true
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Touch Controls Configurator",
                            subtitle = if (touchSupported) {
                                "Syncs directly with connected earbuds"
                            } else {
                                "Local preference profile — hardware remapping requires OEM protocol"
                            },
                            trailing = {
                                Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = extras.accent)
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        if (!touchSupported) {
                            StatusBadgePill(
                                label = "Hardware EEPROM Remap Unsupported on Standard Bluetooth",
                                color = StatusWarning,
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        Text("Left Earbud Gestures", style = MaterialTheme.typography.titleSmall, color = extras.accent)
                        Spacer(Modifier.height(6.dp))
                        TouchRow("Single Tap", touchConfig.leftSingle) { next -> onUpdateTouchConfig { it.copy(leftSingle = next) } }
                        TouchRow("Double Tap", touchConfig.leftDouble) { next -> onUpdateTouchConfig { it.copy(leftDouble = next) } }
                        TouchRow("Triple Tap", touchConfig.leftTriple) { next -> onUpdateTouchConfig { it.copy(leftTriple = next) } }
                        TouchRow("Long Press", touchConfig.leftLong) { next -> onUpdateTouchConfig { it.copy(leftLong = next) } }

                        Spacer(Modifier.height(12.dp))
                        Text("Right Earbud Gestures", style = MaterialTheme.typography.titleSmall, color = extras.accent)
                        Spacer(Modifier.height(6.dp))
                        TouchRow("Single Tap", touchConfig.rightSingle) { next -> onUpdateTouchConfig { it.copy(rightSingle = next) } }
                        TouchRow("Double Tap", touchConfig.rightDouble) { next -> onUpdateTouchConfig { it.copy(rightDouble = next) } }
                        TouchRow("Triple Tap", touchConfig.rightTriple) { next -> onUpdateTouchConfig { it.copy(rightTriple = next) } }
                        TouchRow("Long Press", touchConfig.rightLong) { next -> onUpdateTouchConfig { it.copy(rightLong = next) } }
                    }
                }

                item {
                    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Acoustic Sound Test",
                            subtitle = soundTestRunning?.let { "Playing: $it" } ?: "Real PCM stereo & sub-bass test tones",
                            trailing = {
                                Icon(Icons.Rounded.Speaker, contentDescription = null, tint = extras.accent)
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        val tests = listOf(
                            AcousticTestType.LEFT_CHANNEL,
                            AcousticTestType.RIGHT_CHANNEL,
                            AcousticTestType.STEREO_CHIME,
                            AcousticTestType.SUBWOOFER_SWEEP,
                        )
                        tests.forEach { test ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(test.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                LuxActionButton(
                                    text = if (soundTestRunning == test.label) "Playing..." else "Play",
                                    icon = Icons.Rounded.PlayArrow,
                                    filled = false,
                                    enabled = soundTestRunning == null,
                                    onClick = { onRunSoundTest(test) },
                                )
                            }
                        }
                        if (soundTestRunning != null) {
                            Spacer(Modifier.height(8.dp))
                            LuxActionButton(
                                text = "Stop Sound Test",
                                icon = Icons.Rounded.Stop,
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
            title = { Text("Save Custom EQ Profile") },
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
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(activeEqProfile.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename EQ Profile") },
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
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }

    showExportDialog?.let { json ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text("Export EQ JSON") },
            text = {
                Text(json, style = MaterialTheme.typography.labelSmall)
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(json))
                    onShowToast("EQ profile JSON copied to clipboard")
                    showExportDialog = null
                }) { Text("Copy JSON") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = null }) { Text("Close") }
            },
        )
    }

    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import EQ JSON") },
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
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            },
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
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text("$value$unit", style = MaterialTheme.typography.labelMedium, color = extras.accent)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
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
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(126.dp))
        Slider(
            value = db,
            onValueChange = onChange,
            valueRange = -6f..12f,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%+.1fdB".format(db),
            style = MaterialTheme.typography.labelSmall,
            color = extras.accent,
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(gesture, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(TouchAction.entries) { action ->
                FilterChip(
                    selected = currentAction == action,
                    onClick = { onSelect(action) },
                    label = { Text(action.label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}
