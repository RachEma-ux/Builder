package com.builder.core.model.github

/**
 * Sealed class representing OAuth flow states for GitHub authentication.
 * Supports both Device Flow (legacy) and Authorization Code Flow with PKCE.
 */
sealed class DeviceFlowState {
    /**
     * Loading state - initial request in progress.
     */
    object Loading : DeviceFlowState()

    /**
     * Waiting for user to authorize the device code (Device Flow).
     *
     * @param userCode The code the user needs to enter on GitHub.
     * @param verificationUri The URL where the user should enter the code.
     * @param expiresIn Seconds until the code expires.
     */
    data class WaitingForUser(
        val userCode: String,
        val verificationUri: String,
        val expiresIn: Int
    ) : DeviceFlowState()

    /**
     * Waiting for user to authorize in browser (Authorization Code Flow).
     * Browser has been opened, waiting for user to complete authorization.
     *
     * @param authorizationUrl The URL the user is authorizing at.
     */
    data class WaitingForAuthorization(
        val authorizationUrl: String
    ) : DeviceFlowState()

    /**
     * Success - user has authorized and access token is available.
     *
     * @param accessToken The GitHub access token.
     */
    data class Success(val accessToken: String) : DeviceFlowState()

    /**
     * Error occurred during the flow.
     *
     * @param message Error message.
     */
    data class Error(val message: String) : DeviceFlowState()
}
