package com.builder.data.di

import javax.inject.Qualifier

/**
 * Qualifier for GitHub API OkHttp client with authentication.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubClient

/**
 * Qualifier for GitHub OAuth OkHttp client (no auth).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubOAuthClient
