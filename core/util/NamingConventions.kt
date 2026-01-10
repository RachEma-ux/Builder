package com.builder.core.util

/**
 * Enforces pack naming conventions.
 * See Builder_Final.md §5 for mandatory naming convention.
 *
 * Pack format: pack-<variant>-<target>-<version>.zip
 */
object NamingConventions {
    private val PACK_NAME_REGEX = Regex(
        """^pack-([a-z0-9-]+)-([a-z0-9-]+)-(.+)\.zip$"""
    )

    /**
     * Parses a pack filename into its components.
     * @param filename The pack filename (e.g., "pack-guardrouter-android-arm64-v1.2.3.zip")
     * @return PackNaming components or null if invalid
     */
    fun parse(filename: String): PackNaming? {
        val match = PACK_NAME_REGEX.matchEntire(filename) ?: return null
        val (variant, target, version) = match.destructured
        return PackNaming(
            variant = variant,
            target = target,
            version = version,
            filename = filename
        )
    }

    /**
     * Constructs a pack filename from components.
     * @param variant Pack variant (e.g., "guardrouter")
     * @param target Target platform (e.g., "android-arm64")
     * @param version Version string (e.g., "v1.2.3" or "0.0.0-dev+abc123")
     * @return Pack filename
     */
    fun construct(variant: String, target: String, version: String): String {
        require(variant.matches(Regex("[a-z0-9-]+"))) {
            "Invalid variant: $variant (must be lowercase alphanumeric with hyphens)"
        }
        require(target.matches(Regex("[a-z0-9-]+"))) {
            "Invalid target: $target (must be lowercase alphanumeric with hyphens)"
        }
        require(version.isNotBlank()) {
            "Version cannot be blank"
        }
        return "pack-$variant-$target-$version.zip"
    }

    /**
     * Validates a pack filename.
     * @param filename The pack filename to validate
     * @return true if valid, false otherwise
     */
    fun isValid(filename: String): Boolean = parse(filename) != null

    /**
     * Validates and throws if invalid.
     * @param filename The pack filename to validate
     * @throws IllegalArgumentException if invalid
     */
    fun validateOrThrow(filename: String) {
        require(isValid(filename)) {
            "Invalid pack filename: $filename. " +
                    "Expected format: pack-<variant>-<target>-<version>.zip"
        }
    }
}

/**
 * Represents parsed pack naming components.
 */
data class PackNaming(
    val variant: String,
    val target: String,
    val version: String,
    val filename: String
)
