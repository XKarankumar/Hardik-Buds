package com.example.ota

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class GitHubReleasesFetchResult(
    val latestForChannel: GitHubReleaseInfo?,
    val allReleases: List<GitHubReleaseInfo>,
    val error: OtaErrorInfo? = null,
)

class GitHubReleaseService(
    private val context: Context,
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun fetchReleases(
        owner: String,
        repo: String,
        channel: OtaChannel,
    ): GitHubReleasesFetchResult = withContext(Dispatchers.IO) {
        val cleanOwner = owner.trim()
        val cleanRepo = repo.trim()
        if (!SemVerComparator.isValidGitHubSegment(cleanOwner) || !SemVerComparator.isValidGitHubSegment(cleanRepo)) {
            return@withContext GitHubReleasesFetchResult(
                latestForChannel = null,
                allReleases = emptyList(),
                error = OtaErrorInfo(OtaErrorType.RELEASE_NOT_FOUND, "Invalid GitHub owner/repository format."),
            )
        }

        if (!isNetworkConnected()) {
            return@withContext GitHubReleasesFetchResult(
                latestForChannel = null,
                allReleases = emptyList(),
                error = OtaErrorInfo(OtaErrorType.NO_INTERNET),
            )
        }

        val apiUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/releases?per_page=15"
        if (!SemVerComparator.isSecureHttpsUrl(apiUrl)) {
            return@withContext GitHubReleasesFetchResult(
                latestForChannel = null,
                allReleases = emptyList(),
                error = OtaErrorInfo(OtaErrorType.GITHUB_API_UNAVAILABLE, "Non-HTTPS API URL blocked."),
            )
        }

        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "HardikBudsPro-Android-OTA/1.0")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val rawJson = response.body?.string().orEmpty()
                        val parsed = parseReleasesJson(rawJson, channel)
                        if (parsed.allReleases.isEmpty()) {
                            return@withContext GitHubReleasesFetchResult(
                                latestForChannel = null,
                                allReleases = emptyList(),
                                error = OtaErrorInfo(
                                    OtaErrorType.RELEASE_NOT_FOUND,
                                    "No published GitHub releases found in $cleanOwner/$cleanRepo.",
                                ),
                            )
                        }
                        val target = parsed.latestForChannel
                        if (target == null) {
                            return@withContext GitHubReleasesFetchResult(
                                latestForChannel = null,
                                allReleases = parsed.allReleases,
                                error = OtaErrorInfo(
                                    OtaErrorType.RELEASE_NOT_FOUND,
                                    "No ${channel.label} release found. Try enabling Beta Channel if pre-releases exist.",
                                ),
                            )
                        }
                        if (target.apkDownloadUrl.isNullOrBlank()) {
                            return@withContext GitHubReleasesFetchResult(
                                latestForChannel = target,
                                allReleases = parsed.allReleases,
                                error = OtaErrorInfo(
                                    OtaErrorType.APK_MISSING,
                                    "Release ${target.tagName} does not contain a valid HTTPS .apk asset.",
                                ),
                            )
                        }
                        // If SHA-256 wasn't in release notes, check if a companion .sha256 asset was published
                        val hydratedTarget = if (target.expectedSha256 == null) {
                            val shaFromAsset = fetchCompanionSha256IfPresent(rawJson, target.tagName)
                            if (shaFromAsset != null) target.copy(expectedSha256 = shaFromAsset) else target
                        } else {
                            target
                        }
                        GitHubReleasesFetchResult(
                            latestForChannel = hydratedTarget,
                            allReleases = parsed.allReleases,
                            error = null,
                        )
                    }
                    404 -> GitHubReleasesFetchResult(
                        latestForChannel = null,
                        allReleases = emptyList(),
                        error = OtaErrorInfo(
                            OtaErrorType.RELEASE_NOT_FOUND,
                            "GitHub returned 404 for $cleanOwner/$cleanRepo releases.",
                        ),
                    )
                    403, 429 -> GitHubReleasesFetchResult(
                        latestForChannel = null,
                        allReleases = emptyList(),
                        error = OtaErrorInfo(
                            OtaErrorType.GITHUB_API_UNAVAILABLE,
                            "GitHub API rate limit exceeded (HTTP ${response.code}).",
                        ),
                    )
                    else -> GitHubReleasesFetchResult(
                        latestForChannel = null,
                        allReleases = emptyList(),
                        error = OtaErrorInfo(
                            OtaErrorType.GITHUB_API_UNAVAILABLE,
                            "GitHub API returned HTTP ${response.code}.",
                        ),
                    )
                }
            }
        } catch (e: UnknownHostException) {
            GitHubReleasesFetchResult(
                latestForChannel = null,
                allReleases = emptyList(),
                error = OtaErrorInfo(OtaErrorType.NO_INTERNET, e.message),
            )
        } catch (e: SocketTimeoutException) {
            GitHubReleasesFetchResult(
                latestForChannel = null,
                allReleases = emptyList(),
                error = OtaErrorInfo(OtaErrorType.GITHUB_API_UNAVAILABLE, "Connection timed out."),
            )
        } catch (e: IOException) {
            val type = if (!isNetworkConnected()) OtaErrorType.NO_INTERNET else OtaErrorType.GITHUB_API_UNAVAILABLE
            GitHubReleasesFetchResult(
                latestForChannel = null,
                allReleases = emptyList(),
                error = OtaErrorInfo(type, e.message),
            )
        }
    }

    private fun fetchCompanionSha256IfPresent(rawJson: String, tagName: String): String? {
        return runCatching {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val rel = arr.getJSONObject(i)
                if (rel.optString("tag_name") != tagName) continue
                val assets = rel.optJSONArray("assets") ?: continue
                for (a in 0 until assets.length()) {
                    val asset = assets.getJSONObject(a)
                    val name = asset.optString("name")
                    val url = asset.optString("browser_download_url")
                    if (name.endsWith(".sha256", ignoreCase = true) && SemVerComparator.isTrustedGitHubDownloadUrl(url)) {
                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", "HardikBudsPro-Android-OTA/1.0")
                            .get()
                            .build()
                        client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                return@runCatching SemVerComparator.extractSha256FromText(resp.body?.string())
                            }
                        }
                    }
                }
            }
            null
        }.getOrNull()
    }

    fun isNetworkConnected(): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        fun parseReleasesJson(rawJson: String, channel: OtaChannel): GitHubReleasesFetchResult {
            val releases = mutableListOf<GitHubReleaseInfo>()
            val array = runCatching { JSONArray(rawJson) }.getOrNull()
                ?: return GitHubReleasesFetchResult(null, emptyList())

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optBoolean("draft", false)) continue

                val tagName = obj.optString("tag_name").ifBlank { continue }
                val prerelease = obj.optBoolean("prerelease", false)
                val htmlUrl = obj.optString("html_url")
                if (!SemVerComparator.isSecureHttpsUrl(htmlUrl)) continue

                val releaseTitle = obj.optString("name").ifBlank { tagName }
                val body = obj.optString("body").ifBlank { "Bug fixes and stability improvements." }
                val publishedAt = obj.optString("published_at").take(10).ifBlank { "Recent" }

                val assets = obj.optJSONArray("assets")
                var chosenApkName: String? = null
                var chosenApkUrl: String? = null
                var chosenApkSize = 0L

                if (assets != null) {
                    for (j in 0 until assets.length()) {
                        val asset = assets.optJSONObject(j) ?: continue
                        val name = asset.optString("name")
                        val url = asset.optString("browser_download_url")
                        val size = asset.optLong("size", 0L)
                        if (name.endsWith(".apk", ignoreCase = true) && SemVerComparator.isTrustedGitHubDownloadUrl(url)) {
                            // Prefer branded / release APK if multiple APKs exist
                            val isPreferred = name.contains("Hardik", ignoreCase = true) ||
                                name.contains("release", ignoreCase = true)
                            if (chosenApkUrl == null || isPreferred) {
                                chosenApkName = name
                                chosenApkUrl = url
                                chosenApkSize = size
                                if (isPreferred) break
                            }
                        }
                    }
                }

                val isMandatory = body.contains("mandatoryUpdate: true", ignoreCase = true) ||
                    body.contains("mandatory: true", ignoreCase = true) ||
                    body.contains("critical: true", ignoreCase = true)

                releases.add(
                    GitHubReleaseInfo(
                        tagName = tagName,
                        versionName = SemVerComparator.normalizeVersionLabel(tagName),
                        releaseTitle = releaseTitle,
                        releaseNotes = body,
                        publishedAt = publishedAt,
                        isPrerelease = prerelease,
                        htmlUrl = htmlUrl,
                        apkAssetName = chosenApkName,
                        apkDownloadUrl = chosenApkUrl,
                        apkSizeBytes = chosenApkSize,
                        expectedSha256 = SemVerComparator.extractSha256FromText(body),
                        mandatoryUpdate = isMandatory,
                        minimumSupportedVersion = if (isMandatory) 2L else 1L,
                    ),
                )
            }

            val eligible = releases.filter { rel ->
                channel == OtaChannel.BETA || !rel.isPrerelease
            }
            val latest = eligible.maxWithOrNull { a, b ->
                val va = SemVerComparator.parse(a.tagName)
                val vb = SemVerComparator.parse(b.tagName)
                when {
                    va != null && vb != null -> va.compareTo(vb)
                    else -> a.publishedAt.compareTo(b.publishedAt)
                }
            }

            return GitHubReleasesFetchResult(
                latestForChannel = latest,
                allReleases = releases,
                error = null,
            )
        }
    }
}
