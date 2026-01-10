package com.builder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.HealthMetrics
import com.builder.runtime.HealthMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Health Screen
 *
 * Manages health metrics display with:
 * - Real-time metrics updates via Flow
 * - Filtering by instance
 * - Historical data (future enhancement)
 */
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthMonitor: HealthMonitor
) : ViewModel() {

    private val _selectedInstanceId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HealthUiState> = combine(
        healthMonitor.metricsFlow,
        _selectedInstanceId
    ) { allMetrics, selectedId ->
        val filteredMetrics = if (selectedId != null) {
            allMetrics.filterKeys { it == selectedId }
        } else {
            allMetrics
        }

        HealthUiState(
            metrics = filteredMetrics,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HealthUiState(isLoading = true)
    )

    fun filterByInstance(instanceId: String?) {
        _selectedInstanceId.value = instanceId
    }

    fun refresh() {
        // Metrics are automatically refreshed by HealthMonitor
        // This is just to provide user feedback
        viewModelScope.launch {
            _selectedInstanceId.value = _selectedInstanceId.value
        }
    }
}

data class HealthUiState(
    val metrics: Map<String, HealthMetrics> = emptyMap(),
    val isLoading: Boolean = true
)
