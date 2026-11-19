package com.example.ui.navigation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.model.IconStyleOption
import com.example.permissions.PermissionManager
import com.example.ui.components.DotMatrixLogoMark
import com.example.ui.components.LuxActionButton
import com.example.ui.components.LuxGlassCard
import com.example.ui.components.StatusBadgePill
import com.example.ui.screens.CustomizeScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SoundScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UpdateCenterScreen
import com.example.ui.theme.HardikBudsProTheme
import com.example.ui.theme.LocalLuxThemeExtras
import com.example.viewmodel.BudsViewModel

object BudsRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_SOUND = "sound"
    const val ROUTE_DEVICES = "devices"
    const val ROUTE_CUSTOMIZE = "customize"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_UPDATE_CENTER = "update_center"
}

private data class NavItem(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val testTag: String,
)

@Composable
fun HardikBudsProRoot(
    viewModel: BudsViewModel,
    skipSplash: Boolean = false,
    openUpdateCenterInitially: Boolean = false,
) {
    val customization by viewModel.customization.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val ownerSecurity by viewModel.ownerSecurity.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val bluetoothBanner by viewModel.bluetoothBanner.collectAsStateWithLifecycle()
    var showSplash by remember(skipSplash) { mutableStateOf(!skipSplash) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(bluetoothBanner) {
        bluetoothBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.bluetoothController.clearBanner()
        }
    }

    HardikBudsProTheme(customization = customization) {
        val extras = LocalLuxThemeExtras.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(extras.backgroundBrush),
        ) {
            if (customization.customWallpaperUri != null) {
                AsyncImage(
                    model = customization.customWallpaperUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.22f),
                )
            }

            AnimatedContent(
                targetState = showSplash,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "splashGate",
            ) { splashing ->
                if (splashing) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    AnimatedContent(
                        targetState = isUnlocked || !ownerSecurity.pinEnabled,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "lockGate",
                    ) { unlocked ->
                        if (!unlocked) {
                            OwnerPinLockOverlay(
                                ownerName = ownerSecurity.ownerName,
                                pinLength = ownerSecurity.pinLength,
                                recoveryQuestion = ownerSecurity.recoveryQuestion,
                                onVerifyPin = { viewModel.verifyPin(it) },
                                onRecoverPin = { ans, newPin -> viewModel.recoverPinWithAnswer(ans, newPin) },
                            )
                        } else {
                            MainAdaptiveShell(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                openUpdateCenterInitially = openUpdateCenterInitially,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainAdaptiveShell(
    viewModel: BudsViewModel,
    snackbarHostState: SnackbarHostState,
    openUpdateCenterInitially: Boolean = false,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: BudsRoutes.ROUTE_HOME
    var soundInitialTab by remember { mutableIntStateOf(0) }

    val customization by viewModel.customization.collectAsStateWithLifecycle()
    val activeDevice by viewModel.activeDevice.collectAsStateWithLifecycle()
    val bondedDevices by viewModel.bondedDevices.collectAsStateWithLifecycle()
    val nearbyDevices by viewModel.nearbyDevices.collectAsStateWithLifecycle()
    val isBtEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val isBtSupported by viewModel.isBluetoothSupported.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionsSnapshot.collectAsStateWithLifecycle()
    val subwoofer by viewModel.subwooferConfig.collectAsStateWithLifecycle()
    val eqProfiles by viewModel.eqProfiles.collectAsStateWithLifecycle()
    val activeEqProfile by viewModel.activeEqProfile.collectAsStateWithLifecycle()
    val soundMode by viewModel.activeSoundMode.collectAsStateWithLifecycle()
    val ancMode by viewModel.preferredAncMode.collectAsStateWithLifecycle()
    val touchConfig by viewModel.touchConfig.collectAsStateWithLifecycle()
    val audioCaps by viewModel.audioCapabilities.collectAsStateWithLifecycle()
    val bassEnergy by viewModel.bassEnergy.collectAsStateWithLifecycle()
    val volumePct by viewModel.volumePercent.collectAsStateWithLifecycle()
    val soundTestRunning by viewModel.soundTestRunning.collectAsStateWithLifecycle()
    val smartSettings by viewModel.smartSettings.collectAsStateWithLifecycle()
    val ownerSecurity by viewModel.ownerSecurity.collectAsStateWithLifecycle()
    val connectionHistory by viewModel.connectionHistory.collectAsStateWithLifecycle()
    val securityLogs by viewModel.securityLogs.collectAsStateWithLifecycle()
    val firmwareState by viewModel.firmwareState.collectAsStateWithLifecycle()
    val fastPairDevices by viewModel.fastPairDevices.collectAsStateWithLifecycle()
    val isFastPairScanning by viewModel.isFastPairScanning.collectAsStateWithLifecycle()
    val audioOutputRoutes by viewModel.audioOutputRoutes.collectAsStateWithLifecycle()
    val homeTheatre by viewModel.homeTheatreSettings.collectAsStateWithLifecycle()
    val smoothRampProgress by viewModel.smoothRampProgress.collectAsStateWithLifecycle()
    val otaUiState by viewModel.otaUiState.collectAsStateWithLifecycle()
    val otaSettings by viewModel.otaSettings.collectAsStateWithLifecycle()
    val otaDownloadState by viewModel.otaDownloadState.collectAsStateWithLifecycle()
    val updateHistory by viewModel.updateHistory.collectAsStateWithLifecycle()

    LaunchedEffect(openUpdateCenterInitially) {
        if (openUpdateCenterInitially) {
            navController.navigate(BudsRoutes.ROUTE_UPDATE_CENTER) {
                launchSingleTop = true
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { viewModel.refreshPermissions() },
    )

    val companionFastPairLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                viewModel.handleCompanionDeviceResult(res.data)
            }
        },
    )

    val navItems = remember {
        listOf(
            NavItem(BudsRoutes.ROUTE_HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
            NavItem(BudsRoutes.ROUTE_SOUND, "Sound", Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "nav_sound"),
            NavItem(BudsRoutes.ROUTE_DEVICES, "Devices", Icons.Rounded.Headphones, Icons.Outlined.Headphones, "nav_devices"),
            NavItem(BudsRoutes.ROUTE_CUSTOMIZE, "Customize", Icons.Rounded.Palette, Icons.Outlined.Palette, "nav_customize"),
            NavItem(BudsRoutes.ROUTE_SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings"),
        )
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val extras = LocalLuxThemeExtras.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp,
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                val iconVec = when (customization.iconStyle) {
                                    IconStyleOption.OUTLINED -> item.outlinedIcon
                                    IconStyleOption.FILLED, IconStyleOption.DUOTONE -> if (selected) item.filledIcon else item.outlinedIcon
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = { Icon(imageVector = iconVec, contentDescription = item.label) },
                                    label = if (customization.showBottomNavLabels) {
                                        { Text(item.label.uppercase(), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                                    } else null,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = extras.signalRed,
                                        selectedTextColor = extras.signalRed,
                                        indicatorColor = extras.signalRed.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(Modifier.height(12.dp))
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            val iconVec = when (customization.iconStyle) {
                                IconStyleOption.OUTLINED -> item.outlinedIcon
                                IconStyleOption.FILLED, IconStyleOption.DUOTONE -> if (selected) item.filledIcon else item.outlinedIcon
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = { Icon(iconVec, contentDescription = item.label) },
                                label = if (customization.showBottomNavLabels) {
                                    { Text(item.label.uppercase(), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                                } else null,
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = extras.signalRed,
                                    selectedTextColor = extras.signalRed,
                                    indicatorColor = extras.signalRed.copy(alpha = 0.14f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = BudsRoutes.ROUTE_HOME,
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                    ) {
                        composable(BudsRoutes.ROUTE_HOME) {
                            HomeScreen(
                                activeDevice = activeDevice,
                                bondedDevices = bondedDevices,
                                isBluetoothEnabled = isBtEnabled,
                                isBluetoothSupported = isBtSupported,
                                permissions = permissions,
                                customization = customization,
                                subwoofer = subwoofer,
                                soundMode = soundMode,
                                ancMode = ancMode,
                                audioCapabilities = audioCaps,
                                bassEnergy = bassEnergy,
                                volumePercent = volumePct,
                                smartSettings = smartSettings,
                                ownerSecurity = ownerSecurity,
                                firmwareState = firmwareState,
                                fastPairCount = fastPairDevices.size,
                                isFastPairScanning = isFastPairScanning,
                                homeTheatre = homeTheatre,
                                smoothRampProgress = smoothRampProgress,
                                onRequestBluetoothPerms = {
                                    val perms = buildList {
                                        addAll(PermissionManager.requiredBluetoothPermissions())
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }.toTypedArray()
                                    permLauncher.launch(perms)
                                },
                                onConnectOrSwitch = { viewModel.connectOrSelectDevice(it) },
                                onDisconnect = { viewModel.disconnectActiveDevice() },
                                onStartGoogleFastPair = { viewModel.startGoogleFastPairScan() },
                                onOpenFastPairSheet = {
                                    viewModel.requestCompanionFastPair { sender ->
                                        companionFastPairLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                    }
                                },
                                onSmoothConnectHomeTheatre = { viewModel.smoothConnectHomeTheatre() },
                                onDisconnectHomeTheatre = { viewModel.disconnectHomeTheatreSmooth() },
                                onNavigateToDevices = { navigateTo(BudsRoutes.ROUTE_DEVICES) },
                                onNavigateToSoundTab = { tabIdx ->
                                    soundInitialTab = tabIdx.coerceIn(0, 3)
                                    navigateTo(BudsRoutes.ROUTE_SOUND)
                                },
                                onNavigateToSettings = { navigateTo(BudsRoutes.ROUTE_SETTINGS) },
                                onSelectSoundMode = { viewModel.selectSoundMode(it) },
                                onSelectAncMode = { viewModel.selectAncMode(it) },
                                onToggleSubwoofer = { enabled -> viewModel.updateSubwoofer { it.copy(enabled = enabled) } },
                                onChangeBassIntensity = { v -> viewModel.updateSubwoofer { it.copy(bassIntensity = v, presetName = "Custom") } },
                                onToggleGamingMode = { enabled -> viewModel.toggleGamingMode(enabled) },
                                onCheckUpdates = { navController.navigate(BudsRoutes.ROUTE_UPDATE_CENTER) },
                                onLockNow = { viewModel.lockAppNow() },
                                onSelectUiMode = { mode ->
                                    viewModel.updateCustomization { it.copy(uiMode = mode) }
                                },
                                otaStatusLabel = otaUiState.statusHeadline,
                                otaUpdateAvailable = otaUiState.checkStatus == com.example.ota.OtaCheckStatus.UPDATE_AVAILABLE,
                            )
                        }

                        composable(BudsRoutes.ROUTE_SOUND) {
                            SoundScreen(
                                activeDevice = activeDevice,
                                subwoofer = subwoofer,
                                eqProfiles = eqProfiles,
                                activeEqProfile = activeEqProfile,
                                activeSoundMode = soundMode,
                                preferredAncMode = ancMode,
                                touchConfig = touchConfig,
                                capabilities = audioCaps,
                                smartSettings = smartSettings,
                                bassEnergy = bassEnergy,
                                soundTestRunning = soundTestRunning,
                                micPermissionGranted = permissions.microphoneGranted,
                                initialTab = soundInitialTab,
                                onRequestMicPermission = {
                                    permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                },
                                onUpdateSubwoofer = { viewModel.updateSubwoofer(it) },
                                onResetSubwoofer = { viewModel.resetSubwooferToDefaults() },
                                onSelectEqPreset = { viewModel.selectEqPreset(it) },
                                onUpdateEqBand = { idx, db -> viewModel.updateActiveEqBand(idx, db) },
                                onSaveCustomEq = { viewModel.saveCustomEqProfile(it) },
                                onRenameCustomEq = { id, newName -> viewModel.renameCustomEqProfile(id, newName) },
                                onDeleteCustomEq = { viewModel.deleteCustomEqProfile(it) },
                                onExportEqJson = { viewModel.exportActiveEqJson() },
                                onImportEqJson = { viewModel.importEqJson(it) },
                                onSelectSoundMode = { viewModel.selectSoundMode(it) },
                                onSelectAncMode = { viewModel.selectAncMode(it) },
                                onToggleGamingMode = { enabled -> viewModel.toggleGamingMode(enabled) },
                                onUpdateTouchConfig = { viewModel.updateTouchConfig(it) },
                                onRunSoundTest = { viewModel.runSoundTest(it) },
                                onStopSoundTest = { viewModel.stopSoundTest() },
                                onShowToast = { viewModel.showToast(it) },
                            )
                        }

                        composable(BudsRoutes.ROUTE_DEVICES) {
                            DevicesScreen(
                                activeDevice = activeDevice,
                                bondedDevices = bondedDevices,
                                nearbyDevices = nearbyDevices,
                                fastPairDevices = fastPairDevices,
                                audioOutputRoutes = audioOutputRoutes,
                                connectionHistory = connectionHistory,
                                isBluetoothEnabled = isBtEnabled,
                                isScanning = isScanning,
                                isFastPairScanning = isFastPairScanning,
                                homeTheatre = homeTheatre,
                                smoothRampProgress = smoothRampProgress,
                                permissions = permissions,
                                onStartScan = { viewModel.startScan() },
                                onStopScan = { viewModel.stopScan() },
                                onStartGoogleFastPairScan = { viewModel.startGoogleFastPairScan() },
                                onLaunchCompanionFastPair = {
                                    viewModel.requestCompanionFastPair { sender ->
                                        companionFastPairLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                    }
                                },
                                onRequestBtPerms = {
                                    permLauncher.launch(PermissionManager.requiredBluetoothPermissions())
                                },
                                onConnectOrSelect = { viewModel.connectOrSelectDevice(it) },
                                onDisconnect = { viewModel.disconnectActiveDevice() },
                                onRenameDevice = { dev, newName -> viewModel.renameDevice(dev, newName) },
                                onPinHomeTheatreDevice = { dev -> viewModel.pinHomeTheatreDevice(dev) },
                                onUpdateHomeTheatre = { viewModel.updateHomeTheatreSettings(it) },
                                onSmoothConnectHomeTheatre = { dev, route -> viewModel.smoothConnectHomeTheatre(dev, route) },
                                onDisconnectHomeTheatre = { viewModel.disconnectHomeTheatreSmooth() },
                                onFindEarbudsPing = { viewModel.triggerFindEarbudsPing() },
                            )
                        }

                        composable(BudsRoutes.ROUTE_CUSTOMIZE) {
                            CustomizeScreen(
                                customization = customization,
                                onUpdateCustomization = { viewModel.updateCustomization(it) },
                            )
                        }

                        composable(BudsRoutes.ROUTE_SETTINGS) {
                            SettingsScreen(
                                activeDevice = activeDevice,
                                smartSettings = smartSettings,
                                customization = customization,
                                ownerSecurity = ownerSecurity,
                                securityLogs = securityLogs,
                                permissions = permissions,
                                firmwareState = firmwareState,
                                otaUiState = otaUiState,
                                otaSettings = otaSettings,
                                updateHistory = updateHistory,
                                onUpdateSmartSettings = { viewModel.updateSmartSettings(it) },
                                onUpdateCustomization = { viewModel.updateCustomization(it) },
                                onNavigateToCustomize = { navigateTo(BudsRoutes.ROUTE_CUSTOMIZE) },
                                onRegisterOwner = { name, contact, pin, len, q, a, bind ->
                                    viewModel.registerOwnerAndPin(name, contact, pin, len, q, a, bind)
                                },
                                onUpdateOwnerFlags = { viewModel.updateOwnerSecurityFlags(it) },
                                onRecoverPin = { ans, newPin -> viewModel.recoverPinWithAnswer(ans, newPin) },
                                onClearHistory = { viewModel.clearConnectionHistory() },
                                onClearSecurityLogs = { viewModel.clearSecurityLogs() },
                                onRequestPermissions = { perms -> permLauncher.launch(perms) },
                                onCheckFirmwareAndAppUpdates = { viewModel.checkFirmwareAndAppUpdates() },
                                onCheckOtaUpdates = { viewModel.checkForAppUpdates(userInitiated = true) },
                                onOpenUpdateCenter = {
                                    navController.navigate(BudsRoutes.ROUTE_UPDATE_CENTER) {
                                        launchSingleTop = true
                                    }
                                },
                                onUpdateOtaSettings = { viewModel.updateOtaSettings(it) },
                                onExportFullBackup = { viewModel.exportFullBackup() },
                                onImportFullBackup = { viewModel.importFullBackup(it) },
                                onResetAllSettings = { viewModel.resetAllSettings() },
                                onShowToast = { viewModel.showToast(it) },
                            )
                        }

                        composable(BudsRoutes.ROUTE_UPDATE_CENTER) {
                            UpdateCenterScreen(
                                otaUiState = otaUiState,
                                otaSettings = otaSettings,
                                downloadState = otaDownloadState,
                                updateHistory = updateHistory,
                                onNavigateBack = { navController.popBackStack() },
                                onCheckForUpdates = { viewModel.checkForAppUpdates(userInitiated = true) },
                                onStartDownload = { viewModel.startOtaDownload() },
                                onPauseDownload = { viewModel.pauseOtaDownload() },
                                onResumeDownload = { viewModel.resumeOtaDownload() },
                                onCancelDownload = { viewModel.cancelOtaDownload() },
                                onRetryDownload = { viewModel.retryOtaDownload() },
                                onInstallUpdate = { viewModel.installVerifiedUpdate() },
                                onUpdateOtaSettings = { viewModel.updateOtaSettings(it) },
                                onClearHistory = { viewModel.clearOtaHistory() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerPinLockOverlay(
    ownerName: String,
    pinLength: Int,
    recoveryQuestion: String,
    onVerifyPin: (String) -> Boolean,
    onRecoverPin: (String, String) -> Boolean,
) {
    val extras = LocalLuxThemeExtras.current
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var recoveryMode by remember { mutableStateOf(false) }
    var answerInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }

    BackHandler(enabled = recoveryMode) {
        recoveryMode = false
        errorMsg = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LuxGlassCard(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            highlightRedBorder = errorMsg != null,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DotMatrixLogoMark(dotSize = 4.dp, animatePulse = true)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = extras.signalRed, modifier = Modifier.size(20.dp))
                    Text(
                        text = "HARDIK BUDS PRO",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(Modifier.height(4.dp))
                StatusBadgePill(
                    label = if (ownerName.isNotBlank()) "OWNER LOCK · ${ownerName.uppercase()}" else "$pinLength-DIGIT SECURITY PIN",
                    isRedActive = true,
                )
                Spacer(Modifier.height(16.dp))

                if (!recoveryMode) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= pinLength && it.all(Char::isDigit)) {
                                pinInput = it
                                errorMsg = null
                            }
                        },
                        label = { Text("$pinLength-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("lock_pin_input"),
                    )
                    errorMsg?.let { err ->
                        Spacer(Modifier.height(6.dp))
                        Text(err, color = extras.signalRed, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(14.dp))
                    LuxActionButton(
                        text = "Unlock App",
                        useRedAccent = true,
                        onClick = {
                            if (!onVerifyPin(pinInput)) {
                                errorMsg = "Incorrect PIN. Please try again."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("unlock_submit_button"),
                    )
                    if (recoveryQuestion.isNotBlank()) {
                        TextButton(onClick = { recoveryMode = true }) {
                            Text("FORGOT PIN? RECOVER", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("Recovery Question: $recoveryQuestion", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        label = { Text("Your Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= pinLength && it.all(Char::isDigit)) newPinInput = it },
                        label = { Text("New $pinLength-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    errorMsg?.let { err ->
                        Spacer(Modifier.height(6.dp))
                        Text(err, color = extras.signalRed, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LuxActionButton(text = "Back", filled = false, onClick = { recoveryMode = false }, modifier = Modifier.weight(1f))
                        LuxActionButton(
                            text = "Reset PIN",
                            useRedAccent = true,
                            onClick = {
                                if (newPinInput.length != pinLength || !onRecoverPin(answerInput, newPinInput)) {
                                    errorMsg = "Incorrect recovery answer or invalid PIN"
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
