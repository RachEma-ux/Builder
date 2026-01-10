package com.builder.ui.screens.packs.github

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.builder.core.model.InstallMode
import com.builder.data.remote.github.models.WorkflowRun
import com.builder.ui.components.*

/**
 * Enhanced repository selector using dropdown component.
 */
@Composable
fun EnhancedRepositorySelector(
    uiState: GitHubPacksUiState,
    viewModel: GitHubPacksViewModel,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (uiState.loadingRepositories) {
                LoadingIndicator(message = "Loading repositories...")
            } else if (uiState.repositories.isEmpty()) {
                EmptyState(
                    message = "No repositories found",
                    actionLabel = "Refresh",
                    onAction = { viewModel.loadRepositories() }
                )
            } else {
                RepositoryDropdown(
                    repositories = uiState.repositories,
                    selectedRepository = uiState.selectedRepo,
                    onSelectRepository = { viewModel.selectRepository(it) }
                )
            }
        }
    }
}

/**
 * Enhanced Dev mode content with branch selector.
 */
@Composable
fun EnhancedDevModeContent(
    uiState: GitHubPacksUiState,
    viewModel: GitHubPacksViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Branch selector
        uiState.selectedRepo?.let {
            if (uiState.branches.isEmpty()) {
                Text("No branches found")
            } else {
                BranchDropdown(
                    branches = uiState.branches,
                    selectedBranch = uiState.selectedBranch,
                    onSelectBranch = { viewModel.selectBranch(it) }
                )
            }
        }

        // Workflow runs
        uiState.selectedBranch?.let {
            Text(
                "Workflow Runs",
                style = MaterialTheme.typography.titleMedium
            )

            if (uiState.workflowRuns.isEmpty()) {
                EmptyState(message = "No workflow runs found for this branch")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.workflowRuns) { run ->
                        WorkflowRunCard(run, viewModel)
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Production mode content with tag selector.
 */
@Composable
fun EnhancedProdModeContent(
    uiState: GitHubPacksUiState,
    viewModel: GitHubPacksViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tag selector
        uiState.selectedRepo?.let {
            if (uiState.tags.isEmpty()) {
                Text("No tags found")
            } else {
                TagDropdown(
                    tags = uiState.tags,
                    selectedTag = uiState.selectedTag,
                    onSelectTag = { viewModel.selectTag(it) }
                )
            }
        }

        // Release details
        uiState.selectedRelease?.let { release ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = release.name ?: release.tagName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    release.body?.let { body ->
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 10
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Check for packs.index.json
                    val hasIndex = release.getPacksIndex() != null
                    val hasChecksums = release.getChecksums() != null

                    if (!hasIndex) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "⚠️ This tag is not production-ready: packs.index.json missing",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        Text(
                            "Assets (${release.assets.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        // TODO: Show asset list with install buttons
                    }
                }
            }
        }
    }
}

/**
 * Workflow run card for dev mode.
 */
@Composable
fun WorkflowRunCard(run: WorkflowRun, viewModel: GitHubPacksViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = run.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "#${run.runNumber} • ${run.headSha.take(7)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Badge(
                    containerColor = when {
                        run.isSuccess() -> MaterialTheme.colorScheme.primaryContainer
                        run.isFailed() -> MaterialTheme.colorScheme.errorContainer
                        run.isRunning() -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = run.conclusion ?: run.status,
                        color = when {
                            run.isSuccess() -> MaterialTheme.colorScheme.onPrimaryContainer
                            run.isFailed() -> MaterialTheme.colorScheme.onErrorContainer
                            run.isRunning() -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (run.isSuccess()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* TODO: Load artifacts and install */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Artifacts")
                }
            }
        }
    }
}
