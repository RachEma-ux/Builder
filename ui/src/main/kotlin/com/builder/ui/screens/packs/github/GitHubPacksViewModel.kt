package com.builder.ui.screens.packs.github

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.builder.core.model.InstallMode
import com.builder.core.model.InstallSource
import com.builder.core.repository.GitHubRepository
import com.builder.core.model.github.DeviceFlowState
import com.builder.core.model.github.*
import com.builder.domain.github.ListRepositoriesUseCase
import com.builder.domain.pack.InstallPackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for GitHub Packs screen.
 * Handles Dev and Production pack installation flows.
 */
@HiltViewModel
class GitHubPacksViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val listRepositoriesUseCase: ListRepositoriesUseCase,
    private val installPackUseCase: InstallPackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GitHubPacksUiState())
    val uiState: StateFlow<GitHubPacksUiState> = _uiState.asStateFlow()

    init {
        checkAuthentication()
        observeAuthState()
    }

    /**
     * Checks if user is authenticated.
     */
    private fun checkAuthentication() {
        _uiState.update {
            it.copy(isAuthenticated = gitHubRepository.isAuthenticated())
        }
    }

    /**
     * Observes auth state changes from OAuth callback.
     * This ensures immediate UI update when OAuth completes.
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            gitHubRepository.authState.collect { state ->
                when (state) {
                    is DeviceFlowState.Success -> {
                        Timber.i("Auth state changed to Success")
                        _uiState.update {
                            it.copy(
                                authState = AuthState.Success,
                                isAuthenticated = true
                            )
                        }
                        loadRepositories()
                    }
                    is DeviceFlowState.Error -> {
                        Timber.e("Auth state changed to Error: ${state.message}")
                        _uiState.update {
                            it.copy(authState = AuthState.Error(state.message))
                        }
                    }
                    else -> { /* Ignore other states */ }
                }
            }
        }
    }

    /**
     * Debug bypass for OAuth - allows testing app without network.
     * Only for development/testing purposes.
     */
    fun debugBypassAuth() {
        Timber.w("DEBUG: Bypassing OAuth authentication")
        val mockOwner = Owner(
            login = "demo-user",
            id = 1L,
            avatarUrl = "https://github.com/identicons/demo.png",
            type = "User"
        )
        val mockRepos = listOf(
            Repository(
                id = 1L,
                name = "sample-pack",
                fullName = "demo-user/sample-pack",
                owner = mockOwner,
                description = "A sample Builder pack for testing",
                htmlUrl = "https://github.com/demo-user/sample-pack",
                defaultBranch = "main",
                private = false,
                updatedAt = "2024-01-01T00:00:00Z"
            ),
            Repository(
                id = 2L,
                name = "another-pack",
                fullName = "demo-user/another-pack",
                owner = mockOwner,
                description = "Another example pack",
                htmlUrl = "https://github.com/demo-user/another-pack",
                defaultBranch = "main",
                private = false,
                updatedAt = "2024-01-01T00:00:00Z"
            )
        )
        val mockBranches = listOf(
            Branch(name = "main", commit = BranchCommit(sha = "abc123", url = ""), protected = false),
            Branch(name = "develop", commit = BranchCommit(sha = "def456", url = ""), protected = false)
        )
        val mockTags = listOf(
            Tag(name = "v1.0.0", commit = TagCommit(sha = "abc123", url = ""), zipball_url = "", tarball_url = ""),
            Tag(name = "v0.9.0", commit = TagCommit(sha = "xyz789", url = ""), zipball_url = "", tarball_url = "")
        )
        _uiState.update {
            it.copy(
                isAuthenticated = true,
                authState = AuthState.Success,
                repositories = mockRepos,
                selectedRepo = mockRepos.first(),
                branches = mockBranches,
                tags = mockTags
            )
        }
    }

    /**
     * Initiates OAuth authorization code flow with PKCE.
     */
    fun initiateOAuth() {
        viewModelScope.launch {
            gitHubRepository.initiateAuthCodeFlow().collect { state ->
                when (state) {
                    is DeviceFlowState.Loading -> {
                        _uiState.update { it.copy(authState = AuthState.Loading) }
                    }
                    is DeviceFlowState.WaitingForUser -> {
                        // Legacy device flow - still supported for backwards compatibility
                        _uiState.update {
                            it.copy(
                                authState = AuthState.WaitingForUser(
                                    userCode = state.userCode,
                                    verificationUri = state.verificationUri
                                )
                            )
                        }
                    }
                    is DeviceFlowState.WaitingForAuthorization -> {
                        // Authorization code flow - browser opened
                        _uiState.update {
                            it.copy(
                                authState = AuthState.WaitingForAuthorization(
                                    authorizationUrl = state.authorizationUrl
                                )
                            )
                        }
                        // Auth state is now observed via observeAuthState() - no polling needed
                    }
                    is DeviceFlowState.Success -> {
                        _uiState.update {
                            it.copy(
                                authState = AuthState.Success,
                                isAuthenticated = true
                            )
                        }
                        loadRepositories()
                    }
                    is DeviceFlowState.Error -> {
                        _uiState.update {
                            it.copy(authState = AuthState.Error(state.message))
                        }
                    }
                }
            }
        }
    }

    /**
     * Logs out the user.
     */
    fun logout() {
        gitHubRepository.logout()
        _uiState.update {
            GitHubPacksUiState() // Reset to initial state
        }
    }

    /**
     * Loads repositories for the authenticated user.
     */
    fun loadRepositories() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingRepositories = true) }

            listRepositoriesUseCase().fold(
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

    /**
     * Selects a repository.
     */
    fun selectRepository(repo: Repository) {
        _uiState.update { it.copy(selectedRepo = repo) }

        // Load branches and tags
        loadBranches(repo.owner.login, repo.name)
        loadTags(repo.owner.login, repo.name)
    }

    /**
     * Loads branches for a repository.
     */
    private fun loadBranches(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.listBranches(owner, repo).fold(
                onSuccess = { branches ->
                    _uiState.update { it.copy(branches = branches) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load branches")
                }
            )
        }
    }

    /**
     * Loads tags for a repository.
     */
    private fun loadTags(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.listTags(owner, repo).fold(
                onSuccess = { tags ->
                    _uiState.update { it.copy(tags = tags) }

                    // Load releases for each tag
                    loadReleases(owner, repo)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load tags")
                }
            )
        }
    }

    /**
     * Loads releases for a repository.
     */
    private fun loadReleases(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.listReleases(owner, repo).fold(
                onSuccess = { releases ->
                    _uiState.update { it.copy(releases = releases) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load releases")
                }
            )
        }
    }

    /**
     * Selects a branch (for Dev mode).
     */
    fun selectBranch(branch: Branch) {
        _uiState.update { it.copy(selectedBranch = branch) }

        // Load workflow runs for this branch
        _uiState.value.selectedRepo?.let { repo ->
            loadWorkflowRuns(repo.owner.login, repo.name, branch.name)
        }
    }

    /**
     * Loads workflow runs for a branch.
     */
    private fun loadWorkflowRuns(owner: String, repo: String, branch: String) {
        viewModelScope.launch {
            gitHubRepository.listWorkflowRuns(owner, repo, branch).fold(
                onSuccess = { runs ->
                    _uiState.update { it.copy(workflowRuns = runs) }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load workflow runs")
                }
            )
        }
    }

    /**
     * Selects a tag (for Production mode).
     * Looks up the release from already-loaded releases list.
     */
    fun selectTag(tag: Tag) {
        _uiState.update { it.copy(selectedTag = tag, selectedRelease = null, checksums = emptyMap()) }

        // Find release matching this tag from already-loaded releases
        val release = _uiState.value.releases.find { it.tagName == tag.name }

        if (release != null) {
            _uiState.update { it.copy(selectedRelease = release) }
            // Auto-load checksums for better UX
            loadChecksums(release)
        } else {
            // Tag exists but no GitHub Release was created for it
            _uiState.update {
                it.copy(error = "No release found for tag '${tag.name}'. Create a GitHub Release from this tag to install packs.")
            }
        }
    }

    /**
     * Installs a pack from workflow artifact (Dev mode).
     */
    fun installFromArtifact(artifact: Artifact, workflowRun: WorkflowRun) {
        viewModelScope.launch {
            _uiState.update { it.copy(installing = true) }

            val installSource = InstallSource.dev(
                branch = workflowRun.headBranch ?: "unknown",
                artifactUrl = artifact.archiveDownloadUrl,
                timestamp = System.currentTimeMillis()
            )

            installPackUseCase(
                downloadUrl = artifact.archiveDownloadUrl,
                installSource = installSource,
                expectedChecksum = null // No checksum for dev installs
            ).fold(
                onSuccess = { pack ->
                    Timber.i("Pack installed successfully: ${pack.id}")
                    _uiState.update {
                        it.copy(
                            installing = false,
                            installSuccess = "Pack ${pack.name} installed successfully"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Pack installation failed")
                    _uiState.update {
                        it.copy(
                            installing = false,
                            error = "Installation failed: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Installs a pack from release asset (Production mode).
     */
    fun installFromRelease(release: Release, asset: ReleaseAsset, checksum: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(installing = true) }

            val installSource = InstallSource.prod(
                tag = release.tagName,
                releaseUrl = asset.browserDownloadUrl,
                timestamp = System.currentTimeMillis()
            )

            installPackUseCase(
                downloadUrl = asset.browserDownloadUrl,
                installSource = installSource,
                expectedChecksum = checksum
            ).fold(
                onSuccess = { pack ->
                    Timber.i("Pack installed successfully: ${pack.id}")
                    _uiState.update {
                        it.copy(
                            installing = false,
                            installSuccess = "Pack ${pack.name} installed successfully"
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Pack installation failed")
                    _uiState.update {
                        it.copy(
                            installing = false,
                            error = "Installation failed: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Switches between Dev and Prod tabs.
     */
    fun selectTab(mode: InstallMode) {
        _uiState.update { it.copy(selectedTab = mode) }
    }

    /**
     * Loads checksums from the checksums.sha256 release asset.
     * Format: "<sha256>  <filename>" (two spaces separator)
     */
    fun loadChecksums(release: Release) {
        val checksumAsset = release.getChecksums()
        if (checksumAsset == null) {
            _uiState.update {
                it.copy(error = "No checksums.sha256 file found in release")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loadingChecksums = true) }

            gitHubRepository.downloadFile(checksumAsset.browserDownloadUrl).fold(
                onSuccess = { content ->
                    val checksumMap = parseChecksumFile(content)
                    _uiState.update {
                        it.copy(
                            checksums = checksumMap,
                            loadingChecksums = false
                        )
                    }
                    Timber.i("Loaded ${checksumMap.size} checksums")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load checksums")
                    _uiState.update {
                        it.copy(
                            loadingChecksums = false,
                            error = "Failed to load checksums: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Parses checksums.sha256 file content.
     * Format: "<sha256>  <filename>" (two spaces separator per sha256sum standard)
     */
    private fun parseChecksumFile(content: String): Map<String, String> {
        return content.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                // Format: "sha256  filename" or "sha256 *filename" (binary mode)
                val parts = line.split(Regex("\\s+"), limit = 2)
                if (parts.size == 2) {
                    val checksum = parts[0]
                    val filename = parts[1].removePrefix("*") // Remove binary mode indicator
                    filename to checksum
                } else {
                    null
                }
            }
            .toMap()
    }

    /**
     * Clears error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, installSuccess = null) }
    }
}

/**
 * UI state for GitHub Packs screen.
 */
data class GitHubPacksUiState(
    val selectedTab: InstallMode = InstallMode.DEV,
    val isAuthenticated: Boolean = false,
    val authState: AuthState = AuthState.Idle,
    val loadingRepositories: Boolean = false,
    val repositories: List<Repository> = emptyList(),
    val selectedRepo: Repository? = null,
    val branches: List<Branch> = emptyList(),
    val selectedBranch: Branch? = null,
    val tags: List<Tag> = emptyList(),
    val selectedTag: Tag? = null,
    val workflowRuns: List<WorkflowRun> = emptyList(),
    val releases: List<Release> = emptyList(),
    val selectedRelease: Release? = null,
    val checksums: Map<String, String> = emptyMap(),
    val loadingChecksums: Boolean = false,
    val installing: Boolean = false,
    val installSuccess: String? = null,
    val error: String? = null
)

/**
 * Authentication state.
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class WaitingForUser(val userCode: String, val verificationUri: String) : AuthState()
    data class WaitingForAuthorization(val authorizationUrl: String) : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
