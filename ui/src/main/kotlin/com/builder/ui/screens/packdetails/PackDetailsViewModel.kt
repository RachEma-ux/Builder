package com.builder.ui.screens.packdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.Pack
import com.builder.core.model.WasmExecutionState
import com.builder.core.repository.ExecutionHistoryItem
import com.builder.core.repository.ExecutionHistoryRepository
import com.builder.core.repository.PackRepository
import com.builder.core.util.DebugLogger
import com.builder.domain.wasm.RunWasmPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PackDetailsUiState(
    val pack: Pack? = null,
    val loading: Boolean = true,
    val executionHistory: List<ExecutionHistoryItem> = emptyList(),
    val executionState: WasmExecutionState = WasmExecutionState.Idle,
    val isExecuting: Boolean = false,
    val error: String? = null,
    val updateAvailable: Boolean = false,
    val latestVersion: String? = null
)

@HiltViewModel
class PackDetailsViewModel @Inject constructor(
    private val packRepository: PackRepository,
    private val executionHistoryRepository: ExecutionHistoryRepository,
    private val runWasmPackUseCase: RunWasmPackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackDetailsUiState())
    val uiState: StateFlow<PackDetailsUiState> = _uiState.asStateFlow()

    fun loadPack(packId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            // Load pack details
            val pack = packRepository.getPackById(packId)
            if (pack != null) {
                _uiState.update { it.copy(pack = pack, loading = false) }

                // Load execution history
                loadExecutionHistory(packId)
            } else {
                _uiState.update { it.copy(loading = false, error = "Pack not found") }
            }
        }
    }

    private fun loadExecutionHistory(packId: String) {
        viewModelScope.launch {
            executionHistoryRepository.getHistoryByPackId(packId, limit = 20)
                .catch { e ->
                    DebugLogger.logSync("ERROR", "PackDetails", "Failed to load history: ${e.message}")
                }
                .collect { history ->
                    _uiState.update { it.copy(executionHistory = history) }
                }
        }
    }

    fun runPack() {
        val pack = _uiState.value.pack ?: return
        if (_uiState.value.isExecuting) return

        _uiState.update { it.copy(isExecuting = true, executionState = WasmExecutionState.Idle) }

        // Extract owner/repo from source URL
        val regex = Regex("github\\.com/([^/]+)/([^/]+)")
        val match = regex.find(pack.installSource.sourceUrl)
        if (match == null) {
            _uiState.update {
                it.copy(isExecuting = false, error = "Could not determine repository")
            }
            return
        }

        val owner = match.groupValues[1]
        val repo = match.groupValues[2]

        viewModelScope.launch {
            runWasmPackUseCase(
                owner = owner,
                repo = repo,
                ref = pack.installSource.sourceRef,
                packId = pack.id,
                packName = pack.name
            )
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            executionState = WasmExecutionState.Error(e.message ?: "Unknown error")
                        )
                    }
                }
                .collect { state ->
                    _uiState.update { it.copy(executionState = state) }
                    if (state is WasmExecutionState.Completed || state is WasmExecutionState.Error) {
                        _uiState.update { it.copy(isExecuting = false) }
                    }
                }
        }
    }

    fun resetExecution() {
        _uiState.update { it.copy(executionState = WasmExecutionState.Idle) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
