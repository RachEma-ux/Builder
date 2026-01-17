package com.builder.ui.screens.wasm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.ExecutionStatus
import com.builder.core.model.WasmExecutionResult
import com.builder.core.model.WasmExecutionState
import com.builder.core.model.github.Branch
import com.builder.core.model.github.Repository

/**
 * WASM Run Screen
 *
 * Allows users to trigger WASM pack execution on GitHub Actions
 * and view execution results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasmRunScreen(
    viewModel: WasmRunViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WASM Runner") },
                actions = {
                    IconButton(onClick = { viewModel.loadRepositories() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (!uiState.isAuthenticated) {
            NotAuthenticatedView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Repository Selection
                item {
                    RepositorySelector(
                        repositories = uiState.repositories,
                        selectedRepo = uiState.selectedRepo,
                        loading = uiState.loadingRepositories,
                        onSelectRepo = { viewModel.selectRepository(it) }
                    )
                }

                // Branch Selection
                item {
                    AnimatedVisibility(visible = uiState.selectedRepo != null) {
                        BranchSelector(
                            branches = uiState.branches,
                            selectedBranch = uiState.selectedBranch,
                            onSelectBranch = { viewModel.selectBranch(it) }
                        )
                    }
                }

                // Run Button
                item {
                    AnimatedVisibility(
                        visible = uiState.selectedRepo != null && uiState.selectedBranch != null
                    ) {
                        RunWasmButton(
                            executionState = uiState.executionState,
                            onRun = { viewModel.runWasmPack() },
                            onReset = { viewModel.resetExecution() }
                        )
                    }
                }

                // Execution Status
                item {
                    ExecutionStatusCard(executionState = uiState.executionState)
                }

                // Execution History
                if (uiState.executionHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Executions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(uiState.executionHistory) { result ->
                        ExecutionResultCard(result = result)
                    }
                }
            }
        }

        // Error Snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Auto-dismiss after 5 seconds
                kotlinx.coroutines.delay(5000)
                viewModel.clearError()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositorySelector(
    repositories: List<Repository>,
    selectedRepo: Repository?,
    loading: Boolean,
    onSelectRepo: (Repository) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Repository",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedRepo?.fullName ?: "Select a repository",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    repositories.forEach { repo ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(repo.name)
                                    repo.description?.let {
                                        Text(
                                            text = it,
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSelector(
    branches: List<Branch>,
    selectedBranch: Branch?,
    onSelectBranch: (Branch) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Branch",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedBranch?.name ?: "Select a branch",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    branches.forEach { branch ->
                        DropdownMenuItem(
                            text = { Text(branch.name) },
                            onClick = {
                                onSelectBranch(branch)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RunWasmButton(
    executionState: WasmExecutionState,
    onRun: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = executionState is WasmExecutionState.Triggering ||
            executionState is WasmExecutionState.Running

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Run WASM Pack on GitHub Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This will trigger the CI workflow to execute the hello.wasm test pack",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRun,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run WASM")
                    }
                }

                if (executionState is WasmExecutionState.Completed ||
                    executionState is WasmExecutionState.Error) {
                    OutlinedButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionStatusCard(
    executionState: WasmExecutionState,
    modifier: Modifier = Modifier
) {
    when (executionState) {
        is WasmExecutionState.Idle -> {
            // Don't show anything when idle
        }

        is WasmExecutionState.Triggering -> {
            StatusCard(
                title = "Triggering Workflow",
                message = "Sending request to GitHub Actions...",
                icon = Icons.Default.Send,
                color = MaterialTheme.colorScheme.primary,
                isLoading = true,
                modifier = modifier
            )
        }

        is WasmExecutionState.Running -> {
            StatusCard(
                title = "Executing WASM Pack",
                message = executionState.progress,
                icon = Icons.Default.DirectionsRun,
                color = MaterialTheme.colorScheme.tertiary,
                isLoading = true,
                modifier = modifier
            )
        }

        is WasmExecutionState.Completed -> {
            val result = executionState.result
            val (color, icon) = when (result.status) {
                ExecutionStatus.SUCCESS -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
                ExecutionStatus.FAILURE -> Color(0xFFF44336) to Icons.Default.Error
                ExecutionStatus.CANCELLED -> Color(0xFFFF9800) to Icons.Default.Cancel
                ExecutionStatus.UNKNOWN -> Color(0xFF9E9E9E) to Icons.Default.HelpOutline
            }

            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color
                        )
                        Text(
                            text = "Execution ${result.status.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Output
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = result.output,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Run ID: ${result.runId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Executed: ${result.executedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is WasmExecutionState.Error -> {
            StatusCard(
                title = "Execution Failed",
                message = executionState.message,
                icon = Icons.Default.Error,
                color = Color(0xFFF44336),
                isLoading = false,
                modifier = modifier
            )
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = color
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ExecutionResultCard(
    result: WasmExecutionResult,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (result.status) {
        ExecutionStatus.SUCCESS -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
        ExecutionStatus.FAILURE -> Color(0xFFF44336) to Icons.Default.Error
        ExecutionStatus.CANCELLED -> Color(0xFFFF9800) to Icons.Default.Cancel
        ExecutionStatus.UNKNOWN -> Color(0xFF9E9E9E) to Icons.Default.HelpOutline
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.packName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Run #${result.runId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.executedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = result.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun NotAuthenticatedView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Authentication Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Please authenticate with GitHub first\nusing the GitHub Packs screen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
