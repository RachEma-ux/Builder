package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * GitHub Actions workflow model.
 */
data class GitHubWorkflow(
    val id: Long,
    @SerializedName("node_id")
    val nodeId: String,
    val name: String,
    val path: String,
    val state: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("badge_url")
    val badgeUrl: String?
) {
    /**
     * Gets the workflow filename from the path.
     */
    fun getFileName(): String = path.substringAfterLast("/")

    /**
     * Checks if this is a Builder-compatible deploy workflow.
     */
    fun isBuilderDeployWorkflow(): Boolean {
        return name.contains("Builder", ignoreCase = true) ||
               name.contains("Deploy", ignoreCase = true) ||
               path.contains("builder-deploy", ignoreCase = true)
    }
}

/**
 * Response wrapper for listing workflows.
 */
data class WorkflowsResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    val workflows: List<GitHubWorkflow>
)
