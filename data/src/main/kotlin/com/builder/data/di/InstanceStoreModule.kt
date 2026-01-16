package com.builder.data.di

import com.builder.core.instance.InstanceStore
import com.builder.data.local.instance.RoomInstanceStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InstanceStoreModule {

  @Binds
  @Singleton
  abstract fun bindInstanceStore(impl: RoomInstanceStore): InstanceStore
}
