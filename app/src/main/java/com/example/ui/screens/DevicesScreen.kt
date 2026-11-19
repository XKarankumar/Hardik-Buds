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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.data.ConnectionHistoryEntity
import com.example.model.DiscoveredAudioDevice
import com.example.model.FeatureSupportLevel
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionsSnapshot
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DevicesScreen(
    activeDevice: DiscoveredAudioDevice?,
    bondedDevices: List<DiscoveredAudioDevice>,
    nearbyDevices: List<DiscoveredAudioDevice>,
    connectionHistory: List<ConnectionHistoryEntity>,
    isBluetoothEnabled: Boolean,
    isScanning: Boolean,
    permissions: PermissionsSnapshot,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onRequestBtPerms: () -> Unit,
    onConnectOrSelect: (DiscoveredAudioDevice) -> Unit,
    onDisconnect: () -> Unit,
    onFindEarbudsPing: () -> Unit,
) {
    val context = LocalContext.current
    val extras = LocalLuxThemeExtras.current
    var showAllBrandsMatrix by remember { mutableStateOf(false) }
    var showPairingGuide by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("devices_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "Bluetooth & Device Hub",
                subtitle = if (!isBluetoothEnabled) {
                    "Bluetooth adapter is turned off"
                } else if (isScanning) {
                    "Scanning BLE & Classic audio devices..."
                } else {
                    "${bondedDevices.size} paired · ${nearbyDevices.size} nearby discovered"
                },
                trailing = {
                    IconButton(onClick = { PermissionManager.openBluetoothSettings(context) }) {
                        Icon(
                            Icons.Rounded.SettingsBluetooth,
                            contentDescription = "Open System Bluetooth Settings",
                            tint = extras.accent,
                        )
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!permissions.bluetoothReady) {
                    LuxActionButton(
                        text = "Allow Bluetooth",
                        icon = Icons.Rounded.Bluetooth,
                        onClick = onRequestBtPerms,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LuxActionButton(
                        text = if (isScanning) "Stop Scan" else "Scan Nearby",
                        icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
                        onClick = if (isScanning) onStopScan else onStartScan,
                        modifier = Modifier.weight(1f).testTag("scan_devices_button"),
                    )
                }
                LuxActionButton(
                    text = "Find Earbuds",
                    icon = Icons.Rounded.NotificationsActive,
                    filled = false,
                    onClick = onFindEarbudsPing,
                    modifier = Modifier.weight(1f).testTag("find_earbuds_button"),
                )
            }
            if (isScanning) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = extras.accent,
                )
            }
        }

        // Paired Devices Section
        item {
            SectionHeader(
                title = "Paired Earbuds & Audio",
                subtitle = "Real bonded devices from Android BluetoothAdapter",
            )
        }

        if (bondedDevices.isEmpty()) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Headphones, contentDescription = null, tint = extras.accent)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No Paired Audio Devices Found", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (!permissions.bluetoothReady) {
                                    "Grant Bluetooth permissions to list paired earbuds."
                                } else {
                                    "Pair your earbuds in Android Bluetooth Settings or scan nearby BLE devices above."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LuxActionButton(
                        text = "Open System Bluetooth Pairing",
                        icon = Icons.Rounded.SettingsBluetooth,
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
                    onConnect = { onConnectOrSelect(dev) },
                    onDisconnect = onDisconnect,
                )
            }
        }

        // Nearby Scanned Devices Section
        if (nearbyDevices.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Nearby Discovered Devices",
                    subtitle = "Live BLE & Classic scan results with RSSI",
                )
            }
            items(nearbyDevices, key = { "near_${it.address}" }) { dev ->
                DeviceCard(
                    device = dev,
                    isSelected = activeDevice?.address == dev.address,
                    onConnect = { onConnectOrSelect(dev) },
                    onDisconnect = onDisconnect,
                )
            }
        }

        // Active Device Compatibility Breakdown
        if (activeDevice != null) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Compatibility · ${activeDevice.brand.displayName}",
                        subtitle = activeDevice.compatibilityReport.tier.title,
                    )
                    Spacer(Modifier.height(10.dp))
                    activeDevice.compatibilityReport.features.forEach { feat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = when (feat.supportLevel) {
                                    FeatureSupportLevel.SUPPORTED -> Icons.Rounded.CheckCircle
                                    FeatureSupportLevel.ANDROID_DSP_ONLY -> Icons.Rounded.Info
                                    else -> Icons.Rounded.WarningAmber
                                },
                                contentDescription = null,
                                tint = when (feat.supportLevel) {
                                    FeatureSupportLevel.SUPPORTED -> StatusConnected
                                    FeatureSupportLevel.ANDROID_DSP_ONLY -> StatusInfo
                                    else -> StatusWarning
                                },
                                modifier = Modifier.size(18.dp).padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(feat.featureName, style = MaterialTheme.typography.titleSmall)
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
                        Text("17-Brand Compatibility Engine", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "OnePlus, Samsung, AirPods, Realme, Oppo, Xiaomi, Redmi, Vivo, iQOO, Nothing, JBL, Sony, boAt, Noise, Huawei, Pixel Buds & Generic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        if (showAllBrandsMatrix) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
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
                            Text(rep.brand.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            StatusBadgePill(
                                label = "Tier ${rep.tier.level}",
                                color = when (rep.tier.level) {
                                    3 -> StatusConnected
                                    2 -> StatusInfo
                                    else -> extras.secondaryAccent
                                },
                            )
                        }
                    }
                }
            }
        }

        // Pairing Instructions Card
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
                    Text("Pairing & Troubleshooting Guide", style = MaterialTheme.typography.titleMedium)
                    Icon(
                        if (showPairingGuide) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Toggle pairing guide",
                    )
                }
                if (showPairingGuide) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. Place both earbuds in their charging case with the lid open.\n" +
                            "2. Press & hold the case pairing button for 2–3 seconds until the LED pulses white/blue.\n" +
                            "3. Tap 'Scan Nearby' or open System Bluetooth Settings to complete pairing.\n" +
                            "4. Once connected via A2DP/HFP, Hardik Buds Pro automatically detects brand tier, battery telemetry, and Subwoofer Engine X.",
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
                subtitle = "Stored locally in Room database",
                trailing = { Icon(Icons.Rounded.History, contentDescription = null, tint = extras.accent) },
            )
        }
        if (connectionHistory.isEmpty()) {
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No connection events logged yet. Connect earbuds to record session timestamps, battery level, RSSI, and optional location.",
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
                            Text(item.deviceName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${item.brandName} · Tier ${item.tierLevel} · ${fmt.format(Date(item.connectedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            item.locationLabel?.let { loc ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(13.dp), tint = extras.accent)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Last Location: $loc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        item.batteryAtConnect?.let { bat ->
                            StatusBadgePill(label = "$bat%", color = StatusConnected)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredAudioDevice,
    isSelected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = device.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (device.isConnected) {
                        StatusBadgePill(label = "Connected", color = StatusConnected)
                    } else if (isSelected) {
                        StatusBadgePill(label = "Selected", color = extras.accent)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${device.brand.displayName} · ${device.compatibilityReport.tier.title} · ${device.address}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    device.batteryOverallPercent?.let { bat ->
                        StatusBadgePill(label = "Battery $bat%", color = StatusConnected)
                    }
                    device.rssiDbm?.let { rssi ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = extras.accent)
                            Spacer(Modifier.width(4.dp))
                            Text("$rssi dBm", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    device.firmwareRevision?.let { fw ->
                        StatusBadgePill(label = "FW $fw", color = StatusInfo)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (device.isConnected) {
                LuxActionButton(text = "Disconnect", filled = false, onClick = onDisconnect)
            } else {
                LuxActionButton(
                    text = if (device.isBonded) "Connect" else "Pair",
                    icon = Icons.Rounded.BluetoothConnected,
                    onClick = onConnect,
                )
            }
        }
    }
}
