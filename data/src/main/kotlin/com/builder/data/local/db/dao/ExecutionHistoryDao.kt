package com.builder.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.builder.data.local.db.entities.ExecutionHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for execution history operations.
 */
@Dao
interface ExecutionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(execution: ExecutionHistoryEntity): Long

    @Query("SELECT * FROM execution_history ORDER BY executedAt DESC LIMIT :limit")
    fun getAll(limit: Int = 100): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT * FROM execution_history WHERE packId = :packId ORDER BY executedAt DESC LIMIT :limit")
    fun getByPackId(packId: String, limit: Int = 50): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT * FROM execution_history WHERE id = :id")
    suspend fun getById(id: Long): ExecutionHistoryEntity?

    @Query("SELECT * FROM execution_history WHERE runId = :runId")
    suspend fun getByRunId(runId: Long): ExecutionHistoryEntity?

    @Query("SELECT * FROM execution_history WHERE status = :status ORDER BY executedAt DESC LIMIT :limit")
    fun getByStatus(status: String, limit: Int = 50): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT COUNT(*) FROM execution_history WHERE packId = :packId")
    suspend fun getCountByPackId(packId: String): Int

    @Query("SELECT COUNT(*) FROM execution_history WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("""
        SELECT * FROM execution_history
        WHERE packId = :packId
        ORDER BY executedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestByPackId(packId: String): ExecutionHistoryEntity?

    @Query("DELETE FROM execution_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM execution_history WHERE packId = :packId")
    suspend fun deleteByPackId(packId: String)

    @Query("DELETE FROM execution_history WHERE executedAt < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM execution_history")
    suspend fun deleteAll()
}
