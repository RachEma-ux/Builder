package com.builder.domain.github

import com.builder.core.model.github.Owner
import com.builder.core.model.github.Repository
import com.builder.core.repository.GitHubRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ListRepositoriesUseCase.
 */
class ListRepositoriesUseCaseTest {

    private lateinit var gitHubRepository: GitHubRepository
    private lateinit var useCase: ListRepositoriesUseCase

    @Before
    fun setup() {
        gitHubRepository = mockk()
        useCase = ListRepositoriesUseCase(gitHubRepository)
    }

    @Test
    fun `invoke should return repositories from repository`() = runTest {
        // Given
        val expectedRepos = listOf(
            createTestRepository(1, "repo1"),
            createTestRepository(2, "repo2")
        )
        coEvery { gitHubRepository.listRepositories() } returns Result.success(expectedRepos)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedRepos, result.getOrNull())
        coVerify { gitHubRepository.listRepositories() }
    }

    @Test
    fun `invoke should return empty list when no repositories`() = runTest {
        // Given
        coEvery { gitHubRepository.listRepositories() } returns Result.success(emptyList())

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `invoke should propagate failure from repository`() = runTest {
        // Given
        val error = Exception("Network error")
        coEvery { gitHubRepository.listRepositories() } returns Result.failure(error)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `invoke should handle authentication error`() = runTest {
        // Given
        val authError = Exception("401 Unauthorized")
        coEvery { gitHubRepository.listRepositories() } returns Result.failure(authError)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("401") == true)
    }

    private fun createTestRepository(id: Long, name: String): Repository {
        return Repository(
            id = id,
            name = name,
            fullName = "testuser/$name",
            owner = Owner(
                login = "testuser",
                id = 123,
                avatarUrl = "https://github.com/testuser.png",
                type = "User"
            ),
            description = "Test repository",
            htmlUrl = "https://github.com/testuser/$name",
            defaultBranch = "main",
            private = false,
            updatedAt = "2026-01-17T00:00:00Z"
        )
    }
}
