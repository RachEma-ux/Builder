package com.builder.domain.instance

import com.builder.core.model.Instance
import com.builder.core.model.Pack
import com.builder.core.repository.InstanceRepository
import javax.inject.Inject

/**
 * Use case for creating a new instance.
 */
class CreateInstanceUseCase @Inject constructor(
    private val instanceRepository: InstanceRepository
) {
    suspend operator fun invoke(pack: Pack, name: String): Result<Instance> {
        return instanceRepository.createInstance(pack, name)
    }
}
