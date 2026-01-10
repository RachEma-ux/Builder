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
     * Initiates device flow and returns a flow that polls for completion.
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

            if (!deviceCodeResponse.isSuccessful || deviceCodeResponse.body() == null) {
                emit(DeviceFlowState.Error("Failed to get device code"))
                return@flow
            }

            val deviceCode = deviceCodeResponse.body()!!
            Timber.i("Device code obtained: ${deviceCode.userCode}")

            // Step 2: Show user code to user
            emit(
                DeviceFlowState.WaitingForUser(
                    userCode = deviceCode.userCode,
                    verificationUri = deviceCode.verificationUri,
                    expiresIn = deviceCode.expiresIn
                )
            )

            // Step 3: Poll for access token
            val pollingInterval = (deviceCode.interval * 1000).toLong()
            val expirationTime = System.currentTimeMillis() + (deviceCode.expiresIn * 1000)

            while (System.currentTimeMillis() < expirationTime) {
                delay(pollingInterval)

                val tokenResponse = oauthService.getAccessToken(
                    clientId = GitHubOAuthService.CLIENT_ID,
                    deviceCode = deviceCode.deviceCode
                )

                when {
                    tokenResponse.isSuccessful && tokenResponse.body() != null -> {
                        val accessToken = tokenResponse.body()!!
                        saveAccessToken(accessToken)
                        Timber.i("Access token obtained successfully")
                        emit(DeviceFlowState.Success(accessToken.accessToken))
                        return@flow
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

