package com.builder.di

import android.content.Context
import androidx.room.Room
import com.builder.data.local.db.BuilderDatabase
import com.builder.data.local.db.dao.InstanceDao
import com.builder.data.local.db.dao.KvDao
import com.builder.data.local.db.dao.LogDao
import com.builder.data.local.db.dao.PackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBuilderDatabase(
        @ApplicationContext context: Context
    ): BuilderDatabase {
        return Room.databaseBuilder(
            context,
            BuilderDatabase::class.java,
            BuilderDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePackDao(database: BuilderDatabase): PackDao {
        return database.packDao()
    }

    @Provides
    fun provideInstanceDao(database: BuilderDatabase): InstanceDao {
        return database.instanceDao()
    }

    @Provides
    fun provideLogDao(database: BuilderDatabase): LogDao {
        return database.logDao()
    }

    @Provides
    fun provideKvDao(database: BuilderDatabase): KvDao {
        return database.kvDao()
    }
}
