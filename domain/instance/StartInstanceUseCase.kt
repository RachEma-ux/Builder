package com.builder.domain.instance

import com.builder.core.model.Instance
import com.builder.core.model.Pack
import com.builder.core.repository.InstanceRepository
import com.builder.core.repository.PackRepository
import javax.inject.Inject

/**
 * Use case for starting an instance.
 */
class StartInstanceUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository,
    private val packRepository: PackRepository
) {
    suspend operator fun invoke(
        instance: Instance,
        envVars: Map<String, String> = emptyMap()
    ): Result<Unit> {
        // Get the pack
        val pack = packRepository.getPackById(instance.packId)
            ?: return Result.failure(IllegalStateException("Pack not found: ${instance.packId}"))

        // Validate required environment variables
        val missingVars = pack.requiredSecrets().filter { key -> !envVars.containsKey(key) }
        if (missingVars.isNotEmpty()) {
            return Result.failure(
                IllegalArgumentException("Missing required secrets: ${missingVars.joinToString()}")
            )
        }

        // Start the instance
        return instanceRepository.startInstance(instance, pack, envVars)
    }
}
