package com.builder.data.local.storage

import com.builder.core.model.InstallMode
import com.builder.core.model.InstallSource
import com.builder.core.model.PackManifest
import com.builder.core.util.Checksums
import com.builder.core.util.NamingConventions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles pack installation from various sources.
 * See Builder_Final.md §1, §3, §9 for installation requirements.
 */
@Singleton
class PackInstaller @Inject constructor(
    private val packStorage: PackStorage,
    private val httpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Installs a pack from a URL (artifact or release asset).
     *
     * @param downloadUrl URL to download the pack zip
     * @param installSource Source information (dev or prod)
     * @param expectedChecksum Optional checksum for verification (mandatory for prod)
     * @return Result with installation details
     */
    suspend fun installFromUrl(
        downloadUrl: String,
        installSource: InstallSource,
        expectedChecksum: String? = null
    ): Result<InstallResult> = withContext(Dispatchers.IO) {
        try {
            Timber.i("Installing pack from: $downloadUrl")

            // Step 1: Download pack zip to temp directory
            val tempDir = packStorage.getTempDir()
            val packZipFile = File(tempDir, "pack.zip")

            downloadFile(downloadUrl, packZipFile).onFailure {
                tempDir.deleteRecursively()
                return@withContext Result.failure(it)
            }

            // Step 2: Verify filename follows naming convention
            val filename = downloadUrl.substringAfterLast("/")
            if (!NamingConventions.isValid(filename)) {
                Timber.e("Invalid pack filename: $filename")
                tempDir.deleteRecursively()
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid pack filename: $filename")
                )
            }

            // Step 3: Verify checksum (mandatory for production)
            if (installSource.getMode() == InstallMode.PROD) {
                if (expectedChecksum == null) {
                    tempDir.deleteRecursively()
                    return@withContext Result.failure(
                        IllegalArgumentException("Checksum required for production installs")
                    )
                }

                val actualChecksum = Checksums.sha256(packZipFile)
                if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                    Timber.e("Checksum mismatch! Expected: $expectedChecksum, Actual: $actualChecksum")
                    tempDir.deleteRecursively()
                    return@withContext Result.failure(
                        SecurityException("Checksum verification failed")
                    )
                }
                Timber.i("Checksum verified successfully")
            }

            // Step 4: Extract pack zip
            val extractDir = File(tempDir, "extracted")
            extractDir.mkdirs()

            extractZip(packZipFile, extractDir).onFailure {
                tempDir.deleteRecursively()
                return@withContext Result.failure(it)
            }

            // Step 5: Validate pack.json
            val manifestFile = File(extractDir, "pack.json")
            if (!manifestFile.exists()) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(
                    IllegalArgumentException("pack.json not found in zip root")
                )
            }

            val manifest = try {
                json.decodeFromString<PackManifest>(manifestFile.readText())
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse pack.json")
                tempDir.deleteRecursively()
                return@withContext Result.failure(e)
            }

            // Validate manifest
            try {
                manifest.validate()
            } catch (e: Exception) {
                Timber.e(e, "Pack manifest validation failed")
                tempDir.deleteRecursively()
                return@withContext Result.failure(e)
            }

            // Step 6: Verify entry file exists
            val entryFile = File(extractDir, manifest.entry)
            if (!entryFile.exists()) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(
                    IllegalArgumentException("Entry file not found: ${manifest.entry}")
                )
            }

            // Step 7: Move to pack directory
            val packDir = packStorage.moveToPackDir(extractDir, manifest.id).getOrElse {
                tempDir.deleteRecursively()
                return@withContext Result.failure(it)
            }

            // Cleanup
            tempDir.deleteRecursively()

            Timber.i("Pack installed successfully: ${manifest.id}")

            Result.success(
                InstallResult(
                    manifest = manifest,
                    installPath = packDir.absolutePath,
                    installSource = installSource,
                    checksumSha256 = expectedChecksum ?: Checksums.sha256(packZipFile)
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Pack installation failed")
            Result.failure(e)
        }
    }

    /**
     * Downloads a file from a URL.
     */
    private suspend fun downloadFile(url: String, destination: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Downloading: $url")

                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Download failed: ${response.code}")
                    )
                }

                response.body?.use { body ->
                    FileOutputStream(destination).use { output ->
                        body.byteStream().copyTo(output)
                    }
                }

                Timber.i("Downloaded: ${destination.name} (${destination.length()} bytes)")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Download failed")
                Result.failure(e)
            }
        }

    /**
     * Extracts a zip file.
     */
    private suspend fun extractZip(zipFile: File, destination: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Extracting: ${zipFile.name}")

                ZipFile(zipFile).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        // Security: prevent zip slip vulnerability
                        val entryDestination = File(destination, entry.name)
                        if (!entryDestination.canonicalPath.startsWith(destination.canonicalPath)) {
                            throw SecurityException("Zip entry outside destination: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            entryDestination.mkdirs()
                        } else {
                            entryDestination.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(entryDestination).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }

                Timber.i("Extracted successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Extraction failed")
                Result.failure(e)
            }
        }
}

/**
 * Result of pack installation.
 */
data class InstallResult(
    val manifest: PackManifest,
    val installPath: String,
    val installSource: InstallSource,
    val checksumSha256: String
)
