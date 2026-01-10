package com.builder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.Log
import com.builder.core.model.LogFilter
import com.builder.core.model.LogLevel
import com.builder.core.model.LogSource
import com.builder.core.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Logs Screen
 *
 * Manages log viewing with:
 * - Real-time log updates via Flow
 * - Filtering by level, source, instance, pack
 * - Search functionality
 * - Log statistics
 */
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(LogFilter())

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            _filter
                .flatMapLatest { filter ->
                    logRepository.getWithFilter(filter)
                }
                .catch { e ->
                    Timber.e(e, "Error loading logs")
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { logs ->
                    _uiState.update {
                        it.copy(
                            logs = logs,
                            totalLogs = logs.size,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun filterByInstance(instanceId: String) {
        _filter.update { it.copy(instanceId = instanceId) }
    }

    fun filterByPack(packId: String) {
        _filter.update { it.copy(packId = packId) }
    }

    fun filterByLevel(level: LogLevel?) {
        _filter.update { it.copy(level = level) }
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun filterBySource(source: LogSource?) {
        _filter.update { it.copy(source = source) }
        _uiState.update { it.copy(selectedSource = source) }
    }

    fun search(query: String) {
        _filter.update { it.copy(search = query.takeIf { it.isNotBlank() }) }
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun filterByTimeRange(from: Long?, to: Long?) {
        _filter.update { it.copy(fromTimestamp = from, toTimestamp = to) }
    }

    fun clearFilters() {
        _filter.value = LogFilter(
            instanceId = _filter.value.instanceId, // Keep instance filter
            packId = _filter.value.packId // Keep pack filter
        )
        _uiState.update {
            it.copy(
                selectedLevel = null,
                selectedSource = null,
                searchQuery = ""
            )
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        // Flow will automatically refresh
    }

    fun clearLogs() {
        viewModelScope.launch {
            try {
                val filter = _filter.value
                when {
                    filter.instanceId != null -> {
                        logRepository.deleteByInstance(filter.instanceId)
                    }
                    filter.packId != null -> {
                        logRepository.deleteByPack(filter.packId)
                    }
                    else -> {
                        logRepository.deleteAll()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error clearing logs")
            }
        }
    }

    fun clearOldLogs(olderThanDays: Int) {
        viewModelScope.launch {
            try {
                val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
                logRepository.deleteOlderThan(cutoffTime)
            } catch (e: Exception) {
                Timber.e(e, "Error clearing old logs")
            }
        }
    }
}

data class LogsUiState(
    val logs: List<Log> = emptyList(),
    val totalLogs: Int = 0,
    val selectedLevel: LogLevel? = null,
    val selectedSource: LogSource? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)
