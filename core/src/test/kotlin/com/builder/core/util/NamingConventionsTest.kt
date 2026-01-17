package com.builder.core.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NamingConventions.
 */
class NamingConventionsTest {

    @Test
    fun `parse should return components for valid filename`() {
        val result = NamingConventions.parse("pack-hello-android-arm64-v1.0.0.zip")

        assertNotNull(result)
        assertEquals("hello", result!!.variant)
        assertEquals("android-arm64", result.target)
        assertEquals("v1.0.0", result.version)
        assertEquals("pack-hello-android-arm64-v1.0.0.zip", result.filename)
    }

    @Test
    fun `parse should handle complex variant names`() {
        val result = NamingConventions.parse("pack-my-cool-app-android-universal-v2.3.4.zip")

        assertNotNull(result)
        assertEquals("my-cool-app", result!!.variant)
        assertEquals("android-universal", result.target)
        assertEquals("v2.3.4", result.version)
    }

    @Test
    fun `parse should handle semver with prerelease`() {
        val result = NamingConventions.parse("pack-test-linux-x64-1.0.0-beta.1.zip")

        assertNotNull(result)
        assertEquals("test", result!!.variant)
        assertEquals("linux-x64", result.target)
        assertEquals("1.0.0-beta.1", result.version)
    }

    @Test
    fun `parse should handle dev versions with git sha`() {
        val result = NamingConventions.parse("pack-workflow-android-arm64-0.0.0-dev+abc123.zip")

        assertNotNull(result)
        assertEquals("workflow", result!!.variant)
        assertEquals("android-arm64", result.target)
        assertEquals("0.0.0-dev+abc123", result.version)
    }

    @Test
    fun `parse should return null for invalid filename without pack prefix`() {
        val result = NamingConventions.parse("hello-android-arm64-v1.0.0.zip")
        assertNull(result)
    }

    @Test
    fun `parse should return null for invalid filename without zip extension`() {
        val result = NamingConventions.parse("pack-hello-android-arm64-v1.0.0.tar.gz")
        assertNull(result)
    }

    @Test
    fun `parse should return null for invalid filename with uppercase`() {
        val result = NamingConventions.parse("pack-Hello-android-arm64-v1.0.0.zip")
        assertNull(result)
    }

    @Test
    fun `parse should return null for empty string`() {
        val result = NamingConventions.parse("")
        assertNull(result)
    }

    @Test
    fun `isValid should return true for valid filename`() {
        assertTrue(NamingConventions.isValid("pack-hello-android-arm64-v1.0.0.zip"))
    }

    @Test
    fun `isValid should return false for invalid filename`() {
        assertFalse(NamingConventions.isValid("invalid-filename.zip"))
        assertFalse(NamingConventions.isValid("pack-HELLO-android-v1.0.0.zip"))
        assertFalse(NamingConventions.isValid(""))
    }

    @Test
    fun `construct should create valid filename`() {
        val result = NamingConventions.construct("hello", "android-arm64", "v1.0.0")
        assertEquals("pack-hello-android-arm64-v1.0.0.zip", result)
    }

    @Test
    fun `construct should create filename with complex version`() {
        val result = NamingConventions.construct("app", "linux-x64", "1.0.0-beta+sha.abc123")
        assertEquals("pack-app-linux-x64-1.0.0-beta+sha.abc123.zip", result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `construct should throw for invalid variant with uppercase`() {
        NamingConventions.construct("Hello", "android-arm64", "v1.0.0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `construct should throw for invalid target with spaces`() {
        NamingConventions.construct("hello", "android arm64", "v1.0.0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `construct should throw for blank version`() {
        NamingConventions.construct("hello", "android-arm64", "")
    }

    @Test
    fun `validateOrThrow should not throw for valid filename`() {
        NamingConventions.validateOrThrow("pack-hello-android-arm64-v1.0.0.zip")
        // No exception means success
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validateOrThrow should throw for invalid filename`() {
        NamingConventions.validateOrThrow("invalid.zip")
    }
}
