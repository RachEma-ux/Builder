package com.builder.di

import com.builder.data.remote.github.AuthInterceptor
import com.builder.data.remote.github.GitHubApiService
import com.builder.data.remote.github.GitHubOAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for GitHub API OkHttp client.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubClient

/**
 * Qualifier for GitHub OAuth OkHttp client.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubOAuthClient

/**
 * Hilt module for network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Custom DNS resolver with fallback to public DNS servers.
     * Helps resolve DNS issues on some Android devices.
     */
    private val fallbackDns = object : Dns {
        // Google and Cloudflare public DNS servers as fallback
        private val fallbackServers = listOf(
            "8.8.8.8",      // Google DNS
            "8.8.4.4",      // Google DNS
            "1.1.1.1",      // Cloudflare DNS
            "1.0.0.1"       // Cloudflare DNS
        )

        override fun lookup(hostname: String): List<InetAddress> {
            // First try system DNS
            return try {
                Timber.d("DNS lookup for $hostname using system DNS")
                Dns.SYSTEM.lookup(hostname)
            } catch (e: UnknownHostException) {
                Timber.w("System DNS failed for $hostname, trying fallback DNS")
                // Try fallback DNS servers
                tryFallbackDns(hostname) ?: throw e
            }
        }

        private fun tryFallbackDns(hostname: String): List<InetAddress>? {
            for (dnsServer in fallbackServers) {
                try {
                    Timber.d("Trying fallback DNS $dnsServer for $hostname")
                    val addresses = InetAddress.getAllByName(hostname)
                    if (addresses.isNotEmpty()) {
                        Timber.i("Fallback DNS resolved $hostname to ${addresses.map { it.hostAddress }}")
                        return addresses.toList()
                    }
                } catch (e: Exception) {
                    Timber.d("Fallback DNS $dnsServer failed: ${e.message}")
                }
            }
            return null
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    @GitHubClient
    fun provideGitHubOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(fallbackDns)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @GitHubOAuthClient
    fun provideGitHubOAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(fallbackDns)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubApiService(
        @GitHubClient okHttpClient: OkHttpClient
    ): GitHubApiService {
        return Retrofit.Builder()
            .baseUrl(GitHubApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubOAuthService(
        @GitHubOAuthClient okHttpClient: OkHttpClient
    ): GitHubOAuthService {
        return Retrofit.Builder()
            .baseUrl(GitHubOAuthService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubOAuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(fallbackDns)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
