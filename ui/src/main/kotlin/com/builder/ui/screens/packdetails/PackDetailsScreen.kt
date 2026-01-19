package com.builder.ui.screens.packdetails

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
import com.builder.core.model.Pack
import com.builder.core.model.PackType
import com.builder.core.model.WasmExecutionState
import com.builder.core.repository.ExecutionHistoryItem
import com.builder.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackDetailsScreen(
    packId: String,
    onNavigateBack: () -> Unit,
    viewModel: PackDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(packId) {
        viewModel.loadPack(packId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.pack?.name ?: "Pack Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.pack?.type == PackType.WASM) {
                        IconButton(
                            onClick = { viewModel.runPack() },
                            enabled = !uiState.isExecuting
                        ) {
                            if (uiState.isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                            }
                        }
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
                        message = "Loading pack details...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.pack == null -> {
                    Text(
                        text = uiState.error ?: "Pack not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Basic Info Card
                        item {
                            PackInfoCard(pack = uiState.pack!!, dateFormat = dateFormat)
                        }

                        // Execution State
                        if (uiState.executionState != WasmExecutionState.Idle) {
                            item {
                                ExecutionStateCard(
                                    state = uiState.executionState,
                                    onReset = { viewModel.resetExecution() }
                                )
                            }
                        }

                        // Permissions Card
                        item {
                            PermissionsCard(pack = uiState.pack!!)
                        }

                        // Limits Card
                        item {
                            LimitsCard(pack = uiState.pack!!)
                        }

                        // Execution History
                        item {
                            Text(
                                text = "Execution History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (uiState.executionHistory.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "No execution history yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(uiState.executionHistory) { historyItem ->
                                ExecutionHistoryCard(
                                    item = historyItem,
                                    dateFormat = dateFormat
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackInfoCard(pack: Pack, dateFormat: SimpleDateFormat) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (pack.type == PackType.WASM)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = pack.type.name,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(icon = Icons.Default.Tag, label = "Version", value = pack.version)
            InfoRow(icon = Icons.Default.Source, label = "Source", value = pack.installSource.sourceRef)
            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Installed",
                value = dateFormat.format(Date(pack.installSource.installedAt))
            )
            InfoRow(
                icon = if (pack.installSource.mode == "PROD") Icons.Default.Verified else Icons.Default.Code,
                label = "Mode",
                value = pack.installSource.mode
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SHA256",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = pack.checksumSha256,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ExecutionStateCard(state: WasmExecutionState, onReset: () -> Unit) {
    val (message, color, isLoading) = when (state) {
        is WasmExecutionState.Triggering -> Triple("Triggering workflow...", MaterialTheme.colorScheme.primary, true)
        is WasmExecutionState.Running -> Triple(state.progress, MaterialTheme.colorScheme.tertiary, true)
        is WasmExecutionState.Completed -> {
            val resultColor = when (state.result.status) {
                ExecutionStatus.SUCCESS -> Color(0xFF4CAF50)
                ExecutionStatus.FAILURE -> Color(0xFFF44336)
                else -> Color(0xFF9E9E9E)
            }
            Triple("${state.result.status}: ${state.result.output.take(200)}", resultColor, false)
        }
        is WasmExecutionState.Error -> Triple("Error: ${state.message}", Color(0xFFF44336), false)
        else -> Triple("", MaterialTheme.colorScheme.primary, false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = color
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
            if (!isLoading) {
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
fun PermissionsCard(pack: Pack) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val permissions = pack.manifest.permissions
            val network = permissions.network
            val filesystem = permissions.filesystem

            // Network
            Text(
                text = "Network",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (network != null && network.connect.isNotEmpty()) {
                network.connect.forEach { url ->
                    Text(
                        text = "  • Connect: $url",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                text = "  • Listen localhost: ${if (network?.listenLocalhost == true) "Yes" else "No"}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filesystem
            Text(
                text = "Filesystem",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (filesystem != null && filesystem.read.isNotEmpty()) {
                Text(
                    text = "  • Read: ${filesystem.read.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (filesystem != null && filesystem.write.isNotEmpty()) {
                Text(
                    text = "  • Write: ${filesystem.write.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun LimitsCard(pack: Pack) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resource Limits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val limits = pack.manifest.limits

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LimitItem(
                    icon = Icons.Default.Memory,
                    label = "Memory",
                    value = "${limits.memoryMb} MB"
                )
                LimitItem(
                    icon = Icons.Default.Speed,
                    label = "CPU",
                    value = "${limits.cpuMsPerSec} ms/s"
                )
            }
        }
    }
}

@Composable
fun LimitItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExecutionHistoryCard(item: ExecutionHistoryItem, dateFormat: SimpleDateFormat) {
    val statusColor = when (item.status) {
        ExecutionStatus.SUCCESS -> Color(0xFF4CAF50)
        ExecutionStatus.FAILURE -> Color(0xFFF44336)
        ExecutionStatus.CANCELLED -> Color(0xFFFF9800)
        ExecutionStatus.UNKNOWN -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = item.status.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(item.executedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ref: ${item.sourceRef}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item.duration?.let { duration ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${duration / 1000}s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
