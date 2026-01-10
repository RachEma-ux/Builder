package com.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.builder.ui.screens.packs.github.GitHubPacksScreen
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for Builder application.
 * Sets up the Jetpack Compose UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("MainActivity created")

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
}

@Composable
fun BuilderApp() {
    // Main app UI with GitHub Packs screen
    GitHubPacksScreen()
}

@Composable
fun BuilderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
