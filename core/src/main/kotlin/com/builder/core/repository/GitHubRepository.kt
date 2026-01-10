package com.builder.core.repository

import com.builder.core.model.github.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for GitHub operations.
 */
interface GitHubRepository {
    /**
     * Initiates OAuth device flow.
     */
    fun initiateDeviceFlow(): Flow<DeviceFlowState>

    /**
     * Checks if user is authenticated.
     */
    fun isAuthenticated(): Boolean

    /**
     * Logs out the user.
     */
    fun logout()

    /**
     * Lists repositories for the authenticated user.
     */
    suspend fun listRepositories(): Result<List<Repository>>

    /**
     * Lists branches for a repository.
     */
    suspend fun listBranches(owner: String, repo: String): Result<List<Branch>>

    /**
     * Lists tags for a repository.
     */
    suspend fun listTags(owner: String, repo: String): Result<List<Tag>>

    /**
     * Lists releases for a repository.
     */
    suspend fun listReleases(owner: String, repo: String): Result<List<Release>>

    /**
     * Gets a release by tag.
     */
    suspend fun getReleaseByTag(owner: String, repo: String, tag: String): Result<Release>

    /**
     * Lists workflow runs for a repository.
     */
    suspend fun listWorkflowRuns(
        owner: String,
        repo: String,
        branch: String? = null
    ): Result<List<WorkflowRun>>

    /**
     * Gets a specific workflow run.
     */
    suspend fun getWorkflowRun(owner: String, repo: String, runId: Long): Result<WorkflowRun>

    /**
     * Triggers a workflow dispatch.
     */
    suspend fun triggerWorkflow(
        owner: String,
        repo: String,
        workflowId: String,
        ref: String
    ): Result<Unit>

    /**
     * Lists artifacts for a workflow run.
     */
    suspend fun listArtifacts(owner: String, repo: String, runId: Long): Result<List<Artifact>>

    /**
     * Downloads a file from GitHub (artifact or release asset).
     */
    suspend fun downloadFile(url: String, destination: String): Result<Unit>
}
