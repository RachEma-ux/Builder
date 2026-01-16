package com.builder.data.remote.github

import com.builder.core.model.github.*
import retrofit2.Response
import retrofit2.http.*

/**
 * GitHub OAuth API service for device flow authentication.
 * See https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps#device-flow
 */
interface GitHubOAuthService {
    companion object {
        const val BASE_URL = "https://github.com/"

        /**
         * GitHub OAuth Client ID
         *
         * Configured for Builder - Mobile Orchestration
         * Registered at: https://github.com/settings/developers
         *
         * Security Note: Client ID is public and safe to commit.
         * For production deployments, consider using BuildConfig.
         * See docs/GITHUB_OAUTH_SETUP.md Section 2.2 for BuildConfig approach.
         */
        const val CLIENT_ID = "Ov23li1oiyTmHw29pwBs" // ✅ Configured
    }

    /**
     * Initiate device flow and get device code.
     * POST https://github.com/login/device/code
     */
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/device/code")
    suspend fun getDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = "repo,workflow"
    ): Response<DeviceCodeResponse>

    /**
     * Poll for access token (Device Flow).
     * POST https://github.com/login/oauth/access_token
     */
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): Response<AccessTokenResponse>

    /**
     * Exchange authorization code for access token (Authorization Code Flow with PKCE).
     * POST https://github.com/login/oauth/access_token
     */
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun exchangeCodeForToken(
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("redirect_uri") redirectUri: String = "builder://oauth/callback"
    ): Response<AccessTokenResponse>
}
