package com.builder.ui.screens.instances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.Instance
import com.builder.core.model.Pack
import com.builder.core.repository.PackRepository
import com.builder.domain.instance.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for instances screen.
 */
@HiltViewModel
class InstancesViewModel @Inject constructor(
    private val getAllInstancesUseCase: GetAllInstancesUseCase,
    private val getRunningInstancesUseCase: GetRunningInstancesUseCase,
    private val createInstanceUseCase: CreateInstanceUseCase,
    private val startInstanceUseCase: StartInstanceUseCase,
    private val pauseInstanceUseCase: PauseInstanceUseCase,
    private val stopInstanceUseCase: StopInstanceUseCase,
    private val deleteInstanceUseCase: DeleteInstanceUseCase,
    private val packRepository: PackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstancesUiState())
    val uiState: StateFlow<InstancesUiState> = _uiState.asStateFlow()

    init {
        loadInstances()
    }

    /**
     * Loads all instances.
     */
    private fun loadInstances() {
        viewModelScope.launch {
            getAllInstancesUseCase().collect { instances ->
                _uiState.update {
                    it.copy(instances = instances, loading = false)
                }
            }
        }
    }

    /**
     * Creates a new instance.
     */
    fun createInstance(pack: Pack, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(creating = true) }

            createInstanceUseCase(pack, name).fold(
                onSuccess = { instance ->
                    Timber.i("Instance created: ${instance.id}")
                    _uiState.update {
                        it.copy(
                            creating = false,
                            successMessage = "Instance created: $name"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create instance")
                    _uiState.update {
                        it.copy(
                            creating = false,
                            errorMessage = "Failed to create instance: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Starts an instance.
     */
    fun startInstance(instance: Instance, envVars: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            _uiState.update { it.copy(operatingOnId = instance.id) }

            startInstanceUseCase(instance, envVars).fold(
                onSuccess = {
                    Timber.i("Instance started: ${instance.id}")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            successMessage = "Instance started: ${instance.name}"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to start instance")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            errorMessage = "Failed to start instance: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Pauses an instance.
     */
    fun pauseInstance(instance: Instance) {
        viewModelScope.launch {
            _uiState.update { it.copy(operatingOnId = instance.id) }

            pauseInstanceUseCase(instance).fold(
                onSuccess = {
                    Timber.i("Instance paused: ${instance.id}")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            successMessage = "Instance paused: ${instance.name}"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to pause instance")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            errorMessage = "Failed to pause instance: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Stops an instance.
     */
    fun stopInstance(instance: Instance) {
        viewModelScope.launch {
            _uiState.update { it.copy(operatingOnId = instance.id) }

            stopInstanceUseCase(instance).fold(
                onSuccess = {
                    Timber.i("Instance stopped: ${instance.id}")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            successMessage = "Instance stopped: ${instance.name}"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to stop instance")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            errorMessage = "Failed to stop instance: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Deletes an instance.
     */
    fun deleteInstance(instanceId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(operatingOnId = instanceId) }

            deleteInstanceUseCase(instanceId).fold(
                onSuccess = {
                    Timber.i("Instance deleted: $instanceId")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            successMessage = "Instance deleted"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete instance")
                    _uiState.update {
                        it.copy(
                            operatingOnId = null,
                            errorMessage = "Failed to delete instance: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Clears messages.
     */
    fun clearMessages() {
        _uiState.update {
            it.copy(successMessage = null, errorMessage = null)
        }
    }
}

/**
 * UI state for instances screen.
 */
data class InstancesUiState(
    val instances: List<Instance> = emptyList(),
    val loading: Boolean = true,
    val creating: Boolean = false,
    val operatingOnId: Long? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
