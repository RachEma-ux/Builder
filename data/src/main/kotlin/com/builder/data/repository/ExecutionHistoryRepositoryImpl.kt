package com.builder.data.repository

import com.builder.core.model.ExecutionStatus
import com.builder.core.model.WasmExecutionResult
import com.builder.core.repository.ExecutionHistoryItem
import com.builder.core.repository.ExecutionHistoryRepository
import com.builder.data.local.db.dao.ExecutionHistoryDao
import com.builder.data.local.db.entities.ExecutionHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ExecutionHistoryRepository using Room database.
 */
@Singleton
class ExecutionHistoryRepositoryImpl @Inject constructor(
    private val executionHistoryDao: ExecutionHistoryDao
) : ExecutionHistoryRepository {

    override suspend fun saveExecution(
        packId: String,
        result: WasmExecutionResult,
        sourceRef: String
    ): Long {
        val entity = ExecutionHistoryEntity(
            packId = packId,
            packName = result.packName,
            runId = result.runId,
            status = result.status.name,
            output = result.output,
            executedAt = System.currentTimeMillis(),
            duration = result.duration,
            artifactUrl = result.artifactUrl,
            sourceRef = sourceRef,
            workflowName = "ci.yml"
        )
        return executionHistoryDao.insert(entity)
    }

    override fun getAllHistory(limit: Int): Flow<List<ExecutionHistoryItem>> {
        return executionHistoryDao.getAll(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getHistoryByPackId(packId: String, limit: Int): Flow<List<ExecutionHistoryItem>> {
        return executionHistoryDao.getByPackId(packId, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLatestExecution(packId: String): ExecutionHistoryItem? {
        return executionHistoryDao.getLatestByPackId(packId)?.toDomain()
    }

    override suspend fun getExecutionCount(packId: String): Int {
        return executionHistoryDao.getCountByPackId(packId)
    }

    override suspend fun deleteOlderThan(beforeTimestamp: Long) {
        executionHistoryDao.deleteOlderThan(beforeTimestamp)
    }

    override suspend fun deleteAll() {
        executionHistoryDao.deleteAll()
    }

    private fun ExecutionHistoryEntity.toDomain(): ExecutionHistoryItem {
        return ExecutionHistoryItem(
            id = id,
            packId = packId,
            packName = packName,
            runId = runId,
            status = try {
                ExecutionStatus.valueOf(status)
            } catch (e: Exception) {
                ExecutionStatus.UNKNOWN
            },
            output = output,
            executedAt = executedAt,
            duration = duration,
            artifactUrl = artifactUrl,
            sourceRef = sourceRef,
            workflowName = workflowName
        )
    }
}
