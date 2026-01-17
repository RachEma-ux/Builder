package com.builder.data.repository

import com.builder.core.repository.GitHubRepository
import com.builder.core.model.github.DeviceFlowState
import com.builder.data.remote.github.GitHubApiService
import com.builder.data.remote.github.GitHubOAuthManager
import com.builder.core.model.github.*
import com.builder.data.di.GitHubClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of GitHubRepository.
 */
@Singleton
class GitHubRepositoryImpl @Inject constructor(
    private val apiService: GitHubApiService,
    private val oauthManager: GitHubOAuthManager,
    @GitHubClient private val httpClient: OkHttpClient
) : GitHubRepository {

    override val authState: StateFlow<DeviceFlowState?>
        get() = oauthManager.authState

    override fun initiateAuthCodeFlow(): Flow<DeviceFlowState> {
        return oauthManager.initiateAuthCodeFlow()
    }

    override fun initiateDeviceFlow(): Flow<DeviceFlowState> {
        return oauthManager.initiateDeviceFlow()
    }

    override fun isAuthenticated(): Boolean {
        return oauthManager.isAuthenticated()
    }

    override fun logout() {
        oauthManager.clearAccessToken()
    }

    override suspend fun listRepositories(): Result<List<Repository>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listRepositories()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to list repositories: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list repositories")
                Result.failure(e)
            }
        }
    }

    override suspend fun listBranches(owner: String, repo: String): Result<List<Branch>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listBranches(owner, repo)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to list branches: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list branches")
                Result.failure(e)
            }
        }
    }

    override suspend fun listTags(owner: String, repo: String): Result<List<Tag>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listTags(owner, repo)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to list tags: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list tags")
                Result.failure(e)
            }
        }
    }

    override suspend fun listReleases(owner: String, repo: String): Result<List<Release>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listReleases(owner, repo)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to list releases: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list releases")
                Result.failure(e)
            }
        }
    }

    override suspend fun getReleaseByTag(
        owner: String,
        repo: String,
        tag: String
    ): Result<Release> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getReleaseByTag(owner, repo, tag)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to get release: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get release")
                Result.failure(e)
            }
        }
    }

    override suspend fun listWorkflowRuns(
        owner: String,
        repo: String,
        branch: String?
    ): Result<List<WorkflowRun>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listWorkflowRuns(owner, repo, branch)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body.workflowRuns)
                } else {
                    Result.failure(Exception("Failed to list workflow runs: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list workflow runs")
                Result.failure(e)
            }
        }
    }

    override suspend fun getWorkflowRun(
        owner: String,
        repo: String,
        runId: Long
    ): Result<WorkflowRun> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWorkflowRun(owner, repo, runId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to get workflow run: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get workflow run")
                Result.failure(e)
            }
        }
    }

    override suspend fun triggerWorkflow(
        owner: String,
        repo: String,
        workflowId: String,
        ref: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = WorkflowDispatchRequest(ref = ref)
                val response = apiService.triggerWorkflow(owner, repo, workflowId, request)
                if (response.isSuccessful) {
                    Timber.i("Workflow triggered successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to trigger workflow: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to trigger workflow")
                Result.failure(e)
            }
        }
    }

    override suspend fun listArtifacts(
        owner: String,
        repo: String,
        runId: Long
    ): Result<List<Artifact>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listArtifacts(owner, repo, runId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body.artifacts)
                } else {
                    Result.failure(Exception("Failed to list artifacts: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list artifacts")
                Result.failure(e)
            }
        }
    }

    override suspend fun downloadFile(url: String, destination: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Downloading file from: $url")

                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Download failed: ${response.code}")
                    )
                }

                response.body?.use { body ->
                    FileOutputStream(File(destination)).use { output ->
                        body.byteStream().copyTo(output)
                    }
                }

                Timber.i("File downloaded successfully: $destination")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "File download failed")
                Result.failure(e)
            }
        }
    }

    override suspend fun downloadFile(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Downloading file content from: $url")

                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Download failed: ${response.code}")
                    )
                }

                val content = response.body?.string() ?: ""

                Timber.i("File content downloaded successfully (${content.length} chars)")
                Result.success(content)
            } catch (e: Exception) {
                Timber.e(e, "File content download failed")
                Result.failure(e)
            }
        }
    }
}
