package com.builder.domain.pack

import com.builder.core.model.*
import com.builder.core.repository.PackRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InstallPackUseCase.
 */
class InstallPackUseCaseTest {

    private lateinit var packRepository: PackRepository
    private lateinit var useCase: InstallPackUseCase

    @Before
    fun setup() {
        packRepository = mockk()
        useCase = InstallPackUseCase(packRepository)
    }

    @Test
    fun `invoke should call repository installFromUrl`() = runTest {
        // Given
        val downloadUrl = "https://github.com/test/pack.zip"
        val installSource = InstallSource.dev("main", downloadUrl)
        val expectedPack = createTestPack()

        coEvery {
            packRepository.installFromUrl(downloadUrl, installSource, null)
        } returns Result.success(expectedPack)

        // When
        val result = useCase(downloadUrl, installSource, null)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedPack, result.getOrNull())
        coVerify { packRepository.installFromUrl(downloadUrl, installSource, null) }
    }

    @Test
    fun `invoke should propagate repository failure`() = runTest {
        // Given
        val downloadUrl = "https://github.com/test/pack.zip"
        val installSource = InstallSource.dev("main", downloadUrl)
        val error = Exception("Network error")

        coEvery {
            packRepository.installFromUrl(downloadUrl, installSource, null)
        } returns Result.failure(error)

        // When
        val result = useCase(downloadUrl, installSource, null)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    private fun createTestPack(): Pack {
        return Pack(
            id = "com.test.pack",
            name = "Test Pack",
            version = "1.0.0",
            type = PackType.WASM,
            manifest = createTestManifest(),
            installSource = InstallSource.dev("main", "https://test.com"),
            installPath = "/data/packs/test",
            checksumSha256 = "abc123"
        )
    }

    private fun createTestManifest(): PackManifest {
        return PackManifest(
            packVersion = "0.1",
            id = "com.test.pack",
            name = "Test Pack",
            version = "1.0.0",
            type = PackType.WASM,
            entry = "main.wasm",
            permissions = PackPermissions(),
            limits = PackLimits(memoryMb = 64, cpuMsPerSec = 100),
            requiredEnv = emptyList(),
            build = BuildMetadata(
                gitSha = "abc123",
                builtAt = "2026-01-10T12:00:00Z",
                target = "android-arm64"
            )
        )
    }
}
