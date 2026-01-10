package com.builder.runtime

import com.builder.core.model.Log
import com.builder.core.model.LogLevel
import com.builder.core.model.LogSource
import com.builder.core.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LogCollector captures and persists logs from running instances
 *
 * This class provides a centralized logging interface for all runtime components.
 * Logs are:
 * - Immediately written to Timber for debugging
 * - Asynchronously persisted to the database
 * - Available for real-time streaming in the UI
 */
@Singleton
class LogCollector @Inject constructor(
    private val logRepository: LogRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Emit a log entry
     */
    fun log(
        instanceId: String,
        packId: String,
        level: LogLevel,
        message: String,
        source: LogSource,
        metadata: Map<String, String> = emptyMap()
    ) {
        // Log to Timber immediately
        when (level) {
            LogLevel.DEBUG -> Timber.tag(source.name).d(message)
            LogLevel.INFO -> Timber.tag(source.name).i(message)
            LogLevel.WARN -> Timber.tag(source.name).w(message)
            LogLevel.ERROR -> Timber.tag(source.name).e(message)
        }

        // Persist to database asynchronously
        val log = Log.create(
            instanceId = instanceId,
            packId = packId,
            level = level,
            message = message,
            source = source,
            metadata = metadata
        )

        scope.launch {
            logRepository.insert(log)
        }
    }

    /**
     * Emit multiple log entries (batch)
     */
    fun logBatch(logs: List<Log>) {
        logs.forEach { log ->
            when (log.level) {
                LogLevel.DEBUG -> Timber.tag(log.source.name).d(log.message)
                LogLevel.INFO -> Timber.tag(log.source.name).i(log.message)
                LogLevel.WARN -> Timber.tag(log.source.name).w(log.message)
                LogLevel.ERROR -> Timber.tag(log.source.name).e(log.message)
            }
        }

        scope.launch {
            logRepository.insertAll(logs)
        }
    }

    /**
     * Convenience methods for common log levels
     */
    fun debug(instanceId: String, packId: String, message: String, source: LogSource) {
        log(instanceId, packId, LogLevel.DEBUG, message, source)
    }

    fun info(instanceId: String, packId: String, message: String, source: LogSource) {
        log(instanceId, packId, LogLevel.INFO, message, source)
    }

    fun warn(instanceId: String, packId: String, message: String, source: LogSource) {
        log(instanceId, packId, LogLevel.WARN, message, source)
    }

    fun error(instanceId: String, packId: String, message: String, source: LogSource) {
        log(instanceId, packId, LogLevel.ERROR, message, source)
    }

    /**
     * Clear logs for an instance
     */
    suspend fun clearInstanceLogs(instanceId: String) {
        logRepository.deleteByInstance(instanceId)
    }

    /**
     * Clear logs for a pack
     */
    suspend fun clearPackLogs(packId: String) {
        logRepository.deleteByPack(packId)
    }

    /**
     * Clear old logs (older than timestamp)
     */
    suspend fun clearOldLogs(beforeTimestamp: Long) {
        logRepository.deleteOlderThan(beforeTimestamp)
    }
}
