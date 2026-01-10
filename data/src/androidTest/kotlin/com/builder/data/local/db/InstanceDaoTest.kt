package com.builder.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.builder.core.model.InstallMode
import com.builder.core.model.InstallSource
import com.builder.core.model.InstanceState
import com.builder.core.model.PackType
import com.builder.data.local.db.entities.InstanceEntity
import com.builder.data.local.db.entities.PackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for InstanceDao
 *
 * Tests Room database operations for instances including:
 * - Insert and query operations
 * - Flow-based reactive queries
 * - State updates
 * - Foreign key relationships with packs
 * - Cascade delete behavior
 */
@RunWith(AndroidJUnit4::class)
class InstanceDaoTest {

    private lateinit var database: BuilderDatabase
    private lateinit var instanceDao: InstanceDao
    private lateinit var packDao: PackDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BuilderDatabase::class.java
        ).build()
        instanceDao = database.instanceDao()
        packDao = database.packDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertInstance_andRetrieveById() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance = createTestInstance(
            id = "instance-1",
            packId = pack.id
        )

        // When
        instanceDao.insert(instance)
        val retrieved = instanceDao.getById(instance.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(instance.id, retrieved.id)
        assertEquals(instance.packId, retrieved.packId)
        assertEquals(instance.state, retrieved.state)
    }

    @Test
    fun getAllInstances_emptyDatabase_returnsEmptyList() = runTest {
        // When
        val instances = instanceDao.getAll().first()

        // Then
        assertTrue(instances.isEmpty())
    }

    @Test
    fun getAllInstances_withMultipleInstances_returnsAll() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance1 = createTestInstance(id = "instance-1", packId = pack.id)
        val instance2 = createTestInstance(id = "instance-2", packId = pack.id)
        val instance3 = createTestInstance(id = "instance-3", packId = pack.id)

        instanceDao.insert(instance1)
        instanceDao.insert(instance2)
        instanceDao.insert(instance3)

        // When
        val instances = instanceDao.getAll().first()

        // Then
        assertEquals(3, instances.size)
    }

    @Test
    fun getInstancesByPackId_filtersCorrectly() = runTest {
        // Given
        val pack1 = createTestPack(id = "pack-1")
        val pack2 = createTestPack(id = "pack-2")
        packDao.insert(pack1)
        packDao.insert(pack2)

        val instance1 = createTestInstance(id = "instance-1", packId = pack1.id)
        val instance2 = createTestInstance(id = "instance-2", packId = pack1.id)
        val instance3 = createTestInstance(id = "instance-3", packId = pack2.id)

        instanceDao.insert(instance1)
        instanceDao.insert(instance2)
        instanceDao.insert(instance3)

        // When
        val pack1Instances = instanceDao.getByPackId(pack1.id).first()
        val pack2Instances = instanceDao.getByPackId(pack2.id).first()

        // Then
        assertEquals(2, pack1Instances.size)
        assertEquals(1, pack2Instances.size)
        assertTrue(pack1Instances.all { it.packId == pack1.id })
        assertTrue(pack2Instances.all { it.packId == pack2.id })
    }

    @Test
    fun getInstancesByState_filtersCorrectly() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val stoppedInstance = createTestInstance(
            id = "stopped",
            packId = pack.id,
            state = InstanceState.STOPPED
        )
        val runningInstance = createTestInstance(
            id = "running",
            packId = pack.id,
            state = InstanceState.RUNNING
        )
        val pausedInstance = createTestInstance(
            id = "paused",
            packId = pack.id,
            state = InstanceState.PAUSED
        )

        instanceDao.insert(stoppedInstance)
        instanceDao.insert(runningInstance)
        instanceDao.insert(pausedInstance)

        // When
        val stoppedInstances = instanceDao.getByState(InstanceState.STOPPED).first()
        val runningInstances = instanceDao.getByState(InstanceState.RUNNING).first()
        val pausedInstances = instanceDao.getByState(InstanceState.PAUSED).first()

        // Then
        assertEquals(1, stoppedInstances.size)
        assertEquals(InstanceState.STOPPED, stoppedInstances[0].state)
        assertEquals(1, runningInstances.size)
        assertEquals(InstanceState.RUNNING, runningInstances[0].state)
        assertEquals(1, pausedInstances.size)
        assertEquals(InstanceState.PAUSED, pausedInstances[0].state)
    }

    @Test
    fun updateInstanceState_modifiesExisting() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance = createTestInstance(
            id = "instance-1",
            packId = pack.id,
            state = InstanceState.STOPPED
        )
        instanceDao.insert(instance)

        // When
        instanceDao.updateState(instance.id, InstanceState.RUNNING)
        val retrieved = instanceDao.getById(instance.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(InstanceState.RUNNING, retrieved.state)
    }

    @Test
    fun updateInstance_modifiesExisting() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance = createTestInstance(id = "instance-1", packId = pack.id)
        instanceDao.insert(instance)

        // When
        val updated = instance.copy(
            state = InstanceState.RUNNING,
            lastError = "Test error"
        )
        instanceDao.update(updated)
        val retrieved = instanceDao.getById(instance.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(InstanceState.RUNNING, retrieved.state)
        assertEquals("Test error", retrieved.lastError)
    }

    @Test
    fun deleteInstance_removes() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance = createTestInstance(id = "instance-1", packId = pack.id)
        instanceDao.insert(instance)

        // When
        instanceDao.delete(instance)
        val retrieved = instanceDao.getById(instance.id)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun deleteById_removes() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance = createTestInstance(id = "instance-1", packId = pack.id)
        instanceDao.insert(instance)

        // When
        instanceDao.deleteById(instance.id)
        val retrieved = instanceDao.getById(instance.id)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun cascadeDelete_deletingPackDeletesInstances() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance1 = createTestInstance(id = "instance-1", packId = pack.id)
        val instance2 = createTestInstance(id = "instance-2", packId = pack.id)
        instanceDao.insert(instance1)
        instanceDao.insert(instance2)

        // Verify instances exist
        val initialInstances = instanceDao.getByPackId(pack.id).first()
        assertEquals(2, initialInstances.size)

        // When
        packDao.delete(pack)

        // Then - Instances should be cascade deleted
        val remainingInstances = instanceDao.getByPackId(pack.id).first()
        assertEquals(0, remainingInstances.size)
    }

    @Test
    fun flowUpdates_whenInstanceInserted() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val flow = instanceDao.getAll()
        val initial = flow.first()
        assertEquals(0, initial.size)

        // When
        val instance = createTestInstance(id = "instance-1", packId = pack.id)
        instanceDao.insert(instance)

        // Then - Flow should emit updated list
        val updated = flow.first()
        assertEquals(1, updated.size)
    }

    @Test
    fun flowUpdates_whenInstanceStateChanged() = runTest {
        // Given
        val pack = createTestPack()
        packDao.insert(pack)

        val instance = createTestInstance(
            id = "instance-1",
            packId = pack.id,
            state = InstanceState.STOPPED
        )
        instanceDao.insert(instance)

        val flow = instanceDao.getByState(InstanceState.RUNNING)
        val initial = flow.first()
        assertEquals(0, initial.size)

        // When
        instanceDao.updateState(instance.id, InstanceState.RUNNING)

        // Then - Flow should emit updated list
        val updated = flow.first()
        assertEquals(1, updated.size)
        assertEquals(InstanceState.RUNNING, updated[0].state)
    }

    // Helper functions
    private fun createTestPack(
        id: String = "com.test.pack"
    ) = PackEntity(
        id = id,
        name = "Test Pack",
        version = "v1.0.0",
        type = PackType.WASM,
        entry = "main.wasm",
        installMode = InstallMode.DEV,
        sourceRef = "main",
        sourceType = "WORKFLOW_ARTIFACT",
        sourceMetadata = emptyMap(),
        installPath = "/data/packs/$id",
        installedAt = System.currentTimeMillis(),
        permissions = emptyMap(),
        limits = emptyMap(),
        requiredSecrets = emptyList()
    )

    private fun createTestInstance(
        id: String = "instance-1",
        packId: String,
        state: InstanceState = InstanceState.STOPPED
    ) = InstanceEntity(
        id = id,
        packId = packId,
        state = state,
        createdAt = System.currentTimeMillis(),
        startedAt = null,
        stoppedAt = null,
        secrets = emptyMap(),
        lastError = null
    )
}
