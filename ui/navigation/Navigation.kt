package com.builder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.builder.ui.screens.instances.InstancesScreen
import com.builder.ui.screens.packs.github.GitHubPacksScreen

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object GitHubPacks : Screen("github_packs")
    object Instances : Screen("instances")
    object Logs : Screen("logs")
    object Health : Screen("health")
}

/**
 * Main navigation graph.
 */
@Composable
fun BuilderNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.GitHubPacks.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.GitHubPacks.route) {
            GitHubPacksScreen()
        }

        composable(Screen.Instances.route) {
            InstancesScreen()
        }

        // TODO: Add Logs and Health screens when implemented
    }
}
