package com.builder.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.github.DeviceFlowState
import com.builder.core.repository.ExecutionHistoryRepository
import com.builder.core.repository.PackRepository
import com.builder.data.remote.github.GitHubOAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isAuthenticated: Boolean = false,
    val authState: DeviceFlowState? = null,
    val installedPacksCount: Int = 0,
    val executionHistoryCount: Int = 0,
    val isClearing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val oauthManager: GitHubOAuthManager,
    private val packRepository: PackRepository,
    private val executionHistoryRepository: ExecutionHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeAuthState()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticated = oauthManager.isAuthenticated()) }

            // Load pack count
            packRepository.getAllPacksFlow()
                .catch { }
                .collect { packs ->
                    _uiState.update { it.copy(installedPacksCount = packs.size) }
                }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            oauthManager.authState.collect { state ->
                _uiState.update {
                    it.copy(
                        authState = state,
                        isAuthenticated = oauthManager.isAuthenticated()
                    )
                }
            }
        }
    }

    fun signIn() {
        viewModelScope.launch {
            oauthManager.initiateAuthCodeFlow()
                .catch { e ->
                    _uiState.update { it.copy(message = "Sign in failed: ${e.message}") }
                }
                .collect { state ->
                    _uiState.update { it.copy(authState = state) }
                }
        }
    }

    fun signOut() {
        oauthManager.clearAccessToken()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                message = "Signed out successfully"
            )
        }
    }

    fun clearExecutionHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            try {
                executionHistoryRepository.deleteAll()
                _uiState.update {
                    it.copy(
                        isClearing = false,
                        executionHistoryCount = 0,
                        message = "Execution history cleared"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isClearing = false,
                        message = "Failed to clear history: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
