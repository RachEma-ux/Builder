package com.builder.data.local.db.dao

import androidx.room.*
import com.builder.data.local.db.entities.InstanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for instance database operations.
 */
@Dao
interface InstanceDao {
    /**
     * Inserts an instance.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instance: InstanceEntity): Long

    /**
     * Updates an instance.
     */
    @Update
    suspend fun update(instance: InstanceEntity)

    /**
     * Deletes an instance.
     */
    @Delete
    suspend fun delete(instance: InstanceEntity)

    /**
     * Deletes an instance by ID.
     */
    @Query("DELETE FROM instances WHERE id = :instanceId")
    suspend fun deleteById(instanceId: Long)

    /**
     * Deletes all instances for a pack.
     */
    @Query("DELETE FROM instances WHERE packId = :packId")
    suspend fun deleteByPackId(packId: String)

    /**
     * Gets an instance by ID.
     */
    @Query("SELECT * FROM instances WHERE id = :instanceId")
    suspend fun getById(instanceId: Long): InstanceEntity?

    /**
     * Gets an instance by ID as Flow.
     */
    @Query("SELECT * FROM instances WHERE id = :instanceId")
    fun getByIdFlow(instanceId: Long): Flow<InstanceEntity?>

    /**
     * Gets all instances.
     */
    @Query("SELECT * FROM instances ORDER BY createdAt DESC")
    suspend fun getAll(): List<InstanceEntity>

    /**
     * Gets all instances as Flow.
     */
    @Query("SELECT * FROM instances ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<InstanceEntity>>

    /**
     * Gets instances for a specific pack.
     */
    @Query("SELECT * FROM instances WHERE packId = :packId ORDER BY createdAt DESC")
    fun getByPackId(packId: String): Flow<List<InstanceEntity>>

    /**
     * Gets instances by state.
     */
    @Query("SELECT * FROM instances WHERE state = :state ORDER BY createdAt DESC")
    fun getByState(state: String): Flow<List<InstanceEntity>>

    /**
     * Gets running instances.
     */
    @Query("SELECT * FROM instances WHERE state = 'running' ORDER BY createdAt DESC")
    fun getRunning(): Flow<List<InstanceEntity>>

    /**
     * Gets count of instances for a pack.
     */
    @Query("SELECT COUNT(*) FROM instances WHERE packId = :packId")
    suspend fun getCountByPackId(packId: String): Int
}
