package com.builder.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.builder.data.local.db.entities.LogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for log operations
 *
 * Provides queries for:
 * - Retrieving logs by instance, pack, level, source
 * - Searching logs by message content
 * - Time-range queries
 * - Pagination with limit/offset
 * - Automatic cleanup via foreign key cascade delete
 */
@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<LogEntity>)

    @Query("SELECT * FROM logs WHERE instanceId = :instanceId ORDER BY timestamp DESC LIMIT :limit")
    fun getByInstance(instanceId: String, limit: Int = 1000): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE packId = :packId ORDER BY timestamp DESC LIMIT :limit")
    fun getByPack(packId: String, limit: Int = 1000): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE level = :level ORDER BY timestamp DESC LIMIT :limit")
    fun getByLevel(level: String, limit: Int = 1000): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE source = :source ORDER BY timestamp DESC LIMIT :limit")
    fun getBySource(source: String, limit: Int = 1000): Flow<List<LogEntity>>

    @Query(
        """
        SELECT * FROM logs
        WHERE instanceId = :instanceId
        AND timestamp >= :fromTimestamp
        AND timestamp <= :toTimestamp
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun getByInstanceAndTimeRange(
        instanceId: String,
        fromTimestamp: Long,
        toTimestamp: Long,
        limit: Int = 1000
    ): Flow<List<LogEntity>>

    @Query(
        """
        SELECT * FROM logs
        WHERE instanceId = :instanceId
        AND message LIKE '%' || :search || '%'
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun searchByInstance(
        instanceId: String,
        search: String,
        limit: Int = 1000
    ): Flow<List<LogEntity>>

    @Query(
        """
        SELECT * FROM logs
        WHERE message LIKE '%' || :search || '%'
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun search(search: String, limit: Int = 1000): Flow<List<LogEntity>>

    @Query(
        """
        SELECT * FROM logs
        WHERE (:instanceId IS NULL OR instanceId = :instanceId)
        AND (:packId IS NULL OR packId = :packId)
        AND (:level IS NULL OR level = :level)
        AND (:source IS NULL OR source = :source)
        AND (:search IS NULL OR message LIKE '%' || :search || '%')
        AND (:fromTimestamp IS NULL OR timestamp >= :fromTimestamp)
        AND (:toTimestamp IS NULL OR timestamp <= :toTimestamp)
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun getWithFilter(
        instanceId: String? = null,
        packId: String? = null,
        level: String? = null,
        source: String? = null,
        search: String? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        limit: Int = 1000
    ): Flow<List<LogEntity>>

    @Query("DELETE FROM logs WHERE instanceId = :instanceId")
    suspend fun deleteByInstance(instanceId: String)

    @Query("DELETE FROM logs WHERE packId = :packId")
    suspend fun deleteByPack(packId: String)

    @Query("DELETE FROM logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM logs WHERE instanceId = :instanceId")
    suspend fun getCountByInstance(instanceId: String): Int

    @Query("SELECT COUNT(*) FROM logs WHERE packId = :packId")
    suspend fun getCountByPack(packId: String): Int

    /**
     * Get the most recent N logs for an instance
     */
    @Query(
        """
        SELECT * FROM logs
        WHERE instanceId = :instanceId
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getRecentByInstance(
        instanceId: String,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<LogEntity>>
}
