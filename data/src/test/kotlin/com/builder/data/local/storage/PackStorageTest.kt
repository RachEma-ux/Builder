package com.builder.data.local.storage

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for PackStorage
 *
 * Tests file operations including:
 * - Pack directory management
 * - Zip extraction with security checks
 * - Checksum verification
 * - File cleanup
 */
class PackStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var packStorage: PackStorage
    private lateinit var baseDir: File

    @Before
    fun setup() {
        baseDir = tempFolder.newFolder("packs")
        packStorage = PackStorage(baseDir)
    }

    @Test
    fun getPackDir_createsDirectory() {
        // When
        val packDir = packStorage.getPackDir("com.test.pack")

        // Then
        assertTrue(packDir.exists())
        assertTrue(packDir.isDirectory)
        assertEquals("com.test.pack", packDir.name)
    }

    @Test
    fun getPackDir_existingDirectory_returnsExisting() {
        // Given
        val firstCall = packStorage.getPackDir("com.test.pack")
        val testFile = File(firstCall, "test.txt")
        testFile.writeText("test")

        // When
        val secondCall = packStorage.getPackDir("com.test.pack")

        // Then
        assertEquals(firstCall.absolutePath, secondCall.absolutePath)
        assertTrue(File(secondCall, "test.txt").exists())
    }

    @Test
    fun extractZip_validZip_extractsFiles() = runTest {
        // Given
        val packId = "com.test.pack"
        val zipFile = createTestZip(
            "pack.json" to """{"id":"$packId"}""",
            "main.wasm" to "binary-content"
        )

        // When
        val result = packStorage.extractZip(zipFile, packId)

        // Then
        assertTrue(result.isSuccess)
        val packDir = packStorage.getPackDir(packId)
        assertTrue(File(packDir, "pack.json").exists())
        assertTrue(File(packDir, "main.wasm").exists())
        assertEquals("""{"id":"$packId"}""", File(packDir, "pack.json").readText())
    }

    @Test
    fun extractZip_withSubdirectory_extractsCorrectly() = runTest {
        // Given
        val packId = "com.test.pack"
        val zipFile = createTestZip(
            "assets/icon.png" to "icon-data",
            "assets/config.json" to """{"theme":"dark"}"""
        )

        // When
        val result = packStorage.extractZip(zipFile, packId)

        // Then
        assertTrue(result.isSuccess)
        val packDir = packStorage.getPackDir(packId)
        assertTrue(File(packDir, "assets/icon.png").exists())
        assertTrue(File(packDir, "assets/config.json").exists())
    }

    @Test
    fun extractZip_zipSlipAttack_fails() = runTest {
        // Given - Create zip with path traversal attempt
        val packId = "com.test.pack"
        val zipFile = tempFolder.newFile("malicious.zip")

        ZipOutputStream(zipFile.outputStream()).use { zip ->
            // Attempt to write outside pack directory
            val entry = ZipEntry("../../../etc/malicious.txt")
            zip.putNextEntry(entry)
            zip.write("malicious content".toByteArray())
            zip.closeEntry()
        }

        // When
        val result = packStorage.extractZip(zipFile, packId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception.message?.contains("Path traversal") == true)
    }

    @Test
    fun extractZip_absolutePath_fails() = runTest {
        // Given
        val packId = "com.test.pack"
        val zipFile = tempFolder.newFile("malicious.zip")

        ZipOutputStream(zipFile.outputStream()).use { zip ->
            val entry = ZipEntry("/etc/passwd")
            zip.putNextEntry(entry)
            zip.write("malicious".toByteArray())
            zip.closeEntry()
        }

        // When
        val result = packStorage.extractZip(zipFile, packId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SecurityException)
    }

    @Test
    fun deletePack_removesDirectory() = runTest {
        // Given
        val packId = "com.test.pack"
        val packDir = packStorage.getPackDir(packId)
        File(packDir, "test.txt").writeText("test")
        assertTrue(packDir.exists())

        // When
        val result = packStorage.deletePack(packId)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(packDir.exists())
    }

    @Test
    fun deletePack_nonexistentPack_succeeds() = runTest {
        // When
        val result = packStorage.deletePack("com.nonexistent.pack")

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun getPackSize_calculatesCorrectly() = runTest {
        // Given
        val packId = "com.test.pack"
        val packDir = packStorage.getPackDir(packId)
        File(packDir, "file1.txt").writeText("12345") // 5 bytes
        File(packDir, "file2.txt").writeText("1234567890") // 10 bytes

        // When
        val size = packStorage.getPackSize(packId)

        // Then
        assertEquals(15L, size)
    }

    @Test
    fun getPackSize_withSubdirectories_calculatesCorrectly() = runTest {
        // Given
        val packId = "com.test.pack"
        val packDir = packStorage.getPackDir(packId)
        File(packDir, "file1.txt").writeText("12345") // 5 bytes
        val subDir = File(packDir, "assets")
        subDir.mkdirs()
        File(subDir, "file2.txt").writeText("1234567890") // 10 bytes

        // When
        val size = packStorage.getPackSize(packId)

        // Then
        assertEquals(15L, size)
    }

    @Test
    fun getPackSize_nonexistentPack_returnsZero() = runTest {
        // When
        val size = packStorage.getPackSize("com.nonexistent.pack")

        // Then
        assertEquals(0L, size)
    }

    @Test
    fun calculateChecksum_computesSha256() = runTest {
        // Given
        val testFile = tempFolder.newFile("test.txt")
        testFile.writeText("hello world")

        // When
        val checksum = packStorage.calculateChecksum(testFile)

        // Then
        // SHA-256 of "hello world" is a specific hash
        assertEquals(64, checksum.length) // SHA-256 is 64 hex chars
        assertTrue(checksum.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun calculateChecksum_sameContent_sameChecksum() = runTest {
        // Given
        val file1 = tempFolder.newFile("file1.txt")
        val file2 = tempFolder.newFile("file2.txt")
        file1.writeText("identical content")
        file2.writeText("identical content")

        // When
        val checksum1 = packStorage.calculateChecksum(file1)
        val checksum2 = packStorage.calculateChecksum(file2)

        // Then
        assertEquals(checksum1, checksum2)
    }

    @Test
    fun calculateChecksum_differentContent_differentChecksum() = runTest {
        // Given
        val file1 = tempFolder.newFile("file1.txt")
        val file2 = tempFolder.newFile("file2.txt")
        file1.writeText("content A")
        file2.writeText("content B")

        // When
        val checksum1 = packStorage.calculateChecksum(file1)
        val checksum2 = packStorage.calculateChecksum(file2)

        // Then
        assertTrue(checksum1 != checksum2)
    }

    // Helper function to create test zip files
    private fun createTestZip(vararg files: Pair<String, String>): File {
        val zipFile = tempFolder.newFile("test.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            files.forEach { (name, content) ->
                val entry = ZipEntry(name)
                zip.putNextEntry(entry)
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return zipFile
    }
}
