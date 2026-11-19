package com.example.ota

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.SettingsRepository
import com.example.data.UpdateHistoryEntity
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class OtaUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = SettingsRepository(applicationContext)
        val settings = repo.otaSettingsFlow.first()
        if (!settings.backgroundCheckEnabled || settings.checkFrequency == OtaCheckFrequencyHours.MANUAL_ONLY) {
            return Result.success()
        }

        val releaseService = GitHubReleaseService(applicationContext)
        val fetchResult = releaseService.fetchReleases(
            owner = settings.githubOwner,
            repo = settings.githubRepo,
            channel = settings.channel,
        )

        if (fetchResult.error != null) {
            return if (fetchResult.error.type == OtaErrorType.NO_INTERNET ||
                fetchResult.error.type == OtaErrorType.GITHUB_API_UNAVAILABLE
            ) {
                Result.retry()
            } else {
                Result.success()
            }
        }

        val latest = fetchResult.latestForChannel ?: return Result.success()
        val now = System.currentTimeMillis()
        repo.updateOtaSettings(
            settings.copy(
                lastCheckedTimestamp = now,
                lastKnownLatestVersion = latest.versionName,
            ),
        )

        val isNewer = SemVerComparator.isNewerVersion(latest.versionName, BuildConfig.VERSION_NAME)
        if (isNewer) {
            val db = AppDatabase.getInstance(applicationContext)
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
            if (settings.notificationsEnabled) {
                OtaNotificationHelper.notifyUpdateAvailable(applicationContext, latest)
            }
        }
        return Result.success()
    }
}

object OtaWorkScheduler {
    private const val UNIQUE_WORK_NAME = "hardik_buds_pro_periodic_ota_check"

    fun syncSchedule(context: Context, settings: OtaSettings) {
        val wm = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        if (!settings.backgroundCheckEnabled || settings.checkFrequency == OtaCheckFrequencyHours.MANUAL_ONLY) {
            wm.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val hours = settings.checkFrequency.hours.coerceAtLeast(12L)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<OtaUpdateWorker>(hours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        wm.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
