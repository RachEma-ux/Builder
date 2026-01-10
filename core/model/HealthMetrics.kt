package com.builder.core.model

/**
 * Health metrics for a running instance
 *
 * Captures runtime resource usage including:
 * - CPU usage
 * - Memory consumption
 * - Network I/O
 * - Uptime
 *
 * See Builder_Final.md §13 (UI Model - Health section)
 */
data class HealthMetrics(
    val instanceId: String,
    val packId: String,
    val timestamp: Long,
    val cpuUsagePercent: Float,
    val memoryUsedMb: Long,
    val memoryLimitMb: Long,
    val networkBytesIn: Long,
    val networkBytesOut: Long,
    val uptimeMs: Long,
    val state: String
) {
    val memoryUsagePercent: Float
        get() = if (memoryLimitMb > 0) {
            (memoryUsedMb.toFloat() / memoryLimitMb) * 100
        } else {
            0f
        }

    companion object {
        fun create(
            instanceId: String,
            packId: String,
            cpuUsagePercent: Float = 0f,
            memoryUsedMb: Long = 0,
            memoryLimitMb: Long = 128,
            networkBytesIn: Long = 0,
            networkBytesOut: Long = 0,
            uptimeMs: Long = 0,
            state: String = "STOPPED"
        ): HealthMetrics {
            return HealthMetrics(
                instanceId = instanceId,
                packId = packId,
                timestamp = System.currentTimeMillis(),
                cpuUsagePercent = cpuUsagePercent,
                memoryUsedMb = memoryUsedMb,
                memoryLimitMb = memoryLimitMb,
                networkBytesIn = networkBytesIn,
                networkBytesOut = networkBytesOut,
                uptimeMs = uptimeMs,
                state = state
            )
        }
    }
}

/**
 * Aggregated health metrics for statistics
 */
data class AggregatedHealthMetrics(
    val instanceId: String,
    val packId: String,
    val avgCpuUsagePercent: Float,
    val maxCpuUsagePercent: Float,
    val avgMemoryUsedMb: Long,
    val maxMemoryUsedMb: Long,
    val totalNetworkBytesIn: Long,
    val totalNetworkBytesOut: Long,
    val totalUptime: Long,
    val sampleCount: Int,
    val firstSample: Long,
    val lastSample: Long
)

/**
 * Health status for an instance
 */
enum class HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL,
    UNKNOWN;

    companion object {
        fun fromMetrics(metrics: HealthMetrics): HealthStatus {
            return when {
                metrics.state != "RUNNING" -> UNKNOWN
                metrics.cpuUsagePercent > 90 || metrics.memoryUsagePercent > 90 -> CRITICAL
                metrics.cpuUsagePercent > 75 || metrics.memoryUsagePercent > 75 -> WARNING
                else -> HEALTHY
            }
        }
    }
}
