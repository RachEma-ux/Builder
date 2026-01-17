package com.builder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.builder.core.model.github.Repository
import com.builder.core.model.github.Branch
import com.builder.core.model.github.Tag

/**
 * Dropdown selector for GitHub repositories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryDropdown(
    repositories: List<Repository>,
    selectedRepository: Repository?,
    onSelectRepository: (Repository) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedRepository?.fullName ?: "Select repository",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown"
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            repositories.forEach { repo ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(repo.name, style = MaterialTheme.typography.bodyLarge)
                            repo.description?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectRepository(repo)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Dropdown selector for branches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchDropdown(
    branches: List<Branch>,
    selectedBranch: Branch?,
    onSelectBranch: (Branch) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedBranch?.name ?: "Select branch",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Branch") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown"
                )
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
                    text = {
                        Column {
                            Text(branch.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = branch.commit.sha.take(7),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelectBranch(branch)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Dropdown selector for tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDropdown(
    tags: List<Tag>,
    selectedTag: Tag?,
    onSelectTag: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedTag?.name ?: "Select tag",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Tag") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown"
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(tag.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = tag.commit.sha.take(7),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelectTag(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Install mode badge component.
 */
@Composable
fun InstallModeBadge(mode: com.builder.core.model.InstallMode) {
    Badge(
        containerColor = when (mode) {
            com.builder.core.model.InstallMode.DEV -> MaterialTheme.colorScheme.tertiaryContainer
            com.builder.core.model.InstallMode.PROD -> MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Text(
            text = mode.name,
            color = when (mode) {
                com.builder.core.model.InstallMode.DEV -> MaterialTheme.colorScheme.onTertiaryContainer
                com.builder.core.model.InstallMode.PROD -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

/**
 * Loading indicator with message and pulsing animation.
 */
@Composable
fun LoadingIndicator(message: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = message,
            modifier = Modifier.alpha(alpha)
        )
    }
}

/**
 * Empty state component.
 */
@Composable
fun EmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
