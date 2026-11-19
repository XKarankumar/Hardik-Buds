package com.example.ota

import androidx.compose.runtime.Immutable

enum class OtaChannel(val label: String, val badge: String) {
    STABLE("Stable Channel", "STABLE"),
    BETA("Beta Channel", "BETA"),
}

enum class OtaCheckFrequencyHours(val label: String, val hours: Long) {
    HOURS_12("Every 12 Hours", 12L),
    HOURS_24("Every 24 Hours", 24L),
    HOURS_72("Every 72 Hours", 72L),
    MANUAL_ONLY("Manual Only", 0L),
}

@Immutable
data class OtaSettings(
    val githubOwner: String = "XKarankumar",
    val githubRepo: String = "Hardik-Buds",
    val versionJsonUrl: String = "",
    val autoCheckOnLaunch: Boolean = true,
    val backgroundCheckEnabled: Boolean = true,
    val checkFrequency: OtaCheckFrequencyHours = OtaCheckFrequencyHours.HOURS_24,
    val channel: OtaChannel = OtaChannel.STABLE,
    val notificationsEnabled: Boolean = true,
    val autoDownloadEnabled: Boolean = false,
    val downloadOverMobileData: Boolean = true,
    val lastCheckedTimestamp: Long? = null,
    val lastKnownLatestVersion: String? = null,
    val expectedSha256Override: String = "",
)

enum class OtaErrorType(
    val title: String,
    val message: String,
    val retryable: Boolean = true,
) {
    NO_INTERNET(
        title = "No Internet Connection",
        message = "Internet connection unavailable. Wi-Fi ya Mobile Data on karke Retry karein.",
        retryable = true,
    ),
    GITHUB_API_UNAVAILABLE(
        title = "GitHub API Unavailable",
        message = "GitHub Releases server abhi respond nahi kar raha ya rate-limit hua hai. Thodi der baad Retry karein.",
        retryable = true,
    ),
    RELEASE_NOT_FOUND(
        title = "Release Not Found",
        message = "Configured GitHub repository ya selected channel ke liye koi release nahi mila. Owner/Repo ya Beta toggle check karein.",
        retryable = true,
    ),
    APK_MISSING(
        title = "APK Asset Missing",
        message = "Latest GitHub release me valid .apk file attached nahi hai. Release assets check karein.",
        retryable = true,
    ),
    DOWNLOAD_INTERRUPTED(
        title = "Download Interrupted",
        message = "Network interruption ki wajah se download ruk gaya. Resume ya Retry dabakar continue karein.",
        retryable = true,
    ),
    STORAGE_UNAVAILABLE(
        title = "Storage Unavailable",
        message = "Device internal storage full hai ya writable nahi hai. Free space banakar Retry karein.",
        retryable = true,
    ),
    INVALID_APK(
        title = "Invalid or Corrupted APK",
        message = "Downloaded APK corrupt, incomplete, package ID mismatch, ya SHA-256 verification fail hui. Safety ke liye installation block ki gayi hai.",
        retryable = true,
    ),
    INSTALLATION_BLOCKED(
        title = "Installation Blocked",
        message = "Android package installer ne update block kiya (signing certificate mismatch ya version downgrade).",
        retryable = false,
    ),
    PERMISSION_DENIED(
        title = "Install Permission Required",
        message = "'Install unknown apps' permission allow karein Android Settings me taaki update install ho sake.",
        retryable = true,
    ),
}

@Immutable
data class OtaErrorInfo(
    val type: OtaErrorType,
    val detail: String? = null,
)

@Immutable
data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val htmlUrl: String,
    val apkAssetName: String?,
    val apkDownloadUrl: String?,
    val apkSizeBytes: Long,
    val expectedSha256: String? = null,
    val mandatoryUpdate: Boolean = false,
    val minimumSupportedVersion: Long = 1L,
)

enum class OtaCheckStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    ERROR,
}

enum class OtaDownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    READY_TO_INSTALL,
    FAILED,
    CANCELLED,
}

@Immutable
data class OtaDownloadState(
    val status: OtaDownloadStatus = OtaDownloadStatus.IDLE,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressFraction: Float = 0f,
    val speedBytesPerSec: Long = 0L,
    val localApkPath: String? = null,
    val computedSha256: String? = null,
    val sha256Matched: Boolean? = null,
    val verifiedPackageName: String? = null,
    val verifiedVersionName: String? = null,
    val verifiedVersionCode: Long? = null,
    val signatureMatchesInstalled: Boolean? = null,
    val error: OtaErrorInfo? = null,
)

@Immutable
data class OtaUiState(
    val currentVersionName: String = "1.0.0",
    val currentVersionCode: Long = 1L,
    val checkStatus: OtaCheckStatus = OtaCheckStatus.IDLE,
    val latestRelease: GitHubReleaseInfo? = null,
    val availableReleases: List<GitHubReleaseInfo> = emptyList(),
    val statusHeadline: String = "Ready to check GitHub Releases",
    val error: OtaErrorInfo? = null,
    val canInstallUnknownApps: Boolean = false,
)
