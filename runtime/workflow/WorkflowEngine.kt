package com.builder.runtime.workflow

import com.builder.core.model.LogLevel
import com.builder.core.model.LogSource
import com.builder.core.model.Workflow
import com.builder.core.model.WorkflowStep
import com.builder.runtime.LogCollector
import com.builder.runtime.wasm.WasmRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Workflow engine for executing workflow.json files.
 *
 * Enhanced with:
 * - Progress tracking
 * - Step-by-step logging via LogCollector
 * - Cancellation support
 * - Better error handling
 *
 * See Builder_Final.md §8 for workflow specification.
 */
class WorkflowEngine(
    private val wasmRuntime: WasmRuntime,
    private val httpClient: OkHttpClient,
    private val kvStore: KvStore,
    private val logCollector: LogCollector
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _progress = MutableStateFlow<WorkflowProgress?>(null)
    val progress: StateFlow<WorkflowProgress?> = _progress.asStateFlow()

    private var isCancelled = false

    /**
     * Executes a workflow.
     *
     * @param workflow The workflow to execute
     * @param context The execution context
     * @return Result of workflow execution
     */
    suspend fun execute(
        workflow: Workflow,
        context: WorkflowContext
    ): Result<WorkflowResult> {
        return try {
            isCancelled = false

            log(context, LogLevel.INFO, "Starting workflow execution: ${workflow.id}")
            workflow.validate()

            val totalSteps = workflow.steps.size
            updateProgress(0, totalSteps, "Starting workflow")

            for ((index, step) in workflow.steps.withIndex()) {
                if (isCancelled) {
                    log(context, LogLevel.WARN, "Workflow execution cancelled")
                    return Result.failure(WorkflowCancelledException("Workflow cancelled by user"))
                }

                log(context, LogLevel.DEBUG, "Executing step ${index + 1}/$totalSteps: ${step.id} (${step.type})")
                updateProgress(index, totalSteps, "Executing step: ${step.id}")

                val stepResult = executeStep(step, context)
                context.setStepResult(step.id, stepResult)

                if (stepResult.isFailure) {
                    val error = stepResult.exceptionOrNull()
                    log(context, LogLevel.ERROR, "Step ${step.id} failed: ${error?.message}")
                    updateProgress(index + 1, totalSteps, "Failed at step: ${step.id}")

                    return Result.failure(
                        WorkflowExecutionException(
                            "Step ${step.id} failed",
                            error
                        )
                    )
                }

                log(context, LogLevel.DEBUG, "Step ${step.id} completed successfully")
            }

            updateProgress(totalSteps, totalSteps, "Workflow completed")
            log(context, LogLevel.INFO, "Workflow ${workflow.id} completed successfully")

            Result.success(context.toResult())
        } catch (e: Exception) {
            log(context, LogLevel.ERROR, "Workflow execution failed: ${e.message}")
            Timber.e(e, "Workflow execution failed")
            Result.failure(e)
        } finally {
            _progress.value = null
        }
    }

    /**
     * Cancel the currently executing workflow
     */
    fun cancel() {
        isCancelled = true
    }

    private fun updateProgress(current: Int, total: Int, message: String) {
        _progress.value = WorkflowProgress(
            currentStep = current,
            totalSteps = total,
            message = message,
            percent = if (total > 0) (current.toFloat() / total * 100).toInt() else 0
        )
    }

    private fun log(context: WorkflowContext, level: LogLevel, message: String) {
        logCollector.log(
            instanceId = context.instanceId,
            packId = context.packId,
            level = level,
            message = message,
            source = LogSource.WORKFLOW_STEP
        )
    }

    /**
     * Executes a single workflow step.
     */
    private suspend fun executeStep(
        step: WorkflowStep,
        context: WorkflowContext
    ): Result<Any> {
        return when (step) {
            is WorkflowStep.HttpRequest -> executeHttpRequest(step, context)
            is WorkflowStep.WasmCall -> executeWasmCall(step, context)
            is WorkflowStep.KvPut -> executeKvPut(step, context)
            is WorkflowStep.KvGet -> executeKvGet(step, context)
            is WorkflowStep.Log -> executeLog(step, context)
            is WorkflowStep.Sleep -> executeSleep(step, context)
            is WorkflowStep.EmitEvent -> executeEmitEvent(step, context)
        }
    }

    private suspend fun executeHttpRequest(
        step: WorkflowStep.HttpRequest,
        context: WorkflowContext
    ): Result<Any> {
        return try {
            val requestBuilder = Request.Builder().url(step.url)

            // Add headers
            step.headers?.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            // Add body if present
            step.body?.let { body ->
                val bodyJson = json.encodeToString(
                    kotlinx.serialization.json.JsonElement.serializer(),
                    body
                )
                requestBuilder.method(
                    step.method,
                    bodyJson.toRequestBody()
                )
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()

            val responseBody = response.body?.string() ?: ""
            Result.success(mapOf("status" to response.code, "body" to responseBody))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeWasmCall(
        step: WorkflowStep.WasmCall,
        context: WorkflowContext
    ): Result<Any> {
        return try {
            // Get input from previous step if specified
            val input = step.inputFrom?.let { context.getStepResult(it) }

            // TODO: Call WASM function via wasmRuntime
            // For now, return stub result
            Result.success(mapOf("result" to "wasm_call_stub"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeKvPut(
        step: WorkflowStep.KvPut,
        context: WorkflowContext
    ): Result<Any> {
        return try {
            val value = json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                step.value
            )
            kvStore.put(context.packId, step.key, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeKvGet(
        step: WorkflowStep.KvGet,
        context: WorkflowContext
    ): Result<Any> {
        return try {
            val value = kvStore.get(context.packId, step.key)
            Result.success(value ?: "null")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeLog(
        step: WorkflowStep.Log,
        context: WorkflowContext
    ): Result<Any> {
        return try {
            val level = when (step.level.lowercase()) {
                "debug" -> LogLevel.DEBUG
                "warn" -> LogLevel.WARN
                "error" -> LogLevel.ERROR
                else -> LogLevel.INFO
            }

            log(context, level, step.message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeSleep(
        step: WorkflowStep.Sleep,
        context: WorkflowContext
    ): Result<Any> {
        return try {
            delay(step.durationMs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeEmitEvent(
        step: WorkflowStep.EmitEvent,
        context: WorkflowContext
    ): Result<Any> {
        return try {
            // TODO: Implement event bus
            Timber.i("[${context.packId}] Event: ${step.eventType}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Workflow execution progress
 */
data class WorkflowProgress(
    val currentStep: Int,
    val totalSteps: Int,
    val message: String,
    val percent: Int
)

/**
 * Exception thrown when workflow execution fails.
 */
class WorkflowExecutionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when workflow is cancelled.
 */
class WorkflowCancelledException(
    message: String
) : Exception(message)
