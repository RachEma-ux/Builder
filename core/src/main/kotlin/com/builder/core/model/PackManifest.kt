package com.builder.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the pack.json manifest file structure.
 * See Builder_Final.md §4 for complete specification.
 */
@Serializable
data class PackManifest(
    @SerialName("pack_version")
    val packVersion: Int,

    val id: String,

    val name: String,

    val version: String,

    val type: PackType,

    val entry: String,

    val permissions: PackPermissions = PackPermissions(),

    val limits: PackLimits,

    @SerialName("required_env")
    val requiredEnv: List<String> = emptyList(),

    val build: BuildMetadata
) {
    /**
     * Validates the manifest according to spec requirements.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(id.isNotBlank()) { "Pack ID cannot be blank" }
        require(name.isNotBlank()) { "Pack name cannot be blank" }
        require(version.isNotBlank()) { "Pack version cannot be blank" }
        require(entry.isNotBlank()) { "Entry point cannot be blank" }

        // Validate entry matches type
        when (type) {
            PackType.WASM -> require(entry.endsWith(".wasm")) {
                "WASM pack entry must end with .wasm, got: $entry"
            }
            PackType.WORKFLOW -> require(entry.endsWith(".json")) {
                "Workflow pack entry must end with .json, got: $entry"
            }
        }

        // Validate limits
        require(limits.memoryMb > 0) { "Memory limit must be positive" }
        require(limits.cpuMsPerSec > 0) { "CPU limit must be positive" }

        // Validate permissions
        permissions.validate()
    }
}

@Serializable
enum class PackType {
    @SerialName("wasm")
    WASM,

    @SerialName("workflow")
    WORKFLOW
}

@Serializable
data class PackPermissions(
    val filesystem: FilesystemPermissions? = null,
    val network: NetworkPermissions? = null
) {
    fun validate() {
        filesystem?.validate()
        network?.validate()
    }
}

@Serializable
data class FilesystemPermissions(
    val read: List<String> = emptyList(),
    val write: List<String> = emptyList()
) {
    fun validate() {
        // Ensure no absolute paths or parent directory references
        val allPaths = read + write
        allPaths.forEach { path ->
            require(!path.startsWith("/")) {
                "Filesystem paths must be relative, got: $path"
            }
            require(!path.contains("..")) {
                "Filesystem paths cannot contain .., got: $path"
            }
        }
    }
}

@Serializable
data class NetworkPermissions(
    val connect: List<String> = emptyList(),

    @SerialName("listen_localhost")
    val listenLocalhost: Boolean = false
) {
    fun validate() {
        // Validate URLs are well-formed
        connect.forEach { url ->
            require(url.startsWith("http://") || url.startsWith("https://")) {
                "Network connect URLs must start with http:// or https://, got: $url"
            }
        }
    }
}

@Serializable
data class PackLimits(
    @SerialName("memory_mb")
    val memoryMb: Int,

    @SerialName("cpu_ms_per_sec")
    val cpuMsPerSec: Int
)

@Serializable
data class BuildMetadata(
    @SerialName("git_sha")
    val gitSha: String,

    @SerialName("built_at")
    val builtAt: String,

    val target: String
)
