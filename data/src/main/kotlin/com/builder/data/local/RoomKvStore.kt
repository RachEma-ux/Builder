package com.builder.data.local

import com.builder.data.local.db.dao.KvDao
import com.builder.data.local.db.entities.KvEntity
import com.builder.runtime.workflow.KvStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-based implementation of KvStore.
 * Persists key-value pairs to SQLite database.
 */
@Singleton
class RoomKvStore @Inject constructor(
    private val kvDao: KvDao
) : KvStore {

    override suspend fun put(packId: String, key: String, value: String) {
        kvDao.upsert(
            KvEntity(
                packId = packId,
                key = key,
                value = value,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun get(packId: String, key: String): String? {
        return kvDao.getValue(packId, key)
    }

    override suspend fun delete(packId: String, key: String) {
        kvDao.delete(packId, key)
    }

    /**
     * Deletes all key-value pairs for a pack.
     * Useful when uninstalling a pack.
     */
    suspend fun deleteAllForPack(packId: String) {
        kvDao.deleteAllForPack(packId)
    }

    /**
     * Gets all keys for a pack.
     */
    suspend fun getKeysForPack(packId: String): List<String> {
        return kvDao.getKeysForPack(packId)
    }

    /**
     * Gets all key-value pairs for a pack.
     */
    suspend fun getAllForPack(packId: String): Map<String, String> {
        return kvDao.getAllForPack(packId).associate { it.key to it.value }
    }
}
