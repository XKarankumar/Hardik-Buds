package com.example.bluetooth

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.model.BatteryDataSource
import com.example.model.DiscoveredAudioDevice
import com.example.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothController(private val context: Context) {

    companion object {
        private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

        private val UUID_BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_LEVEL_CHAR: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private val UUID_DEVICE_INFO_SERVICE: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        private val UUID_MODEL_NUMBER_CHAR: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
        private val UUID_FIRMWARE_REV_CHAR: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        private val UUID_FAST_PAIR: UUID = UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")
        private val PARCEL_UUID_FAST_PAIR: ParcelUuid = ParcelUuid(UUID_FAST_PAIR)

        const val CHANNEL_ALERTS = "hardik_buds_alerts"
        const val CHANNEL_MONITOR = "hardik_buds_monitor"
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var activeGatt: BluetoothGatt? = null
    private var scanTimeoutJob: Job? = null

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<DiscoveredAudioDevice>>(emptyList())
    val bondedDevices: StateFlow<List<DiscoveredAudioDevice>> = _bondedDevices.asStateFlow()

    private val _nearbyDevices = MutableStateFlow<List<DiscoveredAudioDevice>>(emptyList())
    val nearbyDevices: StateFlow<List<DiscoveredAudioDevice>> = _nearbyDevices.asStateFlow()

    private val _fastPairDevices = MutableStateFlow<List<DiscoveredAudioDevice>>(emptyList())
    val fastPairDevices: StateFlow<List<DiscoveredAudioDevice>> = _fastPairDevices.asStateFlow()

    private val _isFastPairScanning = MutableStateFlow(false)
    val isFastPairScanning: StateFlow<Boolean> = _isFastPairScanning.asStateFlow()

    private val _activeDevice = MutableStateFlow<DiscoveredAudioDevice?>(null)
    val activeDevice: StateFlow<DiscoveredAudioDevice?> = _activeDevice.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val cachedBatteryByAddress = mutableMapOf<String, Int>()
    private val cachedGattFirmwareByAddress = mutableMapOf<String, String>()
    private val cachedGattModelByAddress = mutableMapOf<String, String>()
    private val cachedRssiByAddress = mutableMapOf<String, Int>()
    private val customAliasByAddress = mutableMapOf<String, String>()
    private val nearbyMap = linkedMapOf<String, DiscoveredAudioDevice>()
    private val fastPairMap = linkedMapOf<String, DiscoveredAudioDevice>()

    var onDeviceConnectedCallback: ((DiscoveredAudioDevice) -> Unit)? = null
    var onDeviceDisconnectedCallback: ((String) -> Unit)? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = proxy as? BluetoothA2dp
            if (profile == BluetoothProfile.HEADSET) headsetProxy = proxy as? BluetoothHeadset
            refreshBondedAndConnectedDevices()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = null
            if (profile == BluetoothProfile.HEADSET) headsetProxy = null
            refreshBondedAndConnectedDevices()
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                    refreshBondedAndConnectedDevices()
                }

                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val device = extractDevice(intent)
                    refreshBondedAndConnectedDevices()
                    if (device != null && intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                        _activeDevice.value?.let { active ->
                            if (active.address == device.address) {
                                onDeviceConnectedCallback?.invoke(active)
                            }
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = extractDevice(intent)
                    if (device != null) {
                        onDeviceDisconnectedCallback?.invoke(device.address)
                    }
                    refreshBondedAndConnectedDevices()
                }

                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val device = extractDevice(intent)
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (device != null && level in 0..100) {
                        cachedBatteryByAddress[device.address] = level
                        refreshBondedAndConnectedDevices()
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    if (!PermissionManager.hasBluetoothConnectPermission(context)) return
                    val device = extractDevice(intent) ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    if (rssi != Short.MIN_VALUE.toInt()) {
                        cachedRssiByAddress[device.address] = rssi
                    }
                    val mapped = mapBluetoothDevice(device, isBle = false, rssi = cachedRssiByAddress[device.address])
                    nearbyMap[mapped.address] = mapped
                    _nearbyDevices.value = nearbyMap.values.toList()
                }
            }
        }
    }

    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!PermissionManager.hasBluetoothConnectPermission(context)) return
            val dev = result.device ?: return
            val record = result.scanRecord
            val fastPairBytes = record?.getServiceData(PARCEL_UUID_FAST_PAIR)
            val serviceUuids = record?.serviceUuids.orEmpty()
            val isFastPair = fastPairBytes != null || serviceUuids.any { it.uuid == UUID_FAST_PAIR }
            val fastPairModel = when {
                fastPairBytes != null && fastPairBytes.size == 3 ->
                    fastPairBytes.joinToString("") { "%02X".format(it) }
                fastPairBytes != null && fastPairBytes.isNotEmpty() ->
                    "GFPS-${fastPairBytes.size}B"
                isFastPair -> "FE2C"
                else -> null
            }

            val rawName = record?.deviceName ?: runCatching { dev.name }.getOrNull()
            val isBonded = runCatching { dev.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
            if (rawName.isNullOrBlank() && !isBonded && !isFastPair) return

            val effectiveName = rawName?.takeIf { it.isNotBlank() }
                ?: if (fastPairModel != null) "Fast Pair Audio ($fastPairModel)" else dev.address

            cachedRssiByAddress[dev.address] = result.rssi
            val brand = DeviceCompatibilityEngine.detectBrand(
                deviceName = effectiveName,
                address = dev.address,
                manufacturerData = record?.manufacturerSpecificData,
                serviceUuids = record?.serviceUuids,
            )
            val report = DeviceCompatibilityEngine.evaluateCompatibility(
                brand = brand,
                serviceUuids = record?.serviceUuids,
            )
            val mapped = mapBluetoothDevice(dev, isBle = true, rssi = result.rssi).copy(
                name = effectiveName,
                brand = brand,
                compatibilityReport = report,
                isFastPairSupported = isFastPair,
                fastPairModelId = fastPairModel,
                isHomeTheatreSpeaker = DeviceCompatibilityEngine.isHomeTheatreDevice(effectiveName),
            )
            nearbyMap[mapped.address] = mapped
            _nearbyDevices.value = nearbyMap.values.sortedByDescending { it.rssiDbm ?: -120 }
            if (isFastPair) {
                fastPairMap[mapped.address] = mapped
                _fastPairDevices.value = fastPairMap.values.sortedByDescending { it.rssiDbm ?: -120 }
            }
        }
    }

    fun startListening() {
        ensureNotificationChannels()
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
        }
        runCatching { context.registerReceiver(btReceiver, filter) }

        val adapter = bluetoothAdapter
        if (adapter != null && PermissionManager.hasBluetoothConnectPermission(context)) {
            runCatching {
                adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
                adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            }
        }
        refreshBondedAndConnectedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedAndConnectedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _isBluetoothSupported.value = false
            _isBluetoothEnabled.value = false
            return
        }
        _isBluetoothSupported.value = true
        _isBluetoothEnabled.value = adapter.isEnabled

        if (!PermissionManager.hasBluetoothConnectPermission(context) || !adapter.isEnabled) {
            _bondedDevices.value = emptyList()
            _activeDevice.value = null
            return
        }

        val a2dpConnected = runCatching { a2dpProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())
        val hfpConnected = runCatching { headsetProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())

        val bonded = runCatching { adapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
        val mappedList = bonded
            .filter { dev ->
                val major = runCatching { dev.bluetoothClass?.majorDeviceClass }.getOrNull()
                major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                    major == BluetoothClass.Device.Major.UNCATEGORIZED ||
                    major == null ||
                    a2dpConnected.any { it.address == dev.address } ||
                    hfpConnected.any { it.address == dev.address }
            }
            .map { dev ->
                val isA2dp = a2dpConnected.any { it.address == dev.address }
                val isHfp = hfpConnected.any { it.address == dev.address }
                mapBluetoothDevice(
                    device = dev,
                    isBle = false,
                    rssi = cachedRssiByAddress[dev.address],
                    isA2dp = isA2dp,
                    isHfp = isHfp,
                )
            }
            .sortedWith(compareByDescending<DiscoveredAudioDevice> { it.isConnected }.thenBy { it.displayTitle })

        _bondedDevices.value = mappedList
        val currentActive = mappedList.firstOrNull { it.isConnected }
            ?: _activeDevice.value?.let { prev -> mappedList.firstOrNull { it.address == prev.address } }
        _activeDevice.value = currentActive
    }

    @SuppressLint("MissingPermission")
    fun startBleAndClassicScan() {
        if (!PermissionManager.hasBluetoothScanPermission(context) ||
            !PermissionManager.hasBluetoothConnectPermission(context)
        ) {
            _statusBanner.value = "Bluetooth Scan & Connect permissions are required to scan nearby earbuds."
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _statusBanner.value = "Bluetooth is turned off. Enable Bluetooth to scan for earbuds."
            return
        }

        stopScan()
        nearbyMap.clear()
        _nearbyDevices.value = emptyList()
        _isScanning.value = true
        _statusBanner.value = "Scanning nearby Bluetooth & BLE audio devices..."

        runCatching {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            bleScanner?.startScan(null, settings, leScanCallback)
        }
        runCatching { adapter.startDiscovery() }

        scanTimeoutJob = scope.launch {
            delay(10_000L)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun startGoogleFastPairScan() {
        if (!PermissionManager.hasBluetoothScanPermission(context) ||
            !PermissionManager.hasBluetoothConnectPermission(context)
        ) {
            _statusBanner.value = "Grant Bluetooth Scan & Connect permissions for Google Fast Connection."
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _statusBanner.value = "Turn on Bluetooth to discover Google Fast Pair (0xFE2C) devices."
            return
        }

        stopScan()
        fastPairMap.clear()
        _fastPairDevices.value = emptyList()
        _isFastPairScanning.value = true
        _isScanning.value = true
        _statusBanner.value = "Scanning Google Fast Pair (0xFE2C) BLE peripherals..."

        runCatching {
            val fastPairFilter = ScanFilter.Builder()
                .setServiceUuid(PARCEL_UUID_FAST_PAIR)
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            bleScanner?.startScan(listOf(fastPairFilter), settings, leScanCallback)
        }.onFailure {
            runCatching {
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                bleScanner?.startScan(null, settings, leScanCallback)
            }
        }

        scanTimeoutJob = scope.launch {
            delay(12_000L)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun requestCompanionFastPairing(
        onChooserIntentReady: (IntentSender) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onError("Companion Fast Pairing requires Android 8.0+ — opening Bluetooth pairing.")
            PermissionManager.openBluetoothSettings(context)
            return
        }
        val cdm = context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
        if (cdm == null) {
            onError("CompanionDeviceManager unavailable on this device.")
            PermissionManager.openBluetoothSettings(context)
            return
        }

        val bleFastPairFilter = BluetoothLeDeviceFilter.Builder()
            .setScanFilter(ScanFilter.Builder().setServiceUuid(PARCEL_UUID_FAST_PAIR).build())
            .build()
        val classicAudioFilter = BluetoothDeviceFilter.Builder().build()

        val request = AssociationRequest.Builder()
            .addDeviceFilter(bleFastPairFilter)
            .addDeviceFilter(classicAudioFilter)
            .build()

        val callback = object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                onChooserIntentReady(intentSender)
            }

            @Deprecated("Deprecated in Java")
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                onChooserIntentReady(chooserLauncher)
            }

            override fun onFailure(error: CharSequence?) {
                val msg = error?.toString()?.takeIf { it.isNotBlank() }
                    ?: "No Fast Pair audio device selected."
                onError(msg)
            }
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                cdm.associate(request, context.mainExecutor, callback)
            } else {
                cdm.associate(request, callback, null)
            }
        }.onFailure { err ->
            onError(err.message ?: "Unable to open OS Fast Pair chooser.")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeoutJob?.cancel()
        if (PermissionManager.hasBluetoothScanPermission(context)) {
            runCatching { bleScanner?.stopScan(leScanCallback) }
            runCatching { bluetoothAdapter?.cancelDiscovery() }
        }
        _isScanning.value = false
        _isFastPairScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun selectAndConnectDevice(device: DiscoveredAudioDevice) {
        if (!PermissionManager.hasBluetoothConnectPermission(context)) {
            _statusBanner.value = "Bluetooth Connect permission is required."
            return
        }
        val btDevice = runCatching { bluetoothAdapter?.getRemoteDevice(device.address) }.getOrNull() ?: return

        // If unbonded, initiate real OS Bluetooth bond/pairing
        if (btDevice.bondState != BluetoothDevice.BOND_BONDED) {
            val started = runCatching { btDevice.createBond() }.getOrDefault(false)
            _statusBanner.value = if (started) {
                "Pairing request sent to ${device.displayTitle}. Confirm in system prompt."
            } else {
                "Opening system Bluetooth settings to pair ${device.displayTitle}..."
            }
            if (!started) PermissionManager.openBluetoothSettings(context)
            return
        }

        // Try A2DP connect via profile proxy reflection, plus BLE GATT telemetry readout
        var a2dpAttempted = false
        a2dpProxy?.let { proxy ->
            runCatching {
                val method = BluetoothA2dp::class.java.getMethod("connect", BluetoothDevice::class.java)
                a2dpAttempted = method.invoke(proxy, btDevice) as? Boolean ?: false
            }
        }
        headsetProxy?.let { proxy ->
            runCatching {
                val method = BluetoothHeadset::class.java.getMethod("connect", BluetoothDevice::class.java)
                method.invoke(proxy, btDevice)
            }
        }

        // Also inspect BLE GATT Device Info (0x180A) & Battery Service (0x180F)
        inspectGattTelemetry(btDevice)

        _activeDevice.value = device
        _statusBanner.value = if (a2dpAttempted) {
            "Connecting audio profile to ${device.displayTitle}..."
        } else {
            "Selected ${device.displayTitle}. If audio doesn't route automatically, tap Pair/Audio in Bluetooth Settings."
        }
        refreshBondedAndConnectedDevices()
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(device: DiscoveredAudioDevice) {
        if (!PermissionManager.hasBluetoothConnectPermission(context)) return
        val btDevice = runCatching { bluetoothAdapter?.getRemoteDevice(device.address) }.getOrNull() ?: return
        var disconnected = false

        a2dpProxy?.let { proxy ->
            runCatching {
                val method = BluetoothA2dp::class.java.getMethod("disconnect", BluetoothDevice::class.java)
                disconnected = method.invoke(proxy, btDevice) as? Boolean ?: false
            }
        }
        headsetProxy?.let { proxy ->
            runCatching {
                val method = BluetoothHeadset::class.java.getMethod("disconnect", BluetoothDevice::class.java)
                method.invoke(proxy, btDevice)
            }
        }
        runCatching {
            activeGatt?.disconnect()
            activeGatt?.close()
            activeGatt = null
        }

        _statusBanner.value = if (disconnected) {
            "Disconnected ${device.displayTitle}."
        } else {
            "Android restricted direct A2DP disconnect for this device — opening Bluetooth Settings."
        }
        if (!disconnected) {
            PermissionManager.openBluetoothSettings(context)
        }
        refreshBondedAndConnectedDevices()
    }

    @SuppressLint("MissingPermission")
    fun inspectGattTelemetry(device: BluetoothDevice) {
        if (!PermissionManager.hasBluetoothConnectPermission(context)) return
        runCatching {
            activeGatt?.close()
            activeGatt = device.connectGatt(
                context,
                false,
                object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.discoverServices()
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) return
                        val batChar = gatt.getService(UUID_BATTERY_SERVICE)?.getCharacteristic(UUID_BATTERY_LEVEL_CHAR)
                        if (batChar != null) {
                            gatt.readCharacteristic(batChar)
                        } else {
                            readNextDeviceInfoChar(gatt)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int,
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            handleGattCharacteristicValue(gatt, characteristic.uuid, characteristic.value)
                        }
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int,
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            handleGattCharacteristicValue(gatt, characteristic.uuid, value)
                        }
                    }

                    private fun handleGattCharacteristicValue(gatt: BluetoothGatt, uuid: UUID, value: ByteArray?) {
                        if (value == null || value.isEmpty()) return
                        val addr = gatt.device.address
                        when (uuid) {
                            UUID_BATTERY_LEVEL_CHAR -> {
                                val pct = value[0].toInt() and 0xFF
                                if (pct in 0..100) cachedBatteryByAddress[addr] = pct
                                readNextDeviceInfoChar(gatt)
                            }
                            UUID_FIRMWARE_REV_CHAR -> {
                                cachedGattFirmwareByAddress[addr] = String(value, Charsets.UTF_8).trim()
                            }
                            UUID_MODEL_NUMBER_CHAR -> {
                                cachedGattModelByAddress[addr] = String(value, Charsets.UTF_8).trim()
                            }
                        }
                        scope.launch { refreshBondedAndConnectedDevices() }
                    }

                    private fun readNextDeviceInfoChar(gatt: BluetoothGatt) {
                        val infoSvc = gatt.getService(UUID_DEVICE_INFO_SERVICE) ?: return
                        val fwChar = infoSvc.getCharacteristic(UUID_FIRMWARE_REV_CHAR)
                        val modelChar = infoSvc.getCharacteristic(UUID_MODEL_NUMBER_CHAR)
                        if (fwChar != null) {
                            gatt.readCharacteristic(fwChar)
                        } else if (modelChar != null) {
                            gatt.readCharacteristic(modelChar)
                        }
                    }
                },
                BluetoothDevice.TRANSPORT_AUTO,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun mapBluetoothDevice(
        device: BluetoothDevice,
        isBle: Boolean,
        rssi: Int?,
        isA2dp: Boolean = false,
        isHfp: Boolean = false,
    ): DiscoveredAudioDevice {
        val rawName = runCatching { device.name }.getOrNull().orEmpty()
        val aliasName = customAliasByAddress[device.address] ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { device.alias }.getOrNull()
        } else null
        val bonded = runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
        val serviceUuids = runCatching { device.uuids?.toList() }.getOrNull()
        val brand = DeviceCompatibilityEngine.detectBrand(
            deviceName = rawName,
            address = device.address,
            serviceUuids = serviceUuids,
        )

        // Read real Android battery telemetry (Reflection-safe getBatteryLevel & API 29+ getMetadata)
        val hfpBattery = readHfpBatteryLevel(device) ?: cachedBatteryByAddress[device.address]
        val leftBat = readUntetheredBattery(device, METADATA_UNTETHERED_LEFT_BATTERY)
        val rightBat = readUntetheredBattery(device, METADATA_UNTETHERED_RIGHT_BATTERY)
        val caseBat = readUntetheredBattery(device, METADATA_UNTETHERED_CASE_BATTERY)
        val mainMetaBat = readUntetheredBattery(device, METADATA_MAIN_BATTERY)
        val overallBattery = mainMetaBat ?: hfpBattery

        val batSource = when {
            leftBat != null || rightBat != null || caseBat != null -> BatteryDataSource.ANDROID_METADATA_UNTETHERED
            cachedBatteryByAddress.containsKey(device.address) -> BatteryDataSource.BLE_GATT_BATTERY_SERVICE
            overallBattery != null -> BatteryDataSource.ANDROID_HFP_METADATA
            else -> BatteryDataSource.UNAVAILABLE
        }

        val fw = cachedGattFirmwareByAddress[device.address]
        val model = cachedGattModelByAddress[device.address]
        val report = DeviceCompatibilityEngine.evaluateCompatibility(
            brand = brand,
            hasBatteryData = overallBattery != null,
            hasUntetheredBattery = leftBat != null || rightBat != null || caseBat != null,
            hasGattBatteryService = cachedBatteryByAddress.containsKey(device.address),
            hasGattDeviceInfo = fw != null,
            serviceUuids = serviceUuids,
        )

        val isFastPair = serviceUuids?.any { it.uuid == UUID_FAST_PAIR } == true
        val isHomeTheatre = DeviceCompatibilityEngine.isHomeTheatreDevice(rawName)

        return DiscoveredAudioDevice(
            address = device.address,
            name = rawName.ifBlank { "Bluetooth Device" },
            alias = aliasName,
            isBonded = bonded,
            isConnected = isA2dp || isHfp,
            isA2dpConnected = isA2dp,
            isHeadsetConnected = isHfp,
            isBleDevice = isBle,
            isFastPairSupported = isFastPair,
            fastPairModelId = if (isFastPair) "FE2C" else null,
            isHomeTheatreSpeaker = isHomeTheatre,
            rssiDbm = rssi,
            deviceClassMajor = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull(),
            brand = brand,
            compatibilityReport = report,
            batteryOverallPercent = overallBattery,
            leftBatteryPercent = leftBat,
            rightBatteryPercent = rightBat,
            caseBatteryPercent = caseBat,
            batterySource = batSource,
            firmwareRevision = fw,
            modelNumber = model,
        )
    }

    private fun readHfpBatteryLevel(device: BluetoothDevice): Int? {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) level else null
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun readUntetheredBattery(device: BluetoothDevice, metadataKey: Int): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getMetadata", Int::class.javaPrimitiveType)
            val bytes = method.invoke(device, metadataKey) as? ByteArray ?: return null
            val parsed = String(bytes, Charsets.UTF_8).trim().toIntOrNull()
            if (parsed != null && parsed in 0..100) parsed else null
        }.getOrNull()
    }

    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    @SuppressLint("MissingPermission")
    fun renameDevice(address: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        customAliasByAddress[address] = trimmed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && PermissionManager.hasBluetoothConnectPermission(context)) {
            runCatching {
                bluetoothAdapter?.getRemoteDevice(address)?.setAlias(trimmed)
            }
        }
        refreshBondedAndConnectedDevices()
    }

    fun seedCustomAliases(aliases: Map<String, String>) {
        customAliasByAddress.putAll(aliases)
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun sendNotification(title: String, body: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionManager.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        ) {
            return
        }
        ensureNotificationChannels()
        val notif = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFFF3038.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notificationId, notif) }
    }

    fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    context.getString(R.string.alerts_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.alerts_channel_desc) },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MONITOR,
                    context.getString(R.string.monitor_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = context.getString(R.string.monitor_channel_desc) },
            )
        }
    }

    fun release() {
        stopScan()
        runCatching { context.unregisterReceiver(btReceiver) }
        runCatching {
            a2dpProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
            headsetProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) }
            activeGatt?.close()
        }
    }

    private val METADATA_UNTETHERED_LEFT_BATTERY = 10
    private val METADATA_UNTETHERED_RIGHT_BATTERY = 11
    private val METADATA_UNTETHERED_CASE_BATTERY = 12
    private val METADATA_MAIN_BATTERY = 18
}
