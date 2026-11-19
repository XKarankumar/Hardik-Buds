package com.example.ota

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class ApkVerificationReport(
    val isValid: Boolean,
    val sha256Hex: String,
    val sha256Matched: Boolean?,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val signatureMatchesInstalled: Boolean?,
    val errorInfo: OtaErrorInfo? = null,
)

object OtaSecurityVerifier {

    fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        FileInputStream(file).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    fun verifyDownloadedApk(
        context: Context,
        apkFile: File,
        expectedSizeBytes: Long,
        expectedSha256: String?,
        installedVersionCode: Long,
        installedVersionName: String,
    ): ApkVerificationReport {
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() <= 0L) {
            return ApkVerificationReport(
                isValid = false,
                sha256Hex = "",
                sha256Matched = null,
                packageName = null,
                versionName = null,
                versionCode = null,
                signatureMatchesInstalled = null,
                errorInfo = OtaErrorInfo(OtaErrorType.INVALID_APK, "APK file missing or empty."),
            )
        }

        if (expectedSizeBytes > 0L && apkFile.length() != expectedSizeBytes) {
            return ApkVerificationReport(
                isValid = false,
                sha256Hex = "",
                sha256Matched = null,
                packageName = null,
                versionName = null,
                versionCode = null,
                signatureMatchesInstalled = null,
                errorInfo = OtaErrorInfo(
                    OtaErrorType.INVALID_APK,
                    "Incomplete download: expected $expectedSizeBytes bytes, got ${apkFile.length()} bytes.",
                ),
            )
        }

        val computedSha = computeFileSha256(apkFile)
        val normalizedExpectedSha = expectedSha256?.trim()?.lowercase()?.takeIf { it.length == 64 }
        val shaMatched = normalizedExpectedSha?.let { it == computedSha }
        if (shaMatched == false) {
            return ApkVerificationReport(
                isValid = false,
                sha256Hex = computedSha,
                sha256Matched = false,
                packageName = null,
                versionName = null,
                versionCode = null,
                signatureMatchesInstalled = null,
                errorInfo = OtaErrorInfo(
                    OtaErrorType.INVALID_APK,
                    "SHA-256 checksum mismatch. Expected $normalizedExpectedSha, computed $computedSha.",
                ),
            )
        }

        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo: PackageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: return ApkVerificationReport(
                isValid = false,
                sha256Hex = computedSha,
                sha256Matched = shaMatched,
                packageName = null,
                versionName = null,
                versionCode = null,
                signatureMatchesInstalled = null,
                errorInfo = OtaErrorInfo(
                    OtaErrorType.INVALID_APK,
                    "Corrupted APK archive: Android PackageManager could not parse manifest.",
                ),
            )

        if (archiveInfo.packageName != context.packageName) {
            return ApkVerificationReport(
                isValid = false,
                sha256Hex = computedSha,
                sha256Matched = shaMatched,
                packageName = archiveInfo.packageName,
                versionName = archiveInfo.versionName,
                versionCode = extractVersionCode(archiveInfo),
                signatureMatchesInstalled = null,
                errorInfo = OtaErrorInfo(
                    OtaErrorType.INVALID_APK,
                    "Package ID mismatch (${archiveInfo.packageName} != ${context.packageName}).",
                ),
            )
        }

        val apkVerCode = extractVersionCode(archiveInfo)
        val apkVerName = archiveInfo.versionName ?: "0.0.0"
        val isDowngradeByCode = apkVerCode < installedVersionCode
        val isDowngradeBySemVer = SemVerComparator.parse(apkVerName)?.let { candidate ->
            val current = SemVerComparator.parse(installedVersionName)
            current != null && candidate < current
        } ?: false

        if (isDowngradeByCode || isDowngradeBySemVer) {
            return ApkVerificationReport(
                isValid = false,
                sha256Hex = computedSha,
                sha256Matched = shaMatched,
                packageName = archiveInfo.packageName,
                versionName = apkVerName,
                versionCode = apkVerCode,
                signatureMatchesInstalled = null,
                errorInfo = OtaErrorInfo(
                    OtaErrorType.INSTALLATION_BLOCKED,
                    "Downgrade blocked: APK v$apkVerName ($apkVerCode) is older than installed v$installedVersionName ($installedVersionCode).",
                ),
            )
        }

        val installedCerts = getInstalledSignaturesSha256(context)
        val archiveCerts = extractSignatureDigests(archiveInfo)
        val signatureMatches = if (installedCerts.isNotEmpty() && archiveCerts.isNotEmpty()) {
            installedCerts.intersect(archiveCerts).isNotEmpty()
        } else {
            null
        }

        if (signatureMatches == false) {
            return ApkVerificationReport(
                isValid = false,
                sha256Hex = computedSha,
                sha256Matched = shaMatched,
                packageName = archiveInfo.packageName,
                versionName = apkVerName,
                versionCode = apkVerCode,
                signatureMatchesInstalled = false,
                errorInfo = OtaErrorInfo(
                    OtaErrorType.INSTALLATION_BLOCKED,
                    "Signing certificate mismatch! Installed app and downloaded APK use different keys.",
                ),
            )
        }

        return ApkVerificationReport(
            isValid = true,
            sha256Hex = computedSha,
            sha256Matched = shaMatched,
            packageName = archiveInfo.packageName,
            versionName = apkVerName,
            versionCode = apkVerCode,
            signatureMatchesInstalled = signatureMatches,
            errorInfo = null,
        )
    }

    @Suppress("DEPRECATION")
    private fun extractVersionCode(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledSignaturesSha256(context: Context): Set<String> {
        return runCatching {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = pm.getPackageInfo(context.packageName, flags)
            extractSignatureDigests(pkgInfo)
        }.getOrDefault(emptySet())
    }

    @Suppress("DEPRECATION")
    private fun extractSignatureDigests(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            info.signatures?.toList().orEmpty()
        }
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.map { sig ->
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }
}
