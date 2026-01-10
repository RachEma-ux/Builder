package com.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    // TODO: Replace with actual navigation and UI implementation
    // This is a placeholder for the scaffolding phase
    Text(
        text = "Builder\nMobile Orchestration System\n\nScaffolding complete.\nReady for implementation.",
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
fun BuilderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun BuilderAppPreview() {
    BuilderTheme {
        BuilderApp()
    }
}
