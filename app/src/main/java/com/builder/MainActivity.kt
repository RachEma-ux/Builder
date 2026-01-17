package com.builder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.builder.data.remote.github.GitHubOAuthManager
import com.builder.ui.screens.packs.github.GitHubPacksScreen
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
                                Toast.makeText(this@MainActivity, "OAuth result: $result", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "OAuth callback failed")
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "OAuth failed: ${e.message}", Toast.LENGTH_LONG).show()
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
