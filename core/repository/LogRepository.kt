package com.builder.core.repository

import com.builder.core.model.Log
import com.builder.core.model.LogFilter
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for log management
 *
 * See Builder_Final.md §13 (UI Model - Logs section)
 */
interface LogRepository {

    /**
     * Insert a single log entry
     */
    suspend fun insert(log: Log)

    /**
     * Insert multiple log entries (batch)
     */
    suspend fun insertAll(logs: List<Log>)

    /**
     * Get logs for a specific instance
     */
    fun getByInstance(instanceId: String, limit: Int = 1000): Flow<List<Log>>

    /**
     * Get logs for a specific pack (all instances)
     */
    fun getByPack(packId: String, limit: Int = 1000): Flow<List<Log>>

    /**
     * Get logs with filter criteria
     */
    fun getWithFilter(filter: LogFilter): Flow<List<Log>>

    /**
     * Search logs by message content
     */
    fun search(query: String, limit: Int = 1000): Flow<List<Log>>

    /**
     * Delete logs for a specific instance
     */
    suspend fun deleteByInstance(instanceId: String)

    /**
     * Delete logs for a specific pack
     */
    suspend fun deleteByPack(packId: String)

    /**
     * Delete logs older than timestamp
     */
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    /**
     * Delete all logs
     */
    suspend fun deleteAll()

    /**
     * Get log count for instance
     */
    suspend fun getCountByInstance(instanceId: String): Int

    /**
     * Get log count for pack
     */
    suspend fun getCountByPack(packId: String): Int
}
