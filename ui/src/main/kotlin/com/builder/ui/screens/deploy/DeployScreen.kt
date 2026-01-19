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
import com.builder.core.model.github.Release
import com.builder.core.model.github.Repository
import com.builder.core.model.github.WorkflowRun
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter

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
                    onRepositorySelected = viewModel::selectRepository,
                    onReleaseSelected = viewModel::selectRelease,
                    onRefreshRepositories = viewModel::loadRepositories,
                    onTriggerDeploy = viewModel::triggerDeploy
                )
                DeployTab.STATUS -> StatusTabContent(
                    uiState = uiState,
                    onRefresh = viewModel::refreshStatus,
                    onCancel = viewModel::cancelRun,
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
    onRepositorySelected: (Repository) -> Unit,
    onReleaseSelected: (Release) -> Unit,
    onRefreshRepositories: () -> Unit,
    onTriggerDeploy: () -> Unit
) {
    var durationExpanded by remember { mutableStateOf(false) }
    var repoExpanded by remember { mutableStateOf(false) }
    var releaseExpanded by remember { mutableStateOf(false) }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Repository",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isLoadingRepositories) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRefreshRepositories) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Repository Dropdown
                ExposedDropdownMenuBox(
                    expanded = repoExpanded,
                    onExpandedChange = { repoExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedRepository?.fullName ?: "Select repository",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repository") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repoExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !uiState.isLoadingRepositories && uiState.repositories.isNotEmpty()
                    )
                    ExposedDropdownMenu(
                        expanded = repoExpanded,
                        onDismissRequest = { repoExpanded = false }
                    ) {
                        if (uiState.repositories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No repositories found") },
                                onClick = { repoExpanded = false },
                                enabled = false
                            )
                        } else {
                            uiState.repositories.forEach { repo ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(repo.fullName, fontWeight = FontWeight.Medium)
                                            repo.description?.let {
                                                Text(
                                                    it.take(50) + if (it.length > 50) "..." else "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onRepositorySelected(repo)
                                        repoExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Release Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Release",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isLoadingReleases) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Release Dropdown
                ExposedDropdownMenuBox(
                    expanded = releaseExpanded,
                    onExpandedChange = { releaseExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedRelease?.let { "${it.tagName} - ${it.name ?: "Release"}" }
                            ?: if (uiState.releases.isEmpty()) "No releases available" else "Select release",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Release") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = releaseExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !uiState.isLoadingReleases && uiState.releases.isNotEmpty()
                    )
                    ExposedDropdownMenu(
                        expanded = releaseExpanded,
                        onDismissRequest = { releaseExpanded = false }
                    ) {
                        if (uiState.releases.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No releases found") },
                                onClick = { releaseExpanded = false },
                                enabled = false
                            )
                        } else {
                            uiState.releases.forEach { release ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(release.tagName, fontWeight = FontWeight.Medium)
                                            Text(
                                                release.name ?: "Release",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                release.publishedAt?.replace("T", " ")?.replace("Z", "") ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onReleaseSelected(release)
                                        releaseExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Show selected release info
                uiState.selectedRelease?.let { release ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Assets: ${release.assets.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            enabled = !uiState.isTriggering && uiState.selectedRepository != null
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
        if (uiState.selectedRepository != null) {
            Text(
                "This will trigger builder-deploy.yml on ${uiState.selectedRepository.fullName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Select a repository to deploy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusTabContent(
    uiState: DeployUiState,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
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
        val isRunning = run.isRunning()

        // Elapsed time state
        var elapsedSeconds by remember { mutableStateOf(0L) }

        // Calculate start time once
        val startTime = remember(run.id) {
            try {
                Instant.parse(run.createdAt).toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        // Update elapsed time - runs every second while the run is in progress
        LaunchedEffect(run.id, isRunning) {
            // Initial calculation
            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000

            // Keep updating while running
            while (isRunning) {
                delay(1000)
                elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            }
        }

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

                    // Elapsed Timer Card
                    Surface(
                        color = if (isRunning)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Timer else Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (isRunning)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (isRunning) "Elapsed Time" else "Total Duration",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isRunning)
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    formatElapsedTime(elapsedSeconds),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning)
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Tunnel URL Card (if available)
                    if (uiState.tunnelUrl != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenInBrowser(uiState.tunnelUrl) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Tunnel URL",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        uiState.tunnelUrl,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Open in browser",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else if (isRunning && uiState.isFetchingUrl) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Waiting for tunnel URL...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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

                if (isRunning) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { onOpenInBrowser(run.htmlUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on GitHub")
                    }
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

/**
 * Formats elapsed seconds into a human-readable duration string.
 * Examples: "0:45", "2:30", "1:05:23"
 */
fun formatElapsedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
