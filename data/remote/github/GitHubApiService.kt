package com.builder.data.remote.github

import com.builder.data.remote.github.models.*
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
    ): Response<List<Tag>>

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
}
