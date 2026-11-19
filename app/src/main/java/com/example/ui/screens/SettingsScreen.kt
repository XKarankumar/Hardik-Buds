package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.SecurityAuditLogEntity
import com.example.data.UpdateHistoryEntity
import com.example.model.AnimationSpeedOption
import com.example.model.AppLanguageOption
import com.example.model.CustomizationSettings
import com.example.model.DiscoveredAudioDevice
import com.example.model.DotMatrixStyle
import com.example.model.FirmwareUpdateState
import com.example.model.OwnerSecuritySettings
import com.example.model.SmartSettings
import com.example.ota.OtaChannel
import com.example.ota.OtaCheckFrequencyHours
import com.example.ota.OtaCheckStatus
import com.example.ota.OtaSettings
import com.example.ota.OtaUiState
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionsSnapshot
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.RedSignalDot
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    activeDevice: DiscoveredAudioDevice?,
    smartSettings: SmartSettings,
    customization: CustomizationSettings,
    ownerSecurity: OwnerSecuritySettings,
    securityLogs: List<SecurityAuditLogEntity>,
    permissions: PermissionsSnapshot,
    firmwareState: FirmwareUpdateState,
    otaUiState: OtaUiState = OtaUiState(),
    otaSettings: OtaSettings = OtaSettings(),
    updateHistory: List<UpdateHistoryEntity> = emptyList(),
    onUpdateSmartSettings: ((SmartSettings) -> SmartSettings) -> Unit,
    onUpdateCustomization: ((CustomizationSettings) -> CustomizationSettings) -> Unit,
    onNavigateToCustomize: () -> Unit,
    onRegisterOwner: (String, String, String, Int, String, String, Boolean) -> Unit,
    onUpdateOwnerFlags: ((OwnerSecuritySettings) -> OwnerSecuritySettings) -> Unit,
    onRecoverPin: (String, String) -> Boolean,
    onClearHistory: () -> Unit,
    onClearSecurityLogs: () -> Unit,
    onRequestPermissions: (Array<String>) -> Unit,
    onCheckFirmwareAndAppUpdates: () -> Unit,
    onCheckOtaUpdates: () -> Unit = {},
    onOpenUpdateCenter: () -> Unit = {},
    onUpdateOtaSettings: ((OtaSettings) -> OtaSettings) -> Unit = {},
    onExportFullBackup: () -> String,
    onImportFullBackup: (String) -> Boolean,
    onResetAllSettings: () -> Unit,
    onShowToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val extras = LocalLuxThemeExtras.current

    var showOwnerSetupDialog by remember { mutableStateOf(false) }
    var showPinRecoveryDialog by remember { mutableStateOf(false) }
    var showBackupExportDialog by remember { mutableStateOf<String?>(null) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = "System Settings",
                subtitle = "BLUETOOTH · AUDIO · NOTIFICATIONS · PRIVACY · SECURITY · UPDATES",
                trailing = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = extras.signalRed) },
            )
        }

        // 1. BLUETOOTH & BATTERY SETTINGS
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Bluetooth & Battery",
                    subtitle = "RECONNECT ENGINE · BACKGROUND MONITOR · THRESHOLDS",
                    trailing = { Icon(Icons.Outlined.Bluetooth, contentDescription = null, tint = extras.signalRed) },
                )
                Spacer(Modifier.height(8.dp))
                SettingsSwitchRow("Auto Reconnect Management", smartSettings.autoReconnect) { v ->
                    onUpdateSmartSettings { it.copy(autoReconnect = v) }
                }
                SettingsSwitchRow("Auto Connect Last Device", smartSettings.autoConnectLastDevice) { v ->
                    onUpdateSmartSettings { it.copy(autoConnectLastDevice = v) }
                }
                SettingsSwitchRow("Background Connection Monitor", smartSettings.backgroundMonitoringEnabled) { v ->
                    onUpdateSmartSettings { it.copy(backgroundMonitoringEnabled = v) }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BatteryStd, contentDescription = null, modifier = Modifier.size(16.dp), tint = extras.signalRed)
                        Spacer(Modifier.width(6.dp))
                        Text("Low Battery Alert Threshold", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${smartSettings.batteryAlertThreshold}%", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                }
                Slider(
                    value = smartSettings.batteryAlertThreshold.toFloat(),
                    onValueChange = { v -> onUpdateSmartSettings { it.copy(batteryAlertThreshold = v.roundToInt()) } },
                    valueRange = 5f..40f,
                    colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                )
            }
        }

        // 2. AUDIO & HEARING PROTECTION
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Audio & Hearing Safety",
                    subtitle = "LOW-LATENCY HINT · VOLUME PROTECTION",
                    trailing = { Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                )
                Spacer(Modifier.height(8.dp))
                SettingsSwitchRow("Gaming Low-Latency Routing Hint", smartSettings.gamingLowLatencyHint) { v ->
                    onUpdateSmartSettings { it.copy(gamingLowLatencyHint = v) }
                }
                SettingsSwitchRow("Hearing Protection & Volume Alert", smartSettings.hearingProtectionEnabled) { v ->
                    onUpdateSmartSettings { it.copy(hearingProtectionEnabled = v) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hearing Volume Warning", style = MaterialTheme.typography.bodySmall)
                    Text("${smartSettings.hearingWarningPercent}%", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                }
                Slider(
                    value = smartSettings.hearingWarningPercent.toFloat(),
                    onValueChange = { v -> onUpdateSmartSettings { it.copy(hearingWarningPercent = v.roundToInt()) } },
                    valueRange = 50f..95f,
                    colors = SliderDefaults.colors(thumbColor = extras.signalRed, activeTrackColor = extras.signalRed),
                )
            }
        }

        // 3. MINIMAL BLACK + RED DOT NOTIFICATIONS
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Notifications",
                    subtitle = "MINIMAL BLACK CARDS · SIGNAL RED ALERT DOT",
                    trailing = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = extras.signalRed) },
                )
                Spacer(Modifier.height(8.dp))
                SettingsSwitchRow("Connection & Battery Alerts", smartSettings.connectionNotifications) { v ->
                    onUpdateSmartSettings { it.copy(connectionNotifications = v) }
                }
                SettingsSwitchRow("Disconnect Alert Notifications", smartSettings.disconnectAlertNotifications) { v ->
                    onUpdateSmartSettings { it.copy(disconnectAlertNotifications = v) }
                }
                Spacer(Modifier.height(8.dp))
                // Minimal Notification Style Preview
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    highlightRedBorder = true,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RedSignalDot(active = true, size = 8.dp)
                        Text(
                            text = "${(activeDevice?.displayTitle ?: "HARDIK BUDS PRO").uppercase()} · ALERT PREVIEW",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Minimal system shade notification with Signal Red (#FF3038) indicator dot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 4. OWNER SECURITY & APP LOCK
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth(), highlightRedBorder = ownerSecurity.pinEnabled) {
                SectionHeader(
                    title = "Owner Security & App Lock",
                    subtitle = if (ownerSecurity.isOwnerRegistered) {
                        "REGISTERED OWNER: ${ownerSecurity.ownerName.uppercase()}"
                    } else {
                        "APP PIN LOCK · OWNER DEVICE BINDING"
                    },
                    trailing = {
                        Icon(
                            Icons.Outlined.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (ownerSecurity.pinEnabled) extras.signalRed else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (ownerSecurity.registeredDeviceName != null) {
                    StatusBadgePill(
                        label = "BOUND: ${ownerSecurity.registeredDeviceName} (${ownerSecurity.registeredDeviceAddress})",
                        isRedActive = true,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = "Secures this companion app locally via salted SHA-256 PIN storage. Hardware pairing locks require manufacturer firmware.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = if (ownerSecurity.isOwnerRegistered) "Update Owner / PIN" else "Register & Set PIN",
                        icon = Icons.Outlined.VerifiedUser,
                        useRedAccent = true,
                        onClick = { showOwnerSetupDialog = true },
                        modifier = Modifier.weight(1f).testTag("owner_register_button"),
                    )
                    if (ownerSecurity.pinEnabled && ownerSecurity.recoveryQuestion.isNotBlank()) {
                        LuxActionButton(
                            text = "PIN Recovery",
                            icon = Icons.Outlined.Lock,
                            filled = false,
                            onClick = { showPinRecoveryDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (ownerSecurity.pinEnabled) {
                    Spacer(Modifier.height(8.dp))
                    SettingsSwitchRow("Lock App on Launch", ownerSecurity.lockAppOnLaunch) { v ->
                        onUpdateOwnerFlags { it.copy(lockAppOnLaunch = v) }
                    }
                    SettingsSwitchRow("Lock Sensitive Settings", ownerSecurity.lockSensitiveSettings) { v ->
                        onUpdateOwnerFlags { it.copy(lockSensitiveSettings = v) }
                    }
                    SettingsSwitchRow("Alert on Unrecognized Device", ownerSecurity.alertOnUnauthorizedDevice) { v ->
                        onUpdateOwnerFlags { it.copy(alertOnUnauthorizedDevice = v) }
                    }
                }
                if (securityLogs.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("RECENT SECURITY AUDIT", style = MaterialTheme.typography.labelMedium, color = extras.signalRed)
                    securityLogs.take(3).forEach { log ->
                        Text(
                            text = "• ${log.eventType}: ${log.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 5. APPEARANCE, ANIMATIONS & LANGUAGE
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Appearance, Motion & Language",
                    subtitle = "THEME: ${customization.themeId.displayName.uppercase()} · ${customization.animationSpeed.label.uppercase()}",
                    trailing = { Icon(Icons.Outlined.Language, contentDescription = null, tint = extras.signalRed) },
                )
                Spacer(Modifier.height(8.dp))
                Text("APP LANGUAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppLanguageOption.entries) { lang ->
                        FilterChip(
                            selected = customization.language == lang,
                            onClick = { onUpdateCustomization { it.copy(language = lang) } },
                            label = { Text(lang.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                selectedLabelColor = extras.signalRed,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("DOT MATRIX FONT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DotMatrixStyle.entries) { dm ->
                        FilterChip(
                            selected = customization.dotMatrixStyle == dm,
                            onClick = { onUpdateCustomization { it.copy(dotMatrixStyle = dm) } },
                            label = { Text(dm.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                selectedLabelColor = extras.signalRed,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("ANIMATIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AnimationSpeedOption.entries) { spd ->
                        FilterChip(
                            selected = customization.animationSpeed == spd,
                            onClick = { onUpdateCustomization { it.copy(animationSpeed = spd) } },
                            label = { Text(spd.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                selectedLabelColor = extras.signalRed,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                SettingsSwitchRow("Haptic Feedback", customization.hapticEnabled) { v ->
                    onUpdateCustomization { it.copy(hapticEnabled = v) }
                }
                Spacer(Modifier.height(6.dp))
                LuxActionButton(
                    text = "Open Full Customize Studio",
                    icon = Icons.Outlined.Palette,
                    filled = false,
                    onClick = onNavigateToCustomize,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 6. PRIVACY & PERMISSIONS
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Privacy & Permissions",
                    subtitle = "LOCAL TELEMETRY · ZERO EXTERNAL TRACKERS",
                    trailing = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = extras.signalRed) },
                )
                Spacer(Modifier.height(8.dp))
                SettingsSwitchRow("Record Last Connected Location", smartSettings.recordLastLocation) { v ->
                    onUpdateSmartSettings { it.copy(recordLastLocation = v) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LuxActionButton(
                        text = "Clear History",
                        icon = Icons.Outlined.DeleteSweep,
                        filled = false,
                        onClick = onClearHistory,
                        modifier = Modifier.weight(1f),
                    )
                    LuxActionButton(
                        text = "Clear Security Logs",
                        icon = Icons.Outlined.Security,
                        filled = false,
                        onClick = onClearSecurityLogs,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                permissions.items.filter { it.isApplicableOnOs }.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (item.isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = if (item.isGranted) MaterialTheme.colorScheme.onSurface else extras.signalRed,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(item.type.title.uppercase(), style = MaterialTheme.typography.titleSmall)
                            }
                            if (!item.isGranted && item.manifestPermissions.isNotEmpty()) {
                                TextButton(onClick = { onRequestPermissions(item.manifestPermissions.toTypedArray()) }) {
                                    Text("GRANT", color = extras.signalRed)
                                }
                            } else if (item.isGranted) {
                                StatusBadgePill(label = "GRANTED")
                            }
                        }
                        Text(
                            text = item.type.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                LuxActionButton(
                    text = "Open Android App Settings",
                    icon = Icons.Outlined.Shield,
                    filled = false,
                    onClick = { PermissionManager.openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 7. SOFTWARE UPDATE & EARBUDS FIRMWARE
        item {
            val isUpdateAvailable = otaUiState.checkStatus == OtaCheckStatus.UPDATE_AVAILABLE
            LuxGlassCard(
                modifier = Modifier.fillMaxWidth().testTag("settings_software_update_card"),
                highlightRedBorder = isUpdateAvailable,
            ) {
                SectionHeader(
                    title = "Software Update",
                    subtitle = "CURRENT v${otaUiState.currentVersionName} (#${otaUiState.currentVersionCode}) · ${otaSettings.githubOwner}/${otaSettings.githubRepo}",
                    trailing = {
                        StatusBadgePill(
                            label = if (isUpdateAvailable) "UPDATE READY" else otaSettings.channel.badge,
                            isRedActive = isUpdateAvailable,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = otaUiState.statusHeadline,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUpdateAvailable) extras.signalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                otaUiState.latestRelease?.let { rel ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "What's New (${rel.tagName}): ${rel.releaseNotes.lines().firstOrNull { it.isNotBlank() } ?: rel.releaseTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                SettingsSwitchRow("Auto Check Updates (Launch & Background)", otaSettings.autoCheckOnLaunch && otaSettings.backgroundCheckEnabled) { v ->
                    onUpdateOtaSettings { it.copy(autoCheckOnLaunch = v, backgroundCheckEnabled = v) }
                }
                SettingsSwitchRow("Update Notifications", otaSettings.notificationsEnabled) { v ->
                    onUpdateOtaSettings { it.copy(notificationsEnabled = v) }
                }
                Spacer(Modifier.height(6.dp))
                Text("UPDATE CHANNEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OtaChannel.entries.forEach { ch ->
                        FilterChip(
                            selected = otaSettings.channel == ch,
                            onClick = { onUpdateOtaSettings { it.copy(channel = ch) } },
                            label = { Text(ch.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                selectedLabelColor = extras.signalRed,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("CHECK FREQUENCY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(OtaCheckFrequencyHours.entries) { freq ->
                        FilterChip(
                            selected = otaSettings.checkFrequency == freq,
                            onClick = { onUpdateOtaSettings { it.copy(checkFrequency = freq) } },
                            label = { Text(freq.label.uppercase(), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extras.signalRed.copy(alpha = 0.16f),
                                selectedLabelColor = extras.signalRed,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = "Open Update Center",
                        icon = Icons.Outlined.SystemUpdate,
                        useRedAccent = true,
                        onClick = onOpenUpdateCenter,
                        modifier = Modifier.weight(1f).testTag("open_update_center_button"),
                    )
                    LuxActionButton(
                        text = "Check Now",
                        icon = Icons.Outlined.SystemUpdate,
                        filled = false,
                        onClick = onCheckOtaUpdates,
                        modifier = Modifier.weight(1f).testTag("check_updates_button"),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Update History: ${updateHistory.size} entries · Earbuds BLE 0x2A26: ${firmwareState.connectedDeviceFirmware ?: "Not exposed"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = "Check Earbuds FW",
                        icon = Icons.Outlined.Bluetooth,
                        filled = false,
                        onClick = onCheckFirmwareAndAppUpdates,
                        modifier = Modifier.weight(1f),
                    )
                    firmwareState.catalogEntry?.let { entry ->
                        LuxActionButton(
                            text = "OEM Support",
                            icon = Icons.Outlined.OpenInNew,
                            filled = false,
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.officialGuideUrl)))
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // 8. BACKUP, RESTORE & RESET
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Backup & Reset")
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = "Backup JSON",
                        icon = Icons.Outlined.Backup,
                        filled = false,
                        onClick = { showBackupExportDialog = onExportFullBackup() },
                        modifier = Modifier.weight(1f),
                    )
                    LuxActionButton(
                        text = "Restore",
                        icon = Icons.Outlined.Restore,
                        filled = false,
                        onClick = { showBackupRestoreDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LuxActionButton(
                    text = "Reset All Settings",
                    icon = Icons.Outlined.DeleteForever,
                    filled = false,
                    useRedAccent = true,
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 9. ABOUT & BRANDING
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = extras.signalRed)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HARDIK BUDS PRO · ULTRA PREMIUM",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Developer: Karan Kumar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "© 2026 Hardik Kumar. All Rights Reserved.",
                            style = MaterialTheme.typography.labelSmall,
                            color = extras.signalRed,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }

    if (showOwnerSetupDialog) {
        var ownerName by remember { mutableStateOf(ownerSecurity.ownerName) }
        var ownerContact by remember { mutableStateOf(ownerSecurity.ownerContact) }
        var pin by remember { mutableStateOf("") }
        var pinLength by remember { mutableIntStateOf(ownerSecurity.pinLength) }
        var question by remember { mutableStateOf(ownerSecurity.recoveryQuestion.ifBlank { "Favorite city?" }) }
        var answer by remember { mutableStateOf("") }
        var bindActive by remember { mutableStateOf(activeDevice != null) }

        AlertDialog(
            onDismissRequest = { showOwnerSetupDialog = false },
            title = { Text("OWNER REGISTRATION & PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true)
                    OutlinedTextField(value = ownerContact, onValueChange = { ownerContact = it }, label = { Text("Contact / Note") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = pinLength == 4, onClick = { pinLength = 4 }, label = { Text("4-DIGIT PIN") })
                        FilterChip(selected = pinLength == 6, onClick = { pinLength = 6 }, label = { Text("6-DIGIT PIN") })
                    }
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= pinLength && it.all(Char::isDigit)) pin = it },
                        label = { Text("$pinLength-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Recovery Question") }, singleLine = true)
                    OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Recovery Answer") }, singleLine = true)
                    if (activeDevice != null) {
                        SettingsSwitchRow("Bind ${activeDevice.displayTitle}", bindActive) { bindActive = it }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (ownerName.isBlank() || (pin.isNotBlank() && pin.length != pinLength)) {
                        onShowToast("Enter owner name and a valid $pinLength-digit PIN")
                    } else {
                        onRegisterOwner(ownerName, ownerContact, pin, pinLength, question, answer, bindActive)
                        showOwnerSetupDialog = false
                    }
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showOwnerSetupDialog = false }) { Text("CANCEL") }
            },
        )
    }

    if (showPinRecoveryDialog) {
        var answer by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinRecoveryDialog = false },
            title = { Text("PIN RECOVERY") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Question: ${ownerSecurity.recoveryQuestion}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Recovery Answer") }, singleLine = true)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= ownerSecurity.pinLength && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New ${ownerSecurity.pinLength}-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPin.length == ownerSecurity.pinLength && onRecoverPin(answer, newPin)) {
                        showPinRecoveryDialog = false
                    } else {
                        onShowToast("Incorrect recovery answer or invalid PIN length")
                    }
                }) { Text("RECOVER") }
            },
            dismissButton = { TextButton(onClick = { showPinRecoveryDialog = false }) { Text("CANCEL") } },
        )
    }

    showBackupExportDialog?.let { json ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showBackupExportDialog = null },
            title = { Text("FULL BACKUP JSON") },
            text = { Text(json.take(900) + if (json.length > 900) "\n..." else "", style = MaterialTheme.typography.labelSmall) },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(json))
                    onShowToast("Full backup JSON copied to clipboard")
                    showBackupExportDialog = null
                }) { Text("COPY BACKUP") }
            },
            dismissButton = { TextButton(onClick = { showBackupExportDialog = null }) { Text("CLOSE") } },
        )
    }

    if (showBackupRestoreDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text("RESTORE BACKUP JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste Full Backup JSON") },
                    minLines = 4,
                    maxLines = 8,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onImportFullBackup(jsonInput)) showBackupRestoreDialog = false
                }) { Text("RESTORE") }
            },
            dismissButton = { TextButton(onClick = { showBackupRestoreDialog = false }) { Text("CANCEL") } },
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("RESET ALL SETTINGS?") },
            text = { Text("Clears themes, subwoofer settings, custom EQ profiles, connection history, and owner PIN from local storage.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAllSettings()
                    showResetConfirmDialog = false
                }) { Text("RESET", color = extras.signalRed) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmDialog = false }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun SettingsSwitchRow(
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = extras.signalRed,
            ),
        )
    }
}
