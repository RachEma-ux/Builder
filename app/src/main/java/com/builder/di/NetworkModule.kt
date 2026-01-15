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
     * Known GitHub IP addresses as fallback when DNS fails.
     * These are GitHub's official IP ranges.
     * Updated periodically - see https://api.github.com/meta
     */
    private val gitHubIps = mapOf(
        "github.com" to listOf(
            "20.27.177.113",
            "20.200.245.247",
            "20.201.28.151",
            "20.205.243.166"
        ),
        "api.github.com" to listOf(
            "20.27.177.113",
            "20.200.245.247",
            "20.201.28.151",
            "20.205.243.166"
        )
    )

    /**
     * Custom DNS resolver with hardcoded fallback for GitHub domains.
     * Bypasses system DNS issues on problematic networks/devices.
     */
    private val fallbackDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            Timber.d("DNS lookup for: $hostname")

            // First try system DNS
            return try {
                val addresses = Dns.SYSTEM.lookup(hostname)
                Timber.d("System DNS resolved $hostname to ${addresses.map { it.hostAddress }}")
                addresses
            } catch (e: UnknownHostException) {
                Timber.w("System DNS failed for $hostname: ${e.message}")

                // Use hardcoded IPs for GitHub domains
                val fallbackIps = gitHubIps[hostname]
                if (fallbackIps != null) {
                    Timber.i("Using hardcoded IPs for $hostname")
                    val addresses = fallbackIps.mapNotNull { ip ->
                        try {
                            InetAddress.getByName(ip)
                        } catch (e2: Exception) {
                            Timber.w("Failed to parse IP $ip: ${e2.message}")
                            null
                        }
                    }
                    if (addresses.isNotEmpty()) {
                        Timber.i("Fallback resolved $hostname to ${addresses.map { it.hostAddress }}")
                        return addresses
                    }
                }

                Timber.e("All DNS resolution failed for $hostname")
                throw e
            }
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
