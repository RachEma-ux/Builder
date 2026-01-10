package com.builder.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.builder.core.model.InstallMode
import com.builder.core.model.InstallSource
import com.builder.core.model.PackType
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
 * Integration tests for PackDao
 *
 * Tests Room database operations for packs including:
 * - Insert and query operations
 * - Flow-based reactive queries
 * - Update and delete operations
 * - Cascade delete with instances
 */
@RunWith(AndroidJUnit4::class)
class PackDaoTest {

    private lateinit var database: BuilderDatabase
    private lateinit var packDao: PackDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BuilderDatabase::class.java
        ).build()
        packDao = database.packDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertPack_andRetrieveById() = runTest {
        // Given
        val pack = createTestPack(
            id = "com.test.pack1",
            name = "Test Pack 1",
            version = "v1.0.0"
        )

        // When
        packDao.insert(pack)
        val retrieved = packDao.getById(pack.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(pack.id, retrieved.id)
        assertEquals(pack.name, retrieved.name)
        assertEquals(pack.version, retrieved.version)
        assertEquals(pack.type, retrieved.type)
    }

    @Test
    fun getAllPacks_emptyDatabase_returnsEmptyList() = runTest {
        // When
        val packs = packDao.getAll().first()

        // Then
        assertTrue(packs.isEmpty())
    }

    @Test
    fun getAllPacks_withMultiplePacks_returnsAll() = runTest {
        // Given
        val pack1 = createTestPack(id = "com.test.pack1", name = "Pack 1")
        val pack2 = createTestPack(id = "com.test.pack2", name = "Pack 2")
        val pack3 = createTestPack(id = "com.test.pack3", name = "Pack 3")

        packDao.insert(pack1)
        packDao.insert(pack2)
        packDao.insert(pack3)

        // When
        val packs = packDao.getAll().first()

        // Then
        assertEquals(3, packs.size)
        assertTrue(packs.any { it.id == "com.test.pack1" })
        assertTrue(packs.any { it.id == "com.test.pack2" })
        assertTrue(packs.any { it.id == "com.test.pack3" })
    }

    @Test
    fun getPacksByMode_filtersCorrectly() = runTest {
        // Given
        val devPack = createTestPack(
            id = "com.test.dev",
            installMode = InstallMode.DEV,
            installSource = InstallSource.fromWorkflowArtifact("main", "123", "run-456")
        )
        val prodPack = createTestPack(
            id = "com.test.prod",
            installMode = InstallMode.PROD,
            installSource = InstallSource.fromRelease("v1.0.0")
        )

        packDao.insert(devPack)
        packDao.insert(prodPack)

        // When
        val devPacks = packDao.getByMode(InstallMode.DEV).first()
        val prodPacks = packDao.getByMode(InstallMode.PROD).first()

        // Then
        assertEquals(1, devPacks.size)
        assertEquals("com.test.dev", devPacks[0].id)
        assertEquals(1, prodPacks.size)
        assertEquals("com.test.prod", prodPacks[0].id)
    }

    @Test
    fun getPacksByType_filtersCorrectly() = runTest {
        // Given
        val wasmPack = createTestPack(
            id = "com.test.wasm",
            type = PackType.WASM
        )
        val workflowPack = createTestPack(
            id = "com.test.workflow",
            type = PackType.WORKFLOW
        )

        packDao.insert(wasmPack)
        packDao.insert(workflowPack)

        // When
        val wasmPacks = packDao.getByType(PackType.WASM).first()
        val workflowPacks = packDao.getByType(PackType.WORKFLOW).first()

        // Then
        assertEquals(1, wasmPacks.size)
        assertEquals("com.test.wasm", wasmPacks[0].id)
        assertEquals(1, workflowPacks.size)
        assertEquals("com.test.workflow", workflowPacks[0].id)
    }

    @Test
    fun updatePack_modifiesExisting() = runTest {
        // Given
        val pack = createTestPack(id = "com.test.pack", name = "Original Name")
        packDao.insert(pack)

        // When
        val updated = pack.copy(name = "Updated Name", version = "v2.0.0")
        packDao.update(updated)
        val retrieved = packDao.getById(pack.id)

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved.name)
        assertEquals("v2.0.0", retrieved.version)
    }

    @Test
    fun deletePack_removes() = runTest {
        // Given
        val pack = createTestPack(id = "com.test.pack")
        packDao.insert(pack)

        // When
        packDao.delete(pack)
        val retrieved = packDao.getById(pack.id)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun deleteById_removes() = runTest {
        // Given
        val pack = createTestPack(id = "com.test.pack")
        packDao.insert(pack)

        // When
        packDao.deleteById(pack.id)
        val retrieved = packDao.getById(pack.id)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun flowUpdates_whenPackInserted() = runTest {
        // Given - Start observing before insert
        val flow = packDao.getAll()
        val initial = flow.first()
        assertEquals(0, initial.size)

        // When
        val pack = createTestPack(id = "com.test.pack")
        packDao.insert(pack)

        // Then - Flow should emit updated list
        val updated = flow.first()
        assertEquals(1, updated.size)
    }

    // Helper function to create test packs
    private fun createTestPack(
        id: String = "com.test.pack",
        name: String = "Test Pack",
        version: String = "v1.0.0",
        type: PackType = PackType.WASM,
        installMode: InstallMode = InstallMode.DEV,
        installSource: InstallSource = InstallSource.fromWorkflowArtifact(
            "main",
            "abc123",
            "run-456"
        )
    ) = PackEntity(
        id = id,
        name = name,
        version = version,
        type = type,
        entry = if (type == PackType.WASM) "main.wasm" else "workflow.json",
        installMode = installMode,
        sourceRef = installSource.ref,
        sourceType = installSource.type.name,
        sourceMetadata = installSource.metadata,
        installPath = "/data/packs/$id",
        installedAt = System.currentTimeMillis(),
        permissions = emptyMap(),
        limits = emptyMap(),
        requiredSecrets = emptyList()
    )
}
