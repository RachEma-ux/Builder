package com.builder.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the packs.index.json file structure for production releases.
 * See Builder_Final.md §6 for complete specification.
 */
@Serializable
data class PackIndex(
    @SerialName("index_version")
    val indexVersion: String,

    @SerialName("default_variant")
    val defaultVariant: String,

    val targets: Map<String, TargetPreferences>,

    val variants: Map<String, VariantDefinition>
) {
    /**
     * Resolves the asset name for a given variant and target.
     * @param variantName The variant to install (or null for default)
     * @param deviceTarget The device target (e.g., "android-arm64")
     * @param version The version tag (e.g., "v1.2.3")
     * @return The resolved asset filename
     * @throws IllegalArgumentException if variant or target not found
     */
    fun resolveAsset(
        variantName: String? = null,
        deviceTarget: String,
        version: String
    ): String {
        val variant = variantName ?: defaultVariant
        val variantDef = variants[variant]
            ?: throw IllegalArgumentException("Variant not found: $variant")

        val targetDef = variantDef.targets[deviceTarget]
            ?: throw IllegalArgumentException(
                "Target $deviceTarget not found for variant $variant"
            )

        // Replace {version} placeholder in asset template
        return targetDef.asset.replace("{version}", version)
    }

    /**
     * Validates the index structure.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(indexVersion.isNotBlank()) { "Index version cannot be blank" }
        require(defaultVariant.isNotBlank()) { "Default variant cannot be blank" }
        require(defaultVariant in variants) {
            "Default variant '$defaultVariant' not found in variants"
        }
        require(variants.isNotEmpty()) { "Variants cannot be empty" }
        require(targets.isNotEmpty()) { "Targets cannot be empty" }
    }
}

@Serializable
data class TargetPreferences(
    val preferred: List<String>
)

@Serializable
data class VariantDefinition(
    val name: String,
    val type: String,
    val targets: Map<String, TargetAsset>
)

@Serializable
data class TargetAsset(
    val asset: String
)
