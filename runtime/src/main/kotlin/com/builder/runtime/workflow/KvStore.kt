package com.builder.runtime.workflow

/**
 * Key-value store interface for workflow state persistence.
 */
interface KvStore {
    /**
     * Stores a value for a key within a pack's namespace.
     */
    suspend fun put(packId: String, key: String, value: String)

    /**
     * Retrieves a value for a key within a pack's namespace.
     */
    suspend fun get(packId: String, key: String): String?

    /**
     * Deletes a key within a pack's namespace.
     */
    suspend fun delete(packId: String, key: String)

    /**
     * Lists all keys within a pack's namespace.
     */
    suspend fun keys(packId: String): List<String>
}

/**
 * In-memory implementation of KvStore.
 * Data is lost when the app is closed.
 */
class InMemoryKvStore : KvStore {
    private val store = mutableMapOf<String, MutableMap<String, String>>()

    override suspend fun put(packId: String, key: String, value: String) {
        val packStore = store.getOrPut(packId) { mutableMapOf() }
        packStore[key] = value
    }

    override suspend fun get(packId: String, key: String): String? {
        return store[packId]?.get(key)
    }

    override suspend fun delete(packId: String, key: String) {
        store[packId]?.remove(key)
    }

    override suspend fun keys(packId: String): List<String> {
        return store[packId]?.keys?.toList() ?: emptyList()
    }
}
