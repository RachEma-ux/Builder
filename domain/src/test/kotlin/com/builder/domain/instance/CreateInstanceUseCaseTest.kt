package com.builder.domain.instance

import com.builder.core.model.*
import com.builder.core.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CreateInstanceUseCase.
 */
class CreateInstanceUseCaseTest {

    private lateinit var instanceRepository: InstanceRepository
    private lateinit var useCase: CreateInstanceUseCase

    @Before
    fun setup() {
        instanceRepository = mockk()
        useCase = CreateInstanceUseCase(instanceRepository)
    }

    @Test
    fun `invoke should create instance via repository`() = runTest {
        // Given
        val pack = createTestPack()
        val instanceName = "My Instance"
        val expectedInstance = createTestInstance(pack.id, instanceName)

        coEvery {
            instanceRepository.createInstance(pack, instanceName)
        } returns Result.success(expectedInstance)

        // When
        val result = useCase(pack, instanceName)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedInstance, result.getOrNull())
        coVerify { instanceRepository.createInstance(pack, instanceName) }
    }

    @Test
    fun `invoke should propagate repository failure`() = runTest {
        // Given
        val pack = createTestPack()
        val instanceName = "My Instance"
        val error = Exception("Database error")

        coEvery {
            instanceRepository.createInstance(pack, instanceName)
        } returns Result.failure(error)

        // When
        val result = useCase(pack, instanceName)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `invoke should handle duplicate instance name`() = runTest {
        // Given
        val pack = createTestPack()
        val duplicateName = "Existing Instance"
        val error = Exception("Instance with name '$duplicateName' already exists")

        coEvery {
            instanceRepository.createInstance(pack, duplicateName)
        } returns Result.failure(error)

        // When
        val result = useCase(pack, duplicateName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun `invoke should create instance with default state STOPPED`() = runTest {
        // Given
        val pack = createTestPack()
        val instanceName = "New Instance"
        val expectedInstance = Instance(
            id = 1L,
            packId = pack.id,
            name = instanceName,
            state = InstanceState.STOPPED,
            createdAt = System.currentTimeMillis()
        )

        coEvery {
            instanceRepository.createInstance(pack, instanceName)
        } returns Result.success(expectedInstance)

        // When
        val result = useCase(pack, instanceName)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(InstanceState.STOPPED, result.getOrNull()?.state)
    }

    private fun createTestInstance(packId: String, name: String): Instance {
        return Instance(
            id = 1L,
            packId = packId,
            name = name,
            state = InstanceState.STOPPED,
            createdAt = System.currentTimeMillis()
        )
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
