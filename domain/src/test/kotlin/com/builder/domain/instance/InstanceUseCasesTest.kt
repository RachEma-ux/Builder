package com.builder.domain.instance

import com.builder.core.model.Instance
import com.builder.core.model.InstanceState
import com.builder.core.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for instance use cases.
 */
class InstanceUseCasesTest {

    private lateinit var instanceRepository: InstanceRepository

    @Before
    fun setup() {
        instanceRepository = mockk()
    }

    // PauseInstanceUseCase tests

    @Test
    fun `PauseInstanceUseCase should pause running instance`() = runTest {
        // Given
        val instance = createTestInstance(InstanceState.RUNNING)
        val useCase = PauseInstanceUseCase(instanceRepository)
        coEvery { instanceRepository.pauseInstance(instance) } returns Result.success(Unit)

        // When
        val result = useCase(instance)

        // Then
        assertTrue(result.isSuccess)
        coVerify { instanceRepository.pauseInstance(instance) }
    }

    @Test
    fun `PauseInstanceUseCase should propagate failure`() = runTest {
        // Given
        val instance = createTestInstance(InstanceState.RUNNING)
        val useCase = PauseInstanceUseCase(instanceRepository)
        val error = Exception("Failed to pause")
        coEvery { instanceRepository.pauseInstance(instance) } returns Result.failure(error)

        // When
        val result = useCase(instance)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    // StopInstanceUseCase tests

    @Test
    fun `StopInstanceUseCase should stop running instance`() = runTest {
        // Given
        val instance = createTestInstance(InstanceState.RUNNING)
        val useCase = StopInstanceUseCase(instanceRepository)
        coEvery { instanceRepository.stopInstance(instance) } returns Result.success(Unit)

        // When
        val result = useCase(instance)

        // Then
        assertTrue(result.isSuccess)
        coVerify { instanceRepository.stopInstance(instance) }
    }

    @Test
    fun `StopInstanceUseCase should stop paused instance`() = runTest {
        // Given
        val instance = createTestInstance(InstanceState.PAUSED)
        val useCase = StopInstanceUseCase(instanceRepository)
        coEvery { instanceRepository.stopInstance(instance) } returns Result.success(Unit)

        // When
        val result = useCase(instance)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `StopInstanceUseCase should propagate failure`() = runTest {
        // Given
        val instance = createTestInstance(InstanceState.RUNNING)
        val useCase = StopInstanceUseCase(instanceRepository)
        val error = Exception("Failed to stop")
        coEvery { instanceRepository.stopInstance(instance) } returns Result.failure(error)

        // When
        val result = useCase(instance)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    // DeleteInstanceUseCase tests

    @Test
    fun `DeleteInstanceUseCase should delete instance by id`() = runTest {
        // Given
        val instanceId = 123L
        val useCase = DeleteInstanceUseCase(instanceRepository)
        coEvery { instanceRepository.deleteInstance(instanceId) } returns Result.success(Unit)

        // When
        val result = useCase(instanceId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { instanceRepository.deleteInstance(instanceId) }
    }

    @Test
    fun `DeleteInstanceUseCase should fail for non-existent instance`() = runTest {
        // Given
        val instanceId = 999L
        val useCase = DeleteInstanceUseCase(instanceRepository)
        val error = Exception("Instance not found")
        coEvery { instanceRepository.deleteInstance(instanceId) } returns Result.failure(error)

        // When
        val result = useCase(instanceId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    // GetAllInstancesUseCase tests

    @Test
    fun `GetAllInstancesUseCase should return all instances`() = runTest {
        // Given
        val instances = listOf(
            createTestInstance(InstanceState.RUNNING, id = 1),
            createTestInstance(InstanceState.STOPPED, id = 2),
            createTestInstance(InstanceState.PAUSED, id = 3)
        )
        val useCase = GetAllInstancesUseCase(instanceRepository)
        every { instanceRepository.getAllInstances() } returns flowOf(instances)

        // When
        val result = useCase().first()

        // Then
        assertEquals(3, result.size)
        assertEquals(instances, result)
    }

    @Test
    fun `GetAllInstancesUseCase should return empty list when no instances`() = runTest {
        // Given
        val useCase = GetAllInstancesUseCase(instanceRepository)
        every { instanceRepository.getAllInstances() } returns flowOf(emptyList())

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // GetInstancesForPackUseCase tests

    @Test
    fun `GetInstancesForPackUseCase should return instances for specific pack`() = runTest {
        // Given
        val packId = "com.test.pack"
        val instances = listOf(
            createTestInstance(InstanceState.RUNNING, id = 1, packId = packId),
            createTestInstance(InstanceState.STOPPED, id = 2, packId = packId)
        )
        val useCase = GetInstancesForPackUseCase(instanceRepository)
        every { instanceRepository.getInstancesForPack(packId) } returns flowOf(instances)

        // When
        val result = useCase(packId).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.packId == packId })
    }

    @Test
    fun `GetInstancesForPackUseCase should return empty for pack with no instances`() = runTest {
        // Given
        val packId = "com.empty.pack"
        val useCase = GetInstancesForPackUseCase(instanceRepository)
        every { instanceRepository.getInstancesForPack(packId) } returns flowOf(emptyList())

        // When
        val result = useCase(packId).first()

        // Then
        assertTrue(result.isEmpty())
    }

    // GetRunningInstancesUseCase tests

    @Test
    fun `GetRunningInstancesUseCase should return only running instances`() = runTest {
        // Given
        val runningInstances = listOf(
            createTestInstance(InstanceState.RUNNING, id = 1),
            createTestInstance(InstanceState.RUNNING, id = 2)
        )
        val useCase = GetRunningInstancesUseCase(instanceRepository)
        every { instanceRepository.getRunningInstances() } returns flowOf(runningInstances)

        // When
        val result = useCase().first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.state == InstanceState.RUNNING })
    }

    @Test
    fun `GetRunningInstancesUseCase should return empty when no running instances`() = runTest {
        // Given
        val useCase = GetRunningInstancesUseCase(instanceRepository)
        every { instanceRepository.getRunningInstances() } returns flowOf(emptyList())

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // Helper functions

    private fun createTestInstance(
        state: InstanceState,
        id: Long = 1L,
        packId: String = "com.test.pack"
    ): Instance {
        return Instance(
            id = id,
            packId = packId,
            name = "Test Instance $id",
            state = state,
            createdAt = System.currentTimeMillis(),
            startedAt = if (state == InstanceState.RUNNING) System.currentTimeMillis() else null
        )
    }
}
