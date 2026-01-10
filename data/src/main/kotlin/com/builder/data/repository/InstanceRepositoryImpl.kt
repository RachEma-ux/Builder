package com.builder.data.repository

import com.builder.core.model.Instance
import com.builder.core.model.Pack
import com.builder.core.repository.InstanceRepository
import com.builder.runtime.instance.InstanceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of InstanceRepository.
 */
@Singleton
class InstanceRepositoryImpl @Inject constructor(
    private val instanceManager: InstanceManager
) : InstanceRepository {

    override suspend fun createInstance(pack: Pack, name: String): Result<Instance> {
        return instanceManager.createInstance(pack, name)
    }

    override suspend fun startInstance(
        instance: Instance,
        pack: Pack,
        envVars: Map<String, String>
    ): Result<Unit> {
        return instanceManager.startInstance(instance, pack, envVars)
    }

    override suspend fun pauseInstance(instance: Instance): Result<Unit> {
        return instanceManager.pauseInstance(instance)
    }

    override suspend fun stopInstance(instance: Instance): Result<Unit> {
        return instanceManager.stopInstance(instance)
    }

    override suspend fun deleteInstance(instanceId: Long): Result<Unit> {
        return instanceManager.deleteInstance(instanceId)
    }

    override suspend fun getInstance(instanceId: Long): Instance? {
        return instanceManager.getInstance(instanceId)
    }

    override fun getAllInstances(): Flow<List<Instance>> {
        return instanceManager.getAllInstances()
    }

    override fun getInstancesForPack(packId: String): Flow<List<Instance>> {
        return instanceManager.getInstancesForPack(packId)
    }

    override fun getRunningInstances(): Flow<List<Instance>> {
        return instanceManager.getRunningInstances()
    }
}
