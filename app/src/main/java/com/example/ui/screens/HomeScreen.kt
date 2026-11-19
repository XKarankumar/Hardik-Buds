package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AncMode
import com.example.model.AudioEngineCapabilities
import com.example.model.BatteryDataSource
import com.example.model.CustomizationSettings
import com.example.model.DiscoveredAudioDevice
import com.example.model.FirmwareUpdateState
import com.example.model.OwnerSecuritySettings
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.permissions.PermissionsSnapshot
import com.example.ui.components.AnimatedBatteryIndicator
import com.example.ui.components.Earbuds3DShowcase
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.MusicReactiveBassVisualizer
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning

@Composable
fun HomeScreen(
    activeDevice: DiscoveredAudioDevice?,
    bondedDevices: List<DiscoveredAudioDevice>,
    isBluetoothEnabled: Boolean,
    isBluetoothSupported: Boolean,
    permissions: PermissionsSnapshot,
    customization: CustomizationSettings,
    subwoofer: SubwooferConfig,
    soundMode: SoundMode,
    ancMode: AncMode,
    audioCapabilities: AudioEngineCapabilities,
    bassEnergy: Float,
    volumePercent: Int,
    smartSettings: SmartSettings,
    ownerSecurity: OwnerSecuritySettings,
    firmwareState: FirmwareUpdateState,
    onRequestBluetoothPerms: () -> Unit,
    onConnectOrSwitch: (DiscoveredAudioDevice) -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToSound: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectSoundMode: (SoundMode) -> Unit,
    onSelectAncMode: (AncMode) -> Unit,
    onToggleSubwoofer: (Boolean) -> Unit,
    onChangeBassIntensity: (Int) -> Unit,
    onCheckUpdates: () -> Unit,
    onLockNow: () -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    val isConnected = activeDevice?.isConnected == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Luxury Header Brand Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HARDIK BUDS PRO",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusBadgePill(label = "ULTRA PREMIUM", color = extras.accent)
                        StatusBadgePill(
                            label = if (isConnected) "LINK ACTIVE" else if (isBluetoothEnabled) "STANDBY" else "BT OFF",
                            color = if (isConnected) StatusConnected else if (isBluetoothEnabled) StatusInfo else StatusWarning,
                        )
                    }
                }
                if (ownerSecurity.pinEnabled) {
                    LuxGlassCard(
                        contentPadding = PaddingValues(10.dp),
                        onClick = onLockNow,
                        modifier = Modifier.testTag("lock_app_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock Hardik Buds Pro",
                            tint = extras.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // Permission Prompt Banner if Bluetooth permissions missing
        if (!permissions.bluetoothReady) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bluetooth Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Grant Nearby Devices & Bluetooth Connect to detect paired earbuds, connection state, and real hardware battery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LuxActionButton(
                        text = "Grant Bluetooth Access",
                        icon = Icons.Rounded.Bluetooth,
                        onClick = onRequestBluetoothPerms,
                        modifier = Modifier.fillMaxWidth().testTag("grant_bt_perm_button"),
                    )
                }
            }
        }

        // Hero 3D Earbuds Showcase + Real Hardware Identity
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                Earbuds3DShowcase(
                    isConnected = isConnected,
                    bassEnergy = bassEnergy,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeDevice?.displayTitle ?: if (!isBluetoothSupported) {
                                "Bluetooth Unavailable"
                            } else if (!isBluetoothEnabled) {
                                "Bluetooth Disabled"
                            } else {
                                "No Earbuds Connected"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        val subtitle = if (activeDevice != null) {
                            buildString {
                                append(activeDevice.brand.displayName)
                                append(" · ")
                                append(activeDevice.compatibilityReport.tier.title)
                                activeDevice.rssiDbm?.let { append(" · $it dBm") }
                            }
                        } else {
                            "Pair or select a Bluetooth audio device to inspect live telemetry"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Rounded.BluetoothConnected
                            isBluetoothEnabled -> Icons.AutoMirrored.Rounded.BluetoothSearching
                            else -> Icons.Rounded.BluetoothDisabled
                        },
                        contentDescription = "Bluetooth connection icon",
                        tint = if (isConnected) StatusConnected else extras.accent,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Real Connect / Disconnect / Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isConnected && activeDevice != null) {
                        LuxActionButton(
                            text = "Disconnect",
                            icon = Icons.Rounded.BluetoothDisabled,
                            filled = false,
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f).testTag("disconnect_button"),
                        )
                    } else if (activeDevice != null) {
                        LuxActionButton(
                            text = "Connect",
                            icon = Icons.Rounded.BluetoothConnected,
                            onClick = { onConnectOrSwitch(activeDevice) },
                            modifier = Modifier.weight(1f).testTag("connect_button"),
                        )
                    } else {
                        LuxActionButton(
                            text = "Scan & Pair",
                            icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
                            onClick = onNavigateToDevices,
                            modifier = Modifier.weight(1f).testTag("scan_pair_button"),
                        )
                    }

                    LuxActionButton(
                        text = if (bondedDevices.size > 1) "Switch (${bondedDevices.size})" else "Devices",
                        icon = Icons.Rounded.Headphones,
                        filled = false,
                        onClick = onNavigateToDevices,
                        modifier = Modifier.weight(1f).testTag("switch_device_button"),
                    )
                }
            }
        }

        // Real Hardware Battery Matrix (Left / Right / Case / Main)
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Battery Telemetry",
                    subtitle = when (activeDevice?.batterySource) {
                        BatteryDataSource.ANDROID_METADATA_UNTETHERED -> "Live untethered Left, Right & Case metadata"
                        BatteryDataSource.BLE_GATT_BATTERY_SERVICE -> "BLE GATT Battery Service (0x180F)"
                        BatteryDataSource.ANDROID_HFP_METADATA -> "Android Bluetooth HFP main battery level"
                        else -> "Real hardware data only — unavailable fields marked honestly"
                    },
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    AnimatedBatteryIndicator(
                        label = "Left Bud",
                        percent = activeDevice?.leftBatteryPercent,
                        isChargingOrActive = isConnected,
                    )
                    AnimatedBatteryIndicator(
                        label = "Right Bud",
                        percent = activeDevice?.rightBatteryPercent,
                        isChargingOrActive = isConnected,
                    )
                    AnimatedBatteryIndicator(
                        label = "Case",
                        percent = activeDevice?.caseBatteryPercent,
                    )
                }
                if (activeDevice?.batteryOverallPercent != null &&
                    activeDevice.leftBatteryPercent == null &&
                    activeDevice.rightBatteryPercent == null
                ) {
                    Spacer(Modifier.height(10.dp))
                    StatusBadgePill(
                        label = "Overall Bluetooth Battery: ${activeDevice.batteryOverallPercent}% (Single-cell reported by hardware)",
                        color = StatusConnected,
                    )
                }
            }
        }

        // Hearing Protection & Volume Monitor
        if (smartSettings.hearingProtectionEnabled) {
            item {
                val overThreshold = volumePercent >= smartSettings.hearingWarningPercent
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (overThreshold) Icons.Rounded.WarningAmber else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = if (overThreshold) StatusWarning else extras.accent,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (overThreshold) "High Volume Warning ($volumePercent%)" else "Hearing Protection Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (overThreshold) StatusWarning else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Media output at $volumePercent% · Alert threshold ${smartSettings.hearingWarningPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        StatusBadgePill(
                            label = "$volumePercent%",
                            color = if (overThreshold) StatusCritical else StatusConnected,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (volumePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (overThreshold) StatusWarning else extras.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        // Customizable Card: Subwoofer Engine X & Music-Reactive Visualizer
        if (customization.showSubwooferCard) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToSound,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Speaker, contentDescription = null, tint = extras.accent)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SUBWOOFER ENGINE X",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (audioCapabilities.bassBoostSupported) {
                                        "Active DSP · Intensity ${subwoofer.bassIntensity}% · Cutoff ${subwoofer.lowFreqCutoffHz}Hz"
                                    } else {
                                        "DSP Band Shaping · Intensity ${subwoofer.bassIntensity}%"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = subwoofer.enabled,
                            onCheckedChange = onToggleSubwoofer,
                            modifier = Modifier.testTag("home_subwoofer_switch"),
                        )
                    }

                    if (customization.showMiniVisualizerWidget) {
                        Spacer(Modifier.height(10.dp))
                        MusicReactiveBassVisualizer(
                            energy = bassEnergy,
                            bassIntensity = subwoofer.bassIntensity,
                            enabled = subwoofer.enabled,
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Bass Intensity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${subwoofer.bassIntensity} / 100", style = MaterialTheme.typography.labelMedium, color = extras.accent)
                    }
                    Slider(
                        value = subwoofer.bassIntensity.toFloat(),
                        onValueChange = { onChangeBassIntensity(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = subwoofer.enabled,
                        modifier = Modifier.testTag("home_bass_slider"),
                    )
                }
            }
        }

        // Customizable Card: Quick Sound Mode & ANC Bar
        if (customization.showQuickAncCard) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Sound Mode & ANC",
                        subtitle = if (activeDevice?.compatibilityReport?.supportsHardwareAncControl == true) {
                            "Active Sound: ${soundMode.title} · Hardware ANC linked"
                        } else {
                            "Active Sound: ${soundMode.title} · Hardware ANC requires OEM protocol"
                        },
                        trailing = {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = extras.accent)
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SoundMode.entries.take(6)) { mode ->
                            FilterChip(
                                selected = soundMode == mode,
                                onClick = { onSelectSoundMode(mode) },
                                label = { Text(mode.title) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.NoiseControlOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "ANC Mode Preference: ${ancMode.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(AncMode.OFF, AncMode.ANC_HIGH, AncMode.ADAPTIVE_ANC, AncMode.TRANSPARENCY)) { mode ->
                            FilterChip(
                                selected = ancMode == mode,
                                onClick = { onSelectAncMode(mode) },
                                label = { Text(mode.shortLabel) },
                            )
                        }
                    }
                }
            }
        }

        // Customizable Card: Compatibility & Owner Identity
        if (customization.showCompatibilityCard) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToDevices,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = extras.accent)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeDevice?.compatibilityReport?.tier?.title ?: "17-Brand Compatibility Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = activeDevice?.compatibilityReport?.summaryNotice
                                    ?: "Supports OnePlus, Samsung, AirPods, Sony, Nothing, Pixel Buds, JBL, boAt, Noise & more.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Customizable Card: Firmware & App Update Center
        if (customization.showFirmwareCard) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToSettings,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = extras.accent)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Firmware & App Updates",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "App v${firmwareState.currentAppVersion} · Device FW: ${firmwareState.connectedDeviceFirmware ?: "Check BLE 0x2A26"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        LuxActionButton(
                            text = "Check",
                            filled = false,
                            onClick = onCheckUpdates,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}
