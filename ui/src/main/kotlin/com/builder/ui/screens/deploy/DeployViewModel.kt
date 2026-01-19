package com.builder.ui.screens.deploy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.github.WorkflowRun
import com.builder.core.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeployUiState())
    val uiState: StateFlow<DeployUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    companion object {
        private const val WORKFLOW_FILE = "builder-deploy.yml"
        private const val POLLING_INTERVAL_MS = 5000L

        // Default repository for MyNewAp1Claude
        private const val DEFAULT_OWNER = "ntninjadev"
        private const val DEFAULT_REPO = "MyNewAp1Claude"
    }

    init {
        checkAuth()
        setDefaultRepo()
    }

    private fun checkAuth() {
        _uiState.update { it.copy(isAuthenticated = gitHubRepository.isAuthenticated()) }
    }

    private fun setDefaultRepo() {
        _uiState.update {
            it.copy(
                owner = DEFAULT_OWNER,
                repo = DEFAULT_REPO
            )
        }
    }

    fun selectTab(tab: DeployTab) {
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

            val result = gitHubRepository.triggerWorkflowWithInputs(
                owner = state.owner,
                repo = state.repo,
                workflowId = WORKFLOW_FILE,
                ref = "main",
                inputs = inputs
            )

            result.fold(
                onSuccess = {
                    Timber.i("Deployment triggered successfully")
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
                    Timber.e(e, "Failed to trigger deployment")
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
            val result = gitHubRepository.listWorkflowRuns(state.owner, state.repo)

            result.fold(
                onSuccess = { runs ->
                    val latestRun = runs.firstOrNull()
                    if (latestRun != null) {
                        _uiState.update {
                            it.copy(
                                activeRunId = latestRun.id,
                                activeRun = latestRun
                            )
                        }
                        startPolling(latestRun.id)
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Failed to find latest run")
                }
            )
        }
    }

    fun viewRun(run: WorkflowRun) {
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
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPolling = true) }

            while (true) {
                val state = _uiState.value
                val result = gitHubRepository.getWorkflowRun(state.owner, state.repo, runId)

                result.fold(
                    onSuccess = { run ->
                        _uiState.update { it.copy(activeRun = run) }

                        if (run.isComplete()) {
                            _uiState.update {
                                it.copy(
                                    isPolling = false,
                                    message = if (run.isSuccess())
                                        "Deployment completed successfully!"
                                    else
                                        "Deployment finished with status: ${run.conclusion}"
                                )
                            }
                            break
                        }
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to poll workflow run")
                        _uiState.update { it.copy(isPolling = false) }
                        break
                    }
                )

                delay(POLLING_INTERVAL_MS)
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
            if (state.owner.isBlank() || state.repo.isBlank()) return@launch

            _uiState.update { it.copy(isLoadingHistory = true) }

            val result = gitHubRepository.listWorkflowRuns(state.owner, state.repo)

            result.fold(
                onSuccess = { runs ->
                    _uiState.update {
                        it.copy(
                            workflowRuns = runs,
                            isLoadingHistory = false
                        )
                    }
                },
                onFailure = { e ->
                    Timber.e(e, "Failed to load history")
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
