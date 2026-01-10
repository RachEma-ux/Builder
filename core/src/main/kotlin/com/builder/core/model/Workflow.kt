package com.builder.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a workflow.json file structure.
 * See Builder_Final.md §8 for complete specification.
 */
@Serializable
data class Workflow(
    @SerialName("workflow_version")
    val workflowVersion: String,

    val id: String,

    val description: String? = null,

    val steps: List<WorkflowStep>
) {
    /**
     * Validates the workflow structure.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(id.isNotBlank()) { "Workflow ID cannot be blank" }
        require(steps.isNotEmpty()) { "Workflow must have at least one step" }

        // Ensure step IDs are unique
        val stepIds = steps.map { it.id }
        val duplicates = stepIds.groupingBy { it }.eachCount().filter { it.value > 1 }
        require(duplicates.isEmpty()) {
            "Duplicate step IDs found: ${duplicates.keys}"
        }
    }
}

/**
 * Base class for workflow steps.
 * See Builder_Final.md §8 for supported step types.
 */
@Serializable
sealed class WorkflowStep {
    abstract val id: String
    abstract val type: String

    @Serializable
    @SerialName("http.request")
    data class HttpRequest(
        override val id: String,
        override val type: String = "http.request",
        val method: String,
        val url: String,
        val headers: Map<String, String>? = null,
        val body: JsonElement? = null
    ) : WorkflowStep()

    @Serializable
    @SerialName("wasm.call")
    data class WasmCall(
        override val id: String,
        override val type: String = "wasm.call",
        val function: String,
        @SerialName("input_from")
        val inputFrom: String? = null
    ) : WorkflowStep()

    @Serializable
    @SerialName("kv.put")
    data class KvPut(
        override val id: String,
        override val type: String = "kv.put",
        val key: String,
        val value: JsonElement
    ) : WorkflowStep()

    @Serializable
    @SerialName("kv.get")
    data class KvGet(
        override val id: String,
        override val type: String = "kv.get",
        val key: String
    ) : WorkflowStep()

    @Serializable
    @SerialName("log")
    data class Log(
        override val id: String,
        override val type: String = "log",
        val level: String = "info",
        val message: String
    ) : WorkflowStep()

    @Serializable
    @SerialName("sleep")
    data class Sleep(
        override val id: String,
        override val type: String = "sleep",
        @SerialName("duration_ms")
        val durationMs: Long
    ) : WorkflowStep()

    @Serializable
    @SerialName("emit.event")
    data class EmitEvent(
        override val id: String,
        override val type: String = "emit.event",
        @SerialName("event_type")
        val eventType: String,
        val payload: JsonElement
    ) : WorkflowStep()
}
