package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * GitHub workflow run response model.
 */
data class WorkflowRun(
    val id: Long,
    val name: String,
    @SerializedName("head_branch")
    val headBranch: String?,
    @SerializedName("head_sha")
    val headSha: String,
    val status: String,
    val conclusion: String?,
    @SerializedName("workflow_id")
    val workflowId: Long,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("run_number")
    val runNumber: Int,
    @SerializedName("run_attempt")
    val runAttempt: Int
) {
    /**
     * Checks if the workflow run is complete.
     */
    fun isComplete(): Boolean = status == "completed"

    /**
     * Checks if the workflow run was successful.
     */
    fun isSuccess(): Boolean = conclusion == "success"

    /**
     * Checks if the workflow run failed.
     */
    fun isFailed(): Boolean = conclusion == "failure"

    /**
     * Checks if the workflow run is still running.
     */
    fun isRunning(): Boolean = status in listOf("queued", "in_progress")
}

data class WorkflowRunsResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("workflow_runs")
    val workflowRuns: List<WorkflowRun>
)

data class WorkflowDispatchRequest(
    val ref: String,
    val inputs: Map<String, String> = emptyMap()
)
