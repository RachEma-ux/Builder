package com.builder.domain.instance

import com.builder.core.model.Instance
import com.builder.core.repository.InstanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for pausing an instance.
 */
class PauseInstanceUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    suspend operator fun invoke(instance: Instance): Result<Unit> {
        return instanceRepository.pauseInstance(instance)
    }
}

/**
 * Use case for stopping an instance.
 */
class StopInstanceUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    suspend operator fun invoke(instance: Instance): Result<Unit> {
        return instanceRepository.stopInstance(instance)
    }
}

/**
 * Use case for deleting an instance.
 */
class DeleteInstanceUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    suspend operator fun invoke(instanceId: Long): Result<Unit> {
        return instanceRepository.deleteInstance(instanceId)
    }
}

/**
 * Use case for getting all instances.
 */
class GetAllInstancesUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    operator fun invoke(): Flow<List<Instance>> {
        return instanceRepository.getAllInstances()
    }
}

/**
 * Use case for getting instances for a pack.
 */
class GetInstancesForPackUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    operator fun invoke(packId: String): Flow<List<Instance>> {
        return instanceRepository.getInstancesForPack(packId)
    }
}

/**
 * Use case for getting running instances.
 */
class GetRunningInstancesUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    operator fun invoke(): Flow<List<Instance>> {
        return instanceRepository.getRunningInstances()
    }
}
