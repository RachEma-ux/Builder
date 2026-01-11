package com.builder.di

import com.builder.runtime.instance.InstanceManager
import com.builder.runtime.wasm.PermissionEnforcer
import com.builder.runtime.wasm.WasiConfig
import com.builder.runtime.wasm.WasmRuntime
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideWasmRuntime(
        wasiConfig: WasiConfig,
        permissionEnforcer: PermissionEnforcer
    ): WasmRuntime {
        return WasmRuntime(
            wasiConfig = wasiConfig,
            permissionEnforcer = permissionEnforcer
        )
    }
}
