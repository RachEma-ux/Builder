package com.builder.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.HealthMetrics
import com.builder.core.model.HealthStatus
import com.builder.ui.viewmodel.HealthViewModel
import kotlin.math.min

/**
 * Health Monitoring Screen
 *
 * Displays real-time health metrics for running instances including:
 * - CPU usage graphs
 * - Memory usage graphs
 * - Network I/O statistics
 * - Uptime
 * - Health status indicators
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    instanceId: String? = null,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(instanceId) {
        if (instanceId != null) {
            viewModel.filterByInstance(instanceId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.metrics.isEmpty() -> {
                EmptyHealthView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.metrics.entries.toList(), key = { it.key }) { (instanceId, metrics) ->
                        HealthCard(
                            metrics = metrics,
                            onClick = { /* Navigate to instance details */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HealthCard(
    metrics: HealthMetrics,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val healthStatus = HealthStatus.fromMetrics(metrics)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = metrics.instanceId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = metrics.packId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HealthStatusBadge(status = healthStatus)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CPU Usage
            MetricRow(
                icon = Icons.Default.Speed,
                label = "CPU Usage",
                value = "${metrics.cpuUsagePercent.toInt()}%",
                progress = metrics.cpuUsagePercent / 100f,
                color = getMetricColor(metrics.cpuUsagePercent / 100f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Memory Usage
            MetricRow(
                icon = Icons.Default.Memory,
                label = "Memory",
                value = "${metrics.memoryUsedMb} / ${metrics.memoryLimitMb} MB",
                progress = metrics.memoryUsagePercent / 100f,
                color = getMetricColor(metrics.memoryUsagePercent / 100f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Network I/O
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NetworkStat(
                    icon = Icons.Default.Download,
                    label = "In",
                    bytes = metrics.networkBytesIn
                )
                NetworkStat(
                    icon = Icons.Default.Upload,
                    label = "Out",
                    bytes = metrics.networkBytesOut
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Uptime
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Uptime: ${formatUptime(metrics.uptimeMs)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun HealthStatusBadge(
    status: HealthStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        HealthStatus.HEALTHY -> Color(0xFF4CAF50) // Green
        HealthStatus.WARNING -> Color(0xFFFF9800) // Orange
        HealthStatus.CRITICAL -> Color(0xFFF44336) // Red
        HealthStatus.UNKNOWN -> Color(0xFF9E9E9E) // Gray
    }

    val icon = when (status) {
        HealthStatus.HEALTHY -> Icons.Default.CheckCircle
        HealthStatus.WARNING -> Icons.Default.Warning
        HealthStatus.CRITICAL -> Icons.Default.Error
        HealthStatus.UNKNOWN -> Icons.Default.HelpOutline
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = status.name,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar with gradient
        LinearProgressIndicator(
            progress = min(1f, progress),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun NetworkStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bytes: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatBytes(bytes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyHealthView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No health data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start an instance to see health metrics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun getMetricColor(progress: Float): Color {
    return when {
        progress > 0.9f -> Color(0xFFF44336) // Red
        progress > 0.75f -> Color(0xFFFF9800) // Orange
        progress > 0.5f -> Color(0xFFFFC107) // Amber
        else -> Color(0xFF4CAF50) // Green
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

fun formatUptime(uptimeMs: Long): String {
    val seconds = uptimeMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
