package com.builder.data.local.db.dao

import androidx.room.*
import com.builder.data.local.db.entities.PackEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for pack database operations.
 */
@Dao
interface PackDao {
    /**
     * Inserts a pack, replacing if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: PackEntity)

    /**
     * Updates an existing pack.
     */
    @Update
    suspend fun update(pack: PackEntity)

    /**
     * Deletes a pack.
     */
    @Delete
    suspend fun delete(pack: PackEntity)

    /**
     * Deletes a pack by ID.
     */
    @Query("DELETE FROM packs WHERE id = :packId")
    suspend fun deleteById(packId: String)

    /**
     * Gets a pack by ID.
     */
    @Query("SELECT * FROM packs WHERE id = :packId")
    suspend fun getById(packId: String): PackEntity?

    /**
     * Gets a pack by ID as Flow.
     */
    @Query("SELECT * FROM packs WHERE id = :packId")
    fun getByIdFlow(packId: String): Flow<PackEntity?>

    /**
     * Gets all packs.
     */
    @Query("SELECT * FROM packs ORDER BY installedAt DESC")
    suspend fun getAll(): List<PackEntity>

    /**
     * Gets all packs as Flow.
     */
    @Query("SELECT * FROM packs ORDER BY installedAt DESC")
    fun getAllFlow(): Flow<List<PackEntity>>

    /**
     * Gets packs by install mode.
     */
    @Query("SELECT * FROM packs WHERE installMode = :mode ORDER BY installedAt DESC")
    fun getByMode(mode: String): Flow<List<PackEntity>>

    /**
     * Gets packs by type.
     */
    @Query("SELECT * FROM packs WHERE type = :type ORDER BY installedAt DESC")
    fun getByType(type: String): Flow<List<PackEntity>>

    /**
     * Checks if a pack exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM packs WHERE id = :packId)")
    suspend fun exists(packId: String): Boolean
}
