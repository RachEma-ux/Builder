package com.builder.domain.github

import com.builder.core.repository.GitHubRepository
import com.builder.data.remote.github.models.Repository
import javax.inject.Inject

/**
 * Use case for listing GitHub repositories.
 */
class ListRepositoriesUseCase @Inject constructor(
    private val gitHubRepository: GitHubRepository
) {
    suspend operator fun invoke(): Result<List<Repository>> {
        return gitHubRepository.listRepositories()
    }
}
