package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * OAuth device code response from GitHub.
 * Used in the device flow authentication.
 */
data class DeviceCodeResponse(
    @SerializedName("device_code")
    val deviceCode: String,

    @SerializedName("user_code")
    val userCode: String,

    @SerializedName("verification_uri")
    val verificationUri: String,

    @SerializedName("expires_in")
    val expiresIn: Int,

    val interval: Int
)

/**
 * OAuth access token response from GitHub.
 * Note: Fields are nullable because GitHub returns 200 for both
 * success and pending states with different JSON shapes.
 */
data class AccessTokenResponse(
    @SerializedName("access_token")
    val accessToken: String? = null,

    @SerializedName("token_type")
    val tokenType: String? = null,

    val scope: String? = null,

    // Error fields (returned when authorization is pending)
    val error: String? = null,

    @SerializedName("error_description")
    val errorDescription: String? = null
)

/**
 * Error response from GitHub OAuth.
 */
data class OAuthErrorResponse(
    val error: String,

    @SerializedName("error_description")
    val errorDescription: String?,

    @SerializedName("error_uri")
    val errorUri: String?
)

/**
 * Request to initiate device flow.
 */
data class DeviceCodeRequest(
    @SerializedName("client_id")
    val clientId: String,

    val scope: String = "repo,workflow"
)

/**
 * Request to poll for access token.
 */
data class AccessTokenRequest(
    @SerializedName("client_id")
    val clientId: String,

    @SerializedName("device_code")
    val deviceCode: String,

    @SerializedName("grant_type")
    val grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
)
