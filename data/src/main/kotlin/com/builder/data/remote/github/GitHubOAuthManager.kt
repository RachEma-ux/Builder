package com.builder.data.remote.github

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.builder.core.model.github.AccessTokenResponse
import com.builder.core.model.github.DeviceCodeResponse
import com.builder.core.model.github.DeviceFlowState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages GitHub OAuth device flow authentication.
 * See Builder_Final.md §2 for GitHub integration requirements.
 */
@Singleton
class GitHubOAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oauthService: GitHubOAuthService
) {
    companion object {
        private const val PREFS_NAME = "github_oauth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_SCOPE = "scope"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_STATE = "oauth_state"

        const val REDIRECT_URI = "builder://oauth/callback"
        private const val AUTHORIZATION_URL = "https://github.com/login/oauth/authorize"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Initiates Authorization Code Flow with PKCE.
     * Opens browser to GitHub authorization page and returns the authorization URL.
     *
     * @return Flow emitting DeviceFlowState updates
     */
    fun initiateAuthCodeFlow(): Flow<DeviceFlowState> = flow {
        try {
            emit(DeviceFlowState.Loading)

            // Generate PKCE parameters
            val pkceParams = PKCEUtils.generatePKCEParams()

            // Generate random state for CSRF protection
            val state = generateRandomState()

            // Store code verifier and state for later verification
            encryptedPrefs.edit().apply {
                putString(KEY_CODE_VERIFIER, pkceParams.codeVerifier)
                putString(KEY_STATE, state)
                apply()
            }

            // Build authorization URL
            val authUrl = buildAuthorizationUrl(
                clientId = GitHubOAuthService.CLIENT_ID,
                redirectUri = REDIRECT_URI,
                state = state,
                codeChallenge = pkceParams.codeChallenge,
                scope = "repo,workflow"
            )

            Timber.i("Opening browser for OAuth authorization: $authUrl")

            // Open browser
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authUrl))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            // Emit waiting state
            emit(DeviceFlowState.WaitingForAuthorization(authUrl))

        } catch (e: Exception) {
            Timber.e(e, "Error initiating authorization code flow")
            emit(DeviceFlowState.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Handles the OAuth callback with authorization code.
     * Exchanges the code for an access token.
     *
     * @param code The authorization code from GitHub
     * @param state The state parameter for CSRF verification
     * @return Flow emitting DeviceFlowState updates
     */
    suspend fun handleOAuthCallback(code: String, state: String): DeviceFlowState {
        return try {
            // Verify state matches (CSRF protection)
            val savedState = encryptedPrefs.getString(KEY_STATE, null)
            if (state != savedState) {
                Timber.e("State mismatch: expected $savedState, got $state")
                return DeviceFlowState.Error("Invalid state parameter")
            }

            // Get stored code verifier
            val codeVerifier = encryptedPrefs.getString(KEY_CODE_VERIFIER, null)
            if (codeVerifier == null) {
                Timber.e("Code verifier not found")
                return DeviceFlowState.Error("Code verifier not found")
            }

            // Exchange code for token
            val tokenResponse = oauthService.exchangeCodeForToken(
                clientId = GitHubOAuthService.CLIENT_ID,
                code = code,
                codeVerifier = codeVerifier,
                redirectUri = REDIRECT_URI
            )

            val accessToken = tokenResponse.body()
            val token = accessToken?.accessToken

            // Check for error response from GitHub (HTTP 200 but with error field)
            if (accessToken?.error != null) {
                Timber.e("GitHub OAuth error: ${accessToken.error} - ${accessToken.errorDescription}")
                return DeviceFlowState.Error("GitHub: ${accessToken.errorDescription ?: accessToken.error}")
            }

            if (!tokenResponse.isSuccessful || accessToken == null || token == null) {
                Timber.e("Failed to exchange code for token: HTTP ${tokenResponse.code()}")
                return DeviceFlowState.Error("Failed to get access token (HTTP ${tokenResponse.code()})")
            }

            // Save token
            saveAccessToken(accessToken)

            // Clear temporary PKCE data
            encryptedPrefs.edit().apply {
                remove(KEY_CODE_VERIFIER)
                remove(KEY_STATE)
                apply()
            }

            Timber.i("Successfully obtained access token via authorization code flow")
            DeviceFlowState.Success(token)

        } catch (e: Exception) {
            Timber.e(e, "Error handling OAuth callback")
            DeviceFlowState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Builds the GitHub authorization URL with PKCE parameters.
     */
    private fun buildAuthorizationUrl(
        clientId: String,
        redirectUri: String,
        state: String,
        codeChallenge: String,
        scope: String
    ): String {
        return "$AUTHORIZATION_URL?" +
                "client_id=$clientId&" +
                "redirect_uri=${android.net.Uri.encode(redirectUri)}&" +
                "state=$state&" +
                "scope=${android.net.Uri.encode(scope)}&" +
                "code_challenge=$codeChallenge&" +
                "code_challenge_method=S256"
    }

    /**
     * Generates a random state parameter for CSRF protection.
     */
    private fun generateRandomState(): String {
        val secureRandom = java.security.SecureRandom()
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
    }

    /**
     * Initiates device flow and returns a flow that polls for completion.
     * LEGACY: Consider using initiateAuthCodeFlow() instead for better UX.
     *
     * @return Flow emitting DeviceFlowState updates
     */
    fun initiateDeviceFlow(): Flow<DeviceFlowState> = flow {
        try {
            emit(DeviceFlowState.Loading)

            // Step 1: Get device code
            val deviceCodeResponse = oauthService.getDeviceCode(
                clientId = GitHubOAuthService.CLIENT_ID
            )

            val deviceCode = deviceCodeResponse.body()
            if (!deviceCodeResponse.isSuccessful || deviceCode == null) {
                emit(DeviceFlowState.Error("Failed to get device code"))
                return@flow
            }
            Timber.i("Device code obtained: ${deviceCode.userCode}")

            // Step 2: Show user code to user
            emit(
                DeviceFlowState.WaitingForUser(
                    userCode = deviceCode.userCode,
                    verificationUri = deviceCode.verificationUri,
                    expiresIn = deviceCode.expiresIn
                )
            )

            // Step 3: Poll for access token with retry logic
            val pollingInterval = (deviceCode.interval * 1000).toLong()
            val expirationTime = System.currentTimeMillis() + (deviceCode.expiresIn * 1000)
            var consecutiveFailures = 0
            val maxRetries = 5

            while (System.currentTimeMillis() < expirationTime) {
                delay(pollingInterval)

                try {
                    val tokenResponse = oauthService.getAccessToken(
                        clientId = GitHubOAuthService.CLIENT_ID,
                        deviceCode = deviceCode.deviceCode
                    )

                    // Reset failure count on successful request
                    consecutiveFailures = 0

                    // Capture body once to avoid multiple calls returning different values
                    val responseBody = tokenResponse.body()
                    val token = responseBody?.accessToken

                    when {
                        tokenResponse.isSuccessful && token != null -> {
                            saveAccessToken(responseBody)
                            Timber.i("Access token obtained successfully")
                            emit(DeviceFlowState.Success(token))
                            return@flow
                        }
                        tokenResponse.isSuccessful -> {
                            // HTTP 200 but no access_token = authorization_pending
                            Timber.d("Authorization pending (200 with no token), continuing to poll")
                            continue
                        }
                        tokenResponse.code() == 428 -> {
                            // authorization_pending - continue polling
                            Timber.d("Authorization pending, continuing to poll")
                            continue
                        }
                        tokenResponse.code() == 400 -> {
                            // expired_token or other error
                            emit(DeviceFlowState.Error("Device code expired or invalid"))
                            return@flow
                        }
                        tokenResponse.code() == 403 -> {
                            // access_denied
                            emit(DeviceFlowState.Error("Access denied by user"))
                            return@flow
                        }
                        tokenResponse.code() == 429 -> {
                            // slow_down - increase polling interval
                            delay(pollingInterval)
                            continue
                        }
                        else -> {
                            emit(DeviceFlowState.Error("Unexpected error: ${tokenResponse.code()}"))
                            return@flow
                        }
                    }
                } catch (e: java.net.ConnectException) {
                    consecutiveFailures++
                    Timber.w("Connection failed (attempt $consecutiveFailures/$maxRetries): ${e.message}")
                    if (consecutiveFailures >= maxRetries) {
                        emit(DeviceFlowState.Error("Connection failed after $maxRetries attempts: ${e.message}"))
                        return@flow
                    }
                    // Wait longer before retry on connection failure
                    delay(pollingInterval * 2)
                    continue
                } catch (e: java.net.SocketTimeoutException) {
                    consecutiveFailures++
                    Timber.w("Connection timeout (attempt $consecutiveFailures/$maxRetries): ${e.message}")
                    if (consecutiveFailures >= maxRetries) {
                        emit(DeviceFlowState.Error("Connection timeout after $maxRetries attempts"))
                        return@flow
                    }
                    delay(pollingInterval)
                    continue
                } catch (e: java.io.IOException) {
                    consecutiveFailures++
                    Timber.w("Network error (attempt $consecutiveFailures/$maxRetries): ${e.message}")
                    if (consecutiveFailures >= maxRetries) {
                        emit(DeviceFlowState.Error("Network error: ${e.message}"))
                        return@flow
                    }
                    delay(pollingInterval)
                    continue
                }
            }

            emit(DeviceFlowState.Error("Device code expired"))
        } catch (e: Exception) {
            Timber.e(e, "Error during device flow")
            emit(DeviceFlowState.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Saves the access token securely.
     */
    private fun saveAccessToken(response: AccessTokenResponse) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, response.accessToken)
            putString(KEY_TOKEN_TYPE, response.tokenType)
            putString(KEY_SCOPE, response.scope)
            apply()
        }
    }

    /**
     * Gets the stored access token.
     */
    fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Checks if the user is authenticated.
     */
    fun isAuthenticated(): Boolean {
        return getAccessToken() != null
    }

    /**
     * Clears the stored access token (logout).
     */
    fun clearAccessToken() {
        encryptedPrefs.edit().clear().apply()
        Timber.i("Access token cleared")
    }

    /**
     * Gets the authorization header value.
     */
    fun getAuthorizationHeader(): String? {
        val token = getAccessToken() ?: return null
        return "Bearer $token"
    }
}

