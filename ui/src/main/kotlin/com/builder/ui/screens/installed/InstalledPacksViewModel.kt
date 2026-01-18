package com.builder.ui.screens.installed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.Pack
import com.builder.core.model.WasmExecutionResult
import com.builder.core.model.WasmExecutionState
import com.builder.core.repository.ExecutionHistoryItem
import com.builder.core.repository.ExecutionHistoryRepository
import com.builder.core.repository.GitHubRepository
import com.builder.core.repository.PackRepository
import com.builder.core.util.DebugLogger
import com.builder.domain.wasm.RunWasmPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledPacksUiState(
    val packs: List<Pack> = emptyList(),
    val loading: Boolean = true,
    val selectedPack: Pack? = null,
    val executionState: WasmExecutionState = WasmExecutionState.Idle,
    val executingPackId: String? = null,
    val lastExecutedPackId: String? = null,  // Persists until user clears, to show results
    val deletingPackId: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val executionHistory: Map<String, List<ExecutionHistoryItem>> = emptyMap(),  // packId -> history
    val showHistoryForPackId: String? = null  // Which pack's history is expanded
)

@HiltViewModel
class InstalledPacksViewModel @Inject constructor(
    private val packRepository: PackRepository,
    private val gitHubRepository: GitHubRepository,
    private val runWasmPackUseCase: RunWasmPackUseCase,
    private val executionHistoryRepository: ExecutionHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstalledPacksUiState())
    val uiState: StateFlow<InstalledPacksUiState> = _uiState.asStateFlow()

    init {
        loadInstalledPacks()
    }

    private fun loadInstalledPacks() {
        viewModelScope.launch {
            packRepository.getAllPacksFlow()
                .catch { e ->
                    DebugLogger.logSync("ERROR", "InstalledPacks", "Failed to load packs: ${e.message}")
                    _uiState.update { it.copy(loading = false, error = e.message) }
                }
                .collect { packs ->
                    DebugLogger.logSync("INFO", "InstalledPacks", "Loaded ${packs.size} installed packs")
                    _uiState.update { it.copy(packs = packs, loading = false) }
                }
        }
    }

    fun selectPack(pack: Pack?) {
        _uiState.update { it.copy(selectedPack = pack) }
    }

    fun runPack(pack: Pack) {
        if (_uiState.value.executingPackId != null) {
            _uiState.update { it.copy(error = "Another pack is already running") }
            return
        }

        DebugLogger.logSync("INFO", "InstalledPacks", "Running pack: ${pack.id}")
        _uiState.update {
            it.copy(
                executingPackId = pack.id,
                lastExecutedPackId = pack.id,
                executionState = WasmExecutionState.Idle,
                error = null
            )
        }

        // Extract owner/repo from source URL
        val (owner, repo) = extractOwnerRepo(pack.installSource.sourceUrl)
        if (owner == null || repo == null) {
            _uiState.update {
                it.copy(
                    executingPackId = null,
                    error = "Could not determine repository from source URL"
                )
            }
            return
        }

        viewModelScope.launch {
            runWasmPackUseCase(
                owner = owner,
                repo = repo,
                ref = pack.installSource.sourceRef,
                packId = pack.id,
                packName = pack.name
            )
                .catch { e ->
                    DebugLogger.logSync("ERROR", "InstalledPacks", "Execution failed: ${e.message}")
                    _uiState.update {
                        it.copy(
                            executingPackId = null,
                            executionState = WasmExecutionState.Error(e.message ?: "Unknown error")
                        )
                    }
                }
                .collect { state ->
                    _uiState.update { it.copy(executionState = state) }

                    // Clear executing ID when done
                    if (state is WasmExecutionState.Completed || state is WasmExecutionState.Error) {
                        _uiState.update { it.copy(executingPackId = null) }
                    }
                }
        }
    }

    fun deletePack(pack: Pack) {
        if (_uiState.value.deletingPackId != null) {
            return
        }

        DebugLogger.logSync("INFO", "InstalledPacks", "Deleting pack: ${pack.id}")
        _uiState.update { it.copy(deletingPackId = pack.id) }

        viewModelScope.launch {
            val result = packRepository.deletePack(pack.id)
            result.fold(
                onSuccess = {
                    DebugLogger.logSync("INFO", "InstalledPacks", "Pack deleted: ${pack.id}")
                    _uiState.update {
                        it.copy(
                            deletingPackId = null,
                            successMessage = "Pack '${pack.name}' deleted",
                            selectedPack = if (it.selectedPack?.id == pack.id) null else it.selectedPack
                        )
                    }
                },
                onFailure = { e ->
                    DebugLogger.logSync("ERROR", "InstalledPacks", "Failed to delete pack: ${e.message}")
                    _uiState.update {
                        it.copy(
                            deletingPackId = null,
                            error = "Failed to delete pack: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun resetExecution() {
        _uiState.update {
            it.copy(
                executionState = WasmExecutionState.Idle,
                executingPackId = null,
                lastExecutedPackId = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun toggleHistory(packId: String) {
        val currentExpanded = _uiState.value.showHistoryForPackId
        if (currentExpanded == packId) {
            // Collapse
            _uiState.update { it.copy(showHistoryForPackId = null) }
        } else {
            // Expand and load history
            _uiState.update { it.copy(showHistoryForPackId = packId) }
            loadHistoryForPack(packId)
        }
    }

    private fun loadHistoryForPack(packId: String) {
        viewModelScope.launch {
            executionHistoryRepository.getHistoryByPackId(packId, limit = 10)
                .catch { e ->
                    DebugLogger.logSync("ERROR", "InstalledPacks", "Failed to load history: ${e.message}")
                }
                .collect { history ->
                    _uiState.update { current ->
                        current.copy(
                            executionHistory = current.executionHistory + (packId to history)
                        )
                    }
                }
        }
    }

    private fun extractOwnerRepo(url: String): Pair<String?, String?> {
        // Parse GitHub URL: https://github.com/owner/repo/...
        val regex = Regex("github\\.com/([^/]+)/([^/]+)")
        val match = regex.find(url)
        return if (match != null) {
            Pair(match.groupValues[1], match.groupValues[2])
        } else {
            Pair(null, null)
        }
    }
}
