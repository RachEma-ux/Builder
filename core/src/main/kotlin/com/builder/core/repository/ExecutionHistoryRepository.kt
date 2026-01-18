package com.builder.core.repository

import com.builder.core.model.ExecutionStatus
import com.builder.core.model.WasmExecutionResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing execution history.
 */
interface ExecutionHistoryRepository {

    /**
     * Save an execution result to history.
     */
    suspend fun saveExecution(
        packId: String,
        result: WasmExecutionResult,
        sourceRef: String
    ): Long

    /**
     * Get all execution history, most recent first.
     */
    fun getAllHistory(limit: Int = 100): Flow<List<ExecutionHistoryItem>>

    /**
     * Get execution history for a specific pack.
     */
    fun getHistoryByPackId(packId: String, limit: Int = 50): Flow<List<ExecutionHistoryItem>>

    /**
     * Get the most recent execution for a pack.
     */
    suspend fun getLatestExecution(packId: String): ExecutionHistoryItem?

    /**
     * Get execution count for a pack.
     */
    suspend fun getExecutionCount(packId: String): Int

    /**
     * Delete execution history older than timestamp.
     */
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    /**
     * Delete all execution history.
     */
    suspend fun deleteAll()
}

/**
 * Domain model for execution history item.
 */
data class ExecutionHistoryItem(
    val id: Long,
    val packId: String,
    val packName: String,
    val runId: Long,
    val status: ExecutionStatus,
    val output: String,
    val executedAt: Long,
    val duration: Long?,
    val artifactUrl: String?,
    val sourceRef: String,
    val workflowName: String
)
