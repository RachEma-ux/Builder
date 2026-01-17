package com.builder.core.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PackManifest validation.
 */
class PackManifestTest {

    @Test
    fun `validate should pass for valid WASM manifest`() {
        val manifest = createValidWasmManifest()
        manifest.validate() // Should not throw
    }

    @Test
    fun `validate should pass for valid workflow manifest`() {
        val manifest = createValidWorkflowManifest()
        manifest.validate() // Should not throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for blank id`() {
        createValidWasmManifest().copy(id = "").validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for blank name`() {
        createValidWasmManifest().copy(name = "").validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for blank version`() {
        createValidWasmManifest().copy(version = "").validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for blank entry`() {
        createValidWasmManifest().copy(entry = "").validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for WASM pack with non-wasm entry`() {
        createValidWasmManifest().copy(entry = "main.js").validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for workflow pack with non-json entry`() {
        createValidWorkflowManifest().copy(entry = "workflow.yaml").validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for zero memory limit`() {
        createValidWasmManifest().copy(
            limits = PackLimits(memoryMb = 0, cpuMsPerSec = 100)
        ).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for negative memory limit`() {
        createValidWasmManifest().copy(
            limits = PackLimits(memoryMb = -1, cpuMsPerSec = 100)
        ).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validate should fail for zero cpu limit`() {
        createValidWasmManifest().copy(
            limits = PackLimits(memoryMb = 64, cpuMsPerSec = 0)
        ).validate()
    }

    // FilesystemPermissions tests

    @Test
    fun `filesystem permissions should pass for valid relative paths`() {
        val permissions = FilesystemPermissions(
            read = listOf("data", "config/settings.json"),
            write = listOf("logs", "cache/temp")
        )
        permissions.validate() // Should not throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun `filesystem permissions should fail for absolute read path`() {
        FilesystemPermissions(
            read = listOf("/etc/passwd"),
            write = emptyList()
        ).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `filesystem permissions should fail for absolute write path`() {
        FilesystemPermissions(
            read = emptyList(),
            write = listOf("/tmp/evil")
        ).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `filesystem permissions should fail for parent directory traversal`() {
        FilesystemPermissions(
            read = listOf("../../../etc/passwd"),
            write = emptyList()
        ).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `filesystem permissions should fail for hidden parent traversal`() {
        FilesystemPermissions(
            read = listOf("data/../../../secrets"),
            write = emptyList()
        ).validate()
    }

    // NetworkPermissions tests

    @Test
    fun `network permissions should pass for valid HTTPS URLs`() {
        val permissions = NetworkPermissions(
            connect = listOf("https://api.example.com", "https://cdn.example.com/assets"),
            listenLocalhost = false
        )
        permissions.validate() // Should not throw
    }

    @Test
    fun `network permissions should pass for valid HTTP URLs`() {
        val permissions = NetworkPermissions(
            connect = listOf("http://localhost:8080", "http://internal.service"),
            listenLocalhost = true
        )
        permissions.validate() // Should not throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun `network permissions should fail for invalid URL scheme`() {
        NetworkPermissions(
            connect = listOf("ftp://files.example.com"),
            listenLocalhost = false
        ).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `network permissions should fail for bare hostname`() {
        NetworkPermissions(
            connect = listOf("api.example.com"),
            listenLocalhost = false
        ).validate()
    }

    @Test
    fun `network permissions should pass for empty connect list`() {
        NetworkPermissions(
            connect = emptyList(),
            listenLocalhost = true
        ).validate() // Should not throw
    }

    // Helper functions

    private fun createValidWasmManifest(): PackManifest {
        return PackManifest(
            packVersion = "0.1",
            id = "com.example.pack",
            name = "Example Pack",
            version = "1.0.0",
            type = PackType.WASM,
            entry = "main.wasm",
            permissions = PackPermissions(),
            limits = PackLimits(memoryMb = 64, cpuMsPerSec = 100),
            requiredEnv = emptyList(),
            build = BuildMetadata(
                gitSha = "abc123",
                builtAt = "2026-01-17T00:00:00Z",
                target = "android-arm64"
            )
        )
    }

    private fun createValidWorkflowManifest(): PackManifest {
        return PackManifest(
            packVersion = "0.1",
            id = "com.example.workflow",
            name = "Example Workflow",
            version = "1.0.0",
            type = PackType.WORKFLOW,
            entry = "workflow.json",
            permissions = PackPermissions(),
            limits = PackLimits(memoryMb = 32, cpuMsPerSec = 50),
            requiredEnv = listOf("API_KEY"),
            build = BuildMetadata(
                gitSha = "def456",
                builtAt = "2026-01-17T00:00:00Z",
                target = "universal"
            )
        )
    }
}
