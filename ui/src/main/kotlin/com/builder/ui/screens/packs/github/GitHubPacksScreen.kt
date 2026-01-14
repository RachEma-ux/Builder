package com.builder.ui.screens.packs.github

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.InstallMode

/**
 * Main screen for installing packs from GitHub.
 * Implements Dev vs Prod mode separation per Builder_Final.md Appendix A.
 */
@Composable
fun GitHubPacksScreen(
    viewModel: GitHubPacksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Packs") },
                actions = {
                    if (uiState.isAuthenticated) {
                        TextButton(onClick = { viewModel.logout() }) {
                            Text("Logout")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.isAuthenticated) {
                OAuthScreen(
                    authState = uiState.authState,
                    onInitiateOAuth = { viewModel.initiateOAuth() }
                )
            } else {
                // Dev vs Prod tabs
                TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                    Tab(
                        selected = uiState.selectedTab == InstallMode.DEV,
                        onClick = { viewModel.selectTab(InstallMode.DEV) },
                        text = { Text("Dev (Branches)") }
                    )
                    Tab(
                        selected = uiState.selectedTab == InstallMode.PROD,
                        onClick = { viewModel.selectTab(InstallMode.PROD) },
                        text = { Text("Production (Tags)") }
                    )
                }

                // Mode-specific banner
                when (uiState.selectedTab) {
                    InstallMode.DEV -> DevModeBanner()
                    InstallMode.PROD -> ProdModeBanner()
                }

                // Repository selection (shared)
                RepositorySelector(
                    repositories = uiState.repositories,
                    selectedRepo = uiState.selectedRepo,
                    loading = uiState.loadingRepositories,
                    onSelectRepo = { viewModel.selectRepository(it) },
                    onRefresh = { viewModel.loadRepositories() }
                )

                // Mode-specific content
                when (uiState.selectedTab) {
                    InstallMode.DEV -> DevModeContent(uiState, viewModel)
                    InstallMode.PROD -> ProdModeContent(uiState, viewModel)
                }

                // Error/Success messages
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }
            }
        }
    }
}

@Composable
fun OAuthScreen(authState: AuthState, onInitiateOAuth: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GitHub Authentication Required", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        when (authState) {
            is AuthState.Idle -> {
                Button(onClick = onInitiateOAuth) {
                    Text("Connect GitHub")
                }
            }
            is AuthState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthState.WaitingForUser -> {
                Text("Visit: ${authState.verificationUri}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter code: ${authState.userCode}", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
                Text("Waiting for authorization...")
            }
            is AuthState.Success -> {
                Text("✓ Authenticated successfully!")
            }
            is AuthState.Error -> {
                Text("Error: ${authState.message}", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onInitiateOAuth) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun DevModeBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "DEV MODE: Installs from workflow artifacts only (temporary). Not for production.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ProdModeBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = "PRODUCTION: Installs from tag Release assets only (stable + auditable).",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun RepositorySelector(
    repositories: List<com.builder.core.model.github.Repository>,
    selectedRepo: com.builder.core.model.github.Repository?,
    loading: Boolean,
    onSelectRepo: (com.builder.core.model.github.Repository) -> Unit,
    onRefresh: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Repository", style = MaterialTheme.typography.titleMedium)
                if (repositories.isEmpty() && !loading) {
                    TextButton(onClick = onRefresh) {
                        Text("Load Repos")
                    }
                }
            }

            if (loading) {
                CircularProgressIndicator()
            } else if (selectedRepo != null) {
                Text(selectedRepo.fullName, style = MaterialTheme.typography.bodyLarge)
            } else if (repositories.isNotEmpty()) {
                Text("Select a repository from the list")
                // TODO: Add dropdown/list of repositories
            } else {
                Text("No repositories loaded")
            }
        }
    }
}

@Composable
fun DevModeContent(uiState: GitHubPacksUiState, viewModel: GitHubPacksViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Dev Mode Content", style = MaterialTheme.typography.titleMedium)
        Text("Branches: ${uiState.branches.size}")
        Text("Workflow Runs: ${uiState.workflowRuns.size}")
        // TODO: Add branch selector, workflow run list, artifact download
    }
}

@Composable
fun ProdModeContent(uiState: GitHubPacksUiState, viewModel: GitHubPacksViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Production Mode Content", style = MaterialTheme.typography.titleMedium)
        Text("Tags: ${uiState.tags.size}")
        Text("Releases: ${uiState.releases.size}")
        // TODO: Add tag selector, release details, asset download with checksum
    }
}
