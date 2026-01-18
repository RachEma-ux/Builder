package com.builder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.builder.ui.screens.installed.InstalledPacksScreen
import com.builder.ui.screens.instances.InstancesScreen
import com.builder.ui.screens.packs.github.GitHubPacksScreen
import com.builder.ui.screens.secrets.SecretsScreen
import com.builder.ui.screens.wasm.WasmRunScreen

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object GitHubPacks : Screen("github_packs")
    object InstalledPacks : Screen("installed_packs")
    object Instances : Screen("instances")
    object WasmRun : Screen("wasm_run")
    object Secrets : Screen("secrets")
    object Logs : Screen("logs")
    object Health : Screen("health")
}

/**
 * Main navigation graph.
 */
@Composable
fun BuilderNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.GitHubPacks.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.GitHubPacks.route) {
            GitHubPacksScreen()
        }

        composable(Screen.InstalledPacks.route) {
            InstalledPacksScreen()
        }

        composable(Screen.Instances.route) {
            InstancesScreen()
        }

        composable(Screen.WasmRun.route) {
            WasmRunScreen()
        }

        composable(Screen.Secrets.route) {
            SecretsScreen()
        }

        // TODO: Add Logs and Health screens when implemented
    }
}
