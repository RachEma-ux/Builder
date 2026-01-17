package com.builder.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.builder.data.local.db.entities.KvEntity

/**
 * Data Access Object for key-value storage.
 */
@Dao
interface KvDao {

    /**
     * Inserts or updates a key-value pair.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KvEntity)

    /**
     * Gets a value by pack ID and key.
     */
    @Query("SELECT value FROM kv_store WHERE packId = :packId AND key = :key")
    suspend fun getValue(packId: String, key: String): String?

    /**
     * Deletes a key-value pair.
     */
    @Query("DELETE FROM kv_store WHERE packId = :packId AND key = :key")
    suspend fun delete(packId: String, key: String)

    /**
     * Deletes all key-value pairs for a pack.
     */
    @Query("DELETE FROM kv_store WHERE packId = :packId")
    suspend fun deleteAllForPack(packId: String)

    /**
     * Gets all keys for a pack.
     */
    @Query("SELECT key FROM kv_store WHERE packId = :packId")
    suspend fun getKeysForPack(packId: String): List<String>

    /**
     * Gets all key-value pairs for a pack.
     */
    @Query("SELECT * FROM kv_store WHERE packId = :packId")
    suspend fun getAllForPack(packId: String): List<KvEntity>

    /**
     * Gets the count of entries for a pack.
     */
    @Query("SELECT COUNT(*) FROM kv_store WHERE packId = :packId")
    suspend fun getCountForPack(packId: String): Int
}
