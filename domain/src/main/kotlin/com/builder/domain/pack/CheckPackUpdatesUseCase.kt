package com.builder.domain.pack

import com.builder.core.model.Pack
import com.builder.core.repository.GitHubRepository
import com.builder.core.repository.PackRepository
import com.builder.core.util.DebugLogger
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Data class representing an available pack update.
 */
data class PackUpdate(
    val pack: Pack,
    val currentVersion: String,
    val latestVersion: String,
    val latestRef: String,
    val releaseNotes: String? = null
)

/**
 * Use case for checking if installed packs have updates available.
 */
class CheckPackUpdatesUseCase @Inject constructor(
    private val packRepository: PackRepository,
    private val gitHubRepository: GitHubRepository
) {
    /**
     * Checks all installed packs for available updates.
     * @return List of packs with available updates
     */
    suspend operator fun invoke(): List<PackUpdate> {
        val updates = mutableListOf<PackUpdate>()

        try {
            val packs = packRepository.getAllPacksFlow().first()

            for (pack in packs) {
                val update = checkPackForUpdate(pack)
                if (update != null) {
                    updates.add(update)
                }
            }
        } catch (e: Exception) {
            DebugLogger.logSync("ERROR", "UpdateCheck", "Failed to check updates: ${e.message}")
        }

        return updates
    }

    /**
     * Checks a single pack for updates.
     * @return PackUpdate if an update is available, null otherwise
     */
    suspend fun checkPackForUpdate(pack: Pack): PackUpdate? {
        try {
            // Extract owner/repo from source URL
            val regex = Regex("github\\.com/([^/]+)/([^/]+)")
            val match = regex.find(pack.installSource.sourceUrl)
            if (match == null) {
                DebugLogger.logSync("WARN", "UpdateCheck", "Could not parse repo from URL: ${pack.installSource.sourceUrl}")
                return null
            }

            val owner = match.groupValues[1]
            val repo = match.groupValues[2]

            // Get releases
            val releasesResult = gitHubRepository.listReleases(owner, repo)
            if (releasesResult.isFailure) {
                return null
            }

            val releases = releasesResult.getOrNull() ?: return null
            if (releases.isEmpty()) {
                return null
            }

            // Find the latest release that contains a pack asset
            val latestRelease = releases.firstOrNull { release ->
                release.assets.any { asset ->
                    asset.name.startsWith("pack-") && asset.name.endsWith(".zip")
                }
            } ?: return null

            val latestTag = latestRelease.tagName
            val currentRef = pack.installSource.sourceRef

            // Compare versions
            if (isNewerVersion(latestTag, currentRef)) {
                DebugLogger.logSync("INFO", "UpdateCheck", "Update available for ${pack.name}: $currentRef -> $latestTag")
                return PackUpdate(
                    pack = pack,
                    currentVersion = pack.version,
                    latestVersion = extractVersion(latestTag),
                    latestRef = latestTag,
                    releaseNotes = latestRelease.body?.take(500)
                )
            }
        } catch (e: Exception) {
            DebugLogger.logSync("ERROR", "UpdateCheck", "Error checking ${pack.name}: ${e.message}")
        }

        return null
    }

    /**
     * Compares two version strings/tags to determine if the new one is newer.
     */
    private fun isNewerVersion(newTag: String, currentRef: String): Boolean {
        // Don't suggest update to same version
        if (newTag == currentRef) return false

        // Try semantic version comparison
        val newVersion = extractVersion(newTag)
        val currentVersion = extractVersion(currentRef)

        try {
            val newParts = newVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

            // Compare major.minor.patch
            for (i in 0 until maxOf(newParts.size, currentParts.size)) {
                val newPart = newParts.getOrElse(i) { 0 }
                val currentPart = currentParts.getOrElse(i) { 0 }
                if (newPart > currentPart) return true
                if (newPart < currentPart) return false
            }
        } catch (e: Exception) {
            // Fall back to string comparison
            return newTag > currentRef
        }

        return false
    }

    /**
     * Extracts version number from a tag string.
     * e.g., "v1.2.0" -> "1.2.0", "hellonewapp-v1.2.0" -> "1.2.0"
     */
    private fun extractVersion(tag: String): String {
        val versionRegex = Regex("v?(\\d+\\.\\d+\\.\\d+)")
        val match = versionRegex.find(tag)
        return match?.groupValues?.get(1) ?: tag
    }
}
