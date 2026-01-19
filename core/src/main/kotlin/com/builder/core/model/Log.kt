package com.builder.core.model

/**
 * Log entry from a pack instance
 *
 * Captures structured logs from running instances including:
 * - Standard output/error
 * - Runtime events
 * - Errors and warnings
 * - Step execution logs (for workflows)
 */
data class Log(
    val id: String,
    val instanceId: String,
    val packId: String,
    val timestamp: Long,
    val level: LogLevel,
    val message: String,
    val source: LogSource,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun create(
            instanceId: String,
            packId: String,
            level: LogLevel,
            message: String,
            source: LogSource,
            metadata: Map<String, String> = emptyMap()
        ): Log {
            return Log(
                id = generateId(),
                instanceId = instanceId,
                packId = packId,
                timestamp = System.currentTimeMillis(),
                level = level,
                message = message,
                source = source,
                metadata = metadata
            )
        }

        private fun generateId(): String {
            return "log-${System.currentTimeMillis()}-${(0..999).random()}"
        }
    }
}

/**
 * Log severity level
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR;

    companion object {
        fun fromString(value: String): LogLevel {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: INFO
        }
    }
}

/**
 * Source of the log entry
 */
enum class LogSource {
    /** Standard output from the runtime */
    STDOUT,

    /** Standard error from the runtime */
    STDERR,

    /** Runtime system log (e.g., lifecycle events) */
    RUNTIME,

    /** Workflow step execution */
    WORKFLOW_STEP,

    /** WASM function call */
    WASM,

    /** HTTP request/response */
    HTTP,

    /** KV store operation */
    KV_STORE,

    /** Permission check */
    PERMISSION,

    /** Deploy tab activities */
    DEPLOY,

    /** App-level system logs */
    APP;

    companion object {
        fun fromString(value: String): LogSource {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: RUNTIME
        }
    }
}

/**
 * Filter criteria for querying logs
 */
data class LogFilter(
    val instanceId: String? = null,
    val packId: String? = null,
    val level: LogLevel? = null,
    val source: LogSource? = null,
    val search: String? = null,
    val fromTimestamp: Long? = null,
    val toTimestamp: Long? = null,
    val limit: Int = 1000
)
