package com.builder.core.model

import kotlinx.serialization.Serializable

/**
 * Represents the install mode (Dev vs Production).
 * See Builder_Final.md §1 and Appendix A for specification.
 */
enum class InstallMode {
    /**
     * DEV mode: Installed from workflow artifacts.
     * Source ref is a branch or commit.
     * Explicitly ephemeral.
     */
    DEV,

    /**
     * PROD mode: Installed from release assets.
     * Source ref is a Git tag.
     * Immutable and auditable.
     */
    PROD
}

/**
 * Tracks the source of a pack installation.
 */
@Serializable
data class InstallSource(
    val mode: String, // Serialized as string for Room compatibility

    /**
     * For DEV: branch name or commit SHA
     * For PROD: Git tag (e.g., "v1.2.3")
     */
    val sourceRef: String,

    /**
     * URL of the artifact/release asset
     */
    val sourceUrl: String,

    /**
     * Timestamp of installation (epoch millis)
     */
    val installedAt: Long
) {
    fun getMode(): InstallMode = InstallMode.valueOf(mode)

    companion object {
        fun dev(branch: String, artifactUrl: String, timestamp: Long = System.currentTimeMillis()) =
            InstallSource(
                mode = InstallMode.DEV.name,
                sourceRef = branch,
                sourceUrl = artifactUrl,
                installedAt = timestamp
            )

        fun prod(tag: String, releaseUrl: String, timestamp: Long = System.currentTimeMillis()) =
            InstallSource(
                mode = InstallMode.PROD.name,
                sourceRef = tag,
                sourceUrl = releaseUrl,
                installedAt = timestamp
            )
    }
}
