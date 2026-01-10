package com.builder.core.repository

import com.builder.core.model.Instance
import com.builder.core.model.Pack
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for instance operations.
 */
interface InstanceRepository {
    /**
     * Creates a new instance for a pack.
     */
    suspend fun createInstance(pack: Pack, name: String): Result<Instance>

    /**
     * Starts an instance.
     */
    suspend fun startInstance(
        instance: Instance,
        pack: Pack,
        envVars: Map<String, String> = emptyMap()
    ): Result<Unit>

    /**
     * Pauses an instance.
     */
    suspend fun pauseInstance(instance: Instance): Result<Unit>

    /**
     * Stops an instance.
     */
    suspend fun stopInstance(instance: Instance): Result<Unit>

    /**
     * Deletes an instance.
     */
    suspend fun deleteInstance(instanceId: Long): Result<Unit>

    /**
     * Gets an instance by ID.
     */
    suspend fun getInstance(instanceId: Long): Instance?

    /**
     * Gets all instances as Flow.
     */
    fun getAllInstances(): Flow<List<Instance>>

    /**
     * Gets instances for a specific pack.
     */
    fun getInstancesForPack(packId: String): Flow<List<Instance>>

    /**
     * Gets running instances.
     */
    fun getRunningInstances(): Flow<List<Instance>>
}
