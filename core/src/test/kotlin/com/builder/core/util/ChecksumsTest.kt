package com.builder.core.util

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Unit tests for Checksums.
 */
class ChecksumsTest {

    @Test
    fun `sha256 should compute correct hash for known input`() {
        // "hello" -> well-known SHA-256 hash
        val input = ByteArrayInputStream("hello".toByteArray())
        val result = Checksums.sha256(input)

        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            result
        )
    }

    @Test
    fun `sha256 should compute correct hash for empty input`() {
        val input = ByteArrayInputStream(ByteArray(0))
        val result = Checksums.sha256(input)

        // SHA-256 of empty string
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            result
        )
    }

    @Test
    fun `sha256 should handle large input`() {
        // 1MB of data
        val data = ByteArray(1024 * 1024) { it.toByte() }
        val input = ByteArrayInputStream(data)
        val result = Checksums.sha256(input)

        // Just verify it produces a valid 64-character hex string
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]+")))
    }

    @Test
    fun `parseChecksumFile should parse standard format`() {
        val content = """
            abc123def456  pack-hello-android-arm64-v1.0.0.zip
            789012345678  pack-world-linux-x64-v2.0.0.zip
        """.trimIndent()

        val result = Checksums.parseChecksumFile(content)

        assertEquals(2, result.size)
        assertEquals("abc123def456", result["pack-hello-android-arm64-v1.0.0.zip"])
        assertEquals("789012345678", result["pack-world-linux-x64-v2.0.0.zip"])
    }

    @Test
    fun `parseChecksumFile should skip blank lines`() {
        val content = """
            abc123  file1.zip

            def456  file2.zip
        """.trimIndent()

        val result = Checksums.parseChecksumFile(content)

        assertEquals(2, result.size)
    }

    @Test
    fun `parseChecksumFile should skip comment lines`() {
        val content = """
            # This is a comment
            abc123  file1.zip
            # Another comment
            def456  file2.zip
        """.trimIndent()

        val result = Checksums.parseChecksumFile(content)

        assertEquals(2, result.size)
        assertNull(result["# This is a comment"])
    }

    @Test
    fun `parseChecksumFile should handle multiple spaces`() {
        val content = "abc123    file.zip"

        val result = Checksums.parseChecksumFile(content)

        assertEquals(1, result.size)
        assertEquals("abc123", result["file.zip"])
    }

    @Test
    fun `parseChecksumFile should handle empty content`() {
        val result = Checksums.parseChecksumFile("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseChecksumFile should handle filenames with spaces`() {
        val content = "abc123  file with spaces.zip"

        val result = Checksums.parseChecksumFile(content)

        assertEquals(1, result.size)
        assertEquals("abc123", result["file with spaces.zip"])
    }

    @Test
    fun `verify should return true for matching checksum`() {
        val tempFile = createTempFileWithContent("test content")
        try {
            val checksum = Checksums.sha256(tempFile)
            assertTrue(Checksums.verify(tempFile, checksum))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `verify should return false for non-matching checksum`() {
        val tempFile = createTempFileWithContent("test content")
        try {
            assertFalse(Checksums.verify(tempFile, "0000000000000000000000000000000000000000000000000000000000000000"))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `verify should be case insensitive`() {
        val tempFile = createTempFileWithContent("hello")
        try {
            val lowercaseHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
            val uppercaseHash = "2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824"

            assertTrue(Checksums.verify(tempFile, lowercaseHash))
            assertTrue(Checksums.verify(tempFile, uppercaseHash))
        } finally {
            tempFile.delete()
        }
    }

    private fun createTempFileWithContent(content: String): File {
        val tempFile = File.createTempFile("checksum_test", ".txt")
        FileOutputStream(tempFile).use { it.write(content.toByteArray()) }
        return tempFile
    }
}
