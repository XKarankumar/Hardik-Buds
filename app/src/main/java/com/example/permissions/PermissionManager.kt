package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat

enum class AppPermissionType(
    val title: String,
    val explanation: String,
    val recoveryInstruction: String,
    val isRequiredForCoreBluetooth: Boolean,
) {
    BLUETOOTH_SCAN(
        title = "Nearby Devices / Bluetooth Scan",
        explanation = "Required to scan for nearby Bluetooth & BLE earbuds and read live RSSI signal strength.",
        recoveryInstruction = "Open App Settings → Permissions → Nearby devices → Allow.",
        isRequiredForCoreBluetooth = true,
    ),
    BLUETOOTH_CONNECT(
        title = "Bluetooth Connect",
        explanation = "Required to read paired earbuds, monitor connection state, and inspect battery telemetry.",
        recoveryInstruction = "Open App Settings → Permissions → Nearby devices → Allow.",
        isRequiredForCoreBluetooth = true,
    ),
    NOTIFICATIONS(
        title = "Notifications",
        explanation = "Allows connection alerts, low-battery warnings, and optional background connection monitor.",
        recoveryInstruction = "Open App Settings → Notifications → Enable All Hardik Buds Pro notifications.",
        isRequiredForCoreBluetooth = false,
    ),
    LOCATION(
        title = "Approximate Location (Optional)",
        explanation = "Used only when you enable 'Last Connected Location' or scan BLE on Android 11 and earlier.",
        recoveryInstruction = "Open App Settings → Permissions → Location → Allow only while using the app.",
        isRequiredForCoreBluetooth = false,
    ),
    MICROPHONE(
        title = "Audio Visualizer Input (Optional)",
        explanation = "Android's AudioEffect Visualizer API requires RECORD_AUDIO permission to sample live FFT bass energy.",
        recoveryInstruction = "Open App Settings → Permissions → Microphone → Allow while using the app.",
        isRequiredForCoreBluetooth = false,
    ),
}

@Immutable
data class PermissionStatusItem(
    val type: AppPermissionType,
    val manifestPermissions: List<String>,
    val isGranted: Boolean,
    val isApplicableOnOs: Boolean = true,
)

@Immutable
data class PermissionsSnapshot(
    val items: List<PermissionStatusItem> = emptyList(),
    val bluetoothReady: Boolean = false,
    val notificationsGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val microphoneGranted: Boolean = false,
)

object PermissionManager {

    fun requiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    }

    fun getSnapshot(context: Context): PermissionsSnapshot {
        val scanPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val connectPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.BLUETOOTH)
        }

        val notifPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }

        val locationPerms = listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        val micPerms = listOf(Manifest.permission.RECORD_AUDIO)

        val scanGranted = scanPerms.all { hasPermission(context, it) }
        val connectGranted = connectPerms.all { hasPermission(context, it) }
        val notifGranted = if (notifPerms.isEmpty()) true else notifPerms.all { hasPermission(context, it) }
        val locationGranted = locationPerms.any { hasPermission(context, it) } ||
            hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val micGranted = micPerms.all { hasPermission(context, it) }

        val items = listOf(
            PermissionStatusItem(
                type = AppPermissionType.BLUETOOTH_SCAN,
                manifestPermissions = scanPerms,
                isGranted = scanGranted,
            ),
            PermissionStatusItem(
                type = AppPermissionType.BLUETOOTH_CONNECT,
                manifestPermissions = connectPerms,
                isGranted = connectGranted,
            ),
            PermissionStatusItem(
                type = AppPermissionType.NOTIFICATIONS,
                manifestPermissions = notifPerms,
                isGranted = notifGranted,
                isApplicableOnOs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            ),
            PermissionStatusItem(
                type = AppPermissionType.LOCATION,
                manifestPermissions = locationPerms,
                isGranted = locationGranted,
            ),
            PermissionStatusItem(
                type = AppPermissionType.MICROPHONE,
                manifestPermissions = micPerms,
                isGranted = micGranted,
            ),
        )

        return PermissionsSnapshot(
            items = items,
            bluetoothReady = scanGranted && connectGranted,
            notificationsGranted = notifGranted,
            locationGranted = locationGranted,
            microphoneGranted = micGranted,
        )
    }

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(context, Manifest.permission.BLUETOOTH)
        }
    }

    fun hasBluetoothScanPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN) &&
                hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
