package com.builder.data.repository

import android.util.Base64
import com.builder.core.repository.GitHubRepository
import com.builder.core.repository.ProjectType
import com.builder.core.repository.SetupResult
import com.builder.core.model.github.DeviceFlowState
import com.builder.data.remote.github.GitHubApiService
import com.builder.data.remote.github.GitHubOAuthManager
import com.builder.core.model.github.*
import com.builder.data.di.GitHubClient
import com.builder.data.workflow.WorkflowGenerator
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
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
    @GitHubClient private val httpClient: OkHttpClient,
    private val workflowGenerator: WorkflowGenerator
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

    override suspend fun cancelWorkflowRun(
        owner: String,
        repo: String,
        runId: Long
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.cancelWorkflowRun(owner, repo, runId)
                if (response.isSuccessful) {
                    Timber.i("Workflow run $runId cancelled successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to cancel workflow run: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to cancel workflow run")
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

    override suspend fun triggerWorkflowWithInputs(
        owner: String,
        repo: String,
        workflowId: String,
        ref: String,
        inputs: Map<String, String>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = WorkflowDispatchRequest(ref = ref, inputs = inputs)
                val response = apiService.triggerWorkflow(owner, repo, workflowId, request)
                if (response.isSuccessful) {
                    Timber.i("Workflow triggered successfully with inputs: $inputs")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to trigger workflow: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to trigger workflow with inputs")
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

    // ========== Workflow Generation Methods ==========

    override suspend fun listWorkflows(owner: String, repo: String): Result<List<GitHubWorkflow>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listWorkflows(owner, repo)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body.workflows)
                } else {
                    Result.failure(Exception("Failed to list workflows: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list workflows")
                Result.failure(e)
            }
        }
    }

    override suspend fun getFileContents(
        owner: String,
        repo: String,
        path: String,
        ref: String?
    ): Result<FileContent> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFileContents(owner, repo, path, ref)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to get file contents: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get file contents")
                Result.failure(e)
            }
        }
    }

    override suspend fun createOrUpdateFile(
        owner: String,
        repo: String,
        path: String,
        message: String,
        content: String,
        sha: String?,
        branch: String?
    ): Result<FileUpdateResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedContent = Base64.encodeToString(
                    content.toByteArray(),
                    Base64.NO_WRAP
                )
                val request = FileUpdateRequest(
                    message = message,
                    content = encodedContent,
                    sha = sha,
                    branch = branch
                )
                val response = apiService.createOrUpdateFile(owner, repo, path, request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Timber.i("File created/updated: $path")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to create/update file: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create/update file")
                Result.failure(e)
            }
        }
    }

    override suspend fun getLanguages(owner: String, repo: String): Result<Map<String, Long>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLanguages(owner, repo)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to get languages: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get languages")
                Result.failure(e)
            }
        }
    }

    override suspend fun detectProjectType(owner: String, repo: String): Result<ProjectType> {
        return withContext(Dispatchers.IO) {
            try {
                // Check for common project files
                val checks = listOf(
                    "package.json" to ProjectType.NODEJS,
                    "requirements.txt" to ProjectType.PYTHON,
                    "setup.py" to ProjectType.PYTHON,
                    "Cargo.toml" to ProjectType.RUST,
                    "go.mod" to ProjectType.GO,
                    "build.gradle.kts" to ProjectType.KOTLIN_JVM,
                    "build.gradle" to ProjectType.JAVA,
                    "pom.xml" to ProjectType.JAVA
                )

                for ((file, type) in checks) {
                    val result = getFileContents(owner, repo, file)
                    if (result.isSuccess) {
                        Timber.i("Detected project type: $type (found $file)")
                        return@withContext Result.success(type)
                    }
                }

                // Check languages as fallback
                val languagesResult = getLanguages(owner, repo)
                if (languagesResult.isSuccess) {
                    val languages = languagesResult.getOrNull() ?: emptyMap()
                    val primaryLanguage = languages.maxByOrNull { it.value }?.key

                    val type = when (primaryLanguage?.lowercase()) {
                        "typescript", "javascript" -> ProjectType.NODEJS
                        "python" -> ProjectType.PYTHON
                        "rust" -> ProjectType.RUST
                        "go" -> ProjectType.GO
                        "kotlin" -> ProjectType.KOTLIN_JVM
                        "java" -> ProjectType.JAVA
                        "html", "css" -> ProjectType.STATIC
                        "webassembly" -> ProjectType.WASM
                        else -> ProjectType.UNKNOWN
                    }
                    Timber.i("Detected project type from language: $type ($primaryLanguage)")
                    return@withContext Result.success(type)
                }

                Result.success(ProjectType.UNKNOWN)
            } catch (e: Exception) {
                Timber.e(e, "Failed to detect project type")
                Result.failure(e)
            }
        }
    }

    override suspend fun generateDeployWorkflow(
        owner: String,
        repo: String,
        projectType: ProjectType?
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val type = projectType ?: detectProjectType(owner, repo).getOrNull() ?: ProjectType.UNKNOWN
                val packName = repo.lowercase().replace(Regex("[^a-z0-9]"), "")
                val workflow = workflowGenerator.generate(type, packName)
                Timber.i("Generated deploy workflow for $owner/$repo (type: $type)")
                Result.success(workflow)
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate deploy workflow")
                Result.failure(e)
            }
        }
    }

    override suspend fun setupBuilderDeployment(
        owner: String,
        repo: String,
        branch: String
    ): Result<FileUpdateResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Generate workflow
                val workflowResult = generateDeployWorkflow(owner, repo)
                if (workflowResult.isFailure) {
                    return@withContext Result.failure(
                        workflowResult.exceptionOrNull() ?: Exception("Failed to generate workflow")
                    )
                }
                val workflowContent = workflowResult.getOrThrow()

                // Check if file already exists to get SHA for update
                val existingFile = getFileContents(owner, repo, ".github/workflows/builder-deploy.yml")
                val sha = existingFile.getOrNull()?.sha

                // Create or update the workflow file
                val result = createOrUpdateFile(
                    owner = owner,
                    repo = repo,
                    path = ".github/workflows/builder-deploy.yml",
                    message = "Add Builder deployment workflow\n\nAutomatically generated by Builder app",
                    content = workflowContent,
                    sha = sha,
                    branch = branch
                )

                if (result.isSuccess) {
                    Timber.i("Builder deployment setup complete for $owner/$repo")
                }
                result
            } catch (e: Exception) {
                Timber.e(e, "Failed to setup Builder deployment")
                Result.failure(e)
            }
        }
    }

    override suspend fun hasBuilderDeployment(owner: String, repo: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val result = getFileContents(owner, repo, ".github/workflows/builder-deploy.yml")
                Result.success(result.isSuccess)
            } catch (e: Exception) {
                Result.success(false)
            }
        }
    }

    override suspend fun extractTunnelUrl(owner: String, repo: String, runId: Long): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Extracting tunnel URL from artifacts for run $runId")

                // First, list artifacts for this run
                val artifactsResponse = apiService.listArtifacts(owner, repo, runId)
                if (!artifactsResponse.isSuccessful) {
                    Timber.w("Failed to list artifacts: ${artifactsResponse.code()}")
                    return@withContext Result.success(null)
                }

                val artifacts = artifactsResponse.body()?.artifacts ?: emptyList()

                // Look for the tunnel-url artifact
                val tunnelArtifact = artifacts.find { it.name == "tunnel-url" }
                if (tunnelArtifact == null) {
                    Timber.d("No tunnel-url artifact found yet")
                    return@withContext Result.success(null)
                }

                Timber.d("Found tunnel-url artifact: ${tunnelArtifact.id}")

                // Download the artifact
                val downloadResponse = apiService.downloadArtifact(owner, repo, tunnelArtifact.id)
                if (!downloadResponse.isSuccessful) {
                    Timber.w("Failed to download artifact: ${downloadResponse.code()}")
                    return@withContext Result.success(null)
                }

                val body = downloadResponse.body() ?: return@withContext Result.success(null)

                // The artifact is a zip file containing tunnel_url.txt
                val tempFile = File.createTempFile("tunnel_artifact_", ".zip")
                try {
                    tempFile.outputStream().use { output ->
                        body.byteStream().copyTo(output)
                    }

                    // Extract URL from the zip
                    val tunnelUrl = extractUrlFromArtifact(tempFile)
                    Timber.i("Extracted tunnel URL: $tunnelUrl")
                    Result.success(tunnelUrl)
                } finally {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract tunnel URL")
                Result.failure(e)
            }
        }
    }

    private fun extractUrlFromArtifact(zipFile: File): String? {
        try {
            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "tunnel_url.txt" || entry.name.endsWith("/tunnel_url.txt")) {
                        val content = zis.bufferedReader().readText().trim()
                        if (content.startsWith("https://") && content.contains("trycloudflare.com")) {
                            return content
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading artifact zip file")
        }
        return null
    }

    // ========== Repository Variables Methods ==========

    override suspend fun listVariables(owner: String, repo: String): Result<VariablesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listVariables(owner, repo)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to list variables: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list variables")
                Result.failure(e)
            }
        }
    }

    override suspend fun getVariable(owner: String, repo: String, name: String): Result<RepoVariable> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getVariable(owner, repo, name)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to get variable: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get variable")
                Result.failure(e)
            }
        }
    }

    override suspend fun createVariable(owner: String, repo: String, name: String, value: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreateVariableRequest(name = name, value = value)
                val response = apiService.createVariable(owner, repo, request)
                if (response.isSuccessful) {
                    Timber.i("Variable $name created successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to create variable: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create variable")
                Result.failure(e)
            }
        }
    }

    override suspend fun updateVariable(owner: String, repo: String, name: String, value: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateVariableRequest(value = value)
                val response = apiService.updateVariable(owner, repo, name, request)
                if (response.isSuccessful) {
                    Timber.i("Variable $name updated successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update variable: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update variable")
                Result.failure(e)
            }
        }
    }

    // ========== Repository Secrets Methods ==========

    override suspend fun getRepoPublicKey(owner: String, repo: String): Result<PublicKey> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRepoPublicKey(owner, repo)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to get public key: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get public key")
                Result.failure(e)
            }
        }
    }

    override suspend fun createOrUpdateSecret(
        owner: String,
        repo: String,
        secretName: String,
        value: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Get the repository's public key
                val publicKeyResult = getRepoPublicKey(owner, repo)
                if (publicKeyResult.isFailure) {
                    return@withContext Result.failure(
                        publicKeyResult.exceptionOrNull() ?: Exception("Failed to get public key")
                    )
                }
                val publicKey = publicKeyResult.getOrThrow()

                // Encrypt the secret value using libsodium sealed box
                val encryptedValue = encryptSecretValue(value, publicKey.key)

                val request = CreateSecretRequest(
                    encryptedValue = encryptedValue,
                    keyId = publicKey.keyId
                )
                val response = apiService.createOrUpdateSecret(owner, repo, secretName, request)
                if (response.isSuccessful) {
                    Timber.i("Secret $secretName created/updated successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to create/update secret: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create/update secret")
                Result.failure(e)
            }
        }
    }

    /**
     * Encrypts a secret value using libsodium sealed box.
     */
    private fun encryptSecretValue(value: String, publicKeyBase64: String): String {
        val sodium = LazySodiumAndroid(SodiumAndroid())

        // Decode the public key from base64
        val publicKey = Base64.decode(publicKeyBase64, Base64.DEFAULT)

        // Encrypt using sealed box
        val messageBytes = value.toByteArray(Charsets.UTF_8)
        val cipherBytes = ByteArray(messageBytes.size + 48) // sealed box adds 48 bytes overhead

        val success = sodium.cryptoBoxSeal(cipherBytes, messageBytes, messageBytes.size.toLong(), publicKey)
        if (!success) {
            throw Exception("Failed to encrypt secret value")
        }

        // Return as base64
        return Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
    }

    // ========== Gist Methods ==========

    override suspend fun createGist(
        description: String?,
        public: Boolean,
        files: Map<String, String>
    ): Result<Gist> {
        return withContext(Dispatchers.IO) {
            try {
                val gistFiles = files.mapValues { GistFileContent(content = it.value) }
                val request = CreateGistRequest(
                    description = description,
                    public = public,
                    files = gistFiles
                )
                val response = apiService.createGist(request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Timber.i("Gist created: ${body.id}")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to create gist: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create gist")
                Result.failure(e)
            }
        }
    }

    override suspend fun getGist(gistId: String): Result<Gist> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getGist(gistId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to get gist: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get gist")
                Result.failure(e)
            }
        }
    }

    override suspend fun updateGist(
        gistId: String,
        description: String?,
        files: Map<String, String?>
    ): Result<Gist> {
        return withContext(Dispatchers.IO) {
            try {
                val gistFiles = files.mapValues { entry ->
                    entry.value?.let { GistFileContent(content = it) }
                }
                val request = UpdateGistRequest(
                    description = description,
                    files = gistFiles
                )
                val response = apiService.updateGist(gistId, request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Timber.i("Gist updated: ${body.id}")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Failed to update gist: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update gist")
                Result.failure(e)
            }
        }
    }

    // ========== Full Deployment Setup ==========

    override suspend fun setupFullBuilderDeployment(
        owner: String,
        repo: String,
        branch: String,
        gistToken: String?
    ): Result<SetupResult> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.i("Setting up full Builder deployment for $owner/$repo")

                var gistId: String
                var variableSet = false
                var secretSet = false
                var workflowCreated = false

                // Step 1: Check if TUNNEL_GIST_ID variable already exists
                val existingVariable = getVariable(owner, repo, "TUNNEL_GIST_ID")
                if (existingVariable.isSuccess) {
                    gistId = existingVariable.getOrThrow().value
                    Timber.i("Using existing TUNNEL_GIST_ID: $gistId")
                    variableSet = true
                } else {
                    // Step 2: Create a new Gist for tunnel status
                    val initialContent = """{"tunnel_url": null, "status": "pending", "repository": "$owner/$repo"}"""
                    val gistResult = createGist(
                        description = "Builder tunnel status for $owner/$repo",
                        public = false,
                        files = mapOf("tunnel-status.json" to initialContent)
                    )
                    if (gistResult.isFailure) {
                        return@withContext Result.failure(
                            gistResult.exceptionOrNull() ?: Exception("Failed to create gist")
                        )
                    }
                    gistId = gistResult.getOrThrow().id
                    Timber.i("Created new Gist: $gistId")

                    // Step 3: Create TUNNEL_GIST_ID variable
                    val createVarResult = createVariable(owner, repo, "TUNNEL_GIST_ID", gistId)
                    variableSet = createVarResult.isSuccess
                    if (!variableSet) {
                        Timber.w("Failed to create TUNNEL_GIST_ID variable")
                    }
                }

                // Step 4: Set GIST_TOKEN secret if provided
                if (gistToken != null && gistToken.isNotBlank()) {
                    val secretResult = createOrUpdateSecret(owner, repo, "GIST_TOKEN", gistToken)
                    secretSet = secretResult.isSuccess
                    if (!secretSet) {
                        Timber.w("Failed to set GIST_TOKEN secret")
                    }
                }

                // Step 5: Create/update builder-deploy.yml workflow
                val workflowResult = setupBuilderDeployment(owner, repo, branch)
                workflowCreated = workflowResult.isSuccess
                if (!workflowCreated) {
                    Timber.w("Failed to create workflow file")
                }

                Timber.i("Full setup complete: gistId=$gistId, variable=$variableSet, secret=$secretSet, workflow=$workflowCreated")
                Result.success(
                    SetupResult(
                        gistId = gistId,
                        workflowCreated = workflowCreated,
                        variableSet = variableSet,
                        secretSet = secretSet
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to setup full Builder deployment")
                Result.failure(e)
            }
        }
    }
}
