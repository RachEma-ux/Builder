package com.builder.ui.screens.deploy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

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

    // History state
    val workflowRuns: List<WorkflowRun> = emptyList(),
    val isLoadingHistory: Boolean = false,

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
                // Clear releases when repo changes
                releases = emptyList(),
                selectedRelease = null
            )
        }
        loadReleases(repository.owner.login, repository.name)
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
        if (state.owner.isBlank() || state.repo.isBlank()) {
            Timber.w("Deploy: Cannot trigger - owner or repo is blank")
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
            log(LogLevel.INFO, "Triggering deployment for ${state.owner}/${state.repo}", mapOf(
                "version" to state.version,
                "duration" to state.duration,
                "runApp" to state.runApp.toString()
            ))

            val result = gitHubRepository.triggerWorkflowWithInputs(
                owner = state.owner,
                repo = state.repo,
                workflowId = WORKFLOW_FILE,
                ref = "main",
                inputs = inputs
            )

            result.fold(
                onSuccess = {
                    Timber.i("Deploy: Deployment triggered successfully for ${state.owner}/${state.repo} v${state.version}")
                    log(LogLevel.INFO, "SUCCESS: Deployment triggered for ${state.owner}/${state.repo} v${state.version}")
                    _uiState.update {
                        it.copy(
                            isTriggering = false,
                            message = "Deployment triggered! Switching to status tab...",
                            selectedTab = DeployTab.STATUS
                        )
                    }
                    // Wait a moment for GitHub to create the run, then find it
                    delay(2000)
                    findLatestRun()
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to trigger deployment for ${state.owner}/${state.repo}")
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
            log(LogLevel.INFO, "Finding latest run for ${state.owner}/${state.repo}")
            val result = gitHubRepository.listWorkflowRuns(state.owner, state.repo)

            result.fold(
                onSuccess = { runs ->
                    val latestRun = runs.firstOrNull()
                    if (latestRun != null) {
                        Timber.i("Deploy: Found latest run #${latestRun.runNumber} (ID: ${latestRun.id}) - status: ${latestRun.status}")
                        log(LogLevel.INFO, "Found run #${latestRun.runNumber} - status: ${latestRun.status}")
                        _uiState.update {
                            it.copy(
                                activeRunId = latestRun.id,
                                activeRun = latestRun
                            )
                        }
                        startPolling(latestRun.id)
                    } else {
                        Timber.w("Deploy: No workflow runs found for ${state.owner}/${state.repo}")
                        log(LogLevel.WARN, "No workflow runs found for ${state.owner}/${state.repo}")
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Deploy: Failed to find latest run for ${state.owner}/${state.repo}")
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
                selectedTab = DeployTab.STATUS
            )
        }
        if (run.isRunning()) {
            startPolling(run.id)
        }
    }

    private fun startPolling(runId: Long) {
        Timber.d("Deploy: Starting polling for run ID $runId")
        log(LogLevel.INFO, "Starting polling for run ID $runId")
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPolling = true) }

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

    fun stopPolling() {
        pollingJob?.cancel()
        _uiState.update { it.copy(isPolling = false) }
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
