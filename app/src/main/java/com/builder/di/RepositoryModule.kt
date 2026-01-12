package com.builder.di

import com.builder.core.repository.GitHubRepository
import com.builder.core.repository.InstanceRepository
import com.builder.core.repository.LogRepository
import com.builder.core.repository.PackRepository
import com.builder.data.repository.GitHubRepositoryImpl
import com.builder.data.repository.LogRepositoryImpl
import com.builder.data.repository.PackRepositoryImpl
import com.builder.runtime.instance.InstanceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    companion object {
        @Provides
        @Singleton
        fun provideInstanceRepository(
            instanceManager: InstanceManager
        ): InstanceRepository = instanceManager
    }

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
    abstract fun bindLogRepository(
        impl: LogRepositoryImpl
    ): LogRepository
}
