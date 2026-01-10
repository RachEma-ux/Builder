package com.builder.runtime.wasm

import com.builder.core.model.PackManifest
import timber.log.Timber
import java.io.File

/**
 * WASM runtime wrapper for Wasmtime.
 * Provides sandboxed execution of WASM modules with WASI capabilities.
 * See Builder_Final.md §3 and Implementation Plan §1.2 for design.
 */
class WasmRuntime(
    private val wasiConfig: WasiConfig,
    private val permissionEnforcer: PermissionEnforcer
) {
    companion object {
        init {
            // Load native Wasmtime library
            try {
                System.loadLibrary("wasmtime_jni")
                Timber.i("Wasmtime native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e, "Failed to load Wasmtime native library")
                throw RuntimeException("Wasmtime native library not available", e)
            }
        }
    }

    /**
     * Loads a WASM module from file.
     * @param wasmFile The WASM file to load
     * @return Module handle (opaque pointer)
     */
    external fun loadModule(wasmFile: File): Long

    /**
     * Loads a WASM module from bytes.
     * @param wasmBytes The WASM module bytes
     * @return Module handle (opaque pointer)
     */
    external fun loadModuleFromBytes(wasmBytes: ByteArray): Long

    /**
     * Instantiates a WASM module with WASI configuration.
     * @param moduleHandle The module handle from loadModule
     * @param wasiConfigJson JSON serialized WASI configuration
     * @return Instance handle (opaque pointer)
     */
    external fun instantiate(moduleHandle: Long, wasiConfigJson: String): Long

    /**
     * Calls a WASM function.
     * @param instanceHandle The instance handle
     * @param functionName The exported function name
     * @param argsJson JSON serialized arguments
     * @return JSON serialized result
     */
    external fun call(instanceHandle: Long, functionName: String, argsJson: String): String

    /**
     * Destroys a WASM instance and frees resources.
     * @param instanceHandle The instance handle to destroy
     */
    external fun destroyInstance(instanceHandle: Long)

    /**
     * Destroys a WASM module and frees resources.
     * @param moduleHandle The module handle to destroy
     */
    external fun destroyModule(moduleHandle: Long)

    /**
     * Executes a WASM module with permissions and environment variables.
     * High-level API that handles permission enforcement.
     *
     * @param packId The pack ID (for permission scoping)
     * @param manifest The pack manifest (contains permissions)
     * @param wasmBytes The WASM module bytes
     * @param entryFunction The entry function to call
     * @param envVars Environment variables (including secrets)
     * @return Result of execution
     */
    fun executeWithPermissions(
        packId: String,
        manifest: PackManifest,
        wasmBytes: ByteArray,
        entryFunction: String,
        envVars: Map<String, String>
    ): Result<String> {
        return try {
            // Enforce permissions
            permissionEnforcer.enforce(packId, manifest.permissions)

            // Configure WASI with permissions
            val wasiConfigJson = wasiConfig.toJson(
                packId = packId,
                permissions = manifest.permissions,
                envVars = envVars,
                limits = manifest.limits
            )

            // Load and instantiate module
            val moduleHandle = loadModuleFromBytes(wasmBytes)
            val instanceHandle = try {
                instantiate(moduleHandle, wasiConfigJson)
            } catch (e: Exception) {
                destroyModule(moduleHandle)
                throw e
            }

            // Call entry function
            val result = try {
                call(instanceHandle, entryFunction, "{}")
            } finally {
                destroyInstance(instanceHandle)
                destroyModule(moduleHandle)
            }

            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "WASM execution failed for pack $packId")
            Result.failure(e)
        }
    }
}
