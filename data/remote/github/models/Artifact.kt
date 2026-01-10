package com.builder.data.remote.github.models

import com.google.gson.annotations.SerializedName

/**
 * GitHub workflow artifact response model.
 */
data class Artifact(
    val id: Long,
    val name: String,
    @SerializedName("size_in_bytes")
    val sizeInBytes: Long,
    val url: String,
    @SerializedName("archive_download_url")
    val archiveDownloadUrl: String,
    val expired: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("expires_at")
    val expiresAt: String,
    @SerializedName("workflow_run")
    val workflowRun: ArtifactWorkflowRun?
)

data class ArtifactWorkflowRun(
    val id: Long,
    @SerializedName("repository_id")
    val repositoryId: Long,
    @SerializedName("head_repository_id")
    val headRepositoryId: Long,
    @SerializedName("head_branch")
    val headBranch: String,
    @SerializedName("head_sha")
    val headSha: String
)

data class ArtifactsResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    val artifacts: List<Artifact>
)
