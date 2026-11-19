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
import androidx.compose.foundation.lazy.items
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SettingsBluetooth
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SpeakerGroup
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.ConnectionHistoryEntity
import com.example.model.AudioOutputRoute
import com.example.model.DiscoveredAudioDevice
import com.example.model.FeatureSupportLevel
import com.example.model.HomeTheatreSettings
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionsSnapshot
import com.example.ui.components.BluetoothRadarScanner
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.RedSignalDot
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DevicesScreen(
    activeDevice: DiscoveredAudioDevice?,
    bondedDevices: List<DiscoveredAudioDevice>,
    nearbyDevices: List<DiscoveredAudioDevice>,
    fastPairDevices: List<DiscoveredAudioDevice> = emptyList(),
    audioOutputRoutes: List<AudioOutputRoute> = emptyList(),
    connectionHistory: List<ConnectionHistoryEntity>,
    isBluetoothEnabled: Boolean,
    isScanning: Boolean,
    isFastPairScanning: Boolean = false,
    homeTheatre: HomeTheatreSettings = HomeTheatreSettings(),
    smoothRampProgress: Float? = null,
    permissions: PermissionsSnapshot,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onStartGoogleFastPairScan: () -> Unit = {},
    onLaunchCompanionFastPair: () -> Unit = {},
    onRequestBtPerms: () -> Unit,
    onConnectOrSelect: (DiscoveredAudioDevice) -> Unit,
    onDisconnect: () -> Unit,
    onRenameDevice: (DiscoveredAudioDevice, String) -> Unit,
    onPinHomeTheatreDevice: (DiscoveredAudioDevice) -> Unit = {},
    onUpdateHomeTheatre: ((HomeTheatreSettings) -> HomeTheatreSettings) -> Unit = {},
    onSmoothConnectHomeTheatre: (DiscoveredAudioDevice?, AudioOutputRoute?) -> Unit = { _, _ -> },
    onDisconnectHomeTheatre: () -> Unit = {},
    onFindEarbudsPing: () -> Unit,
) {
    val context = LocalContext.current
    val extras = LocalLuxThemeExtras.current
    var showAllBrandsMatrix by remember { mutableStateOf(false) }
    var showPairingGuide by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<DiscoveredAudioDevice?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("devices_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header + Bluetooth Radar Scanner
        item {
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth(),
                highlightRedBorder = isScanning,
            ) {
                SectionHeader(
                    title = "Device Hub",
                    subtitle = when {
                        !isBluetoothEnabled -> "BLUETOOTH ADAPTER DISABLED"
                        isScanning -> "BLE & CLASSIC DISCOVERY ACTIVE"
                        else -> "${bondedDevices.size} PAIRED · ${nearbyDevices.size} NEARBY"
                    },
                    trailing = {
                        IconButton(
                            onClick = { PermissionManager.openBluetoothSettings(context) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                Icons.Outlined.SettingsBluetooth,
                                contentDescription = "Open System Bluetooth Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))

                BluetoothRadarScanner(
                    isScanning = isScanning,
                    deviceCount = nearbyDevices.size,
                )

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!permissions.bluetoothReady) {
                        LuxActionButton(
                            text = "Allow Bluetooth",
                            icon = Icons.Outlined.Bluetooth,
                            useRedAccent = true,
                            onClick = onRequestBtPerms,
                            modifier = Modifier.weight(1f).testTag("devices_allow_bt_button"),
                        )
                    } else {
                        LuxActionButton(
                            text = if (isScanning) "Stop Scan" else "Scan Radar",
                            icon = Icons.AutoMirrored.Outlined.BluetoothSearching,
                            useRedAccent = !isScanning,
                            filled = !isScanning,
                            onClick = if (isScanning) onStopScan else onStartScan,
                            modifier = Modifier.weight(1f).testTag("scan_devices_button"),
                        )
                    }
                    LuxActionButton(
                        text = "Find Buds",
                        icon = Icons.Outlined.NotificationsActive,
                        filled = false,
                        onClick = onFindEarbudsPing,
                        modifier = Modifier.weight(1f).testTag("find_earbuds_button"),
                    )
                }
            }
        }

        // GOOGLE FAST CONNECTION (0xFE2C GFPS + Android CompanionDeviceManager)
        item {
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth().testTag("google_fast_pair_card"),
                highlightRedBorder = isFastPairScanning || fastPairDevices.isNotEmpty(),
            ) {
                SectionHeader(
                    title = "Google Fast Connection",
                    subtitle = "BLE 0xFE2C FAST PAIR · ONE-TAP COMPANION PAIRING",
                    trailing = {
                        StatusBadgePill(
                            label = if (isFastPairScanning) "SCANNING 0xFE2C" else "${fastPairDevices.size} FOUND",
                            isRedActive = isFastPairScanning || fastPairDevices.isNotEmpty(),
                        )
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Detects nearby Google Fast Pair (0xFE2C) earbuds & speakers and opens Android's native OS Companion Fast Pair prompt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = "One-Tap Fast Pair",
                        icon = Icons.Outlined.Bolt,
                        useRedAccent = true,
                        onClick = onLaunchCompanionFastPair,
                        modifier = Modifier.weight(1f).testTag("companion_fast_pair_button"),
                    )
                    LuxActionButton(
                        text = if (isFastPairScanning) "Stop" else "Scan 0xFE2C",
                        icon = Icons.AutoMirrored.Outlined.BluetoothSearching,
                        filled = false,
                        onClick = if (isFastPairScanning) onStopScan else onStartGoogleFastPairScan,
                        modifier = Modifier.weight(1f).testTag("fast_pair_ble_scan_button"),
                    )
                }
                if (fastPairDevices.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    fastPairDevices.forEach { fp ->
                        LuxGlassCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            contentPadding = PaddingValues(12.dp),
                            highlightRedBorder = true,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fp.displayTitle.uppercase(), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "FAST PAIR ID ${fp.fastPairModelId ?: "0xFE2C"} · ${fp.rssiDbm ?: -60} dBm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extras.signalRed,
                                    )
                                }
                                LuxActionButton(
                                    text = if (fp.isBonded) "Connect" else "Fast Pair",
                                    useRedAccent = true,
                                    onClick = { onConnectOrSelect(fp) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // HOME THEATRE SMOOTH CONNECTION (Audio Routes + Smooth Gain Ramp + LFE Crossover)
        item {
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth().testTag("home_theatre_card"),
                highlightRedBorder = homeTheatre.theatreConnectedActive,
            ) {
                SectionHeader(
                    title = "Home Theatre Smooth Connect",
                    subtitle = "HDMI / eARC · USB DAC · BLUETOOTH SOUNDBARS · SMOOTH RAMP",
                    trailing = {
                        Icon(
                            Icons.Outlined.SurroundSound,
                            contentDescription = null,
                            tint = if (homeTheatre.theatreConnectedActive) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = homeTheatre.lastSmoothTransitionStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (homeTheatre.theatreConnectedActive) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (smoothRampProgress != null) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { smoothRampProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = extras.signalRed,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Smooth Volume & Bass Fade Ramp", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = homeTheatre.smoothFadeHandoff,
                        onCheckedChange = { v -> onUpdateHomeTheatre { it.copy(smoothFadeHandoff = v) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Auto Cinema Surround & LFE", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = homeTheatre.autoCinemaSubwoofer,
                        onCheckedChange = { v -> onUpdateHomeTheatre { it.copy(autoCinemaSubwoofer = v) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = extras.signalRed),
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ROOM LFE CROSSOVER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${homeTheatre.lfeCrossoverHz} Hz", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                }
                Slider(
                    value = homeTheatre.lfeCrossoverHz.toFloat(),
                    onValueChange = { hz -> onUpdateHomeTheatre { it.copy(lfeCrossoverHz = hz.toInt()) } },
                    valueRange = 40f..140f,
                    colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                )

                if (audioOutputRoutes.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("DETECTED SYSTEM AUDIO ROUTES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    audioOutputRoutes.take(5).forEach { route ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(route.productName.uppercase(), style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${route.typeLabel} · ${route.maxChannels}CH · ${route.sampleRateLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (route.isHomeTheatreCapable) {
                                LuxActionButton(
                                    text = "Smooth",
                                    filled = false,
                                    useRedAccent = true,
                                    onClick = { onSmoothConnectHomeTheatre(null, route) },
                                )
                            } else {
                                StatusBadgePill(label = "OUTPUT")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = if (homeTheatre.theatreConnectedActive) "Exit Cinema Mode" else "Smooth Connect",
                        icon = Icons.Outlined.SpeakerGroup,
                        useRedAccent = true,
                        onClick = {
                            if (homeTheatre.theatreConnectedActive) onDisconnectHomeTheatre()
                            else onSmoothConnectHomeTheatre(null, null)
                        },
                        modifier = Modifier.weight(1f).testTag("devices_home_theatre_btn"),
                    )
                    LuxActionButton(
                        text = "Output Panel",
                        icon = Icons.Outlined.Tune,
                        filled = false,
                        onClick = {
                            runCatching {
                                context.startActivity(Intent("android.settings.panel.action.VOLUME"))
                            }.onFailure {
                                PermissionManager.openBluetoothSettings(context)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Last Connected Device Telemetry Card
        val lastRecord = connectionHistory.firstOrNull()
        if (activeDevice != null || lastRecord != null) {
            item {
                val fmt = remember { SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault()) }
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlightRedBorder = activeDevice?.isConnected == true,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (activeDevice?.isConnected == true) "ACTIVE SESSION" else "LAST CONNECTED DEVICE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeDevice?.isConnected == true) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = (activeDevice?.displayTitle ?: lastRecord?.deviceName ?: "").uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val subtitle = if (activeDevice != null) {
                                "${activeDevice.brand.displayName.uppercase()} · ${activeDevice.address}"
                            } else if (lastRecord != null) {
                                "${lastRecord.brandName.uppercase()} · ${fmt.format(Date(lastRecord.connectedAt))}"
                            } else ""
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusBadgePill(
                            label = if (activeDevice?.isConnected == true) "LIVE" else "RECORDED",
                            isRedActive = activeDevice?.isConnected == true,
                        )
                    }
                }
            }
        }

        // Paired Devices Section
        item {
            SectionHeader(
                title = "Paired Earbuds & Audio",
                subtitle = "BONDED SYSTEM DEVICES · TAP TO SWITCH OR RENAME",
            )
        }

        if (bondedDevices.isEmpty()) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Headphones, contentDescription = null, tint = extras.signalRed)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NO PAIRED AUDIO DEVICES", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (!permissions.bluetoothReady) {
                                    "Grant Bluetooth permissions to inspect paired earbuds."
                                } else {
                                    "Pair earbuds in Android Bluetooth Settings or run the BLE radar scanner above."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LuxActionButton(
                        text = "Open System Bluetooth Pairing",
                        icon = Icons.Outlined.SettingsBluetooth,
                        filled = false,
                        onClick = { PermissionManager.openBluetoothSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            items(bondedDevices, key = { it.address }) { dev ->
                DeviceCard(
                    device = dev,
                    isSelected = activeDevice?.address == dev.address,
                    isPinnedTheatre = homeTheatre.preferredTheatreAddress == dev.address,
                    onConnect = { onConnectOrSelect(dev) },
                    onDisconnect = onDisconnect,
                    onRename = { renameTarget = dev },
                    onPinTheatre = { onPinHomeTheatreDevice(dev) },
                )
            }
        }

        // Nearby Scanned Devices Section
        if (nearbyDevices.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Nearby Discovered Devices",
                    subtitle = "LIVE BLE & CLASSIC SCAN WITH REAL RSSI TELEMETRY",
                )
            }
            items(nearbyDevices, key = { "near_${it.address}" }) { dev ->
                DeviceCard(
                    device = dev,
                    isSelected = activeDevice?.address == dev.address,
                    isPinnedTheatre = homeTheatre.preferredTheatreAddress == dev.address,
                    onConnect = { onConnectOrSelect(dev) },
                    onDisconnect = onDisconnect,
                    onRename = { renameTarget = dev },
                    onPinTheatre = { onPinHomeTheatreDevice(dev) },
                )
            }
        }

        // Active Device Compatibility & Info
        if (activeDevice != null) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Device Telemetry & Compatibility",
                        subtitle = "${activeDevice.brand.displayName.uppercase()} · ${activeDevice.compatibilityReport.tier.title.uppercase()}",
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadgePill(label = activeDevice.address)
                        activeDevice.firmwareRevision?.let { fw ->
                            StatusBadgePill(label = "FW $fw", isRedActive = true)
                        }
                        activeDevice.modelNumber?.let { mdl ->
                            StatusBadgePill(label = mdl)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    activeDevice.compatibilityReport.features.forEach { feat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = when (feat.supportLevel) {
                                    FeatureSupportLevel.SUPPORTED -> Icons.Outlined.CheckCircle
                                    FeatureSupportLevel.ANDROID_DSP_ONLY -> Icons.Outlined.Info
                                    else -> Icons.Outlined.WarningAmber
                                },
                                contentDescription = null,
                                tint = when (feat.supportLevel) {
                                    FeatureSupportLevel.SUPPORTED -> extras.signalRed
                                    FeatureSupportLevel.ANDROID_DSP_ONLY -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(17.dp).padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(feat.featureName.uppercase(), style = MaterialTheme.typography.labelLarge)
                                Text(feat.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // 17-Brand Compatibility Matrix
        item {
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showAllBrandsMatrix = !showAllBrandsMatrix },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("17-BRAND COMPATIBILITY ENGINE", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "OnePlus, Samsung, AirPods, Realme, Oppo, Xiaomi, Redmi, Vivo, iQOO, Nothing, JBL, Sony, boAt, Noise, Huawei, Pixel Buds & Generic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        if (showAllBrandsMatrix) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Toggle brand compatibility matrix",
                    )
                }

                if (showAllBrandsMatrix) {
                    Spacer(Modifier.height(12.dp))
                    DeviceCompatibilityEngine.allSupportedBrandsOverview().forEach { rep ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(rep.brand.displayName.uppercase(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            StatusBadgePill(
                                label = "TIER ${rep.tier.level}",
                                isRedActive = rep.tier.level == 3,
                            )
                        }
                    }
                }
            }
        }

        // Pairing Guide
        item {
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showPairingGuide = !showPairingGuide },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("PAIRING & TROUBLESHOOTING GUIDE", style = MaterialTheme.typography.titleMedium)
                    Icon(
                        if (showPairingGuide) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Toggle pairing guide",
                    )
                }
                if (showPairingGuide) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. Place both earbuds in their charging case with the lid open.\n" +
                            "2. Press & hold the case pairing button for 2–3 seconds until the status LED pulses.\n" +
                            "3. Tap 'Scan Radar' or open System Bluetooth Settings to complete pairing.\n" +
                            "4. Once linked via A2DP/HFP, Hardik Buds Pro detects brand tier, battery telemetry, and activates Subwoofer Engine X.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Connection History
        item {
            SectionHeader(
                title = "Connection History",
                subtitle = "LOCAL ROOM LOGS",
                trailing = { Icon(Icons.Outlined.History, contentDescription = null, tint = extras.signalRed) },
            )
        }
        if (connectionHistory.isEmpty()) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No connection events recorded yet. Connect earbuds to log timestamps, real battery percentage, RSSI, and optional location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(connectionHistory.take(12), key = { it.id }) { item ->
                val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                LuxGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.deviceName.uppercase(), style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${item.brandName.uppercase()} · TIER ${item.tierLevel} · ${fmt.format(Date(item.connectedAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            item.locationLabel?.let { loc ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(13.dp), tint = extras.signalRed)
                                    Spacer(Modifier.width(4.dp))
                                    Text("LOCATION: $loc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        item.batteryAtConnect?.let { bat ->
                            StatusBadgePill(label = "$bat%", isRedActive = bat <= 20)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }

    renameTarget?.let { target ->
        var nameInput by remember(target.address) { mutableStateOf(target.displayTitle) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("RENAME DEVICE") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Updates custom alias locally and requests BluetoothDevice.setAlias on Android 11+.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Device Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_device_input"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameDevice(target, nameInput)
                        renameTarget = null
                    },
                ) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredAudioDevice,
    isSelected: Boolean,
    isPinnedTheatre: Boolean = false,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRename: () -> Unit,
    onPinTheatre: () -> Unit = {},
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(
        modifier = Modifier.fillMaxWidth(),
        highlightRedBorder = device.isConnected || isSelected || isPinnedTheatre,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    RedSignalDot(active = device.isConnected, size = 8.dp)
                    Text(
                        text = device.displayTitle.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPinTheatre,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            Icons.Outlined.SurroundSound,
                            contentDescription = "Set ${device.displayTitle} as Home Theatre",
                            tint = if (isPinnedTheatre) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("rename_device_${device.address}"),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Rename ${device.displayTitle}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Text(
                text = "${device.brand.displayName.uppercase()} · TIER ${device.compatibilityReport.tier.level} · ${device.address}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (device.isConnected) {
                    StatusBadgePill(label = "Connected", isRedActive = true)
                } else if (isSelected) {
                    StatusBadgePill(label = "Selected", isRedActive = false)
                }
                if (device.isFastPairSupported) {
                    StatusBadgePill(label = "Fast Pair", isRedActive = true)
                }
                if (isPinnedTheatre || device.isHomeTheatreSpeaker) {
                    StatusBadgePill(label = "Theatre", isRedActive = isPinnedTheatre)
                }
                device.batteryOverallPercent?.let { bat ->
                    StatusBadgePill(label = "BAT $bat%", isRedActive = bat <= 20)
                }
                device.rssiDbm?.let { rssi ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = extras.signalRed)
                        Spacer(Modifier.width(3.dp))
                        Text("$rssi dBm", style = MaterialTheme.typography.labelSmall)
                    }
                }
                device.firmwareRevision?.let { fw ->
                    StatusBadgePill(label = "FW $fw")
                }
            }

            Spacer(Modifier.height(10.dp))
            if (device.isConnected) {
                LuxActionButton(
                    text = "Disconnect",
                    filled = false,
                    useRedAccent = true,
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LuxActionButton(
                    text = if (device.isBonded) "Connect / Switch" else "Pair Device",
                    icon = Icons.Outlined.BluetoothConnected,
                    useRedAccent = isSelected,
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
