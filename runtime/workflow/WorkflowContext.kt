package com.builder.runtime.workflow

/**
 * Context for workflow execution.
 * Stores step results and metadata.
 */
class WorkflowContext(
    val packId: String,
    val instanceId: String
) {
    private val stepResults = mutableMapOf<String, Result<Any>>()

    /**
     * Sets the result of a step.
     */
    fun setStepResult(stepId: String, result: Result<Any>) {
        stepResults[stepId] = result
    }

    /**
     * Gets the result of a step.
     */
    fun getStepResult(stepId: String): Any? {
        return stepResults[stepId]?.getOrNull()
    }

    /**
     * Converts the context to a workflow result.
     */
    fun toResult(): WorkflowResult {
        return WorkflowResult(
            packId = packId,
            instanceId = instanceId,
            stepResults = stepResults.mapValues { (_, result) ->
                result.getOrNull()
            }
        )
    }
}

/**
 * Result of workflow execution.
 */
data class WorkflowResult(
    val packId: String,
    val instanceId: String,
    val stepResults: Map<String, Any?>
)

/**
 * Simple key-value store for workflow state.
 */
interface KvStore {
    /**
     * Stores a value.
     */
    suspend fun put(packId: String, key: String, value: String)

    /**
     * Retrieves a value.
     */
    suspend fun get(packId: String, key: String): String?

    /**
     * Deletes a value.
     */
    suspend fun delete(packId: String, key: String)
}

/**
 * In-memory implementation of KvStore.
 * TODO: Replace with persistent storage (Room database).
 */
class InMemoryKvStore : KvStore {
    private val storage = mutableMapOf<String, MutableMap<String, String>>()

    override suspend fun put(packId: String, key: String, value: String) {
        storage.getOrPut(packId) { mutableMapOf() }[key] = value
    }

    override suspend fun get(packId: String, key: String): String? {
        return storage[packId]?.get(key)
    }

    override suspend fun delete(packId: String, key: String) {
        storage[packId]?.remove(key)
    }
}
