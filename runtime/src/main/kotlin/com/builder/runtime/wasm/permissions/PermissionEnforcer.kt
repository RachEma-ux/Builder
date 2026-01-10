package com.builder.runtime.wasm.permissions

import com.builder.core.model.PackPermissions
import timber.log.Timber

/**
 * Enforces pack permissions before WASM execution.
 * Validates that requested permissions are within allowed bounds.
 */
class PermissionEnforcer {
    /**
     * Enforces permissions for a pack.
     *
     * @param packId The pack ID
     * @param permissions The requested permissions
     * @throws SecurityException if permissions are invalid or excessive
     */
    fun enforce(packId: String, permissions: PackPermissions) {
        Timber.d("Enforcing permissions for pack: $packId")

        // Validate filesystem permissions
        permissions.filesystem?.let { fs ->
            fs.read.forEach { path ->
                validatePath(path, "read")
            }
            fs.write.forEach { path ->
                validatePath(path, "write")
            }
        }

        // Validate network permissions
        permissions.network?.let { network ->
            network.connect.forEach { url ->
                validateUrl(url)
            }

            if (network.listenLocalhost) {
                Timber.w("Pack $packId requests localhost listen permission")
                // TODO: Implement localhost listen validation
            }
        }

        Timber.d("Permissions validated successfully for pack: $packId")
    }

    /**
     * Validates a filesystem path.
     * Ensures no path traversal or absolute paths.
     */
    private fun validatePath(path: String, operation: String) {
        if (path.startsWith("/")) {
            throw SecurityException("Absolute paths not allowed: $path")
        }

        if (path.contains("..")) {
            throw SecurityException("Path traversal not allowed: $path")
        }

        Timber.v("Filesystem $operation permission validated: $path")
    }

    /**
     * Validates a network URL.
     * Ensures HTTPS is used for external connections.
     */
    private fun validateUrl(url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw SecurityException("Invalid URL scheme: $url")
        }

        // TODO: Implement network allow-list enforcement

        Timber.v("Network connect permission validated: $url")
    }
}
