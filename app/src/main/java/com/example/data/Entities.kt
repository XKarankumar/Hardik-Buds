package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_history")
data class ConnectionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val deviceName: String,
    val brandName: String,
    val tierLevel: Int,
    val connectedAt: Long,
    val disconnectedAt: Long? = null,
    val batteryAtConnect: Int? = null,
    val rssiDbm: Int? = null,
    val locationLabel: String? = null,
)

@Entity(tableName = "eq_profiles")
data class SavedEqProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bandsCsv: String,
    val subwooferBoost: Int,
    val isBuiltIn: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val address: String,
    val customAlias: String,
    val brandName: String,
    val preferredAncMode: String,
    val preferredSoundMode: String,
    val touchConfigJson: String,
    val firmwareVersion: String?,
    val isOwnerVerified: Boolean,
    val lastLocationLabel: String?,
    val updatedAt: Long,
)

@Entity(tableName = "security_audit_logs")
data class SecurityAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val deviceAddress: String?,
    val deviceName: String?,
    val detail: String,
    val timestamp: Long,
)
