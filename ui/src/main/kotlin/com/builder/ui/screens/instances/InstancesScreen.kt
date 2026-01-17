package com.builder.ui.screens.instances

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.Instance
import com.builder.core.model.InstanceState
import com.builder.ui.components.EmptyState
import com.builder.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Instances screen showing all pack instances.
 */
@Composable
fun InstancesScreen(
    viewModel: InstancesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instances") }
            )
        },
        floatingActionButton = {
            // TODO: Add create instance FAB when pack selection is implemented
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
                        message = "Loading instances...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.instances.isEmpty() -> {
                    EmptyState(
                        message = "No instances yet. Install a pack and create an instance to get started.",
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
                            items = uiState.instances,
                            key = { _, instance -> instance.id }
                        ) { index, instance ->
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
                                InstanceCard(
                                    instance = instance,
                                    onStart = { viewModel.startInstance(it) },
                                    onPause = { viewModel.pauseInstance(it) },
                                    onStop = { viewModel.stopInstance(it) },
                                    onDelete = { viewModel.deleteInstance(it.id) },
                                    isOperating = uiState.operatingOnId == instance.id
                                )
                            }
                        }
                    }
                }
            }

            // Show snackbar for messages
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            uiState.successMessage?.let { success ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Dismiss")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(success)
                }
            }
        }
    }
}

@Composable
fun InstanceCard(
    instance: Instance,
    onStart: (Instance) -> Unit,
    onPause: (Instance) -> Unit,
    onStop: (Instance) -> Unit,
    onDelete: (Instance) -> Unit,
    isOperating: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = instance.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = instance.packId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                InstanceStateBadge(state = instance.state)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata
            InstanceMetadata(instance)

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            if (isOperating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                InstanceActions(
                    instance = instance,
                    onStart = { onStart(instance) },
                    onPause = { onPause(instance) },
                    onStop = { onStop(instance) },
                    onDelete = { onDelete(instance) }
                )
            }
        }
    }
}

@Composable
fun InstanceStateBadge(state: InstanceState) {
    Badge(
        containerColor = when (state) {
            InstanceState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
            InstanceState.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
            InstanceState.STOPPED -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = state.name,
            color = when (state) {
                InstanceState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                InstanceState.PAUSED -> MaterialTheme.colorScheme.onSecondaryContainer
                InstanceState.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun InstanceMetadata(instance: Instance) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                text = "Created: ${dateFormat.format(Date(instance.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        instance.startedAt?.let { startedAt ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Started: ${dateFormat.format(Date(startedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InstanceActions(
    instance: Instance,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (instance.state) {
            InstanceState.STOPPED -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
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
            InstanceState.RUNNING -> {
                Button(
                    onClick = onPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
            InstanceState.PAUSED -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume")
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }
    }
}
