package com.builder.ui.screens.installed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.ExecutionStatus
import com.builder.core.model.Pack
import com.builder.core.model.PackType
import com.builder.core.model.WasmExecutionState
import com.builder.core.repository.ExecutionHistoryItem
import com.builder.domain.pack.PackUpdate
import com.builder.ui.components.EmptyState
import com.builder.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen showing all installed packs with options to run and delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledPacksScreen(
    viewModel: InstalledPacksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Installed Packs") },
                actions = {
                    // Update count badge
                    if (uiState.availableUpdates.isNotEmpty()) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "${uiState.availableUpdates.size} updates",
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Refresh button for checking updates
                    IconButton(
                        onClick = { viewModel.checkForUpdates() },
                        enabled = !uiState.checkingUpdates
                    ) {
                        if (uiState.checkingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check for updates"
                            )
                        }
                    }

                    // Pack count badge
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${uiState.packs.size}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.loading -> {
                    LoadingIndicator(
                        message = "Loading installed packs...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.packs.isEmpty() -> {
                    EmptyState(
                        message = "No packs installed yet.\nGo to GitHub Packs to install one.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.packs,
                            key = { _, pack -> pack.id }
                        ) { index, pack ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = index * 50
                                    )
                                ) + slideInVertically(
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = index * 50
                                    ),
                                    initialOffsetY = { it / 2 }
                                )
                            ) {
                                InstalledPackCard(
                                    pack = pack,
                                    isExecuting = uiState.executingPackId == pack.id,
                                    isDeleting = uiState.deletingPackId == pack.id,
                                    executionState = if (uiState.lastExecutedPackId == pack.id)
                                        uiState.executionState else WasmExecutionState.Idle,
                                    executionHistory = uiState.executionHistory[pack.id] ?: emptyList(),
                                    showHistory = uiState.showHistoryForPackId == pack.id,
                                    availableUpdate = uiState.availableUpdates[pack.id],
                                    missingSecrets = uiState.missingSecrets[pack.id] ?: emptyList(),
                                    onRun = { viewModel.runPackWithWarning(pack) },
                                    onDelete = { viewModel.deletePack(pack) },
                                    onResetExecution = { viewModel.resetExecution() },
                                    onToggleHistory = { viewModel.toggleHistory(pack.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Error Snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            // Success Snackbar
            uiState.successMessage?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearSuccess()
                }
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }

    // Missing Secrets Warning Dialog
    uiState.showSecretsWarning?.let { packId ->
        val pack = uiState.packs.find { it.id == packId }
        val missingSecrets = uiState.missingSecrets[packId] ?: emptyList()
        if (pack != null && missingSecrets.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSecretsWarning() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Missing Secrets") },
                text = {
                    Column {
                        Text("This pack requires the following secrets that are not configured:")
                        Spacer(modifier = Modifier.height(8.dp))
                        missingSecrets.forEach { key ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You can configure secrets in Settings > Secrets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmRunWithMissingSecrets(pack) }
                    ) {
                        Text("Run Anyway")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.dismissSecretsWarning() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun InstalledPackCard(
    pack: Pack,
    isExecuting: Boolean,
    isDeleting: Boolean,
    executionState: WasmExecutionState,
    executionHistory: List<ExecutionHistoryItem>,
    showHistory: Boolean,
    availableUpdate: PackUpdate?,
    missingSecrets: List<String>,
    onRun: () -> Unit,
    onDelete: () -> Unit,
    onResetExecution: () -> Unit,
    onToggleHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${pack.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Update available badge
                        if (availableUpdate != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "v${availableUpdate.latestVersion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Type Badge
                PackTypeBadge(type = pack.type)
            }

            // Update available info banner
            if (availableUpdate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Update available: ${availableUpdate.currentVersion} → ${availableUpdate.latestVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (!availableUpdate.releaseNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = availableUpdate.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Missing secrets warning banner
            if (missingSecrets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = "Missing ${missingSecrets.size} secret${if (missingSecrets.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = missingSecrets.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata
            PackMetadata(
                pack = pack,
                dateFormat = dateFormat
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Execution State (if running or completed)
            ExecutionStateDisplay(
                executionState = executionState,
                onReset = onResetExecution
            )

            // Execution History Toggle
            if (pack.type == PackType.WASM) {
                TextButton(
                    onClick = onToggleHistory,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (showHistory) Icons.Default.ExpandLess else Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showHistory) "Hide History" else "History (${executionHistory.size})")
                }

                // History List
                if (showHistory) {
                    ExecutionHistoryList(
                        history = executionHistory,
                        dateFormat = dateFormat
                    )
                }
            }

            // Actions
            if (isDeleting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                PackActions(
                    pack = pack,
                    isExecuting = isExecuting,
                    showDeleteConfirm = showDeleteConfirm,
                    onRun = onRun,
                    onDeleteClick = { showDeleteConfirm = true },
                    onDeleteConfirm = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    onDeleteCancel = { showDeleteConfirm = false }
                )
            }
        }
    }
}

@Composable
fun PackTypeBadge(type: PackType) {
    val (color, icon) = when (type) {
        PackType.WASM -> MaterialTheme.colorScheme.primaryContainer to Icons.Default.Memory
        PackType.WORKFLOW -> MaterialTheme.colorScheme.tertiaryContainer to Icons.Default.AccountTree
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = type.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PackMetadata(
    pack: Pack,
    dateFormat: SimpleDateFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Source
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Source,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = pack.installSource.sourceRef,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Install Mode
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (pack.installSource.mode == "PROD")
                    Icons.Default.Verified else Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Mode: ${pack.installSource.mode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Installed Date
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Installed: ${dateFormat.format(Date(pack.installSource.installedAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Checksum (truncated)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "SHA256: ${pack.checksumSha256.take(16)}...",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExecutionStateDisplay(
    executionState: WasmExecutionState,
    onReset: () -> Unit
) {
    when (executionState) {
        is WasmExecutionState.Idle -> {
            // Don't show anything
        }
        is WasmExecutionState.Triggering -> {
            ExecutionStatusBar(
                message = "Triggering workflow...",
                color = MaterialTheme.colorScheme.primary,
                isLoading = true
            )
        }
        is WasmExecutionState.Running -> {
            ExecutionStatusBar(
                message = executionState.progress,
                color = MaterialTheme.colorScheme.tertiary,
                isLoading = true
            )
        }
        is WasmExecutionState.Completed -> {
            val result = executionState.result
            val color = when (result.status) {
                ExecutionStatus.SUCCESS -> Color(0xFF4CAF50)
                ExecutionStatus.FAILURE -> Color(0xFFF44336)
                else -> Color(0xFF9E9E9E)
            }

            Column {
                ExecutionStatusBar(
                    message = "${result.status.name}: ${result.output.take(100)}",
                    color = color,
                    isLoading = false
                )
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear")
                }
            }
        }
        is WasmExecutionState.Error -> {
            Column {
                ExecutionStatusBar(
                    message = "Error: ${executionState.message}",
                    color = Color(0xFFF44336),
                    isLoading = false
                )
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
fun ExecutionStatusBar(
    message: String,
    color: Color,
    isLoading: Boolean
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = color
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun PackActions(
    pack: Pack,
    isExecuting: Boolean,
    showDeleteConfirm: Boolean,
    onRun: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit
) {
    if (showDeleteConfirm) {
        // Delete confirmation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Delete this pack?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            OutlinedButton(onClick = onDeleteCancel) {
                Text("Cancel")
            }
            Button(
                onClick = onDeleteConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        }
    } else {
        // Normal actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Run button (only for WASM packs)
            if (pack.type == PackType.WASM) {
                Button(
                    onClick = onRun,
                    enabled = !isExecuting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run")
                    }
                }
            }

            // Delete button
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = if (pack.type != PackType.WASM) Modifier.weight(1f) else Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

@Composable
fun ExecutionHistoryList(
    history: List<ExecutionHistoryItem>,
    dateFormat: SimpleDateFormat
) {
    if (history.isEmpty()) {
        Text(
            text = "No execution history yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            history.take(5).forEach { item ->
                ExecutionHistoryItemRow(item = item, dateFormat = dateFormat)
            }
            if (history.size > 5) {
                Text(
                    text = "... and ${history.size - 5} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExecutionHistoryItemRow(
    item: ExecutionHistoryItem,
    dateFormat: SimpleDateFormat
) {
    val statusColor = when (item.status) {
        ExecutionStatus.SUCCESS -> Color(0xFF4CAF50)
        ExecutionStatus.FAILURE -> Color(0xFFF44336)
        ExecutionStatus.CANCELLED -> Color(0xFFFF9800)
        ExecutionStatus.UNKNOWN -> Color(0xFF9E9E9E)
    }

    Surface(
        color = statusColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (item.status) {
                        ExecutionStatus.SUCCESS -> Icons.Default.CheckCircle
                        ExecutionStatus.FAILURE -> Icons.Default.Error
                        ExecutionStatus.CANCELLED -> Icons.Default.Cancel
                        ExecutionStatus.UNKNOWN -> Icons.Default.Help
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = item.status.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                    Text(
                        text = dateFormat.format(Date(item.executedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (item.duration != null) {
                Text(
                    text = "${item.duration / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
