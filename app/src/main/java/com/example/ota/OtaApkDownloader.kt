package com.example.ota

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class OtaApkDownloader(
    private val context: Context,
    private val scope: CoroutineScope,
    private val client: OkHttpClient = defaultDownloadClient(),
) {
    private val _state = MutableStateFlow(OtaDownloadState())
    val state: StateFlow<OtaDownloadState> = _state.asStateFlow()

    private var activeJob: Job? = null
    private val pauseRequested = AtomicBoolean(false)
    private val cancelRequested = AtomicBoolean(false)

    private var lastRelease: GitHubReleaseInfo? = null
    private var lastExpectedSha256: String? = null
    private var lastInstalledVersionCode: Long = 1L
    private var lastInstalledVersionName: String = "1.0.0"

    private val updatesDir: File
        get() = File(context.filesDir, "ota_updates").apply { if (!exists()) mkdirs() }

    private fun partFile(): File = File(updatesDir, "update_pending.apk.part")
    private fun finalApkFile(): File = File(updatesDir, "hardik_buds_pro_update.apk")

    fun startOrResumeDownload(
        release: GitHubReleaseInfo,
        expectedSha256: String?,
        installedVersionCode: Long,
        installedVersionName: String,
        resumeExisting: Boolean = false,
        onCompletedVerified: (OtaDownloadState) -> Unit = {},
        onFailed: (OtaErrorInfo) -> Unit = {},
    ) {
        if (activeJob?.isActive == true) {
            return // Prevent duplicate concurrent downloads
        }

        val url = release.apkDownloadUrl
        if (!SemVerComparator.isTrustedGitHubDownloadUrl(url)) {
            val err = OtaErrorInfo(OtaErrorType.APK_MISSING, "Only verified HTTPS GitHub APK download URLs are permitted.")
            _state.value = _state.value.copy(status = OtaDownloadStatus.FAILED, error = err)
            onFailed(err)
            return
        }

        lastRelease = release
        lastExpectedSha256 = expectedSha256 ?: release.expectedSha256
        lastInstalledVersionCode = installedVersionCode
        lastInstalledVersionName = installedVersionName

        pauseRequested.set(false)
        cancelRequested.set(false)

        activeJob = scope.launch(Dispatchers.IO) {
            val part = partFile()
            val finalFile = finalApkFile()

            if (!resumeExisting) {
                part.delete()
                finalFile.delete()
            }

            val requiredBytes = if (release.apkSizeBytes > 0L) release.apkSizeBytes + 4L * 1024L * 1024L else 20L * 1024L * 1024L
            if (!hasEnoughStorage(requiredBytes)) {
                val err = OtaErrorInfo(OtaErrorType.STORAGE_UNAVAILABLE)
                _state.value = OtaDownloadState(status = OtaDownloadStatus.FAILED, error = err)
                onFailed(err)
                return@launch
            }

            var existingBytes = if (resumeExisting && part.exists()) part.length() else 0L
            if (release.apkSizeBytes > 0L && existingBytes > release.apkSizeBytes) {
                part.delete()
                existingBytes = 0L
            }

            _state.value = OtaDownloadState(
                status = OtaDownloadStatus.DOWNLOADING,
                downloadedBytes = existingBytes,
                totalBytes = release.apkSizeBytes,
                progressFraction = if (release.apkSizeBytes > 0L) (existingBytes.toFloat() / release.apkSizeBytes).coerceIn(0f, 1f) else 0f,
            )

            val requestBuilder = Request.Builder()
                .url(url!!)
                .header("User-Agent", "HardikBudsPro-Android-OTA/1.0")
                .get()
            if (existingBytes > 0L) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            try {
                client.newCall(requestBuilder.build()).execute().use { response ->
                    val code = response.code
                    if (code != 200 && code != 206) {
                        val errType = if (code == 404) OtaErrorType.APK_MISSING else OtaErrorType.DOWNLOAD_INTERRUPTED
                        val err = OtaErrorInfo(errType, "Server returned HTTP $code during APK download.")
                        _state.value = _state.value.copy(status = OtaDownloadStatus.FAILED, error = err)
                        onFailed(err)
                        return@launch
                    }

                    val body = response.body ?: run {
                        val err = OtaErrorInfo(OtaErrorType.DOWNLOAD_INTERRUPTED, "Empty response body.")
                        _state.value = _state.value.copy(status = OtaDownloadStatus.FAILED, error = err)
                        onFailed(err)
                        return@launch
                    }

                    val isPartial = code == 206 && existingBytes > 0L
                    if (!isPartial) {
                        part.delete()
                        existingBytes = 0L
                    }

                    val contentLength = body.contentLength()
                    val totalBytes = when {
                        isPartial && contentLength > 0L -> existingBytes + contentLength
                        contentLength > 0L -> contentLength
                        else -> release.apkSizeBytes
                    }

                    var downloaded = existingBytes
                    var windowBytes = 0L
                    var lastReportTime = System.currentTimeMillis()
                    var currentSpeed = 0L

                    RandomAccessFile(part, "rw").use { raf ->
                        raf.seek(existingBytes)
                        body.byteStream().use { input ->
                            val buffer = ByteArray(16 * 1024)
                            while (isActive && !pauseRequested.get() && !cancelRequested.get()) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                raf.write(buffer, 0, read)
                                downloaded += read
                                windowBytes += read

                                val now = System.currentTimeMillis()
                                val elapsed = now - lastReportTime
                                if (elapsed >= 180L) {
                                    currentSpeed = (windowBytes * 1000L) / elapsed.coerceAtLeast(1L)
                                    windowBytes = 0L
                                    lastReportTime = now
                                    val progress = if (totalBytes > 0L) (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                                    _state.value = OtaDownloadState(
                                        status = OtaDownloadStatus.DOWNLOADING,
                                        downloadedBytes = downloaded,
                                        totalBytes = totalBytes,
                                        progressFraction = progress,
                                        speedBytesPerSec = currentSpeed,
                                    )
                                }
                            }
                        }
                    }

                    if (cancelRequested.get()) {
                        part.delete()
                        _state.value = OtaDownloadState(status = OtaDownloadStatus.CANCELLED)
                        return@launch
                    }

                    if (pauseRequested.get()) {
                        val progress = if (totalBytes > 0L) (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                        _state.value = OtaDownloadState(
                            status = OtaDownloadStatus.PAUSED,
                            downloadedBytes = downloaded,
                            totalBytes = totalBytes,
                            progressFraction = progress,
                            speedBytesPerSec = 0L,
                        )
                        return@launch
                    }

                    // Move completed part file to final verified APK path
                    if (finalFile.exists()) finalFile.delete()
                    if (!part.renameTo(finalFile)) {
                        part.copyTo(finalFile, overwrite = true)
                        part.delete()
                    }

                    _state.value = OtaDownloadState(
                        status = OtaDownloadStatus.VERIFYING,
                        downloadedBytes = finalFile.length(),
                        totalBytes = totalBytes.coerceAtLeast(finalFile.length()),
                        progressFraction = 1f,
                        speedBytesPerSec = 0L,
                    )

                    val report = OtaSecurityVerifier.verifyDownloadedApk(
                        context = context,
                        apkFile = finalFile,
                        expectedSizeBytes = release.apkSizeBytes,
                        expectedSha256 = lastExpectedSha256,
                        installedVersionCode = installedVersionCode,
                        installedVersionName = installedVersionName,
                    )

                    if (!report.isValid) {
                        finalFile.delete()
                        val err = report.errorInfo ?: OtaErrorInfo(OtaErrorType.INVALID_APK)
                        _state.value = OtaDownloadState(
                            status = OtaDownloadStatus.FAILED,
                            downloadedBytes = 0L,
                            totalBytes = totalBytes,
                            progressFraction = 0f,
                            computedSha256 = report.sha256Hex.takeIf { it.isNotBlank() },
                            sha256Matched = report.sha256Matched,
                            signatureMatchesInstalled = report.signatureMatchesInstalled,
                            error = err,
                        )
                        onFailed(err)
                    } else {
                        val readyState = OtaDownloadState(
                            status = OtaDownloadStatus.READY_TO_INSTALL,
                            downloadedBytes = finalFile.length(),
                            totalBytes = totalBytes.coerceAtLeast(finalFile.length()),
                            progressFraction = 1f,
                            speedBytesPerSec = 0L,
                            localApkPath = finalFile.absolutePath,
                            computedSha256 = report.sha256Hex,
                            sha256Matched = report.sha256Matched,
                            verifiedPackageName = report.packageName,
                            verifiedVersionName = report.versionName,
                            verifiedVersionCode = report.versionCode,
                            signatureMatchesInstalled = report.signatureMatchesInstalled,
                            error = null,
                        )
                        _state.value = readyState
                        onCompletedVerified(readyState)
                    }
                }
            } catch (e: CancellationException) {
                if (cancelRequested.get()) {
                    part.delete()
                    _state.value = OtaDownloadState(status = OtaDownloadStatus.CANCELLED)
                } else if (pauseRequested.get()) {
                    val bytes = part.length()
                    val tot = release.apkSizeBytes
                    _state.value = OtaDownloadState(
                        status = OtaDownloadStatus.PAUSED,
                        downloadedBytes = bytes,
                        totalBytes = tot,
                        progressFraction = if (tot > 0L) (bytes.toFloat() / tot).coerceIn(0f, 1f) else 0f,
                    )
                }
            } catch (e: IOException) {
                val err = OtaErrorInfo(OtaErrorType.DOWNLOAD_INTERRUPTED, e.message)
                _state.value = _state.value.copy(
                    status = OtaDownloadStatus.FAILED,
                    speedBytesPerSec = 0L,
                    error = err,
                )
                onFailed(err)
            }
        }
    }

    fun pauseDownload() {
        if (activeJob?.isActive == true) {
            pauseRequested.set(true)
        }
    }

    fun resumeDownload(
        onCompletedVerified: (OtaDownloadState) -> Unit = {},
        onFailed: (OtaErrorInfo) -> Unit = {},
    ) {
        val rel = lastRelease ?: return
        startOrResumeDownload(
            release = rel,
            expectedSha256 = lastExpectedSha256,
            installedVersionCode = lastInstalledVersionCode,
            installedVersionName = lastInstalledVersionName,
            resumeExisting = true,
            onCompletedVerified = onCompletedVerified,
            onFailed = onFailed,
        )
    }

    fun cancelDownload() {
        cancelRequested.set(true)
        pauseRequested.set(false)
        activeJob?.cancel()
        partFile().delete()
        finalApkFile().delete()
        _state.value = OtaDownloadState(status = OtaDownloadStatus.CANCELLED)
    }

    fun retryDownload(
        onCompletedVerified: (OtaDownloadState) -> Unit = {},
        onFailed: (OtaErrorInfo) -> Unit = {},
    ) {
        val rel = lastRelease ?: return
        val canResume = partFile().exists() && partFile().length() > 0L
        startOrResumeDownload(
            release = rel,
            expectedSha256 = lastExpectedSha256,
            installedVersionCode = lastInstalledVersionCode,
            installedVersionName = lastInstalledVersionName,
            resumeExisting = canResume,
            onCompletedVerified = onCompletedVerified,
            onFailed = onFailed,
        )
    }

    private fun hasEnoughStorage(requiredBytes: Long): Boolean {
        return runCatching {
            val stat = StatFs(updatesDir.absolutePath)
            stat.availableBytes >= requiredBytes
        }.getOrDefault(true)
    }

    companion object {
        fun defaultDownloadClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
