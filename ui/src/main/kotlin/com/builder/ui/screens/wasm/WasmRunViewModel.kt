package com.builder.ui.screens.wasm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.WasmExecutionResult
import com.builder.core.model.WasmExecutionState
import com.builder.core.repository.GitHubRepository
import com.builder.core.model.github.*
import com.builder.domain.wasm.RunWasmPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for WASM Run screen.
 * Handles triggering WASM execution on GitHub Actions and displaying results.
 */
@HiltViewModel
class WasmRunViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val runWasmPackUseCase: RunWasmPackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasmRunUiState())
    val uiState: StateFlow<WasmRunUiState> = _uiState.asStateFlow()

    init {
        checkAuthentication()
    }

    private fun checkAuthentication() {
        _uiState.update {
            it.copy(isAuthenticated = gitHubRepository.isAuthenticated())
        }
        if (gitHubRepository.isAuthenticated()) {
            loadRepositories()
        }
    }

    fun loadRepositories() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingRepositories = true) }

            gitHubRepository.listRepositories().fold(
                onSuccess = { repos ->
                    _uiState.update {
                        it.copy(
                            repositories = repos,
                            loadingRepositories = false
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load repositories")
                    _uiState.update {
                        it.copy(
                            loadingRepositories = false,
                            error = "Failed to load repositories: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun selectRepository(repo: Repository) {
        _uiState.update { it.copy(selectedRepo = repo, selectedBranch = null) }
        loadBranches(repo.owner.login, repo.name)
    }

    private fun loadBranches(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.listBranches(owner, repo).fold(
                onSuccess = { branches ->
                    _uiState.update { it.copy(branches = branches) }
                    // Auto-select default branch if available
                    val defaultBranch = branches.find { it.name == "main" }
                        ?: branches.find { it.name == "master" }
                        ?: branches.firstOrNull()
                    if (defaultBranch != null) {
                        _uiState.update { it.copy(selectedBranch = defaultBranch) }
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load branches")
                }
            )
        }
    }

    fun selectBranch(branch: Branch) {
        _uiState.update { it.copy(selectedBranch = branch) }
    }

    /**
     * Triggers WASM pack execution on GitHub Actions.
     */
    fun runWasmPack() {
        val repo = _uiState.value.selectedRepo ?: return
        val branch = _uiState.value.selectedBranch ?: return

        viewModelScope.launch {
            runWasmPackUseCase(
                owner = repo.owner.login,
                repo = repo.name,
                ref = branch.name
            ).collect { state ->
                _uiState.update { it.copy(executionState = state) }

                when (state) {
                    is WasmExecutionState.Completed -> {
                        _uiState.update {
                            it.copy(
                                executionHistory = listOf(state.result) + it.executionHistory.take(9)
                            )
                        }
                    }
                    is WasmExecutionState.Error -> {
                        _uiState.update { it.copy(error = state.message) }
                    }
                    else -> { /* State already updated */ }
                }
            }
        }
    }

    /**
     * Loads recent workflow runs to show execution history.
     */
    fun loadRecentRuns() {
        val repo = _uiState.value.selectedRepo ?: return

        viewModelScope.launch {
            gitHubRepository.listWorkflowRuns(repo.owner.login, repo.name).fold(
                onSuccess = { runs ->
                    _uiState.update { it.copy(recentRuns = runs.take(10)) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load recent runs")
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetExecution() {
        _uiState.update { it.copy(executionState = WasmExecutionState.Idle) }
    }
}

/**
 * UI state for WASM Run screen.
 */
data class WasmRunUiState(
    val isAuthenticated: Boolean = false,
    val loadingRepositories: Boolean = false,
    val repositories: List<Repository> = emptyList(),
    val selectedRepo: Repository? = null,
    val branches: List<Branch> = emptyList(),
    val selectedBranch: Branch? = null,
    val executionState: WasmExecutionState = WasmExecutionState.Idle,
    val executionHistory: List<WasmExecutionResult> = emptyList(),
    val recentRuns: List<WorkflowRun> = emptyList(),
    val error: String? = null
)
