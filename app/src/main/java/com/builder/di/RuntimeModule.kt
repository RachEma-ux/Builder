package com.builder.di

import com.builder.core.repository.InstanceRepository
import com.builder.core.repository.LogRepository
import com.builder.data.instance.InstanceManager
import com.builder.data.local.db.dao.InstanceDao
import com.builder.runtime.LogCollector
import com.builder.runtime.wasm.permissions.PermissionEnforcer
import com.builder.runtime.wasm.WasiConfig
import com.builder.runtime.wasm.WasmRuntime
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Hilt module for runtime dependencies (WASM, workflows, instances).
 */
@Module
@InstallIn(SingletonComponent::class)
object RuntimeModule {

    @Provides
    @Singleton
    fun provideWasiConfig(): WasiConfig {
        return WasiConfig()
    }

    @Provides
    @Singleton
    fun providePermissionEnforcer(): PermissionEnforcer {
        return PermissionEnforcer()
    }

    @Provides
    @Singleton
    fun provideLogCollector(logRepository: LogRepository): LogCollector {
        return LogCollector(logRepository)
    }

    @Provides
    @Singleton
    fun provideWasmRuntime(
        wasiConfig: WasiConfig,
        permissionEnforcer: PermissionEnforcer
    ): WasmRuntime {
        return WasmRuntime(
            wasiConfig = wasiConfig,
            permissionEnforcer = permissionEnforcer
        )
    }

    @Provides
    @Singleton
    fun provideInstanceManager(
        instanceDao: InstanceDao,
        wasmRuntime: WasmRuntime,
        httpClient: OkHttpClient,
        logCollector: LogCollector
    ): InstanceManager {
        return InstanceManager(
            instanceDao = instanceDao,
            wasmRuntime = wasmRuntime,
            httpClient = httpClient,
            logCollector = logCollector
        )
    }
}
