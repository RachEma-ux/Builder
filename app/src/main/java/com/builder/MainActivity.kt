package com.builder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.builder.data.remote.github.GitHubOAuthManager
import com.builder.ui.navigation.BuilderNavHost
import com.builder.ui.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main activity for Builder application.
 * Sets up the Jetpack Compose UI and handles OAuth deep link callbacks.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var oauthManager: GitHubOAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("MainActivity created")

        // Handle OAuth callback if present in launch intent
        handleOAuthCallback(intent)

        setContent {
            BuilderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BuilderApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("MainActivity onNewIntent: ${intent.data}")

        // Update the intent so it can be processed
        setIntent(intent)

        // Handle OAuth callback when activity is brought back to foreground
        handleOAuthCallback(intent)
    }

    /**
     * Handles OAuth callback from GitHub.
     * Extracts authorization code and state from deep link URI.
     */
    private fun handleOAuthCallback(intent: Intent?) {
        val data: Uri? = intent?.data

        if (data != null && data.scheme == "builder" && data.host == "oauth") {
            Timber.i("Received OAuth callback: $data")

            // Clear the intent data to prevent reprocessing
            intent?.data = null

            val code = data.getQueryParameter("code")
            val state = data.getQueryParameter("state")
            val error = data.getQueryParameter("error")

            when {
                error != null -> {
                    val errorDescription = data.getQueryParameter("error_description")
                    Timber.e("OAuth error: $error - $errorDescription")
                    // Error will be handled by the UI
                }
                code != null && state != null -> {
                    Timber.i("Processing OAuth authorization code: code=${code.take(10)}..., state=$state")
                    Toast.makeText(this, "OAuth callback received!", Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        try {
                            val result = oauthManager.handleOAuthCallback(code, state)
                            Timber.i("OAuth callback result: $result")
                            runOnUiThread {
                                val msg = when (result) {
                                    is com.builder.core.model.github.DeviceFlowState.Success -> "Success! Authenticated."
                                    is com.builder.core.model.github.DeviceFlowState.Error -> "Error: ${result.message}"
                                    else -> "Result: $result"
                                }
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "OAuth callback failed")
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                else -> {
                    Timber.w("Invalid OAuth callback - missing code or state. URI: $data")
                }
            }
        }
    }
}

/**
 * Navigation items for bottom bar.
 */
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object GitHubPacks : BottomNavItem(Screen.GitHubPacks.route, Icons.Default.Cloud, "Packs")
    object WasmRun : BottomNavItem(Screen.WasmRun.route, Icons.Default.PlayCircle, "WASM Run")
    object Instances : BottomNavItem(Screen.Instances.route, Icons.Default.Inventory, "Instances")
}

@Composable
fun BuilderApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem.GitHubPacks,
        BottomNavItem.WasmRun,
        BottomNavItem.Instances
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        BuilderNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BuilderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
