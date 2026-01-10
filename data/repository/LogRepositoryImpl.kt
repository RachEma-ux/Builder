package com.builder.data.repository

import com.builder.core.model.Log
import com.builder.core.model.LogFilter
import com.builder.core.repository.LogRepository
import com.builder.data.local.db.dao.LogDao
import com.builder.data.local.db.entities.LogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of LogRepository using Room database
 */
@Singleton
class LogRepositoryImpl @Inject constructor(
    private val logDao: LogDao
) : LogRepository {

    override suspend fun insert(log: Log) {
        try {
            val entity = LogEntity.fromDomain(log)
            logDao.insert(entity)
        } catch (e: Exception) {
            Timber.e(e, "Failed to insert log: ${log.id}")
        }
    }

    override suspend fun insertAll(logs: List<Log>) {
        try {
            val entities = logs.map { LogEntity.fromDomain(it) }
            logDao.insertAll(entities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to insert logs batch: ${logs.size} logs")
        }
    }

    override fun getByInstance(instanceId: String, limit: Int): Flow<List<Log>> {
        return logDao.getByInstance(instanceId, limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getByPack(packId: String, limit: Int): Flow<List<Log>> {
        return logDao.getByPack(packId, limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getWithFilter(filter: LogFilter): Flow<List<Log>> {
        return logDao.getWithFilter(
            instanceId = filter.instanceId,
            packId = filter.packId,
            level = filter.level?.name,
            source = filter.source?.name,
            search = filter.search,
            fromTimestamp = filter.fromTimestamp,
            toTimestamp = filter.toTimestamp,
            limit = filter.limit
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun search(query: String, limit: Int): Flow<List<Log>> {
        return logDao.search(query, limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun deleteByInstance(instanceId: String) {
        try {
            logDao.deleteByInstance(instanceId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete logs for instance: $instanceId")
        }
    }

    override suspend fun deleteByPack(packId: String) {
        try {
            logDao.deleteByPack(packId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete logs for pack: $packId")
        }
    }

    override suspend fun deleteOlderThan(beforeTimestamp: Long) {
        try {
            logDao.deleteOlderThan(beforeTimestamp)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete old logs")
        }
    }

    override suspend fun deleteAll() {
        try {
            logDao.deleteAll()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete all logs")
        }
    }

    override suspend fun getCountByInstance(instanceId: String): Int {
        return try {
            logDao.getCountByInstance(instanceId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get log count for instance: $instanceId")
            0
        }
    }

    override suspend fun getCountByPack(packId: String): Int {
        return try {
            logDao.getCountByPack(packId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get log count for pack: $packId")
            0
        }
    }
}
