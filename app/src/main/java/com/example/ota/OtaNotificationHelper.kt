package com.example.ota

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object OtaNotificationHelper {

    const val CHANNEL_OTA = "hardik_buds_pro_ota"
    const val EXTRA_OPEN_UPDATE_CENTER = "extra_open_update_center"

    private const val NOTIF_UPDATE_AVAILABLE = 2101
    private const val NOTIF_DOWNLOAD_DONE = 2102
    private const val NOTIF_DOWNLOAD_FAILED = 2103

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_OTA,
                context.getString(R.string.ota_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.ota_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return true
    }

    private fun createUpdateCenterPendingIntent(context: Context, reqCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_UPDATE_CENTER, true)
        }
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun notifyUpdateAvailable(context: Context, release: GitHubReleaseInfo) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val channelLabel = if (release.isPrerelease) "Beta Release" else "New Stable Release"
        val body = "${release.releaseTitle} (v${release.versionName}) is ready to download from GitHub."
        val notification = NotificationCompat.Builder(context, CHANNEL_OTA)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFFF3038.toInt())
            .setContentTitle("Hardik Buds Pro · $channelLabel")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(createUpdateCenterPendingIntent(context, NOTIF_UPDATE_AVAILABLE))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_UPDATE_AVAILABLE, notification) }
    }

    fun notifyDownloadCompleted(context: Context, versionLabel: String) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val body = "Update v$versionLabel verified (SHA-256 & package check passed). Tap to install."
        val notification = NotificationCompat.Builder(context, CHANNEL_OTA)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFFF3038.toInt())
            .setContentTitle("Hardik Buds Pro · Update Ready")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(createUpdateCenterPendingIntent(context, NOTIF_DOWNLOAD_DONE))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_DOWNLOAD_DONE, notification) }
    }

    fun notifyDownloadFailed(context: Context, reason: String) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_OTA)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFFF3038.toInt())
            .setContentTitle("Hardik Buds Pro · Update Failed")
            .setContentText(reason)
            .setAutoCancel(true)
            .setContentIntent(createUpdateCenterPendingIntent(context, NOTIF_DOWNLOAD_FAILED))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_DOWNLOAD_FAILED, notification) }
    }
}
