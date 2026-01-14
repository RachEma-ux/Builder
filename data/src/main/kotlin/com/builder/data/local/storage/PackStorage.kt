package com.builder.data.local.storage

import android.content.Context
import com.builder.core.model.Pack
import com.builder.core.util.Checksums
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages pack file storage on the device.
 * Packs are stored in /data/data/<app>/packs/<pack-id>/
 */
@Singleton
class PackStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PACKS_DIR = "packs"
        private const val TEMP_DIR = "temp"
    }

    private val packsRoot: File
        get() = File(context.filesDir, PACKS_DIR).also { it.mkdirs() }

    private val tempDir: File
        get() = File(context.cacheDir, TEMP_DIR).also { it.mkdirs() }

    /**
     * Gets the directory for a specific pack.
     */
    fun getPackDir(packId: String): File {
        return File(packsRoot, packId).also { it.mkdirs() }
    }

    /**
     * Gets a temporary directory for staging.
     */
    fun createStagingDir(): File {
        val tempId = System.currentTimeMillis().toString()
        return File(tempDir, tempId).also { it.mkdirs() }
    }

    /**
     * Moves files from temp directory to pack directory atomically.
     */
    fun moveToPackDir(tempDir: File, packId: String): Result<File> {
        return try {
            val packDir = getPackDir(packId)

            // Delete existing pack directory if it exists
            if (packDir.exists()) {
                Timber.i("Deleting existing pack directory: ${packDir.path}")
                packDir.deleteRecursively()
            }

            // Move temp directory to pack directory
            if (!tempDir.renameTo(packDir)) {
                // If rename fails, try copy
                Timber.w("Rename failed, falling back to copy")
                tempDir.copyRecursively(packDir, overwrite = true)
                tempDir.deleteRecursively()
            }

            Timber.i("Pack files moved to: ${packDir.path}")
            Result.success(packDir)
        } catch (e: Exception) {
            Timber.e(e, "Failed to move files to pack directory")
            Result.failure(e)
        }
    }

    /**
     * Deletes a pack directory.
     */
    fun deletePack(packId: String): Result<Unit> {
        return try {
            val packDir = getPackDir(packId)
            if (packDir.exists()) {
                packDir.deleteRecursively()
                Timber.i("Pack deleted: $packId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete pack: $packId")
            Result.failure(e)
        }
    }

    /**
     * Lists all installed pack directories.
     */
    fun listPackDirs(): List<File> {
        return packsRoot.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    /**
     * Gets the pack.json file for a pack.
     */
    fun getPackManifestFile(packId: String): File {
        return File(getPackDir(packId), "pack.json")
    }

    /**
     * Checks if a pack is installed.
     */
    fun isPackInstalled(packId: String): Boolean {
        val packDir = getPackDir(packId)
        val manifestFile = getPackManifestFile(packId)
        return packDir.exists() && manifestFile.exists()
    }

    /**
     * Gets the total size of a pack directory in bytes.
     */
    fun getPackSize(packId: String): Long {
        val packDir = getPackDir(packId)
        return packDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    /**
     * Cleans up temporary directories.
     */
    fun cleanupTemp() {
        try {
            tempDir.deleteRecursively()
            tempDir.mkdirs()
            Timber.i("Temporary directories cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup temp directories")
        }
    }

    /**
     * Calculates checksum for a file in pack directory.
     */
    fun calculateChecksum(packId: String, filename: String): String {
        val file = File(getPackDir(packId), filename)
        return Checksums.sha256(file)
    }
}
