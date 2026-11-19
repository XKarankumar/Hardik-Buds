package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NoiseControlOff
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.model.HomeTheatreSettings
import com.example.model.OwnerSecuritySettings
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.model.UiModeOption
import com.example.permissions.PermissionsSnapshot
import com.example.ui.components.AnimatedBatteryIndicator
import com.example.ui.components.Earbuds3DShowcase
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.MusicReactiveBassVisualizer
import com.example.ui.components.RedSignalDot
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras

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
    fastPairCount: Int = 0,
    isFastPairScanning: Boolean = false,
    homeTheatre: HomeTheatreSettings = HomeTheatreSettings(),
    smoothRampProgress: Float? = null,
    onRequestBluetoothPerms: () -> Unit,
    onConnectOrSwitch: (DiscoveredAudioDevice) -> Unit,
    onDisconnect: () -> Unit,
    onStartGoogleFastPair: () -> Unit = {},
    onOpenFastPairSheet: () -> Unit = {},
    onSmoothConnectHomeTheatre: () -> Unit = {},
    onDisconnectHomeTheatre: () -> Unit = {},
    onNavigateToDevices: () -> Unit,
    onNavigateToSoundTab: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectSoundMode: (SoundMode) -> Unit,
    onSelectAncMode: (AncMode) -> Unit,
    onToggleSubwoofer: (Boolean) -> Unit,
    onChangeBassIntensity: (Int) -> Unit,
    onToggleGamingMode: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onLockNow: () -> Unit,
    onSelectUiMode: (UiModeOption) -> Unit = {},
    otaStatusLabel: String = "Ready",
    otaUpdateAvailable: Boolean = false,
) {
    val extras = LocalLuxThemeExtras.current
    val isConnected = activeDevice?.isConnected == true
    val gamingActive = smartSettings.gamingLowLatencyHint || soundMode == SoundMode.GAMING
    val uiMode = customization.uiMode

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Nothing OS Header Bar
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
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusBadgePill(label = "ULTRA PREMIUM", isRedActive = false)
                        StatusBadgePill(
                            label = if (isConnected) "CONNECTED" else if (isBluetoothEnabled) "STANDBY" else "BT OFF",
                            isRedActive = isConnected,
                        )
                        StatusBadgePill(label = uiMode.badge, isRedActive = uiMode != UiModeOption.STUDIO)
                    }
                }
                if (ownerSecurity.pinEnabled) {
                    LuxGlassCard(
                        contentPadding = PaddingValues(11.dp),
                        onClick = onLockNow,
                        modifier = Modifier.testTag("lock_app_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Lock Hardik Buds Pro",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Quick UI Mode Switcher Strip
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("ui_mode_strip"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(UiModeOption.entries, key = { it.name }) { mode ->
                    val selected = uiMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectUiMode(mode) },
                        label = {
                            Text(
                                text = mode.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = extras.dotMatrixFont,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                            )
                        },
                        leadingIcon = if (selected) {
                            { RedSignalDot(active = true, size = 6.dp) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                            selectedLabelColor = extras.signalRed,
                        ),
                        modifier = Modifier.testTag("ui_mode_chip_${mode.name}"),
                    )
                }
            }
        }

        // Pro Telemetry HUD (Active in PRO_TELEMETRY UI Mode)
        if (uiMode == UiModeOption.PRO_TELEMETRY) {
            item {
                ProTelemetryHudCard(
                    activeDevice = activeDevice,
                    isBluetoothEnabled = isBluetoothEnabled,
                    audioCapabilities = audioCapabilities,
                    fastPairCount = fastPairCount,
                    homeTheatre = homeTheatre,
                    ancMode = ancMode,
                )
            }
        }

        // Theatre Cinema Spotlight Banner (Active in THEATRE_AMBIENT UI Mode)
        if (uiMode == UiModeOption.THEATRE_AMBIENT) {
            item {
                TheatreAmbientHudBanner(
                    homeTheatre = homeTheatre,
                    subwoofer = subwoofer,
                    audioCapabilities = audioCapabilities,
                    smoothRampProgress = smoothRampProgress,
                    onSmoothConnect = onSmoothConnectHomeTheatre,
                    onDisconnect = onDisconnectHomeTheatre,
                )
            }
        }

        // Drive / Dock Oversized Controls (Active in DRIVE_DOCK UI Mode)
        if (uiMode == UiModeOption.DRIVE_DOCK) {
            item {
                DriveDockControlsCard(
                    activeDevice = activeDevice,
                    isConnected = isConnected,
                    ancMode = ancMode,
                    subwoofer = subwoofer,
                    homeTheatre = homeTheatre,
                    onConnectOrSwitch = {
                        if (isConnected) onDisconnect()
                        else if (activeDevice != null) onConnectOrSwitch(activeDevice)
                        else onNavigateToDevices()
                    },
                    onCycleAnc = {
                        val next = when (ancMode) {
                            AncMode.OFF -> AncMode.ANC_HIGH
                            AncMode.ANC_HIGH -> AncMode.TRANSPARENCY
                            else -> AncMode.OFF
                        }
                        onSelectAncMode(next)
                    },
                    onToggleSubwoofer = { onToggleSubwoofer(!subwoofer.enabled) },
                    onToggleTheatre = {
                        if (homeTheatre.theatreConnectedActive) onDisconnectHomeTheatre()
                        else onSmoothConnectHomeTheatre()
                    },
                )
            }
        }

        // Bluetooth Permission Card
        if (!permissions.bluetoothReady) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = extras.signalRed,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BLUETOOTH ACCESS REQUIRED",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Grant Nearby Devices & Bluetooth Connect to inspect paired earbuds, live RSSI, and hardware battery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LuxActionButton(
                        text = "Grant Bluetooth Access",
                        icon = Icons.Outlined.Bluetooth,
                        useRedAccent = true,
                        onClick = onRequestBluetoothPerms,
                        modifier = Modifier.fillMaxWidth().testTag("grant_bt_perm_button"),
                    )
                }
            }
        }

        // Circular Monochrome Earbuds Display + Telemetry Identity
        item {
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth(),
                highlightRedBorder = isConnected,
            ) {
                Earbuds3DShowcase(
                    isConnected = isConnected,
                    bassEnergy = bassEnergy,
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RedSignalDot(active = isConnected, size = 8.dp)
                            Text(
                                text = (activeDevice?.displayTitle ?: if (!isBluetoothSupported) {
                                    "BLUETOOTH UNSUPPORTED"
                                } else if (!isBluetoothEnabled) {
                                    "BLUETOOTH DISABLED"
                                } else {
                                    "NO EARBUDS CONNECTED"
                                }).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val telemetry = if (activeDevice != null) {
                            buildString {
                                append(activeDevice.brand.displayName.uppercase())
                                activeDevice.modelNumber?.let { append(" · MODEL $it") }
                                append(" · TIER ${activeDevice.compatibilityReport.tier.level}")
                                activeDevice.rssiDbm?.let { append(" · $it dBm") }
                            }
                        } else {
                            "TAP SCAN & PAIR TO DETECT NEARBY BLUETOOTH AUDIO"
                        }
                        Text(
                            text = telemetry,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Outlined.BluetoothConnected
                            isBluetoothEnabled -> Icons.Outlined.BluetoothSearching
                            else -> Icons.Outlined.BluetoothDisabled
                        },
                        contentDescription = "Bluetooth connection state",
                        tint = if (isConnected) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isConnected && activeDevice != null) {
                        LuxActionButton(
                            text = "Disconnect",
                            icon = Icons.Outlined.BluetoothDisabled,
                            filled = false,
                            useRedAccent = true,
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f).testTag("disconnect_button"),
                        )
                    } else if (activeDevice != null) {
                        LuxActionButton(
                            text = "Connect",
                            icon = Icons.Outlined.BluetoothConnected,
                            useRedAccent = true,
                            onClick = { onConnectOrSwitch(activeDevice) },
                            modifier = Modifier.weight(1f).testTag("connect_button"),
                        )
                    } else {
                        LuxActionButton(
                            text = "Scan & Pair",
                            icon = Icons.Outlined.BluetoothSearching,
                            useRedAccent = true,
                            onClick = onNavigateToDevices,
                            modifier = Modifier.weight(1f).testTag("scan_pair_button"),
                        )
                    }

                    LuxActionButton(
                        text = if (bondedDevices.size > 1) "Switch (${bondedDevices.size})" else "Switch Device",
                        icon = Icons.Outlined.Headphones,
                        filled = false,
                        onClick = onNavigateToDevices,
                        modifier = Modifier.weight(1f).testTag("switch_device_button"),
                    )
                }
            }
        }

        // Battery Telemetry Card (Real HW data only)
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Battery Telemetry",
                    subtitle = when (activeDevice?.batterySource) {
                        BatteryDataSource.ANDROID_METADATA_UNTETHERED -> "LIVE UNTETHERED L / R / CASE METADATA"
                        BatteryDataSource.BLE_GATT_BATTERY_SERVICE -> "BLE GATT BATTERY SERVICE (0x180F)"
                        BatteryDataSource.ANDROID_HFP_METADATA -> "ANDROID HFP BATTERY TELEMETRY"
                        else -> "HARDWARE DATA ONLY · UNAVAILABLE CELLS SHOWN AS N/A"
                    },
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    AnimatedBatteryIndicator(
                        label = "Left",
                        percent = activeDevice?.leftBatteryPercent,
                        isChargingOrActive = isConnected,
                        lowThreshold = smartSettings.batteryAlertThreshold,
                    )
                    AnimatedBatteryIndicator(
                        label = "Right",
                        percent = activeDevice?.rightBatteryPercent,
                        isChargingOrActive = isConnected,
                        lowThreshold = smartSettings.batteryAlertThreshold,
                    )
                    AnimatedBatteryIndicator(
                        label = "Case",
                        percent = activeDevice?.caseBatteryPercent,
                        lowThreshold = smartSettings.batteryAlertThreshold,
                    )
                }
                if (activeDevice?.batteryOverallPercent != null &&
                    activeDevice.leftBatteryPercent == null &&
                    activeDevice.rightBatteryPercent == null
                ) {
                    Spacer(Modifier.height(10.dp))
                    StatusBadgePill(
                        label = "MAIN BATTERY: ${activeDevice.batteryOverallPercent}% (SINGLE CELL)",
                        isRedActive = activeDevice.batteryOverallPercent <= smartSettings.batteryAlertThreshold,
                    )
                }
            }
        }

        // Quick Studio Shortcuts: ANC, Equalizer, Subwoofer, Gaming Mode
        if (uiMode != UiModeOption.MINIMAL_ZEN && uiMode != UiModeOption.DRIVE_DOCK) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickStudioTile(
                            title = "ANC STUDIO",
                            value = ancMode.shortLabel.uppercase(),
                            icon = Icons.Outlined.NoiseControlOff,
                            isActive = ancMode != AncMode.OFF,
                            onClick = { onNavigateToSoundTab(2) },
                            modifier = Modifier.weight(1f).testTag("shortcut_anc"),
                        )
                        QuickStudioTile(
                            title = "EQUALIZER",
                            value = "15-BAND DSP",
                            icon = Icons.Outlined.Equalizer,
                            isActive = true,
                            onClick = { onNavigateToSoundTab(0) },
                            modifier = Modifier.weight(1f).testTag("shortcut_eq"),
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickStudioTile(
                            title = "SUBWOOFER X",
                            value = if (subwoofer.enabled) "${subwoofer.bassIntensity}% BASS" else "OFF",
                            icon = Icons.Outlined.Speaker,
                            isActive = subwoofer.enabled,
                            onClick = { onNavigateToSoundTab(1) },
                            modifier = Modifier.weight(1f).testTag("shortcut_subwoofer"),
                        )
                        QuickStudioTile(
                            title = "GAMING MODE",
                            value = if (gamingActive) "LOW LATENCY" else "STANDARD",
                            icon = Icons.Outlined.SportsEsports,
                            isActive = gamingActive,
                            onClick = { onNavigateToSoundTab(3) },
                            modifier = Modifier.weight(1f).testTag("shortcut_gaming"),
                        )
                    }
                }
            }
        }

        // Google Fast Connection + Home Theatre Smooth Connection Hub
        if (uiMode != UiModeOption.MINIMAL_ZEN) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightRedBorder = homeTheatre.theatreConnectedActive || isFastPairScanning || fastPairCount > 0,
                ) {
                SectionHeader(
                    title = "Fast & Home Theatre Connect",
                    subtitle = "GOOGLE 0xFE2C FAST PAIR · SMOOTH CINEMA AUDIO RAMP",
                    trailing = {
                        StatusBadgePill(
                            label = if (homeTheatre.theatreConnectedActive) "THEATRE LIVE" else if (isFastPairScanning) "FAST SCAN" else "READY",
                            isRedActive = homeTheatre.theatreConnectedActive || isFastPairScanning,
                        )
                    },
                )
                Spacer(Modifier.height(12.dp))

                // Google Fast Connection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = "Google Fast Connection",
                            tint = if (isFastPairScanning || fastPairCount > 0) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("GOOGLE FAST CONNECT", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = when {
                                    isFastPairScanning -> "SCANNING 0xFE2C FAST PAIR BEACONS..."
                                    fastPairCount > 0 -> "$fastPairCount FAST PAIR DEVICE(S) NEARBY"
                                    else -> "ONE-TAP BLE 0xFE2C COMPANION PAIRING"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    LuxActionButton(
                        text = if (isFastPairScanning) "Hub" else "Fast Pair",
                        icon = Icons.Outlined.Bolt,
                        useRedAccent = fastPairCount > 0,
                        filled = true,
                        onClick = {
                            if (fastPairCount > 0) onNavigateToDevices() else {
                                onStartGoogleFastPair()
                                onOpenFastPairSheet()
                            }
                        },
                        modifier = Modifier.testTag("google_fast_connect_btn"),
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Home Theatre Smooth Connection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Outlined.SurroundSound,
                            contentDescription = "Home Theatre Smooth Connection",
                            tint = if (homeTheatre.theatreConnectedActive) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("HOME THEATRE SMOOTH", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = homeTheatre.lastSmoothTransitionStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    LuxActionButton(
                        text = if (homeTheatre.theatreConnectedActive) "Exit" else "Smooth",
                        icon = Icons.Outlined.SurroundSound,
                        filled = homeTheatre.theatreConnectedActive,
                        useRedAccent = homeTheatre.theatreConnectedActive,
                        onClick = {
                            if (homeTheatre.theatreConnectedActive) onDisconnectHomeTheatre() else onSmoothConnectHomeTheatre()
                        },
                        modifier = Modifier.testTag("home_theatre_smooth_btn"),
                    )
                }

                if (smoothRampProgress != null) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { smoothRampProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = extras.signalRed,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    )
                }
            }
        }
        }

        // Gaming Mode Quick Switch Card
        if (customization.showGamingCard && uiMode != UiModeOption.MINIMAL_ZEN && uiMode != UiModeOption.DRIVE_DOCK) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightRedBorder = gamingActive,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RedSignalDot(active = gamingActive, size = 8.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("GAMING LOW-LATENCY", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${audioCapabilities.outputSampleRateHz}Hz · ${audioCapabilities.outputRouteLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = gamingActive,
                            onCheckedChange = onToggleGamingMode,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                            modifier = Modifier.testTag("home_gaming_switch"),
                        )
                    }
                }
            }
        }

        // Subwoofer Engine X Quick Control + Visualizer
        if (customization.showSubwooferCard && uiMode != UiModeOption.MINIMAL_ZEN) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigateToSoundTab(1) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SUBWOOFER ENGINE X", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "SOFT + HAL BASS DSP · ${subwoofer.bassIntensity}% · ${subwoofer.lowFreqCutoffHz}Hz",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = subwoofer.enabled,
                            onCheckedChange = onToggleSubwoofer,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                            modifier = Modifier.testTag("home_subwoofer_switch"),
                        )
                    }

                    if (customization.showMiniVisualizerWidget) {
                        Spacer(Modifier.height(10.dp))
                        MusicReactiveBassVisualizer(
                            energy = bassEnergy,
                            bassIntensity = subwoofer.bassIntensity,
                            enabled = subwoofer.enabled,
                            useRedFrequencyBars = true,
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("BASS LEVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${subwoofer.bassIntensity}/100", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                    }
                    Slider(
                        value = subwoofer.bassIntensity.toFloat(),
                        onValueChange = { onChangeBassIntensity(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = subwoofer.enabled,
                        colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                        modifier = Modifier.testTag("home_bass_slider"),
                    )
                }
            }
        }

        // Quick Sound & ANC Strip
        if (customization.showQuickAncCard) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Sound & ANC Quick Strip",
                        subtitle = "MODE: ${soundMode.title.uppercase()} · ANC: ${ancMode.shortLabel.uppercase()}",
                        trailing = { Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SoundMode.entries.take(6)) { mode ->
                            FilterChip(
                                selected = soundMode == mode,
                                onClick = { onSelectSoundMode(mode) },
                                label = { Text(mode.title.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                    selectedLabelColor = extras.signalRed,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(AncMode.OFF, AncMode.ANC_LOW, AncMode.ANC_MEDIUM, AncMode.ANC_HIGH, AncMode.TRANSPARENCY, AncMode.ADAPTIVE_ANC)) { mode ->
                            FilterChip(
                                selected = ancMode == mode,
                                onClick = { onSelectAncMode(mode) },
                                label = { Text(mode.shortLabel.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                    selectedLabelColor = extras.signalRed,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Hearing Protection Monitor
        if (smartSettings.hearingProtectionEnabled && uiMode != UiModeOption.MINIMAL_ZEN) {
            item {
                val over = volumePercent >= smartSettings.hearingWarningPercent
                LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = over) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (over) Icons.Outlined.WarningAmber else Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = null,
                                tint = if (over) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (over) "HIGH VOLUME ALERT ($volumePercent%)" else "HEARING PROTECTION",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (over) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "OUTPUT $volumePercent% · LIMIT ${smartSettings.hearingWarningPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        StatusBadgePill(label = "$volumePercent%", isRedActive = over)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (volumePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = if (over) extras.signalRed else MaterialTheme.colorScheme.onBackground,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    )
                }
            }
        }

        // Compatibility & Firmware Cards
        if (customization.showCompatibilityCard && uiMode != UiModeOption.MINIMAL_ZEN && uiMode != UiModeOption.DRIVE_DOCK) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToDevices) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (activeDevice?.compatibilityReport?.tier?.title ?: "17-BRAND COMPATIBILITY ENGINE").uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = activeDevice?.compatibilityReport?.summaryNotice
                                    ?: "Automatic brand tiering for OnePlus, Samsung, AirPods, Sony, Nothing, Pixel Buds, JBL, boAt & more.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (customization.showFirmwareCard && uiMode != UiModeOption.MINIMAL_ZEN && uiMode != UiModeOption.DRIVE_DOCK) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightRedBorder = otaUpdateAvailable,
                    onClick = onCheckUpdates,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = if (otaUpdateAvailable) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (otaUpdateAvailable) "OTA UPDATE AVAILABLE" else "SOFTWARE & FIRMWARE UPDATES",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (otaUpdateAvailable) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "v${firmwareState.currentAppVersion} · $otaStatusLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        LuxActionButton(
                            text = if (otaUpdateAvailable) "Update" else "Open",
                            useRedAccent = otaUpdateAvailable,
                            filled = otaUpdateAvailable,
                            onClick = onCheckUpdates,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun QuickStudioTile(
    title: String,
    value: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        highlightRedBorder = isActive,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            RedSignalDot(active = isActive, size = 7.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = extras.dotMatrixFont),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProTelemetryHudCard(
    activeDevice: DiscoveredAudioDevice?,
    isBluetoothEnabled: Boolean,
    audioCapabilities: AudioEngineCapabilities,
    fastPairCount: Int,
    homeTheatre: HomeTheatreSettings,
    ancMode: AncMode,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(
        modifier = Modifier.fillMaxWidth().testTag("pro_telemetry_hud_card"),
        highlightRedBorder = true,
    ) {
        SectionHeader(
            title = "Pro Telemetry HUD",
            subtitle = "REAL-TIME HARDWARE & DSP MATRIX",
            trailing = { StatusBadgePill(label = "LIVE HUD", isRedActive = true) },
        )
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCell(
                label = "BT RADIO / RSSI",
                value = if (!isBluetoothEnabled) "BT OFF"
                else activeDevice?.rssiDbm?.let { "$it dBm" } ?: "IDLE",
                modifier = Modifier.weight(1f),
            )
            TelemetryCell(
                label = "OUTPUT ROUTE",
                value = audioCapabilities.outputRouteLabel.uppercase(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCell(
                label = "SAMPLE / SESSION",
                value = "${audioCapabilities.outputSampleRateHz}Hz · #${audioCapabilities.activeAudioSessionId}",
                modifier = Modifier.weight(1f),
            )
            TelemetryCell(
                label = "DSP PIPELINE",
                value = buildString {
                    append(if (audioCapabilities.bassBoostSupported) "BB+" else "EQ")
                    if (audioCapabilities.loudnessSupported) append("/LD")
                    if (audioCapabilities.virtualizerSupported) append("/VT")
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCell(
                label = "FAST PAIR 0xFE2C",
                value = "$fastPairCount BEACONS",
                modifier = Modifier.weight(1f),
            )
            TelemetryCell(
                label = "THEATRE / ANC",
                value = "${if (homeTheatre.theatreConnectedActive) "LIVE" else "OFF"} · ${ancMode.shortLabel.uppercase()}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TelemetryCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = extras.dotMatrixFont),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TheatreAmbientHudBanner(
    homeTheatre: HomeTheatreSettings,
    subwoofer: SubwooferConfig,
    audioCapabilities: AudioEngineCapabilities,
    smoothRampProgress: Float?,
    onSmoothConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(
        modifier = Modifier.fillMaxWidth().testTag("theatre_ambient_hud_banner"),
        highlightRedBorder = homeTheatre.theatreConnectedActive,
    ) {
        SectionHeader(
            title = "Cinema Theatre Mode",
            subtitle = "${audioCapabilities.outputRouteLabel.uppercase()} · LFE ${homeTheatre.lfeCrossoverHz}HZ · BASS ${subwoofer.bassIntensity}%",
            trailing = {
                StatusBadgePill(
                    label = if (homeTheatre.theatreConnectedActive) "CINEMA LIVE" else "STANDBY",
                    isRedActive = homeTheatre.theatreConnectedActive,
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = homeTheatre.lastSmoothTransitionStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (smoothRampProgress != null) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { smoothRampProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = extras.signalRed,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
        }
        Spacer(Modifier.height(10.dp))
        LuxActionButton(
            text = if (homeTheatre.theatreConnectedActive) "Exit Cinema Theatre" else "Smooth Connect Cinema",
            icon = Icons.Outlined.SurroundSound,
            useRedAccent = true,
            filled = true,
            onClick = { if (homeTheatre.theatreConnectedActive) onDisconnect() else onSmoothConnect() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DriveDockControlsCard(
    activeDevice: DiscoveredAudioDevice?,
    isConnected: Boolean,
    ancMode: AncMode,
    subwoofer: SubwooferConfig,
    homeTheatre: HomeTheatreSettings,
    onConnectOrSwitch: () -> Unit,
    onCycleAnc: () -> Unit,
    onToggleSubwoofer: () -> Unit,
    onToggleTheatre: () -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(
        modifier = Modifier.fillMaxWidth().testTag("drive_dock_grid"),
        highlightRedBorder = true,
    ) {
        SectionHeader(
            title = "Drive / Dock Large HUD",
            subtitle = (activeDevice?.displayTitle ?: "TAP CONTROL BELOW").uppercase(),
            trailing = { StatusBadgePill(label = "DOCK", isRedActive = true) },
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DockTileButton(
                title = if (isConnected) "DISCONNECT" else "CONNECT",
                subtitle = if (isConnected) "ACTIVE LINK" else "BLUETOOTH",
                icon = if (isConnected) Icons.Outlined.BluetoothConnected else Icons.Outlined.BluetoothSearching,
                isActive = isConnected,
                onClick = onConnectOrSwitch,
                modifier = Modifier.weight(1f),
            )
            DockTileButton(
                title = "ANC CYCLE",
                subtitle = ancMode.shortLabel.uppercase(),
                icon = Icons.Outlined.NoiseControlOff,
                isActive = ancMode != AncMode.OFF,
                onClick = onCycleAnc,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DockTileButton(
                title = "SUBWOOFER X",
                subtitle = if (subwoofer.enabled) "${subwoofer.bassIntensity}% ON" else "BYPASS",
                icon = Icons.Outlined.Speaker,
                isActive = subwoofer.enabled,
                onClick = onToggleSubwoofer,
                modifier = Modifier.weight(1f),
            )
            DockTileButton(
                title = "THEATRE",
                subtitle = if (homeTheatre.theatreConnectedActive) "LIVE" else "SMOOTH",
                icon = Icons.Outlined.SurroundSound,
                isActive = homeTheatre.theatreConnectedActive,
                onClick = onToggleTheatre,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DockTileButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(
        modifier = modifier.height(92.dp),
        contentPadding = PaddingValues(14.dp),
        highlightRedBorder = isActive,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
            RedSignalDot(active = isActive, size = 8.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = extras.dotMatrixFont),
            color = if (isActive) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

