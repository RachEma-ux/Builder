package com.builder.core.model.github

import com.google.gson.annotations.SerializedName

/**
 * GitHub repository variable model.
 */
data class RepoVariable(
    val name: String,
    val value: String,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

/**
 * Response for listing repository variables.
 */
data class VariablesResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    val variables: List<RepoVariable>
)

/**
 * Request body for creating a repository variable.
 */
data class CreateVariableRequest(
    val name: String,
    val value: String
)

/**
 * Request body for updating a repository variable.
 */
data class UpdateVariableRequest(
    val value: String
)
