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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.SecurityAuditLogEntity
import com.example.model.DiscoveredAudioDevice
import com.example.model.FirmwareUpdateState
import com.example.model.OwnerSecuritySettings
import com.example.model.SmartSettings
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionsSnapshot
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalLuxThemeExtras
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    activeDevice: DiscoveredAudioDevice?,
    smartSettings: SmartSettings,
    ownerSecurity: OwnerSecuritySettings,
    securityLogs: List<SecurityAuditLogEntity>,
    permissions: PermissionsSnapshot,
    firmwareState: FirmwareUpdateState,
    onUpdateSmartSettings: ((SmartSettings) -> SmartSettings) -> Unit,
    onRegisterOwner: (String, String, String, Int, String, String, Boolean) -> Unit,
    onUpdateOwnerFlags: ((OwnerSecuritySettings) -> OwnerSecuritySettings) -> Unit,
    onRecoverPin: (String, String) -> Boolean,
    onRequestPermissions: (Array<String>) -> Unit,
    onCheckFirmwareAndAppUpdates: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = "System, Security & Updates",
                subtitle = "Owner protection, smart audio, permissions & firmware center",
                trailing = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = extras.accent) },
            )
        }

        // FIRMWARE & APP UPDATE SYSTEM
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Firmware & App Update Center",
                    subtitle = "App Build v${firmwareState.currentAppVersion} · Play-Compliant Verified Channel",
                    trailing = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = extras.accent) },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = firmwareState.verificationStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                firmwareState.catalogEntry?.let { entry ->
                    StatusBadgePill(
                        label = "Brand Channel: ${entry.targetBrand.displayName} · FW ${firmwareState.connectedDeviceFirmware ?: "BLE 0x2A26 Ready"}",
                        color = StatusInfo,
                    )
                    Spacer(Modifier.height(8.dp))
                    entry.changelog.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "SHA-256 Integrity: ${entry.sha256Checksum.take(24)}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.accent,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = "Check Updates",
                        icon = Icons.Rounded.SystemUpdate,
                        onClick = onCheckFirmwareAndAppUpdates,
                        modifier = Modifier.weight(1f).testTag("check_updates_button"),
                    )
                    firmwareState.catalogEntry?.let { entry ->
                        LuxActionButton(
                            text = "OEM Support",
                            icon = Icons.Rounded.OpenInNew,
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

        // OWNER SECURITY SYSTEM
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Owner Security & App Lock",
                    subtitle = if (ownerSecurity.isOwnerRegistered) {
                        "Registered Owner: ${ownerSecurity.ownerName}"
                    } else {
                        "Protect app settings & bind owner device identity"
                    },
                    trailing = {
                        Icon(
                            Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (ownerSecurity.pinEnabled) StatusConnected else extras.accent,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (ownerSecurity.registeredDeviceName != null) {
                    StatusBadgePill(
                        label = "Bound Device: ${ownerSecurity.registeredDeviceName} (${ownerSecurity.registeredDeviceAddress})",
                        color = StatusConnected,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = "Protects this companion app & owner preferences locally via SHA-256 salted PIN storage. Note: Standard Bluetooth earbuds do not support phone-side hardware pairing lock unless built into OEM firmware.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = if (ownerSecurity.isOwnerRegistered) "Update Owner / PIN" else "Register & Set PIN",
                        icon = Icons.Rounded.VerifiedUser,
                        onClick = { showOwnerSetupDialog = true },
                        modifier = Modifier.weight(1f).testTag("owner_register_button"),
                    )
                    if (ownerSecurity.pinEnabled && ownerSecurity.recoveryQuestion.isNotBlank()) {
                        LuxActionButton(
                            text = "PIN Recovery",
                            icon = Icons.Rounded.Lock,
                            filled = false,
                            onClick = { showPinRecoveryDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (ownerSecurity.pinEnabled) {
                    Spacer(Modifier.height(10.dp))
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
                    Text("Recent Security Events", style = MaterialTheme.typography.labelMedium, color = extras.accent)
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

        // SMART FEATURES
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Smart Features & Hearing Safety")
                Spacer(Modifier.height(8.dp))
                SettingsSwitchRow("Auto Reconnect Management", smartSettings.autoReconnect) { v ->
                    onUpdateSmartSettings { it.copy(autoReconnect = v) }
                }
                SettingsSwitchRow("Auto Connect Last Device", smartSettings.autoConnectLastDevice) { v ->
                    onUpdateSmartSettings { it.copy(autoConnectLastDevice = v) }
                }
                SettingsSwitchRow("Connection & Battery Notifications", smartSettings.connectionNotifications) { v ->
                    onUpdateSmartSettings { it.copy(connectionNotifications = v) }
                }
                SettingsSwitchRow("Background Connection Monitor", smartSettings.backgroundMonitoringEnabled) { v ->
                    onUpdateSmartSettings { it.copy(backgroundMonitoringEnabled = v) }
                }
                SettingsSwitchRow("Gaming Low-Latency Hint", smartSettings.gamingLowLatencyHint) { v ->
                    onUpdateSmartSettings { it.copy(gamingLowLatencyHint = v) }
                }
                SettingsSwitchRow("Hearing Protection & Volume Alert", smartSettings.hearingProtectionEnabled) { v ->
                    onUpdateSmartSettings { it.copy(hearingProtectionEnabled = v) }
                }
                SettingsSwitchRow("Record Last Connected Location", smartSettings.recordLastLocation) { v ->
                    onUpdateSmartSettings { it.copy(recordLastLocation = v) }
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Low Battery Alert Threshold", style = MaterialTheme.typography.bodySmall)
                    Text("${smartSettings.batteryAlertThreshold}%", style = MaterialTheme.typography.labelMedium, color = extras.accent)
                }
                Slider(
                    value = smartSettings.batteryAlertThreshold.toFloat(),
                    onValueChange = { v -> onUpdateSmartSettings { it.copy(batteryAlertThreshold = v.roundToInt()) } },
                    valueRange = 5f..40f,
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hearing Volume Warning", style = MaterialTheme.typography.bodySmall)
                    Text("${smartSettings.hearingWarningPercent}%", style = MaterialTheme.typography.labelMedium, color = extras.accent)
                }
                Slider(
                    value = smartSettings.hearingWarningPercent.toFloat(),
                    onValueChange = { v -> onUpdateSmartSettings { it.copy(hearingWarningPercent = v.roundToInt()) } },
                    valueRange = 50f..95f,
                )
            }
        }

        // PERMISSIONS CENTER
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Permissions & Privacy Controls",
                    subtitle = "Transparent explanation, recovery & Android settings link",
                    trailing = { Icon(Icons.Rounded.Security, contentDescription = null, tint = extras.accent) },
                )
                Spacer(Modifier.height(10.dp))
                permissions.items.filter { it.isApplicableOnOs }.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (item.isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = if (item.isGranted) StatusConnected else StatusWarning,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(item.type.title, style = MaterialTheme.typography.titleSmall)
                            }
                            if (!item.isGranted && item.manifestPermissions.isNotEmpty()) {
                                TextButton(onClick = { onRequestPermissions(item.manifestPermissions.toTypedArray()) }) {
                                    Text("Grant")
                                }
                            } else if (item.isGranted) {
                                StatusBadgePill(label = "Granted", color = StatusConnected)
                            }
                        }
                        Text(
                            text = item.type.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!item.isGranted) {
                            Text(
                                text = "Recovery: ${item.type.recoveryInstruction}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusWarning,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LuxActionButton(
                    text = "Open Android App Settings",
                    icon = Icons.Rounded.Shield,
                    filled = false,
                    onClick = { PermissionManager.openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // BACKUP, RESTORE & RESET
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = "Backup, Restore & Reset")
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = "Backup JSON",
                        icon = Icons.Rounded.Backup,
                        filled = false,
                        onClick = { showBackupExportDialog = onExportFullBackup() },
                        modifier = Modifier.weight(1f),
                    )
                    LuxActionButton(
                        text = "Restore",
                        icon = Icons.Rounded.Restore,
                        filled = false,
                        onClick = { showBackupRestoreDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LuxActionButton(
                    text = "Reset All App Settings",
                    icon = Icons.Rounded.DeleteForever,
                    filled = false,
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // DEVELOPER BRANDING & COPYRIGHT
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = extras.accent)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HARDIK BUDS PRO — ULTRA PREMIUM",
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
                            color = extras.accent,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
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
            title = { Text("Owner Registration & PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true)
                    OutlinedTextField(value = ownerContact, onValueChange = { ownerContact = it }, label = { Text("Contact / Note") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = pinLength == 4, onClick = { pinLength = 4 }, label = { Text("4-Digit PIN") })
                        FilterChip(selected = pinLength == 6, onClick = { pinLength = 6 }, label = { Text("6-Digit PIN") })
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
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showOwnerSetupDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showPinRecoveryDialog) {
        var answer by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinRecoveryDialog = false },
            title = { Text("PIN Recovery") },
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
                }) { Text("Recover") }
            },
            dismissButton = { TextButton(onClick = { showPinRecoveryDialog = false }) { Text("Cancel") } },
        )
    }

    showBackupExportDialog?.let { json ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showBackupExportDialog = null },
            title = { Text("Full Backup JSON") },
            text = { Text(json.take(900) + if (json.length > 900) "\n..." else "", style = MaterialTheme.typography.labelSmall) },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(json))
                    onShowToast("Full backup JSON copied to clipboard")
                    showBackupExportDialog = null
                }) { Text("Copy Backup") }
            },
            dismissButton = { TextButton(onClick = { showBackupExportDialog = null }) { Text("Close") } },
        )
    }

    if (showBackupRestoreDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text("Restore Backup JSON") },
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
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { showBackupRestoreDialog = false }) { Text("Cancel") } },
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This clears themes, subwoofer settings, custom EQ profiles, connection history, and owner PIN from local storage.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAllSettings()
                    showResetConfirmDialog = false
                }) { Text("Reset", color = StatusCritical) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
