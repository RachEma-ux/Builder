package com.builder.ui.screens.secrets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.builder.core.model.SecretMetadata

/**
 * Secrets Management Screen.
 *
 * Allows users to add, edit, and delete secrets (environment variables)
 * that can be used by packs at runtime.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretsScreen(
    viewModel: SecretsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secrets") },
                actions = {
                    IconButton(onClick = { viewModel.loadSecrets() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, "Add Secret")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.secrets.isEmpty() -> {
                    EmptySecretsView(
                        onAddClick = { viewModel.showAddDialog() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Environment Variables",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Secrets are encrypted and only accessible to packs that declare them in requiredEnv.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        itemsIndexed(
                            items = uiState.secrets,
                            key = { _, secret -> secret.key }
                        ) { index, secret ->
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
                                SecretCard(
                                    secret = secret,
                                    onEdit = { viewModel.showEditDialog(secret) },
                                    onDelete = { viewModel.confirmDelete(secret.key) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (uiState.showDialog) {
            SecretDialog(
                mode = uiState.dialogMode,
                secretKey = uiState.editingKey,
                secretValue = uiState.editingValue,
                description = uiState.editingDescription,
                isSaving = uiState.isSaving,
                onKeyChange = { viewModel.updateEditingKey(it) },
                onValueChange = { viewModel.updateEditingValue(it) },
                onDescriptionChange = { viewModel.updateEditingDescription(it) },
                onSave = { viewModel.saveSecret() },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        // Delete Confirmation Dialog
        if (uiState.showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirm() },
                icon = { Icon(Icons.Default.Warning, null) },
                title = { Text("Delete Secret?") },
                text = {
                    Text("Are you sure you want to delete '${uiState.deletingKey}'? This cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.executeDelete() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error Snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
        }

        // Success Snackbar
        uiState.successMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearSuccessMessage()
            }
        }
    }
}

@Composable
fun SecretCard(
    secret: SecretMetadata,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = secret.key,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (secret.description.isNotBlank()) {
                    Text(
                        text = secret.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "Updated: ${formatTimestamp(secret.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretDialog(
    mode: DialogMode,
    secretKey: String,
    secretValue: String,
    description: String,
    isSaving: Boolean,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var showValue by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (mode == DialogMode.ADD) "Add Secret" else "Edit Secret")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = onKeyChange,
                    label = { Text("Key") },
                    placeholder = { Text("API_KEY") },
                    enabled = mode == DialogMode.ADD,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    ),
                    supportingText = {
                        Text("Uppercase with underscores (e.g., OPENAI_API_KEY)")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = secretValue,
                    onValueChange = onValueChange,
                    label = { Text("Value") },
                    placeholder = { Text("Enter secret value") },
                    singleLine = true,
                    visualTransformation = if (showValue) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showValue = !showValue }) {
                            Icon(
                                imageVector = if (showValue) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (showValue) "Hide" else "Show"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)") },
                    placeholder = { Text("What is this secret for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving && secretKey.isNotBlank() && secretValue.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptySecretsView(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "No secrets configured",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Secrets are environment variables that packs can use at runtime. They are stored encrypted on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Secret")
        }
    }
}

/**
 * Formats an ISO timestamp to a readable format.
 */
private fun formatTimestamp(isoTimestamp: String): String {
    return try {
        if (isoTimestamp.isBlank()) return "Unknown"
        // Simple parsing - just show date part
        isoTimestamp.substringBefore("T")
    } catch (e: Exception) {
        isoTimestamp.take(10)
    }
}
