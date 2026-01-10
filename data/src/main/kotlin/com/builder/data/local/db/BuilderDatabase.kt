package com.builder.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.builder.data.local.db.dao.InstanceDao
import com.builder.data.local.db.dao.LogDao
import com.builder.data.local.db.dao.PackDao
import com.builder.data.local.db.entities.InstanceEntity
import com.builder.data.local.db.entities.LogEntity
import com.builder.data.local.db.entities.PackEntity

/**
 * Room database for Builder app.
 */
@Database(
    entities = [
        PackEntity::class,
        InstanceEntity::class,
        LogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BuilderDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao
    abstract fun instanceDao(): InstanceDao
    abstract fun logDao(): LogDao

    companion object {
        const val DATABASE_NAME = "builder.db"
    }
}
