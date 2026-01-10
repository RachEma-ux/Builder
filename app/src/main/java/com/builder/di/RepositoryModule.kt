package com.builder.di

import com.builder.core.repository.GitHubRepository
import com.builder.core.repository.InstanceRepository
import com.builder.core.repository.PackRepository
import com.builder.data.repository.GitHubRepositoryImpl
import com.builder.data.repository.InstanceRepositoryImpl
import com.builder.data.repository.PackRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPackRepository(
        impl: PackRepositoryImpl
    ): PackRepository

    @Binds
    @Singleton
    abstract fun bindGitHubRepository(
        impl: GitHubRepositoryImpl
    ): GitHubRepository

    @Binds
    @Singleton
    abstract fun bindInstanceRepository(
        impl: InstanceRepositoryImpl
    ): InstanceRepository
}
