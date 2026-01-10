package com.builder.core.model

/**
 * Represents an installed Pack on the device.
 * See Builder_Final.md §4 for complete specification.
 */
data class Pack(
    val id: String,
    val name: String,
    val version: String,
    val type: PackType,
    val manifest: PackManifest,
    val installSource: InstallSource,
    val installPath: String,
    val checksumSha256: String
) {
    /**
     * Returns the display name with version.
     */
    fun displayName(): String = "$name ($version)"

    /**
     * Returns a badge-friendly mode name.
     */
    fun modeBadge(): String = installSource.getMode().name

    /**
     * Checks if this pack requires any environment secrets.
     */
    fun requiresSecrets(): Boolean = manifest.requiredEnv.isNotEmpty()

    /**
     * Returns list of required secret keys.
     */
    fun requiredSecrets(): List<String> = manifest.requiredEnv
}
