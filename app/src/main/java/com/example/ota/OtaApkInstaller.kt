package com.example.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object OtaApkInstaller {

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    fun createUnknownAppsSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun launchSystemPackageInstaller(context: Context, apkPath: String): OtaErrorInfo? {
        if (!canRequestPackageInstalls(context)) {
            return OtaErrorInfo(OtaErrorType.PERMISSION_DENIED)
        }

        val apkFile = File(apkPath)
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() <= 0L) {
            return OtaErrorInfo(OtaErrorType.INVALID_APK, "Verified APK file no longer exists on disk.")
        }

        return runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            null
        }.getOrElse { throwable ->
            OtaErrorInfo(
                OtaErrorType.INSTALLATION_BLOCKED,
                throwable.message ?: "Unable to open Android Package Installer.",
            )
        }
    }

    fun openReleaseInBrowser(context: Context, releaseHtmlUrl: String): Boolean {
        if (!SemVerComparator.isSecureHttpsUrl(releaseHtmlUrl)) return false
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(releaseHtmlUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        }.getOrDefault(false)
    }
}
