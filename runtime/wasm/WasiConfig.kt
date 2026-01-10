package com.builder.runtime.wasm

import com.builder.core.model.PackLimits
import com.builder.core.model.PackPermissions
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WASI configuration for Wasmtime.
 * Maps pack permissions to WASI capabilities.
 */
class WasiConfig {
    /**
     * Converts pack permissions to WASI configuration JSON.
     *
     * @param packId The pack ID
     * @param permissions The pack permissions
     * @param envVars Environment variables to inject
     * @param limits Resource limits
     * @return JSON configuration for Wasmtime
     */
    fun toJson(
        packId: String,
        permissions: PackPermissions,
        envVars: Map<String, String>,
        limits: PackLimits
    ): String {
        val config = WasiConfiguration(
            preopen_dirs = buildPreopenDirs(packId, permissions),
            env_vars = envVars,
            inherit_stdout = true,
            inherit_stderr = true,
            memory_limit_mb = limits.memoryMb,
            cpu_limit_ms_per_sec = limits.cpuMsPerSec
        )
        return Json.encodeToString(config)
    }

    /**
     * Builds the preopened directories list for WASI.
     * Maps pack filesystem permissions to actual device paths.
     */
    private fun buildPreopenDirs(
        packId: String,
        permissions: PackPermissions
    ): List<PreopenDir> {
        val dirs = mutableListOf<PreopenDir>()

        permissions.filesystem?.let { fs ->
            // Map read paths
            fs.read.forEach { guestPath ->
                val hostPath = "/data/data/com.builder/packs/$packId/$guestPath"
                dirs.add(PreopenDir(guest_path = guestPath, host_path = hostPath, readonly = true))
            }

            // Map write paths
            fs.write.forEach { guestPath ->
                val hostPath = "/data/data/com.builder/packs/$packId/$guestPath"
                dirs.add(PreopenDir(guest_path = guestPath, host_path = hostPath, readonly = false))
            }
        }

        return dirs
    }
}

@Serializable
data class WasiConfiguration(
    val preopen_dirs: List<PreopenDir> = emptyList(),
    val env_vars: Map<String, String> = emptyMap(),
    val inherit_stdout: Boolean = true,
    val inherit_stderr: Boolean = true,
    val memory_limit_mb: Int,
    val cpu_limit_ms_per_sec: Int
)

@Serializable
data class PreopenDir(
    val guest_path: String,
    val host_path: String,
    val readonly: Boolean
)
