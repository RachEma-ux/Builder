package com.builder.core.model

/**
 * Represents a running instance of a Pack.
 * See Builder_Final.md §12 for lifecycle specification.
 */
data class Instance(
    val id: Long,
    val packId: String,
    val name: String,
    val state: InstanceState,
    val createdAt: Long,
    val startedAt: Long? = null,
    val stoppedAt: Long? = null,
    val lastExitCode: Int? = null,
    val lastExitReason: String? = null
) {
    /**
     * Checks if the instance is currently running.
     */
    fun isRunning(): Boolean = state == InstanceState.RUNNING

    /**
     * Checks if the instance can be started.
     */
    fun canStart(): Boolean = state == InstanceState.STOPPED

    /**
     * Checks if the instance can be paused.
     */
    fun canPause(): Boolean = state == InstanceState.RUNNING

    /**
     * Checks if the instance can be stopped.
     */
    fun canStop(): Boolean = state in listOf(InstanceState.RUNNING, InstanceState.PAUSED)
}

/**
 * Instance lifecycle states.
 * See Builder_Final.md §12.
 */
enum class InstanceState {
    /**
     * Instance is stopped. Default state after creation.
     */
    STOPPED,

    /**
     * Instance is actively running.
     */
    RUNNING,

    /**
     * Instance is paused (backgrounded or memory pressure).
     */
    PAUSED
}
