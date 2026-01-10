package com.builder.data.remote.github

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

/**
 * OkHttp interceptor that adds GitHub authentication headers.
 */
class AuthInterceptor @Inject constructor(
    private val oauthManager: GitHubOAuthManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Don't add auth to OAuth endpoints
        if (originalRequest.url.toString().contains("github.com/login")) {
            return chain.proceed(originalRequest)
        }

        val authHeader = oauthManager.getAuthorizationHeader()

        val request = if (authHeader != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .build()
        } else {
            Timber.w("No auth token available for API request")
            originalRequest
        }

        return chain.proceed(request)
    }
}
