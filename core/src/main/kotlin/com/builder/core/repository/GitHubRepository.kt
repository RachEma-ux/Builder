package com.builder.core.repository

import com.builder.core.model.github.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Detected project type for workflow generation.
 */
enum class ProjectType {
    NODEJS,        // package.json present
    PYTHON,        // requirements.txt or setup.py present
    RUST,          // Cargo.toml present
    GO,            // go.mod present
    KOTLIN_JVM,    // build.gradle.kts with Kotlin
    JAVA,          // build.gradle or pom.xml
    WASM,          // .wasm files or wat files
    STATIC,        // HTML/CSS/JS only
    UNKNOWN        // Cannot determine
}

/**
 * Repository interface for GitHub operations.
 */
interface GitHubRepository {
    /**
     * Observable auth state that emits when authentication status changes.
     * Observe this to react immediately to OAuth callback completion.
     */
    val authState: StateFlow<DeviceFlowState?>

    /**
     * Initiates OAuth authorization code flow with PKCE.
     * Opens browser and waits for callback via deep link.
     */
    fun initiateAuthCodeFlow(): Flow<DeviceFlowState>

    /**
     * Initiates OAuth device flow (LEGACY).
     * Consider using initiateAuthCodeFlow() for better UX.
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
     * Triggers a workflow dispatch with inputs.
     */
    suspend fun triggerWorkflowWithInputs(
        owner: String,
        repo: String,
        workflowId: String,
        ref: String,
        inputs: Map<String, String>
    ): Result<Unit>

    /**
     * Lists artifacts for a workflow run.
     */
    suspend fun listArtifacts(owner: String, repo: String, runId: Long): Result<List<Artifact>>

    /**
     * Downloads a file from GitHub (artifact or release asset) to a local path.
     */
    suspend fun downloadFile(url: String, destination: String): Result<Unit>

    /**
     * Downloads a file from GitHub and returns its text content.
     * Useful for small text files like checksums.sha256.
     */
    suspend fun downloadFile(url: String): Result<String>

    // ========== Workflow Generation Methods ==========

    /**
     * Lists GitHub Actions workflows for a repository.
     */
    suspend fun listWorkflows(owner: String, repo: String): Result<List<GitHubWorkflow>>

    /**
     * Gets file contents from a repository.
     */
    suspend fun getFileContents(
        owner: String,
        repo: String,
        path: String,
        ref: String? = null
    ): Result<FileContent>

    /**
     * Creates or updates a file in a repository.
     */
    suspend fun createOrUpdateFile(
        owner: String,
        repo: String,
        path: String,
        message: String,
        content: String,
        sha: String? = null,
        branch: String? = null
    ): Result<FileUpdateResponse>

    /**
     * Gets repository languages to help detect project type.
     */
    suspend fun getLanguages(owner: String, repo: String): Result<Map<String, Long>>

    /**
     * Detects the project type by analyzing repository contents.
     */
    suspend fun detectProjectType(owner: String, repo: String): Result<ProjectType>

    /**
     * Generates a Builder-compatible deploy workflow for a repository.
     * Returns the YAML content.
     */
    suspend fun generateDeployWorkflow(
        owner: String,
        repo: String,
        projectType: ProjectType? = null
    ): Result<String>

    /**
     * Sets up a repository for Builder deployment.
     * Creates the .github/workflows/builder-deploy.yml file.
     */
    suspend fun setupBuilderDeployment(
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<FileUpdateResponse>

    /**
     * Checks if a repository already has Builder deployment configured.
     */
    suspend fun hasBuilderDeployment(owner: String, repo: String): Result<Boolean>
}
