package com.builder.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.builder.data.local.db.dao.InstanceDao
import com.builder.data.local.db.dao.PackDao
import com.builder.data.local.db.entities.InstanceEntity
import com.builder.data.local.db.entities.PackEntity

/**
 * Room database for Builder app.
 */
@Database(
    entities = [
        PackEntity::class,
        InstanceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BuilderDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao
    abstract fun instanceDao(): InstanceDao

    companion object {
        const val DATABASE_NAME = "builder.db"
    }
}
