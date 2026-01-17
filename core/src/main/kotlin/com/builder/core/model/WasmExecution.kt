package com.builder.core.model

/**
 * Represents the state of a WASM execution on GitHub Actions.
 */
sealed class WasmExecutionState {
    object Idle : WasmExecutionState()
    object Triggering : WasmExecutionState()
    data class Running(val runId: Long, val progress: String = "Running...") : WasmExecutionState()
    data class Completed(val result: WasmExecutionResult) : WasmExecutionState()
    data class Error(val message: String) : WasmExecutionState()
}

/**
 * Result of a WASM pack execution.
 */
data class WasmExecutionResult(
    val runId: Long,
    val packName: String,
    val status: ExecutionStatus,
    val output: String,
    val executedAt: String,
    val duration: Long?, // in milliseconds
    val artifactUrl: String?
)

/**
 * Status of WASM execution.
 */
enum class ExecutionStatus {
    SUCCESS,
    FAILURE,
    CANCELLED,
    UNKNOWN
}

/**
 * Parsed execution log from GitHub Actions artifact.
 */
data class ExecutionLog(
    val timestamp: String,
    val wasmtimeVersion: String,
    val output: String,
    val exitCode: Int
) {
    companion object {
        /**
         * Parses execution.log content into ExecutionLog.
         * Expected format:
         * === Running WASM Pack ===
         * Timestamp: <timestamp>
         * Wasmtime version: <version>
         * ---
         * <output>
         * ---
         * Exit code: <code>
         * === Execution Complete ===
         */
        fun parse(content: String): ExecutionLog {
            val lines = content.lines()
            var timestamp = ""
            var wasmtimeVersion = ""
            var exitCode = 0
            val outputLines = mutableListOf<String>()
            var inOutput = false

            for (line in lines) {
                when {
                    line.startsWith("Timestamp:") -> {
                        timestamp = line.removePrefix("Timestamp:").trim()
                    }
                    line.startsWith("Wasmtime version:") -> {
                        wasmtimeVersion = line.removePrefix("Wasmtime version:").trim()
                    }
                    line.startsWith("Exit code:") -> {
                        exitCode = line.removePrefix("Exit code:").trim().toIntOrNull() ?: 0
                    }
                    line == "---" -> {
                        inOutput = !inOutput
                    }
                    inOutput -> {
                        outputLines.add(line)
                    }
                }
            }

            return ExecutionLog(
                timestamp = timestamp,
                wasmtimeVersion = wasmtimeVersion,
                output = outputLines.joinToString("\n"),
                exitCode = exitCode
            )
        }
    }
}
