package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionHistoryDao {
    @Query("SELECT * FROM connection_history ORDER BY connectedAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<ConnectionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ConnectionHistoryEntity): Long

    @Query("UPDATE connection_history SET disconnectedAt = :timestamp WHERE address = :address AND disconnectedAt IS NULL")
    suspend fun markDisconnected(address: String, timestamp: Long)

    @Query("DELETE FROM connection_history")
    suspend fun clearAll()
}

@Dao
interface EqProfileDao {
    @Query("SELECT * FROM eq_profiles ORDER BY isBuiltIn DESC, updatedAt DESC")
    fun observeProfiles(): Flow<List<SavedEqProfileEntity>>

    @Query("SELECT COUNT(*) FROM eq_profiles")
    suspend fun countProfiles(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: SavedEqProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(profiles: List<SavedEqProfileEntity>)

    @Query("UPDATE eq_profiles SET name = :newName, updatedAt = :updatedAt WHERE id = :id AND isBuiltIn = 0")
    suspend fun renameCustomProfile(id: String, newName: String, updatedAt: Long)

    @Query("DELETE FROM eq_profiles WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomProfile(id: String)

    @Query("DELETE FROM eq_profiles WHERE isBuiltIn = 0")
    suspend fun deleteAllCustomProfiles()
}

@Dao
interface DeviceProfileDao {
    @Query("SELECT * FROM device_profiles ORDER BY updatedAt DESC")
    fun observeDeviceProfiles(): Flow<List<DeviceProfileEntity>>

    @Query("SELECT * FROM device_profiles WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): DeviceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: DeviceProfileEntity)

    @Query("DELETE FROM device_profiles")
    suspend fun clearAll()
}

@Dao
interface SecurityAuditDao {
    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC LIMIT 40")
    fun observeLogs(): Flow<List<SecurityAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SecurityAuditLogEntity)

    @Query("DELETE FROM security_audit_logs")
    suspend fun clearAll()
}
