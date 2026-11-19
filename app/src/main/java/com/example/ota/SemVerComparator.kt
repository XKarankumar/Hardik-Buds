package com.example.ota

import java.net.URI

data class ParsedSemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null,
) : Comparable<ParsedSemVer> {

    override fun compareTo(other: ParsedSemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        return when {
            prerelease == null && other.prerelease == null -> 0
            prerelease == null -> 1 // stable > prerelease
            other.prerelease == null -> -1
            else -> prerelease.compareTo(other.prerelease, ignoreCase = true)
        }
    }

    val normalizedString: String
        get() = buildString {
            append("$major.$minor.$patch")
            if (!prerelease.isNullOrBlank()) append("-$prerelease")
        }
}

object SemVerComparator {

    private val VERSION_REGEX = Regex("""^[vV]?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.\-]+))?""")
    private val SHA256_REGEX = Regex("""(?i)\b([a-f0-9]{64})\b""")
    private val REPO_SEGMENT_REGEX = Regex("""^[A-Za-z0-9_.\-]+$""")

    fun parse(rawVersion: String?): ParsedSemVer? {
        val cleaned = rawVersion?.trim()?.removePrefix("V")?.removePrefix("v") ?: return null
        val match = VERSION_REGEX.find(cleaned) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: 0
        val patch = match.groupValues[3].toIntOrNull() ?: 0
        val pre = match.groupValues[4].takeIf { it.isNotBlank() }
        return ParsedSemVer(major, minor, patch, pre)
    }

    fun isNewerVersion(candidateTagOrVersion: String?, currentVersion: String): Boolean {
        val candidate = parse(candidateTagOrVersion) ?: return false
        val current = parse(currentVersion) ?: return false
        return candidate > current
    }

    fun normalizeVersionLabel(rawVersion: String?): String {
        return parse(rawVersion)?.normalizedString ?: (rawVersion?.trim()?.removePrefix("v")?.removePrefix("V") ?: "0.0.0")
    }

    fun isValidGitHubSegment(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 100 && REPO_SEGMENT_REGEX.matches(trimmed)
    }

    fun isSecureHttpsUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        return runCatching {
            val uri = URI(rawUrl.trim())
            uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }

    fun isTrustedGitHubDownloadUrl(rawUrl: String?): Boolean {
        if (!isSecureHttpsUrl(rawUrl)) return false
        val host = runCatching { URI(rawUrl!!.trim()).host?.lowercase() }.getOrNull() ?: return false
        return host == "github.com" ||
            host.endsWith(".github.com") ||
            host.endsWith(".githubusercontent.com")
    }

    fun extractSha256FromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return SHA256_REGEX.find(text)?.groupValues?.getOrNull(1)?.lowercase()
    }
}
