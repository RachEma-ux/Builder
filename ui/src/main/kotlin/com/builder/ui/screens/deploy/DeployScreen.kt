package com.builder.ui.screens.deploy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.github.WorkflowRun

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeployScreen(
    viewModel: DeployViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Show snackbar for messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deploy") },
                actions = {
                    if (uiState.selectedTab == DeployTab.HISTORY) {
                        IconButton(onClick = { viewModel.loadHistory() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        },
        snackbarHost = {
            Column {
                uiState.message?.let { message ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal
            ) {
                Tab(
                    selected = uiState.selectedTab == DeployTab.DEPLOY,
                    onClick = { viewModel.selectTab(DeployTab.DEPLOY) },
                    text = { Text("Deploy") },
                    icon = { Icon(Icons.Default.RocketLaunch, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == DeployTab.STATUS,
                    onClick = { viewModel.selectTab(DeployTab.STATUS) },
                    text = { Text("Status") },
                    icon = { Icon(Icons.Default.Pending, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == DeployTab.HISTORY,
                    onClick = { viewModel.selectTab(DeployTab.HISTORY) },
                    text = { Text("History") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            // Tab Content
            when (uiState.selectedTab) {
                DeployTab.DEPLOY -> DeployTabContent(
                    uiState = uiState,
                    onVersionChange = viewModel::updateVersion,
                    onDurationChange = viewModel::updateDuration,
                    onRunAppChange = viewModel::updateRunApp,
                    onOwnerChange = viewModel::updateOwner,
                    onRepoChange = viewModel::updateRepo,
                    onTriggerDeploy = viewModel::triggerDeploy
                )
                DeployTab.STATUS -> StatusTabContent(
                    uiState = uiState,
                    onRefresh = viewModel::refreshStatus,
                    onOpenInBrowser = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
                DeployTab.HISTORY -> HistoryTabContent(
                    uiState = uiState,
                    onViewRun = viewModel::viewRun,
                    onOpenInBrowser = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
        }
    }
}

@Composable
fun DeployTabContent(
    uiState: DeployUiState,
    onVersionChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onRunAppChange: (Boolean) -> Unit,
    onOwnerChange: (String) -> Unit,
    onRepoChange: (String) -> Unit,
    onTriggerDeploy: () -> Unit
) {
    var durationExpanded by remember { mutableStateOf(false) }
    val durations = listOf("5", "10", "15", "30")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Repository Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Repository",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.owner,
                    onValueChange = onOwnerChange,
                    label = { Text("Owner") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.repo,
                    onValueChange = onRepoChange,
                    label = { Text("Repository") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Configuration Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.version,
                    onValueChange = onVersionChange,
                    label = { Text("Version") },
                    placeholder = { Text("e.g., 2.0.0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Duration dropdown
                ExposedDropdownMenuBox(
                    expanded = durationExpanded,
                    onExpandedChange = { durationExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${uiState.duration} minutes",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Duration") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = durationExpanded,
                        onDismissRequest = { durationExpanded = false }
                    ) {
                        durations.forEach { duration ->
                            DropdownMenuItem(
                                text = { Text("$duration minutes") },
                                onClick = {
                                    onDurationChange(duration)
                                    durationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Run app switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Start with Public URL")
                        Text(
                            "Creates a Cloudflare tunnel for access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.runApp,
                        onCheckedChange = onRunAppChange
                    )
                }
            }
        }

        // Deploy Button
        Button(
            onClick = onTriggerDeploy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !uiState.isTriggering && uiState.owner.isNotBlank() && uiState.repo.isNotBlank()
        ) {
            if (uiState.isTriggering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Triggering...")
            } else {
                Icon(Icons.Default.RocketLaunch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deploy Now")
            }
        }

        // Info text
        Text(
            "This will trigger the builder-deploy.yml workflow on GitHub Actions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusTabContent(
    uiState: DeployUiState,
    onRefresh: () -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    if (uiState.activeRun == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Pending,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No active deployment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Trigger a deployment or select one from history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val run = uiState.activeRun

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Run #${run.runNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                run.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(run)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Details
                    DetailRow("Branch", run.headBranch ?: "N/A")
                    DetailRow("Commit", run.headSha.take(7))
                    DetailRow("Created", run.createdAt.replace("T", " ").replace("Z", ""))

                    if (uiState.isPolling) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Polling for updates...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }

                Button(
                    onClick = { onOpenInBrowser(run.htmlUrl) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View on GitHub")
                }
            }

            // Success message with URL hint
            if (run.isSuccess()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Deployment Successful!",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "View the workflow on GitHub to get the public tunnel URL.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    uiState: DeployUiState,
    onViewRun: (WorkflowRun) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    if (uiState.isLoadingHistory) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (uiState.workflowRuns.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No deployment history",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.workflowRuns) { run ->
                WorkflowRunCard(
                    run = run,
                    onViewRun = onViewRun,
                    onOpenInBrowser = onOpenInBrowser
                )
            }
        }
    }
}

@Composable
fun WorkflowRunCard(
    run: WorkflowRun,
    onViewRun: (WorkflowRun) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onViewRun(run) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "#${run.runNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusBadge(run)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    run.headSha.take(7),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    run.createdAt.replace("T", " ").replace("Z", ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onOpenInBrowser(run.htmlUrl) }) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open in browser")
            }
        }
    }
}

@Composable
fun StatusBadge(run: WorkflowRun) {
    val (color, icon, text) = when {
        run.isSuccess() -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.CheckCircle,
            "Success"
        )
        run.isFailed() -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Default.Error,
            "Failed"
        )
        run.isRunning() -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Default.Pending,
            "Running"
        )
        run.conclusion == "cancelled" -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Cancel,
            "Cancelled"
        )
        else -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info,
            run.status
        )
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
