package com.builder.ui.screens.packs.github

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.InstallMode
import com.builder.core.model.github.Release
import com.builder.core.model.github.ReleaseAsset
import com.builder.core.model.github.Repository

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
                    onInitiateOAuth = { viewModel.initiateOAuth() },
                    onDebugBypass = { viewModel.debugBypassAuth() }
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
fun OAuthScreen(
    authState: AuthState,
    onInitiateOAuth: () -> Unit,
    onDebugBypass: () -> Unit = {}
) {
    val context = LocalContext.current

    // Note: Browser is opened automatically by GitHubOAuthManager when using auth code flow.
    // No need to open browser from UI - that was causing the loop.

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

                Spacer(modifier = Modifier.height(32.dp))

                // Debug bypass button for testing without network
                OutlinedButton(
                    onClick = onDebugBypass,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Skip Auth (Debug)")
                }
                Text(
                    text = "Use this to test the app without GitHub connection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is AuthState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connecting to GitHub...")
            }
            is AuthState.WaitingForUser -> {
                Text("Browser opened automatically!", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Code: ${authState.userCode}", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If browser didn't open, visit:",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = authState.verificationUri,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
                Text("Waiting for authorization...")

                Spacer(modifier = Modifier.height(16.dp))

                // Manual open button in case auto-open failed
                OutlinedButton(
                    onClick = {
                        val url = "${authState.verificationUri}?user_code=${authState.userCode}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Browser Again")
                }
            }
            is AuthState.WaitingForAuthorization -> {
                // Authorization code flow
                Text("Browser opened!", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please authorize Builder in your browser")
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Waiting for authorization...", style = MaterialTheme.typography.bodySmall)
            }
            is AuthState.Success -> {
                Text("✓ Authenticated successfully!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            is AuthState.Error -> {
                Text("Error: ${authState.message}", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onInitiateOAuth) {
                    Text("Retry")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Debug bypass button for testing without network
                OutlinedButton(
                    onClick = onDebugBypass,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Skip Auth (Debug)")
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
    repositories: List<Repository>,
    selectedRepo: Repository?,
    loading: Boolean,
    onSelectRepo: (Repository) -> Unit,
    onRefresh: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.height(8.dp))

            if (loading) {
                CircularProgressIndicator()
            } else if (repositories.isNotEmpty()) {
                // Dropdown for repository selection
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedRepo?.fullName ?: "Select repository",
                            modifier = Modifier.weight(1f)
                        )
                        Text(" ▼")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        repositories.forEach { repo ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(repo.fullName, style = MaterialTheme.typography.bodyLarge)
                                        repo.description?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectRepo(repo)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text("No repositories loaded")
            }
        }
    }
}

@Composable
fun DevModeContent(uiState: GitHubPacksUiState, viewModel: GitHubPacksViewModel) {
    var branchExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Branch selector
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Branch", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.branches.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { branchExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.selectedBranch?.name ?: "Select branch",
                                modifier = Modifier.weight(1f)
                            )
                            Text(" ▼")
                        }
                        DropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            uiState.branches.forEach { branch ->
                                DropdownMenuItem(
                                    text = {
                                        Text(branch.name, style = MaterialTheme.typography.bodyLarge)
                                    },
                                    onClick = {
                                        viewModel.selectBranch(branch)
                                        branchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("No branches loaded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workflow runs info
        Text("Workflow Runs: ${uiState.workflowRuns.size}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProdModeContent(uiState: GitHubPacksUiState, viewModel: GitHubPacksViewModel) {
    var tagExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Error display
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tag selector
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tag / Release", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.tags.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { tagExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.selectedTag?.name ?: "Select tag",
                                modifier = Modifier.weight(1f)
                            )
                            Text(" ▼")
                        }
                        DropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            uiState.tags.forEach { tag ->
                                DropdownMenuItem(
                                    text = {
                                        Text(tag.name, style = MaterialTheme.typography.bodyLarge)
                                    },
                                    onClick = {
                                        viewModel.selectTag(tag)
                                        tagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("No tags loaded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Release info and assets
        uiState.selectedRelease?.let { release ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Release: ${release.name ?: release.tagName}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter installable assets (only valid pack files: pack-*.zip)
                    val packAssets = release.assets.filter {
                        it.name.startsWith("pack-") && it.name.endsWith(".zip")
                    }

                    if (packAssets.isEmpty()) {
                        Column {
                            Text(
                                "No pack files found in this release",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Packs must be named: pack-<variant>-<target>-<version>.zip",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text(
                            "Available Assets (${packAssets.size}):",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        packAssets.forEach { asset ->
                            AssetInstallItem(
                                asset = asset,
                                release = release,
                                installing = uiState.installing,
                                loadingChecksums = uiState.loadingChecksums,
                                checksums = uiState.checksums,
                                checksumsNotAvailable = uiState.checksumsNotAvailable,
                                onInstall = { checksum ->
                                    viewModel.installFromRelease(release, asset, checksum)
                                },
                                onLoadChecksums = {
                                    viewModel.loadChecksums(release)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Installation progress
        if (uiState.installing) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Installing pack...")
                }
            }
        }

        // Success message
        uiState.installSuccess?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AssetInstallItem(
    asset: ReleaseAsset,
    release: Release,
    installing: Boolean,
    loadingChecksums: Boolean,
    checksums: Map<String, String>,
    checksumsNotAvailable: Boolean,
    onInstall: (String) -> Unit,
    onLoadChecksums: () -> Unit
) {
    val checksum = checksums[asset.name]
    val sizeInMb = asset.size / (1024.0 * 1024.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "%.2f MB".format(sizeInMb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (loadingChecksums) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading...", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (checksum != null) {
                    Button(
                        onClick = { onInstall(checksum) },
                        enabled = !installing
                    ) {
                        Text("Install")
                    }
                } else if (checksumsNotAvailable) {
                    // No checksum file in release - allow install without verification
                    Button(
                        onClick = { onInstall("") },
                        enabled = !installing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Install (No Verify)")
                    }
                } else {
                    // Checksums should be available but not loaded yet
                    OutlinedButton(
                        onClick = onLoadChecksums,
                        enabled = !installing
                    ) {
                        Text("Load Checksums")
                    }
                }
            }

            if (checksum != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SHA256: ${checksum.take(16)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (checksumsNotAvailable) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Warning: No checksum verification available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
