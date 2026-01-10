package com.builder.core.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utilities for checksum verification.
 * See Builder_Final.md §9 for production verification mechanism.
 */
object Checksums {
    /**
     * Computes SHA-256 checksum of a file.
     *
     * @param file The file to hash
     * @return Hex-encoded SHA-256 checksum
     */
    fun sha256(file: File): String {
        return file.inputStream().use { sha256(it) }
    }

    /**
     * Computes SHA-256 checksum of an input stream.
     *
     * @param inputStream The input stream to hash
     * @return Hex-encoded SHA-256 checksum
     */
    fun sha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }

        return digest.digest().toHexString()
    }

    /**
     * Verifies a file against an expected SHA-256 checksum.
     *
     * @param file The file to verify
     * @param expectedChecksum The expected hex-encoded SHA-256 checksum
     * @return true if checksums match, false otherwise
     */
    fun verify(file: File, expectedChecksum: String): Boolean {
        val actualChecksum = sha256(file)
        return actualChecksum.equals(expectedChecksum, ignoreCase = true)
    }

    /**
     * Parses a checksums.sha256 file.
     * Format: "<checksum>  <filename>" (two spaces between checksum and filename)
     *
     * @param content The content of checksums.sha256 file
     * @return Map of filename to checksum
     */
    fun parseChecksumFile(content: String): Map<String, String> {
        val checksums = mutableMapOf<String, String>()

        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                return@forEach
            }

            // Format: checksum  filename (two spaces)
            val parts = trimmed.split(Regex("\\s+"), limit = 2)
            if (parts.size == 2) {
                val checksum = parts[0]
                val filename = parts[1]
                checksums[filename] = checksum
            }
        }

        return checksums
    }

    /**
     * Converts a byte array to a hex string.
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
