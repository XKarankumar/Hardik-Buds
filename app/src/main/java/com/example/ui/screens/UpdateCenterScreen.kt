package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UpdateHistoryEntity
import com.example.ota.OtaApkInstaller
import com.example.ota.OtaChannel
import com.example.ota.OtaCheckFrequencyHours
import com.example.ota.OtaCheckStatus
import com.example.ota.OtaDownloadState
import com.example.ota.OtaDownloadStatus
import com.example.ota.OtaErrorInfo
import com.example.ota.OtaErrorType
import com.example.ota.OtaSettings
import com.example.ota.OtaUiState
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
fun UpdateCenterScreen(
    otaUiState: OtaUiState,
    otaSettings: OtaSettings,
    downloadState: OtaDownloadState,
    updateHistory: List<UpdateHistoryEntity>,
    onNavigateBack: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onInstallUpdate: () -> OtaErrorInfo?,
    onUpdateOtaSettings: ((OtaSettings) -> OtaSettings) -> Unit,
    onClearHistory: () -> Unit,
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val extras = LocalLuxThemeExtras.current

    var showInstallConfirmDialog by remember { mutableStateOf(false) }
    var githubOwnerInput by remember(otaSettings.githubOwner) { mutableStateOf(otaSettings.githubOwner) }
    var githubRepoInput by remember(otaSettings.githubRepo) { mutableStateOf(otaSettings.githubRepo) }
    var sha256OverrideInput by remember(otaSettings.expectedSha256Override) { mutableStateOf(otaSettings.expectedSha256Override) }

    val activeError = downloadState.error ?: otaUiState.error

    val pulseTransition = rememberInfiniteTransition(label = "otaPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("update_center_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header with Back Arrow
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp).testTag("update_center_back_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "UPDATE CENTER",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "GITHUB RELEASES OTA · HTTPS + SHA-256",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusBadgePill(
                    label = otaSettings.channel.badge,
                    isRedActive = otaSettings.channel == OtaChannel.BETA,
                )
            }
        }

        // 1. Version & Update Status Hero Card
        item {
            val isUpdateAvailable = otaUiState.checkStatus == OtaCheckStatus.UPDATE_AVAILABLE
            val isChecking = otaUiState.checkStatus == OtaCheckStatus.CHECKING
            val latest = otaUiState.latestRelease

            LuxGlassCard(
                modifier = Modifier.fillMaxWidth().testTag("update_center_status_card"),
                highlightRedBorder = isUpdateAvailable || isChecking,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RedSignalDot(
                            active = isUpdateAvailable || isChecking,
                            size = 9.dp,
                        )
                        Text(
                            text = otaUiState.statusHeadline.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    StatusBadgePill(
                        label = when (otaUiState.checkStatus) {
                            OtaCheckStatus.CHECKING -> "CHECKING"
                            OtaCheckStatus.UP_TO_DATE -> "UP TO DATE"
                            OtaCheckStatus.UPDATE_AVAILABLE -> "NEW BUILD"
                            OtaCheckStatus.ERROR -> "ERROR"
                            OtaCheckStatus.IDLE -> "READY"
                        },
                        isRedActive = isUpdateAvailable || otaUiState.checkStatus == OtaCheckStatus.ERROR,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OtaMetricTile(
                        label = "INSTALLED",
                        value = "v${otaUiState.currentVersionName} (#${otaUiState.currentVersionCode})",
                        modifier = Modifier.weight(1f),
                    )
                    OtaMetricTile(
                        label = "LATEST (${otaSettings.channel.badge})",
                        value = latest?.let { "v${it.versionName}" }
                            ?: otaSettings.lastKnownLatestVersion?.let { "v$it" }
                            ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OtaMetricTile(
                        label = "LAST CHECKED",
                        value = formatOtaTimestamp(otaSettings.lastCheckedTimestamp),
                        modifier = Modifier.weight(1f),
                    )
                    OtaMetricTile(
                        label = "UPDATE SIZE",
                        value = latest?.apkSizeBytes?.takeIf { it > 0L }?.let { formatBytes(it) } ?: "N/A",
                        modifier = Modifier.weight(1f),
                    )
                }

                if (isChecking) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = extras.signalRed.copy(alpha = pulseAlpha),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LuxActionButton(
                        text = if (isChecking) "Checking..." else "Check for Updates",
                        icon = Icons.Outlined.Refresh,
                        useRedAccent = !isUpdateAvailable,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCheckForUpdates()
                        },
                        enabled = !isChecking,
                        modifier = Modifier.weight(1f).testTag("check_updates_ota_btn"),
                    )
                    if (isUpdateAvailable && latest?.apkDownloadUrl != null &&
                        (downloadState.status == OtaDownloadStatus.IDLE ||
                            downloadState.status == OtaDownloadStatus.CANCELLED ||
                            downloadState.status == OtaDownloadStatus.FAILED)
                    ) {
                        LuxActionButton(
                            text = "Download Update",
                            icon = Icons.Outlined.CloudDownload,
                            useRedAccent = true,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStartDownload()
                            },
                            modifier = Modifier.weight(1f).testTag("download_update_btn"),
                        )
                    }
                }
            }
        }

        // 2. Active Download, Verification & Explicit Install Card
        if (downloadState.status != OtaDownloadStatus.IDLE && downloadState.status != OtaDownloadStatus.CANCELLED) {
            item {
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("ota_download_card"),
                    highlightRedBorder = downloadState.status == OtaDownloadStatus.DOWNLOADING ||
                        downloadState.status == OtaDownloadStatus.READY_TO_INSTALL,
                ) {
                    SectionHeader(
                        title = when (downloadState.status) {
                            OtaDownloadStatus.DOWNLOADING -> "Downloading APK..."
                            OtaDownloadStatus.PAUSED -> "Download Paused"
                            OtaDownloadStatus.VERIFYING -> "Verifying Package & SHA-256..."
                            OtaDownloadStatus.READY_TO_INSTALL -> "Verified & Ready to Install"
                            OtaDownloadStatus.FAILED -> "Download / Verification Failed"
                            else -> "Update Package"
                        },
                        subtitle = if (downloadState.totalBytes > 0L) {
                            val pct = (downloadState.progressFraction * 100f).toInt().coerceIn(0, 100)
                            val speedText = if (downloadState.speedBytesPerSec > 0L) " · ${formatBytes(downloadState.speedBytesPerSec)}/s" else ""
                            "$pct% · ${formatBytes(downloadState.downloadedBytes)} / ${formatBytes(downloadState.totalBytes)}$speedText"
                        } else {
                            "${formatBytes(downloadState.downloadedBytes)} downloaded"
                        },
                        trailing = {
                            Icon(
                                imageVector = if (downloadState.status == OtaDownloadStatus.READY_TO_INSTALL) {
                                    Icons.Outlined.VerifiedUser
                                } else {
                                    Icons.Outlined.CloudDownload
                                },
                                contentDescription = null,
                                tint = extras.signalRed,
                            )
                        },
                    )

                    Spacer(Modifier.height(10.dp))
                    if (downloadState.status == OtaDownloadStatus.VERIFYING) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = extras.signalRed,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { downloadState.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = extras.signalRed,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        )
                    }

                    // Verification security summary
                    if (downloadState.status == OtaDownloadStatus.READY_TO_INSTALL) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "PACKAGE: ${downloadState.verifiedPackageName} · v${downloadState.verifiedVersionName} (#${downloadState.verifiedVersionCode})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        downloadState.computedSha256?.let { sha ->
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "SHA-256: ${sha.take(28)}... (${if (downloadState.sha256Matched == true) "MATCHED" else "RECORDED"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = extras.signalRed,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Your app data, EQ presets, themes, and paired device aliases remain preserved during update.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (downloadState.status) {
                            OtaDownloadStatus.DOWNLOADING -> {
                                LuxActionButton(
                                    text = "Pause",
                                    icon = Icons.Outlined.Pause,
                                    filled = false,
                                    onClick = onPauseDownload,
                                    modifier = Modifier.weight(1f).testTag("pause_download_btn"),
                                )
                                LuxActionButton(
                                    text = "Cancel",
                                    icon = Icons.Outlined.Cancel,
                                    filled = false,
                                    useRedAccent = true,
                                    onClick = onCancelDownload,
                                    modifier = Modifier.weight(1f).testTag("cancel_download_btn"),
                                )
                            }
                            OtaDownloadStatus.PAUSED -> {
                                LuxActionButton(
                                    text = "Resume",
                                    icon = Icons.Outlined.PlayArrow,
                                    useRedAccent = true,
                                    onClick = onResumeDownload,
                                    modifier = Modifier.weight(1f).testTag("resume_download_btn"),
                                )
                                LuxActionButton(
                                    text = "Cancel",
                                    icon = Icons.Outlined.Cancel,
                                    filled = false,
                                    onClick = onCancelDownload,
                                    modifier = Modifier.weight(1f).testTag("cancel_download_btn"),
                                )
                            }
                            OtaDownloadStatus.FAILED -> {
                                LuxActionButton(
                                    text = "Retry",
                                    icon = Icons.Outlined.Refresh,
                                    useRedAccent = true,
                                    onClick = onRetryDownload,
                                    modifier = Modifier.weight(1f).testTag("retry_download_btn"),
                                )
                                LuxActionButton(
                                    text = "Cancel",
                                    icon = Icons.Outlined.Cancel,
                                    filled = false,
                                    onClick = onCancelDownload,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            OtaDownloadStatus.READY_TO_INSTALL -> {
                                LuxActionButton(
                                    text = "Install Update",
                                    icon = Icons.Outlined.InstallMobile,
                                    useRedAccent = true,
                                    onClick = { showInstallConfirmDialog = true },
                                    modifier = Modifier.weight(1f).testTag("install_update_btn"),
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }

        // 3. Error Handling Card (Hinglish/English + Retry + Settings)
        AnimatedVisibility(visible = activeError != null) {
            activeError?.let { err ->
                LuxGlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("ota_error_card"),
                    highlightRedBorder = true,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = err.type.title,
                            tint = extras.signalRed,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(err.type.title.uppercase(), style = MaterialTheme.typography.titleSmall, color = extras.signalRed)
                            Text(err.type.message, style = MaterialTheme.typography.bodySmall)
                            err.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                                Spacer(Modifier.height(2.dp))
                                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (err.type == OtaErrorType.PERMISSION_DENIED) {
                            LuxActionButton(
                                text = "Open Install Settings",
                                icon = Icons.Outlined.Settings,
                                useRedAccent = true,
                                onClick = {
                                    context.startActivity(OtaApkInstaller.createUnknownAppsSettingsIntent(context))
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (err.type.retryable) {
                            LuxActionButton(
                                text = "Retry",
                                icon = Icons.Outlined.Refresh,
                                useRedAccent = err.type != OtaErrorType.PERMISSION_DENIED,
                                filled = err.type != OtaErrorType.PERMISSION_DENIED,
                                onClick = {
                                    if (downloadState.status == OtaDownloadStatus.FAILED) onRetryDownload()
                                    else onCheckForUpdates()
                                },
                                modifier = Modifier.weight(1f).testTag("ota_error_retry_btn"),
                            )
                        }
                    }
                }
            }
        }

        // 4. What's New / Release Notes
        otaUiState.latestRelease?.let { release ->
            item {
                LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("ota_release_notes_card")) {
                    SectionHeader(
                        title = "What's New · ${release.releaseTitle}",
                        subtitle = "${release.tagName} · PUBLISHED ${release.publishedAt} · ${release.apkAssetName ?: "NO APK"}",
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = release.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    release.expectedSha256?.let { sha ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "RELEASE SHA-256: $sha",
                            style = MaterialTheme.typography.labelSmall,
                            color = extras.signalRed,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LuxActionButton(
                        text = "Open Release on GitHub",
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        filled = false,
                        onClick = { OtaApkInstaller.openReleaseInBrowser(context, release.htmlUrl) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 5. Auto Check, Channel, Notifications & GitHub Source Config
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("ota_config_card")) {
                SectionHeader(
                    title = "OTA Channels & GitHub Source",
                    subtitle = "HTTPS ONLY · WORKMANAGER PERIODIC CHECK · SECURITY VERIFIED",
                    trailing = { Icon(Icons.Outlined.Security, contentDescription = null, tint = extras.signalRed) },
                )
                Spacer(Modifier.height(10.dp))
                Text("RELEASE CHANNEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            modifier = Modifier.testTag("ota_channel_${ch.name}"),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OtaSwitchRow("Check Automatically on Launch", otaSettings.autoCheckOnLaunch) { v ->
                    onUpdateOtaSettings { it.copy(autoCheckOnLaunch = v) }
                }
                OtaSwitchRow("Background Check (WorkManager)", otaSettings.backgroundCheckEnabled) { v ->
                    onUpdateOtaSettings { it.copy(backgroundCheckEnabled = v) }
                }
                OtaSwitchRow("Update Notifications", otaSettings.notificationsEnabled) { v ->
                    onUpdateOtaSettings { it.copy(notificationsEnabled = v) }
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = githubOwnerInput,
                        onValueChange = { githubOwnerInput = it },
                        label = { Text("GitHub Owner") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = githubRepoInput,
                        onValueChange = { githubRepoInput = it },
                        label = { Text("Repo Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sha256OverrideInput,
                    onValueChange = { sha256OverrideInput = it },
                    label = { Text("Optional Expected SHA-256 (64 hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LuxActionButton(
                    text = "Save GitHub Config & Check",
                    icon = Icons.Outlined.CheckCircle,
                    filled = false,
                    onClick = {
                        onUpdateOtaSettings {
                            it.copy(
                                githubOwner = githubOwnerInput.trim().ifBlank { "XKarankumar" },
                                githubRepo = githubRepoInput.trim().ifBlank { "Hardik-Buds" },
                                expectedSha256Override = sha256OverrideInput.trim().lowercase(),
                            )
                        }
                        onCheckForUpdates()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 6. Update History Card
        item {
            LuxGlassCard(modifier = Modifier.fillMaxWidth().testTag("ota_history_card")) {
                SectionHeader(
                    title = "Update History",
                    subtitle = "${updateHistory.size} RECORDED CHECKS & RELEASES",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = onClearHistory, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear update history", tint = extras.signalRed)
                            }
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (updateHistory.isEmpty()) {
                    Text(
                        text = "No update history recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        updateHistory.take(10).forEach { item ->
                            LuxGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "v${item.versionName} · ${item.releaseDate}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatusBadgePill(
                                        label = item.status,
                                        isRedActive = item.status == "AVAILABLE" || item.status == "FAILED",
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.changelog,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    // Explicit User Confirmation Dialog before launching Android Package Installer
    if (showInstallConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmDialog = false },
            title = { Text("INSTALL VERIFIED UPDATE") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Package ${downloadState.verifiedPackageName ?: ""} v${downloadState.verifiedVersionName ?: ""} passed integrity checks.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = if (otaUiState.canInstallUnknownApps) {
                            "Android Package Installer will ask for your final confirmation. All earbuds profiles, themes, and EQ presets stay safe."
                        } else {
                            "Permission needed: Enable 'Allow from this source' in Android Install Unknown Apps settings, then tap Install again."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                if (otaUiState.canInstallUnknownApps) {
                    TextButton(
                        onClick = {
                            showInstallConfirmDialog = false
                            onInstallUpdate()
                        },
                    ) {
                        Text("LAUNCH INSTALLER", color = extras.signalRed)
                    }
                } else {
                    TextButton(
                        onClick = {
                            showInstallConfirmDialog = false
                            context.startActivity(OtaApkInstaller.createUnknownAppsSettingsIntent(context))
                        },
                    ) {
                        Text("OPEN PERMISSION SETTINGS", color = extras.signalRed)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallConfirmDialog = false }) {
                    Text("CANCEL")
                }
            },
        )
    }
}

@Composable
private fun OtaMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val extras = LocalLuxThemeExtras.current
    LuxGlassCard(modifier = modifier, contentPadding = PaddingValues(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = extras.dotMatrixFont),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OtaSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val extras = LocalLuxThemeExtras.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatOtaTimestamp(ts: Long?): String {
    if (ts == null || ts <= 0L) return "NEVER"
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts))
}
