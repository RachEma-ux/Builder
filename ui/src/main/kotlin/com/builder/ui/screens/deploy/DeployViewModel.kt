package com.builder.ui.screens.deploy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.github.Branch
import com.builder.core.model.github.Release
import com.builder.core.model.github.Repository
import com.builder.core.model.github.WorkflowRun
import com.builder.core.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import com.builder.core.model.Log
import com.builder.core.model.LogLevel
import com.builder.core.model.LogSource
import com.builder.core.repository.LogRepository
import com.builder.core.util.DebugLogger
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Tab selection for the deploy screen.
 */
enum class DeployTab {
    DEPLOY, STATUS, HISTORY
}

/**
 * UI state for deploy screen.
 */
data class DeployUiState(
    val selectedTab: DeployTab = DeployTab.DEPLOY,
    val isAuthenticated: Boolean = false,

    // Repository selection
    val repositories: List<Repository> = emptyList(),
    val selectedRepository: Repository? = null,
    val isLoadingRepositories: Boolean = false,

    // Branch selection
    val branches: List<Branch> = emptyList(),
    val selectedBranch: Branch? = null,
    val isLoadingBranches: Boolean = false,

    // Release selection
    val releases: List<Release> = emptyList(),
    val selectedRelease: Release? = null,
    val isLoadingReleases: Boolean = false,

    // Deploy form state
    val version: String = "2.0.0",
    val duration: String = "15",
    val runApp: Boolean = true,
    val owner: String = "",
    val repo: String = "",
    val isTriggering: Boolean = false,

    // Status state
    val activeRunId: Long? = null,
    val activeRun: WorkflowRun? = null,
    val isPolling: Boolean = false,
    val tunnelUrl: String? = null,
    val isFetchingUrl: Boolean = false,
    val deployVersion: String? = null,
    val deployType: String? = null, // "new" or "redeploy"
    val isDeployComplete: Boolean = false, // true when tunnel URL is available

    // History state
    val workflowRuns: List<WorkflowRun> = emptyList(),
    val isLoadingHistory: Boolean = false,

    // Setup state (per-repo)
    val tunnelGistId: String? = null,
    val hasBuilderWorkflow: Boolean = false,
    val isCheckingSetup: Boolean = false,
    val isSettingUp: Boolean = false,
    val needsSetup: Boolean = false,

    // Messages
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class DeployViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val logRepository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeployUiState())
    val uiState: StateFlow<DeployUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    companion object {
        private const val WORKFLOW_FILE = "builder-deploy.yml"
        private const val POLLING_INTERVAL_MS = 5000L
        private const val DEPLOY_INSTANCE_ID = "deploy-tab"
        private const val DEPLOY_PACK_ID = "app"
        private const val GIST_API_BASE = "https://api.github.com/gists/"
    }

    /**
     * Log a Deploy tab activity to the LogRepository
     */
    private fun log(level: LogLevel, message: String, metadata: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            val logEntry = Log.create(
                instanceId = DEPLOY_INSTANCE_ID,
                packId = DEPLOY_PACK_ID,
                level = level,
                message = message,
                source = LogSource.DEPLOY,
                metadata = metadata
            )
            logRepository.insert(logEntry)
        }
    }

    init {
        Timber.d("DeployViewModel initialized")
        DebugLogger.logSync("INFO", "Deploy", "DeployViewModel initialized")
        log(LogLevel.INFO, "DeployViewModel initialized")
        checkAuth()
        loadRepositories()
    }

    private fun checkAuth() {
        val isAuth = gitHubRepository.isAuthenticated()
        Timber.d("Deploy: Auth check - isAuthenticated=$isAuth")
        log(LogLevel.INFO, "Auth check - isAuthenticated=$isAuth")
        _uiState.update { it.copy(isAuthenticated = isAuth) }
    }

    fun loadRepositories() {
        viewModelScope.launch {
            Timber.d("Deploy: Loading repositories...")
            log(LogLevel.INFO, "Loading repositories...")
            _uiState.update { it.copy(isLoadingRepositories = true) }

            val result = gitHubRepository.listRepositories()

            result.fold(
                onSuccess = { repos ->
                    Timber.i("Deploy: Loaded ${repos.size} repositories")
                    log(LogLevel.INFO, "Loaded ${repos.size} repositories")
                    _uiState.update {
                        it.copy(
                            repositories = repos,
                            isLoadingRepositories = false
                        )
                    }
                    // Auto-select first repo if available
                    if (repos.isNotEmpty()) {
                        selectRepository(repos.first())
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to load repositories")
                    log(LogLevel.ERROR, "Failed to load repositories: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoadingRepositories = false,
                            error = "Failed to load repositories: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun selectRepository(repository: Repository) {
        Timber.i("Deploy: Selected repository ${repository.fullName}")
        log(LogLevel.INFO, "Selected repository: ${repository.fullName}")
        _uiState.update {
            it.copy(
                selectedRepository = repository,
                owner = repository.owner.login,
                repo = repository.name,
                // Clear branches and releases when repo changes
                branches = emptyList(),
                selectedBranch = null,
                releases = emptyList(),
                selectedRelease = null,
                // Reset setup state for new repo
                tunnelGistId = null,
                hasBuilderWorkflow = false,
                needsSetup = false,
                tunnelUrl = null
            )
        }
        loadBranches(repository.owner.login, repository.name)
        loadReleases(repository.owner.login, repository.name)
        // Check if repo has Builder deployment setup
        checkRepoSetup(repository.owner.login, repository.name)
    }

    /**
     * Checks if the repository has Builder deployment configured.
     * Looks for TUNNEL_GIST_ID variable and builder-deploy.yml workflow.
     */
    private fun checkRepoSetup(owner: String, repo: String) {
        viewModelScope.launch {
            Timber.d("Deploy: Checking setup for $owner/$repo")
            _uiState.update { it.copy(isCheckingSetup = true) }

            var gistId: String? = null
            var hasWorkflow = false

            // Check for TUNNEL_GIST_ID variable
            val varResult = gitHubRepository.getVariable(owner, repo, "TUNNEL_GIST_ID")
            varResult.fold(
                onSuccess = { variable ->
                    gistId = variable.value
                    Timber.i("Deploy: Found TUNNEL_GIST_ID: $gistId")
                    log(LogLevel.INFO, "Found TUNNEL_GIST_ID for $owner/$repo: $gistId")
                },
                onFailure = {
                    Timber.d("Deploy: No TUNNEL_GIST_ID variable found for $owner/$repo")
                }
            )

            // Check for builder-deploy.yml workflow
            val workflowResult = gitHubRepository.hasBuilderDeployment(owner, repo)
            workflowResult.fold(
                onSuccess = { has ->
                    hasWorkflow = has
                    Timber.i("Deploy: Has builder-deploy.yml: $hasWorkflow")
                    log(LogLevel.INFO, "Has builder-deploy.yml for $owner/$repo: $hasWorkflow")
                },
                onFailure = {
                    Timber.d("Deploy: Failed to check workflow for $owner/$repo")
                }
            )

            val needsSetup = gistId == null || !hasWorkflow

            _uiState.update {
                it.copy(
                    isCheckingSetup = false,
                    tunnelGistId = gistId,
                    hasBuilderWorkflow = hasWorkflow,
                    needsSetup = needsSetup
                )
            }

            if (needsSetup) {
                Timber.i("Deploy: Repository $owner/$repo needs setup (gistId=$gistId, hasWorkflow=$hasWorkflow)")
                log(LogLevel.INFO, "Repository needs setup: $owner/$repo")
            }
        }
    }

    /**
     * Sets up the repository with full Builder deployment:
     * - Creates tunnel status Gist
     * - Sets TUNNEL_GIST_ID variable
     * - Creates builder-deploy.yml workflow
     */
    fun setupRepository() {
        val state = _uiState.value
        if (state.owner.isBlank() || state.repo.isBlank()) {
            _uiState.update { it.copy(error = "No repository selected") }
            return
        }

        viewModelScope.launch {
            Timber.i("Deploy: Setting up repository ${state.owner}/${state.repo}")
            log(LogLevel.INFO, "Setting up repository: ${state.owner}/${state.repo}")
            _uiState.update { it.copy(isSettingUp = true, error = null) }

            val branch = state.selectedBranch?.name ?: "main"

            // Note: GIST_TOKEN secret needs to be set manually by the user
            // as we cannot securely store/transfer their personal access token
            val result = gitHubRepository.setupFullBuilderDeployment(
                owner = state.owner,
                repo = state.repo,
                branch = branch,
                gistToken = null // User must set this manually
            )

            result.fold(
                onSuccess = { setupResult ->
                    Timber.i("Deploy: Setup complete - gistId=${setupResult.gistId}, workflow=${setupResult.workflowCreated}")
                    log(LogLevel.INFO, "Setup complete for ${state.owner}/${state.repo}: gistId=${setupResult.gistId}")

                    _uiState.update {
                        it.copy(
                            isSettingUp = false,
                            tunnelGistId = setupResult.gistId,
                            hasBuilderWorkflow = setupResult.workflowCreated,
                            needsSetup = !setupResult.workflowCreated || !setupResult.variableSet,
                            message = buildString {
                                append("Repository setup complete!")
                                if (!setupResult.secretSet) {
                                    append("\n\nNote: Please manually add GIST_TOKEN secret to your repository for tunnel URL updates.")
                                }
                            }
                        )
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Setup failed for ${state.owner}/${state.repo}")
                    log(LogLevel.ERROR, "Setup failed: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isSettingUp = false,
                            error = "Setup failed: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadBranches(owner: String, repo: String) {
        viewModelScope.launch {
            Timber.d("Deploy: Loading branches for $owner/$repo...")
            log(LogLevel.INFO, "Loading branches for $owner/$repo")
            _uiState.update { it.copy(isLoadingBranches = true) }

            val result = gitHubRepository.listBranches(owner, repo)

            result.fold(
                onSuccess = { branches ->
                    Timber.i("Deploy: Loaded ${branches.size} branches for $owner/$repo")
                    log(LogLevel.INFO, "Loaded ${branches.size} branches for $owner/$repo")
                    _uiState.update {
                        it.copy(
                            branches = branches,
                            isLoadingBranches = false
                        )
                    }
                    // Auto-select default branch (main or master) if available
                    val defaultBranch = branches.find { it.name == "main" }
                        ?: branches.find { it.name == "master" }
                        ?: branches.firstOrNull()
                    defaultBranch?.let { selectBranch(it) }
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to load branches for $owner/$repo")
                    log(LogLevel.ERROR, "Failed to load branches for $owner/$repo: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoadingBranches = false,
                            error = "Failed to load branches: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun selectBranch(branch: Branch) {
        Timber.i("Deploy: Selected branch ${branch.name}")
        log(LogLevel.INFO, "Selected branch: ${branch.name}")
        _uiState.update { it.copy(selectedBranch = branch) }
    }

    fun loadReleases(owner: String, repo: String) {
        viewModelScope.launch {
            Timber.d("Deploy: Loading releases for $owner/$repo...")
            log(LogLevel.INFO, "Loading releases for $owner/$repo")
            _uiState.update { it.copy(isLoadingReleases = true) }

            val result = gitHubRepository.listReleases(owner, repo)

            result.fold(
                onSuccess = { releases ->
                    Timber.i("Deploy: Loaded ${releases.size} releases for $owner/$repo")
                    log(LogLevel.INFO, "Loaded ${releases.size} releases for $owner/$repo")
                    _uiState.update {
                        it.copy(
                            releases = releases,
                            isLoadingReleases = false
                        )
                    }
                    // Auto-select latest release if available
                    if (releases.isNotEmpty()) {
                        selectRelease(releases.first())
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to load releases for $owner/$repo")
                    log(LogLevel.ERROR, "Failed to load releases for $owner/$repo: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoadingReleases = false,
                            error = "Failed to load releases: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun selectRelease(release: Release) {
        Timber.i("Deploy: Selected release ${release.tagName}")
        log(LogLevel.INFO, "Selected release: ${release.tagName}")
        _uiState.update {
            it.copy(
                selectedRelease = release,
                version = release.tagName.removePrefix("v")
            )
        }
    }

    fun selectTab(tab: DeployTab) {
        Timber.d("Deploy: Tab selected - $tab")
        _uiState.update { it.copy(selectedTab = tab) }

        when (tab) {
            DeployTab.HISTORY -> loadHistory()
            DeployTab.STATUS -> {
                // Resume polling if there's an active run
                _uiState.value.activeRunId?.let { startPolling(it) }
            }
            else -> {}
        }
    }

    fun updateVersion(version: String) {
        _uiState.update { it.copy(version = version) }
    }

    fun updateDuration(duration: String) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun updateRunApp(runApp: Boolean) {
        _uiState.update { it.copy(runApp = runApp) }
    }

    fun updateOwner(owner: String) {
        _uiState.update { it.copy(owner = owner) }
    }

    fun updateRepo(repo: String) {
        _uiState.update { it.copy(repo = repo) }
    }

    fun triggerDeploy() {
        val state = _uiState.value
        DebugLogger.logSync("INFO", "Deploy", "triggerDeploy called - owner=${state.owner}, repo=${state.repo}")
        if (state.owner.isBlank() || state.repo.isBlank()) {
            Timber.w("Deploy: Cannot trigger - owner or repo is blank")
            DebugLogger.logSync("WARN", "Deploy", "Cannot trigger - owner or repo is blank")
            log(LogLevel.WARN, "Cannot trigger - owner or repo is blank")
            _uiState.update { it.copy(error = "Owner and repository are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTriggering = true, error = null) }

            val inputs = mapOf(
                "version" to state.version,
                "duration" to state.duration,
                "run_app" to if (state.runApp) "yes" else "no"
            )

            Timber.i("Deploy: Triggering deployment for ${state.owner}/${state.repo}")
            DebugLogger.logSync("INFO", "Deploy", "Triggering workflow for ${state.owner}/${state.repo}")
            log(LogLevel.INFO, "Triggering deployment for ${state.owner}/${state.repo}", mapOf(
                "version" to state.version,
                "duration" to state.duration,
                "runApp" to state.runApp.toString()
            ))

            val branchRef = state.selectedBranch?.name ?: "main"
            Timber.d("Deploy: Using branch ref: $branchRef")
            DebugLogger.logSync("INFO", "Deploy", "Using branch: $branchRef")

            val result = gitHubRepository.triggerWorkflowWithInputs(
                owner = state.owner,
                repo = state.repo,
                workflowId = WORKFLOW_FILE,
                ref = branchRef,
                inputs = inputs
            )

            result.fold(
                onSuccess = {
                    Timber.i("Deploy: Deployment triggered successfully for ${state.owner}/${state.repo} v${state.version}")
                    DebugLogger.logSync("INFO", "Deploy", "SUCCESS: Deployment triggered, switching to Status tab")
                    log(LogLevel.INFO, "SUCCESS: Deployment triggered for ${state.owner}/${state.repo} v${state.version}")

                    // Determine if this is a redeploy by checking if version exists in releases
                    val versionToCheck = state.version.removePrefix("v")
                    val isRedeploy = state.releases.any {
                        it.tagName.removePrefix("v") == versionToCheck
                    }

                    _uiState.update {
                        it.copy(
                            isTriggering = false,
                            message = "Deployment triggered! Switching to status tab...",
                            selectedTab = DeployTab.STATUS,
                            // Set version info immediately
                            deployVersion = versionToCheck,
                            deployType = if (isRedeploy) "redeploy" else "new",
                            isDeployComplete = false,
                            tunnelUrl = null
                        )
                    }
                    // Wait a moment for GitHub to create the run, then find it
                    delay(2000)
                    findLatestRun()
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to trigger deployment for ${state.owner}/${state.repo}")
                    DebugLogger.logSync("ERROR", "Deploy", "FAILED: ${e.message}")
                    log(LogLevel.ERROR, "FAILED: Deployment trigger failed for ${state.owner}/${state.repo}: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isTriggering = false,
                            error = "Failed to trigger deployment: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    private fun findLatestRun() {
        viewModelScope.launch {
            val state = _uiState.value
            Timber.d("Deploy: Finding latest run for ${state.owner}/${state.repo}")
            DebugLogger.logSync("INFO", "Deploy", "Finding latest run for ${state.owner}/${state.repo}")
            log(LogLevel.INFO, "Finding latest run for ${state.owner}/${state.repo}")
            val result = gitHubRepository.listWorkflowRuns(state.owner, state.repo)

            result.fold(
                onSuccess = { runs ->
                    val latestRun = runs.firstOrNull()
                    if (latestRun != null) {
                        Timber.i("Deploy: Found latest run #${latestRun.runNumber} (ID: ${latestRun.id}) - status: ${latestRun.status}")
                        DebugLogger.logSync("INFO", "Deploy", "Found run #${latestRun.runNumber} - status: ${latestRun.status}, isRunning: ${latestRun.isRunning()}")
                        log(LogLevel.INFO, "Found run #${latestRun.runNumber} - status: ${latestRun.status}")
                        _uiState.update {
                            it.copy(
                                activeRunId = latestRun.id,
                                activeRun = latestRun
                            )
                        }
                        DebugLogger.logSync("INFO", "Deploy", "activeRun set, starting polling")
                        startPolling(latestRun.id)
                    } else {
                        Timber.w("Deploy: No workflow runs found for ${state.owner}/${state.repo}")
                        DebugLogger.logSync("WARN", "Deploy", "No workflow runs found")
                        log(LogLevel.WARN, "No workflow runs found for ${state.owner}/${state.repo}")
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to find latest run for ${state.owner}/${state.repo}")
                    DebugLogger.logSync("ERROR", "Deploy", "Failed to find latest run: ${e.message}")
                    log(LogLevel.ERROR, "Failed to find latest run: ${e.message}")
                }
            )
        }
    }

    fun viewRun(run: WorkflowRun) {
        Timber.i("Deploy: Viewing run #${run.runNumber} (ID: ${run.id})")
        _uiState.update {
            it.copy(
                activeRunId = run.id,
                activeRun = run,
                selectedTab = DeployTab.STATUS,
                // Reset version info so it's fetched from Gist for this run
                deployVersion = null,
                deployType = null,
                tunnelUrl = null,
                isDeployComplete = false
            )
        }
        if (run.isRunning()) {
            startPolling(run.id)
        } else {
            // For completed runs, try to fetch version info from Gist once
            fetchTunnelUrl(run.id)
        }
    }

    private fun startPolling(runId: Long) {
        Timber.d("Deploy: Starting polling for run ID $runId")
        log(LogLevel.INFO, "Starting polling for run ID $runId")
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPolling = true, tunnelUrl = null, isDeployComplete = false) }

            var shouldContinue = true
            var pollCount = 0
            while (shouldContinue) {
                pollCount++
                val state = _uiState.value
                Timber.d("Deploy: Polling run $runId (poll #$pollCount)")
                val result = gitHubRepository.getWorkflowRun(state.owner, state.repo, runId)

                result.fold(
                    onSuccess = { run ->
                        Timber.d("Deploy: Run $runId status: ${run.status}, conclusion: ${run.conclusion}")
                        if (pollCount % 6 == 1) { // Log to file every 30 seconds (6 polls * 5 sec)
                            log(LogLevel.DEBUG, "Polling run $runId - status: ${run.status}")
                        }
                        _uiState.update { it.copy(activeRun = run) }

                        // Try to fetch tunnel URL while run is in progress
                        // Keep fetching until we have the URL (version is already set from triggerDeploy)
                        if (run.status == "in_progress" && state.tunnelUrl == null) {
                            // Fetch from Gist every poll (5 seconds) - Gist is updated when tunnel starts
                            fetchTunnelUrl(runId)
                        }

                        if (run.isComplete()) {
                            Timber.i("Deploy: Run $runId completed with conclusion: ${run.conclusion}")
                            log(LogLevel.INFO, "Run $runId COMPLETED - conclusion: ${run.conclusion}")
                            _uiState.update {
                                it.copy(
                                    isPolling = false,
                                    message = if (run.isSuccess())
                                        "Deployment completed successfully!"
                                    else
                                        "Deployment finished with status: ${run.conclusion}"
                                )
                            }
                            shouldContinue = false
                        }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Deploy: Failed to poll workflow run $runId")
                        log(LogLevel.ERROR, "Failed to poll run $runId: ${e.message}")
                        _uiState.update { it.copy(isPolling = false) }
                        shouldContinue = false
                    }
                )

                if (shouldContinue) {
                    delay(POLLING_INTERVAL_MS)
                }
            }
        }
    }

    fun fetchTunnelUrl(runId: Long? = null) {
        val id = runId ?: _uiState.value.activeRunId ?: return

        viewModelScope.launch {
            // Check current state, not captured state
            if (_uiState.value.isFetchingUrl) return@launch

            _uiState.update { it.copy(isFetchingUrl = true) }
            Timber.d("Deploy: Fetching tunnel URL from Gist for run $id")

            try {
                val gistResult = fetchTunnelUrlFromGist()
                if (gistResult != null) {
                    Timber.i("Deploy: Found tunnel URL from Gist: ${gistResult.tunnelUrl}")
                    log(LogLevel.INFO, "Tunnel URL found from Gist: ${gistResult.tunnelUrl}")
                    // Update URL, version, deployType and mark as complete - all atomically
                    _uiState.update { current ->
                        current.copy(
                            tunnelUrl = gistResult.tunnelUrl,
                            // Update version from Gist if available (has the actual resolved version)
                            deployVersion = gistResult.version?.takeIf { it.isNotEmpty() && it != "null" } ?: current.deployVersion,
                            deployType = gistResult.deployType?.takeIf { it.isNotEmpty() && it != "null" } ?: current.deployType,
                            isFetchingUrl = false,
                            isDeployComplete = true
                        )
                    }
                } else {
                    Timber.d("Deploy: No tunnel URL in Gist yet, trying GitHub logs...")
                    // Fallback to GitHub logs extraction
                    val state = _uiState.value
                    val result = gitHubRepository.extractTunnelUrl(state.owner, state.repo, id)
                    result.fold(
                        onSuccess = { logUrl ->
                            if (logUrl != null) {
                                Timber.i("Deploy: Found tunnel URL from logs: $logUrl")
                                log(LogLevel.INFO, "Tunnel URL found from logs: $logUrl")
                                _uiState.update { it.copy(tunnelUrl = logUrl, isFetchingUrl = false, isDeployComplete = true) }
                            } else {
                                Timber.d("Deploy: No tunnel URL found yet")
                                _uiState.update { it.copy(isFetchingUrl = false) }
                            }
                        },
                        onFailure = { e ->
                            Timber.e(e, "Deploy: Failed to fetch tunnel URL from logs")
                            _uiState.update { it.copy(isFetchingUrl = false) }
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Deploy: Failed to fetch tunnel URL from Gist")
                _uiState.update { it.copy(isFetchingUrl = false) }
            }
        }
    }

    // Data class to hold Gist fetch result
    private data class GistResult(
        val tunnelUrl: String,
        val version: String?,
        val deployType: String?
    )

    private suspend fun fetchTunnelUrlFromGist(): GistResult? {
        val gistId = _uiState.value.tunnelGistId
        if (gistId.isNullOrBlank()) {
            Timber.d("Deploy: No gist ID available for this repository")
            return null
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("$GIST_API_BASE$gistId")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "{}")
                        val files = json.optJSONObject("files") ?: return@withContext null
                        val tunnelFile = files.optJSONObject("tunnel-status.json") ?: return@withContext null
                        val content = tunnelFile.optString("content", null) ?: return@withContext null

                        val statusJson = JSONObject(content)
                        val tunnelUrl = statusJson.optString("tunnel_url", null)
                        val status = statusJson.optString("status", null)
                        val version = statusJson.optString("version", null)
                        val deployType = statusJson.optString("deploy_type", null)

                        // Only return data if status is "running" and URL is valid
                        // The workflow resets the Gist to "pending" at start, so stale URLs are cleared
                        if (tunnelUrl != null && tunnelUrl.isNotEmpty() && tunnelUrl != "null" && status == "running") {
                            Timber.d("Deploy: Gist data - url=$tunnelUrl, version=$version, deployType=$deployType")
                            return@withContext GistResult(tunnelUrl, version, deployType)
                        }
                    }
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Deploy: Error fetching Gist")
                null
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        _uiState.update { it.copy(isPolling = false) }
    }

    fun cancelRun() {
        val runId = _uiState.value.activeRunId ?: return
        val state = _uiState.value

        viewModelScope.launch {
            Timber.i("Deploy: Cancelling workflow run $runId")
            log(LogLevel.INFO, "Cancelling workflow run $runId")

            val result = gitHubRepository.cancelWorkflowRun(state.owner, state.repo, runId)

            result.fold(
                onSuccess = {
                    Timber.i("Deploy: Workflow run $runId cancelled successfully")
                    log(LogLevel.INFO, "Workflow run $runId cancelled")
                    stopPolling()
                    _uiState.update {
                        it.copy(message = "Deployment cancelled")
                    }
                    // Refresh to get updated status
                    refreshStatus()
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to cancel workflow run $runId")
                    log(LogLevel.ERROR, "Failed to cancel run $runId: ${e.message}")
                    _uiState.update {
                        it.copy(error = "Failed to cancel: ${e.message}")
                    }
                }
            )
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.owner.isBlank() || state.repo.isBlank()) {
                Timber.w("Deploy: Cannot load history - owner or repo is blank")
                return@launch
            }

            Timber.d("Deploy: Loading history for ${state.owner}/${state.repo}")
            log(LogLevel.INFO, "Loading history for ${state.owner}/${state.repo}")
            _uiState.update { it.copy(isLoadingHistory = true) }

            val result = gitHubRepository.listWorkflowRuns(state.owner, state.repo)

            result.fold(
                onSuccess = { runs ->
                    Timber.i("Deploy: Loaded ${runs.size} workflow runs for history")
                    log(LogLevel.INFO, "Loaded ${runs.size} runs for history")
                    _uiState.update {
                        it.copy(
                            workflowRuns = runs,
                            isLoadingHistory = false
                        )
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to load history for ${state.owner}/${state.repo}")
                    log(LogLevel.ERROR, "Failed to load history: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            error = "Failed to load history: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun refreshStatus() {
        _uiState.value.activeRunId?.let { runId ->
            viewModelScope.launch {
                val state = _uiState.value
                val result = gitHubRepository.getWorkflowRun(state.owner, state.repo, runId)

                result.fold(
                    onSuccess = { run ->
                        _uiState.update { it.copy(activeRun = run) }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to refresh status")
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
