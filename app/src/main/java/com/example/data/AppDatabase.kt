package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.EqualizerProfile

@Database(
    entities = [
        ConnectionHistoryEntity::class,
        SavedEqProfileEntity::class,
        DeviceProfileEntity::class,
        SecurityAuditLogEntity::class,
        UpdateHistoryEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionHistoryDao(): ConnectionHistoryDao
    abstract fun eqProfileDao(): EqProfileDao
    abstract fun deviceProfileDao(): DeviceProfileDao
    abstract fun securityAuditDao(): SecurityAuditDao
    abstract fun updateHistoryDao(): UpdateHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `update_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `versionName` TEXT NOT NULL,
                        `releaseTitle` TEXT NOT NULL,
                        `releaseDate` TEXT NOT NULL,
                        `changelog` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `sha256` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hardik_buds_pro.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object DefaultEqPresets {
    fun allBuiltIn(): List<EqualizerProfile> = listOf(
        EqualizerProfile(
            id = "preset_deep_bass",
            name = "Deep Bass",
            bandsDb = listOf(8.0f, 7.0f, 5.5f, 3.0f, 0.5f, 0.0f, 1.0f, 2.0f, 2.5f, 2.0f),
            bands15Db = listOf(7.5f, 8.0f, 7.8f, 7.0f, 6.2f, 5.5f, 4.0f, 3.0f, 0.5f, 0.0f, 1.0f, 2.0f, 2.5f, 2.2f, 2.0f),
            subwooferBoost = 82,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_bass_head",
            name = "Bass Head",
            bandsDb = listOf(10.5f, 9.5f, 7.5f, 4.5f, 1.0f, 0.5f, 1.5f, 3.0f, 3.5f, 2.5f),
            bands15Db = listOf(9.5f, 10.5f, 10.2f, 9.5f, 8.5f, 7.5f, 5.5f, 4.5f, 1.0f, 0.5f, 1.5f, 3.0f, 3.5f, 3.0f, 2.5f),
            subwooferBoost = 95,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_balanced",
            name = "Balanced",
            bandsDb = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            bands15Db = List(15) { 0f },
            subwooferBoost = 35,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_vocal",
            name = "Vocal",
            bandsDb = listOf(-1.5f, -1.0f, 0.0f, 2.0f, 4.5f, 6.0f, 5.5f, 4.0f, 2.0f, 1.0f),
            bands15Db = listOf(-2.0f, -1.5f, -1.2f, -1.0f, -0.5f, 0.0f, 1.2f, 2.0f, 4.5f, 6.0f, 5.5f, 4.0f, 2.0f, 1.5f, 1.0f),
            subwooferBoost = 20,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_clarity",
            name = "Clarity",
            bandsDb = listOf(1.5f, 1.0f, 0.5f, 0.0f, 1.5f, 3.0f, 4.5f, 6.0f, 6.5f, 5.5f),
            bands15Db = listOf(1.0f, 1.5f, 1.2f, 1.0f, 0.8f, 0.5f, 0.2f, 0.0f, 1.5f, 3.0f, 4.5f, 6.0f, 6.5f, 6.0f, 5.5f),
            subwooferBoost = 30,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_studio",
            name = "Studio",
            bandsDb = listOf(2.0f, 1.5f, 0.5f, 0.0f, 0.5f, 1.0f, 1.5f, 2.5f, 3.0f, 2.5f),
            bands15Db = listOf(1.8f, 2.0f, 1.8f, 1.5f, 1.0f, 0.5f, 0.2f, 0.0f, 0.5f, 1.0f, 1.5f, 2.5f, 3.0f, 2.8f, 2.5f),
            subwooferBoost = 40,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_cinematic",
            name = "Cinematic",
            bandsDb = listOf(7.0f, 6.0f, 4.5f, 2.5f, 2.5f, 4.0f, 4.5f, 4.0f, 3.5f, 3.0f),
            bands15Db = listOf(6.8f, 7.0f, 6.5f, 6.0f, 5.2f, 4.5f, 3.2f, 2.5f, 2.5f, 4.0f, 4.5f, 4.0f, 3.5f, 3.2f, 3.0f),
            subwooferBoost = 80,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_gaming",
            name = "Gaming",
            bandsDb = listOf(4.5f, 3.0f, 1.0f, -1.0f, 1.5f, 3.0f, 5.5f, 6.5f, 5.0f, 3.5f),
            subwooferBoost = 55,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_rock",
            name = "Rock",
            bandsDb = listOf(5.0f, 4.0f, 2.5f, 1.0f, -1.5f, 1.0f, 3.5f, 5.0f, 5.5f, 4.5f),
            subwooferBoost = 60,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_pop",
            name = "Pop",
            bandsDb = listOf(3.0f, 2.5f, 1.5f, 0.5f, 2.0f, 3.5f, 3.5f, 2.5f, 2.0f, 2.0f),
            subwooferBoost = 48,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_edm",
            name = "EDM",
            bandsDb = listOf(7.0f, 6.0f, 4.0f, 1.0f, -1.0f, 1.5f, 3.5f, 5.0f, 5.5f, 4.5f),
            subwooferBoost = 78,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_subwoofer",
            name = "Subwoofer",
            bandsDb = listOf(10.0f, 9.0f, 7.0f, 4.0f, 1.0f, 0.0f, 1.5f, 2.5f, 3.0f, 2.0f),
            subwooferBoost = 95,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_punjabi",
            name = "Punjabi",
            bandsDb = listOf(8.5f, 7.5f, 5.0f, 2.5f, 1.5f, 2.5f, 4.0f, 4.5f, 3.5f, 2.5f),
            subwooferBoost = 88,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_bollywood",
            name = "Bollywood",
            bandsDb = listOf(5.0f, 4.5f, 3.0f, 1.5f, 2.5f, 4.0f, 4.5f, 3.5f, 3.0f, 2.5f),
            subwooferBoost = 64,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_hiphop",
            name = "Hip-Hop",
            bandsDb = listOf(7.5f, 6.5f, 4.5f, 2.0f, 0.0f, 1.5f, 2.5f, 3.0f, 3.5f, 2.5f),
            subwooferBoost = 84,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_classical",
            name = "Classical",
            bandsDb = listOf(1.5f, 1.0f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.5f, 4.0f, 4.5f),
            subwooferBoost = 25,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_cinema",
            name = "Cinema",
            bandsDb = listOf(6.5f, 5.5f, 4.0f, 2.0f, 2.5f, 4.0f, 4.5f, 4.0f, 3.5f, 3.0f),
            subwooferBoost = 76,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_vocal_boost",
            name = "Vocal Boost",
            bandsDb = listOf(-1.0f, -0.5f, 0.5f, 2.5f, 5.0f, 6.0f, 5.5f, 4.0f, 2.0f, 1.0f),
            subwooferBoost = 20,
            isBuiltIn = true,
        ),
        EqualizerProfile(
            id = "preset_custom",
            name = "Custom",
            bandsDb = listOf(4.0f, 3.5f, 2.0f, 1.0f, 0.5f, 1.0f, 2.0f, 3.0f, 3.5f, 2.5f),
            subwooferBoost = 55,
            isBuiltIn = true,
        ),
    )

    fun toEntities(): List<SavedEqProfileEntity> = allBuiltIn().map {
        SavedEqProfileEntity(
            id = it.id,
            name = it.name,
            bandsCsv = it.resolved15Bands().joinToString(","),
            subwooferBoost = it.subwooferBoost,
            isBuiltIn = it.isBuiltIn,
            updatedAt = 0L,
        )
    }
}

fun SavedEqProfileEntity.toDomain(): EqualizerProfile {
    val parsedBands = bandsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
    val bands15 = when {
        parsedBands.size >= 15 -> parsedBands.take(15).map { it.coerceIn(-12f, 12f) }
        parsedBands.size == 10 -> com.example.model.DspConfig.expand10To15(parsedBands)
        else -> com.example.model.DspConfig.expand10To15(List(10) { idx -> parsedBands.getOrElse(idx) { 0f } })
    }
    val bands10 = if (parsedBands.size == 10) {
        parsedBands.map { it.coerceIn(-12f, 12f) }
    } else {
        com.example.model.DspConfig.compress15To10(bands15)
    }
    return EqualizerProfile(
        id = id,
        name = name,
        bandsDb = bands10,
        bands15Db = bands15,
        subwooferBoost = subwooferBoost,
        isBuiltIn = isBuiltIn,
    )
}

fun EqualizerProfile.toEntity(): SavedEqProfileEntity = SavedEqProfileEntity(
    id = id,
    name = name,
    bandsCsv = resolved15Bands().joinToString(","),
    subwooferBoost = subwooferBoost,
    isBuiltIn = isBuiltIn,
    updatedAt = System.currentTimeMillis(),
)
