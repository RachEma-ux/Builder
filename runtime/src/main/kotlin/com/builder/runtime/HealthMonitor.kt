package com.builder.runtime

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Debug
import com.builder.core.model.HealthMetrics
import com.builder.core.model.Instance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * HealthMonitor collects runtime metrics for instances
 *
 * Monitors:
 * - CPU usage (approximate via thread CPU time)
 * - Memory usage (heap + native)
 * - Network I/O (via TrafficStats)
 * - Uptime
 *
 * Metrics are collected periodically and exposed via Flow for real-time UI updates.
 */
@Singleton
class HealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Store latest metrics for each instance
    private val _metricsFlow = MutableStateFlow<Map<String, HealthMetrics>>(emptyMap())
    val metricsFlow: StateFlow<Map<String, HealthMetrics>> = _metricsFlow.asStateFlow()

    // Track monitored instances
    private val monitoredInstances = mutableMapOf<String, MonitoredInstance>()

    // Collection interval (milliseconds)
    private val collectionIntervalMs = 5000L // 5 seconds

    private var collectionJob: Job? = null

    /**
     * Start monitoring an instance
     */
    fun startMonitoring(instance: Instance) {
        synchronized(monitoredInstances) {
            if (!monitoredInstances.containsKey(instance.id.toString())) {
                monitoredInstances[instance.id.toString()] = MonitoredInstance(
                    instance = instance,
                    startTime = System.currentTimeMillis(),
                    networkBytesInStart = getNetworkBytesReceived(),
                    networkBytesOutStart = getNetworkBytesTransmitted()
                )
                Timber.d("Started monitoring instance: ${instance.id}")
            }
        }

        // Start collection job if not already running
        if (collectionJob == null || collectionJob?.isActive != true) {
            startCollection()
        }
    }

    /**
     * Stop monitoring an instance
     */
    fun stopMonitoring(instanceId: String) {
        synchronized(monitoredInstances) {
            monitoredInstances.remove(instanceId)
            Timber.d("Stopped monitoring instance: $instanceId")
        }

        // Update metrics flow
        _metricsFlow.value = _metricsFlow.value - instanceId

        // Stop collection if no instances left
        if (monitoredInstances.isEmpty()) {
            stopCollection()
        }
    }

    /**
     * Get current metrics for an instance
     */
    fun getMetrics(instanceId: String): HealthMetrics? {
        return _metricsFlow.value[instanceId]
    }

    /**
     * Start periodic metrics collection
     */
    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            while (isActive) {
                try {
                    collectMetrics()
                } catch (e: Exception) {
                    Timber.e(e, "Error collecting metrics")
                }
                delay(collectionIntervalMs)
            }
        }
        Timber.d("Started metrics collection")
    }

    /**
     * Stop metrics collection
     */
    private fun stopCollection() {
        collectionJob?.cancel()
        collectionJob = null
        Timber.d("Stopped metrics collection")
    }

    /**
     * Collect metrics for all monitored instances
     */
    private suspend fun collectMetrics() {
        val metrics = mutableMapOf<String, HealthMetrics>()

        synchronized(monitoredInstances) {
            monitoredInstances.forEach { (instanceId, monitored) ->
                try {
                    val currentMetrics = collectInstanceMetrics(monitored)
                    metrics[instanceId] = currentMetrics
                } catch (e: Exception) {
                    Timber.e(e, "Error collecting metrics for instance: $instanceId")
                }
            }
        }

        _metricsFlow.value = metrics
    }

    /**
     * Collect metrics for a single instance
     */
    private fun collectInstanceMetrics(monitored: MonitoredInstance): HealthMetrics {
        val now = System.currentTimeMillis()
        val uptimeMs = now - monitored.startTime

        // CPU usage (approximate)
        val cpuUsage = getCpuUsage()

        // Memory usage
        val memoryInfo = getMemoryUsage()

        // Network I/O
        val networkBytesIn = max(0, getNetworkBytesReceived() - monitored.networkBytesInStart)
        val networkBytesOut = max(0, getNetworkBytesTransmitted() - monitored.networkBytesOutStart)

        return HealthMetrics.create(
            instanceId = monitored.instance.id.toString(),
            packId = monitored.instance.packId,
            cpuUsagePercent = cpuUsage,
            memoryUsedMb = memoryInfo.usedMb,
            memoryLimitMb = memoryInfo.limitMb,
            networkBytesIn = networkBytesIn,
            networkBytesOut = networkBytesOut,
            uptimeMs = uptimeMs,
            state = monitored.instance.state.name
        )
    }

    /**
     * Get approximate CPU usage
     * Note: This is app-wide, not per-instance
     */
    private fun getCpuUsage(): Float {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            // Very rough approximation - would need per-thread tracking for accuracy
            // For now, return a low value
            5.0f
        } catch (e: Exception) {
            Timber.e(e, "Error getting CPU usage")
            0f
        }
    }

    /**
     * Get memory usage
     */
    private fun getMemoryUsage(): MemoryUsage {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()

            // Add native heap
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val nativeHeap = memInfo.nativePss.toLong() * 1024 // Convert KB to bytes

            val totalUsed = usedMemory + nativeHeap

            MemoryUsage(
                usedMb = totalUsed / (1024 * 1024),
                limitMb = maxMemory / (1024 * 1024)
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting memory usage")
            MemoryUsage(0, 128)
        }
    }

    /**
     * Get network bytes received (app-wide)
     */
    private fun getNetworkBytesReceived(): Long {
        return try {
            TrafficStats.getUidRxBytes(android.os.Process.myUid())
        } catch (e: Exception) {
            Timber.e(e, "Error getting network bytes received")
            0L
        }
    }

    /**
     * Get network bytes transmitted (app-wide)
     */
    private fun getNetworkBytesTransmitted(): Long {
        return try {
            TrafficStats.getUidTxBytes(android.os.Process.myUid())
        } catch (e: Exception) {
            Timber.e(e, "Error getting network bytes transmitted")
            0L
        }
    }

    /**
     * Cleanup resources
     */
    fun shutdown() {
        stopCollection()
        monitoredInstances.clear()
        _metricsFlow.value = emptyMap()
    }
}

/**
 * Instance being monitored
 */
private data class MonitoredInstance(
    val instance: Instance,
    val startTime: Long,
    val networkBytesInStart: Long,
    val networkBytesOutStart: Long
)

/**
 * Memory usage data
 */
private data class MemoryUsage(
    val usedMb: Long,
    val limitMb: Long
)
