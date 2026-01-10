package com.builder.runtime.workflow

import com.builder.core.model.Workflow
import com.builder.core.model.WorkflowStep
import com.builder.runtime.wasm.WasmRuntime
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Workflow engine for executing workflow.json files.
 * See Builder_Final.md §8 for workflow specification.
 */
class WorkflowEngine(
    private val wasmRuntime: WasmRuntime,
    private val httpClient: OkHttpClient,
    private val kvStore: KvStore
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

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
            Timber.i("Starting workflow execution: ${workflow.id}")
            workflow.validate()

            for (step in workflow.steps) {
                Timber.d("Executing step: ${step.id} (${step.type})")

                val stepResult = executeStep(step, context)
                context.setStepResult(step.id, stepResult)

                if (stepResult.isFailure) {
                    Timber.e("Step ${step.id} failed: ${stepResult.exceptionOrNull()}")
                    return Result.failure(
                        WorkflowExecutionException(
                            "Step ${step.id} failed",
                            stepResult.exceptionOrNull()
                        )
                    )
                }
            }

            Timber.i("Workflow ${workflow.id} completed successfully")
            Result.success(context.toResult())
        } catch (e: Exception) {
            Timber.e(e, "Workflow execution failed")
            Result.failure(e)
        }
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
            when (step.level.lowercase()) {
                "debug" -> Timber.d("[${context.packId}] ${step.message}")
                "info" -> Timber.i("[${context.packId}] ${step.message}")
                "warn" -> Timber.w("[${context.packId}] ${step.message}")
                "error" -> Timber.e("[${context.packId}] ${step.message}")
                else -> Timber.i("[${context.packId}] ${step.message}")
            }
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
 * Exception thrown when workflow execution fails.
 */
class WorkflowExecutionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
