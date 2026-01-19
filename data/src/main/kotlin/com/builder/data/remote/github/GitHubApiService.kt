package com.builder.data.remote.github

import com.builder.core.model.github.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * GitHub REST API service interface.
 * See https://docs.github.com/en/rest for API documentation.
 */
interface GitHubApiService {
    companion object {
        const val BASE_URL = "https://api.github.com/"
    }

    // ========== Repositories ==========

    /**
     * List repositories for the authenticated user.
     * GET /user/repos
     */
    @GET("user/repos")
    suspend fun listRepositories(
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc"
    ): Response<List<Repository>>

    /**
     * Get a specific repository.
     * GET /repos/{owner}/{repo}
     */
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Repository>

    // ========== Branches ==========

    /**
     * List branches for a repository.
     * GET /repos/{owner}/{repo}/branches
     */
    @GET("repos/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): Response<List<Branch>>

    /**
     * Get a specific branch.
     * GET /repos/{owner}/{repo}/branches/{branch}
     */
    @GET("repos/{owner}/{repo}/branches/{branch}")
    suspend fun getBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): Response<Branch>

    // ========== Tags ==========

    /**
     * List tags for a repository.
     * GET /repos/{owner}/{repo}/tags
     */
    @GET("repos/{owner}/{repo}/tags")
    suspend fun listTags(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): Response<List<com.builder.core.model.github.Tag>>

    // ========== Releases ==========

    /**
     * List releases for a repository.
     * GET /repos/{owner}/{repo}/releases
     */
    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): Response<List<Release>>

    /**
     * Get a release by tag name.
     * GET /repos/{owner}/{repo}/releases/tags/{tag}
     */
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String
    ): Response<Release>

    /**
     * Get the latest release.
     * GET /repos/{owner}/{repo}/releases/latest
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Release>

    // ========== Workflow Runs ==========

    /**
     * List workflow runs for a repository.
     * GET /repos/{owner}/{repo}/actions/runs
     */
    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun listWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("branch") branch: String? = null,
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int = 30
    ): Response<WorkflowRunsResponse>

    /**
     * Get a specific workflow run.
     * GET /repos/{owner}/{repo}/actions/runs/{run_id}
     */
    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<WorkflowRun>

    /**
     * Cancel a workflow run.
     * POST /repos/{owner}/{repo}/actions/runs/{run_id}/cancel
     */
    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
    suspend fun cancelWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<Unit>

    /**
     * Trigger a workflow dispatch event.
     * POST /repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches
     */
    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun triggerWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body request: WorkflowDispatchRequest
    ): Response<Unit>

    // ========== Artifacts ==========

    /**
     * List artifacts for a workflow run.
     * GET /repos/{owner}/{repo}/actions/runs/{run_id}/artifacts
     */
    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun listArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
        @Query("per_page") perPage: Int = 100
    ): Response<ArtifactsResponse>

    /**
     * Download an artifact.
     * GET /repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip
     * Note: This requires following redirects.
     */
    @Streaming
    @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip")
    suspend fun downloadArtifact(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifact_id") artifactId: Long
    ): Response<ResponseBody>

    // ========== Downloads ==========

    /**
     * Download a release asset.
     * Note: Use the browser_download_url directly with OkHttp.
     */
    @Streaming
    @GET
    suspend fun downloadFile(
        @Url url: String
    ): Response<ResponseBody>

    // ========== File Operations (for workflow generation) ==========

    /**
     * Get file contents from a repository.
     * GET /repos/{owner}/{repo}/contents/{path}
     */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): Response<FileContent>

    /**
     * Create or update a file in a repository.
     * PUT /repos/{owner}/{repo}/contents/{path}
     */
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: FileUpdateRequest
    ): Response<FileUpdateResponse>

    /**
     * List workflows for a repository.
     * GET /repos/{owner}/{repo}/actions/workflows
     */
    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun listWorkflows(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): Response<WorkflowsResponse>

    /**
     * Get repository languages.
     * GET /repos/{owner}/{repo}/languages
     */
    @GET("repos/{owner}/{repo}/languages")
    suspend fun getLanguages(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Map<String, Long>>

    // ========== Workflow Logs ==========

    /**
     * Download workflow run logs.
     * GET /repos/{owner}/{repo}/actions/runs/{run_id}/logs
     * Returns a redirect to a zip file containing logs.
     */
    @Streaming
    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/logs")
    suspend fun downloadWorkflowLogs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<ResponseBody>

    // ========== Repository Variables ==========

    /**
     * List repository variables.
     * GET /repos/{owner}/{repo}/actions/variables
     */
    @GET("repos/{owner}/{repo}/actions/variables")
    suspend fun listVariables(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<VariablesResponse>

    /**
     * Get a repository variable.
     * GET /repos/{owner}/{repo}/actions/variables/{name}
     */
    @GET("repos/{owner}/{repo}/actions/variables/{name}")
    suspend fun getVariable(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("name") name: String
    ): Response<RepoVariable>

    /**
     * Create a repository variable.
     * POST /repos/{owner}/{repo}/actions/variables
     */
    @POST("repos/{owner}/{repo}/actions/variables")
    suspend fun createVariable(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateVariableRequest
    ): Response<Unit>

    /**
     * Update a repository variable.
     * PATCH /repos/{owner}/{repo}/actions/variables/{name}
     */
    @PATCH("repos/{owner}/{repo}/actions/variables/{name}")
    suspend fun updateVariable(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("name") name: String,
        @Body request: UpdateVariableRequest
    ): Response<Unit>

    // ========== Repository Secrets ==========

    /**
     * Get repository public key for encrypting secrets.
     * GET /repos/{owner}/{repo}/actions/secrets/public-key
     */
    @GET("repos/{owner}/{repo}/actions/secrets/public-key")
    suspend fun getRepoPublicKey(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<PublicKey>

    /**
     * Create or update a repository secret.
     * PUT /repos/{owner}/{repo}/actions/secrets/{secret_name}
     */
    @PUT("repos/{owner}/{repo}/actions/secrets/{secret_name}")
    suspend fun createOrUpdateSecret(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("secret_name") secretName: String,
        @Body request: CreateSecretRequest
    ): Response<Unit>

    // ========== Gists ==========

    /**
     * Create a gist.
     * POST /gists
     */
    @POST("gists")
    suspend fun createGist(
        @Body request: CreateGistRequest
    ): Response<Gist>

    /**
     * Get a gist.
     * GET /gists/{gist_id}
     */
    @GET("gists/{gist_id}")
    suspend fun getGist(
        @Path("gist_id") gistId: String
    ): Response<Gist>

    /**
     * Update a gist.
     * PATCH /gists/{gist_id}
     */
    @PATCH("gists/{gist_id}")
    suspend fun updateGist(
        @Path("gist_id") gistId: String,
        @Body request: UpdateGistRequest
    ): Response<Gist>
}
