package com.builder.domain.instance

import com.builder.core.model.*
import com.builder.core.repository.InstanceRepository
import com.builder.core.repository.PackRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StartInstanceUseCase.
 */
class StartInstanceUseCaseTest {

    private lateinit var instanceRepository: InstanceRepository
    private lateinit var packRepository: PackRepository
    private lateinit var useCase: StartInstanceUseCase

    @Before
    fun setup() {
        instanceRepository = mockk()
        packRepository = mockk()
        useCase = StartInstanceUseCase(instanceRepository, packRepository)
    }

    @Test
    fun `invoke should start instance when pack found and no required secrets`() = runTest {
        // Given
        val instance = createTestInstance()
        val pack = createTestPack(requiredSecrets = emptyList())
        val envVars = emptyMap<String, String>()

        coEvery { packRepository.getPackById(instance.packId) } returns pack
        coEvery { instanceRepository.startInstance(instance, pack, envVars) } returns Result.success(Unit)

        // When
        val result = useCase(instance, envVars)

        // Then
        assertTrue(result.isSuccess)
        coVerify { instanceRepository.startInstance(instance, pack, envVars) }
    }

    @Test
    fun `invoke should fail when pack not found`() = runTest {
        // Given
        val instance = createTestInstance()

        coEvery { packRepository.getPackById(instance.packId) } returns null

        // When
        val result = useCase(instance)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Pack not found: ${instance.packId}", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should fail when required secrets missing`() = runTest {
        // Given
        val instance = createTestInstance()
        val pack = createTestPack(requiredSecrets = listOf("API_KEY", "SECRET"))
        val envVars = mapOf("API_KEY" to "test") // Missing SECRET

        coEvery { packRepository.getPackById(instance.packId) } returns pack

        // When
        val result = useCase(instance, envVars)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("SECRET") == true)
    }

    @Test
    fun `invoke should start instance when all required secrets provided`() = runTest {
        // Given
        val instance = createTestInstance()
        val pack = createTestPack(requiredSecrets = listOf("API_KEY", "SECRET"))
        val envVars = mapOf("API_KEY" to "test", "SECRET" to "value")

        coEvery { packRepository.getPackById(instance.packId) } returns pack
        coEvery { instanceRepository.startInstance(instance, pack, envVars) } returns Result.success(Unit)

        // When
        val result = useCase(instance, envVars)

        // Then
        assertTrue(result.isSuccess)
        coVerify { instanceRepository.startInstance(instance, pack, envVars) }
    }

    private fun createTestInstance(): Instance {
        return Instance(
            id = 1L,
            packId = "com.test.pack",
            name = "Test Instance",
            state = InstanceState.STOPPED,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createTestPack(requiredSecrets: List<String>): Pack {
        return Pack(
            id = "com.test.pack",
            name = "Test Pack",
            version = "1.0.0",
            type = PackType.WASM,
            manifest = createTestManifest(requiredSecrets),
            installSource = InstallSource.dev("main", "https://test.com"),
            installPath = "/data/packs/test",
            checksumSha256 = "abc123"
        )
    }

    private fun createTestManifest(requiredEnv: List<String>): PackManifest {
        return PackManifest(
            packVersion = "0.1",
            id = "com.test.pack",
            name = "Test Pack",
            version = "1.0.0",
            type = PackType.WASM,
            entry = "main.wasm",
            permissions = PackPermissions(),
            limits = PackLimits(memoryMb = 64, cpuMsPerSec = 100),
            requiredEnv = requiredEnv,
            build = BuildMetadata(
                gitSha = "abc123",
                builtAt = "2026-01-10T12:00:00Z",
                target = "android-arm64"
            )
        )
    }
}
