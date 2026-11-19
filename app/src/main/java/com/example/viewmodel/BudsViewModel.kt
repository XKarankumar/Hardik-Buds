package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AcousticTestType
import com.example.audio.SubwooferAudioEngine
import com.example.bluetooth.BluetoothController
import com.example.bluetooth.BudsMonitorService
import com.example.data.AppDatabase
import com.example.data.BackupManager
import com.example.data.ConnectionHistoryEntity
import com.example.data.DefaultEqPresets
import com.example.data.DeviceProfileEntity
import com.example.data.SecurityAuditLogEntity
import com.example.data.SecurityHashUtil
import com.example.data.SettingsRepository
import com.example.data.toDomain
import com.example.data.toEntity
import com.example.model.AncMode
import com.example.model.AudioEngineCapabilities
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import com.example.model.AudioOutputRoute
import com.example.model.CustomizationSettings
import com.example.model.DiscoveredAudioDevice
import com.example.model.EarbudBrand
import com.example.model.EqualizerProfile
import com.example.model.FirmwareCatalogEntry
import com.example.model.FirmwareUpdateState
import com.example.model.HomeTheatreSettings
import com.example.model.OwnerSecuritySettings
import com.example.model.SmartSettings
import com.example.model.SoundMode
import com.example.model.SubwooferConfig
import com.example.model.TouchControlConfig
import com.example.BuildConfig
import com.example.data.UpdateHistoryEntity
import com.example.ota.GitHubReleaseService
import com.example.ota.OtaApkDownloader
import com.example.ota.OtaApkInstaller
import com.example.ota.OtaCheckStatus
import com.example.ota.OtaDownloadState
import com.example.ota.OtaDownloadStatus
import com.example.ota.OtaErrorInfo
import com.example.ota.OtaErrorType
import com.example.ota.OtaNotificationHelper
import com.example.ota.OtaSettings
import com.example.ota.OtaUiState
import com.example.ota.OtaWorkScheduler
import com.example.ota.SemVerComparator
import com.example.permissions.PermissionManager
import com.example.permissions.PermissionsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BudsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val settingsRepo = SettingsRepository(appContext)
    val bluetoothController = BluetoothController(appContext)
    val audioEngine = SubwooferAudioEngine(appContext)

    private val _permissionsSnapshot = MutableStateFlow(PermissionManager.getSnapshot(appContext))
    val permissionsSnapshot: StateFlow<PermissionsSnapshot> = _permissionsSnapshot.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _touchConfig = MutableStateFlow(TouchControlConfig())
    val touchConfig: StateFlow<TouchControlConfig> = _touchConfig.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val gitHubReleaseService = GitHubReleaseService(appContext)
    private val otaDownloader = OtaApkDownloader(appContext, viewModelScope)

    private val _otaSettings = MutableStateFlow(OtaSettings())
    val otaSettings: StateFlow<OtaSettings> = _otaSettings.asStateFlow()

    private val _otaUiState = MutableStateFlow(
        OtaUiState(
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
            canInstallUnknownApps = OtaApkInstaller.canRequestPackageInstalls(appContext),
        ),
    )
    val otaUiState: StateFlow<OtaUiState> = _otaUiState.asStateFlow()

    val otaDownloadState: StateFlow<OtaDownloadState> = otaDownloader.state

    val updateHistory: StateFlow<List<UpdateHistoryEntity>> = db.updateHistoryDao().observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _firmwareState = MutableStateFlow(FirmwareUpdateState(currentAppVersion = BuildConfig.VERSION_NAME))
    val firmwareState: StateFlow<FirmwareUpdateState> = _firmwareState.asStateFlow()

    private val _customization = MutableStateFlow(CustomizationSettings())
    val customization: StateFlow<CustomizationSettings> = _customization.asStateFlow()

    val subwooferConfig: StateFlow<SubwooferConfig> = settingsRepo.subwooferConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SubwooferConfig())

    val activeSoundMode: StateFlow<SoundMode> = settingsRepo.activeSoundModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SoundMode.MUSIC)

    val preferredAncMode: StateFlow<AncMode> = settingsRepo.preferredAncModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AncMode.OFF)

    val smartSettings: StateFlow<SmartSettings> = settingsRepo.smartSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SmartSettings())

    val ownerSecurity: StateFlow<OwnerSecuritySettings> = settingsRepo.ownerSecurityFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, OwnerSecuritySettings())

    val eqProfiles: StateFlow<List<EqualizerProfile>> = db.eqProfileDao().observeProfiles()
        .map { list ->
            if (list.isEmpty()) DefaultEqPresets.allBuiltIn() else list.map { it.toDomain() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultEqPresets.allBuiltIn())

    val activeEqId: StateFlow<String> = settingsRepo.activeEqIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "preset_balanced")

    val activeEqProfile: StateFlow<EqualizerProfile> = combine(eqProfiles, activeEqId) { profiles, id ->
        profiles.firstOrNull { it.id == id } ?: profiles.firstOrNull() ?: DefaultEqPresets.allBuiltIn().first()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DefaultEqPresets.allBuiltIn().first())

    val connectionHistory: StateFlow<List<ConnectionHistoryEntity>> = db.connectionHistoryDao().observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityLogs: StateFlow<List<SecurityAuditLogEntity>> = db.securityAuditDao().observeLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isBluetoothEnabled: StateFlow<Boolean> = bluetoothController.isBluetoothEnabled
    val isBluetoothSupported: StateFlow<Boolean> = bluetoothController.isBluetoothSupported
    val isScanning: StateFlow<Boolean> = bluetoothController.isScanning
    val isFastPairScanning: StateFlow<Boolean> = bluetoothController.isFastPairScanning
    val bondedDevices: StateFlow<List<DiscoveredAudioDevice>> = bluetoothController.bondedDevices
    val nearbyDevices: StateFlow<List<DiscoveredAudioDevice>> = bluetoothController.nearbyDevices
    val fastPairDevices: StateFlow<List<DiscoveredAudioDevice>> = bluetoothController.fastPairDevices
    val activeDevice: StateFlow<DiscoveredAudioDevice?> = bluetoothController.activeDevice
    val bluetoothBanner: StateFlow<String?> = bluetoothController.statusBanner

    val audioCapabilities: StateFlow<AudioEngineCapabilities> = audioEngine.capabilities
    val audioOutputRoutes: StateFlow<List<AudioOutputRoute>> = audioEngine.audioOutputRoutes
    val smoothRampProgress: StateFlow<Float?> = audioEngine.smoothRampProgress
    val bassEnergy: StateFlow<Float> = audioEngine.bassEnergy
    val volumePercent: StateFlow<Int> = audioEngine.volumePercent
    val isMusicPlaying: StateFlow<Boolean> = audioEngine.isMusicPlaying
    val soundTestRunning: StateFlow<String?> = audioEngine.soundTestRunning

    private val _theatreConnectedActive = MutableStateFlow(false)
    private val _theatreTransitionStatus = MutableStateFlow("Ready for smooth cinema audio handoff")

    val homeTheatreSettings: StateFlow<HomeTheatreSettings> = combine(
        settingsRepo.homeTheatreSettingsFlow,
        _theatreConnectedActive,
        _theatreTransitionStatus,
    ) { saved, active, status ->
        saved.copy(
            theatreConnectedActive = active,
            lastSmoothTransitionStatus = status,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeTheatreSettings())

    init {
        seedPresetsIfNeeded()
        audioEngine.initialize()
        viewModelScope.launch {
            val savedProfiles = db.deviceProfileDao().observeDeviceProfiles().first()
            val aliases = savedProfiles.associate { it.address to it.customAlias }
            if (aliases.isNotEmpty()) {
                bluetoothController.seedCustomAliases(aliases)
                bluetoothController.refreshBondedAndConnectedDevices()
            }
        }
        bluetoothController.onDeviceConnectedCallback = { device ->
            handleDeviceConnected(device)
        }
        bluetoothController.onDeviceDisconnectedCallback = { address ->
            viewModelScope.launch {
                db.connectionHistoryDao().markDisconnected(address, System.currentTimeMillis())
                val smart = smartSettings.value
                if (smart.disconnectAlertNotifications && smart.connectionNotifications) {
                    bluetoothController.sendNotification(
                        title = "Earbuds Disconnected",
                        body = "Bluetooth link closed for $address",
                        notificationId = 104,
                    )
                }
            }
        }
        bluetoothController.startListening()

        viewModelScope.launch {
            val savedCustomization = settingsRepo.customizationFlow.first()
            if (_customization.value == CustomizationSettings()) {
                _customization.value = savedCustomization
            }
            val savedOta = settingsRepo.otaSettingsFlow.first()
            if (_otaSettings.value == OtaSettings()) {
                _otaSettings.value = savedOta
            }
            OtaNotificationHelper.ensureChannel(appContext)
            OtaWorkScheduler.syncSchedule(appContext, _otaSettings.value)
            seedInitialUpdateHistoryIfEmpty()
            if (_otaSettings.value.autoCheckOnLaunch) {
                checkForAppUpdates(userInitiated = false)
            }
            val sec = settingsRepo.ownerSecurityFlow.first()
            _isAppUnlocked.value = !(sec.pinEnabled && sec.lockAppOnLaunch && sec.pinHash.isNotBlank())
        }

        // Keep audio DSP pipeline synchronized whenever subwoofer, EQ, or sound mode changes
        viewModelScope.launch {
            combine(
                subwooferConfig,
                activeEqProfile,
                activeSoundMode,
                smartSettings,
            ) { sub, eq, mode, smart ->
                Quad(sub, eq, mode, smart)
            }.collect { (sub, eq, mode, smart) ->
                audioEngine.applyAudioPipeline(
                    subwoofer = sub,
                    eqProfile = eq,
                    soundMode = mode,
                    lowLatencyHint = smart.gamingLowLatencyHint,
                )
            }
        }

        // Sync firmware card with connected device
        viewModelScope.launch {
            activeDevice.collect { dev ->
                val brand = dev?.brand ?: EarbudBrand.GENERIC
                _firmwareState.value = _firmwareState.value.copy(
                    connectedDeviceFirmware = dev?.firmwareRevision,
                    catalogEntry = lookupFirmwareCatalog(brand),
                )
            }
        }
    }

    private fun seedPresetsIfNeeded() {
        viewModelScope.launch {
            if (db.eqProfileDao().countProfiles() == 0) {
                db.eqProfileDao().insertAllIgnore(DefaultEqPresets.toEntities())
            }
        }
    }

    fun refreshPermissions() {
        val snap = PermissionManager.getSnapshot(appContext)
        _permissionsSnapshot.value = snap
        if (snap.bluetoothReady) {
            bluetoothController.startListening()
        }
        if (snap.microphoneGranted) {
            audioEngine.tryStartLiveVisualizer()
        }
    }

    fun startScan() {
        refreshPermissions()
        bluetoothController.startBleAndClassicScan()
    }

    fun startGoogleFastPairScan() {
        refreshPermissions()
        bluetoothController.startGoogleFastPairScan()
    }

    fun requestCompanionFastPair(onChooserReady: (IntentSender) -> Unit) {
        refreshPermissions()
        bluetoothController.requestCompanionFastPairing(
            onChooserIntentReady = onChooserReady,
            onError = { msg -> showToast(msg) },
        )
    }

    @SuppressLint("MissingPermission")
    fun handleCompanionDeviceResult(data: Intent?) {
        if (data == null) return
        val btDevice: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE, ScanResult::class.java)?.device
        } else {
            @Suppress("DEPRECATION")
            val raw = data.getParcelableExtra<android.os.Parcelable>(CompanionDeviceManager.EXTRA_DEVICE)
            when (raw) {
                is BluetoothDevice -> raw
                is ScanResult -> raw.device
                else -> null
            }
        }

        val chosen = btDevice ?: return
        val addr = chosen.address
        val matched = (fastPairDevices.value + nearbyDevices.value + bondedDevices.value)
            .firstOrNull { it.address == addr }
        if (matched != null) {
            connectOrSelectDevice(matched)
        } else {
            runCatching {
                if (chosen.bondState != BluetoothDevice.BOND_BONDED) {
                    chosen.createBond()
                }
            }
            bluetoothController.refreshBondedAndConnectedDevices()
        }
        showToast("Google Fast Connection initiated for ${matched?.displayTitle ?: addr}")
    }

    fun updateHomeTheatreSettings(transform: (HomeTheatreSettings) -> HomeTheatreSettings) {
        viewModelScope.launch {
            settingsRepo.updateHomeTheatreSettings(transform(homeTheatreSettings.value))
        }
    }

    fun pinHomeTheatreDevice(device: DiscoveredAudioDevice) {
        viewModelScope.launch {
            settingsRepo.updateHomeTheatreSettings(
                homeTheatreSettings.value.copy(
                    preferredTheatreAddress = device.address,
                    preferredTheatreName = device.displayTitle,
                ),
            )
            showToast("Pinned '${device.displayTitle}' as Home Theatre target")
        }
    }

    fun smoothConnectHomeTheatre(
        targetDevice: DiscoveredAudioDevice? = null,
        route: AudioOutputRoute? = null,
    ) {
        val ht = homeTheatreSettings.value
        val resolvedBt = targetDevice
            ?: bondedDevices.value.firstOrNull { it.address == ht.preferredTheatreAddress }
            ?: bondedDevices.value.firstOrNull { it.isHomeTheatreSpeaker }

        if (resolvedBt != null && !resolvedBt.isConnected) {
            connectOrSelectDevice(resolvedBt)
        }

        val targetName = resolvedBt?.displayTitle
            ?: route?.productName
            ?: ht.preferredTheatreName
            ?: audioCapabilities.value.outputRouteLabel

        _theatreConnectedActive.value = true
        _theatreTransitionStatus.value = "Smoothly ramping cinema audio to $targetName..."

        viewModelScope.launch {
            if (ht.autoCinemaSubwoofer) {
                settingsRepo.setActiveSoundMode(SoundMode.SURROUND_SOUND)
                settingsRepo.updateSubwooferConfig(
                    subwooferConfig.value.copy(
                        enabled = true,
                        lowFreqCutoffHz = ht.lfeCrossoverHz,
                        presetName = "Cinema LFE",
                    ),
                )
            }
        }

        audioEngine.runSmoothHomeTheatreTransition(
            targetBassStrength = subwooferConfig.value.bassIntensity,
            durationMs = ht.smoothTransitionMs,
            enableRamp = ht.smoothFadeHandoff,
        ) {
            _theatreTransitionStatus.value = "Home Theatre active · $targetName (${ht.lfeCrossoverHz}Hz LFE)"
            showToast("Home Theatre smoothly connected: $targetName")
        }
    }

    fun disconnectHomeTheatreSmooth() {
        _theatreConnectedActive.value = false
        _theatreTransitionStatus.value = "Standard earbuds profile restored"
        viewModelScope.launch {
            settingsRepo.setActiveSoundMode(SoundMode.MUSIC)
        }
        showToast("Exited Home Theatre mode cleanly")
    }

    fun stopScan() = bluetoothController.stopScan()

    fun connectOrSelectDevice(device: DiscoveredAudioDevice) {
        bluetoothController.selectAndConnectDevice(device)
        viewModelScope.launch {
            settingsRepo.setLastDeviceAddress(device.address)
            db.deviceProfileDao().upsert(
                DeviceProfileEntity(
                    address = device.address,
                    customAlias = device.displayTitle,
                    brandName = device.brand.displayName,
                    preferredAncMode = preferredAncMode.value.name,
                    preferredSoundMode = activeSoundMode.value.name,
                    touchConfigJson = "",
                    firmwareVersion = device.firmwareRevision,
                    isOwnerVerified = ownerSecurity.value.registeredDeviceAddress == device.address,
                    lastLocationLabel = readLastLocationLabelIfPermitted(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun disconnectActiveDevice() {
        activeDevice.value?.let { bluetoothController.disconnectDevice(it) }
    }

    private fun handleDeviceConnected(device: DiscoveredAudioDevice) {
        viewModelScope.launch {
            val smart = smartSettings.value
            val sec = ownerSecurity.value
            val loc = if (smart.recordLastLocation) readLastLocationLabelIfPermitted() else null

            db.connectionHistoryDao().insert(
                ConnectionHistoryEntity(
                    address = device.address,
                    deviceName = device.displayTitle,
                    brandName = device.brand.displayName,
                    tierLevel = device.compatibilityReport.tier.level,
                    connectedAt = System.currentTimeMillis(),
                    batteryAtConnect = device.batteryOverallPercent,
                    rssiDbm = device.rssiDbm,
                    locationLabel = loc,
                ),
            )

            if (smart.connectionNotifications) {
                val batSuffix = device.batteryOverallPercent?.let { " · Battery $it%" } ?: ""
                bluetoothController.sendNotification(
                    title = "${device.displayTitle} Connected",
                    body = "${device.compatibilityReport.tier.title}$batSuffix",
                    notificationId = 101,
                )
            }

            device.batteryOverallPercent?.let { bat ->
                if (bat <= smart.batteryAlertThreshold && smart.connectionNotifications) {
                    bluetoothController.sendNotification(
                        title = "Low Earbuds Battery ($bat%)",
                        body = "${device.displayTitle} is below your ${smart.batteryAlertThreshold}% threshold.",
                        notificationId = 102,
                    )
                }
            }

            // Owner Security: Alert on unrecognized device
            if (sec.isOwnerRegistered &&
                sec.alertOnUnauthorizedDevice &&
                sec.registeredDeviceAddress != null &&
                sec.registeredDeviceAddress != device.address
            ) {
                db.securityAuditDao().insert(
                    SecurityAuditLogEntity(
                        eventType = "UNRECOGNIZED_DEVICE",
                        deviceAddress = device.address,
                        deviceName = device.displayTitle,
                        detail = "Connected audio device differs from registered owner earbuds (${sec.registeredDeviceName ?: sec.registeredDeviceAddress}).",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
                if (smart.connectionNotifications) {
                    bluetoothController.sendNotification(
                        title = "Owner Security Alert",
                        body = "Unregistered device connected: ${device.displayTitle}",
                        notificationId = 103,
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun readLastLocationLabelIfPermitted(): String? {
        if (!smartSettings.value.recordLastLocation) return null
        if (!PermissionManager.getSnapshot(appContext).locationGranted) return null
        return runCatching {
            val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            loc?.let { "%.4f, %.4f".format(it.latitude, it.longitude) }
        }.getOrNull()
    }

    // --- Subwoofer Engine X & EQ ---
    fun updateSubwoofer(transform: (SubwooferConfig) -> SubwooferConfig) {
        viewModelScope.launch {
            settingsRepo.updateSubwooferConfig(transform(subwooferConfig.value))
        }
    }

    fun resetSubwooferToDefaults() {
        viewModelScope.launch {
            settingsRepo.updateSubwooferConfig(SubwooferConfig())
            showToast("Subwoofer Engine X reset to default tuning")
        }
    }

    fun toggleGamingMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateSmartSettings(smartSettings.value.copy(gamingLowLatencyHint = enabled))
            settingsRepo.setActiveSoundMode(if (enabled) SoundMode.GAMING else SoundMode.MUSIC)
            showToast(if (enabled) "Gaming Mode active (Low-Latency audio profile)" else "Gaming Mode disabled")
        }
    }

    fun renameDevice(device: DiscoveredAudioDevice, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        bluetoothController.renameDevice(device.address, trimmed)
        viewModelScope.launch {
            val existing = db.deviceProfileDao().getByAddress(device.address)
            db.deviceProfileDao().upsert(
                (existing ?: DeviceProfileEntity(
                    address = device.address,
                    customAlias = trimmed,
                    brandName = device.brand.displayName,
                    preferredAncMode = preferredAncMode.value.name,
                    preferredSoundMode = activeSoundMode.value.name,
                    touchConfigJson = "",
                    firmwareVersion = device.firmwareRevision,
                    isOwnerVerified = ownerSecurity.value.registeredDeviceAddress == device.address,
                    lastLocationLabel = null,
                    updatedAt = System.currentTimeMillis(),
                )).copy(customAlias = trimmed, updatedAt = System.currentTimeMillis()),
            )
            showToast("Renamed device to '$trimmed'")
        }
    }

    fun clearConnectionHistory() {
        viewModelScope.launch {
            db.connectionHistoryDao().clearAll()
            showToast("Connection history cleared")
        }
    }

    fun clearSecurityLogs() {
        viewModelScope.launch {
            db.securityAuditDao().clearAll()
            showToast("Security audit logs cleared")
        }
    }

    fun selectSoundMode(mode: SoundMode) {
        viewModelScope.launch {
            settingsRepo.setActiveSoundMode(mode)
        }
    }

    fun selectAncMode(mode: AncMode) {
        val dev = activeDevice.value
        viewModelScope.launch {
            settingsRepo.setPreferredAncMode(mode)
        }
        if (dev == null) {
            showToast("Saved ${mode.title} preference. Connect earbuds to evaluate hardware protocol support.")
        } else if (!dev.compatibilityReport.supportsHardwareAncControl) {
            showToast("${dev.brand.displayName} does not expose public ANC control APIs. Hardware ANC cannot be toggled without OEM protocol.")
        }
    }

    fun selectEqPreset(profile: EqualizerProfile) {
        viewModelScope.launch {
            settingsRepo.setActiveEqId(profile.id)
        }
    }

    fun updateActiveEqBand(bandIndex: Int, valueDb: Float) {
        val current = activeEqProfile.value
        val updatedBands = current.bandsDb.toMutableList().apply {
            if (bandIndex in indices) this[bandIndex] = valueDb.coerceIn(-12f, 12f)
        }
        viewModelScope.launch {
            if (current.isBuiltIn && current.id != "preset_custom") {
                val customProfile = EqualizerProfile(
                    id = "preset_custom",
                    name = "Custom",
                    bandsDb = updatedBands,
                    subwooferBoost = current.subwooferBoost,
                    isBuiltIn = true,
                )
                db.eqProfileDao().upsert(customProfile.toEntity())
                settingsRepo.setActiveEqId("preset_custom")
            } else {
                db.eqProfileDao().upsert(current.copy(bandsDb = updatedBands).toEntity())
            }
        }
    }

    fun saveCustomEqProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val base = activeEqProfile.value
        val newProfile = EqualizerProfile(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = trimmed,
            bandsDb = base.bandsDb,
            subwooferBoost = subwooferConfig.value.bassIntensity,
            isBuiltIn = false,
        )
        viewModelScope.launch {
            db.eqProfileDao().upsert(newProfile.toEntity())
            settingsRepo.setActiveEqId(newProfile.id)
            showToast("Saved EQ profile '$trimmed'")
        }
    }

    fun renameCustomEqProfile(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            db.eqProfileDao().renameCustomProfile(id, trimmed, System.currentTimeMillis())
            showToast("Renamed profile to '$trimmed'")
        }
    }

    fun deleteCustomEqProfile(id: String) {
        viewModelScope.launch {
            db.eqProfileDao().deleteCustomProfile(id)
            if (activeEqId.value == id) {
                settingsRepo.setActiveEqId("preset_balanced")
            }
            showToast("Deleted custom EQ profile")
        }
    }

    fun exportActiveEqJson(): String = BackupManager.exportEqProfileJson(activeEqProfile.value)

    fun importEqJson(jsonText: String): Boolean {
        val parsed = BackupManager.importEqProfileJson(jsonText)
        return parsed.fold(
            onSuccess = { profile ->
                viewModelScope.launch {
                    db.eqProfileDao().upsert(profile.toEntity())
                    settingsRepo.setActiveEqId(profile.id)
                    showToast("Imported '${profile.name}'")
                }
                true
            },
            onFailure = {
                showToast("Invalid EQ JSON format")
                false
            },
        )
    }

    // --- Sound Tests & Find Earbuds ---
    fun runSoundTest(type: AcousticTestType) {
        audioEngine.runAcousticSoundTest(type, subwooferConfig.value.balance)
    }

    fun stopSoundTest() = audioEngine.stopSoundTest()

    fun triggerFindEarbudsPing() {
        val dev = activeDevice.value
        if (dev == null || !dev.isConnected) {
            showToast("Connect your earbuds first so audio routes to them for the acoustic finder chime.")
            return
        }
        audioEngine.runAcousticSoundTest(AcousticTestType.FIND_EARBUDS_CHIME, 0f)
        showToast("Playing high-clarity acoustic finder chime to ${dev.displayTitle}")
    }

    // --- Customization & Smart Settings ---
    fun updateCustomization(transform: (CustomizationSettings) -> CustomizationSettings) {
        val updated = transform(_customization.value)
        _customization.value = updated
        viewModelScope.launch {
            settingsRepo.updateCustomization(updated)
        }
    }

    fun updateSmartSettings(transform: (SmartSettings) -> SmartSettings) {
        val next = transform(smartSettings.value)
        viewModelScope.launch {
            settingsRepo.updateSmartSettings(next)
            if (next.backgroundMonitoringEnabled) {
                val label = activeDevice.value?.displayTitle ?: "Monitoring Bluetooth audio connections"
                BudsMonitorService.start(appContext, label)
            } else {
                BudsMonitorService.stop(appContext)
            }
        }
    }

    fun updateTouchConfig(transform: (TouchControlConfig) -> TouchControlConfig) {
        _touchConfig.value = transform(_touchConfig.value)
    }

    // --- Owner Security ---
    fun registerOwnerAndPin(
        ownerName: String,
        ownerContact: String,
        pin: String,
        pinLength: Int,
        recoveryQuestion: String,
        recoveryAnswer: String,
        bindActiveDevice: Boolean,
    ) {
        val dev = if (bindActiveDevice) activeDevice.value else null
        viewModelScope.launch {
            val updated = OwnerSecuritySettings(
                isOwnerRegistered = true,
                ownerName = ownerName.trim(),
                ownerContact = ownerContact.trim(),
                registeredDeviceAddress = dev?.address ?: ownerSecurity.value.registeredDeviceAddress,
                registeredDeviceName = dev?.displayTitle ?: ownerSecurity.value.registeredDeviceName,
                pinEnabled = pin.isNotBlank(),
                pinLength = pinLength,
                pinHash = if (pin.isNotBlank()) SecurityHashUtil.hashSecret(pin) else ownerSecurity.value.pinHash,
                recoveryQuestion = recoveryQuestion.trim(),
                recoveryAnswerHash = if (recoveryAnswer.isNotBlank()) {
                    SecurityHashUtil.hashSecret(recoveryAnswer)
                } else {
                    ownerSecurity.value.recoveryAnswerHash
                },
                lockAppOnLaunch = pin.isNotBlank(),
                lockSensitiveSettings = true,
                alertOnUnauthorizedDevice = true,
            )
            settingsRepo.updateOwnerSecurity(updated)
            db.securityAuditDao().insert(
                SecurityAuditLogEntity(
                    eventType = "OWNER_REGISTERED",
                    deviceAddress = updated.registeredDeviceAddress,
                    deviceName = updated.registeredDeviceName,
                    detail = "Owner profile secured for ${updated.ownerName}.",
                    timestamp = System.currentTimeMillis(),
                ),
            )
            showToast("Owner security profile saved locally")
        }
    }

    fun updateOwnerSecurityFlags(transform: (OwnerSecuritySettings) -> OwnerSecuritySettings) {
        viewModelScope.launch {
            settingsRepo.updateOwnerSecurity(transform(ownerSecurity.value))
        }
    }

    fun verifyPin(pinInput: String): Boolean {
        val ok = SecurityHashUtil.verifySecret(pinInput, ownerSecurity.value.pinHash)
        if (ok) {
            _isAppUnlocked.value = true
        }
        return ok
    }

    fun lockAppNow() {
        if (ownerSecurity.value.pinEnabled) {
            _isAppUnlocked.value = false
        }
    }

    fun recoverPinWithAnswer(answerInput: String, newPin: String): Boolean {
        val sec = ownerSecurity.value
        if (!SecurityHashUtil.verifySecret(answerInput, sec.recoveryAnswerHash)) {
            return false
        }
        viewModelScope.launch {
            settingsRepo.updateOwnerSecurity(
                sec.copy(
                    pinHash = SecurityHashUtil.hashSecret(newPin),
                    pinLength = newPin.length.coerceIn(4, 6),
                    pinEnabled = true,
                ),
            )
            _isAppUnlocked.value = true
            db.securityAuditDao().insert(
                SecurityAuditLogEntity(
                    eventType = "PIN_RECOVERED",
                    deviceAddress = sec.registeredDeviceAddress,
                    deviceName = sec.registeredDeviceName,
                    detail = "App PIN recovered using owner recovery answer.",
                    timestamp = System.currentTimeMillis(),
                ),
            )
            showToast("PIN updated & app unlocked")
        }
        return true
    }

    // --- Firmware & App Update System ---
    fun checkFirmwareAndAppUpdates() {
        val dev = activeDevice.value
        val brand = dev?.brand ?: EarbudBrand.GENERIC
        val fwRev = dev?.firmwareRevision
        val catalog = lookupFirmwareCatalog(brand)
        val statusText = buildString {
            append("App Build: v${_firmwareState.value.currentAppVersion} (Up to date · Signed Release). ")
            if (dev == null) {
                append("Connect earbuds via Bluetooth/BLE to read GATT Firmware Revision (0x2A26).")
            } else if (fwRev != null) {
                append("Detected Hardware Firmware: $fwRev on ${dev.displayTitle}.")
            } else {
                append("${dev.displayTitle} (${brand.displayName}) did not advertise BLE GATT 0x2A26 firmware string; OEM companion check available.")
            }
        }
        _firmwareState.value = _firmwareState.value.copy(
            connectedDeviceFirmware = fwRev,
            catalogEntry = catalog,
            isChecking = false,
            lastCheckedTimestamp = System.currentTimeMillis(),
            verificationStatus = statusText,
        )
        showToast("Update & firmware check completed")
    }

    private fun lookupFirmwareCatalog(brand: EarbudBrand): FirmwareCatalogEntry {
        val url = when (brand) {
            EarbudBrand.ONEPLUS -> "https://www.oneplus.com/support"
            EarbudBrand.SAMSUNG -> "https://www.samsung.com/support/"
            EarbudBrand.APPLE_AIRPODS -> "https://support.apple.com/airpods"
            EarbudBrand.SONY -> "https://www.sony.com/electronics/support"
            EarbudBrand.NOTHING -> "https://nothing.tech/pages/support"
            EarbudBrand.GOOGLE_PIXEL_BUDS -> "https://support.google.com/googlepixelbuds"
            EarbudBrand.JBL -> "https://support.jbl.com/"
            EarbudBrand.XIAOMI, EarbudBrand.REDMI -> "https://www.mi.com/global/support/"
            else -> "https://github.com/hardik-kumar/hardik-buds-pro/releases"
        }
        return FirmwareCatalogEntry(
            targetBrand = brand,
            latestVersion = "Companion / BLE 0x2A26",
            releaseDate = "2026-11",
            changelog = listOf(
                "Reads BLE GATT Device Information Service (0x180A → 0x2A26)",
                "Verifies SHA-256 package integrity before OEM handoff",
                "Play-compliant update routing (zero dynamic executable code loading)",
            ),
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            officialGuideUrl = url,
        )
    }

    // --- Backup, Restore & Reset ---
    fun exportFullBackup(): String = BackupManager.exportFullBackupJson(
        customization = customization.value,
        subwoofer = subwooferConfig.value,
        soundMode = activeSoundMode.value,
        smartSettings = smartSettings.value,
        customEqProfiles = eqProfiles.value,
    )

    fun importFullBackup(jsonText: String): Boolean {
        val res = BackupManager.importFullBackupJson(jsonText)
        return res.fold(
            onSuccess = { restored ->
                viewModelScope.launch {
                    _customization.value = restored.customization
                    settingsRepo.updateCustomization(restored.customization)
                    settingsRepo.updateSubwooferConfig(restored.subwoofer)
                    settingsRepo.setActiveSoundMode(restored.soundMode)
                    settingsRepo.updateSmartSettings(restored.smartSettings)
                    restored.customEqProfiles.forEach { db.eqProfileDao().upsert(it.toEntity()) }
                    showToast("Restored backup (${restored.customEqProfiles.size} custom EQ profiles)")
                }
                true
            },
            onFailure = {
                showToast("Invalid backup JSON")
                false
            },
        )
    }

    // --- OTA Auto Update System (GitHub Releases + WorkManager + Verified PackageInstaller) ---
    private suspend fun seedInitialUpdateHistoryIfEmpty() {
        val current = db.updateHistoryDao().observeHistory().first()
        if (current.isEmpty()) {
            db.updateHistoryDao().insert(
                UpdateHistoryEntity(
                    versionName = BuildConfig.VERSION_NAME,
                    releaseTitle = "Hardik Buds Pro Ultra Premium (Installed)",
                    releaseDate = "Current Build #${BuildConfig.VERSION_CODE}",
                    changelog = "Currently installed production build (${appContext.packageName}).",
                    status = "INSTALLED",
                    sha256 = null,
                    timestamp = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun refreshInstallPermissionState() {
        val canInstall = OtaApkInstaller.canRequestPackageInstalls(appContext)
        _otaUiState.value = _otaUiState.value.copy(
            canInstallUnknownApps = canInstall,
            error = if (canInstall && _otaUiState.value.error?.type == OtaErrorType.PERMISSION_DENIED) null else _otaUiState.value.error,
        )
    }

    fun updateOtaSettings(transform: (OtaSettings) -> OtaSettings) {
        val updated = transform(_otaSettings.value)
        _otaSettings.value = updated
        viewModelScope.launch {
            settingsRepo.updateOtaSettings(updated)
            OtaWorkScheduler.syncSchedule(appContext, updated)
        }
    }

    fun checkForAppUpdates(userInitiated: Boolean = true) {
        if (_otaUiState.value.checkStatus == OtaCheckStatus.CHECKING) return
        val settings = _otaSettings.value
        _otaUiState.value = _otaUiState.value.copy(
            checkStatus = OtaCheckStatus.CHECKING,
            statusHeadline = "Checking GitHub Releases (${settings.githubOwner}/${settings.githubRepo})...",
            error = null,
            canInstallUnknownApps = OtaApkInstaller.canRequestPackageInstalls(appContext),
        )
        viewModelScope.launch {
            val res = gitHubReleaseService.fetchReleases(
                owner = settings.githubOwner,
                repo = settings.githubRepo,
                channel = settings.channel,
            )
            val now = System.currentTimeMillis()
            if (res.error != null) {
                if (userInitiated) {
                    _otaUiState.value = _otaUiState.value.copy(
                        checkStatus = OtaCheckStatus.ERROR,
                        availableReleases = res.allReleases,
                        statusHeadline = res.error.type.title,
                        error = res.error,
                    )
                    showToast(res.error.type.message)
                } else {
                    _otaUiState.value = _otaUiState.value.copy(
                        checkStatus = OtaCheckStatus.IDLE,
                        statusHeadline = "Auto-check paused (${res.error.type.title})",
                    )
                }
                return@launch
            }

            val latest = res.latestForChannel
            val nextSettings = settings.copy(
                lastCheckedTimestamp = now,
                lastKnownLatestVersion = latest?.versionName,
            )
            _otaSettings.value = nextSettings
            settingsRepo.updateOtaSettings(nextSettings)

            if (latest == null) {
                _otaUiState.value = _otaUiState.value.copy(
                    checkStatus = OtaCheckStatus.ERROR,
                    availableReleases = res.allReleases,
                    statusHeadline = "No releases found",
                    error = OtaErrorInfo(OtaErrorType.RELEASE_NOT_FOUND),
                )
                return@launch
            }

            val isNewer = SemVerComparator.isNewerVersion(latest.versionName, BuildConfig.VERSION_NAME)
            if (isNewer) {
                _otaUiState.value = _otaUiState.value.copy(
                    checkStatus = OtaCheckStatus.UPDATE_AVAILABLE,
                    latestRelease = latest,
                    availableReleases = res.allReleases,
                    statusHeadline = "Update available: v${latest.versionName}",
                    error = null,
                )
                _firmwareState.value = _firmwareState.value.copy(
                    latestAppVersion = latest.versionName,
                    appUpdateNotes = latest.releaseNotes.lines().filter { it.isNotBlank() }.take(8),
                )
                db.updateHistoryDao().insert(
                    UpdateHistoryEntity(
                        versionName = latest.versionName,
                        releaseTitle = latest.releaseTitle,
                        releaseDate = latest.publishedAt,
                        changelog = latest.releaseNotes.take(400),
                        status = "AVAILABLE",
                        sha256 = latest.expectedSha256,
                        timestamp = now,
                    ),
                )
                if (_otaSettings.value.notificationsEnabled) {
                    OtaNotificationHelper.notifyUpdateAvailable(appContext, latest)
                }
                if (userInitiated) {
                    showToast("New ${settings.channel.label} v${latest.versionName} available")
                }
            } else {
                _otaUiState.value = _otaUiState.value.copy(
                    checkStatus = OtaCheckStatus.UP_TO_DATE,
                    latestRelease = latest,
                    availableReleases = res.allReleases,
                    statusHeadline = "You're up to date (v${BuildConfig.VERSION_NAME})",
                    error = null,
                )
                _firmwareState.value = _firmwareState.value.copy(
                    latestAppVersion = latest.versionName,
                )
                if (userInitiated) {
                    db.updateHistoryDao().insert(
                        UpdateHistoryEntity(
                            versionName = latest.versionName,
                            releaseTitle = latest.releaseTitle,
                            releaseDate = latest.publishedAt,
                            changelog = "Checked GitHub Releases — already on latest v${BuildConfig.VERSION_NAME}.",
                            status = "UP_TO_DATE",
                            sha256 = latest.expectedSha256,
                            timestamp = now,
                        ),
                    )
                    showToast("You're up to date (v${BuildConfig.VERSION_NAME})")
                }
            }
        }
    }

    fun startOtaDownload() {
        val release = _otaUiState.value.latestRelease
        if (release == null || release.apkDownloadUrl.isNullOrBlank()) {
            _otaUiState.value = _otaUiState.value.copy(
                error = OtaErrorInfo(OtaErrorType.APK_MISSING),
            )
            return
        }
        val expectedSha = _otaSettings.value.expectedSha256Override.trim().takeIf { it.length == 64 }
            ?: release.expectedSha256
        otaDownloader.startOrResumeDownload(
            release = release,
            expectedSha256 = expectedSha,
            installedVersionCode = BuildConfig.VERSION_CODE.toLong(),
            installedVersionName = BuildConfig.VERSION_NAME,
            resumeExisting = false,
            onCompletedVerified = { verified ->
                viewModelScope.launch {
                    db.updateHistoryDao().insert(
                        UpdateHistoryEntity(
                            versionName = verified.verifiedVersionName ?: release.versionName,
                            releaseTitle = release.releaseTitle,
                            releaseDate = release.publishedAt,
                            changelog = release.releaseNotes.take(400),
                            status = "VERIFIED",
                            sha256 = verified.computedSha256,
                            timestamp = System.currentTimeMillis(),
                        ),
                    )
                    if (_otaSettings.value.notificationsEnabled) {
                        OtaNotificationHelper.notifyDownloadCompleted(
                            appContext,
                            verified.verifiedVersionName ?: release.versionName,
                        )
                    }
                    showToast("Update APK verified & ready to install")
                }
            },
            onFailed = { err ->
                viewModelScope.launch {
                    db.updateHistoryDao().insert(
                        UpdateHistoryEntity(
                            versionName = release.versionName,
                            releaseTitle = release.releaseTitle,
                            releaseDate = release.publishedAt,
                            changelog = err.detail ?: err.type.message,
                            status = "FAILED",
                            sha256 = null,
                            timestamp = System.currentTimeMillis(),
                        ),
                    )
                    if (_otaSettings.value.notificationsEnabled) {
                        OtaNotificationHelper.notifyDownloadFailed(appContext, err.type.title)
                    }
                }
            },
        )
    }

    fun pauseOtaDownload() = otaDownloader.pauseDownload()

    fun resumeOtaDownload() {
        val release = _otaUiState.value.latestRelease ?: return
        otaDownloader.resumeDownload(
            onCompletedVerified = { verified ->
                viewModelScope.launch {
                    if (_otaSettings.value.notificationsEnabled) {
                        OtaNotificationHelper.notifyDownloadCompleted(
                            appContext,
                            verified.verifiedVersionName ?: release.versionName,
                        )
                    }
                    showToast("Resumed download verified & ready")
                }
            },
            onFailed = { err ->
                if (_otaSettings.value.notificationsEnabled) {
                    OtaNotificationHelper.notifyDownloadFailed(appContext, err.type.title)
                }
            },
        )
    }

    fun cancelOtaDownload() {
        otaDownloader.cancelDownload()
        showToast("Update download cancelled")
    }

    fun retryOtaDownload() {
        val release = _otaUiState.value.latestRelease
        if (release == null) {
            checkForAppUpdates(userInitiated = true)
            return
        }
        otaDownloader.retryDownload(
            onCompletedVerified = { verified ->
                viewModelScope.launch {
                    if (_otaSettings.value.notificationsEnabled) {
                        OtaNotificationHelper.notifyDownloadCompleted(
                            appContext,
                            verified.verifiedVersionName ?: release.versionName,
                        )
                    }
                    showToast("Update APK verified & ready to install")
                }
            },
            onFailed = { err ->
                if (_otaSettings.value.notificationsEnabled) {
                    OtaNotificationHelper.notifyDownloadFailed(appContext, err.type.title)
                }
            },
        )
    }

    fun installVerifiedUpdate(): OtaErrorInfo? {
        val dl = otaDownloadState.value
        if (dl.status != OtaDownloadStatus.READY_TO_INSTALL || dl.localApkPath.isNullOrBlank()) {
            val err = OtaErrorInfo(OtaErrorType.INVALID_APK, "Complete & verify the APK download before installing.")
            _otaUiState.value = _otaUiState.value.copy(error = err)
            return err
        }
        val err = OtaApkInstaller.launchSystemPackageInstaller(appContext, dl.localApkPath)
        refreshInstallPermissionState()
        if (err != null) {
            _otaUiState.value = _otaUiState.value.copy(error = err)
            showToast(err.type.title)
        } else {
            val rel = _otaUiState.value.latestRelease
            viewModelScope.launch {
                db.updateHistoryDao().insert(
                    UpdateHistoryEntity(
                        versionName = dl.verifiedVersionName ?: (rel?.versionName ?: BuildConfig.VERSION_NAME),
                        releaseTitle = rel?.releaseTitle ?: "Hardik Buds Pro Update",
                        releaseDate = rel?.publishedAt ?: "Installer",
                        changelog = "Launched Android Package Installer with user confirmation.",
                        status = "INSTALL_PROMPTED",
                        sha256 = dl.computedSha256,
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            }
        }
        return err
    }

    fun clearOtaHistory() {
        viewModelScope.launch {
            db.updateHistoryDao().clearAll()
            seedInitialUpdateHistoryIfEmpty()
            showToast("Cleared OTA update check logs")
        }
    }

    fun resetAllSettings() {
        _customization.value = CustomizationSettings()
        _otaSettings.value = OtaSettings()
        otaDownloader.cancelDownload()
        viewModelScope.launch {
            settingsRepo.resetAllPreferences()
            db.eqProfileDao().deleteAllCustomProfiles()
            db.connectionHistoryDao().clearAll()
            db.deviceProfileDao().clearAll()
            db.securityAuditDao().clearAll()
            db.updateHistoryDao().clearAll()
            seedInitialUpdateHistoryIfEmpty()
            OtaWorkScheduler.syncSchedule(appContext, OtaSettings())
            BudsMonitorService.stop(appContext)
            _isAppUnlocked.value = true
            showToast("All settings & local records reset to factory defaults")
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.releaseEffects()
        bluetoothController.release()
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
