package com.builder.data.remote.github

import com.builder.data.remote.github.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * GitHub OAuth API service for device flow authentication.
 * See https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps#device-flow
 */
interface GitHubOAuthService {
    companion object {
        const val BASE_URL = "https://github.com/"
        const val CLIENT_ID = "Ov23liYourClientIdHere" // TODO: Replace with actual Client ID
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
     * Poll for access token.
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
}
