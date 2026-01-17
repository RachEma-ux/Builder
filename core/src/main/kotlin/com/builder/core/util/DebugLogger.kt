package com.builder.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug logger that writes to a file for easy export and debugging.
 * Uses a singleton pattern for simplicity.
 */
object DebugLogger {

    private var logFile: File? = null
    private val mutex = Mutex()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val logs = mutableListOf<String>()

    /**
     * Initialize the logger with a cache directory path.
     * Must be called before logging.
     */
    fun init(cacheDir: File) {
        logFile = File(cacheDir, "debug_log.txt")
        // Clear old logs on init
        logFile?.writeText("=== Builder Debug Log ===\n")
        logFile?.appendText("Started: ${dateFormat.format(Date())}\n")
        logFile?.appendText("========================\n\n")
        synchronized(logs) {
            logs.clear()
            logs.add("=== Builder Debug Log ===")
            logs.add("Started: ${dateFormat.format(Date())}")
        }
    }

    /**
     * Log a debug message.
     */
    suspend fun d(tag: String, message: String) = log("DEBUG", tag, message)

    /**
     * Log an info message.
     */
    suspend fun i(tag: String, message: String) = log("INFO", tag, message)

    /**
     * Log a warning message.
     */
    suspend fun w(tag: String, message: String) = log("WARN", tag, message)

    /**
     * Log an error message.
     */
    suspend fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n  Exception: ${throwable.javaClass.simpleName}: ${throwable.message}\n  ${throwable.stackTraceToString().take(500)}"
        } else {
            message
        }
        log("ERROR", tag, fullMessage)
    }

    private suspend fun log(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] [$level] [$tag] $message"

        mutex.withLock {
            logs.add(logLine)
            // Keep only last 500 logs in memory
            if (logs.size > 500) {
                logs.removeAt(0)
            }
        }

        withContext(Dispatchers.IO) {
            try {
                logFile?.appendText("$logLine\n")
            } catch (e: Exception) {
                // Ignore file write errors
            }
        }
    }

    /**
     * Synchronous logging for use in non-suspend contexts.
     */
    fun logSync(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] [$level] [$tag] $message"

        synchronized(logs) {
            logs.add(logLine)
            if (logs.size > 500) {
                logs.removeAt(0)
            }
        }

        try {
            logFile?.appendText("$logLine\n")
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Get all logs as a single string.
     */
    fun getLogsAsString(): String {
        return synchronized(logs) {
            logs.joinToString("\n")
        }
    }

    /**
     * Get the log file for sharing.
     */
    fun getLogFile(): File? = logFile

    /**
     * Get log file path.
     */
    fun getLogFilePath(): String = logFile?.absolutePath ?: "Not initialized"

    /**
     * Clear all logs.
     */
    suspend fun clear() {
        mutex.withLock {
            logs.clear()
            logFile?.writeText("=== Log cleared at ${dateFormat.format(Date())} ===\n")
        }
    }
}
