package com.builder.ui.screens.secrets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.SecretInput
import com.builder.core.model.SecretMetadata
import com.builder.core.repository.SecretRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Secrets management screen.
 */
@HiltViewModel
class SecretsViewModel @Inject constructor(
    private val secretRepository: SecretRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecretsUiState())
    val uiState: StateFlow<SecretsUiState> = _uiState.asStateFlow()

    init {
        loadSecrets()
        observeSecrets()
    }

    private fun observeSecrets() {
        viewModelScope.launch {
            secretRepository.secrets.collect { secrets ->
                _uiState.update { it.copy(secrets = secrets) }
            }
        }
    }

    fun loadSecrets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val secrets = secretRepository.listSecrets()
                _uiState.update {
                    it.copy(
                        secrets = secrets,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load secrets")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load secrets: ${e.message}"
                    )
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                dialogMode = DialogMode.ADD,
                editingKey = "",
                editingValue = "",
                editingDescription = ""
            )
        }
    }

    fun showEditDialog(secret: SecretMetadata) {
        _uiState.update {
            it.copy(
                showDialog = true,
                dialogMode = DialogMode.EDIT,
                editingKey = secret.key,
                editingValue = "", // Don't pre-fill value for security
                editingDescription = secret.description
            )
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                showDialog = false,
                editingKey = "",
                editingValue = "",
                editingDescription = ""
            )
        }
    }

    fun updateEditingKey(key: String) {
        _uiState.update { it.copy(editingKey = key.uppercase()) }
    }

    fun updateEditingValue(value: String) {
        _uiState.update { it.copy(editingValue = value) }
    }

    fun updateEditingDescription(description: String) {
        _uiState.update { it.copy(editingDescription = description) }
    }

    fun saveSecret() {
        val state = _uiState.value
        if (state.editingKey.isBlank() || state.editingValue.isBlank()) {
            _uiState.update { it.copy(error = "Key and value are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val input = SecretInput(
                    key = state.editingKey,
                    value = state.editingValue,
                    description = state.editingDescription
                )

                secretRepository.setSecret(input).fold(
                    onSuccess = {
                        Timber.i("Secret saved: ${state.editingKey}")
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                showDialog = false,
                                editingKey = "",
                                editingValue = "",
                                editingDescription = "",
                                successMessage = "Secret saved successfully"
                            )
                        }
                        loadSecrets()
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to save secret")
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = "Failed to save secret: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Invalid input"
                    )
                }
            }
        }
    }

    fun deleteSecret(key: String) {
        viewModelScope.launch {
            secretRepository.deleteSecret(key).fold(
                onSuccess = {
                    Timber.i("Secret deleted: $key")
                    _uiState.update {
                        it.copy(successMessage = "Secret deleted")
                    }
                    loadSecrets()
                },
                onFailure = { e ->
                    Timber.e(e, "Failed to delete secret")
                    _uiState.update {
                        it.copy(error = "Failed to delete secret: ${e.message}")
                    }
                }
            )
        }
    }

    fun confirmDelete(key: String) {
        _uiState.update {
            it.copy(
                showDeleteConfirm = true,
                deletingKey = key
            )
        }
    }

    fun dismissDeleteConfirm() {
        _uiState.update {
            it.copy(
                showDeleteConfirm = false,
                deletingKey = null
            )
        }
    }

    fun executeDelete() {
        val key = _uiState.value.deletingKey ?: return
        dismissDeleteConfirm()
        deleteSecret(key)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

/**
 * UI state for Secrets screen.
 */
data class SecretsUiState(
    val secrets: List<SecretMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showDialog: Boolean = false,
    val dialogMode: DialogMode = DialogMode.ADD,
    val editingKey: String = "",
    val editingValue: String = "",
    val editingDescription: String = "",
    val showDeleteConfirm: Boolean = false,
    val deletingKey: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)

enum class DialogMode {
    ADD, EDIT
}
